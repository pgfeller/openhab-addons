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

/**
 * Channel metadata for the user-name channel.
 * <p>
 * Exposes the currently authenticated Jellyfin user's display name as a read-only String channel.
 * The channel state is NULL when no session is active.
 */
public class UserNameChannel {

    /**
     * The channel type UID for the user-name channel.
     * Format: bindingId + ":" + channelId
     */
    public static final java.lang.String CHANNEL_ID = "user-name";

    /**
     * The channel type UID for the user-name channel.
     * Used by openHAB to identify this channel type.
     */
    public static final org.openhab.core.thing.type.ChannelTypeUID CHANNEL_TYPE_UID =
            new org.openhab.core.thing.type.ChannelTypeUID(org.openhab.binding.jellyfin.internal.Constants.BINDING_ID, "user-name");

    /**
     * Private constructor to prevent instantiation.
     * This class only contains channel metadata constants.
     */
    private UserNameChannel() {
    }
}
