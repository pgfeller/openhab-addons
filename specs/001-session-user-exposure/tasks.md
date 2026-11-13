# Tasks - Session User Exposure

**Input**: [spec.md](spec.md), [plan.md](plan.md), and supporting design artifacts.

**Current state (2026-11-08)**: T001-T015 complete; T016-T018 remain open. T016 is blocked
by Karaf feature resolution; T017 is blocked by a runtime status-API mismatch. Resume with
those blockers, not by repeating completed implementation tasks.
See [handoff.md](handoff.md) for the current branch, evidence, runtime setup, and privacy rules.

**Reconciled**: 2026-10-30. This backlog replaces the unexecuted scaffolding list with
repository-grounded work. No completed task markers or task completion history existed to migrate.
IDs below identify this repaired backlog. Completion markers reflect executed evidence;
unverified or blocked tasks remain open.

## Existing Baseline

The module, Java 21 configuration, generated DTO, SLF4J, and session subscription already exist.
The handler publishes both identity channels through the pure state updater. Client Thing XML
declares both as read-only String channels, reusing the shared `user-name-channel` type and the
client-specific `user-id-channel` type. T007-T009 and their focused tests are complete. Do not
recreate the module, DTO, factory, or logging configuration.
The SDD setup repair does not claim implementation completion.

## Phase 1: Setup - Restore Test Compilation

- [x] T001 Fix non-null fixture initialization in `bundles/org.openhab.binding.jellyfin/src/test/java/org/openhab/binding/jellyfin/internal/discovery/PublicUserDiscoveryServiceTest.java` without suppressing null analysis; preserve discovery behavior.
- [x] T002 Fix typed Thing/ThingUID and UUID fixtures in `bundles/org.openhab.binding.jellyfin/src/test/java/org/openhab/binding/jellyfin/internal/handler/ClientHandlerTest.java`; run both reported classes using `specs/001-session-user-exposure/quickstart.md` and record results.

Discovery behavior is outside feature scope; its test compilation blocked the shared module build.

Supporting blocker repair was explicitly authorized on 2026-10-30: incomplete public-user
responses must not authorize stale Inbox cleanup. This is not session-user feature work.
Historical supporting repair evidence (2026-10-30): both reported classes compiled and passed:
`./mvnw -pl bundles/org.openhab.binding.jellyfin -Dtest=ClientHandlerTest,PublicUserDiscoveryServiceTest test`
ran 17 tests (9 handler, 8 discovery), with no failures, errors, or skipped tests.
Three separate post-format discovery runs also passed (8 tests each). See
[bug assessment](../../.specify/bugs/jellyfin-public-user-cleanup/assessment.md),
[fix report](../../.specify/bugs/jellyfin-public-user-cleanup/fix.md), and
[verification](../../.specify/bugs/jellyfin-public-user-cleanup/test.md).
The discovery repair is supporting work, not completion evidence for this feature.

Historical local-build and openHAB 5.2.3 smoke evidence is retained in
[the smoke-build guide](quickstart.md#historical-local-smoke-build-2026-10-30) and
[the runtime smoke report](../../.specify/bugs/jellyfin-public-user-cleanup/container-smoke.md).
That pre-migration run passed 319 tests and discovery smoke checks, but did not exercise the
identity channels and is not acceptance evidence for the current implementation.

## Phase 2: Foundational - Metadata Contract Checks

- [x] T003 Extend `bundles/org.openhab.binding.jellyfin/src/test/java/org/openhab/binding/jellyfin/internal/handler/ClientHandlerTest.java` with parsed XML checks for both client channel references, matching String channel types, and read-only state metadata; observe missing-metadata failures before XML changes (FR-001, FR-002, FR-005).

Reuse existing fixtures and XML parsers. Handler tests must assert observable state callbacks
with typed UIDs, initialized handlers, linked channels, and valid bridge state; stored-session
assertions alone do not verify channel publication. Do not hand-edit generated DTOs.

## Phase 3: User Story 1 - Read the Session User Name (P1)

**Goal**: Expose the supplied display name and clear a missing name.

**Independent test**: Exact name or `UnDefType.NULL`, including when only an identifier exists.

- [x] T004 [US1] Add name-state tests to `bundles/org.openhab.binding.jellyfin/src/test/java/org/openhab/binding/jellyfin/internal/util/client/ClientStateUpdaterTest.java` and `stateUpdated` callback assertions to `bundles/org.openhab.binding.jellyfin/src/test/java/org/openhab/binding/jellyfin/internal/handler/ClientHandlerTest.java`; cover normal, Unicode, empty, absent-name, and no-media sessions with an ONLINE client and bridge; observe failures (FR-001, FR-004). This task does not verify Thing status callbacks, status-detail mapping, or unresolved-bridge initialization; T010 owns those tests and T011 owns the status migration (FR-006, FR-007, SC-005).
- [x] T005 [US1] Add `user-name` to `bundles/org.openhab.binding.jellyfin/src/main/resources/OH-INF/thing/types/client-thing-type.xml` using compatible existing `user-name-channel` metadata in `bundles/org.openhab.binding.jellyfin/src/main/resources/OH-INF/thing/types/user-channel-types.xml`; preserve user-Thing compatibility (FR-001).
- [x] T006 [US1] Implement name calculation in `bundles/org.openhab.binding.jellyfin/src/main/java/org/openhab/binding/jellyfin/internal/util/client/ClientStateUpdater.java` and reconcile publication in `bundles/org.openhab.binding.jellyfin/src/main/java/org/openhab/binding/jellyfin/internal/handler/ClientHandler.java`; "Missing identity fields clear independently" and "Preserve supplied display names, including Unicode and an empty String"; rerun T003/T004 (FR-001, FR-004).

## Phase 4: User Story 2 - Read the Session User Identifier (P2)

**Goal**: Expose canonical UUID text independently of name availability.

**Independent test**: ID equals `UUID.toString()`; absent ID is NULL even with a name.

- [x] T007 [US2] Add typed UUID-present/absent state and callback tests to `bundles/org.openhab.binding.jellyfin/src/test/java/org/openhab/binding/jellyfin/internal/util/client/ClientStateUpdaterTest.java` and `bundles/org.openhab.binding.jellyfin/src/test/java/org/openhab/binding/jellyfin/internal/handler/ClientHandlerTest.java`; cover both partial-identity combinations and observe failures (FR-002, FR-004).
- [x] T008 [US2] Declare `user-id` in `bundles/org.openhab.binding.jellyfin/src/main/resources/OH-INF/thing/types/client-thing-type.xml` and add a matching read-only String `user-id-channel` type in `bundles/org.openhab.binding.jellyfin/src/main/resources/OH-INF/thing/types/client-channel-types.xml` (FR-002).
- [x] T009 [US2] Implement ID calculation in `bundles/org.openhab.binding.jellyfin/src/main/java/org/openhab/binding/jellyfin/internal/util/client/ClientStateUpdater.java` and reconcile `bundles/org.openhab.binding.jellyfin/src/main/java/org/openhab/binding/jellyfin/internal/handler/ClientHandler.java`; "UserId is a nullable UUID, not an arbitrary identifier String"; use `UUID.toString()` or NULL independently and rerun T003/T007 (FR-002, FR-004).

## Phase 5: User Story 3 - Avoid Stale or Writable Identity (P3)

**Goal**: Clear unavailable identity, recover with current values, and reject identity changes.

**Independent test**: Both states clear on recognized unavailability, recover on the next
valid session, and remain unaffected by ordinary channel commands.

- [x] T010 [US3] Extend `bundles/org.openhab.binding.jellyfin/src/test/java/org/openhab/binding/jellyfin/internal/handler/ClientHandlerEventIntegrationTest.java` and `bundles/org.openhab.binding.jellyfin/src/test/java/org/openhab/binding/jellyfin/internal/handler/ClientHandlerStatusCheckTest.java` with published-state and status-callback assertions for initial absence, anonymous session, switch, null-session end, timeout, bridge uninitialized/offline, and reconnect; assert resulting status details and that an unresolved bridge during initial setup does not publish a false error status, use bounded asynchronous checks, and observe failures (FR-003, FR-004, FR-007, SC-005).
- [x] T011 [US3] Repair identity invalidation/publication in `bundles/org.openhab.binding.jellyfin/src/main/java/org/openhab/binding/jellyfin/internal/handler/ClientHandler.java` and null mappings in `bundles/org.openhab.binding.jellyfin/src/main/java/org/openhab/binding/jellyfin/internal/util/client/ClientStateUpdater.java`; migrate detailed states to `updateStatus(ThingStatusDetail, description)` without the deprecated status-plus-detail overload, preserve UNINITIALIZED for an unresolved/uninitialized bridge, use `BRIDGE_OFFLINE` only when offline, and clear after recognized unavailable-bridge/session transitions while preserving selection/timeout behavior; then rerun T010 (FR-003, FR-004, FR-006, FR-007).
- [x] T012 [US3] Extend initialized-handler tests in `bundles/org.openhab.binding.jellyfin/src/test/java/org/openhab/binding/jellyfin/internal/handler/ClientHandlerTest.java` and routing tests in `bundles/org.openhab.binding.jellyfin/src/test/java/org/openhab/binding/jellyfin/internal/util/command/ClientCommandRouterTest.java` to verify ordinary identity commands cause no identity state change or remote identity-changing request and refresh republishes available identity without mutation; run metadata checks too (FR-005).
- [x] T013 [US3] Make only changes demonstrated necessary by T012 in `bundles/org.openhab.binding.jellyfin/src/main/java/org/openhab/binding/jellyfin/internal/handler/ClientHandler.java` or `bundles/org.openhab.binding.jellyfin/src/main/java/org/openhab/binding/jellyfin/internal/util/command/ClientCommandRouter.java`; if existing command behavior passes, record that evidence instead of adding redundant guards (FR-005).

## Final Phase: Polish and Verification

- [x] T014 [P] Document both channels, String Item links, and independent NULL behavior in `bundles/org.openhab.binding.jellyfin/README.md` (FR-001 through FR-005).
- [x] T015 Reconcile channel constants/type IDs and relevant Javadoc in `bundles/org.openhab.binding.jellyfin/src/main/java/org/openhab/binding/jellyfin/internal/Constants.java`, `bundles/org.openhab.binding.jellyfin/src/main/java/org/openhab/binding/jellyfin/channel/UserNameChannel.java`, and `bundles/org.openhab.binding.jellyfin/src/main/java/org/openhab/binding/jellyfin/channel/UserIdChannel.java` with XML; reuse existing conventions rather than parallel unused abstractions.
- [ ] T016 Run focused tests, Spotless, full module `clean install` against the repository's 5.3+ core baseline, and Markdown lint documented in `specs/001-session-user-exposure/quickstart.md`; inspect the diff for unrelated generated changes and record actual results in `specs/001-session-user-exposure/.spec-context.json` (FR-001 through FR-007, SC-001 through SC-005).
  - **Focused tests**: handler/discovery suite passed 26 tests; identity/lifecycle/command suite passed 54 tests (80 total, no failures, errors, or skips). The separate 11-test status regression run also passed.
  - **Spotless**: `./mvnw -pl bundles/org.openhab.binding.jellyfin spotless:apply` passed.
  - **Full module clean install**: `./mvnw -pl bundles/org.openhab.binding.jellyfin clean install` ran all 341 tests with no failures, errors, or skips, then failed Karaf feature verification because core semantics requires ASM >=9.10.0 while the supplied feature resources provide ASM 9.9.1. No override or verification skip was used.
  - **Markdown lint**: passed for all 9 feature Markdown files with 0 issues.
  - **Other checks**: feature prerequisites passed and `git diff --check` was clean.
  - **Status**: BLOCKED. The latest full module build ran 341 tests without failures, errors,
    or skips, but Karaf feature verification could not resolve `org.openhab.core.semantics`
    because its ASM requirement is `>=9.10.0` and the supplied feature resources provide
    ASM 9.9.1. Retrying with `-DwithResolver` failed at the same requirement. No resolver
    override or verification skip was used. Keep open until the unmodified clean install passes.
- [ ] T017 Perform and record interactive linked-Item acceptance from `specs/001-session-user-exposure/quickstart.md` on openHAB 5.3 or later. Do not deploy the migrated bundle to 5.2.3, which lacks the required status API; retain that runtime's results as pre-migration baseline evidence only (SC-001 through SC-005).
  - **Status**: BLOCKED. On 2026-11-08, the user reported the Jellyfin bundle ACTIVE at version `5.3.0.202611080927`, with its services and SCR components ACTIVE. A temporary client Thing was added and both `user-name` and `user-id` appeared as String channels. Temporary String Items were created and linked to the channels, but the client Thing stayed UNINITIALIZED with `HANDLER_INITIALIZING_ERROR` because the runtime could not resolve `ClientHandler.updateStatus(ThingStatusDetail, String)`. The Items displayed NULL, which is not valid identity acceptance evidence while the handler is uninitialized. The temporary Items and Thing were removed. No token, account, or identity values were recorded; no container restart was performed. Retry only on a runtime with a compatible openHAB core API.
- [ ] T018 Reconcile completed markers in `specs/001-session-user-exposure/tasks.md` and journal evidence in `specs/001-session-user-exposure/.spec-context.json` using Companion hooks; record disposition of the cross-artifact analysis findings and mark complete only when all acceptance/build gates actually pass.
  - **Status**: OPEN. T016's clean-install gate remains blocked and T017's linked-Item acceptance is blocked by the runtime API mismatch, so no further task markers can be completed and the feature remains incomplete.
  - **Constitution workflow note (2026-11-13)**: Cross-artifact analysis was performed after T001-T015 were completed, so it does not retroactively satisfy the constitution's pre-implementation gate. Resolve the reported findings before further implementation or closure, preserve the existing completion history, and run cross-artifact analysis before implementation in future feature workflows.
  - **Cross-artifact state (2026-11-08)**: FR-001 through FR-007 and SC-001 through SC-005
    remain mapped in the coverage table. Source, metadata, tests, and README implement the
    identity channels and status handling. A linked-Item attempt was made on the user-reported
    active 5.3 bundle, but the client handler did not initialize because the runtime could not
    resolve `ClientHandler.updateStatus(ThingStatusDetail, String)`. The visible NULL Item states
    are not acceptance evidence. Verify the actual runtime/core compatibility before retrying.
    Do not mark the behavior accepted based solely on a bundle or SCR component being ACTIVE.
  - **Evidence reconciliation**:

    | Gate | Verified outcome | Status |
    | ------ | ------------------ | -------- |
    | Focused tests | 26 handler/discovery and 54 identity/lifecycle/command tests passed | PASS |
    | Spotless | Wrapper formatter completed successfully | PASS |
    | Full module clean install | 341 tests passed; Karaf verification failed on ASM 9.9.1 vs >=9.10.0 requirement | BLOCKED |
    | Feature Markdown lint | 9 files, 0 issues | PASS |
    | Linked-Item acceptance | Channels appeared and temporary Items linked, but the client handler failed initialization with a missing core API method; test objects were removed | BLOCKED |

  - Companion context retains the verified run history; the after-implement hook must leave status as implementing while T016-T020 remain unchecked.
- [ ] T019 [US3] Extend `bundles/org.openhab.binding.jellyfin/src/test/java/org/openhab/binding/jellyfin/internal/handler/ClientHandlerStatusCheckTest.java` with status-mapping tests for invalid configuration and invalid bridge type; assert both produce `CONFIGURATION_ERROR` and record the results (FR-007, SC-005).
- [ ] T020 [US3] Extend `bundles/org.openhab.binding.jellyfin/src/test/java/org/openhab/binding/jellyfin/internal/handler/ClientHandlerStatusCheckTest.java` with focused tests verifying status-only states use the status-only API, detail-bearing states use the detail-derived API, and `ThingStatusDetail.NONE` is not passed to the detail-derived API (FR-006, SC-005).

## Dependencies and Execution Strategy

1. T001 and T002 restore compilation; T003 establishes failing metadata checks.
1. US1: T004 tests, then T005/T006 implementation and focused validation.
1. US2: T007 tests, then T008/T009 implementation and focused validation.
1. US3: T010 tests, T011 status and identity repair, T012 command checks, then T013 only if needed.
1. T014/T015 follow settled metadata; complete T019/T020 before T016, then T016 precedes interactive acceptance T017 and closure T018.

US1 is the smallest useful slice, but all stories are required for feature completion.
Story order avoids simultaneous edits to shared files. T014 can run in parallel with T015
once metadata is settled: they touch different files. Do not mark existing implementation
complete until its stated checks have run.

## Requirement Coverage

| Requirement | Tasks | Acceptance evidence |
| --- | --- | --- |
| FR-001 | T003-T006, T014-T017 | Exact name, absent name, metadata, linked Item |
| FR-002 | T003, T007-T009, T014-T017 | Canonical UUID, absent ID, metadata, linked Item |
| FR-003 | T010-T011, T016-T017 | Initial absence, anonymous/end/timeout/offline clearing |
| FR-004 | T004, T006-T007, T009-T011, T016-T017 | Partial identity, no media, switch, recovery |
| FR-005 | T003, T012-T013, T016-T017 | Read-only XML, ignored commands, non-mutating refresh |
| FR-006 | T010-T011, T016, T020 | Detail-derived API use; status-only updates and NONE handling |
| FR-007 | T010-T011, T016-T017, T019 | Bridge lifecycle, session, invalid configuration and bridge type status mapping |
| SC-001 | T004-T009, T016-T017 | Supplied values match |
| SC-002 | T004, T007, T010-T011, T016-T017 | No stale values after processed transition |
| SC-003 | T010-T011, T016-T017 | Current identity on next processed update |
| SC-004 | T003, T012-T013, T016-T017 | No identity-changing commands |
| SC-005 | T010-T011, T016-T017, T019-T020 | Correct status details, invalid configuration/type mapping, and no false missing-bridge error |
