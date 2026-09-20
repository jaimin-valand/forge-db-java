# SQL Engine

ForgeDB now uses a four-stage SQL path:

`SQL text -> lexer -> parser/AST -> database execution`

The lexer recognises keywords, identifiers, strings, numbers and symbols. The parser produces typed statements rather than dispatching SQL with regular expressions.

Supported statements currently include:

- `CREATE TABLE` with `TEXT`/`INT` types
- `PRIMARY KEY`, `UNIQUE`, `NOT NULL`
- `INSERT INTO ... VALUES (...)`
- `SELECT * FROM ...`
- equality predicates using `WHERE column = value`
- `EXPLAIN SELECT ...`

The parser is intentionally small, deterministic and dependency-free. More SQL grammar will be added as the execution engine grows.
