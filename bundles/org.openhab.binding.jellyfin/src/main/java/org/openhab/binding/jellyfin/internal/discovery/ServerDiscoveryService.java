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
package org.openhab.binding.jellyfin.internal.discovery;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.jellyfin.internal.JellyfinBindingConstants;
import org.openhab.binding.jellyfin.internal.handler.JellyfinServerHandler;
import org.openhab.core.config.discovery.AbstractThingHandlerDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryService;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ServerDiscoveryService} discover Jellyfin servers in the network.
 * 
 * @author Miguel Álvarez - Initial contribution
 * @author Patrik Gfeller - Adjustments to work independently of the Android SDK and respective runtime
 */
@NonNullByDefault
@Component(service = DiscoveryService.class, immediate = true, configurationPid = "discovery.jellyfin")
public class ServerDiscoveryService extends AbstractThingHandlerDiscoveryService<JellyfinServerHandler> {
    private final Logger logger = LoggerFactory.getLogger(ServerDiscoveryService.class);

    public ServerDiscoveryService() throws IllegalArgumentException {
        super(JellyfinServerHandler.class, Set.of(JellyfinBindingConstants.THING_TYPE_SERVER), 60, false);
        logger.info("public ServerDiscoveryService");
    }

    public static void main(String[] args) {
        ServerDiscoveryHelper discoverer = new ServerDiscoveryHelper();

        List<ServerDiscoveryResult> servers = discoverer.discoverServers();

        if (servers.isEmpty()) {
            System.out.println("No Jellyfin servers found.");
        } else {
            System.out.println("\nDiscovered Jellyfin Servers:");
            for (ServerDiscoveryResult server : servers) {
                System.out.println(server);
            }
        }
    }

    @Override
    protected synchronized void startScan() {
        // TODO Auto-generated method stub
        logger.info("ServerDiscoveryService.startScan");

        ServerDiscoveryHelper discoverer = new ServerDiscoveryHelper();

        List<ServerDiscoveryResult> servers = discoverer.discoverServers();

        if (servers.isEmpty()) {
            logger.info("No Jellyfin servers found.");
        } else {
            logger.info("\nDiscovered Jellyfin Servers:");
            for (ServerDiscoveryResult server : servers) {
                logger.info("Server {} @ {}", server.getName(), server.getAddress());
            }
        }
    }
}
