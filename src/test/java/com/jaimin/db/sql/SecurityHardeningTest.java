package com.jaimin.db.sql;

/** Dependency-free negative tests for parser/lexer resource limits and malformed input. */
public final class SecurityHardeningTest {
  public static void main(String[] args) {
    SqlLexer lexer = new SqlLexer();
    expectFailure(() -> lexer.tokenize(null), "null SQL");
    expectFailure(() -> lexer.tokenize("SELECT 'unterminated"), "unterminated string");
    expectFailure(() -> lexer.tokenize("SELECT " + "a".repeat(129) + " FROM users"), "oversized identifier");
    expectFailure(() -> lexer.tokenize("SELECT '" + "x".repeat(65_537) + "'"), "oversized string");
    expectFailure(() -> lexer.tokenize("SELECT " + "9".repeat(20)), "oversized number");
    expectFailure(() -> lexer.tokenize("SELECT " + "a ".repeat(20_001)), "token budget");
    expectFailure(() -> new SqlParser().parse("SELECT * FROM users; SELECT * FROM users"), "multiple statements");
    expectFailure(() -> new SqlParser().parse("DROP TABLE users"), "unsupported statement");
    System.out.println("PASS: security hardening tests");
  }
  private static void expectFailure(Runnable action, String name) {
    try { action.run(); throw new AssertionError("Expected failure: " + name); }
    catch (IllegalArgumentException expected) { }
  }
}
