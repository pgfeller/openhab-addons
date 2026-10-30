# Container Smoke Test - Jellyfin

**Date**: 2026-10-30

**Result**: Startup, live bridge connectivity, client availability, and public-user discovery
smoke checks passed. Full session-user feature acceptance remains pending.

## Target and Baseline

- Container: `ad76edf434165470865f5f926a55278de8d0bb719cbfd03e06782b8d751c2556`.
- Name: `openhab-development-server`.
- Image and running runtime: openHAB 5.2.3 Debian, confirmed in the authenticated console.
- Java: OpenJDK 21.0.12.1.
- Add-ons, configuration, and userdata use persistent bind mounts.
- Before deployment, no Jellyfin bundle was installed and the add-ons directory was empty.
- Console authentication was entered directly in the terminal, not captured in this report.

## Candidate and Deployment

- Source commit: `95f3c66b7d`, including prior cleanup/fixture commit `97960f5eea`.
- Artifact: `bundles/org.openhab.binding.jellyfin/target/org.openhab.binding.jellyfin-5.3.0-SNAPSHOT.jar`.
- SHA-256: `3c263ec9ae6f59a00d89b3cb56574295f48e2945fdcc12e9befa8c1bc6d66a6d`.
- Deployment: copied to `/openhab/addons/org.openhab.binding.jellyfin-5.3.0-SNAPSHOT.jar`
  using `docker cp`, as requested. The destination checksum matched the candidate.
- Installed by the normal watched add-ons mechanism, not a second console installation.
- Observed bundle ID: `269`; version: `5.3.0.202610301015`.

## Checks and Observations

| Check | Command or evidence | Result |
| --- | --- | --- |
| Bundle startup | `bundle:list -s -t 0` | Candidate Active at start level 80 |
| Resolution | `bundle:diag 269` | No unresolved requirements reported |
| Service registration | `bundle:services 269` | API client factory, client/public-user discovery, DiscoveryService, and ThingHandlerFactory services registered |
| Component states | `scr:list 269` | API client factory, handler factory, server discovery Active; client and public-user discovery Satisfied |
| Initially configured Things | `openhab:things list`, before user setup | No Things found |
| Runtime log check | Last 200 lines of `/openhab/userdata/logs/openhab.log` | No Jellyfin, BundleException, NoClassDefFoundError, NoSuchMethodError, or unresolved-requirement matches |
| Installation integrity | Container-side `sha256sum` | Same checksum as local artifact |

The console table truncated symbolic names, so a text-filtered bundle list initially missed
the candidate. The complete inventory and numeric-ID service/component checks established
the actual state. This was an output-format issue, not an installation failure.

## Interactive Browser Follow-Up

**Window**: 2026-10-30, approximately 11:30-11:39 UTC, using the user-shared Main UI.
The user discovered and configured the server before these checks. Credentials and private
account labels were not copied into this report. Configuration and Code views were avoided
after the initial bridge status inspection because configuration snapshots expose credential fields.

| Check | Observation | Result |
| --- | --- | --- |
| Configured bridge | ONLINE in Main UI before and after the tests | Pass |
| Initial Inbox | Nine client results, no user results visible yet | Observed baseline |
| Temporary client setup | Approved the discovered browser client as `Smoke Test - Chrome`, retaining its discovered device ID and parent bridge | Pass |
| Live client availability | Initially OFFLINE with device-not-connected detail; later transitioned to ONLINE without a command from this test | Pass for observed session availability |
| Client channel metadata | Existing media/control channels render; filtering for `user` yields no matching channels | Confirms unfinished identity-channel declarations |
| Temporary Thing cleanup | Deleted only the temporary client; Things list returned to one ONLINE bridge | Pass |
| Rediscovery identity | Removed client reappeared in discovery under the same identity | Pass for this observation |
| Manual discovery | Started one normal scan; nine clients and two public users were displayed | Pass for successful live discovery |
| Duplicate check | Eleven discovered entries, eleven distinct Thing UIDs | No duplicates in this displayed result set |
| Scan cleanup | Stopped the initiated scan; the control returned to Scan | Pass |
| Final UI state | One configured ONLINE bridge and eleven Inbox entries | Preserved user setup |

The scan remained active beyond two bounded UI waits of 20 and 75 seconds. This is not
evidence of a hang: bridge-associated discovery services pass `DISCOVERY_RESULT_TTL_SEC`
(600 seconds) to their discovery-service constructors. Natural scan timeout was not tested;
the scan was stopped explicitly after results arrived. No timeout or discovery code was changed.

Browser event-stream requests were aborted during page transitions; subsequent UI reads and
live status updates succeeded. These navigation cancellations were not classified as binding
failures. Runtime file-log sampling produced no new lines during this interactive window;
it therefore provides limited additional error evidence, not proof that every path is error-free.

No Items or channel links were created, no playback/notification commands were sent, and no
remote accounts, login settings, or media sessions were intentionally changed. The temporary
client Thing was removed; its discovered Inbox entry was allowed to return normally. The two
public-user entries remain unapproved. No malformed or empty server responses were injected.

## Limits and Next Steps

- No newer openHAB snapshot is required based on this startup smoke test. This does not
  establish compatibility for every API path or unfinished feature behavior.
- The user-configured bridge and successful public-user/client discoveries were exercised
  against a live server. A temporary client reached ONLINE and was removed after inspection.
- Playback controls, linked Item state updates, user switching, timeout/offline clearing,
  and malformed/empty-response retention were not exercised against the live server.
- Malformed-response behavior retains the previously recorded automated regression evidence;
  this smoke test does not upgrade it to live end-to-end verification.
- Planned client `user-name` and `user-id` declarations and T003-T018 remain incomplete.
- The separate Maven Karaf verifier's 5.3 snapshot/ASM mismatch is not fixed by this test.
- No runtime dependencies, credentials, existing Thing configurations, or runtime version
  were changed by these tests. Only the temporary client was created and then removed.
- The test bundle and user-configured ONLINE bridge remain available. No newer openHAB
  snapshot is indicated by the observed checks. Complete the feature and its automated tests
  before using this environment for identity-channel acceptance.

## Rollback

Because no Jellyfin bundle was installed before this test, rollback is to remove only
`/openhab/addons/org.openhab.binding.jellyfin-5.3.0-SNAPSHOT.jar` and confirm that the watched
bundle disappears from the console inventory. Do not remove persistent configuration or userdata.
Rollback was not triggered because startup and service checks passed.
