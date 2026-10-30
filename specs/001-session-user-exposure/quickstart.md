# Quickstart - Session User Exposure

This guide separates automated checks from interactive acceptance. Neither has been performed
as part of the SDD setup repair. Follow [tasks.md](tasks.md) before claiming implementation complete.

## Prerequisites

- Java 21 and the repository Maven wrapper for automated validation.
- A configured openHAB 5.x runtime and reachable Jellyfin server/client for interactive checks.
- The user-selected interactive runtime is openHAB 5.2.3. Confirm compatibility of the tested
 bundle before installation: the working module inherits `5.3.0-SNAPSHOT`.
- A Jellyfin server bridge with existing authentication configured securely, and an existing
 client Thing with the correct `serialNumber` device ID. Do not expose tokens in reports.
- A bundle built from the repaired implementation. The spec ID is not a runtime feature toggle.

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

## Interactive Setup

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
