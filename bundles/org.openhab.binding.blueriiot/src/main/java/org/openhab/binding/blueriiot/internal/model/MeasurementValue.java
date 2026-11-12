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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * One measurement of a Blue Connect device, with the ranges supplied by the cloud.
 *
 * @author Simon Scherer - Initial contribution
 */
@NonNullByDefault
public record MeasurementValue(String name, double value, @Nullable Instant timestamp, boolean expired,
        @Nullable Double okMin, @Nullable Double okMax, @Nullable Double warningLow, @Nullable Double warningHigh,
        @Nullable String issuer) {

    public static final String STATUS_OK = "OK";
    public static final String STATUS_WARNING = "WARNING";
    public static final String STATUS_CRITICAL = "CRITICAL";

    public boolean isStrip() {
        return "strip".equals(issuer);
    }

    /** {@link #STATUS_OK}, {@link #STATUS_WARNING} or {@link #STATUS_CRITICAL}; null if the cloud sent no range. */
    public @Nullable String status() {
        Double min = okMin;
        Double max = okMax;
        if (min == null && max == null) {
            return null;
        }
        if ((min == null || value >= min) && (max == null || value <= max)) {
            return STATUS_OK;
        }
        Double low = warningLow;
        Double high = warningHigh;
        return (low == null || value >= low) && (high == null || value <= high) ? STATUS_WARNING : STATUS_CRITICAL;
    }
}
