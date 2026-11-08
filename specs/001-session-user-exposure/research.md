# Research - Session User Exposure

**Reviewed**: 2026-11-08. Design decisions are grounded in source inspection. The implementation
and test coverage are in place, but the full install and linked-Item runtime acceptance remain
blocked. See [tasks.md](tasks.md) and [handoff.md](handoff.md) for current evidence.
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

**Evidence at design time (2026-10-30)**: `ClientHandler.updateStateFromSession(...)` returned
for non-ONLINE Things. Session end and timeout could change Thing status before publication.
The handler had an identity-publishing block, but `ClientStateUpdater` had no identity mapping.

**Decision**: Test initial absence, session end, timeout, bridge offline, user switch, and recovery
using observable state callbacks. Make the smallest identity-scoped change needed to clear stale
state without weakening unrelated guards. Preserve a present empty name and independent fields.

**Rationale**: Stored session assertions do not prove linked Items were updated or cleared.

**Alternatives considered**: Treating `IsActive=false` or stopped playback as logout changes
existing semantics. Atomic two-channel publication is not an openHAB contract and is not required.

## 5. Separate Setup, Implementation, and Runtime Evidence

**Evidence**: The original handler fixtures supplied Strings where typed UUIDs and ThingUIDs
were required, and the discovery fixture initialized a non-null field only in setup. Those
compilation errors are repaired. The module POM already targets Java 21 and uses inherited
dependencies and logging infrastructure. The supporting malformed-response cleanup defect
was also repaired and covered by regression tests.

**Decision**: Retain the completed fixture repairs and extend existing test classes; do not
generate duplicate test suites, dependencies, logging configs, or DTOs.
Validate read-only metadata and initialized command handling separately.

**Rationale**: A module compilation failure blocks even selectively requested tests.
Public-user discovery is outside feature behavior; its supporting repair was separately authorized.

**Alternatives considered**: Marking inspected feature code as acceptance-tested would conceal gaps.
Invented console commands and a spec-specific runtime feature toggle cannot verify the binding.

## Outstanding Verification, Not Design Ambiguity

- The current full module run passed all 341 tests, but Karaf feature verification failed because
  core semantics requires ASM >=9.10.0 and the supplied feature resources provide ASM 9.9.1.
  Retrying with `-DwithResolver` failed on the same requirement. See [tasks.md](tasks.md).
- Source, metadata, automated lifecycle tests, and README now cover the identity channels and
  status transitions. Their implementation is not equivalent to linked-Item runtime acceptance.
- On 2026-11-08, the temporary client Thing exposed both String channels and temporary Items were
  linked, but its handler did not initialize because the runtime could not resolve
  `ClientHandler.updateStatus(ThingStatusDetail, String)`. The visible NULL values do not verify
  the feature. The temporary test objects were removed without a container restart.
- The previous openHAB 5.2.3 smoke checks remain historical, pre-migration evidence only. Do not
  infer that the current runtime is compatible from an image label or active binding bundle.

The design has no unresolved clarification markers. See [tasks.md](tasks.md) for executable work.

## 6. Thing Status API and Runtime Compatibility

**Evidence**: [openHAB core PR #5874](https://github.com/openhab/openhab-core/pull/5874)
introduced `updateStatus(ThingStatusDetail, description)`, which derives the Thing status from
the detail and rejects `ThingStatusDetail.NONE`. The old overloads that take both a status and
a detail are deprecated. Core 5.2.3's `BaseThingHandler` exposes only those legacy overloads;
the new method is absent. [Add-ons PR #21904](https://github.com/openhab/openhab-addons/pull/21904)
migrates Jellyfin status calls and leaves a client without a resolved bridge in its initial
uninitialized state rather than reporting a configuration or bridge-offline error.

**Decision**: Adopt the detail-derived status API and require openHAB 5.3 or later for build and
interactive acceptance of the migrated binding. Preserve the earlier 5.2.3 run only as historical
baseline smoke evidence.

**Rationale**: Calling a method absent from the 5.2.3 core at runtime can cause
`NoSuchMethodError` when the handler reports status. The API's mapping also distinguishes an
uninitialized bridge from an offline bridge.

**Alternatives considered**: Retaining deprecated status-plus-detail calls would keep source and
binary compatibility with 5.2.3 but would not complete the migration. A reflective fallback would
duplicate framework behavior and add unnecessary complexity.
