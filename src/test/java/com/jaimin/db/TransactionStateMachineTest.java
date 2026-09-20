package com.jaimin.db;

import java.nio.file.*;

/** Validates ForgeDB transaction lifecycle and invalid state transitions. */
public final class TransactionStateMachineTest {
  public static void main(String[] args) throws Exception {
    Path dir = Files.createTempDirectory("forgedb-tx-state-");
    Database db = new Database(dir.resolve("data.fdb"));
    if (db.transactionState() != TransactionManager.State.IDLE) throw new AssertionError("initial state");

    db.begin();
    if (db.transactionState() != TransactionManager.State.ACTIVE) throw new AssertionError("begin state");
    try { db.begin(); throw new AssertionError("nested transaction accepted"); }
    catch (IllegalStateException expected) {}

    db.rollback();
    if (db.transactionState() != TransactionManager.State.ABORTED) throw new AssertionError("rollback state");
    try { db.commit(); throw new AssertionError("commit after rollback accepted"); }
    catch (IllegalStateException expected) {}
    try { db.rollback(); throw new AssertionError("double rollback accepted"); }
    catch (IllegalStateException expected) {}

    db.begin();
    db.commit();
    if (db.transactionState() != TransactionManager.State.COMMITTED) throw new AssertionError("commit state");
    try { db.commit(); throw new AssertionError("double commit accepted"); }
    catch (IllegalStateException expected) {}
    try { db.rollback(); throw new AssertionError("rollback after commit accepted"); }
    catch (IllegalStateException expected) {}

    System.out.println("PASS: transaction state machine and invalid transitions");
  }
}
