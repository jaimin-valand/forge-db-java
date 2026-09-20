# Aggregation Engine

ForgeDB v0.18 adds relational aggregation with `COUNT`, `SUM`, `AVG`, `MIN`, `MAX`, `GROUP BY`, and `HAVING`.

Execution order:

```text
FROM -> JOIN -> WHERE -> GROUP BY -> aggregates -> HAVING -> ORDER BY -> LIMIT/OFFSET -> projection
```

Examples:

```sql
SELECT COUNT(*) FROM orders;
SELECT user_id, COUNT(*), SUM(amount) FROM orders GROUP BY user_id;
SELECT user_id, COUNT(*), SUM(amount) FROM orders GROUP BY user_id HAVING SUM(amount) >= 100 ORDER BY SUM(amount) DESC;
```

The implementation is intentionally numeric-first for `SUM`/`AVG`; `MIN`/`MAX` use numeric comparison when values are integers and lexical comparison otherwise. This is documented as a current engine limitation rather than a claim of full SQL compatibility.
