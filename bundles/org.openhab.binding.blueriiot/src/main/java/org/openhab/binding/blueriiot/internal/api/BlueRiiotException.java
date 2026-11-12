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
package org.openhab.binding.blueriiot.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Error talking to the Blue Riiot cloud. {@link #isAuthError()} distinguishes rejected credentials from transient
 * communication problems.
 *
 * @author Simon Scherer - Initial contribution
 */
@NonNullByDefault
public class BlueRiiotException extends Exception {

    private static final long serialVersionUID = 1L;
    private final boolean authError;

    public BlueRiiotException(String message, boolean authError) {
        super(message);
        this.authError = authError;
    }

    public BlueRiiotException(String message, boolean authError, @Nullable Throwable cause) {
        super(message, cause);
        this.authError = authError;
    }

    public boolean isAuthError() {
        return authError;
    }
}
