# JavaScript Language Server

This optional automation add-on provides language-server support for openHAB
JavaScript rules. Its service ID is `org.openhab.lsp.javascript`.

The authenticated WebSocket endpoint is available at
`/ws/jsscriptinglsp` when enabled. It uses the existing openHAB HTTP(S) listener
and authentication; it does not open a separate port. The initial
implementation supports LSP initialization, full document synchronization,
and JavaScript syntax diagnostics using the installed JavaScript scripting
engine. Completion and hover support are not yet implemented.

The endpoint accepts JavaScript source for parsing only; it does not execute
rule content.
