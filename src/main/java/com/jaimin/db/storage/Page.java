package com.jaimin.db.storage;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

/** Fixed-size physical page with a versioned header and integrity metadata. */
public final class Page {
    public static final int PAGE_SIZE = 4096;
    private static final int MAGIC = 0x46444732; // FDG2
    private static final int LEGACY_MAGIC = 0x46444731; // FDG1
    public static final short FORMAT_VERSION = 2;
    public static final int HEADER_SIZE = 32;
    public static final int LEGACY_HEADER_SIZE = 24;
    public static final int PAYLOAD_CAPACITY = PAGE_SIZE - HEADER_SIZE;

    private final long pageId;
    private final byte[] data;
    private PageType type;
    private int freeSpace;
    private boolean dirty;

    public Page(long pageId) { this(pageId, new byte[PAGE_SIZE]); }

    public Page(long pageId, byte[] data) {
        if (pageId < 0) throw new IllegalArgumentException("pageId must be non-negative");
        if (data.length != PAGE_SIZE) throw new IllegalArgumentException("page must be 4096 bytes");
        this.pageId = pageId;
        this.data = data.clone();
        this.type = PageType.DATA;
        this.freeSpace = PAYLOAD_CAPACITY;
        detectMetadata();
    }

    public long pageId() { return pageId; }
    public byte[] bytes() { return data.clone(); }
    public boolean dirty() { return dirty; }
    public void markDirty() { dirty = true; }
    public void clearDirty() { dirty = false; }
    public PageType type() { return type; }
    public int freeSpace() { return freeSpace; }
    public boolean isFree() { return type == PageType.FREE; }

    public void setType(PageType type) {
        if (type == null) throw new IllegalArgumentException("page type cannot be null");
        this.type = type;
        dirty = true;
    }

    public void writePayload(byte[] payload) {
        if (payload.length > PAYLOAD_CAPACITY) throw new IllegalArgumentException("payload too large");
        writePayload(payload, type);
    }

    public void writePayload(byte[] payload, PageType type) {
        if (payload.length > PAYLOAD_CAPACITY) throw new IllegalArgumentException("payload too large");
        if (type == null) throw new IllegalArgumentException("page type cannot be null");
        Arrays.fill(data, (byte) 0);
        this.type = type;
        this.freeSpace = PAYLOAD_CAPACITY - payload.length;
        ByteBuffer b = ByteBuffer.wrap(data);
        b.putInt(MAGIC)
         .putShort(FORMAT_VERSION)
         .putShort((short) type.code())
         .putLong(pageId)
         .putInt(payload.length)
         .putInt(freeSpace)
         .putInt(crc32(payload))
         .putInt(0);
        b.put(payload);
        dirty = true;
    }

    public void markFree() {
        writePayload(new byte[0], PageType.FREE);
    }

    public byte[] readPayload() {
        Header h = parseHeader();
        byte[] payload = new byte[h.length];
        ByteBuffer.wrap(data, h.headerSize, h.length).get(payload);
        if (crc32(payload) != h.checksum) throw new IllegalStateException("page checksum mismatch");
        return payload;
    }

    private Header parseHeader() {
        ByteBuffer b = ByteBuffer.wrap(data);
        int magic = b.getInt();
        if (magic == MAGIC) {
            short version = b.getShort();
            if (version != FORMAT_VERSION) throw new IllegalStateException("unsupported page format version: " + version);
            PageType parsedType = PageType.fromCode(Short.toUnsignedInt(b.getShort()));
            long storedId = b.getLong();
            if (storedId != pageId) throw new IllegalStateException("page id mismatch");
            int len = b.getInt();
            int storedFree = b.getInt();
            int checksum = b.getInt();
            b.getInt();
            if (len < 0 || len > PAYLOAD_CAPACITY) throw new IllegalStateException("invalid payload length");
            if (storedFree != PAYLOAD_CAPACITY - len) throw new IllegalStateException("invalid free-space metadata");
            this.type = parsedType;
            this.freeSpace = storedFree;
            return new Header(HEADER_SIZE, len, checksum);
        }
        if (magic == LEGACY_MAGIC) {
            long storedId = b.getLong();
            if (storedId != pageId) throw new IllegalStateException("page id mismatch");
            int len = b.getInt();
            int checksum = b.getInt();
            b.getInt();
            if (len < 0 || len > PAGE_SIZE - LEGACY_HEADER_SIZE) throw new IllegalStateException("invalid payload length");
            this.type = PageType.DATA;
            this.freeSpace = PAGE_SIZE - LEGACY_HEADER_SIZE - len;
            return new Header(LEGACY_HEADER_SIZE, len, checksum);
        }
        throw new IllegalStateException("invalid page magic");
    }

    private void detectMetadata() {
        int magic = ByteBuffer.wrap(data).getInt();
        if (magic != MAGIC && magic != LEGACY_MAGIC) return;
        try { parseHeader(); } catch (RuntimeException ignored) { /* validation happens on read */ }
    }

    private static int crc32(byte[] value) {
        CRC32 crc = new CRC32();
        crc.update(value);
        return (int) crc.getValue();
    }

    private record Header(int headerSize, int length, int checksum) {}
}
