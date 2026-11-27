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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeResponse;
import org.openhab.core.automation.module.script.ScriptEngineManager;
import org.openhab.core.config.core.ConfigurableService;
import org.openhab.core.io.websocket.WebSocketAdapter;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import jakarta.ws.rs.core.SecurityContext;

/**
 * Registers the JavaScript language-server endpoint with openHAB's authenticated WebSocket service.
 *
 * @author Patrik Gfeller - Initial contribution
 */
@NonNullByDefault
@Component(service = WebSocketAdapter.class, configurationPid = JavaScriptLanguageServerAdapter.CONFIGURATION_PID, //
        property = Constants.SERVICE_PID + "=" + JavaScriptLanguageServerAdapter.CONFIGURATION_PID)
@ConfigurableService(category = "automation", label = "JavaScript Language Server", description_uri = "automation:jsscriptinglsp")
public class JavaScriptLanguageServerAdapter implements WebSocketAdapter {
    public static final String ADAPTER_ID = "jsscriptinglsp";
    public static final String CONFIGURATION_PID = "org.openhab.lsp.javascript";

    private final ScriptEngineManager scriptEngineManager;
    private final Set<JavaScriptLspWebSocket> connections = ConcurrentHashMap.newKeySet();
    private volatile boolean enabled;

    @Activate
    public JavaScriptLanguageServerAdapter(@Reference ScriptEngineManager scriptEngineManager,
            Map<String, ?> configuration) {
        this.scriptEngineManager = scriptEngineManager;
        modified(configuration);
    }

    @Modified
    public void modified(Map<String, ?> configuration) {
        List<JavaScriptLspWebSocket> connectionsToClose;
        synchronized (this) {
            enabled = Boolean.parseBoolean(String.valueOf(configuration.get("enabled")));
            connectionsToClose = enabled ? List.of() : List.copyOf(connections);
        }
        connectionsToClose.forEach(JavaScriptLspWebSocket::close);
    }

    @Deactivate
    public void deactivate() {
        List<JavaScriptLspWebSocket> connectionsToClose;
        synchronized (this) {
            enabled = false;
            connectionsToClose = List.copyOf(connections);
        }
        connectionsToClose.forEach(JavaScriptLspWebSocket::close);
    }

    @Override
    public String getId() {
        return ADAPTER_ID;
    }

    @Override
    public synchronized @Nullable Object createWebSocket(JettyServerUpgradeRequest servletUpgradeRequest,
            JettyServerUpgradeResponse servletUpgradeResponse, SecurityContext securityContext) {
        if (!enabled) {
            return null;
        }
        JavaScriptLspWebSocket connection = new JavaScriptLspWebSocket(scriptEngineManager, connections::remove);
        connections.add(connection);
        return connection;
    }
}
