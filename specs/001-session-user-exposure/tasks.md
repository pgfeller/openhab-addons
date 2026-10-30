# Tasks - Session User Exposure

**Input**: [spec.md](spec.md), [plan.md](plan.md), and supporting design artifacts.

**Reconciled**: 2026-10-30. This backlog replaces the unexecuted scaffolding list with
repository-grounded work. No completed task markers or task completion history existed to migrate.
IDs below identify this repaired backlog. Completion markers reflect executed evidence;
unverified or blocked tasks remain open.

## Existing Baseline

The module, Java 21 configuration, generated DTO, SLF4J, and session subscription already exist.
The handler contains partial identity publication and Java channel helpers exist. These are
source-inspection findings, not test results. Client XML lacks identity channel references and
the pure state updater lacks identity mappings. Do not recreate the module, DTO, factory, or
logging configuration. The SDD setup repair does not claim implementation completion.

## Phase 1: Setup - Restore Test Compilation

- [x] T001 Fix non-null fixture initialization in `bundles/org.openhab.binding.jellyfin/src/test/java/org/openhab/binding/jellyfin/internal/discovery/PublicUserDiscoveryServiceTest.java` without suppressing null analysis; preserve discovery behavior.
- [x] T002 Fix typed Thing/ThingUID and UUID fixtures in `bundles/org.openhab.binding.jellyfin/src/test/java/org/openhab/binding/jellyfin/internal/handler/ClientHandlerTest.java`; run both reported classes using `specs/001-session-user-exposure/quickstart.md` and record results.

Discovery behavior is outside feature scope; its test compilation blocks the shared module build.

Supporting blocker repair was explicitly authorized on 2026-10-30: incomplete public-user
responses must not authorize stale Inbox cleanup. This is not session-user feature work.
The existing fixture repairs were preserved. Both reported classes now compile and pass:
`./mvnw -pl bundles/org.openhab.binding.jellyfin -Dtest=ClientHandlerTest,PublicUserDiscoveryServiceTest test`
ran 17 tests (9 handler, 8 discovery), with no failures, errors, or skipped tests.
Three separate post-format discovery runs also passed (8 tests each). See
[bug assessment](../../.specify/bugs/jellyfin-public-user-cleanup/assessment.md),
[fix report](../../.specify/bugs/jellyfin-public-user-cleanup/fix.md), and
[verification](../../.specify/bugs/jellyfin-public-user-cleanup/test.md).
T003-T018 remain open; the feature remains implementing.

Local-build preparation was authorized on 2026-10-30. Missing author tags in the two
session-channel helper classes were restored. The full 319-test module suite passes, and
`package` succeeds. Static analysis reports zero errors. Full `install` is still blocked
by the supplied runtime snapshot's ASM 9.10 requirement versus its ASM 9.9.1 feature set,
including after a snapshot refresh. See [the smoke-build guide](quickstart.md#first-local-smoke-build---2026-10-30).
No runtime deployment or session-user acceptance was performed; T015-T018 remain open.

## Phase 2: Foundational - Metadata Contract Checks

- [ ] T003 Extend `bundles/org.openhab.binding.jellyfin/src/test/java/org/openhab/binding/jellyfin/internal/handler/ClientHandlerTest.java` with parsed XML checks for both client channel references, matching String channel types, and read-only state metadata; observe missing-metadata failures before XML changes (FR-001, FR-002, FR-005).

Reuse existing fixtures and XML parsers. Handler tests must assert observable state callbacks
with typed UIDs, initialized handlers, linked channels, and valid bridge state; stored-session
assertions alone do not verify channel publication. Do not hand-edit generated DTOs.

## Phase 3: User Story 1 - Read the Session User Name (P1)

**Goal**: Expose the supplied display name and clear a missing name.

**Independent test**: Exact name or `UnDefType.NULL`, including when only an identifier exists.

- [ ] T004 [US1] Add name-state tests to `bundles/org.openhab.binding.jellyfin/src/test/java/org/openhab/binding/jellyfin/internal/util/client/ClientStateUpdaterTest.java` and callback assertions to `bundles/org.openhab.binding.jellyfin/src/test/java/org/openhab/binding/jellyfin/internal/handler/ClientHandlerTest.java`; cover normal, Unicode, empty, absent-name, and no-media sessions; observe failures (FR-001, FR-004).
- [ ] T005 [US1] Add `user-name` to `bundles/org.openhab.binding.jellyfin/src/main/resources/OH-INF/thing/types/client-thing-type.xml` using compatible existing `user-name-channel` metadata in `bundles/org.openhab.binding.jellyfin/src/main/resources/OH-INF/thing/types/user-channel-types.xml`; preserve user-Thing compatibility (FR-001).
- [ ] T006 [US1] Implement name calculation in `bundles/org.openhab.binding.jellyfin/src/main/java/org/openhab/binding/jellyfin/internal/util/client/ClientStateUpdater.java` and reconcile publication in `bundles/org.openhab.binding.jellyfin/src/main/java/org/openhab/binding/jellyfin/internal/handler/ClientHandler.java`; "Missing identity fields clear independently" and "Preserve supplied display names, including Unicode and an empty String"; rerun T003/T004 (FR-001, FR-004).

## Phase 4: User Story 2 - Read the Session User Identifier (P2)

**Goal**: Expose canonical UUID text independently of name availability.

**Independent test**: ID equals `UUID.toString()`; absent ID is NULL even with a name.

- [ ] T007 [US2] Add typed UUID-present/absent state and callback tests to `bundles/org.openhab.binding.jellyfin/src/test/java/org/openhab/binding/jellyfin/internal/util/client/ClientStateUpdaterTest.java` and `bundles/org.openhab.binding.jellyfin/src/test/java/org/openhab/binding/jellyfin/internal/handler/ClientHandlerTest.java`; cover both partial-identity combinations and observe failures (FR-002, FR-004).
- [ ] T008 [US2] Declare `user-id` in `bundles/org.openhab.binding.jellyfin/src/main/resources/OH-INF/thing/types/client-thing-type.xml` and add a matching read-only String `user-id-channel` type in `bundles/org.openhab.binding.jellyfin/src/main/resources/OH-INF/thing/types/client-channel-types.xml` (FR-002).
- [ ] T009 [US2] Implement ID calculation in `bundles/org.openhab.binding.jellyfin/src/main/java/org/openhab/binding/jellyfin/internal/util/client/ClientStateUpdater.java` and reconcile `bundles/org.openhab.binding.jellyfin/src/main/java/org/openhab/binding/jellyfin/internal/handler/ClientHandler.java`; "UserId is a nullable UUID, not an arbitrary identifier String"; use `UUID.toString()` or NULL independently and rerun T003/T007 (FR-002, FR-004).

## Phase 5: User Story 3 - Avoid Stale or Writable Identity (P3)

**Goal**: Clear unavailable identity, recover with current values, and reject identity changes.

**Independent test**: Both states clear on recognized unavailability, recover on the next
valid session, and remain unaffected by ordinary channel commands.

- [ ] T010 [US3] Extend `bundles/org.openhab.binding.jellyfin/src/test/java/org/openhab/binding/jellyfin/internal/handler/ClientHandlerEventIntegrationTest.java` and `bundles/org.openhab.binding.jellyfin/src/test/java/org/openhab/binding/jellyfin/internal/handler/ClientHandlerStatusCheckTest.java` with published-state assertions for initial absence, anonymous session, switch, null-session end, timeout, bridge offline, and reconnect; bound asynchronous checks and observe failures (FR-003, FR-004).
- [ ] T011 [US3] Repair identity invalidation/publication in `bundles/org.openhab.binding.jellyfin/src/main/java/org/openhab/binding/jellyfin/internal/handler/ClientHandler.java` and null mappings in `bundles/org.openhab.binding.jellyfin/src/main/java/org/openhab/binding/jellyfin/internal/util/client/ClientStateUpdater.java`; clear after OFFLINE transitions, preserve unrelated guards and selection/timeout behavior, then rerun T010 (FR-003, FR-004).
- [ ] T012 [US3] Extend initialized-handler tests in `bundles/org.openhab.binding.jellyfin/src/test/java/org/openhab/binding/jellyfin/internal/handler/ClientHandlerTest.java` and routing tests in `bundles/org.openhab.binding.jellyfin/src/test/java/org/openhab/binding/jellyfin/internal/util/command/ClientCommandRouterTest.java` to verify ordinary identity commands cause no identity state change or remote identity-changing request and refresh republishes available identity without mutation; run metadata checks too (FR-005).
- [ ] T013 [US3] Make only changes demonstrated necessary by T012 in `bundles/org.openhab.binding.jellyfin/src/main/java/org/openhab/binding/jellyfin/internal/handler/ClientHandler.java` or `bundles/org.openhab.binding.jellyfin/src/main/java/org/openhab/binding/jellyfin/internal/util/command/ClientCommandRouter.java`; if existing command behavior passes, record that evidence instead of adding redundant guards (FR-005).

## Final Phase: Polish and Verification

- [ ] T014 [P] Document both channels, String Item links, and independent NULL behavior in `bundles/org.openhab.binding.jellyfin/README.md` (FR-001 through FR-005).
- [ ] T015 Reconcile channel constants/type IDs and relevant Javadoc in `bundles/org.openhab.binding.jellyfin/src/main/java/org/openhab/binding/jellyfin/internal/Constants.java`, `bundles/org.openhab.binding.jellyfin/src/main/java/org/openhab/binding/jellyfin/channel/UserNameChannel.java`, and `bundles/org.openhab.binding.jellyfin/src/main/java/org/openhab/binding/jellyfin/channel/UserIdChannel.java` with XML; reuse existing conventions rather than parallel unused abstractions.
- [ ] T016 Run focused tests, Spotless, full module `clean install`, and Markdown lint documented in `specs/001-session-user-exposure/quickstart.md`; inspect the diff for unrelated generated changes and record actual results in `specs/001-session-user-exposure/.spec-context.json` (FR-001 through FR-005, SC-001 through SC-004).
- [ ] T017 Perform and record interactive linked-Item acceptance from `specs/001-session-user-exposure/quickstart.md`, confirming bundle compatibility with the chosen openHAB 5.2.3 runtime first; keep this task open if that environment is unavailable (SC-001 through SC-004).
- [ ] T018 Reconcile completed markers in `specs/001-session-user-exposure/tasks.md` and journal evidence in `specs/001-session-user-exposure/.spec-context.json` using Companion hooks; run cross-artifact analysis and mark complete only when all acceptance/build gates actually pass.

## Dependencies and Execution Strategy

1. T001 and T002 restore compilation; T003 establishes failing metadata checks.
1. US1: T004 tests, then T005/T006 implementation and focused validation.
1. US2: T007 tests, then T008/T009 implementation and focused validation.
1. US3: T010 tests, T011 repair, T012 command checks, then T013 only if needed.
1. T014/T015 follow settled metadata; T016 precedes interactive acceptance T017 and closure T018.

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
| SC-001 | T004-T009, T016-T017 | Supplied values match |
| SC-002 | T004, T007, T010-T011, T016-T017 | No stale values after processed transition |
| SC-003 | T010-T011, T016-T017 | Current identity on next processed update |
| SC-004 | T003, T012-T013, T016-T017 | No identity-changing commands |
