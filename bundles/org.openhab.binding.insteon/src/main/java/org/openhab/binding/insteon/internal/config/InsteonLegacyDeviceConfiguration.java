/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.binding.insteon.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link InsteonLegacyDeviceConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Rob Nielsen - Initial contribution
 */
@NonNullByDefault
public class InsteonLegacyDeviceConfiguration {

    private String address = "";
    private String productKey = "";
    private @Nullable String deviceConfig;

    public String getAddress() {
        return address;
    }

    public String getProductKey() {
        return productKey;
    }

    public @Nullable String getDeviceConfig() {
        return deviceConfig;
    }
}
