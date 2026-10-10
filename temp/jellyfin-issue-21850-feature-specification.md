# Feature Specification: Discover Jellyfin Login Users

**Issue:** openHAB Add-ons #21850  
**Binding:** Jellyfin  
**Status:** Ready for implementation; live behavior was checked on Jellyfin Server 12.1.0. Compatibility with older servers and reverse proxies remains subject to the fallbacks below.

## Objective

Automatically discover the Jellyfin server accounts that are publicly visible on its login screen and represent each account as a separate openHAB user Thing. Each user Thing exposes read-only channels for the account name and profile picture when available, for use by openHAB rules and UI integrations.

These are server user accounts, not playback sessions or client devices. Do not create a user Thing for a session's `AdditionalUsers` entry or infer accounts from client activity.

## Scope

### In Scope

- Discover users through Jellyfin's public-login-user list API.
- Create, update, and remove one user Thing for each discovered Jellyfin user.
- Expose stable user identity, account name, and profile-image information as read-only Thing properties or channels, following the binding's established metadata conventions.
- Integrate discovery with the configured Jellyfin bridge and respect bridge authentication, lifecycle, and disposal behavior.
- Define behavior for missing names/images, user removal, unavailable servers, and image retrieval failures.
- Add focused tests for discovery, reconciliation, data mapping, and failure behavior.

### Out of Scope

- Creating Things for all server accounts regardless of login-screen visibility.
- Authentication, password management, account creation, or account modification.
- Exposing passwords, password flags, policies, or other sensitive account data.
- Creating Things for client devices, sessions, or entries in `AdditionalUsers` as part of this feature.
- Changing existing client discovery, device-ID matching, playback channels, or commands.
- Downloading and persisting image binaries in openHAB unless a supported framework mechanism requires it.

## API Evidence and Boundaries

The bundled Jellyfin OpenAPI 10.11.6 specification defines `GET /Users/Public` with the summary “Gets a list of publicly visible users for display on a login screen.” It returns an array of `UserDto`; unlike `GET /Users`, this operation has no security requirement declared in the specification. A live server reported version 12.1.0 and returned HTTP 200 with an array of two users both without and with the configured API token. This verifies that endpoint on that server only; it does not establish compatibility across older versions or reverse proxies. Use the configured server base URL and shared HTTP/client facilities, allow the documented public request without requiring a token, and do not weaken TLS or other transport security.

The `UserDto` schema includes:

| Property | OpenAPI type | Feature use |
| --- | --- | --- |
| `Id` | UUID string | Stable Thing identity and discovery key |
| `Name` | Nullable string | Display name; may be unavailable |
| `PrimaryImageTag` | Nullable string | Indicates/tag-identifies the primary image when available |
| `ServerId`, `ServerName` | Nullable strings | Server metadata; do not use in place of the bridge identity |

The schema does not mark `Name` or `PrimaryImageTag` as required. Do not assume every public user has a name or profile image. Do not use `/Users` as an interchangeable endpoint: it is separately documented as the user list operation with authorization requirements and optional hidden/disabled filters.

Both the bundled OpenAPI 10.11.6 and Jellyfin's latest stable OpenAPI (12.1.0 at verification time) define `GET /UserImage` with `userId`, `tag`, and `format` query parameters and an `image/*` binary response. The generated binding client already provides `ImageApi.getUserImage(userId, tag, format)`. Use this generated operation with the Jellyfin user UUID and `PrimaryImageTag`; request `format=Jpg` when a predictable image format is useful. The operation does not define `maxWidth` or `maxHeight`, so do not pass those parameters. The legacy `GET /Users/{userId}/Images/{imageType}` route also worked on the live server, but upstream marks it obsolete and excludes it from API exploration; do not use it for generated-client implementation. The OpenAPI-backed request returned `200 image/jpeg` on Jellyfin Server 12.1.0, anonymously and with the configured token. Treat a 404 or request failure as an unavailable image, not a user-list failure. Pass configured credentials only through the shared request interceptor/header, never in the URL. Cache image data in memory with a key including user UUID, image tag, and format; do not persist image bytes or credentials.

## Thing Model

Add a user Thing type associated with the existing Jellyfin bridge. Use the Jellyfin user UUID as the stable Thing UID component; never use the display name as identity because names can change or collide. Keep the public Thing identifier stable across rediscovery and name changes.

User Thing contract:

| Data | Representation | Rules |
| --- | --- | --- |
| Jellyfin user ID | Thing UID component under the configured bridge | Use the canonical UUID; do not create an additional ID channel |
| Name | Read-only String channel `user-name` | Publish non-blank `Name`; absent, null, or blank maps to `UNDEF`. Use `Jellyfin User` as the Thing label only when no name is available. |
| Profile picture | Read-only Image channel `profile-image` | Fetch through generated `GET /UserImage` with user UUID, image tag, and `format=Jpg` when a tag exists. Absent tag or 404 maps to `UNDEF`; transient retrieval failure must not remove the user. Do not publish stale bytes for a changed tag. |

Use channels because the name and image are intended for Items, rules, and UI integrations; the existing binding already exposes Image channels and read-only metadata channels. These user Things are informational and have no commands.

## Discovery and Lifecycle Contract

1. Discover `UserDto` entries from `GET /Users/Public` through the configured bridge. Do not make discovery depend on the playback session list.
1. Key each discovered Thing by the combination of bridge identity and user UUID so identically numbered users on different Jellyfin servers cannot collide.
1. Update existing Things when name or primary image metadata changes; do not create duplicates when a name changes.
1. Reconcile discovery results only after a successful, complete public-user-list response. Remove a stale result from the openHAB Inbox after such a response confirms the user is no longer public. Never automatically delete a configured Thing; if a complete list confirms its user is no longer public, keep the configured Thing and last known channel values and set its status to OFFLINE. Restore it and refresh its channels if the user becomes public again.
1. If a valid response omits `Name` or `PrimaryImageTag`, update only that field to unavailable; do not discard the user Thing or substitute an empty string, literal `null`, fabricated value, or another account's data.
1. On timeout, HTTP error, malformed response, partial failure, or bridge unavailability, do not remove discovery results or configured Things and do not clear last known channel values. Retry using existing binding discovery conventions. Reconcile idempotently across repeated refreshes and rediscovery. Stop scheduled work and release listeners/resources when the bridge is disposed, following existing binding lifecycle patterns.
1. Do not expose or persist credentials in Thing properties, discovery results, image URLs, logs, or exceptions. Use established authenticated HTTP facilities where the verified image route requires authentication.

The public endpoint's OpenAPI definition has no security requirement. Runtime checks on Jellyfin 12.1.0 showed that both the list and image route work without authentication; still apply configured authentication through the shared client when present. Other server versions and reverse-proxy behavior are not verified. Parse the public-user response using the generated structured API model, not response-text matching.

## Relationship to Existing Client Identity

The previously proposed `user` and `user-id` client channels describe the primary user of a currently associated client session. They are distinct from the discovered user Things specified here:

- A user Thing represents a public Jellyfin account and exists independently of active playback/client sessions.
- A client Thing represents a device/session and may report the session's primary user identity through its own channels if that separate feature is implemented.
- A session can reference a user Thing by Jellyfin user UUID, but this discovery feature does not change client/session association or require creating a Thing link.
- `AdditionalUsers` does not create additional user Things; the public-users endpoint is the source of login-screen accounts.

## Acceptance Criteria

- The binding defines a user Thing type linked to the Jellyfin bridge and discovers one Inbox result per entry returned by a complete `GET /Users/Public` response.
- Thing identity uses the user UUID scoped to the bridge; name changes update the existing Thing rather than changing its identity.
- The user's display name is exposed through read-only channel `user-name`; absent, null, or blank names produce `UNDEF` without fabricated account data.
- The primary picture is exposed through read-only Image channel `profile-image` using the verified route when available; absent tags, unsupported routes, and 404 responses produce `UNDEF` without failing user discovery.
- Repeated successful discovery is idempotent. A complete list removes stale Inbox results but never deletes configured Things; transient or malformed failures retain prior discovery results and configured Thing data.
- The public-users endpoint is used instead of the privileged general user-list endpoint for login-screen visibility.
- No password, credential, or sensitive policy data is exposed in Things, logs, discovery results, or image references.
- Tests cover user mapping, bridge-scoped stable IDs, name changes, missing name/image, duplicate prevention, stale Inbox removal after a complete response, preservation of configured Things, and transient failure retention.
- Existing bridge, client Thing, session matching, and playback behavior remain unchanged.

## Suggested Implementation and Validation Surfaces

- Jellyfin binding Thing type XML and channel type metadata for the new user Thing.
- Existing bridge discovery/service patterns for user-list retrieval and user Thing reconciliation.
- Generated Jellyfin API model for `UserDto`; confirm generated nullability and UUID types before mapping.
- Existing read-only String and Image channel patterns, plus the shared HTTP request interceptor and in-memory image cache.
- Focused discovery tests with complete, empty, malformed, and failed responses; verify only Inbox result removal after successful complete reconciliation and preserve configured Things.
- Image retrieval tests for the primary route, tag changes, 404, unsupported route, and transient failures. The route was exercised against Jellyfin 12.1.0; older server compatibility must be verified before claiming it.

## Decisions and Compatibility Boundary

- Discovery uses the public endpoint. The live check verified anonymous and token-authenticated access on Jellyfin Server 12.1.0 only; retain compatibility with versions where the endpoint requires configured authentication or is unavailable.
- `user-name` and `profile-image` are read-only channels. The canonical user UUID is the Thing UID component under its bridge.
- Remove only stale Inbox results after a successful complete response. Preserve configured Things and their last known values; report them OFFLINE when their account is no longer public, and retain state through bridge outages.
- The generated `GET /UserImage` operation is present in both bundled OpenAPI 10.11.6 and latest stable OpenAPI 12.1.0, and its request succeeded on server 12.1.0. The legacy path-based user-image GET is explicitly excluded from API exploration and must not be used by generated code. On servers where the generated operation returns 404 or fails, the image is unavailable, not a user-discovery failure.
- No credentials are required for the live-tested public list/image requests, but use the configured shared request interceptor when present. Never place credentials in a URL, Thing property, log, cache key, or discovery result.
