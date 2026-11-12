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

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

/**
 * Response of {@code POST user/login}: temporary AWS credentials.
 *
 * @author Simon Scherer - Initial contribution
 */
@NonNullByDefault
public class LoginResponse {

    public @Nullable Credentials credentials;

    /** AWS credentials issued by Cognito. */
    public static class Credentials {
        @SerializedName("access_key")
        public @Nullable String accessKey;
        @SerializedName("secret_key")
        public @Nullable String secretKey;
        @SerializedName("session_token")
        public @Nullable String sessionToken;
        /** ISO timestamp or epoch seconds/milliseconds, depending on the API version. */
        public @Nullable JsonElement expiration;
    }
}
