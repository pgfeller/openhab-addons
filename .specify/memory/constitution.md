# openHAB Add-ons Project Constitution

<!--
Sync Impact Report
Version: 2.0.0 -> 3.0.0
Modified: I. Library-First -> I. Existing Binding and API Reuse;
II. CLI Interface -> II. openHAB Interface Contracts.
III-VII retain testing, integration, observability, quality, and security obligations.
Added: concrete platform constraints, workflow gates, and amendment governance.
Removed: standalone-library and CLI mandates copied from template examples.
Migration: feature plans and tasks target existing binding interfaces, not new libraries/CLIs.
Deferred placeholders: none. Dependent templates remain unchanged and read this document at runtime.
-->

## Core Principles

### I. Existing Binding and API Reuse

Features MUST extend the owning add-on and reuse existing openHAB APIs, utilities, and
architecture. New libraries or abstractions require a concrete need that existing facilities
cannot satisfy. Components MUST have a clear purpose and be independently testable where practical.

### II. openHAB Interface Contracts

Features MUST use established Thing, Channel, configuration, discovery, and handler contracts.
Identifiers and existing behavior MUST remain compatible unless a specification explicitly
authorizes a change. Visibility MUST be the narrowest necessary. A binding feature does not
require a separate CLI or standalone library.

### III. Test-First (NON-NEGOTIABLE)

Behavior changes MUST follow tests written, user-approved acceptance intent, observed failure,
implementation, and passing verification. Tests MUST exercise observable behavior, including
relevant boundary, malformed-input, failure, and recovery cases. Tests MUST be deterministic,
with bounded synchronization rather than timing-sensitive sleeps. Existing failing tests may
be repaired before further feature implementation; passing tests MUST NOT be claimed without execution.

### IV. Integration Testing

Contract changes, inter-service communication, and shared schemas MUST have integration coverage.
Tests requiring an OSGi runtime MUST use the repository's bndrun integration-test conventions.
Lifecycle-sensitive changes MUST consider activation, deactivation, reactivation, service arrival,
and service removal. Unit tests alone MUST NOT be described as runtime integration verification.

### V. Observability, Versioning & Simplicity

Logging MUST use parameterized SLF4J and distinguish expected communication failures from defects.
Designs MUST favor the smallest coherent change and avoid speculative abstractions.
Constitution versions MUST follow the amendment policy below; feature work MUST NOT introduce
unnecessary project or dependency version changes.

### VI. Code Quality & Documentation

Code MUST follow applicable AGENTS.md instructions and existing repository conventions.
Javadoc and user-facing documentation MUST remain accurate. Comments MUST explain non-obvious
intent rather than narrate code. Generated sources MUST be changed through their generator or
source definition unless the documented repository workflow requires otherwise.

### VII. Security & Sensitive Data

Credentials, API keys, access tokens, refresh tokens, private keys, and similar secrets MUST NOT
appear in logs, discovery results, Thing properties, exceptions, or user-facing diagnostics.
Features MUST reuse openHAB authentication and networking facilities and validate untrusted
external values at their owning boundaries. TLS verification MUST NOT be weakened by default.

## Additional Constraints, Security Requirements, Performance Standards

- Java source MUST target the repository's Java 21 baseline and follow openHAB null annotations.
- Binding metadata, implementation, and documentation MUST agree on types, units, and null behavior.
- Lifecycle work MUST release resources and prevent stale callbacks from restoring obsolete state.
- External protocol assumptions MUST be checked against authoritative specifications or source.
- Performance requirements MUST have measurable checks; features MUST NOT add unnecessary polling.

## Development Workflow, Review Process, Quality Gates

1. Select the feature directory through `.specify/feature.json` and define testable requirements.
1. Review specification quality in `checklists/requirements.md`; this checklist is not test evidence.
1. Produce a constitution-compliant plan, research, data model, contracts, and validation guide.
1. Generate dependency-ordered tasks with real repository paths and requirement coverage.
1. Run cross-artifact analysis and resolve critical inconsistencies before implementation.
1. Implement with test-first validation and record only genuinely completed tasks.
1. Run scoped tests, the affected module build, Spotless, and Markdown lint as applicable.
1. Execute configured workflow hooks. Preserve history and report blocked or unverified gates.

Source inspection MAY establish that implementation exists; it MUST NOT establish that tests pass.
Partial implementations MUST retain open verification tasks. Completion requires recorded evidence
for acceptance criteria and an explicit account of any unavailable runtime validation.

## Governance

This constitution governs spec-kit artifacts together with applicable repository instructions.
An unresolved conflict MUST be reported before implementation; it MUST NOT be silently waived.
Amendments require an explicit user request or approval, rationale, a version change, and review
of affected feature artifacts. Previously valid history MUST be preserved during migration.

Version changes use MAJOR for incompatible principle removals or redefinitions, MINOR for new
principles or materially expanded guidance, and PATCH for non-semantic corrections. Ratification
dates remain unchanged; amendment dates record the actual change date. Plans and reviews MUST
evaluate compliance before and after design. Exceptions require explicit approval and documented
justification rather than an implicit bypass of a gate.

**Version**: 3.0.0 | **Ratified**: 2026-10-18 | **Last Amended**: 2026-10-30
