# Implementation Plan: Session User Exposure

**Feature**: `001-session-user-exposure` | **Date**: 2026-10-30 | **Spec**: [spec.md](spec.md)

**Input**: Existing partial implementation and reviewed feature specification.

**Status**: Implementing; T001/T002 complete, T003-T018 open. Baseline runtime smoke checks
passed; identity-channel acceptance and the full install gate remain open. See [handoff.md](handoff.md).

## Summary

Expose `user-name` and `user-id` as read-only String channels on existing Jellyfin client Things.
Reuse the generated session DTO, binding-local event bus, and state updater. Map absent fields
to `UnDefType.NULL`, UUIDs through `UUID.toString()`, and clear identity on unavailable-session
paths. Test fixtures are repaired; continue with metadata and published-state tests before
implementing the missing behavior.

## Technical Context

**Language/Version**: Java 21, openHAB null annotations.

**Primary Dependencies**: Existing openHAB Thing/Channel APIs, generated Jellyfin model,
Jackson, SLF4J; no added dependencies.

**Storage**: None; current session state only.

**Testing**: Existing JUnit 5 and Mockito tests, Maven compiler and Surefire, Spotless,
metadata validation, and interactive runtime acceptance.

**Target Platform**: Repository openHAB 5.x baseline. The user-selected interactive runtime
is openHAB 5.2.3. Startup, live bridge connectivity, client availability, and public-user discovery
were smoke-tested successfully. Full feature compatibility remains unverified.

**Project Type**: Existing OSGi binding module, not a new library or CLI.

**Performance Goals**: Publish identity during the existing session update; no extra polling,
requests, schedulers, or identity cache.

**Constraints**: Preserve device matching, session selection, timeout, transport, existing channel
IDs, and unrelated user-Thing discovery. Do not edit generated DTOs or translations.

**Scale/Scope**: Two channels per existing client Thing, five functional requirements.

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
- Keep ordinary identity commands unsupported and retain non-mutating refresh behavior.
- Follow [data-model.md](data-model.md) and [the contract](contracts/jellyfin-session-contract.md).
- Validate with [quickstart.md](quickstart.md), including linked Item states rather than only
  internal session storage assertions. Separate automated and interactive evidence.

## Existing Implementation Baseline

Source inspection on 2026-10-30 confirms a generated UUID-typed DTO, session subscription,
and a handler identity-publishing block. Client Thing XML has no identity channel declarations.
The pure state updater has no identity mappings. Handler publication is ONLINE-gated, so clearing
must be tested across offline transitions. Existing handler tests mostly inspect stored session
data rather than fully asserting identity publications. The reported compilation errors and
supporting public-user cleanup blocker were repaired; all 319 module tests now pass. Packaging
succeeds, but full install is blocked by the supplied core snapshot's ASM dependency mismatch.
Discovery changes remain a separately authorized supporting repair, not session-user feature scope.

The implementation gaps are source-inspection findings; passing baseline checks are recorded
separately in [handoff.md](handoff.md). [tasks.md](tasks.md) tracks the remaining feature work.

## Complexity Tracking

No constitution violations or additional architectural components are required.
