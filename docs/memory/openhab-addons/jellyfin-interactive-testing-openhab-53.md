---
title: Jellyfin interactive testing on openHAB 5.3+
type: runbook
permalink: openhab-addons/jellyfin-interactive-testing-openhab-53
tags:
- openhab-addons
- jellyfin
- interactive-testing
- docker
- runbook
---

## Trigger

Run interactive Jellyfin binding acceptance after adopting `BaseThingHandler.updateStatus(ThingStatusDetail, ...)` or testing other code that requires openHAB 5.3+.

## Procedure

- Use an openHAB 5.3-or-later runtime. The previous `openhab-development-server` container used `openhab/openhab:5.2.3-debian` and exited with code 137 on 2026-11-03; do not reuse it for migrated code.
- A snapshot image was inspected on 2026-11-03 as version `5.3.0-snapshot`, Java `21.0.11`, digest `sha256:0b6782ed409150c3ec89c4f3656b4fbf1dedd08db98bb4038cc0f22408ca99ef`. The snapshot tag is mutable; use the recorded digest only to reproduce that historical image. For acceptance, inspect a current image's version and revision labels and verify its Java version.
- Use dedicated isolated configuration mounts. Do not assume previous container names, host paths, bridge setup, or bundle IDs are portable.
- Build the binding JAR, copy only the main JAR into the watched `/openhab/addons` directory, verify its SHA-256 inside the container, then use an authenticated interactive openHAB console to inspect the bundle and Things.
- Verify identity channels with linked Items only after metadata and implementation work is complete and the handler is initialized. A handler initialization error or `NULL` linked Item is not feature acceptance evidence.
- Protect credentials and account identities. Never put tokens in chat or artifacts. Coordinate manual configuration and token setup with the user; do not perform token-dependent steps independently.

## Verification and rollback

The 2026-11-03 observation verified the image and Java version only, not a running compatible container or feature acceptance. For a newly added candidate, remove only that JAR from the watched add-ons directory; for an update, restore the backed-up JAR. Never delete persistent configuration or userdata, force OSGi resolution, or weaken imports to work around an incompatible bundle.

## Provenance

Migrated from agent-memory-mcp record `67ebcbaf-654f-4e84-9165-1ff82f55a00e` (created 2026-11-08). The verification and historical image details above were dated 2026-11-03; the current host stack and feature acceptance were not verified during migration.
