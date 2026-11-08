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
package org.openhab.binding.jellyfin.internal.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.openhab.binding.jellyfin.internal.events.SessionEventBus;
import org.openhab.binding.jellyfin.internal.gen.current.model.BaseItemDto;
import org.openhab.binding.jellyfin.internal.gen.current.model.PlayerStateInfo;
import org.openhab.binding.jellyfin.internal.gen.current.model.SessionInfoDto;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.thing.binding.builder.ThingStatusInfoBuilder;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 * Integration tests for ClientHandler event bus integration.
 * Verifies that SessionEventListener is properly implemented.
 *
 * Note: Full framework integration tests (with Thing/Bridge setup) are complex
 * and require extensive mocking. These tests verify the core event listener logic.
 *
 * @author Patrik Gfeller - Initial contribution
 */
@NonNullByDefault
class ClientHandlerEventIntegrationTest {

    private static final long CALLBACK_TIMEOUT_MS = 2_000;

    @Test
    void testClientHandlerImplementsSessionEventListener() {
        // Verify ClientHandler implements the SessionEventListener interface
        ClientHandler handler = new ClientHandler(org.mockito.Mockito.mock(org.openhab.core.thing.Thing.class));
        assertNotNull(handler, "ClientHandler should be created");

        // Verify handler is an instance of SessionEventListener
        org.openhab.binding.jellyfin.internal.events.SessionEventListener listener = handler;
        assertNotNull(listener, "ClientHandler should implement SessionEventListener");
    }

    @Test
    void testOnSessionUpdateWithNullSession() {
        ClientHandler handler = new ClientHandler(org.mockito.Mockito.mock(org.openhab.core.thing.Thing.class));

        // Call onSessionUpdate with null - should not throw exception
        handler.onSessionUpdate(null);

        // Verify current session is cleared
        assertNull(handler.getCurrentSession(), "Current session should be null after null update");
    }

    @Test
    void testOnSessionUpdateWithValidSession() {
        ClientHandler handler = new ClientHandler(org.mockito.Mockito.mock(org.openhab.core.thing.Thing.class));

        // Create test session
        SessionInfoDto session = createTestSession("test-device-123");

        // Call onSessionUpdate - should not throw exception
        handler.onSessionUpdate(session);

        // Verify current session is set
        assertNotNull(handler.getCurrentSession(), "Current session should be set after update");
        assertEquals("test-session-id", handler.getCurrentSession().getId(), "Session ID should match");
    }

    @Test
    void testEventBusSubscriptionAndPublication() {
        String deviceId = "test-device-456";
        SessionEventBus eventBus = new SessionEventBus();

        // Create handler and subscribe manually (simulating what initialize() does)
        ClientHandler handler = new ClientHandler(org.mockito.Mockito.mock(org.openhab.core.thing.Thing.class));
        eventBus.subscribe(deviceId, handler);

        // Verify subscription
        assertEquals(1, eventBus.getListenerCount(deviceId), "Event bus should have 1 listener");

        // Publish session update
        SessionInfoDto session = createTestSession(deviceId);
        eventBus.publishSessionUpdate(deviceId, session);

        // Verify session was received (current session should be updated)
        assertNotNull(handler.getCurrentSession(), "Handler should receive session update");
        assertEquals("test-session-id", handler.getCurrentSession().getId(), "Session ID should match");

        // Unsubscribe (simulating what dispose() does)
        eventBus.unsubscribe(deviceId, handler);
        assertEquals(0, eventBus.getListenerCount(deviceId), "Event bus should have no listeners after unsubscribe");
    }

    @Test
    void testMultipleSessionUpdates() {
        String deviceId = "test-device-789";
        SessionEventBus eventBus = new SessionEventBus();

        ClientHandler handler = new ClientHandler(org.mockito.Mockito.mock(org.openhab.core.thing.Thing.class));
        eventBus.subscribe(deviceId, handler);

        // First update
        SessionInfoDto session1 = createTestSession(deviceId);
        session1.getNowPlayingItem().setName("Movie 1");
        eventBus.publishSessionUpdate(deviceId, session1);
        assertEquals("Movie 1", handler.getCurrentSession().getNowPlayingItem().getName(),
                "First movie name should be set");

        // Second update
        SessionInfoDto session2 = createTestSession(deviceId);
        session2.getNowPlayingItem().setName("Movie 2");
        eventBus.publishSessionUpdate(deviceId, session2);
        assertEquals("Movie 2", handler.getCurrentSession().getNowPlayingItem().getName(),
                "Second movie name should be set");

        eventBus.unsubscribe(deviceId, handler);
    }

    @Test
    void testOfflineNotification() {
        String deviceId = "test-device-offline";
        SessionEventBus eventBus = new SessionEventBus();

        ClientHandler handler = new ClientHandler(org.mockito.Mockito.mock(org.openhab.core.thing.Thing.class));
        eventBus.subscribe(deviceId, handler);

        // First, send a session update
        SessionInfoDto session = createTestSession(deviceId);
        eventBus.publishSessionUpdate(deviceId, session);
        assertNotNull(handler.getCurrentSession(), "Session should be set");

        // Now send null session (offline)
        eventBus.publishSessionUpdate(deviceId, null);
        assertNull(handler.getCurrentSession(), "Session should be cleared on offline notification");

        eventBus.unsubscribe(deviceId, handler);
    }

    @Test
    void testIdentityTracksSessionAbsenceSwitchEndTimeoutAndReconnect() throws Exception {
        HandlerFixture fixture = createHandlerFixture();
        SessionEventBus eventBus = new SessionEventBus();
        eventBus.subscribe("device-identity", fixture.handler());

        SessionInfoDto userA = createIdentitySession("User A", "123e4567-e89b-12d3-a456-426614174000");
        SessionInfoDto userB = createIdentitySession("User B", "223e4567-e89b-12d3-a456-426614174000");
        SessionInfoDto reconnectedUser = createIdentitySession("Reconnected", "323e4567-e89b-12d3-a456-426614174000");
        assertAll("Identity transitions",
                () -> assertIdentityPublished(fixture, eventBus, null, UnDefType.NULL, UnDefType.NULL),
                () -> assertIdentityPublished(fixture, eventBus, new SessionInfoDto(), UnDefType.NULL, UnDefType.NULL),
                () -> assertIdentityPublished(fixture, eventBus, userA, new StringType("User A"),
                        new StringType("123e4567-e89b-12d3-a456-426614174000")),
                () -> assertIdentityPublished(fixture, eventBus, userB, new StringType("User B"),
                        new StringType("223e4567-e89b-12d3-a456-426614174000")),
                () -> assertIdentityPublished(fixture, eventBus, null, UnDefType.NULL, UnDefType.NULL),
                () -> assertIdentityPublished(fixture, eventBus, reconnectedUser, new StringType("Reconnected"),
                        new StringType("323e4567-e89b-12d3-a456-426614174000")));

        eventBus.unsubscribe("device-identity", fixture.handler());
    }

    @Test
    void testSessionTimeoutClearsIdentity() throws Exception {
        HandlerFixture fixture = createHandlerFixture();
        SessionEventBus eventBus = new SessionEventBus();
        eventBus.subscribe("device-identity", fixture.handler());
        assertIdentityPublished(fixture, eventBus,
                createIdentitySession("User A", "123e4567-e89b-12d3-a456-426614174000"), new StringType("User A"),
                new StringType("123e4567-e89b-12d3-a456-426614174000"));

        clearInvocations(fixture.callback());
        Method timeoutHandler = ClientHandler.class.getDeclaredMethod("onSessionTimeout");
        timeoutHandler.setAccessible(true);
        timeoutHandler.invoke(fixture.handler());
        verifyIdentity(fixture, UnDefType.NULL, UnDefType.NULL);

        eventBus.unsubscribe("device-identity", fixture.handler());
    }

    @Test
    void testBridgeUninitializedAndOfflineClearIdentity() {
        HandlerFixture fixture = createHandlerFixture();
        SessionEventBus eventBus = new SessionEventBus();
        eventBus.subscribe("device-identity", fixture.handler());

        SessionInfoDto session = createIdentitySession("User A", "123e4567-e89b-12d3-a456-426614174000");
        assertIdentityPublished(fixture, eventBus, session, new StringType("User A"),
                new StringType("123e4567-e89b-12d3-a456-426614174000"));

        assertAll("Bridge unavailable transitions", () -> {
            clearInvocations(fixture.callback());
            fixture.bridgeStatus().set(ThingStatus.UNINITIALIZED);
            fixture.handler().bridgeStatusChanged(ThingStatusInfoBuilder.create(ThingStatus.UNINITIALIZED).build());
            verifyIdentity(fixture, UnDefType.NULL, UnDefType.NULL);
        }, () -> {
            clearInvocations(fixture.callback());
            fixture.bridgeStatus().set(ThingStatus.OFFLINE);
            fixture.handler().bridgeStatusChanged(ThingStatusInfoBuilder.create(ThingStatus.OFFLINE).build());
            verifyIdentity(fixture, UnDefType.NULL, UnDefType.NULL);
        });

        eventBus.unsubscribe("device-identity", fixture.handler());
    }

    @Test
    void testExceptionHandlingInListener() {
        String deviceId = "test-device-exception";
        SessionEventBus eventBus = new SessionEventBus();

        ClientHandler handler = new ClientHandler(org.mockito.Mockito.mock(org.openhab.core.thing.Thing.class));
        eventBus.subscribe(deviceId, handler);

        // Create session with minimal fields (may trigger edge cases)
        SessionInfoDto session = new SessionInfoDto();
        session.setId("test-session");
        session.setDeviceId(deviceId);
        // Deliberately leave NowPlayingItem null

        // This should not throw exception - should be handled gracefully
        eventBus.publishSessionUpdate(deviceId, session);

        // Event bus should still be functional
        assertEquals(1, eventBus.getListenerCount(deviceId), "Event bus should still have listener after exception");

        eventBus.unsubscribe(deviceId, handler);
    }

    private HandlerFixture createHandlerFixture() {
        ThingUID thingUID = new ThingUID("jellyfin:client:server:device-identity");
        ThingUID bridgeUID = new ThingUID("jellyfin:server:server");
        Thing thing = mock(Thing.class);
        when(thing.getUID()).thenReturn(thingUID);
        when(thing.getBridgeUID()).thenReturn(bridgeUID);
        when(thing.getStatus()).thenReturn(ThingStatus.ONLINE);

        AtomicReference<ThingStatus> bridgeStatus = new AtomicReference<>(ThingStatus.ONLINE);
        Bridge bridge = mock(Bridge.class);
        when(bridge.getStatus()).thenAnswer(ignored -> bridgeStatus.get());

        ThingHandlerCallback callback = mock(ThingHandlerCallback.class);
        when(callback.getBridge(bridgeUID)).thenReturn(bridge);
        when(callback.isChannelLinked(org.mockito.ArgumentMatchers.any())).thenReturn(true);

        ClientHandler handler = new ClientHandler(thing);
        handler.setCallback(callback);
        return new HandlerFixture(thingUID, callback, handler, bridgeStatus);
    }

    private void assertIdentityPublished(HandlerFixture fixture, SessionEventBus eventBus,
            @Nullable SessionInfoDto session,
            State expectedName, State expectedId) {
        clearInvocations(fixture.callback());
        eventBus.publishSessionUpdate("device-identity", session);
        verifyIdentity(fixture, expectedName, expectedId);
    }

    private void verifyIdentity(HandlerFixture fixture, State expectedName, State expectedId) {
        verify(fixture.callback(), timeout(CALLBACK_TIMEOUT_MS))
                .stateUpdated(new ChannelUID(fixture.thingUID(), "user-name"), expectedName);
        verify(fixture.callback(), timeout(CALLBACK_TIMEOUT_MS))
                .stateUpdated(new ChannelUID(fixture.thingUID(), "user-id"), expectedId);
    }

    private SessionInfoDto createIdentitySession(String name, String id) {
        SessionInfoDto session = new SessionInfoDto();
        session.setUserName(name);
        session.setUserId(UUID.fromString(id));
        return session;
    }

    private record HandlerFixture(ThingUID thingUID, ThingHandlerCallback callback, ClientHandler handler,
            AtomicReference<ThingStatus> bridgeStatus) {
    }

    /**
     * Helper method to create a test session with playing media.
     */
    private SessionInfoDto createTestSession(String deviceId) {
        SessionInfoDto session = new SessionInfoDto();
        session.setId("test-session-id");
        session.setDeviceId(deviceId);

        BaseItemDto item = new BaseItemDto();
        item.setId(java.util.UUID.randomUUID());
        item.setName("Test Movie");
        item.setRunTimeTicks(36000000000L); // 1 hour

        PlayerStateInfo playState = new PlayerStateInfo();
        playState.setIsPaused(false);
        playState.setPositionTicks(18000000000L); // 30 minutes

        session.setNowPlayingItem(item);
        session.setPlayState(playState);

        return session;
    }
}
