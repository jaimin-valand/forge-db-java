package com.jaimin.db.sql;

import java.util.Random;

/** Deterministic parser fuzz regression: malformed input must fail closed without unchecked VM errors. */
public final class SecurityFuzzRegressionTest {
  public static void main(String[] args) {
    Random random = new Random(0xF0A6E);
    SqlParser parser = new SqlParser();
    int rejected = 0;
    for (int i = 0; i < 2_000; i++) {
      int length = random.nextInt(4_096);
      StringBuilder sql = new StringBuilder(length);
      for (int j = 0; j < length; j++) {
        int bucket = random.nextInt(12);
        char c = switch (bucket) {
          case 0 -> '\'';
          case 1 -> '(';
          case 2 -> ')';
          case 3 -> ';';
          case 4 -> '<';
          case 5 -> '>';
          case 6 -> '=';
          case 7 -> ',';
          default -> (char) ('a' + random.nextInt(26));
        };
        sql.append(c);
      }
      try {
        parser.parse(sql.toString());
      } catch (IllegalArgumentException expected) {
        rejected++;
      } catch (RuntimeException unexpected) {
        throw new AssertionError("parser escaped its controlled error boundary", unexpected);
      }
    }
    if (rejected == 0) throw new AssertionError("fuzz corpus unexpectedly accepted every input");
    System.out.println("PASS: deterministic parser fuzz regression (2000 cases, " + rejected + " rejected)");
  }
}
