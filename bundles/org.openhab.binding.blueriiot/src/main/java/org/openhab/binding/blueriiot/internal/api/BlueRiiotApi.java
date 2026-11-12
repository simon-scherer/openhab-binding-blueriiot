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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpResponseException;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.FutureResponseListener;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.blueriiot.internal.api.dto.BlueDevice;
import org.openhab.binding.blueriiot.internal.api.dto.LastMeasurementsResponse;
import org.openhab.binding.blueriiot.internal.api.dto.LoginResponse;
import org.openhab.binding.blueriiot.internal.api.dto.PoolDevicesResponse;
import org.openhab.binding.blueriiot.internal.api.dto.PoolsResponse;
import org.openhab.binding.blueriiot.internal.api.dto.ReleaseResponse;
import org.openhab.binding.blueriiot.internal.model.AccountData;
import org.openhab.binding.blueriiot.internal.model.AccountData.Pool;
import org.openhab.binding.blueriiot.internal.model.DeviceData;
import org.openhab.binding.blueriiot.internal.model.MeasurementValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * Client for the unofficial Blue Riiot cloud API ({@code api.riiotlabs.com}) used by the Blue Connect app.
 * <ol>
 * <li>{@code POST user/login} with e-mail and password returns temporary AWS credentials.</li>
 * <li>All further requests are signed with AWS Signature Version 4 (service {@code execute-api}, region
 * {@code eu-west-1}).</li>
 * </ol>
 *
 * @author Simon Scherer - Initial contribution
 */
@NonNullByDefault
public class BlueRiiotApi {

    public static final String BASE_URL = "https://api.riiotlabs.com/prod/";
    private static final String REGION = "eu-west-1";
    private static final String SERVICE = "execute-api";
    private static final String USER_AGENT = "openHAB Blue Riiot binding";
    private static final int TIMEOUT_SECONDS = 30;
    private static final int MAX_RESPONSE_BYTES = 2 * 1024 * 1024;
    private static final long REFRESH_MARGIN_SECONDS = 120;
    private static final long FALLBACK_TTL_SECONDS = 3300;

    private final Logger logger = LoggerFactory.getLogger(BlueRiiotApi.class);
    private final Gson gson = new Gson();
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String email;
    private final String password;
    private final String language;

    private @Nullable String accessKey;
    private @Nullable String secretKey;
    private @Nullable String sessionToken;
    private Instant expiresAt = Instant.EPOCH;

    private record Response(int status, String body) {
    }

    public BlueRiiotApi(HttpClient httpClient, String email, String password, String language) {
        this(httpClient, BASE_URL, email, password, language);
    }

    BlueRiiotApi(HttpClient httpClient, String baseUrl, String email, String password, String language) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.email = email;
        this.password = password;
        this.language = language.isBlank() ? "en" : language;
    }

    // ------------------------------------------------------------------ public API

    /** Polls pools, devices and last measurements in one go. */
    public synchronized AccountData fetchAll() throws BlueRiiotException {
        List<Pool> pools = new ArrayList<>();
        Map<String, DeviceData> devices = new LinkedHashMap<>();
        PoolsResponse poolsResponse = get("swimming_pool", PoolsResponse.class);
        List<PoolsResponse.PoolEntry> poolEntries = poolsResponse.data;
        if (poolEntries == null) {
            return new AccountData(pools, devices);
        }
        for (PoolsResponse.PoolEntry entry : poolEntries) {
            String poolId = entry.swimmingPoolId;
            if (poolId == null) {
                continue;
            }
            Pool pool = new Pool(poolId, poolName(entry));
            pools.add(pool);
            PoolDevicesResponse devicesResponse = get("swimming_pool/" + seg(poolId) + "/blue",
                    PoolDevicesResponse.class);
            List<PoolDevicesResponse.Entry> deviceEntries = devicesResponse.data;
            if (deviceEntries == null) {
                continue;
            }
            for (PoolDevicesResponse.Entry deviceEntry : deviceEntries) {
                String serial = deviceEntry.blueDeviceSerial;
                if (serial == null) {
                    continue;
                }
                DeviceData device = new DeviceData(serial, pool.id(), pool.name());
                try {
                    applyDeviceInfo(device, get("blue/" + seg(serial), BlueDevice.class));
                } catch (BlueRiiotException e) {
                    if (e.isAuthError()) {
                        throw e;
                    }
                    logger.debug("Cannot read device info of {}: {}", serial, e.getMessage());
                }
                applyMeasurements(device, get("swimming_pool/" + seg(poolId) + "/blue/" + seg(serial)
                        + "/lastMeasurements?mode=blue_and_strip", LastMeasurementsResponse.class));
                devices.put(serial, device);
            }
        }
        return new AccountData(pools, devices);
    }

    /**
     * Asks the cloud to take over the newest measurement the Blue Extender has stored for this device (the "Wi-Fi"
     * option in the app). It does not start a new measurement.
     *
     * @return the message returned by the cloud, may be null
     */
    public synchronized @Nullable String releaseLastUnprocessedEvent(String serial) throws BlueRiiotException {
        Response response = request(HttpMethod.POST, "blue/" + seg(serial) + "/releaseLastUnprocessedEvent", true);
        if (response.body().isBlank()) {
            return null;
        }
        return parse(response.body(), ReleaseResponse.class).message;
    }

    public synchronized void invalidate() {
        accessKey = null;
        secretKey = null;
        sessionToken = null;
        expiresAt = Instant.EPOCH;
    }

    // ------------------------------------------------------------------ login

    private void login() throws BlueRiiotException {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);
        Request request = httpClient.newRequest(baseUrl + "user/login").method(HttpMethod.POST)
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS).agent(USER_AGENT)
                .header(HttpHeader.ACCEPT, "application/json")
                .content(new StringContentProvider("application/json", body.toString(), StandardCharsets.UTF_8));
        Response response = send(request);
        int status = response.status();
        if (status == 400 || status == 401 || status == 403) {
            throw new BlueRiiotException("Login rejected (HTTP " + status + "), check e-mail and password", true);
        }
        if (status != 200) {
            throw new BlueRiiotException("Login failed: HTTP " + status + " " + abbreviate(response.body()), false);
        }
        LoginResponse.Credentials credentials = parse(response.body(), LoginResponse.class).credentials;
        String ak = credentials == null ? null : credentials.accessKey;
        String sk = credentials == null ? null : credentials.secretKey;
        if (credentials == null || ak == null || sk == null) {
            throw new BlueRiiotException("Login response contains no credentials", true);
        }
        accessKey = ak;
        secretKey = sk;
        sessionToken = credentials.sessionToken;
        Instant expiration = parseExpiration(credentials.expiration);
        expiresAt = expiration != null ? expiration : Instant.now().plusSeconds(FALLBACK_TTL_SECONDS);
        logger.debug("Logged in to Blue Riiot, credentials valid until {}", expiresAt);
    }

    static @Nullable Instant parseExpiration(@Nullable JsonElement value) {
        if (value == null || !value.isJsonPrimitive()) {
            return null;
        }
        if (value.getAsJsonPrimitive().isNumber()) {
            long number = value.getAsLong();
            // seconds or milliseconds since the epoch
            return number > 1_000_000_000_000L ? Instant.ofEpochMilli(number) : Instant.ofEpochSecond(number);
        }
        return parseInstant(value.getAsString());
    }

    // ------------------------------------------------------------------ requests

    private <T> T get(String endpoint, Class<T> type) throws BlueRiiotException {
        Response response = request(HttpMethod.GET, endpoint, true);
        return parse(response.body(), type);
    }

    private Response request(HttpMethod method, String endpoint, boolean retryOnAuth) throws BlueRiiotException {
        if (accessKey == null || Instant.now().isAfter(expiresAt.minusSeconds(REFRESH_MARGIN_SECONDS))) {
            login();
        }
        String ak = accessKey;
        String sk = secretKey;
        if (ak == null || sk == null) {
            throw new BlueRiiotException("Not logged in", true);
        }
        URI uri = URI.create(baseUrl + endpoint);
        Map<String, String> signed = AwsSigV4Signer.sign(method.asString(), uri, ak, sk, sessionToken, REGION, SERVICE,
                ZonedDateTime.now(ZoneOffset.UTC));
        Request request = httpClient.newRequest(uri).method(method).timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .agent(USER_AGENT).header(HttpHeader.ACCEPT, "application/json")
                .header(HttpHeader.ACCEPT_LANGUAGE, language);
        signed.forEach(request::header);
        Response response = send(request);
        int status = response.status();
        if (status == 401 || status == 403) {
            invalidate();
            if (retryOnAuth) {
                logger.debug("HTTP {} for {}, logging in again", status, endpoint);
                return request(method, endpoint, false);
            }
            throw new BlueRiiotException("Not authorized for " + endpoint + " (HTTP " + status + ")", true);
        }
        if (status != 200) {
            throw new BlueRiiotException(
                    method + " " + endpoint + " failed: HTTP " + status + " " + abbreviate(response.body()), false);
        }
        logger.trace("{} {} -> {}", method, endpoint, response.body());
        return response;
    }

    private Response send(Request request) throws BlueRiiotException {
        FutureResponseListener listener = new FutureResponseListener(request, MAX_RESPONSE_BYTES);
        request.send(listener);
        try {
            ContentResponse response = listener.get(TIMEOUT_SECONDS + 5, TimeUnit.SECONDS);
            return new Response(response.getStatus(), new String(response.getContent(), StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            request.abort(e);
            Thread.currentThread().interrupt();
            throw new BlueRiiotException("Interrupted", false, e);
        } catch (TimeoutException e) {
            request.abort(e);
            throw new BlueRiiotException("Timeout: " + request.getURI(), false, e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof HttpResponseException hre) {
                // Jetty rejects a 401 without WWW-Authenticate header as protocol violation; the status is still
                // needed to detect expired credentials
                return new Response(hre.getResponse().getStatus(), "");
            }
            throw new BlueRiiotException("Connection error: " + (cause != null ? cause.getMessage() : e.getMessage()),
                    false, e);
        }
    }

    private <T> T parse(String body, Class<T> type) throws BlueRiiotException {
        try {
            @Nullable
            T result = gson.fromJson(body, type);
            if (result == null) {
                throw new BlueRiiotException("Empty response from Blue Riiot", false);
            }
            return result;
        } catch (JsonSyntaxException e) {
            throw new BlueRiiotException("Invalid JSON from Blue Riiot: " + e.getMessage(), false, e);
        }
    }

    // ------------------------------------------------------------------ mapping

    static String poolName(PoolsResponse.PoolEntry entry) {
        PoolsResponse.PoolDetails details = entry.swimmingPool;
        String name = details == null ? null : details.name;
        if (name == null) {
            name = entry.name;
        }
        return name == null ? "Pool" : name;
    }

    static void applyDeviceInfo(DeviceData device, BlueDevice info) {
        device.productName = info.hwProductName;
        device.productType = info.hwProductType;
        device.hwGeneration = info.hwGeneration;
        String firmware = info.fwVersionPsoc;
        device.firmware = firmware != null ? firmware : info.fwVersion;
        device.batteryLow = info.batteryLow;
        device.lastMeasureBle = parseInstant(info.lastMeasureBle);
        device.lastMeasureSigfox = parseInstant(info.lastMeasureSigfox);
        device.lastMeasureWifi = parseInstant(info.lastMeasureWifi);
    }

    static void applyMeasurements(DeviceData device, LastMeasurementsResponse response) {
        device.lastBlueMeasurement = parseInstant(response.lastBlueMeasureTimestamp);
        device.measurements.clear();
        List<LastMeasurementsResponse.Measurement> data = response.data;
        if (data == null) {
            return;
        }
        for (LastMeasurementsResponse.Measurement m : data) {
            String name = m.name;
            Double value = m.value;
            if (name == null || value == null) {
                continue;
            }
            device.measurements.add(new MeasurementValue(name, value, parseInstant(m.timestamp),
                    Boolean.TRUE.equals(m.expired), m.okMin, m.okMax, m.warningLow, m.warningHigh, m.issuer));
        }
    }

    static @Nullable Instant parseInstant(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            try {
                return OffsetDateTime.parse(value).toInstant();
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }

    private static String seg(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String abbreviate(String s) {
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }
}
