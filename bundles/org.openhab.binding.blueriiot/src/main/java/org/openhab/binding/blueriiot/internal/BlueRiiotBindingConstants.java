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
package org.openhab.binding.blueriiot.internal;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ThingTypeUID;

/**
 * Constants of the Blue Riiot binding.
 *
 * @author Simon Scherer - Initial contribution
 */
@NonNullByDefault
public final class BlueRiiotBindingConstants {

    public static final String BINDING_ID = "blueriiot";

    public static final ThingTypeUID THING_TYPE_ACCOUNT = new ThingTypeUID(BINDING_ID, "account");
    public static final ThingTypeUID THING_TYPE_BLUECONNECT = new ThingTypeUID(BINDING_ID, "blueconnect");
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Set.of(THING_TYPE_ACCOUNT, THING_TYPE_BLUECONNECT);

    public static final String PROPERTY_SERIAL = "serial";

    public static final String CHANNEL_LAST_MEASUREMENT = "last-measurement";
    public static final String CHANNEL_BATTERY_LOW = "battery-low";
    public static final String CHANNEL_FETCH_MEASUREMENT = "fetch-measurement";

    /**
     * Mapping of a measurement name of the API to a channel. Without unit the channel is a plain {@code Number}.
     */
    public record MeasurementChannel(String apiName, String channelId, @Nullable String unit,
            @Nullable String statusChannelId) {
    }

    public static final List<MeasurementChannel> MEASUREMENT_CHANNELS = List.of(
            new MeasurementChannel("temperature", "temperature", "°C", "temperature-status"),
            new MeasurementChannel("ph", "ph", null, "ph-status"),
            new MeasurementChannel("orp", "orp", "mV", "orp-status"),
            new MeasurementChannel("conductivity", "conductivity", null, null),
            new MeasurementChannel("salinity", "salinity", null, null),
            new MeasurementChannel("salt", "salt", null, null), new MeasurementChannel("tds", "tds", null, null),
            new MeasurementChannel("fcl", "free-chlorine", null, "free-chlorine-status"),
            new MeasurementChannel("ccl", "combined-chlorine", null, null),
            new MeasurementChannel("tcl", "total-chlorine", null, null),
            new MeasurementChannel("ta", "alkalinity", null, "alkalinity-status"),
            new MeasurementChannel("cya", "cyanuric-acid", null, null),
            new MeasurementChannel("cal", "calcium-hardness", null, null));

    private BlueRiiotBindingConstants() {
    }
}
