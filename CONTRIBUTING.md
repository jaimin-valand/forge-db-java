# Contributing to ForgeDB

ForgeDB is a learning-oriented systems project. Contributions should preserve clarity, deterministic behaviour and explicit failure modes.

## Development principles

1. Keep subsystem responsibilities narrow.
2. Prefer deterministic tests over timing-sensitive assertions.
3. Do not silently accept unsupported SQL.
4. Preserve storage and WAL integrity checks.
5. Document trade-offs when introducing a new subsystem.
6. Avoid machine-specific benchmark claims.

## Change workflow

```text
Change → focused test → release gate → documentation → review
```

For a new SQL feature, update the lexer/parser, planner/executor behaviour, regression coverage and SQL reference as appropriate.

For storage or WAL changes, add failure/recovery coverage and verify existing integrity scenarios.

## Commit guidance

Use concise imperative commit messages, for example:

```text
Add hash join candidate to optimizer
Harden WAL tail recovery
Document storage page lifecycle
```
