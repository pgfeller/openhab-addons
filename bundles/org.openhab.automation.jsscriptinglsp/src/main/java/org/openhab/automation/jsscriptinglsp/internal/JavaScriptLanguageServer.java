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

import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.openhab.core.automation.module.script.ScriptEngineManager;

/**
 * Implements the LSP lifecycle and delegates document analysis.
 *
 * @author Patrik Gfeller - Initial contribution
 */
@NonNullByDefault
final class JavaScriptLanguageServer implements LanguageServer {
    private final JavaScriptTextDocumentService textDocumentService;
    private final Runnable closeConnection;
    private final WorkspaceService workspaceService = new JavaScriptWorkspaceService();

    JavaScriptLanguageServer(ScriptEngineManager scriptEngineManager, Runnable closeConnection) {
        textDocumentService = new JavaScriptTextDocumentService(scriptEngineManager);
        this.closeConnection = closeConnection;
    }

    void connect(LanguageClient client) {
        textDocumentService.connect(client);
    }

    void dispose() {
        textDocumentService.dispose();
    }

    @Override
    @NonNullByDefault({})
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        TextDocumentSyncOptions syncOptions = new TextDocumentSyncOptions();
        syncOptions.setOpenClose(true);
        syncOptions.setChange(TextDocumentSyncKind.Full);

        ServerCapabilities capabilities = new ServerCapabilities();
        capabilities.setTextDocumentSync(Either.forRight(syncOptions));
        CompletableFuture<InitializeResult> result = new CompletableFuture<>();
        result.complete(new InitializeResult(capabilities));
        return result;
    }

    @Override
    @NonNullByDefault({})
    public CompletableFuture<Object> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
        closeConnection.run();
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }
}
