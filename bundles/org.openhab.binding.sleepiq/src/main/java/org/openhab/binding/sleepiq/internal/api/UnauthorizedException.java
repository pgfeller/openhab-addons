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
package org.openhab.binding.sleepiq.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link UnauthorizedException} is thrown when a login to the sleepiq service
 * is unauthorized to use the API.
 *
 * @author Mark Hilbush - Initial contribution
 */
@NonNullByDefault
public class UnauthorizedException extends LoginException {
    private static final long serialVersionUID = 1L;

    public UnauthorizedException(String message) {
        super(message);
    }
}
