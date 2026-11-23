---
title: Repository-specific guidance placement
type: decision
permalink: openhab-addons/repository-guidance-placement
tags:
- openhab-addons
- instructions
- decision
---

## Decision

Keep the root `AGENTS.md` focused on repository-wide contributor guidance. Put repository- or session-specific agent guidance in `.github/copilot-instructions.md` and explicitly routed project memory rather than adding it to the shared guidance file.

## Rationale

This keeps shared contributor instructions broadly applicable while retaining local workflow details in their intended locations.

## Provenance

Migrated from the accepted agent-memory-mcp decision `d841edc0-4d19-4d02-8533-cd4bc081e7bd`, recorded 2026-10-06. The current checkout maintains separate `AGENTS.md` and `.github/copilot-instructions.md` files.

## Memory project routing

Use the explicit Basic Memory route `openhab-addons` for this checkout, rooted at `docs/memory/openhab-addons/`. The existing `openhab` route points to a different checkout and is not interchangeable. The legacy per-host `agent-memory-mcp` store remains under `data/memory-store/`; there is no automatic conversion, so migrate only reviewed, durable, non-sensitive project facts with provenance, preserve the source, and exclude working/session content.
