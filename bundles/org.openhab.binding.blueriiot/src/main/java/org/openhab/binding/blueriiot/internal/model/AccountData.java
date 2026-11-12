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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Result of one complete poll of the account.
 *
 * @author Simon Scherer - Initial contribution
 */
@NonNullByDefault
public record AccountData(List<Pool> pools, Map<String, DeviceData> devices) {

    /** A pool of the account. */
    public record Pool(String id, String name) {
    }

    public Collection<DeviceData> allDevices() {
        return devices.values();
    }

    /** Finds a device by serial, ignoring case. */
    public @Nullable DeviceData device(String serial) {
        DeviceData exact = devices.get(serial);
        if (exact != null) {
            return exact;
        }
        for (DeviceData d : devices.values()) {
            if (d.serial.equalsIgnoreCase(serial)) {
                return d;
            }
        }
        return null;
    }
}
