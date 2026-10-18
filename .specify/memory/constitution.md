# openHAB Add-ons Project Constitution
<!-- Example: Spec Constitution, TaskFlow Constitution, etc. -->

## Core Principles

### I. Library-First

Every feature starts as a standalone library. Libraries must be self-contained, independently testable, documented; Clear purpose required - no organizational-only libraries. Libraries must reuse existing openHAB APIs and abstractions rather than introducing parallel or speculative solutions.

### II. CLI Interface

Every library exposes functionality via CLI. Text in/out protocol: stdin/args → stdout, errors → stderr; Support JSON + human-readable formats. Libraries must follow the narrowest visibility that satisfies the design and avoid expanding public API surface as a side effect.

### III. Test-First (NON-NEGOTIABLE)

TDD mandatory: Tests written → User approved → Tests fail → Then implement; Red-Green-Refactor cycle strictly enforced. Every change must include relevant boundary, failure, malformed-input, and recovery cases. Tests must remain deterministic and avoid unnecessary timing dependencies.

### IV. Integration Testing

Focus areas requiring integration tests: New library contract tests, Contract changes, Inter-service communication, Shared schemas. Integration tests use bndrun configurations to define realistic OSGi runtime environments. Lifecycle-sensitive changes consider activation, deactivation, reactivation, service arrival/removal.

### V. Observability, Versioning & Simplicity

Text I/O ensures debuggability; Structured logging required using parameterized SLF4J logging; Or: MAJOR.MINOR.BUILD format; Or: Start simple, YAGNI principles. Version bump rules: MAJOR for backward incompatible governance/principle removals, MINOR for new principle/section additions, PATCH for clarifications, wording, typo fixes. All logging uses parameterized SLF4J instead of string concatenation.

### VI. Code Quality & Documentation

Code must reuse existing openHAB APIs, utilities, and implementation patterns instead of introducing parallel or speculative solutions. Prefer straightforward implementations over speculative abstractions. Javadoc where appropriate for API, class, and method documentation; Keep Javadoc accurate when behavior changes. Code comments explain non-obvious intent, constraints, invariants, protocol or API behavior, compatibility requirements, lifecycle or concurrency assumptions, and intentional deviations.

### VII. Security & Sensitive Data

Treat credentials, API keys, access tokens, refresh tokens, webhook secrets, private keys, and similar values as sensitive data. Do not expose sensitive data through logs, exceptions, Thing properties, discovery results, toString() implementations, or other user-visible diagnostics. Reuse established openHAB authentication, networking, and security facilities. Treat externally controlled URLs, paths, headers, identifiers, and other values crossing trust boundaries as untrusted and validate or constrain them.

## Additional Constraints, Security Requirements, Performance Standards

[SECTION_2_CONTENT]
<!-- Example: Technology stack requirements, compliance standards, deployment policies, etc. -->

## Development Workflow, Review Process, Quality Gates

[SECTION_3_CONTENT]
<!-- Example: Code review requirements, testing gates, deployment approval process, etc. -->

## Governance
<!-- Example: Constitution supersedes all other practices; Amendments require documentation, approval, migration plan -->

[GOVERNANCE_RULES]
<!-- Example: All PRs/reviews must verify compliance; Complexity must be justified; Use [GUIDANCE_FILE] for runtime development guidance -->

**Version**: 2.0.0 | **Ratified**: 2026-10-18 | **Last Amended**: 2026-10-18
<!-- Example: Version: 2.1.1 | Ratified: 2025-06-13 | Last Amended: 2025-07-16 -->
