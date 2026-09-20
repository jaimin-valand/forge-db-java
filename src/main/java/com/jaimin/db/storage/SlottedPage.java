package com.jaimin.db.storage;

import java.nio.ByteBuffer;
import java.util.*;

/** Variable-length record page using a compact slot directory at the front. */
public final class SlottedPage {
    private static final int HEADER = 8; // slot count + free-start
    private final Page page;
    private final List<byte[]> records = new ArrayList<>();
    private int freeStart = HEADER;

    public SlottedPage(Page page) { this.page = page; load(); }
    public int slotCount() { return records.size(); }
    public int freeSpace() { return Page.PAYLOAD_CAPACITY - freeStart - records.size() * 4; }
    public int insert(byte[] record) {
        int needed = record.length + 4; if (needed > freeSpace()) return -1;
        records.add(record.clone()); freeStart += record.length; persist(); return records.size() - 1;
    }
    public byte[] read(int slot) { check(slot); return records.get(slot).clone(); }
    public void delete(int slot) { check(slot); records.set(slot, null); persist(); }
    public void update(int slot, byte[] record) { check(slot); if (record.length > freeSpace() + sizeOf(slot)) throw new IllegalArgumentException("record too large"); records.set(slot, record.clone()); persist(); }
    private int sizeOf(int slot) { return records.get(slot) == null ? 0 : records.get(slot).length; }
    private void check(int slot) { if (slot < 0 || slot >= records.size() || records.get(slot) == null) throw new IllegalArgumentException("invalid record slot"); }
    private void persist() {
        ByteBuffer b = ByteBuffer.allocate(Page.PAYLOAD_CAPACITY); b.putInt(records.size()).putInt(HEADER);
        for (byte[] r : records) { b.putInt(r == null ? -1 : r.length); }
        for (byte[] r : records) if (r != null) b.put(r);
        byte[] payload = Arrays.copyOf(b.array(), b.position()); page.writePayload(payload, PageType.DATA); freeStart = b.position();
    }
    private void load() {
        try { byte[] payload = page.readPayload(); if (payload.length == 0) return; ByteBuffer b = ByteBuffer.wrap(payload); int n=b.getInt(); b.getInt(); int[] lens=new int[n]; for(int i=0;i<n;i++) lens[i]=b.getInt(); for(int len:lens){ if(len<0)records.add(null); else{byte[] r=new byte[len];b.get(r);records.add(r);} } freeStart=payload.length; }
        catch (IllegalStateException e) { /* fresh zero page */ }
    }
}
