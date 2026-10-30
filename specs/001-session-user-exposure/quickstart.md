# Quickstart - Session User Exposure

This guide separates the verified baseline build/runtime smoke test from unfinished identity-channel
acceptance. Start with [handoff.md](handoff.md) and [tasks.md](tasks.md) when resuming development.

## Prerequisites

- Java 21 and the repository Maven wrapper for automated validation.
- A configured openHAB 5.x runtime and reachable Jellyfin server/client for interactive checks.
- The user-selected runtime is openHAB 5.2.3. Startup, bridge connectivity, client availability,
 and discovery smoke checks passed for the tested `5.3.0-SNAPSHOT` bundle. Recheck newly built
 bundles; smoke success is not full feature acceptance.
- A Jellyfin server bridge with existing authentication configured securely, and an existing
 client Thing with the correct `serialNumber` device ID. Do not expose tokens in reports.
- A bundle built from the repaired implementation. The spec ID is not a runtime feature toggle.

## First Local Smoke Build - 2026-10-30

The compilation/discovery repairs are committed as `97960f5eea`. The missing author
attribution in the two session-channel helper classes has also been restored. Local packaging
succeeds with all 319 module tests passing, with no failures, errors, or skipped tests:

```sh
./mvnw -pl bundles/org.openhab.binding.jellyfin package
```

The deployment artifact is
`bundles/org.openhab.binding.jellyfin/target/org.openhab.binding.jellyfin-5.3.0-SNAPSHOT.jar`.
Use the main JAR, not the sources JAR. This is a package-phase build, not a successful install.

Both `clean install` and `-U spotless:apply clean install` passed all 319 tests and reported
zero static-analysis errors, but failed subsequent Karaf feature verification. The supplied
5.3.0-SNAPSHOT runtime set requires ASM 9.10 through `org.openhab.core.semantics`, while its
resolved feature set supplies ASM 9.9.1. Refreshing snapshots did not resolve the mismatch.
No verifier skip flags, dependency overrides, or runtime changes were applied.

This artifact supports an initial discovery/regression smoke test only. The client Thing XML
still lacks the planned `user-name` and `user-id` declarations; T003-T018 are not complete.
It has since passed the baseline runtime smoke checks below on openHAB 5.2.3, but not session-user
acceptance or every compatibility path.

## Interactive Deployment and Console

Reinspect the target before each deployment. The verified development container is named
`openhab-development-server` (last ID prefix `ad76edf43416`) and exposes Main UI at
`http://localhost:8080`. Its persistent host mounts are `~/Temp/openhab/{addons,conf,userdata}`.

Build the main JAR and copy it to the watched add-ons folder, which is the user's preferred
installation/update method:

```sh
docker cp bundles/org.openhab.binding.jellyfin/target/org.openhab.binding.jellyfin-5.3.0-SNAPSHOT.jar openhab-development-server:/openhab/addons/org.openhab.binding.jellyfin-5.3.0-SNAPSHOT.jar
docker exec openhab-development-server sha256sum /openhab/addons/org.openhab.binding.jellyfin-5.3.0-SNAPSHOT.jar
docker exec -it openhab-development-server /openhab/runtime/bin/client -u openhab
```

Confirm the checksum against the local candidate. Back up an existing JAR before replacement;
avoid keeping multiple versions in the watched directory. Do not also install the same bundle
through the console. Enter the console password directly in the terminal, never in chat.
Noninteractive authentication failed in this environment; the interactive TTY succeeded.

In the authenticated console, discover the actual numeric bundle ID, then inspect it:

```text
bundle:list -s -t 0
bundle:diag <jellyfin-bundle-id>
bundle:services <jellyfin-bundle-id>
scr:list <jellyfin-bundle-id>
openhab:things list
```

Narrow console tables truncate symbolic names; a filtered list can miss the candidate.
Inspect the full inventory and use the numeric ID, not the historical value 269. `scr:list`
takes a positional bundle ID on this runtime, not `-b`. Satisfied delayed discovery components
are not the same as unsatisfied dependencies. Logout when checks are complete.

Do not proceed unless the binding resolves and becomes ACTIVE with its services available.
If requirements cannot be resolved on 5.2.3, restore the original bundle and use a compatible
test runtime; do not force resolution or weaken imports. Roll back a new add-on by removing only
its JAR; restore the backed-up JAR for an update. Never delete persistent configuration/userdata.

The installed candidate passed startup/service checks. The user then configured a live ONLINE
bridge; a temporary client reached ONLINE and was removed; discovery produced eleven unique
results (nine clients, two public users). The candidate and bridge remain installed. Read the
[runtime report](../../.specify/bugs/jellyfin-public-user-cleanup/container-smoke.md) for exact evidence.
Playback and malformed/empty-response retention were not tested against the live server.

## Browser Safety and Cleanup

- Reuse the shared browser tab. Avoid bridge Configuration/Code snapshots: token fields may be
 exposed. Never publish credentials, private account names, server addresses, or device IDs.
- Wait for rendered controls after navigation, not Framework7 loading skeletons. A discovery
 view lacked `.page-current`; use observed accessible roles rather than assuming that selector.
- Use explicit labels for authorized temporary Things, preserve discovered IDs/bridge references,
 and remove only those test-created Things/Items/links afterwards. Preserve the user's bridge.
- Do not send playback/notification commands or alter remote users/settings without permission.
- A normal scan can keep Stop Scanning visible after results arrive: bridge-associated discovery
 uses a 600-second timeout. Stop only the scan started for the test; shorter waits do not prove a hang.
- Inspect `/openhab/userdata/logs/openhab.log` with the actual timezone/timestamp coverage and
 redact before output. Docker stdout alone missed application messages. Quiet logs are limited evidence.

## Automated Validation

Run from the repository root, after the test fixture and feature tasks are implemented:

```sh
./mvnw -pl bundles/org.openhab.binding.jellyfin -Dtest=ClientHandlerTest,PublicUserDiscoveryServiceTest test
./mvnw -pl bundles/org.openhab.binding.jellyfin -Dtest=ClientStateUpdaterTest,ClientHandlerEventIntegrationTest,ClientHandlerStatusCheckTest,ClientCommandRouterTest test
./mvnw -pl bundles/org.openhab.binding.jellyfin spotless:apply
./mvnw -pl bundles/org.openhab.binding.jellyfin clean install
.specify/scripts/bash/check-prerequisites.sh --json --require-spec --require-tasks --include-tasks
npx --yes markdownlint-cli2 --config .github/markdownlint.yaml "specs/001-session-user-exposure/**/*.md"
```

The first command must compile all module tests and pass the two originally reported classes.
The subsequent tests must assert actual published identity states, including the cases below.
The module build must validate XML metadata and existing regressions, not only compilation.
No resolver change is planned; use `-DwithResolver` if implementation changes dependencies.

## Feature Acceptance Setup - Still Pending

1. Install the compatible tested bundle using the normal development deployment procedure.
1. In Main UI, confirm the Jellyfin server bridge and selected client are available.
1. Open the client's Channels tab. Confirm `user-name` and `user-id` exist as read-only
  String channels. Use the actual discovered UID, not an invented `client-thing` type.
1. Link each channel to a new String Item, for example `JellyfinSessionUserName` and
  `JellyfinSessionUserId`. Inspect those Items in Main UI during each scenario.

For a text-based configuration, adapt this example to the actual existing client UID:

```java
String JellyfinSessionUserName { channel="jellyfin:client:serverId:clientId:user-name" }
String JellyfinSessionUserId { channel="jellyfin:client:serverId:clientId:user-id" }
```

## Acceptance Scenarios

| Scenario | Expected linked Item states | Requirements |
| --- | --- | --- |
| Authenticated selected session | Exact display name; lowercase, hyphenated UUID | FR-001, FR-002 |
| User switches from A to B | Both values change to B after the update is processed | FR-004 |
| Name absent, ID present | Name NULL; current ID retained | FR-001, FR-002, FR-004 |
| ID absent, name present | ID NULL; current name retained | FR-001, FR-002, FR-004 |
| Anonymous session or initial absence | Both NULL | FR-003 |
| Session end recognized by binding | Both NULL, even after client becomes OFFLINE | FR-003 |
| Existing timeout expires | Both NULL | FR-003 |
| Bridge becomes offline | Both NULL | FR-003 |
| Session reconnects with current identity | Both current values, not the previous user's values | FR-004 |
| Playback stops but identity remains in session | Identity remains available | FR-004 |
| Identity-channel command attempted | No reported identity change or remote identity-changing request | FR-005 |
| Refresh while session is available | Same current identity; no user change | FR-005 |

Use the existing session update and timeout cadence; the feature adds no identity polling.
Some boundary payloads cannot be produced by normal Jellyfin UI interactions. Verify those
through deterministic automated fixtures rather than claiming they were manually exercised.

Read-only metadata is not proof of backend command rejection. Automated initialized-handler
tests must check that ordinary commands do not update state or invoke identity-changing API calls.
An Item may accept local updates independently of binding behavior; do not confuse those with
channel commands or a changed Jellyfin identity. Use fresh channel updates for comparison.

## Evidence and Completion

Record commands, exit codes, test counts, runtime/bundle versions, and acceptance outcomes in
Companion verification metadata. Keep task markers open for failed or unexecuted work.
Do not record credentials, tokens, or unnecessary personal identity in evidence.
Do not mark the feature complete until the automated and interactive acceptance gates are met.

## Troubleshooting

- Missing channels: confirm client XML references and matching channel-type metadata in the
 installed bundle; Java helper classes alone do not declare channels.
- NULL while authenticated: check the selected device/session and linked Items, then existing
 bridge and session-delivery diagnostics without printing credentials.
- Stale identity after offline transition: inspect clear-state callbacks, not only the stored
 session; an ONLINE guard must not suppress identity invalidation.
- Runtime install failure: check bundle requirements against the selected runtime version;
 do not change the stated validation target silently.
