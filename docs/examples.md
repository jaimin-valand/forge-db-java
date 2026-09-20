# Examples

## Indexed lookup

```sql
CREATE TABLE users (id INT PRIMARY KEY, name TEXT NOT NULL);
INSERT INTO users VALUES (1, 'Jaimin');
INSERT INTO users VALUES (2, 'Alex');
EXPLAIN SELECT * FROM users WHERE id = 2;
SELECT * FROM users WHERE id = 2;
```

## Transaction rollback

```sql
BEGIN;
UPDATE users SET name = 'Temporary' WHERE id = 1;
ROLLBACK;
SELECT * FROM users WHERE id = 1;
```

## Join and aggregation

```sql
SELECT u.id, COUNT(*)
FROM users u
INNER JOIN orders o ON u.id = o.user_id
GROUP BY u.id
HAVING COUNT(*) > 1;
```

## Pagination

```sql
SELECT id, name
FROM users
ORDER BY id ASC
OFFSET 20
LIMIT 10;
```
