---
title: Jellyfin linked-Item acceptance blocked by status API mismatch
type: incident
permalink: openhab-addons/jellyfin-linked-item-acceptance-incident
time: 2026-11-08
tags:
- openhab-addons
- jellyfin
- interactive-testing
- incident
- openhab-5.3
---

## Impact at the time

During user-coordinated openHAB UI acceptance, a temporary Jellyfin client Thing exposed `user-name` and `user-id` String channels, but its handler stayed `UNINITIALIZED` with `HANDLER_INITIALIZING_ERROR` because the runtime could not resolve `ClientHandler.updateStatus(ThingStatusDetail, String)`. Linked temporary Items displayed `NULL`, so this was not valid feature acceptance evidence. The temporary Items and Thing were removed without restarting the test container or recording identity values.

## Root cause and resolution at the time

The runtime's available openHAB core API did not provide the status-update method signature expected by the local binding build. The exact runtime/core version mismatch was not established. The test objects were cleaned up and the container was left unchanged.

## Follow-up

Before repeating linked-Item acceptance, confirm a compatible openHAB core/runtime, then coordinate the test with the user. The old record said T017 and feature completion remained open at that time; their current status was not rechecked during migration.

## Provenance

Migrated from agent-memory-mcp record `69d169b0-98e2-451a-b41f-420e43e90ddc` (recorded 2026-11-08). The incident status is historical and was not independently revalidated during migration.

- relates_to [[Jellyfin interactive testing on openHAB 5.3+]]
- relates_to [[Jellyfin client identity channels]]
