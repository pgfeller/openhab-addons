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
package org.openhab.binding.jellyfin.channel;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.jellyfin.internal.Constants;
import org.openhab.core.thing.type.ChannelTypeUID;

/**
 * Channel metadata for the user-id channel.
 * <p>
 * Exposes the current client's session user's UUID as a read-only String channel. It reports NULL when the session
 * or user ID is unavailable.
 *
 * @author Patrik Gfeller - Initial contribution - Session user exposure
 */
@NonNullByDefault
public class UserIdChannel {

    /**
     * The channel ID for the user-id channel.
     */
    public static final String CHANNEL_ID = "user-id";

    /**
     * The channel type UID declared by the client Thing type metadata.
     */
    public static final ChannelTypeUID CHANNEL_TYPE_UID = new ChannelTypeUID(Constants.BINDING_ID, "user-id-channel");

    /**
     * Private constructor to prevent instantiation.
     * This class only contains channel metadata constants.
     */
    private UserIdChannel() {
    }
}
