package com.jaimin.db.storage;

import java.io.IOException;

/** Cheap storage-engine invariant checks used by tests and diagnostics. */
public final class StorageInvariant {
    private StorageInvariant() {}

    public static void verify(PageFile file) throws IOException {
        long count = file.pageCount();
        if (file.freePageCount() < 0 || file.freePageCount() > count) throw new AssertionError("invalid free-page count");
        for (long id = 0; id < count; id++) {
            Page page = file.read(id);
            if (page.pageId() != id) throw new AssertionError("page id mismatch at " + id);
            if (page.freeSpace() < 0 || page.freeSpace() > Page.PAYLOAD_CAPACITY) throw new AssertionError("invalid free space at " + id);
            if (page.isFree() && !file.isFree(id)) throw new AssertionError("free-page index mismatch at " + id);
        }
    }
}
