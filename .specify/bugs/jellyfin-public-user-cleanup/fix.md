# Bug Fix: Jellyfin Public-User Cleanup

- **Slug**: jellyfin-public-user-cleanup
- **Fixed**: 2026-10-30
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

The existing publication loop now tracks completeness across the entire response.
A missing user ID prevents stale-result cleanup without suppressing valid-user
publication. Empty successful responses still clean up; null responses and API
failures retain prior results. No null-element contract or new API was introduced.

## Changes

| File | Change | Notes |
| --- | --- | --- |
| `bundles/org.openhab.binding.jellyfin/src/main/java/org/openhab/binding/jellyfin/internal/discovery/PublicUserDiscoveryService.java` | Modified | Track completeness before authorizing cleanup |
| `bundles/org.openhab.binding.jellyfin/src/test/java/org/openhab/binding/jellyfin/internal/discovery/PublicUserDiscoveryServiceTest.java` | Modified | Actual cleanup callbacks, scope/cutoff, mixed/null responses, callback-order independence |
| `specs/001-session-user-exposure/tasks.md` | Modified | Close T002 only after both reported classes passed; supporting blocker record |
| `specs/001-session-user-exposure/.spec-context.json` | Writer updated | Add authorization and executed verification evidence; preserve history and implementing status |

Existing compilation-fixture changes in both reported test classes were preserved.
No additional handler edits were made. Existing Patrik Gfeller feature-specific
author tags already attribute both changed Java classes; no duplicate was added.

## Tests Added or Updated

- Malformed and API-failure tests observe `removeOlderResults`, not just
  `thingRemoved`, after synchronizing and clearing first-scan callbacks.
- Successful-empty test captures the scan cutoff bounded by scan invocation,
  and verifies the exact service, user Thing type set, and bridge UID.
- Mixed response publishes users before and after the invalid DTO without cleanup;
  a subsequent valid empty response proves cleanup recovery.
- Null list retains prior results. No speculative null-element behavior is tested.
- Multi-user and rename assertions retain identities, labels, and bridge checks
  without relying on asynchronous callback arrival order.

## Local Verification

- Red: `./mvnw -pl bundles/org.openhab.binding.jellyfin -Dtest=PublicUserDiscoveryServiceTest test`:
  6 tests, 3 failures, 0 errors/skipped before production changes. The regression
  observed two cleanup callbacks instead of one; other failures were callback
  order and the old empty-response expectation.
- First green: same command, 8 tests, 0 failures/errors/skipped.
- Formatting: `./mvnw -pl bundles/org.openhab.binding.jellyfin spotless:apply`, exit 0.
- Three independent post-format executions of the same discovery command, each exit 0,
  8 tests per execution; no Surefire retry masking was configured.
- Combined: `./mvnw -pl bundles/org.openhab.binding.jellyfin -Dtest=ClientHandlerTest,PublicUserDiscoveryServiceTest test`,
  exit 0; handler 9/9, discovery 8/8, total 17, 0 failures/errors/skipped.
- `get_errors`: no errors in service, discovery tests, or handler test fixtures.
- `git diff --check`: exit 0 at this checkpoint.
- Final scoped build, Markdown lint, and whitespace results belong in `test.md`.

## Deviations from Assessment

None. The user explicitly authorized this supporting blocker independently of the
session-user feature. T001 and all existing user changes were preserved.

## Follow-ups

- Keep T003-T018 open and feature status implementing.
- Full session-feature build/acceptance remains T016/T017; no live Jellyfin or
  PersistentInbox runtime acceptance is claimed by mocked listener tests.
