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
package org.openhab.binding.jellyfin.internal.discovery;

import static org.openhab.binding.jellyfin.internal.Constants.DISCOVERY_RESULT_TTL_SEC;
import static org.openhab.binding.jellyfin.internal.Constants.THING_TYPE_JELLYFIN_USER;
import static org.openhab.binding.jellyfin.internal.Constants.getUserThingUID;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.jellyfin.internal.api.ApiClientWrapper;
import org.openhab.binding.jellyfin.internal.gen.ApiException;
import org.openhab.binding.jellyfin.internal.gen.current.UserApi;
import org.openhab.binding.jellyfin.internal.gen.current.model.UserDto;
import org.openhab.binding.jellyfin.internal.handler.ServerHandler;
import org.openhab.core.config.discovery.AbstractThingHandlerDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovers Jellyfin accounts visible on the server's public login screen.
 *
 * @author Patrik Gfeller - Initial contribution - Jellyfin public user discovery
 */
@Component(scope = ServiceScope.PROTOTYPE, service = PublicUserDiscoveryService.class)
@NonNullByDefault
public class PublicUserDiscoveryService extends AbstractThingHandlerDiscoveryService<ServerHandler> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublicUserDiscoveryService.class);

    private final Function<ApiClientWrapper, UserApi> userApiFactory;

    @Activate
    public PublicUserDiscoveryService() throws IllegalArgumentException {
        this(UserApi::new);
    }

    PublicUserDiscoveryService(Function<ApiClientWrapper, UserApi> userApiFactory) throws IllegalArgumentException {
        super(ServerHandler.class, Set.of(THING_TYPE_JELLYFIN_USER), DISCOVERY_RESULT_TTL_SEC, true);
        this.userApiFactory = userApiFactory;
    }

    @Override
    protected void startScan() {
        if (thingHandler.getThing().getStatus() != ThingStatus.ONLINE) {
            LOGGER.debug("Skipping public-user discovery while Jellyfin bridge {} is offline",
                    thingHandler.getThing().getUID());
            return;
        }

        Instant scanStarted = Instant.now();
        try {
            List<UserDto> users = userApiFactory.apply(thingHandler.getApiClient()).getPublicUsers();
            if (users == null || !publishUsers(users)) {
                return;
            }
            removeOlderResults(scanStarted, thingHandler.getThing().getUID());
        } catch (ApiException e) {
            LOGGER.debug("Public-user discovery failed for Jellyfin bridge {}: {}", thingHandler.getThing().getUID(),
                    e.getMessage());
        }
    }

    private boolean publishUsers(List<UserDto> users) {
        ThingUID bridgeUID = thingHandler.getThing().getUID();
        boolean complete = true;
        for (UserDto user : users) {
            UUID userId = user.getId();
            if (userId == null) {
                LOGGER.debug("Ignoring incomplete public-user response for bridge {}", bridgeUID);
                complete = false;
                continue;
            }
            String name = user.getName();
            String label = name == null || name.isBlank() ? "Jellyfin User" : name;
            thingDiscovered(DiscoveryResultBuilder.create(getUserThingUID(bridgeUID, userId))
                    .withThingType(THING_TYPE_JELLYFIN_USER).withBridge(bridgeUID).withLabel(label)
                    .withRepresentationProperty(Thing.PROPERTY_SERIAL_NUMBER)
                    .withProperty(Thing.PROPERTY_SERIAL_NUMBER, userId.toString()).build());
        }
        return complete;
    }
}
