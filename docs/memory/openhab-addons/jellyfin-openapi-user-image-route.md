---
title: Jellyfin OpenAPI user image route
type: note
permalink: openhab-addons/jellyfin-openapi-user-image-route
tags:
- openhab-addons
- jellyfin
- openapi
- api-compatibility
---

## Findings recorded 2026-10-05

The legacy memory record states that the bundled Jellyfin OpenAPI 10.11.6 specification defines `GET /UserImage` with `userId` (UUID), `tag`, and `format` query parameters and an `image/*` response. The checked-in generated `ImageApi.getUserImage(UUID, String, ImageFormat)` returns a downloaded `File`, so the record says regeneration is unnecessary for this operation. It also reports that Jellyfin OpenAPI 12.1.0 retained the operation and parameters.

The legacy record describes the path-based `GET /Users/{userId}/Images/{imageType}` as obsolete and ignored by ApiExplorer. It reports a 200 `image/jpeg` response from `GET /UserImage?userId=...&tag=...&format=Jpg`, both anonymously and with `X-Emby-Token`; it notes that `maxWidth` and `maxHeight` are not defined for this operation. No account identifiers or credentials were present in the record.

This is a historical migration, not a fresh API verification. Recheck the checked-in specification and generated client before relying on it for new work.

## Provenance

Migrated from agent-memory-mcp record `1608b3bb-0115-4386-99a0-3a9517022517` (created 2026-10-05). The same record was present in the verified ThinkPad-T14 and workspace database backups.

- relates_to [[Jellyfin client identity channels]]
