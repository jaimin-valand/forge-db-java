package com.jaimin.db;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.jaimin.db.index.BPlusTree;
import com.jaimin.db.query.QueryPlan;
import com.jaimin.db.query.QueryPlanner;
import com.jaimin.db.query.TableStatistics;
import com.jaimin.db.sql.SqlStatement;

/** ForgeDB relational core with lightweight MVCC row versions and durable snapshots. */
public final class Database {
  private static final int FORMAT = 5;
  private final Map<String, Table> tables = new LinkedHashMap<>();
  private final Path file;
  private long lastAppliedLsn;
  private boolean recoveryMode;
  private long schemaVersion = 1;
  private final List<String> migrationHistory = new ArrayList<>();
  private final TransactionManager transactions;
  private final Metrics metrics = new Metrics();
  private final Observability observability = new Observability(metrics);

  public Database(Path file) throws IOException {
    this.file=file;
    if(Files.exists(file)) load();
    Path walPath = file == null ? Path.of("forge-db.wal") : file.resolveSibling(file.getFileName()+".wal");
    this.transactions = new TransactionManager(this, walPath);
    this.transactions.recover();
  }

  public synchronized void begin() throws IOException { metrics.statement(); metrics.transaction(); transactions.begin(); com.jaimin.db.reliability.FaultInjector.hit(com.jaimin.db.reliability.FaultInjector.Point.AFTER_BEGIN); }
  public synchronized void commit() throws IOException { metrics.statement(); metrics.commit(); transactions.commit(); }
  public synchronized void rollback() throws IOException { metrics.statement(); metrics.rollback(); transactions.rollback(); }
  public synchronized boolean inTransaction(){ return transactions.active(); }
  public synchronized TransactionManager.State transactionState(){ return transactions.state(); }
  public synchronized long currentTransactionId(){ return transactions.active() ? transactions.currentTransactionId() : 0L; }
  public synchronized void recordCreate(String sql) throws IOException { transactions.recordCreate(sql); }
  public synchronized void recordInsert(String sql) throws IOException { transactions.recordInsert(sql); }
  public synchronized void recordUpdate(String sql) throws IOException { transactions.recordUpdate(sql); }
  public synchronized void recordDelete(String sql) throws IOException { transactions.recordDelete(sql); }

  public synchronized void createTable(String name,List<String> columns) throws IOException {
    List<SqlStatement.ColumnDef> defs=columns.stream().map(c->new SqlStatement.ColumnDef(c,"TEXT",false,false,false)).toList();
    createTableWithDefinitions(name,defs);
  }
  public synchronized void createTableWithDefinitions(String name,List<SqlStatement.ColumnDef> defs) throws IOException {
    requireName(name); SecurityLimits.value(name, "table name"); if(tables.size() >= SecurityLimits.MAX_TABLES) throw new IllegalArgumentException("maximum table count exceeded"); if(tables.containsKey(name)) throw new IllegalArgumentException("table exists: "+name);
    if(defs.isEmpty() || defs.size() > SecurityLimits.MAX_COLUMNS_PER_TABLE) throw new IllegalArgumentException("invalid column count; maximum is " + SecurityLimits.MAX_COLUMNS_PER_TABLE); Set<String> seen=new HashSet<>(); for(var d:defs){requireName(d.name()); SecurityLimits.value(d.name(), "column name");if(!seen.add(d.name()))throw new IllegalArgumentException("duplicate column: "+d.name());validateType(d.type());}
    Table t=new Table(name,defs);t.rebuildIndexes();tables.put(name,t);persist();
  }
  public synchronized void addColumn(String table, SqlStatement.ColumnDef def, String defaultValue, String migrationSql) throws IOException {
    Table t=table(table); requireName(def.name()); validateType(def.type());
    if(t.columns.contains(def.name())) throw new IllegalArgumentException("column exists: "+def.name());
    if(def.notNull() && defaultValue==null) throw new IllegalArgumentException("NOT NULL column requires DEFAULT: "+def.name());
    validateValue(def, defaultValue);
    for(Row r:t.rows) r.data.add(defaultValue);
    t.defs=new ArrayList<>(t.defs); t.defs.add(def); t.columns=new ArrayList<>(t.columns); t.columns.add(def.name()); t.rebuildIndexes();
    if(migrationHistory.size() >= SecurityLimits.MAX_MIGRATION_HISTORY) throw new IllegalArgumentException("migration history limit exceeded"); schemaVersion++; migrationHistory.add(migrationSql); persist();
  }

  public synchronized long schemaVersion(){ return schemaVersion; }
  public synchronized List<String> migrationHistory(){ return List.copyOf(migrationHistory); }

  public synchronized void insert(String table,List<String> values) throws IOException { metrics.statement(); metrics.insert();
    Table t=table(table); if(t.rows.size() >= SecurityLimits.MAX_ROWS_PER_TABLE) throw new IllegalArgumentException("maximum row count exceeded"); if(values.size()!=t.columns.size())throw new IllegalArgumentException("column count mismatch");
    for(int c=0;c<values.size();c++){ SecurityLimits.value(values.get(c), "value"); validateValue(t.defs.get(c),values.get(c)); }
    for(int c=0;c<values.size();c++){var d=t.defs.get(c);if((d.primaryKey()||d.unique())&&t.indexes.get(c).get(values.get(c))!=null&&!t.indexes.get(c).get(values.get(c)).isEmpty())throw new IllegalArgumentException("duplicate value for unique column: "+d.name());}
    t.rows.add(new Row(new ArrayList<>(values), inTransaction() ? currentTransactionId() : 0, 0)); t.rebuildIndexes(); persist(); com.jaimin.db.reliability.FaultInjector.hit(com.jaimin.db.reliability.FaultInjector.Point.AFTER_MUTATION);
  }

  public synchronized int update(String table,List<SqlStatement.Assignment> assignments,List<SqlStatement.Condition> conditions) throws IOException { metrics.statement(); metrics.update();
    Table t=table(table); Map<Integer,String> assignmentMap=new HashMap<>();
    for(var a:assignments){ int c=t.columnIndex(a.column()); validateValue(t.defs.get(c),a.value()); assignmentMap.put(c,a.value()); }
    long txId=currentTransactionId();
    List<Row> visible=t.visibleRows(txId);
    List<Row> replacing=new ArrayList<>();
    List<List<String>> candidates=new ArrayList<>();
    for(Row row:visible){
      if(!matches(t,row.data,conditions)) continue;
      List<String> next=new ArrayList<>(row.data);
      for(var e:assignmentMap.entrySet()) next.set(e.getKey(),e.getValue());
      replacing.add(row); candidates.add(next);
    }
    // Statement-level atomicity: validate every candidate before mutating any row.
    validateUpdateCandidates(t,replacing,candidates,txId);
    for(int i=0;i<replacing.size();i++){
      Row row=replacing.get(i);
      row.deletedTx=inTransaction()?txId:-1;
      t.rows.add(new Row(candidates.get(i),inTransaction()?txId:0,0));
    }
    int changed=replacing.size();
    t.rebuildIndexes(); persist(); com.jaimin.db.reliability.FaultInjector.hit(com.jaimin.db.reliability.FaultInjector.Point.AFTER_MUTATION); return changed;
  }

  public synchronized int delete(String table,List<SqlStatement.Condition> conditions) throws IOException { metrics.statement(); metrics.delete();
    Table t=table(table); int changed=0; for(Row row:t.visibleRows(currentTransactionId())){ if(matches(t,row.data,conditions)){row.deletedTx=inTransaction()?currentTransactionId():-1; changed++;} }
    t.rebuildIndexes(); persist(); com.jaimin.db.reliability.FaultInjector.hit(com.jaimin.db.reliability.FaultInjector.Point.AFTER_MUTATION); return changed;
  }

  public synchronized List<List<String>> selectAll(String table){ metrics.statement(); metrics.select(); metrics.tableScan(); return table(table).visibleData(currentTransactionId());}
  public synchronized List<String> columns(String table){return List.copyOf(table(table).columns);}
  public synchronized List<SqlStatement.ColumnDef> definitions(String table){return List.copyOf(table(table).defs);}
  public synchronized List<String> indexedColumns(String table){Table t=table(table);return t.indexes.keySet().stream().sorted().map(t.columns::get).toList();}
  public synchronized List<List<String>> rows(String table){return table(table).visibleData(currentTransactionId());}
  public synchronized List<List<String>> selectWhere(String table,String column,String value){ metrics.statement(); metrics.select(); metrics.indexLookup(); Table t=table(table);Integer idx=t.columnIndex(column);List<Integer> hits=t.indexes.get(idx).get(value);List<List<String>> out=new ArrayList<>();if(hits!=null)for(int i:hits){Row r=t.rows.get(i);if(t.visible(r,currentTransactionId()))out.add(new ArrayList<>(r.data));}return out;}
  public synchronized QueryPlan explain(String table,String column,String value){
    metrics.statement();
    Table t=table(table);
    Set<String> indexed=new HashSet<>();
    for(Integer i:t.indexes.keySet()) indexed.add(t.columns.get(i));
    return new QueryPlanner().plan(table,column,value,indexed,statistics(t));
  }

  public synchronized QueryPlan explain(String table,List<SqlStatement.Condition> conditions){
    metrics.statement();
    Table t=table(table);
    Set<String> indexed=new HashSet<>();
    for(Integer i:t.indexes.keySet()) indexed.add(t.columns.get(i));
    return new QueryPlanner().plan(table,conditions,indexed,statistics(t));
  }

  public synchronized TableStatistics statistics(String table){ return statistics(table(table)); }

  private TableStatistics statistics(Table t){
    List<Row> visible=t.visibleRows(currentTransactionId());
    Map<String,Integer> distinct=new LinkedHashMap<>();
    for(int c=0;c<t.columns.size();c++){
      Set<String> values=new HashSet<>();
      for(Row r:visible) values.add(r.data.get(c));
      distinct.put(t.columns.get(c),values.size());
    }
    return new TableStatistics(t.name,visible.size(),distinct);
  }
  public synchronized int count(String table){ metrics.statement(); return table(table).visibleRows(currentTransactionId()).size(); }
  public synchronized Set<String> tables(){return Collections.unmodifiableSet(tables.keySet());}

  private Table table(String n){Table t=tables.get(n);if(t==null)throw new IllegalArgumentException("unknown table: "+n);return t;}
  private static void requireName(String n){if(n==null||!n.matches("[A-Za-z_][A-Za-z0-9_]*"))throw new IllegalArgumentException("invalid identifier");}
  private static void validateType(String type){String t=type.toUpperCase(Locale.ROOT);if(!Set.of("TEXT","INT","INTEGER").contains(t))throw new IllegalArgumentException("unsupported type: "+type);}
  private static void validateValue(SqlStatement.ColumnDef d,String value){SecurityLimits.value(value, "value");String type=d.type().toUpperCase(Locale.ROOT);if((d.notNull()||d.primaryKey())&&value==null)throw new IllegalArgumentException("NULL not allowed: "+d.name());if(type.equals("INT")||type.equals("INTEGER")){try{Integer.parseInt(value);}catch(Exception e){throw new IllegalArgumentException("invalid integer for "+d.name());}}}
  private static boolean compare(String actual,String op,String expected){
    int cmp;
    try { cmp=Integer.compare(Integer.parseInt(actual),Integer.parseInt(expected)); }
    catch(NumberFormatException e){ cmp=actual.compareTo(expected); }
    return switch(op){case "="->actual.equals(expected);case "!=","<>"->!actual.equals(expected);case ">"->cmp>0;case ">="->cmp>=0;case "<"->cmp<0;case "<="->cmp<=0;default->false;};
  }
  private static boolean matches(Table t,List<String> row,List<SqlStatement.Condition> cs){for(var c:cs){int idx=t.columnIndex(c.column());if(!compare(row.get(idx),c.operator(),c.value()))return false;}return true;}
  private static void validateUpdateCandidates(Table t,List<Row> replacing,List<List<String>> candidates,long txId){
    for(int i=0;i<candidates.size();i++){
      List<String> row=candidates.get(i);
      for(int c=0;c<row.size();c++){
        var d=t.defs.get(c); validateValue(d,row.get(c));
        if(!(d.primaryKey()||d.unique())) continue;
        for(Row other:t.rows){
          if(replacing.contains(other)) continue;
          if(!t.visible(other,txId)) continue;
          if(Objects.equals(other.data.get(c),row.get(c))) throw new IllegalArgumentException("duplicate value for unique column: "+d.name());
        }
        for(int j=0;j<i;j++){
          if(Objects.equals(candidates.get(j).get(c),row.get(c))) throw new IllegalArgumentException("duplicate value for unique column: "+d.name());
        }
      }
    }
  }

  void persistForTransactionCommit() throws IOException { persistInternal(); }
  private void persist() throws IOException {if(inTransaction() && !recoveryMode)return; persistInternal();}
  private void persistInternal() throws IOException {
    if(file==null)return; Path parent=file.toAbsolutePath().getParent(); if(parent!=null)Files.createDirectories(parent); Path tmp=file.resolveSibling(file.getFileName()+".tmp");
    try(DataOutputStream o=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(tmp)))){
      o.writeInt(FORMAT);o.writeLong(lastAppliedLsn);o.writeLong(schemaVersion);o.writeInt(migrationHistory.size());for(String m:migrationHistory)o.writeUTF(m);o.writeInt(tables.size());
      for(Table t:tables.values()){o.writeUTF(t.name);o.writeInt(t.defs.size());for(var d:t.defs){o.writeUTF(d.name());o.writeUTF(d.type());o.writeBoolean(d.primaryKey());o.writeBoolean(d.unique());o.writeBoolean(d.notNull());}
        o.writeInt(t.rows.size());for(Row r:t.rows){o.writeLong(r.createdTx);o.writeLong(r.deletedTx);for(String v:r.data)o.writeUTF(v);}}
      o.flush();
    }
    try{Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}
  }
  private void load() throws IOException {
    try(DataInputStream in=new DataInputStream(Files.newInputStream(file))){int format=in.readInt();if(format<3||format>FORMAT)throw new IOException("unsupported ForgeDB format: "+format);lastAppliedLsn=in.readLong();if(format>=5){schemaVersion=in.readLong();int mh=in.readInt();for(int i=0;i<mh;i++)migrationHistory.add(in.readUTF());}int n=in.readInt();for(int i=0;i<n;i++){String name=in.readUTF();int c=in.readInt();List<SqlStatement.ColumnDef> defs=new ArrayList<>();for(int j=0;j<c;j++)defs.add(new SqlStatement.ColumnDef(in.readUTF(),in.readUTF(),in.readBoolean(),in.readBoolean(),in.readBoolean()));Table t=new Table(name,defs);int rows=in.readInt();for(int r=0;r<rows;r++){long created=format>=4?in.readLong():0;long deleted=format>=4?in.readLong():0;List<String> row=new ArrayList<>();for(int j=0;j<c;j++)row.add(in.readUTF());t.rows.add(new Row(row,created,deleted));}t.rebuildIndexes();tables.put(name,t);}}
  }

  public synchronized Metrics metrics(){ return metrics; }

  synchronized long lastAppliedLsn(){ return lastAppliedLsn; }
  synchronized void setLastAppliedLsn(long lsn){ lastAppliedLsn=lsn; }
  synchronized Snapshot snapshot(){Map<String,Table> copy=new LinkedHashMap<>();for(Table t:tables.values()){Table c=t.copy();copy.put(c.name,c);}return new Snapshot(copy,lastAppliedLsn,schemaVersion,List.copyOf(migrationHistory));}
  synchronized void restore(Snapshot snap){tables.clear();for(Table t:snap.tables.values()){Table c=t.copy();tables.put(c.name,c);}lastAppliedLsn=snap.lsn;schemaVersion=snap.schemaVersion; migrationHistory.clear(); migrationHistory.addAll(snap.migrationHistory);}

  synchronized void finalizeCommittedVersions(long txId){for(Table t:tables.values()){t.rows.removeIf(r->r.deletedTx==txId);for(Row r:t.rows){if(r.createdTx==txId)r.createdTx=0;if(r.deletedTx==txId)r.deletedTx=0;}}for(Table t:tables.values())t.rebuildIndexes();}
  synchronized void applyRecoverySql(String sql) throws IOException {recoveryMode=true;try{var st=new com.jaimin.db.sql.SqlParser().parse(sql);if(st instanceof SqlStatement.CreateTable c)createTableWithDefinitions(c.name(),c.columns());else if(st instanceof SqlStatement.AlterTableAddColumn a)addColumn(a.table(),a.column(),a.defaultValue(),"RECOVERY: "+sql);else if(st instanceof SqlStatement.Insert i)insert(i.table(),i.values());else if(st instanceof SqlStatement.Update u)update(u.table(),u.assignments(),u.conditions());else if(st instanceof SqlStatement.Delete d)delete(d.table(),d.conditions());}finally{recoveryMode=false;}}
  record Snapshot(Map<String,Table> tables,long lsn,long schemaVersion,List<String> migrationHistory) {}

  private static final class Row {final List<String> data;long createdTx;long deletedTx;Row(List<String>d,long c,long del){data=d;createdTx=c;deletedTx=del;}}
  private static final class Table {
    final String name;List<SqlStatement.ColumnDef> defs;List<String> columns;final List<Row> rows=new ArrayList<>();final Map<Integer,BPlusTree<String,Integer>> indexes=new HashMap<>();
    Table(String n,List<SqlStatement.ColumnDef>d){name=n;defs=List.copyOf(d);columns=defs.stream().map(SqlStatement.ColumnDef::name).toList();}
    Integer columnIndex(String c){int i=columns.indexOf(c);if(i<0)throw new IllegalArgumentException("unknown column: "+c);return i;}
    boolean visible(Row r,long tx){if(r.createdTx!=0&&r.createdTx!=tx)return false;if(r.deletedTx!=0&&r.deletedTx==tx)return false;if(r.deletedTx!=0&&r.deletedTx!=tx)return false;return true;}
    List<Row> visibleRows(long tx){List<Row> out=new ArrayList<>();for(Row r:rows)if(visible(r,tx))out.add(r);return out;}
    List<List<String>> visibleData(long tx){List<List<String>> out=new ArrayList<>();for(Row r:visibleRows(tx))out.add(new ArrayList<>(r.data));return out;}
    void rebuildIndexes(){indexes.clear();for(int c=0;c<columns.size();c++){BPlusTree<String,Integer> tree=new BPlusTree<>();for(int r=0;r<rows.size();r++)if(rows.get(r).deletedTx==0)tree.put(rows.get(r).data.get(c),r);indexes.put(c,tree);}}
    Table copy(){Table c=new Table(name,defs);for(Row r:rows)c.rows.add(new Row(new ArrayList<>(r.data),r.createdTx,r.deletedTx));c.rebuildIndexes();return c;}
  }
  public Observability observability() { return observability; }

}
