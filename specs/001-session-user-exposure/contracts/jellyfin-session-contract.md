# Jellyfin Session Contract

## Overview

This contract maps existing Jellyfin session identity to two client channels.
It does not add an endpoint, event DTO, Thing type, or authentication behavior.

## Existing Session Input

The generated `SessionInfoDto` is the authoritative local API contract for this feature.
Jellyfin property names are PascalCase. Example object within a sessions response:

```json
{
  "UserId": "123e4567-e89b-12d3-a456-426614174000",
  "UserName": "Alice",
  "DeviceId": "living-room-client",
  "DeviceName": "Living Room",
  "IsActive": true
}
```

`UserId` and `UserName` may be absent or null independently. The Java setter for UserId
accepts `java.util.UUID`, not arbitrary text. Retain the binding's existing deserialization
and session selection. `IsActive` is not a new feature-specific clearing trigger.

The existing binding-local listener receives `onSessionUpdate(@Nullable SessionInfoDto)`;
a null payload means the selected session ended. Timeout and bridge-offline notifications
also invalidate exposed identity through existing lifecycle paths.

## openHAB Channel Contract

| Channel ID | Planned XML type ID | Item type | Read-only | Supplied value | Missing value |
| --- | --- | --- | --- | --- | --- |
| `user-name` | `user-name-channel` | String | true | Exact display name | `UnDefType.NULL` |
| `user-id` | `user-id-channel` | String | true | `UUID.toString()` | `UnDefType.NULL` |

Declare both channel references on the existing `client` Thing in
`src/main/resources/OH-INF/thing/types/client-thing-type.xml` under the binding module.
Channel-type metadata uses `<item-type>String</item-type>` and `<state readOnly="true"/>`.
The existing `user-name-channel` type is shared with the user Thing; preserve its compatibility.
Add only missing metadata in the existing XML files and reconcile Java helpers with these type IDs.

A typical bridged client channel UID has the form
`jellyfin:client:serverId:clientId:user-name`; use the actual discovered Thing UID.
No new `client-thing` Thing type or YAML Thing schema is defined.

## Commands and Refresh

Ordinary commands addressed to either identity channel must not alter reported identity
or cause identity-changing requests. `RefreshType.REFRESH` may republish the selected session's
identity using the existing handler behavior, without modifying identity or adding polling.
Tests must exercise an initialized handler so unsupported commands are not merely rejected
because a fixture is offline or lacks a command router.

## Contract Validation Rules

1. **FR-001 / SC-001**: Published name equals the supplied name; absent name publishes NULL.
1. **FR-002 / SC-001**: Published ID equals the canonical UUID text; absent ID publishes NULL.
1. **FR-003 / SC-002**: Initial absence, session end, timeout, and bridge-offline paths clear
   both channels, including after status has transitioned OFFLINE.
1. **FR-004 / SC-002 / SC-003**: User switch, partial identity, no current media item,
   and reconnect use the current snapshot; no stale identity or extra identity request cycle.
1. **FR-005 / SC-004**: Validate read-only XML and ignored commands separately; refresh
   republishes state without changing the user. Separate publications are not atomic.

Automated tests must assert observable channel-state callbacks, not only `getCurrentSession()`.
Interactive validation reads linked String Items; see [quickstart.md](../quickstart.md).
