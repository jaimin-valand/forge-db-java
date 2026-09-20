# CLI Guide

ForgeDB provides an interactive shell for exploration and diagnostics.

## SQL input

Enter SQL statements terminated with `;`. Multi-line statements are supported.

## Meta-commands

| Command | Purpose |
|---|---|
| `:help` | Show CLI help |
| `:tables` | List tables |
| `:schema <table>` | Inspect a table schema |
| `:indexes <table>` | Inspect indexes |
| `:stats` | Show table statistics |
| `:metrics` | Show runtime metrics |
| `:tx` | Show transaction state |
| `:migrations` | Show schema migrations |
| `:history` | Show command history |
| `:benchmark` | Run benchmark workload |
| `:export <file>` | Export SQL |
| `:import <file>` | Execute SQL from a file |
| `:version` | Show ForgeDB version |
| `:clear` | Clear the terminal view |
| `:quit` | Exit the shell |

File-oriented commands validate paths before access.
