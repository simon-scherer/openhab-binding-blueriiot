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
package org.openhab.binding.blueriiot.internal.handler;

import static org.openhab.binding.blueriiot.internal.BlueRiiotBindingConstants.*;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.blueriiot.internal.api.BlueRiiotException;
import org.openhab.binding.blueriiot.internal.config.BlueConnectConfiguration;
import org.openhab.binding.blueriiot.internal.model.AccountData;
import org.openhab.binding.blueriiot.internal.model.DeviceData;
import org.openhab.binding.blueriiot.internal.model.MeasurementValue;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.MetricPrefix;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler of one Blue Connect device. Gets its data from the account bridge and polls nothing itself.
 *
 * @author Simon Scherer - Initial contribution
 */
@NonNullByDefault
public class BlueConnectHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(BlueConnectHandler.class);

    private String serial = "";
    private @Nullable DeviceData current;
    private @Nullable ScheduledFuture<?> fetchJob;

    public BlueConnectHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        BlueConnectConfiguration config = getConfigAs(BlueConnectConfiguration.class);
        if (config.serial.isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "@text/offline.conf-error.serial-missing");
            return;
        }
        serial = config.serial.trim();
        scheduleFetch(config.fetchInterval);
        Bridge bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "@text/offline.conf-error.bridge-missing");
            return;
        }
        if (bridge.getStatus() != ThingStatus.ONLINE) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            return;
        }
        updateStatus(ThingStatus.UNKNOWN);
        BlueRiiotAccountHandler account = accountHandler();
        AccountData data = account == null ? null : account.getLastData();
        if (data != null) {
            updateFromAccount(data);
        }
    }

    @Override
    public void dispose() {
        cancelFetchJob();
        super.dispose();
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE) {
            initialize();
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (CHANNEL_FETCH_MEASUREMENT.equals(channelUID.getId())) {
            if (command == OnOffType.ON) {
                scheduler.execute(this::fetchMeasurement);
            } else if (command instanceof RefreshType) {
                updateState(CHANNEL_FETCH_MEASUREMENT, OnOffType.OFF);
            }
            return;
        }
        if (command instanceof RefreshType) {
            DeviceData data = current;
            if (data != null) {
                publish(data);
            }
        }
    }

    void updateFromAccount(AccountData data) {
        if (serial.isEmpty()) {
            return;
        }
        DeviceData device = data.device(serial);
        if (device == null) {
            current = null;
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "@text/offline.conf-error.device-not-found [\"" + serial + "\"]");
            return;
        }
        current = device;
        updateStatus(ThingStatus.ONLINE);
        updateProperties(device.properties());
        publish(device);
    }

    private @Nullable BlueRiiotAccountHandler accountHandler() {
        Bridge bridge = getBridge();
        return bridge != null && bridge.getHandler() instanceof BlueRiiotAccountHandler handler ? handler : null;
    }

    private synchronized void scheduleFetch(int hours) {
        cancelFetchJob();
        if (hours > 0) {
            fetchJob = scheduler.scheduleWithFixedDelay(this::fetchMeasurement, hours, hours, TimeUnit.HOURS);
        }
    }

    private synchronized void cancelFetchJob() {
        ScheduledFuture<?> job = fetchJob;
        if (job != null) {
            job.cancel(false);
            fetchJob = null;
        }
    }

    private void fetchMeasurement() {
        BlueRiiotAccountHandler account = accountHandler();
        try {
            if (account != null) {
                account.fetchMeasurement(serial);
            }
        } catch (BlueRiiotException e) {
            logger.warn("Blue Connect {}: fetching the newest measurement failed: {}", serial, e.getMessage());
        } finally {
            updateState(CHANNEL_FETCH_MEASUREMENT, OnOffType.OFF);
        }
    }

    private void publish(DeviceData device) {
        for (MeasurementChannel mc : MEASUREMENT_CHANNELS) {
            Optional<MeasurementValue> best = device.best(mc.apiName());
            MeasurementValue m = best.isPresent() ? best.get() : null;
            updateState(mc.channelId(), m == null ? UnDefType.UNDEF : toState(m.value(), mc.unit()));
            String statusChannel = mc.statusChannelId();
            if (statusChannel != null) {
                String status = m == null ? null : m.status();
                updateState(statusChannel, status == null ? UnDefType.UNDEF : new StringType(status));
            }
        }
        Instant last = device.lastMeasurement();
        updateState(CHANNEL_LAST_MEASUREMENT, last == null ? UnDefType.UNDEF : new DateTimeType(last));
        Boolean battery = device.batteryLow;
        updateState(CHANNEL_BATTERY_LOW, battery == null ? UnDefType.UNDEF : OnOffType.from(battery));
        updateState(CHANNEL_FETCH_MEASUREMENT, OnOffType.OFF);
    }

    private static State toState(double value, @Nullable String unit) {
        if ("°C".equals(unit)) {
            return new QuantityType<>(value, SIUnits.CELSIUS);
        }
        if ("mV".equals(unit)) {
            Unit<javax.measure.quantity.ElectricPotential> millivolt = MetricPrefix.MILLI(Units.VOLT);
            return new QuantityType<>(value, millivolt);
        }
        return new DecimalType(value);
    }
}
