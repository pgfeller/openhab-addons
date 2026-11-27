<!--
Sync Impact Report
Version change: none -> 1.0.0 (initial constitution)
Modified principles: none
Added sections: five core principles, Project Constraints, Development Workflow
and Quality Gates, Governance
Removed sections: none
Follow-up TODO: Confirm the original ratification date with project maintainers.
-->

# openHAB Add-ons Constitution

## Core Principles

### I. Compatibility and Core Reuse

Changes MUST preserve existing APIs, identifiers, configuration semantics, and
supported protocol representations unless an accepted requirement explicitly
requires a change. Implementations MUST reuse suitable openHAB core APIs and
established repository patterns rather than introducing parallel abstractions.

### II. Lifecycle-Safe Integrations

Asynchronous and lifecycle-bound integrations MUST handle activation, failure,
recovery, reconfiguration, and disposal without stale work or leaked resources.
Use openHAB schedulers and typed service dependencies where suitable, and keep
state transitions explicit.

### III. Verifiable Behavior

Behavioral changes MUST have focused, deterministic tests for their observable
requirements. Validation MUST use the narrowest relevant repository quality
gates and expand when the change's scope or impact warrants it.

### IV. Secure and Explicit Failure Handling

External input and services MUST be treated as untrusted. Credentials MUST NOT
be exposed in logs or user-visible data, and transport security MUST NOT be
weakened by default. Failures MUST remain distinguishable from valid results;
do not hide errors behind success-shaped fallbacks.

### V. Minimal, Reviewable Contributions

Changes MUST be the smallest coherent implementation of an accepted need, with
related documentation and configuration kept consistent. Upstream pull requests
MUST contain only changes and artifacts necessary for the contribution and
allowed by the upstream project's contribution policies. Local-development
artifacts, including memory-bank content and Spec Kit files, MUST stay out of
those pull requests.

## Project Constraints

- Java modules target Java 21 and follow the repository's nullability and
  formatting conventions.
- OSGi services are dynamic dependencies; prefer Declarative Services and
  supported openHAB APIs over low-level framework access.
- Thing, channel, configuration, and documentation metadata MUST describe the
  implemented behavior consistently.
- Dependency changes MUST account for runtime features and integration-test
  resolver configuration where applicable.
- Locale-specific `OH-INF/i18n/*_xx.properties` files are managed externally
  and MUST NOT be edited directly.

## Development Workflow and Quality Gates

- Establish requirements from the issue, discussion, documentation, tests, and
  existing behavior before implementation.
- Inspect nearby implementation and tests; extend existing coverage rather
  than duplicating it.
- Run Spotless for Java changes, relevant tests or Maven builds for the
  affected module, and dependency resolution checks when dependencies change.
- Review the complete diff for scope, generated files, static-analysis
  findings, and consistency with documentation and metadata.
- Follow `CONTRIBUTING.md` and `.github/PULL_REQUEST_TEMPLATE.md` for upstream
  contributions, including required sign-off and commit conventions.

## Governance

This constitution supplements, and does not override, openHAB contribution
requirements, repository guidance, or applicable upstream policies. Where they
conflict, the upstream requirements take precedence.

Amendments require project-maintainer approval and a documented rationale.
Reviewers MUST assess proposed changes against these principles. Versioning
follows semantic versioning: MAJOR for incompatible governance changes, MINOR
for new or materially expanded principles, and PATCH for clarifications.

**Version**: 1.0.0 | **Ratified**: TODO(RATIFICATION_DATE): pending maintainer confirmation | **Last Amended**: 2026-11-27
