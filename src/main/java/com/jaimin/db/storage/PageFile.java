package com.jaimin.db.storage;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Fixed-size page file with typed pages, reuse of freed pages, and corruption guards. */
public final class PageFile implements Closeable {
    private final RandomAccessFile file;
    private final NavigableSet<Long> freePages = new TreeSet<>();

    public PageFile(Path path) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        file = new RandomAccessFile(path.toFile(), "rw");
        if (file.length() % Page.PAGE_SIZE != 0) throw new IOException("corrupt page file: partial page detected");
        rebuildFreePageIndex();
    }

    public synchronized long pageCount() throws IOException { return file.length() / Page.PAGE_SIZE; }

    public synchronized Page read(long pageId) throws IOException {
        validatePageId(pageId);
        long offset = Math.multiplyExact(pageId, (long) Page.PAGE_SIZE);
        if (offset + Page.PAGE_SIZE > file.length()) throw new EOFException("page " + pageId + " not found");
        byte[] bytes = new byte[Page.PAGE_SIZE];
        file.seek(offset);
        file.readFully(bytes);
        return new Page(pageId, bytes);
    }

    public synchronized void write(Page page) throws IOException {
        if (page == null) throw new IllegalArgumentException("page cannot be null");
        validatePageId(page.pageId());
        long offset = Math.multiplyExact(page.pageId(), (long) Page.PAGE_SIZE);
        if (offset + Page.PAGE_SIZE > file.length()) throw new EOFException("cannot write beyond allocated page file");
        file.seek(offset);
        file.write(page.bytes());
        file.getFD().sync();
        if (page.isFree()) freePages.add(page.pageId()); else freePages.remove(page.pageId());
    }

    public synchronized Page allocate() throws IOException { return allocate(PageType.DATA); }

    public synchronized Page allocate(PageType type) throws IOException {
        if (type == null || type == PageType.FREE) throw new IllegalArgumentException("allocated page must have a non-free type");
        long id;
        if (!freePages.isEmpty()) {
            id = freePages.pollFirst();
        } else {
            id = pageCount();
            file.setLength((id + 1) * (long) Page.PAGE_SIZE);
        }
        Page page = new Page(id);
        page.writePayload(new byte[0], type);
        write(page);
        return page;
    }

    public synchronized void free(long pageId) throws IOException {
        Page page = read(pageId);
        page.markFree();
        write(page);
    }

    public synchronized boolean isFree(long pageId) throws IOException { return read(pageId).isFree(); }
    public synchronized long freePageCount() { return freePages.size(); }
    public synchronized List<Long> freePages() { return List.copyOf(freePages); }

    private void rebuildFreePageIndex() throws IOException {
        freePages.clear();
        long count = pageCount();
        for (long id = 0; id < count; id++) {
            byte[] bytes = new byte[Page.PAGE_SIZE];
            file.seek(id * (long) Page.PAGE_SIZE);
            file.readFully(bytes);
            try {
                Page page = new Page(id, bytes);
                if (page.isFree()) freePages.add(id);
            } catch (RuntimeException ignored) {
                // Uninitialised pages are not considered reusable until a valid header is written.
            }
        }
    }

    private void validatePageId(long pageId) {
        if (pageId < 0) throw new IllegalArgumentException("pageId must be non-negative");
    }

    @Override public void close() throws IOException { file.close(); }
}
