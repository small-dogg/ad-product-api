# CLAUDE.md

Project rules for Claude Code.

## Implementation
- Follow SOLID.
- Use DDD-based design.
- Add concise comments explaining business intent and structure.

## Refactoring
- Explain plan and get approval before changes.
- Structural improvements only; no behavior changes.
- All tests must pass after refactoring.

## Debugging
- Explain root cause and solution; get approval before fixing.
- Correct behavior > error suppression.
- If unclear, add detailed logs for analysis.

## Language & Docs
- Use English for technical terms, libraries, and AWS resources.
- Use Mermaid for simple diagrams.
- Use separate SVG files for complex architecture diagrams.

## Commits
- Follow Conventional Commits.
- Never use `--no-verify`.

## Environments
- Local only for now.
- Explicitly define: local / dev / prod.

## Database
- PostgreSQL.
- Local datasource: `jerry.world:32222/product`
- Credentials provided separately.
- Use JPA.
- Use QueryDSL for complex queries.

## Change Scope
- Modify only the code explicitly requested.
- Do not refactor adjacent code unless approved.
- Do not introduce new abstractions without justification.

## Assumptions
- Do not assume requirements or behavior.
- Ask before filling missing specifications.
- Prefer explicit TODO over guessing.

## Testing
- Tests must assert business behavior, not implementation details.
- Avoid excessive mocking.
- Name tests in a business-readable way.

## DDD Boundaries
- Do not bypass domain layer.
- Application layer orchestrates only.
- Infrastructure must not leak into domain.

## Safety
- Do not change transaction boundaries.
- Do not add async, cache, or batching unless requested.

## Response
- Prefer structured answers.
- Avoid verbose explanations unless requested.
- Use code blocks only when necessary.
