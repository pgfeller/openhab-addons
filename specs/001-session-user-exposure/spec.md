# Feature Specification: Session User Exposure

**Issue:** openHAB Add-ons #21850
**Binding:** Jellyfin
**Status:** Ready for implementation

## Objective

Expose the Jellyfin session's `UserName` and `UserId` as read-only channels on client Things in the openHAB Jellyfin binding. This allows users to access the currently logged-in user's identity within their openHAB rules and UI integrations.

The specification covers adding read-only String channels for `user-name` and `user-id` to client Things, derived from the session information already available in the binding's session management.

## User Stories

1. As a user, I want to see the currently logged-in Jellyfin user's name in my openHAB rules so I can conditionally show/hide UI elements based on user identity.
2. As a user, I want to access the session UserId so I can correlate openHAB sessions with Jellyfin user accounts.
3. As a developer, I want read-only channels that automatically clear when a session ends, ensuring stale data is not persisted.

## Assumptions

- The Jellyfin server's `GET /Sessions` endpoint returns session objects containing `UserId` and `UserName` fields (nullable).
- The current binding already associates sessions with client Things using the `DeviceId` (from `serialNumber` configuration).
- Session data is available through the existing session event bus architecture.
- UserName and UserId may be null/absent for anonymous sessions or when the user is not authenticated.

## User Scenarios & Testing

### Scenario 1: User identifies the current session user
**Given:** A Jellyfin session is active with a known user
**When:** The user queries the `user-name` channel on their client Thing
**Then:** The channel state contains the Jellyfin user's display name (non-null string)

### Scenario 2: Session user is null/anonymous
**Given:** A Jellyfin session with no authenticated user
**When:** The user queries the `user-name` channel
**Then:** The channel state is `NULL` or empty string, indicating no authenticated user

### Scenario 3: Session transitions
**Given:** A session starts, user changes, or session ends
**When:** The session event bus dispatches a new session state
**Then:** Both `user-name` and `user-id` channels are updated accordingly, clearing when no session is active

## Functional Requirements

| # | Requirement | Testable |
|---|------------|----------|
| FR1 | Client Things expose a `user-name` channel (String type, read-only) | Read the channel state after session event |
| FR2 | Client Things expose a `user-id` channel (String type, read-only) | Read the channel state after session event |
| FR3 | When no session is active, both channels show NULL/empty | Verify channel state after session ends |
| FR4 | Channel values update when session UserName/UserId changes | Monitor channel state after session update |
| FR5 | Channels are read-only and cannot be modified by users | Check channel write permissions |

## Success Criteria

- [ ] `user-name` channel displays the Jellyfin user's display name when authenticated
- [ ] `user-id` channel displays the Jellyfin user's UUID when authenticated
- [ ] Both channels show NULL/empty when no user is authenticated
- [ ] Channels update in near real-time when session state changes
- [ ] No implementation details (languages, frameworks, APIs) leak into the specification
- [ ] All requirements are testable and unambiguous
- [ ] Success criteria are measurable and technology-agnostic
- [ ] scope is clearly bounded to session user identity exposure

## Key Entities

- **SessionInfoDto**: Jellyfin API model containing `UserId`, `UserName`, `DeviceId`, `DeviceName`, `IsActive`
- **ClientThing**: openHAB Thing representing a Jellyfin client device/session
- **Session Event Bus**: Internal mechanism for dispatching session state changes
- **ClientHandler**: Binding component that updates Thing state from session events

## Channel Design

| Channel | Type | Read-only | Default when null | Description |
|---------|------|-----------|-------------------|-------------|
| `user-name` | String | Yes | NULL/empty string | Display name of the authenticated Jellyfin user |
| `user-id` | String | Yes | NULL/empty string | UUID of the authenticated Jellyfin user |

## API Integration Details

The specification does not define implementation details, but the following describes the expected data flow:

- `ClientHandler.initialize()` reads `serialNumber` from Thing configuration, stores as `deviceId`
- `SessionManager` groups and dispatches sessions using each session's `DeviceId`
- When a session is received, `ClientHandler.updateStateFromSession()` should populate `user-name` and `user-id` channels from the session's `UserName` and `UserId` fields
- Channels should be cleared (set to NULL) when no active session exists or when the session's UserName/UserId is null

## Non-Goals (Out of Scope)

- Do not implement user authentication or login flows
- Do not modify Jellyfin server API or authentication behavior
- Do not create new Things for users (this is about session user exposure, not user discovery)
- Do not change device matching logic or session association

## Requirements Checklist

- [ ] No [NEEDS CLARIFICATION] markers remain
- [ ] Requirements are testable and unambiguous
- [ ] Success criteria are measurable
- [ ] Success criteria are technology-agnostic
- [ ] All acceptance scenarios are defined
- [ ] Edge cases are identified
- [ ] Scope is clearly bounded
- [ ] Dependencies and assumptions identified

## Notes

- This specification focuses on exposing existing session data as read-only channels
- Does not modify the binding's core session management or device association logic
- Channel values should be treated as read-only snapshots of the current session state
- Edge cases: anonymous sessions, multiple additional users, session transitions