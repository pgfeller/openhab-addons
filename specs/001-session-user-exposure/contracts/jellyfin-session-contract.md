# Jellyfin Session Contract

## Overview

This contract defines the expected session data format from the Jellyfin API and the corresponding openHAB channel contract.

## Jellyfin API Session Response

### SessionInfoDto

The JSON response format from `GET /Sessions` endpoint.

```json
{
  "userId": "string",     // UUID of the authenticated user, nullable
  "userName": "string",   // Display name of the user, nullable
  "deviceId": "string",   // Identifier for the client device
  "deviceName": "string", // Human-readable device name
  "isActive": boolean     // Whether the session is currently active
}
```

## openHAB Channel Contract

### user-name Channel

| Aspect | Specification |
| -------- | -------------- |
| **Channel Type** | `String` |
| **Read-Only** | `true` |
| **Default Value** | `NULL` (when no session active) |
| **Update Source** | Session event bus |
| **Expected Value** | Non-null display name when user authenticated |
| **Null Behavior** | NULL indicates no authenticated user |

### user-id Channel

| Aspect | Specification |
| -------- | -------------- |
| **Channel Type** | `String` |
| **Read-Only** | `true` |
| **Default Value** | `NULL` (when no session active) |
| **Update Source** | Session event bus |
| **Expected Value** | UUID string when user authenticated |
| **Null Behavior** | NULL indicates no authenticated user |

## Thing Configuration Contract

### Client Thing Definition

```yaml
uid: org.openhab.binding.jellyfin:client-thing
label: Jellyfin Client Session
description: Represents a Jellyfin client session with user identity channels

channels:
  - id: user-name
    type: String
    label: User Name
    description: Display name of the authenticated Jellyfin user
    readOnly: true
    tags: [user, identity]

  - id: user-id
    type: String
    label: User ID
    description: UUID of the authenticated Jellyfin user
    readOnly: true
    tags: [user, identity]
```

## Contract Validation Rules

1. **user-name channel**: When `userName` is present in Jellyfin session, channel state must match; when absent, channel must be NULL
2. **user-id channel**: When `userId` is present in Jellyfin session, channel state must match UUID format; when absent, channel must be NULL
3. **Null consistency**: Both channels must be NULL simultaneously when no session is active
4. **Update consistency**: Both channels must update atomically when session event bus dispatches new state
