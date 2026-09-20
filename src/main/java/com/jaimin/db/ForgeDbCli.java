package com.jaimin.db;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import com.jaimin.db.sql.SqlStatement;

/** Professional interactive shell for ForgeDB. SQL ends with ';'; meta commands begin with ':'. */
public final class ForgeDbCli {
  private ForgeDbCli() {}

  public static void main(String[] args) throws Exception {
    Path dbPath = args.length > 0 ? Path.of(args[0]).toAbsolutePath().normalize() : Path.of("forge.db").toAbsolutePath().normalize();
    Database db = new Database(dbPath);
    SqlEngine engine = new SqlEngine(db);
    List<String> history = new ArrayList<>();
    Path historyFile = dbPath.resolveSibling("." + dbPath.getFileName() + ".history");
    loadHistory(historyFile, history);
    printBanner(dbPath);
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    StringBuilder buffer = new StringBuilder();
    while (true) {
      System.out.print(buffer.isEmpty() ? "forge> " : "   ...> "); System.out.flush();
      String line = reader.readLine();
      if (line == null) { System.out.println(); break; }
      if (buffer.isEmpty() && line.trim().startsWith(":")) {
        if (handleMeta(line.trim(), db, engine, history, historyFile)) break;
        continue;
      }
      buffer.append(line).append('\n');
      if (!line.trim().endsWith(";")) continue;
      String sql = buffer.toString().trim(); buffer.setLength(0);
      history.add(sql); saveHistory(historyFile, history);
      executeSql(sql, engine);
    }
    if (db.inTransaction()) try { db.rollback(); } catch (Exception ignored) {}
    System.out.println("Bye.");
  }

  private static void executeSql(String sql, SqlEngine engine) {
    try {
      List<List<String>> result = engine.execute(sql);
      if (!result.isEmpty()) printRows(result);
    } catch (Exception e) {
      System.out.println("ERROR: " + e.getMessage());
    }
  }

  private static boolean handleMeta(String command, Database db, SqlEngine engine, List<String> history, Path historyFile) {
    String[] parts = command.split("\\s+", 2);
    String op = parts[0].toLowerCase(Locale.ROOT);
    String arg = parts.length == 2 ? parts[1].trim() : "";
    try {
      switch (op) {
        case ":q", ":quit", ":exit" -> { return true; }
        case ":help" -> printHelp();
        case ":tables" -> printTables(db);
        case ":schema" -> printSchema(db, arg.isBlank() ? null : arg);
        case ":indexes" -> printIndexes(db, arg.isBlank() ? null : arg);
        case ":migrations" -> printMigrations(db);
        case ":tx", ":transactions" -> printTx(db);
        case ":metrics" -> printMetrics(db);
        case ":stats" -> printStats(db, arg.isBlank() ? null : arg);
        case ":history" -> printHistory(history, arg);
        case ":clear" -> { history.clear(); saveHistory(historyFile, history); System.out.println("history cleared"); }
        case ":benchmark" -> runBenchmark(db);
        case ":export" -> exportDatabase(db, safePath(arg));
        case ":import" -> importSql(engine, safePath(arg), history, historyFile);
        case ":version" -> System.out.println("ForgeDB 1.0.0");
        default -> System.out.println("Unknown command. Type :help.");
      }
    } catch (Exception e) { System.out.println("ERROR: " + e.getMessage()); }
    return false;
  }

  private static void printBanner(Path path) {
    System.out.println("ForgeDB 1.0.0 - professional interactive shell");
    System.out.println("Database: " + path);
    System.out.println("Type :help for commands; end SQL with ';'.");
  }

  private static void printHelp() {
    System.out.println("Meta commands:");
    System.out.println("  :tables                 list tables");
    System.out.println("  :schema [table]         show schema and constraints");
    System.out.println("  :indexes [table]        show indexed columns");
    System.out.println("  :stats [table]          optimizer statistics");
    System.out.println("  :metrics                runtime metrics");
    System.out.println("  :tx / :transactions     transaction state");
    System.out.println("  :migrations             schema migration history");
    System.out.println("  :history [n]             recent SQL commands");
    System.out.println("  :clear                  clear command history");
    System.out.println("  :benchmark              run lightweight shell benchmark");
    System.out.println("  :export <file>          export portable SQL");
    System.out.println("  :import <file>          execute SQL script");
    System.out.println("  :version                show ForgeDB version");
    System.out.println("  :help / :quit           help or exit");
    System.out.println("SQL: BEGIN, COMMIT, ROLLBACK, CREATE, ALTER, INSERT, UPDATE, DELETE, SELECT, EXPLAIN");
  }

  private static void printTables(Database db) {
    if (db.tables().isEmpty()) { System.out.println("(no tables)"); return; }
    for (String t : db.tables()) System.out.printf("%-24s %d row(s)%n", t, db.count(t));
  }

  private static void printSchema(Database db, String requested) {
    boolean found = false;
    for (String table : db.tables()) {
      if (requested != null && !requested.equalsIgnoreCase(table)) continue;
      found = true; System.out.println(table + ":");
      for (SqlStatement.ColumnDef d : db.definitions(table)) {
        List<String> flags = new ArrayList<>();
        if (d.primaryKey()) flags.add("PK"); if (d.unique()) flags.add("UNIQUE"); if (d.notNull()) flags.add("NOT NULL");
        System.out.printf("  %-20s %-10s %s%n", d.name(), d.type(), flags.isEmpty() ? "" : String.join(", ", flags));
      }
    }
    if (!found && requested != null) System.out.println("ERROR: unknown table: " + requested);
  }

  private static void printIndexes(Database db, String requested) {
    for (String table : db.tables()) {
      if (requested != null && !requested.equalsIgnoreCase(table)) continue;
      System.out.println(table + ": " + (db.indexedColumns(table).isEmpty() ? "(none)" : String.join(", ", db.indexedColumns(table))));
    }
    if (requested != null && !db.tables().stream().anyMatch(t -> t.equalsIgnoreCase(requested))) System.out.println("ERROR: unknown table: " + requested);
  }

  private static void printMigrations(Database db) {
    System.out.println("schema_version=" + db.schemaVersion());
    List<String> h = db.migrationHistory();
    if (h.isEmpty()) { System.out.println("(no migrations)"); return; }
    for (int i=0;i<h.size();i++) System.out.println((i+1) + ". " + h.get(i));
  }

  private static void printTx(Database db) {
    System.out.println("state=" + db.transactionState() + (db.inTransaction() ? " txid=" + db.currentTransactionId() : ""));
  }

  private static void printMetrics(Database db) { db.metrics().snapshot().forEach((k,v) -> System.out.println("  " + k + "=" + v)); }

  private static void printStats(Database db, String requested) {
    if (requested == null) { for (String t : db.tables()) System.out.println(db.statistics(t)); return; }
    if (!db.tables().contains(requested)) { System.out.println("ERROR: unknown table: " + requested); return; }
    System.out.println(db.statistics(requested));
  }

  private static void printHistory(List<String> history, String arg) {
    int n = 20; try { if (!arg.isBlank()) n = Math.max(1, Integer.parseInt(arg)); } catch (NumberFormatException e) { System.out.println("ERROR: history count must be numeric"); return; }
    int start = Math.max(0, history.size() - n);
    for (int i=start;i<history.size();i++) System.out.println((i+1) + "  " + history.get(i).replace('\n',' '));
    if (history.isEmpty()) System.out.println("(empty history)");
  }

  private static void runBenchmark(Database db) {
    long started = System.nanoTime(); int rows=0;
    for (String t : db.tables()) rows += db.count(t);
    long micros = (System.nanoTime()-started)/1_000;
    System.out.println("benchmark: count-visible-rows");
    System.out.println("tables=" + db.tables().size() + " rows=" + rows + " elapsed=" + micros + " µs");
  }

  private static void exportDatabase(Database db, Path file) throws IOException {
    if (file == null) throw new IllegalArgumentException("usage: :export <file>");
    try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
      out.write("-- ForgeDB portable SQL export 1.0.0\n");
      for (String table : db.tables()) {
        out.write("CREATE TABLE " + table + " (\n");
        List<SqlStatement.ColumnDef> defs=db.definitions(table);
        for(int i=0;i<defs.size();i++) { var d=defs.get(i); out.write("  "+d.name()+" "+d.type()+(d.primaryKey()?" PRIMARY KEY":"")+(d.unique()?" UNIQUE":"")+(d.notNull()?" NOT NULL":"")+(i+1<defs.size()?",":"")+"\n"); }
        out.write(");\n");
        for (List<String> row : db.rows(table)) { out.write("INSERT INTO "+table+" VALUES ("+row.stream().map(ForgeDbCli::sqlLiteral).reduce((a,b)->a+", "+b).orElse("")+ ");\n"); }
      }
    }
    System.out.println("exported: " + file);
  }

  private static void importSql(SqlEngine engine, Path file, List<String> history, Path historyFile) throws IOException {
    if (file == null) throw new IllegalArgumentException("usage: :import <file>");
    String content=Files.readString(file, StandardCharsets.UTF_8);
    for(String statement: splitSql(content)) { if(statement.isBlank() || statement.stripLeading().startsWith("--")) continue; String sql=statement.trim()+";"; history.add(sql); executeSql(sql,engine); }
    saveHistory(historyFile,history); System.out.println("import complete: " + file);
  }

  private static String sqlLiteral(String value) { if(value==null || "NULL".equalsIgnoreCase(value)) return "NULL"; return "'"+value.replace("'","''")+"'"; }

  private static List<String> splitSql(String content) { List<String> out=new ArrayList<>(); StringBuilder b=new StringBuilder(); boolean quote=false; for(int i=0;i<content.length();i++){char c=content.charAt(i); if(c=='\'' && (i==0 || content.charAt(i-1)!='\\')) quote=!quote; if(c==';'&&!quote){out.add(b.toString());b.setLength(0);} else b.append(c);} if(!b.isEmpty())out.add(b.toString()); return out; }

  private static Path safePath(String arg) { if(arg.isBlank()) return null; Path p=Path.of(arg).normalize(); if(p.toString().contains("..")) throw new IllegalArgumentException("path traversal is not allowed"); return p.toAbsolutePath(); }

  private static void loadHistory(Path file,List<String> history){ if(!Files.isRegularFile(file))return; try{for(String l:Files.readAllLines(file,StandardCharsets.UTF_8))if(!l.isBlank())history.add(l);}catch(IOException ignored){} }
  private static void saveHistory(Path file,List<String> history){ try{Path parent=file.getParent();if(parent!=null)Files.createDirectories(parent); int start=Math.max(0,history.size()-500);Files.write(file,history.subList(start,history.size()),StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);}catch(IOException ignored){} }

  private static void printRows(List<List<String>> rows) {
    if(rows.isEmpty())return; int cols=rows.stream().mapToInt(List::size).max().orElse(0); int[] w=new int[cols];
    for(List<String> r:rows)for(int i=0;i<r.size();i++)w[i]=Math.min(60,Math.max(w[i],String.valueOf(r.get(i)).length()));
    String border=border(w);System.out.println(border);for(List<String> r:rows){StringBuilder o=new StringBuilder("|");for(int i=0;i<cols;i++){String v=i<r.size()?String.valueOf(r.get(i)):"";if(v.length()>60)v=v.substring(0,57)+"...";o.append(' ').append(pad(v,w[i])).append(" |");}System.out.println(o);}System.out.println(border);System.out.println(rows.size()+" row(s)");
  }
  private static String border(int[] w){StringBuilder s=new StringBuilder("+");for(int x:w)s.append("-").append("-".repeat(x)).append("-+");return s.toString();}
  private static String pad(String v,int w){return v+" ".repeat(Math.max(0,w-v.length()));}
}
