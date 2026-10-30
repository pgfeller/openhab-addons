# Bug Assessment: Jellyfin Public-User Cleanup

- **Slug**: jellyfin-public-user-cleanup (auto-generated for authorized bounded execution)
- **Created**: 2026-10-30
- **Source**: pasted investigation and explicit implementation authorization
- **Verdict**: valid
- **Severity**: medium

## Report (Summarized)

The supplied investigation found that a response containing `new UserDto()` skips
publication but still authorizes stale Inbox cleanup. Existing negative tests watch
`thingRemoved`, whereas core cleanup emits `DiscoveryListener.removeOlderResults`.
The user authorized this supporting build/test blocker only, not T003+ feature work.

## Symptom

A malformed or incomplete public-user response can remove previously discovered
Inbox entries. Successful empty responses must still request scoped cleanup.

## Reproduction

1. Discover a valid public user on an online bridge.
1. Scan again with a DTO missing its ID.
1. Observe a second `removeOlderResults` callback despite the incomplete response.

## Suspected Code Paths

- `bundles/org.openhab.binding.jellyfin/src/main/java/org/openhab/binding/jellyfin/internal/discovery/PublicUserDiscoveryService.java`:
  `publishUsers` always returns true; `startScan` uses it to authorize cleanup.
- `bundles/org.openhab.binding.jellyfin/src/test/java/org/openhab/binding/jellyfin/internal/discovery/PublicUserDiscoveryServiceTest.java`:
  cleanup assertions observe the wrong callback; discovery callbacks are asynchronous.

## Root Cause Hypothesis

Confidence: high. Missing IDs are skipped without recording that the response was
incomplete. The supplied investigation checked AbstractDiscoveryService source
(5.0.3, line 369), PersistentInbox (line 415), and the installed 5.3.0-SNAPSHOT
binary: cleanup is delegated with cutoff, supported types, and bridge scope.

## Proposed Remediation

**Preferred**: Track response completeness at the existing publication boundary.
Retain publication of valid users in mixed responses, but authorize cleanup only
after the entire list has been processed without a missing ID. Preserve null-list
and API-failure behavior, and cleanup for genuinely successful empty lists.

**Files likely to change**:

- The discovery service and its existing test class identified above.
- Supporting bug reports and the current T002 record, without renumbering tasks.

**Tests to add or update**:

- Observe actual cleanup and distinguish first-scan cleanup from subsequent scans.
- Cover missing ID, mixed valid/invalid response, null response, and API error.
- Verify successful-empty cleanup cutoff, supported Thing types, and bridge scope.
- Make identity and label assertions independent of asynchronous callback order.

## Risks & Considerations

- Preserve valid-user publication, stable IDs, bridge association, and fallback labels.
- No speculative null-element behavior, new API, dependency, or session feature changes.
- Preserve existing fixture fixes, task markers, and Companion history.
- Automated listener-contract verification is not live Inbox/runtime acceptance.

## Open Questions

None within the explicitly authorized package.
