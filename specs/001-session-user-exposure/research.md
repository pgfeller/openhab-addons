# Research - Session User Exposure

**Reviewed**: 2026-10-30. These findings come from source inspection, not a passing build.
Paths below are relative to `bundles/org.openhab.binding.jellyfin/`.

## 1. Reuse the Existing API Model

**Evidence**: `src/3rdparty/java/org/openhab/binding/jellyfin/internal/gen/current/model/SessionInfoDto.java`
declares nullable UUID `UserId`, nullable String `UserName`, and PascalCase JSON property names.

**Decision**: Keep the generated DTO unchanged. Test fixtures use `UUID.fromString(...)`;
channel output uses `UUID.toString()`.

**Rationale**: The generated source already defines the API contract and nullability.

**Alternatives considered**: A duplicate String-based model would conflict with existing callers.
A new UUID parser would duplicate the binding's existing deserialization support.

## 2. Reuse the Binding-Local Session Bus

**Evidence**: `src/main/java/org/openhab/binding/jellyfin/internal/events/SessionEventListener.java`
defines `onSessionUpdate(@Nullable SessionInfoDto)`. `SessionEventBus` invokes listeners by device ID.
`internal/handler/ClientHandler.java` subscribes using `serialNumber` and unsubscribes during disposal.

**Decision**: Retain the existing callback, session selection, polling, and WebSocket paths.
Null payloads represent session end; no separate event class is required.

**Rationale**: Identity is already part of each selected session snapshot.

**Alternatives considered**: Extra per-client requests or a new event system would add overhead
and could select a different user from the one already associated with the client.

## 3. Reuse Channel Calculation and XML Metadata

**Evidence**: `internal/util/client/ClientStateUpdater.java` calculates existing channel states.
`src/main/resources/OH-INF/thing/types/client-thing-type.xml` declares client channel references.
`client-channel-types.xml` and `user-channel-types.xml` use String item types and read-only state
metadata. The existing user Thing already references `user-name-channel`.

**Decision**: Extend the pure state updater for identity, with corresponding handler publication.
Add client references for `user-name`/`user-id`; reuse compatible existing types and add only
missing types. Reconcile existing Java helper type IDs rather than adding parallel abstractions.

**Rationale**: XML metadata, not a Java constants class alone, exposes channels to openHAB.

**Alternatives considered**: New Thing factories or a standalone library/CLI are unnecessary.
Number channels cannot represent UUID identity. Java null and empty strings do not replace
`UnDefType.NULL` for absent values.

## 4. Test Clearing Through Lifecycle Paths

**Evidence**: `ClientHandler.updateStateFromSession(...)` currently returns for non-ONLINE Things.
Session end and timeout can change Thing status before publication. The existing handler has
an identity-publishing block, but `ClientStateUpdater` has no identity mapping yet.

**Decision**: Test initial absence, session end, timeout, bridge offline, user switch, and recovery
using observable state callbacks. Make the smallest identity-scoped change needed to clear stale
state without weakening unrelated guards. Preserve a present empty name and independent fields.

**Rationale**: Stored session assertions do not prove linked Items were updated or cleared.

**Alternatives considered**: Treating `IsActive=false` or stopped playback as logout changes
existing semantics. Atomic two-channel publication is not an openHAB contract and is not required.

## 5. Separate Setup, Implementation, and Runtime Evidence

**Evidence**: `ClientHandlerTest.java` currently supplies Strings where typed UUIDs and ThingUIDs
are needed. `PublicUserDiscoveryServiceTest.java` has a non-null field initialized only in setup.
The module POM already targets Java 21 and uses inherited dependencies and logging infrastructure.

**Decision**: Repair those compilation fixtures first during implementation. Extend existing
test classes; do not generate duplicate test suites, dependencies, logging configs, or DTOs.
Validate read-only metadata and initialized command handling separately.

**Rationale**: A module compilation failure blocks even selectively requested tests.
Public-user discovery is outside feature behavior, but its test fixture blocks the shared build.

**Alternatives considered**: Marking inspected code as tested would conceal the reported failures.
Invented console commands and a spec-specific runtime feature toggle cannot verify the binding.

## Outstanding Verification, Not Design Ambiguity

- Automated tests and the affected module build remain unexecuted for this setup-only repair.
- The requested openHAB 5.2.3 interactive target must be checked for compatibility with the
 built bundle; the repository module currently inherits version `5.3.0-SNAPSHOT`.
- Interactive acceptance requires a configured runtime and Jellyfin client. It must remain
 recorded as pending until performed, rather than inferred from unit tests.

The design has no unresolved clarification markers. See [tasks.md](tasks.md) for executable work.
