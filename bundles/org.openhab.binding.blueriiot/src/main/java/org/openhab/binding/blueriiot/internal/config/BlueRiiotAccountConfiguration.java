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
package org.openhab.binding.blueriiot.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Configuration of the account bridge.
 *
 * @author Simon Scherer - Initial contribution
 */
@NonNullByDefault
public class BlueRiiotAccountConfiguration {
    public String email = "";
    public String password = "";
    /** Polling interval in minutes. */
    public int refreshInterval = 30;
    /** Language for texts returned by the cloud. */
    public String language = "en";
}
