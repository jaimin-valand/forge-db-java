# Fault Injection and Crash Recovery

ForgeDB includes a deliberately small process-level fault-injection harness to validate its WAL/recovery boundary.

## Failure point

The primary injected failure occurs immediately after a `COMMIT` record is appended and synchronised to the WAL, but before the database snapshot is checkpointed.

This models a realistic failure window:

```text
mutation -> WAL mutation -> WAL COMMIT -> PROCESS CRASH -> snapshot checkpoint
                                      X
```

After restart, recovery must detect the committed transaction as newer than the snapshot and replay its logical mutations exactly once.

## Safety

Fault injection is disabled by default. It only activates when the test process explicitly sets:

```text
-Dforge.fault.afterWalCommit=HALT
```

The hook uses `Runtime.halt(86)` to model abrupt process loss. It must never be enabled in normal development or production deployments.

## Test

After compiling the project, run:

```text
java -cp target/classes com.jaimin.db.reliability.CrashRecoveryIntegration
```

Expected result:

```text
PASS: crash after WAL COMMIT recovered committed state
```

The test uses a child JVM and a temporary database, so the simulated crash cannot terminate the test runner or touch a developer database.
