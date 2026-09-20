package com.jaimin.db;

import com.jaimin.db.storage.*;
import java.nio.file.*;
import java.util.*;

/** Deterministic property-style checks without a third-party property-testing library. */
public final class PropertyInvariantTest {
    public static void main(String[] args) throws Exception {
        pageAllocationAndReuseIsStable();
        bufferPoolNeverExceedsCapacity();
        sqlRoundTripPreservesRowCount();
        System.out.println("PASS: property/invariant checks");
    }

    private static void pageAllocationAndReuseIsStable() throws Exception {
        Path path = Files.createTempFile("forge-properties", ".pages");
        Files.deleteIfExists(path);
        try (PageFile file = new PageFile(path)) {
            List<Long> ids = new ArrayList<>();
            PageType[] types = {PageType.DATA, PageType.INDEX, PageType.CATALOG, PageType.WAL_CHECKPOINT};
            for (int i = 0; i < 40; i++) ids.add(file.allocate(types[i % types.length]).pageId());
            for (int i = 0; i < ids.size(); i += 2) file.free(ids.get(i));
            StorageInvariant.verify(file);
            long reused = file.allocate(PageType.DATA).pageId();
            if (!ids.contains(reused)) throw new AssertionError("allocator did not reuse a free page");
            StorageInvariant.verify(file);
        } finally { Files.deleteIfExists(path); }
    }

    private static void bufferPoolNeverExceedsCapacity() throws Exception {
        Path path = Files.createTempFile("forge-buffer", ".pages");
        Files.deleteIfExists(path);
        try (PageFile file = new PageFile(path); BufferPool pool = new BufferPool(file, 3)) {
            for (int i = 0; i < 12; i++) {
                pool.newPage();
                if (pool.size() > 3) throw new AssertionError("buffer pool exceeded capacity");
            }
            if (pool.evictions() == 0) throw new AssertionError("expected buffer evictions");
            if (pool.flushes() == 0) throw new AssertionError("expected dirty-page flushes");
        } finally { Files.deleteIfExists(path); }
    }

    private static void sqlRoundTripPreservesRowCount() throws Exception {
        Path dir = Files.createTempDirectory("forge-sql-property-");
        Path path = dir.resolve("db");
        try {
            Database db = new Database(path);
            SqlEngine sql = new SqlEngine(db);
            sql.execute("CREATE TABLE items (id INT PRIMARY KEY, value INT)");
            int expected = 0;
            for (int i = 1; i <= 75; i++) {
                sql.execute("INSERT INTO items VALUES (" + i + ", " + (i * 7) + ")");
                expected++;
            }
            if (db.count("items") != expected) throw new AssertionError("row-count invariant failed before reload");
            Database reopened = new Database(path);
            if (reopened.count("items") != expected) throw new AssertionError("row-count invariant failed after reload");
        } finally {
            try (var walk = Files.walk(dir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ignored) {} });
            }
        }
    }
}
