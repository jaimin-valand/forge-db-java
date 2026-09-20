package com.jaimin.db.query;

import java.util.*;

/** Pull-based relational execution operator. Each next() returns one row or null at EOF. */
public interface ExecutionOperator extends AutoCloseable {
  List<String> next();
  default void open() {}
  default void close() {}
}
