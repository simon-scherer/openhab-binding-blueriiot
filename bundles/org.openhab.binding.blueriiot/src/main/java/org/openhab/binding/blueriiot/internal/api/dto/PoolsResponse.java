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
 * Response of {@code GET swimming_pool}.
 *
 * @author Simon Scherer - Initial contribution
 */
@NonNullByDefault
public class PoolsResponse {

    public @Nullable List<PoolEntry> data;

    /** One pool the user has access to. */
    public static class PoolEntry {
        @SerializedName("swimming_pool_id")
        public @Nullable String swimmingPoolId;
        public @Nullable String name;
        @SerializedName("swimming_pool")
        public @Nullable PoolDetails swimmingPool;
    }

    /** Nested pool details. */
    public static class PoolDetails {
        public @Nullable String name;
    }
}
