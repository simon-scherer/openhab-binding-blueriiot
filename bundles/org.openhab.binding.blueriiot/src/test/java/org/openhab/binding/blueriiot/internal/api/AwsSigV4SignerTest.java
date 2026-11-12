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

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AwsSigV4Signer}. The expected signatures come from the AWS test suite and from botocore.
 *
 * @author Simon Scherer - Initial contribution
 */
@NonNullByDefault
public class AwsSigV4SignerTest {

    private static final ZonedDateTime NOW = ZonedDateTime.of(2026, 10, 31, 19, 30, 0, 0, ZoneOffset.UTC);

    @Test
    public void awsTestSuiteGetVanilla() {
        Map<String, String> headers = AwsSigV4Signer.sign("GET", URI.create("https://example.amazonaws.com/"),
                "AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY", null, "us-east-1", "service",
                ZonedDateTime.of(2015, 8, 30, 12, 36, 0, 0, ZoneOffset.UTC));
        assertEquals(
                "AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20150830/us-east-1/service/aws4_request, "
                        + "SignedHeaders=host;x-amz-date, "
                        + "Signature=5fa00fa31553b73ebf1942676e86291e8372ff2a2260956d9b8aae1d763fbf31",
                headers.get("Authorization"));
        assertEquals("20150830T123600Z", headers.get("x-amz-date"));
    }

    @Test
    public void getWithQueryAndSessionToken() {
        Map<String, String> headers = AwsSigV4Signer.sign("GET", URI.create(
                "https://api.riiotlabs.com/prod/swimming_pool/abc-123/blue/00ABC/lastMeasurements?mode=blue_and_strip"),
                "AKIAFAKE", "secretsecret", "tok/en+abc==", "eu-west-1", "execute-api", NOW);
        assertEquals("tok/en+abc==", headers.get("x-amz-security-token"));
        assertSignature("2fc778019d20bf4bfd64aed65eb426e99ac31fa469aafd1148c54a6190c6fc74", headers);
    }

    @Test
    public void getWithoutQuery() {
        Map<String, String> headers = AwsSigV4Signer.sign("GET",
                URI.create("https://api.riiotlabs.com/prod/swimming_pool"), "AKIAFAKE", "secretsecret", "tok/en+abc==",
                "eu-west-1", "execute-api", NOW);
        assertSignature("14bcbe6458ec4853a6b7c4261b6390f5525e2142674032403e61ea6be7d974d0", headers);
    }

    @Test
    public void postWithoutBody() {
        Map<String, String> headers = AwsSigV4Signer.sign("POST",
                URI.create("https://api.riiotlabs.com/prod/blue/0A1B2C3D/releaseLastUnprocessedEvent"), "AKIAFAKE",
                "secretsecret", "tok/en+abc==", "eu-west-1", "execute-api", NOW);
        assertSignature("45f9724e32f0f6262dd3be7d248e11af17e506347507c3b25963214d4f350ecc", headers);
    }

    private static void assertSignature(String expected, Map<String, String> headers) {
        String authorization = headers.get("Authorization");
        assertNotNull(authorization);
        assertTrue(authorization.endsWith("Signature=" + expected), authorization);
    }
}
