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
package org.openhab.binding.blueriiot.internal.discovery;

import static org.openhab.binding.blueriiot.internal.BlueRiiotBindingConstants.*;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.blueriiot.internal.handler.BlueRiiotAccountHandler;
import org.openhab.binding.blueriiot.internal.model.AccountData;
import org.openhab.binding.blueriiot.internal.model.DeviceData;
import org.openhab.core.config.discovery.AbstractThingHandlerDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * Discovers the Blue Connect devices of an account.
 *
 * @author Simon Scherer - Initial contribution
 */
@NonNullByDefault
@Component(scope = ServiceScope.PROTOTYPE, service = BlueRiiotDiscoveryService.class)
public class BlueRiiotDiscoveryService extends AbstractThingHandlerDiscoveryService<BlueRiiotAccountHandler> {

    private static final int TIMEOUT_SECONDS = 10;

    public BlueRiiotDiscoveryService() {
        super(BlueRiiotAccountHandler.class, Set.of(THING_TYPE_BLUECONNECT), TIMEOUT_SECONDS, true);
    }

    @Override
    public void initialize() {
        thingHandler.setDiscoveryService(this);
        super.initialize();
    }

    @Override
    public void dispose() {
        super.dispose();
        thingHandler.setDiscoveryService(null);
    }

    @Override
    protected void startScan() {
        AccountData data = thingHandler.getLastData();
        if (data != null) {
            publish(data);
        } else {
            thingHandler.requestRefresh();
        }
    }

    /** Called by the account handler after each successful poll. */
    public void publish(AccountData data) {
        ThingUID bridgeUID = thingHandler.getThing().getUID();
        for (DeviceData device : data.allDevices()) {
            ThingUID uid = new ThingUID(THING_TYPE_BLUECONNECT, bridgeUID, device.serial.replaceAll("[^A-Za-z0-9_-]", "_"));
            String product = device.productName;
            thingDiscovered(DiscoveryResultBuilder.create(uid).withBridge(bridgeUID)
                    .withProperty(PROPERTY_SERIAL, device.serial).withRepresentationProperty(PROPERTY_SERIAL)
                    .withLabel((product != null ? product : "Blue Connect") + " " + device.poolName).build());
        }
    }
}
