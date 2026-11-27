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
package org.openhab.automation.jsscriptinglsp.internal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeResponse;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.module.script.ScriptEngineManager;

import jakarta.ws.rs.core.SecurityContext;

/**
 * @author Patrik Gfeller - Initial contribution
 */
class JavaScriptLanguageServerAdapterTest {
    @Test
    void disablingServerClosesPendingConnections() {
        ScriptEngineManager scriptEngineManager = mock(ScriptEngineManager.class);
        JavaScriptLanguageServerAdapter adapter = new JavaScriptLanguageServerAdapter(scriptEngineManager,
                Map.of("enabled", true));
        Object socket = adapter.createWebSocket(mock(JettyServerUpgradeRequest.class),
                mock(JettyServerUpgradeResponse.class), mock(SecurityContext.class));
        assertNotNull(socket);

        adapter.modified(Map.of("enabled", false));

        Session session = mock(Session.class);
        ((JavaScriptLspWebSocket) socket).onOpen(session);

        verify(session).close();
    }
}
