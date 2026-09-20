# ForgeDB Professional CLI

ForgeDB 1.0.0 provides a database-shell experience designed for development, debugging and portfolio demonstration.

## SQL

SQL statements end with `;` and can span multiple lines. Errors are reported without terminating the shell.

## Meta commands

- `:tables` - list tables and visible row counts
- `:schema [table]` - inspect columns and constraints
- `:indexes [table]` - inspect indexed columns
- `:stats [table]` - optimizer statistics
- `:metrics` - runtime counters
- `:tx` / `:transactions` - transaction state
- `:migrations` - schema migration history
- `:history [n]` - recent commands
- `:clear` - clear command history
- `:benchmark` - lightweight shell benchmark
- `:export <file>` - portable SQL export
- `:import <file>` - execute SQL script
- `:version` - version information
- `:help` - command reference
- `:quit` - exit

History is retained in a bounded sidecar file next to the database. Export/import are explicit filesystem operations and reject path traversal segments.
