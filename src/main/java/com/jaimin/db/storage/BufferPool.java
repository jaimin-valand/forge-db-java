package com.jaimin.db.storage;

import java.io.*;
import java.util.*;

/** LRU buffer pool with dirty writeback, pinning, and lifecycle statistics. */
public final class BufferPool implements Closeable {
    private final PageFile file;
    private final int capacity;
    private final LinkedHashMap<Long, Frame> cache;
    private long hits, misses, evictions, flushes, failedEvictions;

    private static final class Frame {
        final Page page;
        int pins;
        Frame(Page page) { this.page = page; }
    }

    public BufferPool(PageFile file, int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        this.file = Objects.requireNonNull(file, "file");
        this.capacity = capacity;
        cache = new LinkedHashMap<>(capacity, 0.75f, true);
    }

    public synchronized Page get(long id) throws IOException {
        Frame frame = cache.get(id);
        if (frame != null) { hits++; return frame.page; }
        misses++;
        evictIfNeeded();
        Page page = file.read(id);
        cache.put(id, new Frame(page));
        return page;
    }

    public synchronized Page pin(long id) throws IOException {
        Frame frame = cache.get(id);
        if (frame == null) {
            misses++;
            evictIfNeeded();
            frame = new Frame(file.read(id));
            cache.put(id, frame);
        } else hits++;
        frame.pins++;
        return frame.page;
    }

    public synchronized void unpin(long id) {
        Frame frame = cache.get(id);
        if (frame == null) throw new IllegalArgumentException("page is not resident: " + id);
        if (frame.pins == 0) throw new IllegalStateException("page is not pinned: " + id);
        frame.pins--;
    }

    public synchronized Page newPage() throws IOException { return newPage(PageType.DATA); }

    public synchronized Page newPage(PageType type) throws IOException {
        evictIfNeeded();
        Page page = file.allocate(type);
        cache.put(page.pageId(), new Frame(page));
        return page;
    }

    public synchronized void deletePage(long id) throws IOException {
        Frame frame = cache.get(id);
        if (frame != null && frame.pins > 0) throw new IllegalStateException("cannot delete pinned page: " + id);
        cache.remove(id);
        file.free(id);
    }

    public synchronized void flush(long id) throws IOException {
        Frame frame = cache.get(id);
        if (frame != null && frame.page.dirty()) flushFrame(frame);
    }

    public synchronized void flushAll() throws IOException {
        for (Frame frame : cache.values()) if (frame.page.dirty()) flushFrame(frame);
    }

    private void flushFrame(Frame frame) throws IOException {
        file.write(frame.page);
        frame.page.clearDirty();
        flushes++;
    }

    private void evictIfNeeded() throws IOException {
        if (cache.size() < capacity) return;
        Iterator<Map.Entry<Long, Frame>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Frame> entry = it.next();
            Frame victim = entry.getValue();
            if (victim.pins > 0) continue;
            if (victim.page.dirty()) flushFrame(victim);
            it.remove();
            evictions++;
            return;
        }
        failedEvictions++;
        throw new IllegalStateException("buffer pool exhausted: all resident pages are pinned");
    }

    public synchronized int size() { return cache.size(); }
    public synchronized long hits() { return hits; }
    public synchronized long misses() { return misses; }
    public synchronized long evictions() { return evictions; }
    public synchronized long flushes() { return flushes; }
    public synchronized long failedEvictions() { return failedEvictions; }
    public synchronized double hitRatio() {
        long total = hits + misses;
        return total == 0 ? 1.0 : (double) hits / total;
    }
    public synchronized String performanceSummary() {
        return "hits=" + hits + ", misses=" + misses + ", hitRatio=" + String.format(java.util.Locale.ROOT, "%.4f", hitRatio())
            + ", evictions=" + evictions + ", flushes=" + flushes + ", failedEvictions=" + failedEvictions;
    }
    public synchronized int pinnedPages() { return (int) cache.values().stream().filter(f -> f.pins > 0).count(); }

    @Override public synchronized void close() throws IOException { flushAll(); file.close(); }
}
