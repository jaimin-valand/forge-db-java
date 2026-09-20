# Integration Testing

ForgeDB uses a dependency-free end-to-end release-gate in addition to its JUnit unit tests.

`ForgeDbIntegrationSuite` drives the public SQL path and verifies that multiple subsystems work together:

1. SQL parsing and execution
2. primary-key constraint enforcement
3. index-aware query planning
4. projection and filtering
5. transactional update/delete
6. rollback restoration
7. commit durability
8. schema migration and defaults
9. database reload/persistence

The suite is deliberately scenario-oriented. A feature can pass its unit test while still failing when connected to another subsystem; this suite is intended to catch those integration regressions.

The reliability tests remain separate because crash injection intentionally terminates a child JVM.
