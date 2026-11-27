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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.script.Compilable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.module.script.ScriptEngineContainer;
import org.openhab.core.automation.module.script.ScriptEngineFactory;
import org.openhab.core.automation.module.script.ScriptEngineManager;

class JavaScriptTextDocumentServiceTest {
    @Test
    void syntaxErrorsArePublishedWithoutExecutingDocument() throws Exception {
        ScriptEngineManager scriptEngineManager = mock(ScriptEngineManager.class);
        ScriptEngine scriptEngine = mock(ScriptEngine.class, withSettings().extraInterfaces(Compilable.class));
        Compilable compilable = (Compilable) scriptEngine;
        ScriptException syntaxError = new ScriptException("Unexpected token", "example.js", 2, 4);
        when(scriptEngineManager.createScriptEngine(eq("application/javascript"), anyString()))
                .thenAnswer(invocation -> new ScriptEngineContainer(scriptEngine, mock(ScriptEngineFactory.class),
                        invocation.getArgument(1)));
        when(compilable.compile(anyString())).thenThrow(syntaxError);

        CountDownLatch diagnosticsPublished = new CountDownLatch(1);
        AtomicReference<PublishDiagnosticsParams> published = new AtomicReference<>();
        LanguageClient client = mock(LanguageClient.class);
        doAnswer(invocation -> {
            published.set(invocation.getArgument(0));
            diagnosticsPublished.countDown();
            return null;
        }).when(client).publishDiagnostics(org.mockito.ArgumentMatchers.any(PublishDiagnosticsParams.class));

        JavaScriptTextDocumentService service = new JavaScriptTextDocumentService(scriptEngineManager);
        service.connect(client);
        service.didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem("file:///example.js", "javascript", 1, "const answer = ;")));

        try {
            assertTrue(diagnosticsPublished.await(5, TimeUnit.SECONDS));
            PublishDiagnosticsParams result = published.get();
            Diagnostic diagnostic = result.getDiagnostics().getFirst();
            assertEquals(1, diagnostic.getRange().getStart().getLine());
            assertEquals(3, diagnostic.getRange().getStart().getCharacter());
            verify(scriptEngine, never()).eval(anyString());
            verify(scriptEngineManager).removeEngine(anyString());
        } finally {
            service.dispose();
        }
    }
}
