# Bug Verification: Jellyfin Public-User Cleanup

- **Slug**: jellyfin-public-user-cleanup
- **Tested**: 2026-10-30
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: partial

## Summary

The original malformed-response cleanup defect was reproduced before the fix and
no longer reproduces in the listener-contract tests. All selected discovery and
handler tests pass. Overall verification is partial because the module install
gate remains blocked by two pre-existing author-tag errors in untouched
session-channel classes, not by the cleanup change.

## Checks Performed

Commands below ran from the repository root. Companion records actual exit codes
and durations in `specs/001-session-user-exposure/.spec-context.json`.

| Check | Command / Action | Result | Notes |
| --- | --- | --- | --- |
| Pre-fix reproduction | `./mvnw -pl bundles/org.openhab.binding.jellyfin -Dtest=PublicUserDiscoveryServiceTest test` | Expected failure, exit 1 | 6 tests, 3 failures, 0 errors/skipped; malformed scan emits a second cleanup |
| First green | Same discovery command | Pass, exit 0 | 8 tests, 0 failures/errors/skipped |
| Repeat 1 | Same discovery command, after Spotless | Pass, exit 0 | 8 tests; 42.5 seconds wall time |
| Repeat 2 | Same discovery command, separate Maven execution | Pass, exit 0 | 8 tests; 43.3 seconds wall time |
| Repeat 3 | Same discovery command, separate Maven execution | Pass, exit 0 | 8 tests; 48.5 seconds wall time |
| Shared baseline | `./mvnw -pl bundles/org.openhab.binding.jellyfin -Dtest=ClientHandlerTest,PublicUserDiscoveryServiceTest test` | Pass, exit 0 | 17 tests: handler 9, discovery 8; 0 failures/errors/skipped |
| Formatting | `./mvnw -pl bundles/org.openhab.binding.jellyfin spotless:apply` | Pass, exit 0 | Three actual executions, including final newline repair |
| Initial scoped build | `./mvnw -pl bundles/org.openhab.binding.jellyfin -Dtest=ClientHandlerTest,PublicUserDiscoveryServiceTest clean install` | Fail, exit 1 | Selected 17 tests passed; static analysis exposed missing final newlines in touched files, repaired locally |
| Indentation follow-up | Discovery command above | Pass, exit 0 | 8 tests after aligning two new declarations |
| Newline follow-up | Discovery command above | Pass, exit 0 | 8 tests after restoring final newlines |
| Final scoped build | Same clean-install command | Fail, exit 1 | Selected 17 tests passed after final Spotless; JARs built; SAT reports only two pre-existing author-tag errors |
| Editor diagnostics | `get_errors` on service and both reported test classes | Pass | No errors |
| Markdown | `npx --yes markdownlint-cli2 --config .github/markdownlint.yaml --fix .specify/bugs/jellyfin-public-user-cleanup/*.md specs/001-session-user-exposure/tasks.md` | Recorded in Companion | All four changed Markdown files |
| Whitespace | `git diff --check` | Recorded in Companion | Final diff check |
| Feature selection | `.specify/scripts/bash/check-prerequisites.sh --json --require-spec --require-tasks --include-tasks` | Pass, exit 0 | Correct active feature; requirements checklist 16/16 checked |

The three repeated runs were independent actual executions, not Surefire retry
masking. Later formatting-only corrections were also followed by actual tests.
The final clean build recompiled the module and ran the combined 17-test baseline.

## Output Excerpts

Pre-fix regression: `removeOlderResults` wanted 1 invocation but was called 2 times.

Final Surefire reports:

```text
PublicUserDiscoveryServiceTest: Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
ClientHandlerTest: Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
```

Final scoped install:

```text
Code Analysis Tool has found: 2 error(s), 11 warning(s), 140 info(s)
org.openhab.binding.jellyfin.channel.UserIdChannel.java:[21] An author tag is missing
org.openhab.binding.jellyfin.channel.UserNameChannel.java:[21] An author tag is missing
BUILD FAILURE
```

The full final install output is retained locally at
`/tmp/jellyfin-cleanup-final-install.log`. Existing unused-import and other
nonfatal module findings were not cleaned up outside the authorized scope.

## Residual Risks

- This verifies the actual discovery cleanup callback, cutoff, supported type set,
  and bridge scope. It does not instantiate PersistentInbox or a live Jellyfin server.
- Valid mixed-response users still publish, including entries after an invalid DTO;
  unseen prior entries are protected by withholding cleanup. A later successful
  empty response resumes cleanup. Null lists and API errors do not authorize cleanup.
- The pre-existing session-channel author errors block a successful install.
  They belong to later feature work, not this cleanup repair.
- Full session-feature validation and interactive runtime acceptance remain
  T016/T017. No generated DTO, channel metadata, or feature implementation was changed.

## Recommendation

The bounded cleanup repair is ready for review with passing regression evidence.
Keep the overall module-install gate visibly blocked until the unrelated author
errors are addressed in their authorized task. T002 is complete because both
originally reported classes compile and pass; T003-T018 remain open. Preserve
feature status implementing and all prior history. No commit, push, branch, or
further agent was created.
