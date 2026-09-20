# JOIN Engine

ForgeDB v0.17 adds an equality-based `INNER JOIN` execution path.

## Supported syntax

```sql
SELECT users.name, orders.amount
FROM users
JOIN orders
  ON users.id = orders.user_id
WHERE orders.amount >= 50
ORDER BY orders.amount ASC;
```

Aliases are accepted with `AS` or directly after the table name. Join predicates must currently be equality predicates and should use qualified column references (`users.id = orders.user_id`).

## Execution model

The query is parsed into a `SqlStatement.Join`, then the SQL facade builds a `JoinOperator`. The operator currently uses a deterministic nested-loop algorithm, producing a combined row stream. Filtering, ordering, pagination and projection are applied after the join.

This is deliberately explicit rather than pretending the current implementation is a full cost-based join optimizer. A later optimizer phase can choose between nested-loop and index nested-loop strategies once index metadata is exposed through the planning layer.

## Correctness rules

- Unknown tables fail before execution.
- Unknown or ambiguous join columns fail explicitly.
- Equality is the only join predicate in v0.17.
- Qualified output names prevent collisions between identically named columns.
- `SELECT *` returns columns from both sides of the join.

## Example

```sql
CREATE TABLE users (id INT PRIMARY KEY, name TEXT NOT NULL);
CREATE TABLE orders (id INT PRIMARY KEY, user_id INT NOT NULL, amount INT);

SELECT users.name, orders.amount
FROM users
JOIN orders ON users.id = orders.user_id
ORDER BY orders.amount DESC
LIMIT 10;
```
