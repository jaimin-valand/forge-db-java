# Schema Migrations

ForgeDB tracks a monotonically increasing schema version and a small migration history in the database catalog metadata.

## Supported migration

```sql
ALTER TABLE users ADD COLUMN country TEXT DEFAULT 'UK';
```

The migration:

1. validates the table and column definition;
2. appends the default value to existing rows;
3. updates the table schema;
4. increments the schema version;
5. records the migration SQL in the catalog metadata;
6. persists the updated database atomically.

The current implementation intentionally keeps migrations narrow: it demonstrates catalog/version management without pretending to implement a full production migration DSL.
