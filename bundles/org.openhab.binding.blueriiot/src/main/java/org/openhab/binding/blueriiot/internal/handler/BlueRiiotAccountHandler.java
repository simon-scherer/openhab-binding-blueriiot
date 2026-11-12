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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.blueriiot.internal.api.BlueRiiotApi;
import org.openhab.binding.blueriiot.internal.api.BlueRiiotException;
import org.openhab.binding.blueriiot.internal.config.BlueRiiotAccountConfiguration;
import org.openhab.binding.blueriiot.internal.discovery.BlueRiiotDiscoveryService;
import org.openhab.binding.blueriiot.internal.model.AccountData;
import org.openhab.binding.blueriiot.internal.model.DeviceData;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridge handler for a Blue Riiot account. Polls the cloud and hands the results to the Blue Connect things.
 *
 * @author Simon Scherer - Initial contribution
 */
@NonNullByDefault
public class BlueRiiotAccountHandler extends BaseBridgeHandler {

    private static final int MIN_REFRESH_MINUTES = 5;
    private static final int[] FOLLOW_UP_POLL_MINUTES = { 2, 6 };

    private final Logger logger = LoggerFactory.getLogger(BlueRiiotAccountHandler.class);
    private final HttpClient httpClient;

    private @Nullable BlueRiiotApi api;
    private @Nullable AccountData lastData;
    private @Nullable BlueRiiotDiscoveryService discoveryService;
    private @Nullable ScheduledFuture<?> pollJob;

    public BlueRiiotAccountHandler(Bridge bridge, HttpClient httpClient) {
        super(bridge);
        this.httpClient = httpClient;
    }

    @Override
    public void initialize() {
        BlueRiiotAccountConfiguration config = getConfigAs(BlueRiiotAccountConfiguration.class);
        if (config.email.isBlank() || config.password.isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "@text/offline.conf-error.credentials-missing");
            return;
        }
        api = new BlueRiiotApi(httpClient, config.email.trim(), config.password, config.language);
        int interval = Math.max(MIN_REFRESH_MINUTES, config.refreshInterval);
        updateStatus(ThingStatus.UNKNOWN);
        pollJob = scheduler.scheduleWithFixedDelay(this::poll, 0, interval, TimeUnit.MINUTES);
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> job = pollJob;
        if (job != null) {
            job.cancel(true);
            pollJob = null;
        }
        api = null;
        lastData = null;
        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // the bridge has no channels
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Set.of(BlueRiiotDiscoveryService.class);
    }

    @Override
    public void childHandlerInitialized(ThingHandler childHandler, Thing childThing) {
        AccountData data = lastData;
        if (data != null && childHandler instanceof BlueConnectHandler handler) {
            handler.updateFromAccount(data);
        }
    }

    public @Nullable AccountData getLastData() {
        return lastData;
    }

    public void setDiscoveryService(@Nullable BlueRiiotDiscoveryService service) {
        this.discoveryService = service;
    }

    /** Triggers an immediate poll. */
    public void requestRefresh() {
        scheduler.execute(this::poll);
    }

    /**
     * Asks the cloud to take over the newest measurement stored by the Blue Extender and polls again shortly
     * afterwards, so the values show up without waiting for the regular interval.
     */
    public void fetchMeasurement(String serial) throws BlueRiiotException {
        BlueRiiotApi localApi = api;
        if (localApi == null) {
            throw new BlueRiiotException("Account not initialized", false);
        }
        String message = localApi.releaseLastUnprocessedEvent(serial);
        logger.debug("Blue Connect {}: newest measurement requested from the Blue Extender ({})", serial, message);
        for (int minutes : FOLLOW_UP_POLL_MINUTES) {
            scheduler.schedule(this::poll, minutes, TimeUnit.MINUTES);
        }
    }

    private synchronized void poll() {
        BlueRiiotApi localApi = api;
        if (localApi == null) {
            return;
        }
        try {
            AccountData data = localApi.fetchAll();
            lastData = data;
            updateStatus(ThingStatus.ONLINE);
            updateBridgeProperties(data);
            for (Thing thing : getThing().getThings()) {
                if (thing.getHandler() instanceof BlueConnectHandler handler) {
                    handler.updateFromAccount(data);
                }
            }
            BlueRiiotDiscoveryService discovery = discoveryService;
            if (discovery != null) {
                discovery.publish(data);
            }
        } catch (BlueRiiotException e) {
            if (e.isAuthError()) {
                logger.warn("Blue Riiot login failed: {}", e.getMessage());
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "@text/offline.conf-error.login-failed");
                // stop polling, repeated failed logins could lock the account
                ScheduledFuture<?> job = pollJob;
                if (job != null) {
                    job.cancel(false);
                }
            } else {
                logger.debug("Blue Riiot poll failed: {}", e.getMessage(), e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            }
        }
    }

    private void updateBridgeProperties(AccountData data) {
        StringJoiner devices = new StringJoiner(", ");
        for (DeviceData device : data.allDevices()) {
            devices.add(device.serial + " (" + device.poolName + ")");
        }
        StringJoiner pools = new StringJoiner(", ");
        data.pools().forEach(p -> pools.add(p.name() + " [" + p.id() + "]"));
        Map<String, String> properties = editProperties();
        properties.put("pools", pools.toString());
        properties.put("devices", devices.toString());
        updateProperties(properties);
    }
}
