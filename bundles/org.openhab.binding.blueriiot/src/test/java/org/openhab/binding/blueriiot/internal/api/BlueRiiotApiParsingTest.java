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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.openhab.binding.blueriiot.internal.api.dto.BlueDevice;
import org.openhab.binding.blueriiot.internal.api.dto.LastMeasurementsResponse;
import org.openhab.binding.blueriiot.internal.api.dto.LoginResponse;
import org.openhab.binding.blueriiot.internal.api.dto.PoolDevicesResponse;
import org.openhab.binding.blueriiot.internal.api.dto.PoolsResponse;
import org.openhab.binding.blueriiot.internal.model.DeviceData;
import org.openhab.binding.blueriiot.internal.model.MeasurementValue;

import com.google.gson.Gson;
import com.google.gson.JsonPrimitive;

/**
 * Tests the mapping of (anonymized) cloud responses.
 *
 * @author Simon Scherer - Initial contribution
 */
@NonNullByDefault
public class BlueRiiotApiParsingTest {

    private final Gson gson = new Gson();

    @Test
    public void login() throws IOException {
        LoginResponse response = read("login.json", LoginResponse.class);
        LoginResponse.Credentials credentials = response.credentials;
        assertNotNull(credentials);
        assertEquals("AKIAEXAMPLE", credentials.accessKey);
        assertEquals("secret", credentials.secretKey);
        assertEquals("session", credentials.sessionToken);
        assertEquals(Instant.parse("2099-01-01T00:00:00Z"), BlueRiiotApi.parseExpiration(credentials.expiration));
    }

    @Test
    public void expirationAsEpoch() {
        assertEquals(Instant.ofEpochSecond(1_800_000_000L),
                BlueRiiotApi.parseExpiration(new JsonPrimitive(1_800_000_000L)));
        assertEquals(Instant.ofEpochMilli(1_800_000_000_000L),
                BlueRiiotApi.parseExpiration(new JsonPrimitive(1_800_000_000_000L)));
        assertNull(BlueRiiotApi.parseExpiration(null));
    }

    @Test
    public void poolsAndDevices() throws IOException {
        PoolsResponse pools = read("swimming_pool.json", PoolsResponse.class);
        List<PoolsResponse.PoolEntry> entries = pools.data;
        assertNotNull(entries);
        assertEquals(1, entries.size());
        assertEquals("pool-1", entries.get(0).swimmingPoolId);
        assertEquals("Garden Pool", BlueRiiotApi.poolName(entries.get(0)));

        PoolDevicesResponse devices = read("pool_blue.json", PoolDevicesResponse.class);
        List<PoolDevicesResponse.Entry> deviceEntries = devices.data;
        assertNotNull(deviceEntries);
        assertEquals("0A1B2C3D", deviceEntries.get(0).blueDeviceSerial);
    }

    @Test
    public void deviceAndMeasurements() throws IOException {
        DeviceData device = new DeviceData("0A1B2C3D", "pool-1", "Garden Pool");
        BlueRiiotApi.applyDeviceInfo(device, read("blue.json", BlueDevice.class));
        BlueRiiotApi.applyMeasurements(device, read("last_measurements.json", LastMeasurementsResponse.class));

        assertEquals("Blue Connect Go", device.productName);
        assertEquals("2.3.0", device.firmware);
        assertEquals(Boolean.FALSE, device.batteryLow);
        assertEquals(Instant.parse("2026-11-12T21:23:20.331Z"), device.lastMeasurement());

        MeasurementValue ph = device.best("ph").orElseThrow();
        assertEquals(8.2, ph.value(), 0.001);
        assertFalse(ph.isStrip());
        assertEquals(MeasurementValue.STATUS_WARNING, ph.status());

        assertEquals(MeasurementValue.STATUS_CRITICAL, device.best("orp").orElseThrow().status());
        assertEquals(MeasurementValue.STATUS_WARNING, device.best("temperature").orElseThrow().status());
        assertTrue(device.best("ta").orElseThrow().expired());
        assertTrue(device.best("fcl").isEmpty());
    }

    private <T> T read(String name, Class<T> type) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(name)) {
            assertNotNull(in, name);
            @Nullable
            T result = gson.fromJson(new String(in.readAllBytes(), StandardCharsets.UTF_8), type);
            if (result == null) {
                throw new IOException("Cannot parse " + name);
            }
            return result;
        }
    }
}
