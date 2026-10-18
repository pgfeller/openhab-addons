# Data Model - Session User Exposure

## Entities

### SessionInfoDto

The Jellyfin API model containing session information.

| Field | Type | Description | Nullable |
| ------- | ------ | ------------- | ---------- |
| `UserId` | `String` | Unique identifier for the authenticated user | Yes |
| `UserName` | `String` | Display name of the authenticated user | Yes |
| `DeviceId` | `String` | Identifier for the client device | Yes |
| `DeviceName` | `String` | Human-readable name of the client device | Yes |
| `IsActive` | `Boolean` | Whether the session is currently active | Yes |

### ClientThing

An openHAB Thing representing a Jellyfin client device/session.

| Property | Type | Description |
|----------|------|-------------|
| `thingUid` | `String` | Unique Thing identifier |
| `channels` | `Channel[]` | Collection of read-only channels |

### Channel

A read-only channel on a client Thing carrying session user information.

| Property | Type | Description |
| ---------- | ------ | ------------- |
| `name` | `String` | Channel name (`user-name` or `user-id`) |
| `type` | `String` | Channel type (`String`) |
| `readOnly` | `boolean` | Always true for these channels |
| `defaultValue` | `String` | NULL/empty when no session active |
| `updates` | `boolean` | Updated via session event bus |

### Session Event

An event dispatched by the session event bus when user identity changes.

| Property | Type | Description |
| ---------- | ------ | ------------- |
| `type` | `String` | Event type (`session-updated`, `session-ended`) |
| `userId` | `String` | New user ID, or null if session ended |
| `userName` | `String` | New user name, or null if session ended |
| `deviceId` | `String` | Associated device ID |

## Relationships

### SessionInfoDto → ClientThing

- Session data maps to ClientThing channels via the ClientHandler
- `UserId` → `user-id` channel state
- `UserName` → `user-name` channel state
- When `IsActive` is false, both channels clear to NULL

### ClientThing → Session Event Bus

- ClientHandler subscribes to session event bus
- On `session-updated`: updates `user-name` and `user-id` channel states
- On `session-ended`: sets both channels to NULL/empty

## State Transitions

### Active Session

- `user-id`: contains Jellyfin user UUID string
- `user-name`: contains Jellyfin user display name
- Both channels show valid user identity

### No Active Session

- `user-id`: NULL/empty string
- `user-name`: NULL/empty string
- Both channels indicate no authenticated user

### Session Change

- Channels update synchronously when session event bus dispatches new state
- Transitions happen in near real-time as per success criteria

## Channel State Transition Table

| Session State | user-id Channel | user-name Channel |
| -------------- | ---------------- | ------------------- |
| Active with user | UUID string | Display name |
| No session | NULL | NULL |
| Session ended | NULL | NULL |
| Anonymous session | NULL | NULL |

## Validation Rules

- `user-name` must be a non-null String when a user is authenticated
- `user-id` must be a non-null String when a user is authenticated
- Both channels must be NULL/empty when no session is active
- Channels must update within the event bus delivery latency when session state changes
