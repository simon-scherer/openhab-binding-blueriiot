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
 * Response of {@code GET swimming_pool/{poolId}/blue}.
 *
 * @author Simon Scherer - Initial contribution
 */
@NonNullByDefault
public class PoolDevicesResponse {

    public @Nullable List<Entry> data;

    /** A Blue Connect device assigned to the pool. */
    public static class Entry {
        @SerializedName("blue_device_serial")
        public @Nullable String blueDeviceSerial;
    }
}
