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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Response of {@code GET blue/{serial}}.
 *
 * @author Simon Scherer - Initial contribution
 */
@NonNullByDefault
public class BlueDevice {

    @SerializedName("hw_product_name")
    public @Nullable String hwProductName;
    @SerializedName("hw_product_type")
    public @Nullable String hwProductType;
    @SerializedName("hw_generation")
    public @Nullable Integer hwGeneration;
    @SerializedName("fw_version_psoc")
    public @Nullable String fwVersionPsoc;
    @SerializedName("fw_version")
    public @Nullable String fwVersion;
    @SerializedName("battery_low")
    public @Nullable Boolean batteryLow;
    @SerializedName("last_measure_ble")
    public @Nullable String lastMeasureBle;
    @SerializedName("last_measure_sigfox")
    public @Nullable String lastMeasureSigfox;
    @SerializedName("last_measure_wifi")
    public @Nullable String lastMeasureWifi;
}
