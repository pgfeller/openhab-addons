# Implementation Plan: Session User Exposure

**Branch**: `specs` | **Date**: 2026-10-18 | **Spec**: specs/001-session-user-exposure/spec.md

**Input**: Feature specification from `/specs/001-session-user-exposure/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Expose the Jellyfin session's `UserName` and `UserId` as read-only channels on client Things in the openHAB Jellyfin binding. This allows users to access the currently logged-in user's identity within their openHAB rules and UI integrations.

The specification covers adding read-only String channels for `user-name` and `user-id` to client Things, derived from the session information already available in the binding's session management.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: openHAB binding APIs, Jellyfin client library

**Storage**: N/A (runtime state via openHAB event bus)

**Testing**: JUnit tests for openHAB bindings, mock-based integration tests

**Target Platform**: openHAB runtime (OSGi container)

**Project Type**: openHAB binding

**Performance Goals**: Channels should update in near real-time when session state changes

**Constraints**: Must follow openHAB binding conventions; channels must be read-only; null/empty when no session active

**Scale/Scope**: Single binding integration for Jellyfin server session user identity exposure

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Library-First**: This feature starts as a binding extension following openHAB APIs. Must reuse existing openHAB APIs and abstractions rather than introducing parallel solutions.

**CLI Interface**: N/A for this binding feature; follows openHAB binding patterns.

**Test-First (NON-NEGOTIABLE)**: Tests must cover boundary conditions (null user, anonymous session), failure cases, and recovery. Red-Green-Refactor cycle applies.

**Integration Testing**: Focus on session event bus architecture, channel state transitions when sessions start/end/change. (Note: standardized terminology to "session event bus" per analysis recommendations)

**Observability, Versioning & Simplicity**: Use parameterized SLF4J logging; version according to openHAB binding versioning rules.

**Code Quality & Documentation**: Javadoc where appropriate for API, class, and method documentation. Code comments explain non-obvious intent and protocol behavior.

**Security & Sensitive Data**: User identity information must not be exposed through logs, exceptions, or Thing properties beyond the channel state. Treat session data as sensitive.

**Gates:**

- [ ] Channel design follows openHAB binding conventions
- [ ] Read-only channel semantics preserved
- [ ] Null handling for unauthenticated sessions
- [ ] Session event bus integration pattern validated
- [ ] No sensitive data leakage in logs or diagnostics

## Project Structure

### Documentation (this feature)

```text
specs/001-session-user-exposure/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

`org.openhab.binding.jellyfin/` - Binding source code

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

[Gates determined based on constitution file]

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)
<!--
  ACTION REQUIRED: Replace the placeholder tree below with the concrete layout
  for this feature. Delete unused options and expand the chosen structure with
  real paths (e.g., apps/admin, packages/something). The delivered plan must
  not include Option labels.
-->

```text
# [REMOVE IF UNUSED] Option 1: Single project (DEFAULT)
src/
├── models/
├── services/
├── cli/
└── lib/

tests/
├── contract/
├── integration/
└── unit/

# [REMOVE IF UNUSED] Option 2: Web application (when "frontend" + "backend" detected)
backend/
├── src/
│   ├── models/
│   ├── services/
│   └── api/
└── tests/

frontend/
├── src/
│   ├── components/
│   ├── pages/
│   └── services/
└── tests/

# [REMOVE IF UNUSED] Option 3: Mobile + API (when "iOS/Android" detected)
api/
└── [same as backend above]

ios/ or android/
└── [platform-specific structure: feature modules, UI flows, platform tests]
```

**Structure Decision**: [Document the selected structure and reference the real
directories captured above]

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
| ----------- | ------------ | ------------------------------------- |
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
