# Implementation Plan: Session User Exposure

**Feature**: `001-session-user-exposure` | **Date**: 2026-10-30 | **Spec**: [spec.md](spec.md)

**Input**: Existing partial implementation and reviewed feature specification.

**Status (2026-11-08)**: Implementing; T001-T015 complete, T016-T018 open. The 341-test
module run passed, but Karaf feature verification remains blocked by the ASM dependency
mismatch. Linked-Item acceptance was attempted but blocked by a runtime status-API mismatch.
See [tasks.md](tasks.md) and [handoff.md](handoff.md) for current evidence.

## Summary

Expose `user-name` and `user-id` as read-only String channels on existing Jellyfin client Things.
Reuse the generated session DTO, binding-local event bus, and state updater. Map absent fields
to `UnDefType.NULL`, UUIDs through `UUID.toString()`, and clear identity on unavailable-session
paths. Use openHAB's detail-derived Thing status API and preserve lifecycle distinctions for
uninitialized bridges, offline bridges, missing sessions, and invalid configuration. This API
requires openHAB 5.3 or later, so acceptance testing must move from the 5.2.3 baseline runtime.

## Technical Context

**Language/Version**: Java 21, openHAB null annotations.

**Primary Dependencies**: Existing openHAB Thing/Channel APIs, generated Jellyfin model,
Jackson, SLF4J; no added dependencies.

**Storage**: None; current session state only.

**Testing**: Existing JUnit 5 and Mockito tests, Maven compiler and Surefire, Spotless,
metadata validation, and interactive runtime acceptance.

**Target Platform**: Repository openHAB 5.3.0-SNAPSHOT baseline. The previous interactive
runtime was openHAB 5.2.3, where startup, live bridge connectivity, client availability, and
public-user discovery were smoke-tested before status API migration. The detail-derived status
API is absent in 5.2.3. Use openHAB 5.3 or later for feature acceptance; the migrated bundle is
not runtime-compatible with 5.2.3.

**Project Type**: Existing OSGi binding module, not a new library or CLI.

**Performance Goals**: Publish identity during the existing session update; no extra polling,
requests, schedulers, or identity cache.

**Constraints**: Preserve device matching, session selection, timeout, transport, existing channel
IDs, and unrelated user-Thing discovery. Do not edit generated DTOs or translations. For detailed
Thing states, use `updateStatus(ThingStatusDetail, description)` without a separate status;
use the status-only overload when there is no detail, and never pass `ThingStatusDetail.NONE`
to the detail-derived overload.

**Scale/Scope**: Two channels per existing client Thing, seven functional requirements.

## Constitution Check

Evaluated against constitution v3.0.0 before and after this design repair.

| Principle | Design evidence | Gate |
| --- | --- | --- |
| I. Reuse | Extend existing handler, updater, DTO, and XML metadata | PASS |
| II. Interfaces | Existing client Thing; additive channel IDs; no new CLI | PASS |
| III. Test-first | Repair compilation, then assert published states before implementation | PASS for plan; execution pending |
| IV. Integration | Callback/event path tests and explicit runtime validation tasks | PASS for plan; execution pending |
| V. Simplicity | No new requests, dependencies, storage, or scheduler | PASS |
| VI. Quality | Real source paths, generated model retained, formatting/documentation gates | PASS |
| VII. Security | No authentication change or credential-bearing examples | PASS |

There are no design exceptions. No retroactive test-first or passing-build claim is made for
the existing partial implementation. Runtime OSGi tests, if added, must follow bndrun conventions;
Mockito callback tests are not a substitute for runtime acceptance.

## Project Structure

### Documentation (this feature)

```text
specs/001-session-user-exposure/
  spec.md
  plan.md
  research.md
  data-model.md
  quickstart.md
  handoff.md
  checklists/requirements.md
  contracts/jellyfin-session-contract.md
  tasks.md
```

### Source Code (repository root)

```text
bundles/org.openhab.binding.jellyfin/
  pom.xml
  README.md
  src/3rdparty/java/org/openhab/binding/jellyfin/internal/gen/current/model/SessionInfoDto.java
  src/main/java/org/openhab/binding/jellyfin/channel/
  src/main/java/org/openhab/binding/jellyfin/internal/Constants.java
  src/main/java/org/openhab/binding/jellyfin/internal/events/
  src/main/java/org/openhab/binding/jellyfin/internal/handler/ClientHandler.java
  src/main/java/org/openhab/binding/jellyfin/internal/util/client/ClientStateUpdater.java
  src/main/resources/OH-INF/thing/types/client-thing-type.xml
  src/main/resources/OH-INF/thing/types/client-channel-types.xml
  src/test/java/org/openhab/binding/jellyfin/internal/
```

**Structure Decision**: Reuse the binding module. Treat `ClientStateUpdater` as the pure
identity-state calculation owner and `ClientHandler` as the publisher and lifecycle owner.
Prefer existing channel constants and XML patterns over new metadata abstractions. Existing
`UserNameChannel`/`UserIdChannel` helpers must be reconciled with the actual XML type IDs,
not taken as proof that the channels are registered.

## Phase 0: Research

See [research.md](research.md) for verified code evidence, decisions, and rejected alternatives.
No unresolved design questions require new research agents.

## Phase 1: Design and Contracts

- Reuse `SessionInfoDto.getUserId(): UUID` and `getUserName(): String`, both nullable.
- Preserve binding-local `onSessionUpdate(@Nullable SessionInfoDto)`; null means session ended.
- Calculate both identity states from the same snapshot with independent null handling.
- Declare `user-name` and `user-id` in client Thing XML with matching String, read-only types.
- Ensure initial absence, null session, timeout, and bridge-offline paths publish `NULL` even
  when the Thing has just transitioned offline. Do not weaken unrelated ONLINE guards.
- Use the detail-derived status API for detailed states. Leave the client UNINITIALIZED while
  its bridge is unresolved during initial setup or reports `BRIDGE_UNINITIALIZED`; use
  `BRIDGE_OFFLINE` only for an offline bridge, `COMMUNICATION_ERROR` for an absent/timed-out
  session, and `CONFIGURATION_ERROR` for invalid configuration or bridge type. Clear identity
  on recognized unavailable-bridge transitions.
- Require an openHAB 5.3+ runtime for interactive acceptance because 5.2.3 lacks the new
  status API. Treat the earlier 5.2.3 deployment as historical baseline smoke evidence only.
- Keep ordinary identity commands unsupported and retain non-mutating refresh behavior.
- Follow [data-model.md](data-model.md) and [the contract](contracts/jellyfin-session-contract.md).
- Validate with [quickstart.md](quickstart.md), including linked Item states rather than only
  internal session storage assertions. Separate automated and interactive evidence.

## Current Implementation State

The client Thing XML declares both read-only String identity channels. The state updater maps
nullable names and UUIDs independently, and the handler publishes and clears those states on
session and bridge lifecycle transitions. Handler tests cover the published states, status
callbacks, command behavior, and bridge initialization/status mapping. The implementation uses
the detail-derived status API and preserves UNINITIALIZED while bridge state is unresolved.
T001-T015 are complete; see [tasks.md](tasks.md) for their scope and the remaining acceptance gates.

The implementation is not considered runtime-accepted: the 2026-11-08 linked-Item attempt left
the client UNINITIALIZED with `HANDLER_INITIALIZING_ERROR` because the running core could not
resolve `ClientHandler.updateStatus(ThingStatusDetail, String)`. The visible NULL Item states do
not validate identity behavior. Separately, the full module build passed all 341 tests but Karaf
verification failed because `org.openhab.core.semantics` requires ASM >=9.10.0 while the supplied
feature resources provide ASM 9.9.1. Retrying with `-DwithResolver` did not resolve that mismatch.
Keep T016-T018 open; do not bypass either gate.

## Complexity Tracking

No constitution violations or additional architectural components are required.
