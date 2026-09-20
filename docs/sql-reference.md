# SQL Reference

ForgeDB implements a deliberately bounded SQL subset. Unsupported syntax should fail explicitly rather than be silently interpreted.

## DDL

### CREATE TABLE

```sql
CREATE TABLE users (
  id INT PRIMARY KEY,
  email TEXT UNIQUE,
  name TEXT NOT NULL
);
```

Supported column types: `INT`, `TEXT`.

Supported constraints: `PRIMARY KEY`, `UNIQUE`, `NOT NULL`.

### ALTER TABLE

```sql
ALTER TABLE users ADD COLUMN country TEXT DEFAULT 'UK';
```

## DML

```sql
INSERT INTO users VALUES (1, 'a@example.com', 'A');

UPDATE users SET name = 'B' WHERE id = 1;

DELETE FROM users WHERE id = 1;
```

## SELECT

```sql
SELECT * FROM users;
SELECT id, name FROM users WHERE id = 1;
SELECT id, name FROM users ORDER BY name ASC LIMIT 10 OFFSET 0;
```

Supported predicates include `=`, `!=`, `<>`, `<`, `<=`, `>`, `>=`, with `AND` conjunctions.

## JOIN

```sql
SELECT u.id, o.total
FROM users u
INNER JOIN orders o ON u.id = o.user_id;
```

## Aggregation

```sql
SELECT country, COUNT(*)
FROM users
GROUP BY country
HAVING COUNT(*) > 1;
```

Aggregate functions: `COUNT`, `SUM`, `AVG`, `MIN`, `MAX`.

## Transactions

```sql
BEGIN;
UPDATE users SET name = 'Updated' WHERE id = 1;
COMMIT;
```

or:

```sql
BEGIN;
DELETE FROM users WHERE id = 1;
ROLLBACK;
```

ForgeDB currently models a single-writer transaction architecture with MVCC-lite visibility; it does not claim full multi-writer isolation semantics.

## EXPLAIN

```sql
EXPLAIN SELECT * FROM users WHERE id = 1;
```

The plan exposes the selected access path and optimizer cost information.
