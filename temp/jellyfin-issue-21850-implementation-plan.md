# Jellyfin Public User Discovery Implementation Plan

**Issue:** openHAB Add-ons #21850  
**Binding:** Jellyfin  
**Status:** Work packages 1-2 complete; work packages 3-9 pending

## Objective

Discover the Jellyfin accounts returned by `GET /Users/Public` and represent each as an informational user Thing under its configured Jellyfin bridge. Expose the account name and primary profile picture through read-only channels. Use the Jellyfin UUID, scoped beneath the bridge, as stable identity.

This is account discovery, independent of active sessions and client devices. The separate per-client session `user` / `user-id` channel proposal in the API investigation is out of scope.

## Implementation Status

- **Work package 1 complete:** The generated `UserApi.getPublicUsers()` returns `List<UserDto>` from `/Users/Public`; `UserDto.Id` is represented as a nullable `UUID`, and `Name` and `PrimaryImageTag` are nullable strings. `ImageApi.getUserImage(UUID, String, ImageFormat)` returns a `File`, and `ImageFormat.JPG` serializes as `Jpg`. The bridge owns the shared API client and existing client discovery lifecycle. Its `ImageCache` is keyed and evicted for currently playing media, so it should not be reused for user images without changing that ownership/eviction contract.
- **Work package 2 complete:** Added the bridge-associated informational user Thing type and read-only `user-name` and `profile-image` channel types. Added `THING_TYPE_JELLYFIN_USER` to supported Thing types, plus bridge-scoped canonical UUID UID construction and a regression test for stable identity and bridge isolation. The UID helper and its test are temporarily in `Constants.java` and `ConstantsTest.java`; move both into the user discovery implementation when that work is added. Runtime name/image state mapping remains pending.
- **Work packages 3-9 pending:** User-list retrieval, discovery reconciliation, configured-Thing status/state handling, profile-image retrieval/cache behavior, compiler diagnosis, test-strategy review, focused regression tests, and README documentation remain to be completed and validated.

## Scope and Decisions

In scope:

- Add a bridge-associated user Thing type and read-only `user-name` (String) and `profile-image` (Image) channels.
- Fetch the public user list using the generated `UserDto` API model and existing bridge API client facilities.
- Map and reconcile users idempotently using bridge UID plus canonical Jellyfin user UUID.
- Fetch the image with generated `ImageApi.getUserImage(userId, tag, format)` using `format=Jpg` when a primary image tag exists.
- Preserve configured Things and last-known states across temporary failures; distinguish configured Things from Inbox discovery results when a complete successful list removes a user.
- Add focused tests for mapping, image behavior, reconciliation, and failure retention.

Out of scope:

- Discovering private/hidden server accounts, account administration, credentials or policies.
- Creating Things from sessions or `AdditionalUsers`; changing client/session matching, playback state or commands.
- Persisting image bytes or introducing a UI beyond normal Thing channels.

Fixed contract from the feature specification:

- UID uses the canonical Jellyfin user UUID under the configured bridge; names are mutable labels/data, never identity.
- A non-blank name updates `user-name`; absent, null or blank name yields `UNDEF`. Use `Jellyfin User` only as the Thing label fallback.
- `PrimaryImageTag` absent or an image 404 yields `UNDEF` for `profile-image`, without failing user discovery. Do not expose stale image bytes after the tag changes.
- Reconcile/remove stale Inbox results only after a successful, complete `/Users/Public` response. Never automatically delete a configured Thing. If a complete response no longer includes its user, retain last-known channel values and set the configured Thing OFFLINE; restore and refresh it if the user returns.
- Timeout, HTTP error, malformed or partial response, bridge unavailability, and image retrieval failure must not be mistaken for an empty complete list or remove a user. Preserve channel values on discovery/bridge failures and retry according to existing binding conventions.
- Credentials only pass through the established shared client/interceptor. Never put them in URLs, properties, cache keys, logs or discovery data.

## Repository Starting Points

Paths verified in the current workspace:

- Bridge lifecycle and orchestration: `bundles/org.openhab.binding.jellyfin/src/main/java/org/openhab/binding/jellyfin/internal/handler/ServerHandler.java`, `handler/TaskManager.java`, and `handler/tasks/DiscoveryTask.java`.
- Existing bridge-bound discovery pattern: `bundles/org.openhab.binding.jellyfin/src/main/java/org/openhab/binding/jellyfin/internal/discovery/ClientDiscoveryService.java` and `discovery/ServerDiscoveryService.java`.
- Shared API facilities and current image cache: `bundles/org.openhab.binding.jellyfin/src/main/java/org/openhab/binding/jellyfin/internal/api/ApiClientFactory.java`, `api/ApiClientWrapper.java`, and `api/ImageCache.java`.
- Existing Thing metadata: `bundles/org.openhab.binding.jellyfin/src/main/resources/OH-INF/thing/types/client-thing-type.xml`, `client-channel-types.xml`, and `server-bridge-type.xml`.
- Existing discovery/bridge tests: `bundles/org.openhab.binding.jellyfin/src/test/java/org/openhab/binding/jellyfin/internal/handler/ClientDiscoveryServiceTest.java`, `handler/ServerHandlerTest.java`, and `handler/tasks/DiscoveryTaskTest.java`.
- Generated sources are under `bundles/org.openhab.binding.jellyfin/src/3rdparty/java`; confirm exact generated `UserApi`, `ImageApi`, and `UserDto` package/type signatures there before implementation. The checked-in OpenAPI source is `bundles/org.openhab.binding.jellyfin/tools/generate-sources/scripts/specifications/yaml/jellyfin-openapi-10.11.6.yaml`.

These are investigation starting points, not a mandate to force the feature into a particular class. Reuse the owning bridge and current abstractions; add narrowly scoped user discovery/handler code only where the existing lifecycle requires it. Do not alter client discovery to source user Things.

## Ordered Work Packages

### 1. Confirm API and ownership boundaries

**Depends on:** None  
**Likely surfaces:** Generated Jellyfin API sources; `ApiClientWrapper`; `ServerHandler`; `TaskManager`; `DiscoveryTask`; `ClientDiscoveryService` as pattern only.

- Confirm generated method signatures, UUID/nullability types, response completion/error semantics, `ImageApi.getUserImage` response type, and availability of the `Jpg` format enum.
- Trace how the bridge supplies its configured URL/authenticated shared client, starts discovery, owns discovery services, and disposes tasks/listeners.
- Trace how discovery identifies already-configured child Things and how Thing status/channel updates are performed by handlers. Decide which bridge-owned component coordinates configured-user availability without writing/deleting configured Things.
- Check whether an existing in-memory image cache can be safely reused for users; document whether it supports the required key/invalidation and Image channel representation. Extend it only if it is the correct existing abstraction.

**Done when:** Implementation decisions use confirmed generated and openHAB APIs; no hand-built JSON parsing, guessed image URL, new thread, or parallel CRUD framework is planned.

**Status:** Complete. The generated API signatures and nullable model fields were confirmed. The existing cache is specific to currently playing item images and is not a drop-in cache for user profiles.

### 2. Define user Thing metadata and stable identity

**Depends on:** Work package 1  
**Likely surfaces:** `OH-INF/thing/types/` XML and Thing UID construction in the discovery implementation.

- Add the user Thing type under the Jellyfin bridge and define `user-name` as read-only String and `profile-image` as read-only Image channels, following local metadata conventions.
- Construct its UID from the bridge UID and canonical `UserDto.Id`; verify the UID segment encoding accepted by openHAB. Do not generate a random UUID or use name as a key.
- Use a non-blank API name as display metadata; map missing/null/blank state to `UNDEF`, and use `Jellyfin User` only for the Thing label when unavailable.
- Keep the type informational: no account commands, password fields, sensitive properties or extra user-ID channel.

**Done when:** Metadata parses and the same Jellyfin user on one bridge always maps to one UID; the same UUID on a different bridge maps to a different Thing.

**Status:** Complete for Thing metadata and stable UID construction. Runtime label/state mapping remains part of work package 4.

### 3. Implement public-list retrieval and discovery mapping

**Depends on:** Work packages 1-2  
**Likely surfaces:** Existing bridge discovery classes and task lifecycle; add a dedicated user discovery service/helper only if required by established binding patterns.

- Request `GET /Users/Public` through the generated API and configured shared client. Do not substitute privileged `GET /Users` or depend on sessions/client polling.
- Convert each valid `UserDto` to one Inbox discovery result using bridge-scoped UUID identity and the existing bridge association/representation-property conventions.
- Treat only a valid, successfully parsed, complete response (including a valid empty array) as authoritative for Inbox reconciliation. Keep prior Inbox entries on transport/HTTP/parse/partial errors and offline bridge state.
- Make repeated scans idempotent; a changed name updates the same result. Remove only stale Inbox results after a complete authoritative response. Do not automatically remove configured Things.

**Done when:** Tests or a focused discovery test demonstrate one result per user, stable identity across name changes, valid empty-list reconciliation, and no deletion on failure.

### 4. Implement configured user Thing state and reconciliation

**Depends on:** Work packages 1-3  
**Likely surfaces:** User Thing handler and bridge-managed user availability coordination, aligned with existing handler lifecycle.

- On valid user data, update configured user Thing label and `user-name`; absent/null/blank name sets only that channel to `UNDEF` and uses the specified label fallback.
- Compare each complete response with configured user Things under this bridge. If a configured account is absent, retain its last-known channel values and set its Thing OFFLINE; never remove its Thing or configuration. When it returns, restore status and refresh its data.
- On bridge outage or invalid/incomplete list, retain last-known channel states and avoid interpreting uncertainty as removal.
- Ensure repeated updates are idempotent and status transitions do not race disposal. Stop or release listeners, tasks and scheduled work with the owning bridge lifecycle.

**Done when:** Configured and Inbox lifecycles are distinct; removal, offline, restoration, bridge-failure retention and disposal behavior match the feature contract.

### 5. Implement profile-image retrieval and cache behavior

**Depends on:** Work package 1; channel type from work package 2; integrate with work package 4  
**Likely surfaces:** Generated `ImageApi`, `api/ImageCache.java` if suitable, and user Thing channel updates.

- When `PrimaryImageTag` is present, call the generated `/UserImage` operation with canonical user UUID, that tag, and `Jpg`; represent the returned image using the binding/openHAB Image channel pattern confirmed in work package 1.
- Use an in-memory cache keyed by user UUID, image tag, and format. A changed tag must not publish bytes cached for the old tag. Do not persist image bytes or include credentials in cache keys.
- No tag or HTTP 404 maps `profile-image` to `UNDEF`. Image failure is isolated from successful user discovery. A transient fetch failure must not remove the user and should preserve last-known image state unless the tag changed; on tag change, never show stale bytes for the new tag.
- Route authentication only through the configured shared client/interceptor; never put tokens in image URLs, Thing properties or logs.

**Done when:** Tag/no-tag/change, successful image, 404, transient error, cache isolation and stale-image behavior are covered deterministically.

### 6. Analyze and resolve the compiler problem

**Depends on:** Work packages 1-2; complete before relying on compiled/test results.
**Observed failure:** The focused Maven build passes the Maven/Java enforcer checks and reaches `maven-compiler-plugin`, but fails before compiling source with `No such compiler 'eclipse'`. Maven metadata requests to the configured openHAB repositories have also returned HTTP 401.

- Determine why the configured Eclipse compiler provider cannot be loaded in this environment. Check the effective Maven compiler-plugin configuration, availability/integrity of `plexus-compiler-eclipse` and ECJ artifacts, Maven local-cache state, and repository authentication/resolution without exposing credentials.
- Decide whether the cause is local Maven setup/repository access or a project configuration defect. Do not weaken Java 21 enforcement, Eclipse null analysis, or repository-wide compiler settings to get a green build.
- Apply the smallest approved fix or document the concrete external prerequisite if the blocker is outside the repository (for example, repository credentials or artifact availability).
- Verify that the Jellyfin module can compile using its prescribed compiler and strict null-analysis options before treating later test results as meaningful.

**Done when:** The root cause is recorded, the prescribed module compiler works without weakened checks, and a focused module compile completes; otherwise the exact external blocker and required resolution are documented.

### 7. Review discovery unit-test realism and timing

**Depends on:** Work packages 3 and 6.
**Likely surfaces:** `PublicUserDiscoveryServiceTest`, `ClientDiscoveryServiceTest`, openHAB discovery listener/core behavior, and repository unit/integration test conventions.

- Assess whether unit tests should instantiate the real discovery service and framework discovery classes, or isolate deterministic mapping/reconciliation logic behind a narrow seam. Preserve a focused test of actual observable discovery behavior where it can be deterministic.
- Review the asynchronous `DiscoveryListener` dispatch used by the new tests. Determine whether Mockito `timeout(...)` and especially negative `after(...).never()` windows are accepted repository practice, or whether latches/futures, observable completion signals, a synchronous mapping seam, or integration tests are more reliable.
- Prefer synchronization on completion/observable events over elapsed-time assumptions. Any unavoidable timeout must be bounded and conservative enough for slow CI; avoid fixed-delay assertions and arbitrary sleeps.
- Record the chosen test split and rationale, then adjust the discovery tests if the review finds a meaningful flakiness risk. Do not replace behavior coverage with tests of implementation details merely to avoid asynchronous APIs.

**Done when:** The test boundary and timing approach are justified against core behavior and repository conventions, and any identified flaky timing pattern is replaced with a deterministic bounded approach.

### 8. Add focused regression tests

**Depends on:** Work packages 2-5 and 7
**Likely surfaces:** Existing `src/test/java/.../discovery`, `handler`, and `api` test conventions; extend appropriate tests before adding new fixtures.

- Cover mapping with canonical UUID and bridge-scoped identity; repeat discovery and name change without duplicates.
- Cover absent/null/blank `Name`, `PrimaryImageTag`, valid empty list, and multiple bridges with same user UUID.
- Cover stale Inbox removal only after complete success; preserve configured Things on omission while marking OFFLINE; restore and refresh on reappearance.
- Cover timeout, HTTP error, malformed/partial response, bridge unavailable, and disposal without stale removal or clearing last-known values.
- Cover image request arguments, cache key/invalidation, valid image, no tag, 404, transient error, and changed-tag stale-byte prevention.
- Assert the existing client discovery, session association, playback channels and commands are unchanged where the existing test boundary makes that practical.

**Done when:** Tests are deterministic, test observable discovery/status/channel behavior, and include failure plus recovery paths without timing-sensitive sleeps.

### 9. Validate and document

**Depends on:** Work packages 1-8

- Update Jellyfin binding documentation (`bundles/org.openhab.binding.jellyfin/README.md`) with the user Thing, channels and public-user discovery behavior using the repository's established Thing/channel docs.
- Run formatter and focused module tests from the repository root, for example `mvn spotless:apply -pl :org.openhab.binding.jellyfin` followed by `mvn test -pl :org.openhab.binding.jellyfin`; use the project's prescribed targeted Maven command if local conventions require additional flags. Do not report tests as passing if compilation is blocked before test execution.
- Review the complete diff for generated-file mistakes, accidental reference edits, changed identifiers, credential leakage and consistency between metadata, runtime mapping, docs and tests.
- Verify image and list compatibility claims remain bounded: the spec covers Jellyfin OpenAPI 10.11.6 and the provided live test covers server 12.1.0 only; older servers and reverse-proxy behavior need validation before broader claims.

**Done when:** Formatting and focused tests pass, docs match implementation, and remaining compatibility limitations are stated rather than guessed.

## API Facts and Compatibility Risks

Established by the supplied feature specification: OpenAPI 10.11.6 defines `GET /Users/Public` as a public-login user list returning `UserDto[]`; `UserDto.Id` is the stable UUID, while `Name` and `PrimaryImageTag` are nullable. The generated API provides `/UserImage` with `userId`, `tag`, and `format`; both OpenAPI 10.11.6 and 12.1.0 include it. Jellyfin Server 12.1.0 returned the public list and a JPEG image with and without a token. The operation has no `maxWidth` or `maxHeight`; the older path-based image route is obsolete and is not to be used.

Still to validate: generated Java nullability and result types; exact behavior on older supported Jellyfin releases and reverse proxies; exception/status mapping for API errors; correctness of openHAB image channel encoding; and bridge/client teardown interactions. Do not weaken TLS or require a token for the endpoint based only on assumption; continue using configured shared authentication when present.

## Acceptance Checklist

- [ ] One user Thing per complete public-list entry, with bridge-scoped canonical Jellyfin UUID identity.
- [ ] `user-name` and `profile-image` are read-only String/Image channels with exact unavailable-state behavior.
- [ ] Name or image changes update the same identity and do not duplicate Things or expose stale image bytes.
- [ ] Only complete successful lists reconcile stale Inbox results; configured Things are preserved, go OFFLINE when no longer public, and recover when rediscovered.
- [ ] Transient, malformed, partial and bridge failures retain prior results and configured channel values.
- [ ] Image 404/no-tag/failure does not fail user discovery; credentials and image bytes are not leaked or persisted.
- [ ] Focused tests cover mapping, reconciliation, lifecycle, image caching/failures and relevant recovery.
- [ ] The Eclipse compiler issue is resolved without weakening Java 21 or null-analysis checks, or the exact external blocker is documented.
- [ ] Discovery-test use of real framework classes and asynchronous listener timeouts has been reviewed; timing is bounded and deterministic or justified.
- [ ] Every changed Java class has exactly one new author line in its class header for the Jellyfin public-user feature. For each newly created class, that line must include `Initial contribution - <feature>`.
- [ ] Client/session behavior remains outside this feature and unchanged.
- [ ] README, metadata, tests and implementation agree; targeted Spotless/Maven checks pass.
