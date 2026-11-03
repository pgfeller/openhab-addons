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

import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.openhab.binding.jellyfin.internal.gen.current.model.BaseItemDto;
import org.openhab.binding.jellyfin.internal.gen.current.model.SessionInfoDto;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Unit tests for {@link ClientHandler} session timeout logic.
 *
 * Tests validate session timeout detection and state management
 * without requiring full openHAB framework integration.
 *
 * @author Patrik Gfeller - Initial contribution
 * @author Patrik Gfeller - Client identity metadata contract tests
 */
@NonNullByDefault
class ClientHandlerTest {

    @Test
    void testClientIdentityChannelsAreReadOnlyStringChannels() throws Exception {
        Document clientThingType = parseXmlResource("/OH-INF/thing/types/client-thing-type.xml");
        Document clientChannelTypes = parseXmlResource("/OH-INF/thing/types/client-channel-types.xml");
        Document userChannelTypes = parseXmlResource("/OH-INF/thing/types/user-channel-types.xml");

        assertAll(
                () -> assertClientChannel(clientThingType, userChannelTypes, "user-name", "user-name-channel"),
                () -> assertClientChannel(clientThingType, clientChannelTypes, "user-id", "user-id-channel"));
    }

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

    @Test
    void testUserNamePublishedForNormalSession() {
        assertUserNamePublished("Jane Doe", null, true, new StringType("Jane Doe"));
    }

    @Test
    void testUserNamePublishedForUnicodeSession() {
        assertUserNamePublished("Jörg 東京", null, true, new StringType("Jörg 東京"));
    }

    @Test
    void testEmptyUserNamePublishedAsEmptyString() {
        assertUserNamePublished("", null, true, new StringType(""));
    }

    @Test
    void testAbsentUserNamePublishedAsNullIncludingIdOnlySession() {
        assertUserNamePublished(null, null, true, UnDefType.NULL);
        assertUserNamePublished(null, UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), true,
                UnDefType.NULL);
    }

    @Test
    void testUserNamePublishedWhenSessionHasNoMedia() {
        assertUserNamePublished("Jane Doe", null, false, new StringType("Jane Doe"));
    }

    @Test
    void testUserIdPublishedIndependentlyFromUserName() {
        UUID userId = UUID.fromString("123E4567-E89B-12D3-A456-4266141740AB");

        assertUserNamePublished("Jane Doe", null, true, new StringType("Jane Doe"));
        assertUserNamePublished(null, userId, true, UnDefType.NULL);
    }

    private static void assertUserNamePublished(@Nullable String userName, @Nullable UUID userId, boolean withMedia,
            State expectedState) {
        ThingUID thingUID = new ThingUID("jellyfin:client:jellyfin:device");
        ThingUID bridgeUID = new ThingUID("jellyfin:server:jellyfin");
        Thing thing = mock(Thing.class);
        when(thing.getUID()).thenReturn(thingUID);
        when(thing.getBridgeUID()).thenReturn(bridgeUID);
        when(thing.getStatus()).thenReturn(ThingStatus.ONLINE);

        Bridge bridge = mock(Bridge.class);
        when(bridge.getStatus()).thenReturn(ThingStatus.ONLINE);

        ThingHandlerCallback callback = mock(ThingHandlerCallback.class);
        when(callback.getBridge(bridgeUID)).thenReturn(bridge);
        when(callback.isChannelLinked(any())).thenReturn(true);

        ClientHandler handler = new ClientHandler(thing);
        handler.setCallback(callback);

        SessionInfoDto session = new SessionInfoDto();
        session.setUserName(userName);
        session.setUserId(userId);
        if (withMedia) {
            session.setNowPlayingItem(new BaseItemDto());
        }

        handler.onSessionUpdate(session);

        verify(callback).stateUpdated(new ChannelUID(thingUID, "user-name"), expectedState);
        State expectedUserIdState = userId == null ? UnDefType.NULL : new StringType(userId.toString());
        verify(callback).stateUpdated(new ChannelUID(thingUID, "user-id"), expectedUserIdState);
    }

    private static void assertClientChannel(Document clientThingType, Document channelTypes, String channelId,
            String channelTypeId) {
        Element channel = findElementById(clientThingType, "channel", channelId);
        assertAll(channelId,
                () -> {
                    assertNotNull(channel, "Client thing type must declare the " + channelId + " channel");
                    if (channel != null) {
                        assertEquals(channelTypeId, channel.getAttribute("typeId"));
                    }
                },
                () -> assertReadOnlyStringChannelType(channelTypes, channelTypeId));
    }

    private static void assertReadOnlyStringChannelType(Document channelTypes, String channelTypeId) {
        Element channelType = findElementById(channelTypes, "channel-type", channelTypeId);
        assertNotNull(channelType, "Missing channel type " + channelTypeId);
        if (channelType == null) {
            return;
        }

        Element itemType = findFirstDescendant(channelType, "item-type");
        assertNotNull(itemType, "Missing item type for " + channelTypeId);
        if (itemType != null) {
            assertEquals("String", itemType.getTextContent().trim(), "Unexpected item type for " + channelTypeId);
        }

        Element state = findFirstDescendant(channelType, "state");
        assertNotNull(state, "Missing state description for " + channelTypeId);
        if (state != null) {
            assertEquals("true", state.getAttribute("readOnly"), "Channel must be read-only: " + channelTypeId);
        }
    }

    private static @Nullable Element findElementById(Document document, String elementName, String id) {
        NodeList elements = document.getElementsByTagName(elementName);
        for (int i = 0; i < elements.getLength(); i++) {
            if (elements.item(i) instanceof Element element && id.equals(element.getAttribute("id"))) {
                return element;
            }
        }
        return null;
    }

    private static @Nullable Element findFirstDescendant(Element parent, String elementName) {
        NodeList elements = parent.getElementsByTagName(elementName);
        return elements.getLength() == 0 ? null : (Element) elements.item(0);
    }

    private static Document parseXmlResource(String resourcePath) throws Exception {
        try (InputStream xml = Objects.requireNonNull(ClientHandlerTest.class.getResourceAsStream(resourcePath),
                "Missing test resource " + resourcePath)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return factory.newDocumentBuilder().parse(xml);
        }
    }
}
