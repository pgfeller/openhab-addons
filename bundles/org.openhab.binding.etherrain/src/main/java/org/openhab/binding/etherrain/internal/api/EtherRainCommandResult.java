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
package org.openhab.binding.etherrain.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link EtherRainCommandResult} is the response packet for Command Result
 *
 * @author Joe Inkenbrandt - Initial contribution
 */

@NonNullByDefault
public enum EtherRainCommandResult {
    OK("OK"),
    RN("RN"),
    SH("SH"),
    NC("NC");

    private final String result;

    EtherRainCommandResult(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }
}
