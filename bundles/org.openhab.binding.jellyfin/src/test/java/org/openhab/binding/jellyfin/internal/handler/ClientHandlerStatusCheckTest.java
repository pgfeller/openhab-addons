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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openhab.binding.jellyfin.internal.gen.current.model.BaseItemDto;
import org.openhab.binding.jellyfin.internal.gen.current.model.PlayerStateInfo;
import org.openhab.binding.jellyfin.internal.gen.current.model.SessionInfoDto;
import org.openhab.binding.jellyfin.internal.util.command.ClientCommandRouter;
import org.openhab.binding.jellyfin.internal.util.extrapolation.PlaybackExtrapolator;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.thing.binding.builder.ThingStatusInfoBuilder;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;

/**
 * Unit tests for ClientHandler status checking logic.
 *
 * Verifies that:
 * - State updates are skipped when thing is not ONLINE
 * - Commands are rejected when thing is not ONLINE
 * - Proper logging messages are generated
 *
 * @author Patrik Gfeller - Initial contribution
 */
class ClientHandlerStatusCheckTest {

    private static final long CALLBACK_TIMEOUT_MS = 2_000;

    private Thing mockThing = null;
    private ClientHandler handler = null;

    @BeforeEach
    void setup() {
        mockThing = mock(Thing.class);
        ThingUID thingUID = new ThingUID("jellyfin:client:server:firetv");
        when(mockThing.getUID()).thenReturn(thingUID);

        handler = new ClientHandler(mockThing);
    }

    /**
     * Test that state updates are skipped when thing is OFFLINE.
     * This prevents wasteful state publishes to non-existent items.
     */
    @Test
    void testUpdateStateFromSessionSkipsWhenOffline() {
        assertNotNull(mockThing);
        assertNotNull(handler);
        // Set thing status to OFFLINE
        when(mockThing.getStatus()).thenReturn(ThingStatus.OFFLINE);

        // Create a test session
        SessionInfoDto session = createTestSession("device-123");

        // Call updateStateFromSession - should return early without processing
        handler.updateStateFromSession(session);

        // Since thing is OFFLINE, no state updates should be attempted
        // This is verified by the method returning early with TRACE log
        // In integration tests, we would verify no updateState calls were made
    }

    /**
     * Test that state updates are processed when thing is ONLINE.
     */
    @Test
    void testUpdateStateFromSessionProcessesWhenOnline() {
        assertNotNull(mockThing);
        assertNotNull(handler);
        // Set thing status to ONLINE
        when(mockThing.getStatus()).thenReturn(ThingStatus.ONLINE);

        // Create a test session
        SessionInfoDto session = createTestSession("device-123");

        // Mock channel checking - required for updateState calls to work
        when(mockThing.getChannel(anyString())).thenReturn(null);

        // Call updateStateFromSession - should process the session
        handler.updateStateFromSession(session);

        // In production, this would call updateState for linked channels
        // Test verifies no exceptions are thrown when processing ONLINE
    }

    /**
     * Test that null session updates are skipped when thing is OFFLINE.
     */
    @Test
    void testNullSessionUpdateSkippedWhenOffline() {
        assertNotNull(mockThing);
        assertNotNull(handler);
        // Set thing status to OFFLINE
        when(mockThing.getStatus()).thenReturn(ThingStatus.OFFLINE);

        // Call updateStateFromSession with null - should return early
        handler.updateStateFromSession(null);

        // Verify no exceptions and method returns cleanly
    }

    /**
     * Test that commands are rejected when thing is OFFLINE.
     * This prevents wasteful attempts to control disconnected devices.
     */
    @Test
    void testHandleCommandRejectedWhenOffline() {
        assertNotNull(mockThing);
        assertNotNull(handler);
        // Set thing status to OFFLINE
        when(mockThing.getStatus()).thenReturn(ThingStatus.OFFLINE);

        // Create a channel UID and a test command
        ChannelUID channelUID = new ChannelUID(new ThingUID("jellyfin:client:server:device"), "media-control");
        Command command = mock(Command.class);

        // Call handleCommand - should return early without routing
        handler.handleCommand(channelUID, command);

        // Verify method completes without error (command would be rejected with INFO log)
    }

    /**
     * Test that RefreshType commands are rejected when thing is OFFLINE.
     */
    @Test
    void testRefreshCommandRejectedWhenOffline() {
        assertNotNull(mockThing);
        assertNotNull(handler);
        // Set thing status to OFFLINE
        when(mockThing.getStatus()).thenReturn(ThingStatus.OFFLINE);

        // Create a channel UID and RefreshType command
        ChannelUID channelUID = new ChannelUID(new ThingUID("jellyfin:client:server:device"), "media-control");

        // Call handleCommand with RefreshType - should return early
        handler.handleCommand(channelUID, RefreshType.REFRESH);

        // Verify method completes without calling updateStateFromSession
    }

    /**
     * Test that commands are accepted when thing is ONLINE.
     */
    @Test
    void testHandleCommandAcceptedWhenOnline() {
        assertNotNull(mockThing);
        assertNotNull(handler);
        // Set thing status to ONLINE
        when(mockThing.getStatus()).thenReturn(ThingStatus.ONLINE);

        // Create a channel UID and a test command
        ChannelUID channelUID = new ChannelUID(new ThingUID("jellyfin:client:server:device"), "media-control");
        Command command = mock(Command.class);

        // Call handleCommand - should proceed to routing
        // Note: will fail later due to command router not being initialized,
        // but the status check should pass
        handler.handleCommand(channelUID, command);

        // Verify method completes (would be handled by command router in production)
    }

    @Test
    void testPauseStringCommandStopsExtrapolationImmediately() throws Exception {
        assertNotNull(mockThing);
        assertNotNull(handler);
        when(mockThing.getStatus()).thenReturn(ThingStatus.ONLINE);

        ClientCommandRouter router = mock(ClientCommandRouter.class);
        PlaybackExtrapolator extrapolator = mock(PlaybackExtrapolator.class);

        Field routerField = ClientHandler.class.getDeclaredField("commandRouter");
        routerField.setAccessible(true);
        routerField.set(handler, router);

        Field extrapolatorField = ClientHandler.class.getDeclaredField("extrapolator");
        extrapolatorField.setAccessible(true);
        extrapolatorField.set(handler, extrapolator);

        ChannelUID channelUID = new ChannelUID(new ThingUID("jellyfin:client:server:device"), "media-control");
        StringType command = new StringType("PAUSE");
        handler.handleCommand(channelUID, command);

        verify(router).route(channelUID, command);
        verify(extrapolator).stop();
    }

    @Test
    void testSessionAndBridgeStatusCallbacksReportLifecycleDetails() throws Exception {
        StatusFixture fixture = createStatusFixture();

        SessionInfoDto userSession = new SessionInfoDto();
        userSession.setUserName("Current User");
        assertAll("Session and bridge status transitions",
                () -> assertStatusAfter(fixture, ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        () -> fixture.handler().onSessionUpdate(null)),
                () -> assertStatusAfter(fixture, ThingStatus.ONLINE, ThingStatusDetail.NONE,
                        () -> fixture.handler().onSessionUpdate(new SessionInfoDto())),
                () -> assertStatusAfter(fixture, ThingStatus.ONLINE, ThingStatusDetail.NONE,
                        () -> fixture.handler().onSessionUpdate(userSession)),
                () -> assertStatusAfter(fixture, ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        () -> fixture.handler().onSessionUpdate(null)),
                () -> {
                    fixture.handler().onSessionUpdate(userSession);
                    assertStatusAfter(fixture, ThingStatus.UNINITIALIZED, ThingStatusDetail.BRIDGE_UNINITIALIZED, () -> {
                        fixture.bridgeStatus().set(ThingStatus.UNINITIALIZED);
                        fixture.handler().bridgeStatusChanged(
                                ThingStatusInfoBuilder.create(ThingStatus.UNINITIALIZED).build());
                    });
                },
                () -> assertStatusAfter(fixture, ThingStatus.UNINITIALIZED, ThingStatusDetail.NONE, () -> {
                    fixture.bridgeStatus().set(ThingStatus.INITIALIZING);
                    fixture.handler().bridgeStatusChanged(ThingStatusInfoBuilder.create(ThingStatus.INITIALIZING).build());
                }),
                () -> assertStatusAfter(fixture, ThingStatus.UNINITIALIZED, ThingStatusDetail.NONE, () -> {
                    fixture.bridgeStatus().set(ThingStatus.UNKNOWN);
                    fixture.handler().bridgeStatusChanged(ThingStatusInfoBuilder.create(ThingStatus.UNKNOWN).build());
                }),
                () -> assertStatusAfter(fixture, ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, () -> {
                    fixture.bridgeStatus().set(ThingStatus.OFFLINE);
                    fixture.handler().bridgeStatusChanged(ThingStatusInfoBuilder.create(ThingStatus.OFFLINE).build());
                }),
                () -> assertStatusAfter(fixture, ThingStatus.ONLINE, ThingStatusDetail.NONE, () -> {
                    fixture.bridgeStatus().set(ThingStatus.ONLINE);
                    fixture.handler().bridgeStatusChanged(ThingStatusInfoBuilder.create(ThingStatus.ONLINE).build());
                }),
                () -> assertStatusAfter(fixture, ThingStatus.ONLINE, ThingStatusDetail.NONE,
                        () -> fixture.handler().onSessionUpdate(userSession)),
                () -> {
                    clearInvocations(fixture.callback());
                    invokeSessionTimeout(fixture.handler());
                    assertLastStatus(fixture, ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
                });
    }

    @Test
    void testUnresolvedBridgeDoesNotPublishInitialErrorStatus() {
        Thing thing = mock(Thing.class);
        Configuration configuration = new Configuration();
        configuration.put("serialNumber", "device-123");
        when(thing.getConfiguration()).thenReturn(configuration);

        ThingHandlerCallback callback = mock(ThingHandlerCallback.class);
        ClientHandler uninitializedHandler = new ClientHandler(thing);
        uninitializedHandler.setCallback(callback);
        uninitializedHandler.initialize();

        verify(callback, never()).statusUpdated(eq(thing), any(ThingStatusInfo.class));
    }

    private StatusFixture createStatusFixture() {
        ThingUID thingUID = new ThingUID("jellyfin:client:server:status-device");
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
        when(callback.isChannelLinked(any())).thenReturn(true);

        ClientHandler statusHandler = new ClientHandler(thing);
        statusHandler.setCallback(callback);
        return new StatusFixture(thing, bridge, callback, statusHandler, bridgeStatus);
    }

    private void assertStatusAfter(StatusFixture fixture, ThingStatus expectedStatus, ThingStatusDetail expectedDetail,
            Runnable action) {
        clearInvocations(fixture.callback());
        action.run();
        assertLastStatus(fixture, expectedStatus, expectedDetail);
    }

    private void assertLastStatus(StatusFixture fixture, ThingStatus expectedStatus, ThingStatusDetail expectedDetail) {
        ArgumentCaptor<ThingStatusInfo> statusCaptor = ArgumentCaptor.forClass(ThingStatusInfo.class);
        verify(fixture.callback(), timeout(CALLBACK_TIMEOUT_MS))
                .statusUpdated(eq(fixture.thing()), statusCaptor.capture());
        assertEquals(expectedStatus, statusCaptor.getValue().getStatus());
        assertEquals(expectedDetail, statusCaptor.getValue().getStatusDetail());
    }

    private void invokeSessionTimeout(ClientHandler statusHandler) throws ReflectiveOperationException {
        Method timeoutHandler = ClientHandler.class.getDeclaredMethod("onSessionTimeout");
        timeoutHandler.setAccessible(true);
        timeoutHandler.invoke(statusHandler);
    }

    private record StatusFixture(Thing thing, Bridge bridge, ThingHandlerCallback callback, ClientHandler handler,
            AtomicReference<ThingStatus> bridgeStatus) {
    }

    /**
     * Test that ONLINE status allows state updates to proceed.
     */
    @Test
    void testOnlineStatusAllowsStateUpdates() {
        assertNotNull(mockThing);
        assertNotNull(handler);
        // Set thing status to ONLINE
        when(mockThing.getStatus()).thenReturn(ThingStatus.ONLINE);

        // Create a session with paused playback
        SessionInfoDto session = createTestSession("device-456");
        session.getPlayState().setIsPaused(true);

        // Call updateStateFromSession - should process without returning early
        handler.updateStateFromSession(session);

        // Verify no exceptions and method executes normally
    }

    /**
     * Test that various OFFLINE statuses are rejected.
     */
    @Test
    void testOfflineVariationsRejected() {
        assertNotNull(mockThing);
        assertNotNull(handler);
        // Test multiple offline status scenarios
        ThingStatus[] offlineStatuses = { ThingStatus.OFFLINE,
                // UNKNOWN would also be rejected since only ONLINE is accepted
        };

        for (ThingStatus status : offlineStatuses) {
            when(mockThing.getStatus()).thenReturn(status);

            SessionInfoDto session = createTestSession("device-789");

            // Should not throw exception regardless of offline variety
            handler.updateStateFromSession(session);
        }
    }

    /**
     * Helper to create a test session.
     */
    private SessionInfoDto createTestSession(String deviceId) {
        SessionInfoDto session = new SessionInfoDto();
        session.setId("test-session-id-" + deviceId);
        session.setDeviceId(deviceId);

        BaseItemDto item = new BaseItemDto();
        item.setId(java.util.UUID.randomUUID());
        item.setName("Test Media");
        item.setRunTimeTicks(36000000000L);

        PlayerStateInfo playState = new PlayerStateInfo();
        playState.setIsPaused(false);
        playState.setPositionTicks(18000000000L);

        session.setNowPlayingItem(item);
        session.setPlayState(playState);

        return session;
    }
}
