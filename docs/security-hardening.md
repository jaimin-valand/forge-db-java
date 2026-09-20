# Security and Input Hardening

ForgeDB is an embedded educational database, not a network-exposed production DBMS. The security work here focuses on deterministic rejection of malformed or excessively large SQL input and on reducing parser/resource abuse.

## Limits

- Maximum SQL statement length: 1,000,000 characters
- Maximum token count: 20,000 tokens
- Maximum identifier length: 128 characters
- Maximum string literal length: 65,536 characters
- Maximum integer literal length: 19 characters

These are parser-level guardrails. They prevent accidental or hostile inputs from causing unbounded lexer allocations or extremely large token streams.

## Parsing behaviour

The lexer rejects:

- null SQL input
- unterminated string literals
- unsupported characters
- oversized identifiers
- oversized string literals
- oversized numeric literals
- excessive token counts

The parser rejects unsupported statements and trailing tokens, so a single `parse()` call cannot silently execute multiple SQL statements separated by semicolons.

## Injection model

ForgeDB does not construct SQL by concatenating application parameters internally. Its query engine receives an already-parsed AST. The appropriate production boundary for parameterised SQL would be a prepared-statement API; this is intentionally listed as future work rather than claimed as implemented.

## Error handling

Errors are deterministic `IllegalArgumentException` failures at the SQL boundary. The CLI should present these messages to the user without exposing filesystem paths, stack traces, WAL contents, or internal object state.
