# Feature Specification: Session User Exposure

**Issue:** openHAB Add-ons #21850
**Binding:** Jellyfin
**Status:** Ready for implementation

**Feature**: `001-session-user-exposure`

**Created**: 2026-10-18 | **Reviewed**: 2026-10-30

**Issue**: openHAB Add-ons #21850 | **Binding**: Jellyfin

**Status**: Specification reviewed; partially implemented, not yet verified

**Input**: Expose the current Jellyfin session user's name and identifier on client Things
so users can use the identity in rules and UI integrations without changing authentication.

## User Scenarios & Testing

### User Story 1 - Read the Session User Name (Priority: P1)

As a user, I want to read the current session user's display name so my rules and UI
can respond to the person using a Jellyfin client.

**Why this priority**: The name provides immediately useful session identity.

**Independent Test**: Link the client name channel to a String Item and compare its value
with the current Jellyfin session's display name, including a session with no name.

**Acceptance Scenarios**:

1. **Given** a session with a display name, **When** its current state is processed,
  **Then** `user-name` contains that exact name.
1. **Given** a previously named session, **When** the current session has no name,
  **Then** `user-name` becomes `NULL` rather than retaining the previous name.

### User Story 2 - Read the Session User Identifier (Priority: P2)

As a user, I want the session user's stable identifier so I can correlate session activity
with Jellyfin accounts independently of display-name changes.

**Why this priority**: A stable identifier supports account-specific automation.

**Independent Test**: Link the client identifier channel to a String Item and compare its
value with the session user's UUID, including a session with no identifier.

**Acceptance Scenarios**:

1. **Given** a session with a user UUID, **When** its current state is processed,
  **Then** `user-id` contains the UUID in lowercase, hyphenated text form.
1. **Given** a previous user identifier, **When** the current session has no identifier,
  **Then** `user-id` becomes `NULL` independently of the name.

### User Story 3 - Avoid Stale or Writable Identity (Priority: P3)

As a user, I want both identity channels to track the current session and remain read-only
so that rules cannot mistake old or manually supplied identity for the actual session user.

**Why this priority**: Clearing and write protection make the exposed identity trustworthy.

**Independent Test**: Start with known identity, switch users, end the session, and reconnect;
check both linked Items and attempt to send channel commands.

**Acceptance Scenarios**:

1. **Given** identity for user A, **When** the current session changes to user B,
  **Then** both channels reflect B after the update has been processed.
1. **Given** populated identity channels, **When** the binding recognizes that the session
  ended, timed out, or became unavailable because the bridge is offline,
  **Then** both channels become `NULL`.
1. **Given** cleared channels, **When** a valid session becomes available again,
  **Then** both channels reflect that session's supplied identity.
1. **Given** either identity channel, **When** a user attempts to command a new identity,
  **Then** the binding neither changes the reported identity nor requests an identity change
  from Jellyfin; the channel is advertised as read-only.

### Edge Cases

- An anonymous session has neither identity value: both channels are `NULL`.
- A session can supply only one identity field: preserve that field and clear the missing one.
- A present but empty display name remains an empty value; absence is represented by `NULL`.
- No session exists initially: linked identity Items must not report a previous session's values.
- Playback stopping does not itself imply logout; identity belongs to the session, not the media item.
- Multiple users or sessions retain the binding's existing device/session selection behavior;
 these channels expose only the selected session's primary user.
- The two channels need not be published atomically, but both must reflect the processed snapshot.

## Requirements

### Functional Requirements

- **FR-001**: Client Things MUST expose a read-only String `user-name` channel containing
 the selected session's display name, or `NULL` when that field is absent.
- **FR-002**: Client Things MUST expose a read-only String `user-id` channel containing
 the selected session user's lowercase, hyphenated UUID, or `NULL` when that field is absent.
- **FR-003**: Both channels MUST be `NULL` when no session is available, including initial
 absence, recognized session end, timeout, and bridge unavailability.
- **FR-004**: Both channels MUST reflect the latest processed session snapshot on user
 changes and reconnection, without requiring a separate identity polling cycle. Missing fields
 MUST clear independently. Playback inactivity alone MUST NOT clear identity.
- **FR-005**: Both channels MUST be advertised as read-only. Commands MUST NOT change their
 reported identity or cause Jellyfin identity-changing requests. Reading or refreshing identity
 MUST NOT change the session user.

### Key Entities

- **Client Thing**: An existing Jellyfin client represented in openHAB, associated with one
 selected session according to existing binding behavior.
- **Session identity**: The primary user's optional display name and optional stable UUID.
- **Identity channels**: The two read-only values available to linked Items, rules, and the UI.

## Success Criteria

### Measurable Outcomes

- **SC-001**: In all authenticated acceptance scenarios, the linked name and identifier
 exactly match the selected session's supplied values after its update is processed.
- **SC-002**: In all missing-field and unavailable-session scenarios, no previous identity
 survives in the affected linked Items after the transition is processed.
- **SC-003**: User-switch and reconnect scenarios expose the new identity on the next
 processed session update, without an additional identity refresh cycle.
- **SC-004**: All attempted identity-changing channel commands leave session identity unchanged,
 and both channels are shown as read-only in their metadata.

## Assumptions

- The existing binding already obtains session identity and associates sessions with clients.
- A configured Jellyfin server and client are available for interactive validation.
- Session end is measured from the binding's recognition of unavailability, not the instant
 the remote application closes. Existing timeout and delivery behavior are unchanged.
- A valid session may lack either identity field; fields are interpreted independently.
- Users link the channels to String Items for rules and UI use.

## Non-Goals

- No authentication, login, account switching, or account management feature.
- No new user Things or user discovery behavior; existing discovery remains out of scope.
- No change to device matching, session selection, polling intervals, or timeout thresholds.
- No replacement of the binding's existing session management or transport.
