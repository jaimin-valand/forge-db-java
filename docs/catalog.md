# ForgeDB Catalog

The database file stores schema metadata alongside table rows. The catalog currently contains:

- database format version;
- last applied WAL LSN;
- schema version;
- migration history;
- table names;
- column definitions and constraints.

Schema versioning makes schema evolution observable and gives recovery/reload code an explicit metadata contract.
