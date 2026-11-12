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
package org.openhab.binding.blueriiot.internal.api.dto;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Response of {@code GET swimming_pool/{poolId}/blue/{serial}/lastMeasurements}.
 *
 * @author Simon Scherer - Initial contribution
 */
@NonNullByDefault
public class LastMeasurementsResponse {

    @SerializedName("last_blue_measure_timestamp")
    public @Nullable String lastBlueMeasureTimestamp;
    public @Nullable List<Measurement> data;

    /** One measured or manually entered value. */
    public static class Measurement {
        public @Nullable String name;
        public @Nullable Double value;
        public @Nullable String timestamp;
        public @Nullable Boolean expired;
        @SerializedName("ok_min")
        public @Nullable Double okMin;
        @SerializedName("ok_max")
        public @Nullable Double okMax;
        @SerializedName("warning_low")
        public @Nullable Double warningLow;
        @SerializedName("warning_high")
        public @Nullable Double warningHigh;
        public @Nullable String issuer;
    }
}
