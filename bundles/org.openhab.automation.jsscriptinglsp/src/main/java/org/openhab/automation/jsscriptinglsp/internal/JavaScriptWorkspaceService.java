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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.services.WorkspaceService;

/**
 * Workspace notifications are not needed for the initial document-analysis implementation.
 *
 * @author Patrik Gfeller - Initial contribution
 */
@NonNullByDefault
final class JavaScriptWorkspaceService implements WorkspaceService {
    @Override
    @NonNullByDefault({})
    public void didChangeConfiguration(DidChangeConfigurationParams params) {
    }

    @Override
    @NonNullByDefault({})
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
    }
}
