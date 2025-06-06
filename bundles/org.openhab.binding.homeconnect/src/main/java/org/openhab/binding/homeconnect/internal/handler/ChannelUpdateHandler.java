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
package org.openhab.binding.homeconnect.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.homeconnect.internal.client.exception.ApplianceOfflineException;
import org.openhab.binding.homeconnect.internal.client.exception.AuthorizationException;
import org.openhab.binding.homeconnect.internal.client.exception.CommunicationException;
import org.openhab.binding.homeconnect.internal.handler.cache.ExpiringStateMap;
import org.openhab.core.thing.ChannelUID;

/**
 * The {@link ChannelUpdateHandler} is responsible for updating channels.
 *
 * @author Jonas Brüstel - Initial contribution
 */
@NonNullByDefault
public interface ChannelUpdateHandler {
    void handle(ChannelUID channelUID, ExpiringStateMap cache)
            throws CommunicationException, ApplianceOfflineException, AuthorizationException;
}
