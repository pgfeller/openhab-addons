# Data Model - Session User Exposure

## Entities

### SessionInfoDto

Existing generated model; do not create or hand-edit a duplicate.

Source: `bundles/org.openhab.binding.jellyfin/src/3rdparty/java/org/openhab/binding/jellyfin/internal/gen/current/model/SessionInfoDto.java`.

| JSON field | Java type | Feature interpretation |
| --- | --- | --- |
| `UserId` | nullable `java.util.UUID` | Primary session user identifier; stringify with `UUID.toString()` |
| `UserName` | nullable `String` | Primary session user's display name, preserved verbatim |
| `DeviceId` | nullable `String` | Existing device/session association; unchanged |
| `DeviceName` | nullable `String` | Existing device display information; unchanged |
| `IsActive` | nullable `Boolean` | Existing session metadata; not a new identity-clearing predicate |

### Client Thing

Existing `jellyfin:client` Thing managed by `ClientHandler`; no new `ClientThing` class.
Its UID is an openHAB `ThingUID`, not a String returned by `Thing.getUID()`.
The `serialNumber` configuration supplies the existing device ID used for subscription.

### Identity Channel States

| Channel ID | Item type | Present value | Absent field or session |
| --- | --- | --- | --- |
| `user-name` | String | `StringType(userName)` | `UnDefType.NULL` |
| `user-id` | String | `StringType(userId.toString())` | `UnDefType.NULL` |

Read-only status belongs to XML channel-type `<state readOnly="true"/>` metadata and
unsupported command handling, not a `defaultValue` String property. `NULL` is an openHAB
undefined state, not Java null, the text `"NULL"`, or an empty-string substitute.

### Session Delivery

The binding-local `SessionEventBus` routes a nullable `SessionInfoDto` to
`SessionEventListener.onSessionUpdate(...)` using the existing device ID.
There is no new event DTO or string-valued `session-updated`/`session-ended` event type.
A null callback payload signals session end.

## Relationships

1. Existing session selection supplies the snapshot for a client device.
1. `ClientHandler` receives the nullable snapshot through the existing listener interface.
1. `ClientStateUpdater.calculateChannelStates(...)` calculates both identity states from
 that same snapshot; `ClientHandler` publishes them through openHAB callbacks.
1. Existing timeout and bridge-status paths must also clear the identity channels.

The updater is the planned calculation owner; its identity mapping is not yet implemented.

## State Transitions

| Processed snapshot or transition | user-id | user-name |
| --- | --- | --- |
| Both identity fields supplied | Canonical UUID text | Exact display name |
| Only name supplied | NULL | Exact display name |
| Only identifier supplied | Canonical UUID text | NULL |
| Anonymous session | NULL | NULL |
| Initial absence, null session, timeout, or offline bridge | NULL | NULL |
| Session changes from A to B | B's supplied UUID or NULL | B's supplied name or NULL |
| Session recovers after unavailability | Current UUID or NULL | Current name or NULL |

Updates share one source snapshot but are separate channel publications, not an atomic transaction.

## Validation Rules

- UserId is a nullable UUID, not an arbitrary identifier String.
- Missing identity fields clear independently; do not infer one from the other.
- Preserve supplied display names, including Unicode and an empty String.
- Playback inactivity and `IsActive=false` alone do not introduce new logout semantics.
- Existing UUID deserialization remains responsible for wire-format validation; no new parser.
- No persistence, account lookup, new session selector, or identity cache is introduced.
