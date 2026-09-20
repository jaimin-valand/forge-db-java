# GitHub Repository Setup - ForgeDB 1.0.0

## Recommended repository name

`forge-db-java`

## Description

From-scratch Java 21 embedded SQL database engine demonstrating query processing, storage, transactions, WAL recovery, security, observability, testing and release engineering.

## Suggested topics

`java` `java21` `database` `sql` `database-engine` `storage-engine` `query-optimizer` `b-tree` `mvcc` `wal` `systems-programming` `backend` `software-engineering`

## Initial repository settings

- Default branch: `main`
- Enable Issues
- Enable Discussions only if you intend to maintain them
- Enable Actions
- Protect `main` after the first CI run
- Require the CI workflow to pass before merging
- Require pull-request review for collaborative changes

## First release

Create GitHub Release:

- Tag: `v1.0.0`
- Title: `ForgeDB 1.0.0`
- Attach the validated release JAR and SHA-256 checksum produced under `target/release/` by the final release gate.

## Suggested About text

**ForgeDB - a from-scratch Java 21 embedded SQL database engine built to demonstrate database internals and production-minded software engineering.**
