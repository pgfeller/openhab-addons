# Copilot Instructions

## Memory MCP

- Before repository work, consult the memory MCP for relevant project knowledge, decisions, runbooks, and prior incidents when the MCP tools are available. Use the repository's actual files and current behavior as the source of truth; verify remembered details that may have changed.
- At the end of a substantive session, update the memory MCP with new, durable repository knowledge that will help future work, such as verified conventions, decisions, useful validation commands, or resolved non-obvious failures. Do not store secrets, speculative conclusions, or short-lived task state.
- When adding or updating memory, consolidate related entries, correct or mark superseded information obsolete, and remove stale or duplicate content using the memory MCP's supported operations. Preserve still-valid context and provenance; do not promote uncertain information as confirmed knowledge.
- If the memory MCP is unavailable or has no relevant entries, proceed using repository evidence and do not invent memory content.

## Java Development Conventions

- Target Java 21 for all new code. Use Java 21 language features where they improve clarity, correctness, or maintainability such as records, pattern matching, switch expressions, and text blocks.
- Prefer established repository patterns over introducing parallel or speculative solutions. Reuse existing openHAB APIs, utilities, and abstractions rather than creating new ones without a concrete benefit.
- Use the narrowest visibility that satisfies the design and avoid expanding the public API surface as a side effect of implementation changes.
- Use parameterized SLF4J logging instead of string concatenation. Treat expected device, network, and communication failures as normal runtime conditions; update Thing status where appropriate rather than routinely logging at `warn` or `error`.
- Reserve `warn`, `error`, and stack traces for unexpected conditions that require attention or may indicate a software defect.

## openHAB Binding Specific Conventions

- When implementing bindings, prefer openHAB-provided HTTP and REST APIs and abstractions where they fit the use case. For servlet, filter, listener, and web-resource registration that requires OSGi HTTP integration, use the OSGi HTTP Whiteboard and standard service properties such as `HttpWhiteboardConstants`.
- Treat dynamic HTTP registrations as lifecycle-bound resources and unregister them when no longer needed. Reuse openHAB-provided authentication, networking utilities, and shared HTTP clients where appropriate.
- Avoid depending directly on Jetty or other HTTP-server implementation details unless there is no supported OSGi or openHAB API for the required behavior.
- Use quantity types and Units of Measurement for values that represent measurable quantities where appropriate.
- Keep Thing, Channel, configuration, and README documentation consistent with the implemented behavior.
- Use appropriate semantic tags where applicable.
- Handle `RefreshType.REFRESH` explicitly for channels that support refresh rather than treating it as a normal device command.
- Preserve stable identifiers and representation properties used for discovered Things.

## Lifecycle, Concurrency, and State

When code performs asynchronous, scheduled, or lifecycle-sensitive work:

- Use openHAB-provided schedulers rather than creating threads where possible.
- Ensure tasks, futures, listeners, connections, and other resources are released during `dispose()` or deactivation.
- Consider callbacks that may execute while or after the owning object is being disposed, as well as initialization, shutdown, failure, reconnection, and repeated lifecycle transitions.
- Ensure already-running work cannot publish stale state, restore an obsolete status, or reschedule itself after the state that authorized that work has become invalid.
- When validation and subsequent use depend on mutable shared state, validate and use the same captured state or provide appropriate synchronization.
- Keep distinct states separate when conflating them could break recovery, for example session validity, connection state, configuration state, and Thing status.
- Shut down dependent tasks, contexts, listeners, or consumers before closing shared resources they still depend on.
- Ensure reconnect and retry behavior cannot leave stale tasks or resources.
- When correctness depends on the order of asynchronous stages, explicitly compose or wait for their completion; submitting work to an executor does not establish that it completed before dependent work begins.
- When configuration or metadata can change at runtime, apply changes to active state and resources rather than consuming them only during initial activation.
- Initialization, reconciliation, registry-change, and reconnect paths that may run repeatedly must be idempotent and must not create duplicate registrations, listeners, channels, or protocol state.
- Consider material performance and resource impact for polling, discovery, event callbacks, registry scans, retry loops, and large collections; avoid unnecessary repeated work, unbounded state, and blocking I/O on shared scheduler or event threads.
- Avoid holding locks while calling framework or external code, and review synchronization carefully for potential races and deadlocks.
- Framework lifecycle methods should return promptly unless their API explicitly allows blocking operations.

## Error Handling and External Data

For code interacting with devices, services, files, sockets, or external APIs:

- Validate external input where malformed, incomplete, missing, or unexpected values can realistically occur.
- Rely on existing framework, library, parser, or API guarantees where appropriate instead of adding redundant validation.
- Follow the surrounding implementation's established validation and error-handling patterns.
- When a protocol provides structured data such as JSON or XML, use an appropriate parser and inspect structured fields rather than relying on formatting-sensitive substring matching.
- Do not use human-readable exception messages, log messages, or similar text for program control flow when a dedicated exception type, error code, enum, or other structured state can represent the condition.
- Catch or wrap exceptions only when they can be handled, translated at an abstraction boundary, or given useful context; preserve the original cause when wrapping.
- Do not map missing or unknown external values to valid values such as zero unless the protocol explicitly defines that mapping.
- Do not treat absence from a partial or transient response as proof that a capability is unsupported, and do not make baseline discovery or operation depend on optional enrichment data unless the protocol requires it.
- Preserve supported legacy protocol and API representations when introducing newer alternatives unless compatibility is intentionally dropped; gate version- or capability-specific behavior on the actual supported feature rather than assuming related features were introduced together.
- When acknowledgement controls redelivery, acknowledge external messages or events only after successful processing or durable retention.
- Preserve protocol-defined units, encodings, charsets, identifiers, and semantics.
- Close resources reliably, preferably using try-with-resources.
- Handle interruption correctly and do not silently consume interrupt-related exceptions.
- Distinguish expected communication failures from actual software defects.

## Caching

When caching external or computed data:

- Do not cache transient failure or sentinel results as successful data unless that behavior is intentional.
- Consider whether authentication, configuration, connection, or lifecycle changes require related cached data to be invalidated.
- Ensure cache keys and invalidation account for every input that can change the derived result, including transitive dependencies where relevant.

## Nullability and Mutable State

Follow openHAB null-annotation conventions:

- Annotate non-DTO classes with `@NonNullByDefault`.
- Use `@Nullable` only when `null` is an intentional state.
- Do not suppress null warnings instead of addressing the underlying issue.
- When mutable fields may change concurrently, use a local copy where needed so a null check and subsequent access operate on the same value.

## Testing

- Cover new production code with unit tests. Do not assume tests are unnecessary or silently omit them.
- If unit tests are not feasible, explain the technical reason and obtain the user's explicit approval and justification before proceeding without them.
- Include relevant boundary, failure, malformed-input, and recovery cases in tests.
- Tests should be deterministic and avoid unnecessary timing dependencies.
- Avoid timing-sensitive tests that depend on fixed delays; synchronize on observable events or conditions using latches, barriers, futures, locks or conditions, or bounded polling where appropriate.
- Tests must tolerate slow or congested build runners. If a fixed sleep cannot reasonably be avoided, use a conservative duration with sufficient margin for CI load and keep the delay bounded.
- When testing blocking, shutdown, retry, or deadlock-prone behavior, bound the operation itself with a timeout so regressions fail the test instead of hanging the build, and ensure helper threads cannot keep the JVM alive after the timeout.
- When modifying existing behavior, inspect existing tests first and extend them rather than creating redundant coverage.
- Integration tests are located in the `itests/` directory. Use bndrun configurations to define realistic OSGi runtime environments.
- For lifecycle-sensitive changes, consider activation, deactivation, reactivation, service arrival/removal, and different installation orders where relevant.
- In bnd-based integration tests, do not infer a service-availability race merely from a direct `getService(...)` call during test setup. Services supplied by the resolved runtime are normally available before the tests execute; use polling only when the specific service can actually arrive asynchronously after test execution has started.
- When an integration test modifies a provider tracked by an `AbstractRegistry`-based registry, ensure asynchronous registry activation has completed before modifying the provider. Use `waitForCompletedAsyncActivationTasks()` when available, before adding, updating, or removing provider elements.

## Build and Formatting

- Run `mvn spotless:apply` for the affected project to apply repository formatting, including import organization.
- Keep POM sections sorted.
- When a file is generated from another source, update the source or generator and regenerate the output rather than hand-editing generated content, unless the repository workflow explicitly requires direct edits.
- Avoid formatting or generation changes outside the intended scope.
- Run `mvn clean install` or `mvn clean install -pl :bindingname` to build and validate changes.
- For dependency or resolver changes, also validate with `-DwithResolver` where relevant.

## Documentation

- Keep Thing, Channel, configuration, and README documentation consistent with the implemented behavior.
- Use Javadoc where appropriate for API, class, and method documentation. Keep Javadoc accurate when behavior changes.
- Do not add or edit locale-specific `OH-INF/i18n/*_xx.properties` files; translations are managed externally.
- Reference documentation: <https://www.openhab.org/docs/developer/>

## Security and Sensitive Data

- Treat credentials, API keys, access tokens, refresh tokens, webhook secrets, private keys, and similar values as sensitive data.
- Do not expose sensitive data through logs, exceptions, Thing properties, discovery results, `toString()` implementations, or other user-visible diagnostics.
- Reuse established openHAB authentication, networking, and security facilities where applicable, and do not disable TLS certificate or hostname verification or weaken authentication by default.
- Treat externally controlled URLs, paths, headers, identifiers, and other values crossing trust boundaries as untrusted and validate or constrain them where they can affect security-sensitive behavior.

## Nullability and Mutable State

Follow openHAB null-annotation conventions:

- Annotate non-DTO classes with `@NonNullByDefault`.
- Use `@Nullable` only when `null` is an intentional state.
- Do not suppress null warnings instead of addressing the underlying issue.
- When mutable fields may change concurrently, use a local copy where needed so a null check and subsequent access operate on the same value.
