package com.jaimin.db.storage;

import java.nio.file.*;

public final class StorageEngineV2Test {
    public static void main(String[] args) throws Exception {
        Path path = Files.createTempFile("forgedb-storage-v2", ".db");
        Files.deleteIfExists(path);
        try (PageFile file = new PageFile(path)) {
            Page data = file.allocate(PageType.DATA);
            if (data.type() != PageType.DATA) throw new AssertionError("wrong data type");
            data.writePayload("storage-v2".getBytes(), PageType.DATA);
            file.write(data);

            Page index = file.allocate(PageType.INDEX);
            if (index.type() != PageType.INDEX) throw new AssertionError("wrong index type");
            long indexId = index.pageId();
            file.free(indexId);
            if (!file.isFree(indexId)) throw new AssertionError("page was not freed");
            Page reused = file.allocate(PageType.CATALOG);
            if (reused.pageId() != indexId || reused.type() != PageType.CATALOG) throw new AssertionError("free page was not reused");

            StorageInvariant.verify(file);
        }

        try (PageFile file = new PageFile(path); BufferPool pool = new BufferPool(file, 1)) {
            Page first = pool.pin(0);
            boolean rejected = false;
            try { pool.newPage(PageType.INDEX); } catch (IllegalStateException e) { rejected = true; }
            if (!rejected) throw new AssertionError("all-pinned eviction should fail");
            pool.unpin(first.pageId());
            pool.newPage(PageType.INDEX);
            pool.flushAll();
            if (pool.evictions() == 0) throw new AssertionError("expected eviction");
            if (pool.flushes() == 0) throw new AssertionError("expected flush");
        }
        Files.deleteIfExists(path);
        System.out.println("PASS: storage engine 2.0 page types, reuse, pinning and invariants");
    }
}
