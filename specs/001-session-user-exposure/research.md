# Research.md - Session User Exposure

## Research Tasks

### 1. Jellyfin API Session User Info Integration

**Task**: Research best practices for integrating Jellyfin API session user information (`UserName`, `UserId`) into openHAB bindings.

- Investigate Jellyfin API endpoints for session data (`GET /Sessions`)
- Understand the structure of session response objects (`SessionInfoDto`)
- Determine how session data is currently fetched and cached in the Jellyfin binding
- Research patterns for exposing nullable API fields as openHAB channels

**Decision**: Use existing session event bus architecture to propagate user info to client Things

**Rationale**: The binding already associates sessions with client Things via DeviceId; leveraging existing architecture minimizes duplication

**Alternatives Considered**:

- Direct API calls per channel update (higher overhead, less coherent)
- Separate session management component (increased complexity, unnecessary indirection)

---

### 2. openHAB Binding Read-Only Channel Design Patterns

**Task**: Research openHAB binding patterns for read-only String channels that auto-clear when inactive.

- Investigate openHAB Channel UIDs and configuration patterns
- Understand how to define read-only channels in Thing configuration
- Research null/empty state handling for optional channels
- Look at existing bindings with similar optional channel patterns

**Decision**: Use String channel type with NULL/empty default when no session active; leverage Thing configuration `readOnly` attribute

**Rationale**: OpenHAB supports read-only channels; null state clearly indicates unauthenticated session

**Alternatives Considered**:

- Number channel for user ID (loses semantic meaning, can't represent UUID string)
- Boolean channel (insufficient for user identity information)

---

### 3. Session Event Bus Architecture in openHAB

**Task**: Research the openHAB session event bus architecture and how bindings propagate state changes to client Things.

- Investigate `SessionEventBus` and `SessionEvent` classes
- Understand how bindings register session listeners
- Research patterns for updating Thing state from session events
- Look at existing bindings that use event bus for state propagation

**Decision**: Use the existing session event bus architecture; ClientHandler updates Thing channels on session events

**Rationale**: The spec assumes this architecture already exists; leveraging it minimizes new code

**Alternatives Considered**:

- Polling Jellyfin API for session state (inefficient, higher latency)
- Direct WebSocket connections (not consistent with openHAB patterns)

---

### 4. Jellyfin Binding Existing Code Patterns

**Task**: Research the existing Jellyfin binding code structure and session management patterns.

- Investigate existing binding source code at `org.openhab.binding.jellyfin`
- Understand how sessions are currently managed
- Research ClientHandler and session event patterns in the codebase
- Look at how other channels are defined and updated

**Decision**: Follow existing binding patterns for channel definition and session event handling

**Rationale**: Consistency with existing code reduces errors and integration effort

**Alternatives Considered**:

- Rewrite binding architecture (excessive for this feature scope)
- Different event propagation mechanism (deviates from established patterns)

---

## Consolidated Findings

### Research Summary

- **API Integration**: Leverage existing Jellyfin session event bus; no need for direct per-channel API calls
- **Channel Design**: String type with NULL defaults is the correct pattern for optional user identity fields
- **Event Architecture**: Existing session event bus is the established pattern; ClientHandler updates Thing state
- **Code Consistency**: Follow existing Jellyfin binding patterns for minimal diff and easier maintenance

### Key Decisions

1. Use `user-name` and `user-id` as String read-only channels on client Things
2. Channels show NULL/empty when no session is active
3. Channels update via session event bus when user identity changes
4. Follow existing Jellyfin binding code patterns for implementation

### Alternatives Considered and Rejected

- Direct API polling: Higher overhead, less coherent state management
- Custom event system: Duplicates existing openHAB session bus
- Number channel for user ID: Loses semantic meaning, can't represent arbitrary UUID strings
