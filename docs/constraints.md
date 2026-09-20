# Integrity Constraints

ForgeDB validates schema and row constraints before a row becomes visible:

- `PRIMARY KEY` values are unique and non-null.
- `UNIQUE` values are unique.
- `NOT NULL` values cannot be null.
- `INT`/`INTEGER` values must parse as signed Java integers.
- Duplicate column names and unsupported types are rejected.

Constraints are checked against the secondary-index structures, making uniqueness validation an indexed equality operation rather than a full table scan.
