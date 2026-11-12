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
package org.openhab.binding.blueriiot.internal.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Everything known about one Blue Connect device after a poll.
 *
 * @author Simon Scherer - Initial contribution
 */
@NonNullByDefault
public class DeviceData {

    public final String serial;
    public final String poolId;
    public final String poolName;
    public @Nullable String productName;
    public @Nullable String productType;
    public @Nullable Integer hwGeneration;
    public @Nullable String firmware;
    public @Nullable Boolean batteryLow;
    public @Nullable Instant lastMeasureBle;
    public @Nullable Instant lastMeasureSigfox;
    public @Nullable Instant lastMeasureWifi;
    public @Nullable Instant lastBlueMeasurement;
    public final List<MeasurementValue> measurements = new ArrayList<>();

    public DeviceData(String serial, String poolId, String poolName) {
        this.serial = serial;
        this.poolId = poolId;
        this.poolName = poolName;
    }

    /**
     * The most relevant value for a measurement name. The cloud may return a name twice (device and test strip):
     * not expired wins, then device over strip, then the newest.
     */
    public Optional<MeasurementValue> best(String name) {
        return measurements.stream().filter(m -> name.equals(m.name()))
                .max(Comparator.comparing((MeasurementValue m) -> !m.expired())
                        .thenComparing((MeasurementValue m) -> !m.isStrip())
                        .thenComparing((MeasurementValue m) -> timestampOrEpoch(m.timestamp())));
    }

    /** Newest of all known "last measurement" timestamps. */
    public @Nullable Instant lastMeasurement() {
        Instant result = lastBlueMeasurement;
        for (Instant candidate : new @Nullable Instant[] { lastMeasureBle, lastMeasureSigfox, lastMeasureWifi }) {
            if (candidate != null && (result == null || candidate.isAfter(result))) {
                result = candidate;
            }
        }
        return result;
    }

    public Map<String, String> properties() {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("serial", serial);
        p.put("poolId", poolId);
        p.put("poolName", poolName);
        putIfNotNull(p, "productName", productName);
        putIfNotNull(p, "productType", productType);
        Integer generation = hwGeneration;
        putIfNotNull(p, "hwGeneration", generation == null ? null : generation.toString());
        putIfNotNull(p, "firmwareVersion", firmware);
        return p;
    }

    private static Instant timestampOrEpoch(@Nullable Instant instant) {
        return instant == null ? Instant.EPOCH : instant;
    }

    private static void putIfNotNull(Map<String, String> map, String key, @Nullable String value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
