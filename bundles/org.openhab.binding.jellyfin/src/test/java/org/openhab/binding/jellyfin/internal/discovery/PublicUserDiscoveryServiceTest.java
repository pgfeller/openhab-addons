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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.binding.jellyfin.internal.Constants;
import org.openhab.binding.jellyfin.internal.api.ApiClientWrapper;
import org.openhab.binding.jellyfin.internal.gen.ApiException;
import org.openhab.binding.jellyfin.internal.gen.current.UserApi;
import org.openhab.binding.jellyfin.internal.gen.current.model.UserDto;
import org.openhab.binding.jellyfin.internal.handler.ServerHandler;
import org.openhab.core.config.discovery.DiscoveryListener;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingUID;

/**
 * Tests public Jellyfin user discovery without a live server.
 *
 * @author Patrik Gfeller - Initial contribution - Jellyfin public user discovery tests
 */
@NonNullByDefault
class PublicUserDiscoveryServiceTest {
    private final ServerHandler serverHandler = mock(ServerHandler.class);
    private final Bridge bridge = mock(Bridge.class);
    private final ApiClientWrapper apiClient = mock(ApiClientWrapper.class);
    private final UserApi userApi = mock(UserApi.class);
    private final DiscoveryListener listener = mock(DiscoveryListener.class);
    private final ThingUID bridgeUID = new ThingUID(Constants.THING_TYPE_SERVER, "test-server");
    private final PublicUserDiscoveryService discoveryService = new PublicUserDiscoveryService(ignored -> userApi);

    @BeforeEach
    void setUp() {
        when(bridge.getUID()).thenReturn(bridgeUID);
        when(bridge.getStatus()).thenReturn(ThingStatus.ONLINE);
        when(serverHandler.getThing()).thenReturn(bridge);
        when(serverHandler.getApiClient()).thenReturn(apiClient);

        discoveryService.setThingHandler(serverHandler);
        discoveryService.addDiscoveryListener(listener);
    }

    @Test
    void testDiscoversMultipleUsersWithBridgeScopedStableIdentity() throws Exception {
        UUID firstUserId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID secondUserId = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
        when(userApi.getPublicUsers()).thenReturn(List.of(user(firstUserId, "Alice"), user(secondUserId, "Bob")));

        discoveryService.startScan();

        var results = org.mockito.ArgumentCaptor.forClass(DiscoveryResult.class);
        verify(listener, timeout(500).times(2)).thingDiscovered(any(DiscoveryService.class), results.capture());
        var labelsByUID = results.getAllValues().stream()
            .collect(Collectors.toMap(DiscoveryResult::getThingUID, DiscoveryResult::getLabel));
        assertEquals(Set.of(Constants.getUserThingUID(bridgeUID, firstUserId),
            Constants.getUserThingUID(bridgeUID, secondUserId)), labelsByUID.keySet());
        assertEquals("Alice", labelsByUID.get(Constants.getUserThingUID(bridgeUID, firstUserId)));
        assertEquals("Bob", labelsByUID.get(Constants.getUserThingUID(bridgeUID, secondUserId)));
        results.getAllValues().forEach(result -> assertEquals(bridgeUID, result.getBridgeUID()));
    }

    @Test
    void testNameChangeRetainsUserIdentity() throws Exception {
        UUID userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        when(userApi.getPublicUsers()).thenReturn(List.of(user(userId, "Old name")), List.of(user(userId, "New name")));

        discoveryService.startScan();
        discoveryService.startScan();

        var results = org.mockito.ArgumentCaptor.forClass(DiscoveryResult.class);
        verify(listener, timeout(500).times(2)).thingDiscovered(any(DiscoveryService.class), results.capture());
        assertEquals(Constants.getUserThingUID(bridgeUID, userId), results.getAllValues().get(0).getThingUID());
        assertEquals(results.getAllValues().get(0).getThingUID(), results.getAllValues().get(1).getThingUID());
        assertEquals(Set.of("Old name", "New name"),
            results.getAllValues().stream().map(DiscoveryResult::getLabel).collect(Collectors.toSet()));
    }

    @Test
    void testSameUserOnDifferentBridgesHasDifferentIdentity() throws Exception {
        UUID userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        ThingUID otherBridgeUID = new ThingUID(Constants.THING_TYPE_SERVER, "other-server");
        Bridge otherBridge = mock(Bridge.class);
        ServerHandler otherHandler = mock(ServerHandler.class);
        when(otherBridge.getUID()).thenReturn(otherBridgeUID);
        when(otherBridge.getStatus()).thenReturn(ThingStatus.ONLINE);
        when(otherHandler.getThing()).thenReturn(otherBridge);
        when(otherHandler.getApiClient()).thenReturn(apiClient);
        when(userApi.getPublicUsers()).thenReturn(List.of(user(userId, "Alice")));
        PublicUserDiscoveryService otherService = new PublicUserDiscoveryService(ignored -> userApi);
        otherService.setThingHandler(otherHandler);
        otherService.addDiscoveryListener(listener);

        discoveryService.startScan();
        otherService.startScan();

        var results = org.mockito.ArgumentCaptor.forClass(DiscoveryResult.class);
        verify(listener, timeout(500).times(2)).thingDiscovered(any(DiscoveryService.class), results.capture());
        List<DiscoveryResult> discovered = results.getAllValues();
        assertNotEquals(discovered.get(0).getThingUID(), discovered.get(1).getThingUID());
        assertTrue(discovered.stream().anyMatch(result -> bridgeUID.equals(result.getBridgeUID())));
        assertTrue(discovered.stream().anyMatch(result -> otherBridgeUID.equals(result.getBridgeUID())));
    }

    @Test
    void testSuccessfulEmptyResponseRemovesPreviouslyDiscoveredInboxResult() throws Exception {
        UUID userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        when(userApi.getPublicUsers()).thenReturn(List.of(user(userId, "Alice")), List.of());

        discoveryService.startScan();
        verify(listener, timeout(500)).thingDiscovered(any(DiscoveryService.class), any(DiscoveryResult.class));
        verify(listener, timeout(500)).removeOlderResults(eq(discoveryService), any(Instant.class),
            eq(Set.of(Constants.THING_TYPE_JELLYFIN_USER)), eq(bridgeUID));
        clearInvocations(listener);
        Instant beforeScan = Instant.now();
        discoveryService.startScan();
        Instant afterScan = Instant.now();

        var cutoff = org.mockito.ArgumentCaptor.forClass(Instant.class);
        verify(listener, timeout(500)).removeOlderResults(eq(discoveryService), cutoff.capture(),
            eq(Set.of(Constants.THING_TYPE_JELLYFIN_USER)), eq(bridgeUID));
        assertTrue(!cutoff.getValue().isBefore(beforeScan));
        assertTrue(!cutoff.getValue().isAfter(afterScan));
    }

    @Test
    void testMalformedResponseRetainsPreviouslyDiscoveredInboxResult() throws Exception {
        UUID userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        when(userApi.getPublicUsers()).thenReturn(List.of(user(userId, "Alice")), List.of(new UserDto()));

        discoveryService.startScan();
        verify(listener, timeout(500)).thingDiscovered(any(DiscoveryService.class), any(DiscoveryResult.class));
        verify(listener, timeout(500)).removeOlderResults(eq(discoveryService), any(Instant.class),
            eq(Set.of(Constants.THING_TYPE_JELLYFIN_USER)), eq(bridgeUID));
        clearInvocations(listener);
        discoveryService.startScan();

        verify(listener, after(200).never()).removeOlderResults(any(DiscoveryService.class), any(Instant.class), any(),
            any(ThingUID.class));
        verify(listener, after(200).never()).thingRemoved(any(DiscoveryService.class), any(ThingUID.class));
    }

    @Test
    void testMixedResponsePublishesValidUsersWithoutRemovingPreviouslyDiscoveredInboxResult() throws Exception {
        UUID previousUserId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID firstUserId = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
        UUID secondUserId = UUID.fromString("323e4567-e89b-12d3-a456-426614174000");
        when(userApi.getPublicUsers()).thenReturn(List.of(user(previousUserId, "Previous user")),
            List.of(user(firstUserId, "Alice"), new UserDto(), user(secondUserId, "Bob")), List.of());

        discoveryService.startScan();
        verify(listener, timeout(500)).thingDiscovered(any(DiscoveryService.class), any(DiscoveryResult.class));
        verify(listener, timeout(500)).removeOlderResults(eq(discoveryService), any(Instant.class),
            eq(Set.of(Constants.THING_TYPE_JELLYFIN_USER)), eq(bridgeUID));
        clearInvocations(listener);
        discoveryService.startScan();

        var results = org.mockito.ArgumentCaptor.forClass(DiscoveryResult.class);
        verify(listener, timeout(500).times(2)).thingDiscovered(eq(discoveryService), results.capture());
        assertEquals(Set.of(Constants.getUserThingUID(bridgeUID, firstUserId),
            Constants.getUserThingUID(bridgeUID, secondUserId)),
            results.getAllValues().stream().map(DiscoveryResult::getThingUID).collect(Collectors.toSet()));
        results.getAllValues().forEach(result -> assertEquals(bridgeUID, result.getBridgeUID()));
        verify(listener, after(200).never()).removeOlderResults(any(DiscoveryService.class), any(Instant.class), any(),
            any(ThingUID.class));
        verify(listener, after(200).never()).thingRemoved(any(DiscoveryService.class), any(ThingUID.class));

        discoveryService.startScan();
        verify(listener, timeout(500)).removeOlderResults(eq(discoveryService), any(Instant.class),
            eq(Set.of(Constants.THING_TYPE_JELLYFIN_USER)), eq(bridgeUID));
    }

    @Test
    void testNullResponseRetainsPreviouslyDiscoveredInboxResult() throws Exception {
        UUID userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        when(userApi.getPublicUsers()).thenReturn(List.of(user(userId, "Alice"))).thenReturn(null);

        discoveryService.startScan();
        verify(listener, timeout(500)).thingDiscovered(any(DiscoveryService.class), any(DiscoveryResult.class));
        verify(listener, timeout(500)).removeOlderResults(eq(discoveryService), any(Instant.class),
            eq(Set.of(Constants.THING_TYPE_JELLYFIN_USER)), eq(bridgeUID));
        clearInvocations(listener);
        discoveryService.startScan();

        verify(listener, after(200).never()).removeOlderResults(any(DiscoveryService.class), any(Instant.class), any(),
            any(ThingUID.class));
        verify(listener, after(200).never()).thingRemoved(any(DiscoveryService.class), any(ThingUID.class));
    }

    @Test
    void testApiFailureRetainsPreviouslyDiscoveredInboxResult() throws Exception {
        UUID userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        when(userApi.getPublicUsers()).thenReturn(List.of(user(userId, "Alice")))
            .thenThrow(new ApiException("request failed"));

        discoveryService.startScan();
        verify(listener, timeout(500)).thingDiscovered(any(DiscoveryService.class), any(DiscoveryResult.class));
        verify(listener, timeout(500)).removeOlderResults(eq(discoveryService), any(Instant.class),
            eq(Set.of(Constants.THING_TYPE_JELLYFIN_USER)), eq(bridgeUID));
        clearInvocations(listener);
        discoveryService.startScan();

        verify(listener, after(200).never()).removeOlderResults(any(DiscoveryService.class), any(Instant.class), any(),
            any(ThingUID.class));
        verify(listener, after(200).never()).thingRemoved(any(DiscoveryService.class), any(ThingUID.class));
    }

    private static UserDto user(UUID userId, String name) {
        UserDto user = new UserDto();
        user.setId(userId);
        user.setName(name);
        return user;
    }
}
