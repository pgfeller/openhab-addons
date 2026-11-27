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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import javax.script.Compilable;
import javax.script.ScriptException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.openhab.core.automation.module.script.ScriptEngineContainer;
import org.openhab.core.automation.module.script.ScriptEngineManager;
import org.openhab.core.common.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks open documents and publishes JavaScript parser diagnostics.
 *
 * @author Patrik Gfeller - Initial contribution
 */
@NonNullByDefault
final class JavaScriptTextDocumentService implements TextDocumentService {
    private static final String JS_MIME_TYPE = "application/javascript";
    private static final ExecutorService ANALYSIS_EXECUTOR = ThreadPoolManager.getPool("javascript-lsp");

    private final Logger logger = LoggerFactory.getLogger(JavaScriptTextDocumentService.class);
    private final ScriptEngineManager scriptEngineManager;
    private final ConcurrentMap<String, DocumentSnapshot> documents = new ConcurrentHashMap<>();
    private final AtomicLong documentGeneration = new AtomicLong();
    private volatile @Nullable LanguageClient client;

    JavaScriptTextDocumentService(ScriptEngineManager scriptEngineManager) {
        this.scriptEngineManager = scriptEngineManager;
    }

    void connect(LanguageClient client) {
        this.client = client;
    }

    void dispose() {
        documents.clear();
        client = null;
    }

    @Override
    @NonNullByDefault({})
    public void didOpen(DidOpenTextDocumentParams params) {
        TextDocumentItem document = params.getTextDocument();
        DocumentSnapshot snapshot = new DocumentSnapshot(document.getUri(), document.getVersion(), document.getText(),
                documentGeneration.incrementAndGet());
        documents.put(snapshot.uri(), snapshot);
        scheduleAnalysis(snapshot);
    }

    @Override
    @NonNullByDefault({})
    public void didChange(DidChangeTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        int version = params.getTextDocument().getVersion();
        List<org.eclipse.lsp4j.TextDocumentContentChangeEvent> changes = params.getContentChanges();
        if (changes.isEmpty()) {
            return;
        }

        String text = changes.getLast().getText();
        @Nullable
        DocumentSnapshot current = documents.get(uri);
        if (current == null || version <= current.version()) {
            return;
        }
        DocumentSnapshot updated = new DocumentSnapshot(uri, version, text, documentGeneration.incrementAndGet());
        if (documents.replace(uri, current, updated)) {
            scheduleAnalysis(updated);
        }
    }

    @Override
    @NonNullByDefault({})
    public void didClose(DidCloseTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        documents.remove(uri);
        LanguageClient connectedClient = client;
        if (connectedClient != null) {
            connectedClient.publishDiagnostics(new PublishDiagnosticsParams(uri, List.of()));
        }
    }

    @Override
    @NonNullByDefault({})
    public void didSave(DidSaveTextDocumentParams params) {
    }

    private void scheduleAnalysis(DocumentSnapshot snapshot) {
        try {
            ANALYSIS_EXECUTOR.execute(() -> analyzeAndPublish(snapshot));
        } catch (RejectedExecutionException e) {
            logger.error("Could not schedule JavaScript analysis for {}", snapshot.uri(), e);
        }
    }

    private void analyzeAndPublish(DocumentSnapshot snapshot) {
        List<Diagnostic> diagnostics = analyze(snapshot.uri(), snapshot.text());
        DocumentSnapshot current = documents.get(snapshot.uri());
        LanguageClient connectedClient = client;
        if (current != null && current.generation() == snapshot.generation() && connectedClient != null) {
            PublishDiagnosticsParams params = createDiagnosticsParams(snapshot.uri(), diagnostics);
            params.setVersion(snapshot.version());
            connectedClient.publishDiagnostics(params);
        }
    }

    @SuppressWarnings("null") // LSP4J's generated list API is unannotated; diagnostics are non-null here.
    private static PublishDiagnosticsParams createDiagnosticsParams(String uri, List<Diagnostic> diagnostics) {
        return new PublishDiagnosticsParams(uri, diagnostics);
    }

    private List<Diagnostic> analyze(String uri, String source) {
        String engineIdentifier = "jsscriptinglsp-" + UUID.randomUUID();
        ScriptEngineContainer container;
        try {
            container = scriptEngineManager.createScriptEngine(JS_MIME_TYPE, engineIdentifier);
        } catch (RuntimeException e) {
            logger.error("Could not create a JavaScript engine for {}", uri, e);
            return List.of(analysisFailureDiagnostic());
        }
        if (container == null) {
            logger.error("No JavaScript script engine is available to analyze {}", uri);
            return List.of(analysisFailureDiagnostic());
        }

        try {
            if (!(container.getScriptEngine() instanceof Compilable compilable)) {
                logger.error("The JavaScript engine does not support compilation for {}", uri);
                return List.of(analysisFailureDiagnostic());
            }
            // Compile only: editor contents must never be executed by the language server.
            compilable.compile(source);
            return List.of();
        } catch (ScriptException e) {
            return List.of(syntaxDiagnostic(e));
        } catch (RuntimeException e) {
            logger.error("JavaScript analysis failed for {}", uri, e);
            return List.of(analysisFailureDiagnostic());
        } finally {
            scriptEngineManager.removeEngine(engineIdentifier);
        }
    }

    private static Diagnostic syntaxDiagnostic(ScriptException exception) {
        int line = Math.max(0, exception.getLineNumber() - 1);
        int character = Math.max(0, exception.getColumnNumber() - 1);
        Position position = new Position(line, character);
        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setRange(new Range(position, position));
        diagnostic.setSeverity(DiagnosticSeverity.Error);
        diagnostic.setSource("openHAB JavaScript LSP");
        diagnostic.setMessage(exception.getMessage() == null ? "Invalid JavaScript syntax" : exception.getMessage());
        return diagnostic;
    }

    private static Diagnostic analysisFailureDiagnostic() {
        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setRange(new Range(new Position(0, 0), new Position(0, 0)));
        diagnostic.setSeverity(DiagnosticSeverity.Error);
        diagnostic.setSource("openHAB JavaScript LSP");
        diagnostic.setMessage("JavaScript analysis failed; see the openHAB log.");
        return diagnostic;
    }

    private record DocumentSnapshot(String uri, int version, String text, long generation) {
    }
}
