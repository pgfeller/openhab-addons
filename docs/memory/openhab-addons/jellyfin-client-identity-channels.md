---
title: Jellyfin client identity channels
type: note
permalink: openhab-addons/jellyfin-client-identity-channels
tags:
- openhab-addons
- jellyfin
---

## Verified behavior

The Jellyfin client Thing exposes public channel IDs `user-name` and `user-id`, backed by XML channel type IDs `user-name-channel` and `user-id-channel`. `ClientStateUpdater.calculateChannelStates` maps a non-null session user name directly to `StringType` (including an empty string) and a non-null user UUID to its string representation; missing values map to `UnDefType.NULL`. `ClientHandler` publishes the calculated state only for linked channels.

## Provenance

Consolidates agent-memory-mcp records `7bbbebae-fd27-4077-b809-64b94971a768` (created 2026-11-08) and `de21ce73-5045-420e-b343-39b15d828f0e` (created 2026-11-08). Verified against the current Jellyfin binding source on 2026-11-24: `ClientStateUpdater.java`, `ClientHandler.java`, and `client-thing-type.xml`.
