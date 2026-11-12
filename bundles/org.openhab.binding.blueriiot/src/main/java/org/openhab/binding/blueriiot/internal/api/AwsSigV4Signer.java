/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.blueriiot.internal.api;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * AWS Signature Version 4 for GET requests without body (as needed for the Blue Riiot API Gateway).
 *
 * @author Simon Scherer - Initial contribution
 */
@NonNullByDefault
public final class AwsSigV4Signer {

    private static final DateTimeFormatter AMZ_DATE = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter DATE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String EMPTY_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    private AwsSigV4Signer() {
    }

    /**
     * Returns the headers to add to the request: {@code x-amz-date}, optionally {@code x-amz-security-token}, and
     * {@code Authorization}. The {@code host} header is part of the signature but set by the HTTP client itself.
     */
    public static Map<String, String> sign(String method, URI uri, String accessKey, String secretKey,
            @Nullable String sessionToken, String region, String service, ZonedDateTime now) {
        ZonedDateTime utc = now.withZoneSameInstant(ZoneOffset.UTC);
        String amzDate = AMZ_DATE.format(utc);
        String dateStamp = DATE_STAMP.format(utc);

        TreeMap<String, String> headers = new TreeMap<>();
        String host = String.valueOf(uri.getHost());
        if (uri.getPort() != -1 && uri.getPort() != 443 && uri.getPort() != 80) {
            host += ":" + uri.getPort();
        }
        headers.put("host", host);
        headers.put("x-amz-date", amzDate);
        if (sessionToken != null && !sessionToken.isEmpty()) {
            headers.put("x-amz-security-token", sessionToken);
        }

        StringBuilder canonicalHeaders = new StringBuilder();
        for (Map.Entry<String, String> e : headers.entrySet()) {
            canonicalHeaders.append(e.getKey()).append(':').append(e.getValue().trim()).append('\n');
        }
        String signedHeaders = String.join(";", headers.keySet());

        String canonicalRequest = method + '\n' + canonicalPath(uri.getRawPath()) + '\n'
                + canonicalQuery(uri.getRawQuery()) + '\n' + canonicalHeaders + '\n' + signedHeaders + '\n'
                + EMPTY_SHA256;

        String scope = dateStamp + '/' + region + '/' + service + "/aws4_request";
        String stringToSign = "AWS4-HMAC-SHA256\n" + amzDate + '\n' + scope + '\n' + sha256Hex(canonicalRequest);

        byte[] kDate = hmac(("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8), dateStamp);
        byte[] kRegion = hmac(kDate, region);
        byte[] kService = hmac(kRegion, service);
        byte[] kSigning = hmac(kService, "aws4_request");
        String signature = HexFormat.of().formatHex(hmac(kSigning, stringToSign));

        TreeMap<String, String> result = new TreeMap<>();
        result.put("x-amz-date", amzDate);
        if (sessionToken != null && !sessionToken.isEmpty()) {
            result.put("x-amz-security-token", sessionToken);
        }
        result.put("Authorization", "AWS4-HMAC-SHA256 Credential=" + accessKey + '/' + scope + ", SignedHeaders="
                + signedHeaders + ", Signature=" + signature);
        return result;
    }

    /** Non-S3 services: each path segment is URI-encoded twice (raw path is already encoded once). */
    static String canonicalPath(@Nullable String rawPath) {
        if (rawPath == null || rawPath.isEmpty()) {
            return "/";
        }
        StringBuilder sb = new StringBuilder();
        String[] segments = rawPath.split("/", -1);
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                sb.append('/');
            }
            sb.append(encode(segments[i]));
        }
        return sb.toString();
    }

    static String canonicalQuery(@Nullable String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return "";
        }
        List<String[]> params = new ArrayList<>();
        for (String part : rawQuery.split("&")) {
            if (part.isEmpty()) {
                continue;
            }
            int idx = part.indexOf('=');
            String k = idx >= 0 ? part.substring(0, idx) : part;
            String v = idx >= 0 ? part.substring(idx + 1) : "";
            params.add(new String[] { encode(decode(k)), encode(decode(v)) });
        }
        params.sort((a, b) -> a[0].equals(b[0]) ? a[1].compareTo(b[1]) : a[0].compareTo(b[0]));
        StringBuilder sb = new StringBuilder();
        for (String[] p : params) {
            if (!sb.isEmpty()) {
                sb.append('&');
            }
            sb.append(p[0]).append('=').append(p[1]);
        }
        return sb.toString();
    }

    /** RFC 3986 encoding: everything except unreserved characters. */
    static String encode(String value) {
        StringBuilder sb = new StringBuilder();
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            char c = (char) (b & 0xff);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '_'
                    || c == '.' || c == '~') {
                sb.append(c);
            } else {
                sb.append('%').append(String.format("%02X", b & 0xff));
            }
        }
        return sb.toString();
    }

    private static String decode(String value) {
        return java.net.URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    private static byte[] hmac(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 not available", e);
        }
    }

    private static String sha256Hex(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
