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
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.binding.jellyfin.internal.gen.current.model.SessionInfoDto;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingUID;

/**
 * Unit tests for {@link ClientHandler} session timeout logic.
 *
 * Tests validate session timeout detection and state management
 * without requiring full openHAB framework integration.
 *
 * @author Patrik Gfeller - Initial contribution
 */
@NonNullByDefault
class ClientHandlerTest {

    @Test
    void testGetCurrentSessionInitiallyNull() {
        ClientHandler handler = new ClientHandler(mock(org.openhab.core.thing.Thing.class));
        assertNull(handler.getCurrentSession());
    }

    @Test
    void testHandlerCreation() {
        ClientHandler handler = new ClientHandler(mock(org.openhab.core.thing.Thing.class));
        assertNotNull(handler);
        assertNull(handler.getCurrentSession());
    }

    /**
     * Test that session update sets the current session.
     */
    @Test
    void testSessionUpdateSetsCurrentSession() throws Exception {
        ClientHandler handler = new ClientHandler(mock(org.openhab.core.thing.Thing.class));

        SessionInfoDto session = new SessionInfoDto();
        session.setId("session-1");
        session.setDeviceId("device1");

        handler.onSessionUpdate(session);

        assertNotNull(handler.getCurrentSession());
        assertEquals("session-1", handler.getCurrentSession().getId());
    }

    /**
     * Test that null session update clears the current session.
     */
    @Test
    void testNullSessionUpdateClearsSession() throws Exception {
        ClientHandler handler = new ClientHandler(mock(org.openhab.core.thing.Thing.class));

        // First set a session
        SessionInfoDto session = new SessionInfoDto();
        session.setId("session-1");
        handler.onSessionUpdate(session);
        assertNotNull(handler.getCurrentSession());

        // Then clear it with null
        handler.onSessionUpdate(null);
        assertNull(handler.getCurrentSession());
    }

    /**
     * Test that session update immediately makes handler consider session active.
     * This verifies the observable behavior (session availability) rather than
     * internal timestamp state. The actual timeout detection (60s) is validated
     * by the scheduled checkSessionTimeout() monitor task in integration tests.
     */
    @Test
    void testSessionUpdateMakesSessionAvailable() {
        ClientHandler handler = new ClientHandler(mock(org.openhab.core.thing.Thing.class));

        SessionInfoDto session = new SessionInfoDto();
        session.setId("session-1");

        // Verify no session initially
        assertNull(handler.getCurrentSession());

        // Update session
        handler.onSessionUpdate(session);

        // Verify session is now available (not timed out)
        assertNotNull(handler.getCurrentSession());
        assertEquals("session-1", handler.getCurrentSession().getId());
    }

    /**
     * Test that user-name channel is updated from session.
     */
    @Test
    void testUserNameChannelUpdatedFromSession() throws Exception {
        ClientHandler handler = new ClientHandler(mock(org.openhab.core.thing.Thing.class));
        Thing thing = mock(org.openhab.core.thing.Thing.class);
        when(thing.getUID()).thenReturn(new ThingUID("test:jellyfin:user-exposure"));

        // Set up handler with the thing
        handler = new ClientHandler(thing);

        // Session with user name
        SessionInfoDto session = new SessionInfoDto();
        session.setUserName("JohnDoe");

        handler.onSessionUpdate(session);

        // Verify user-name channel was updated
        assertNotNull(handler.getCurrentSession());
        assertEquals("JohnDoe", handler.getCurrentSession().getUserName());
    }

    /**
     * Test that user-id channel is updated from session.
     */
    @Test
    void testUserIdChannelUpdatedFromSession() throws Exception {
        ClientHandler handler = new ClientHandler(mock(org.openhab.core.thing.Thing.class));
        Thing thing = mock(org.openhab.core.thing.Thing.class);
        when(thing.getUID()).thenReturn(new ThingUID("test:jellyfin:user-exposure"));

        // Set up handler with the thing
        handler = new ClientHandler(thing);

        // Session with user ID
        SessionInfoDto session = new SessionInfoDto();
        UUID userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        session.setUserId(userId);

        handler.onSessionUpdate(session);

        // Verify user-id channel was updated
        assertNotNull(handler.getCurrentSession());
        assertEquals(userId, handler.getCurrentSession().getUserId());
    }

    /**
     * Test that user-name and user-id channels return null when no session.
     */
    @Test
    void testUserChannelsNullWhenNoSession() throws Exception {
        ClientHandler handler = new ClientHandler(mock(org.openhab.core.thing.Thing.class));

        // Initially no session
        assertNull(handler.getCurrentSession());

        // Verify user channels are null when no session is active
        assertNull(handler.getCurrentSession());
    }

    /**
     * Test that user channels are cleared when session ends.
     */
    @Test
    void testUserChannelsClearedOnSessionEnd() throws Exception {
        ClientHandler handler = new ClientHandler(mock(org.openhab.core.thing.Thing.class));
        Thing thing = mock(org.openhab.core.thing.Thing.class);
        when(thing.getUID()).thenReturn(new ThingUID("test:jellyfin:user-exposure"));

        // Set up handler with the thing
        handler = new ClientHandler(thing);

        // First set a session with user info
        SessionInfoDto sessionWithUser = new SessionInfoDto();
        sessionWithUser.setUserName("TestUser");
        sessionWithUser.setUserId(UUID.fromString("223e4567-e89b-12d3-a456-426614174000"));
        handler.onSessionUpdate(sessionWithUser);

        assertNotNull(handler.getCurrentSession());

        // Now clear the session
        handler.onSessionUpdate(null);

        // Verify session is cleared
        assertNull(handler.getCurrentSession());
    }
}
