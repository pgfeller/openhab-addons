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

import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketOpen;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.MessageIssueException;
import org.eclipse.lsp4j.jsonrpc.RemoteEndpoint;
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;
import org.eclipse.lsp4j.jsonrpc.messages.Message;
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints;
import org.eclipse.lsp4j.services.LanguageClient;
import org.openhab.core.automation.module.script.ScriptEngineManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonParseException;

/**
 * Bridges complete WebSocket text frames to the LSP4J JSON-RPC endpoint.
 *
 * @author Patrik Gfeller - Initial contribution
 */
@WebSocket
@NonNullByDefault
public class JavaScriptLspWebSocket {
    private final Logger logger = LoggerFactory.getLogger(JavaScriptLspWebSocket.class);
    private final JavaScriptLanguageServer languageServer;
    private final Consumer<JavaScriptLspWebSocket> closeHandler;
    private boolean released;

    private volatile @Nullable Session session;
    private volatile @Nullable MessageJsonHandler jsonHandler;
    private volatile @Nullable RemoteEndpoint remoteEndpoint;

    public JavaScriptLspWebSocket(ScriptEngineManager scriptEngineManager,
            Consumer<JavaScriptLspWebSocket> closeHandler) {
        languageServer = new JavaScriptLanguageServer(scriptEngineManager, this::closeConnection);
        this.closeHandler = closeHandler;
    }

    @OnWebSocketOpen
    public void onOpen(Session session) {
        synchronized (this) {
            if (!released) {
                openConnection(session);
                return;
            }
        }
        session.close();
    }

    private void openConnection(Session session) {
        this.session = session;

        WebSocketLauncherBuilder builder = new WebSocketLauncherBuilder();
        builder.setLocalService(languageServer);
        builder.setRemoteInterface(LanguageClient.class);

        MessageJsonHandler handler = builder.createJsonHandlerForWebSocket();
        MessageConsumer outgoing = message -> sendMessage(handler.serialize(message));
        RemoteEndpoint endpoint = new RemoteEndpoint(outgoing, builder.createLocalEndpoint());
        handler.setMethodProvider(endpoint);
        endpoint.setJsonHandler(handler);

        jsonHandler = handler;
        remoteEndpoint = endpoint;
        languageServer.connect(builder.createRemoteProxy(endpoint));
    }

    @OnWebSocketMessage
    public void onText(String message) {
        MessageJsonHandler handler = jsonHandler;
        RemoteEndpoint endpoint = remoteEndpoint;
        if (handler == null || endpoint == null) {
            return;
        }

        try {
            Message parsedMessage = handler.parseMessage(message);
            if (parsedMessage == null) {
                closeConnection();
                return;
            }
            endpoint.consume(parsedMessage);
        } catch (MessageIssueException e) {
            endpoint.handle(e.getRpcMessage(), e.getIssues());
        } catch (JsonParseException e) {
            logger.debug("Closing JavaScript LSP WebSocket after invalid JSON-RPC input");
            closeConnection();
        }
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        releaseConnection();
    }

    @OnWebSocketError
    public void onError(@Nullable Session session, @Nullable Throwable error) {
        if (error != null) {
            logger.debug("JavaScript LSP WebSocket failed", error);
        }
        closeConnection();
    }

    private void sendMessage(String message) {
        Session activeSession = session;
        if (activeSession == null || !activeSession.isOpen()) {
            return;
        }

        activeSession.sendText(message, new Callback() {
            @Override
            public void succeed() {
            }

            @Override
            @NonNullByDefault({})
            public void fail(Throwable failure) {
                logger.debug("Failed to send JavaScript LSP WebSocket message", failure);
            }
        });
    }

    void close() {
        closeConnection();
    }

    private void closeConnection() {
        Session activeSession;
        synchronized (this) {
            activeSession = session;
            releaseConnection();
        }
        if (activeSession != null && activeSession.isOpen()) {
            activeSession.close();
        }
    }

    private synchronized void releaseConnection() {
        if (released) {
            return;
        }
        released = true;
        session = null;
        jsonHandler = null;
        remoteEndpoint = null;
        languageServer.dispose();
        closeHandler.accept(this);
    }

    private static final class WebSocketLauncherBuilder extends Launcher.Builder<LanguageClient> {
        MessageJsonHandler createJsonHandlerForWebSocket() {
            return createJsonHandler();
        }

        Endpoint createLocalEndpoint() {
            return ServiceEndpoints.toEndpoint(localServices);
        }

        LanguageClient createRemoteProxy(RemoteEndpoint endpoint) {
            return java.util.Objects.requireNonNull(createProxy(endpoint));
        }
    }
}
