package com.jaimin.db;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32;

/** Append-only logical WAL with integrity checks and safe torn-tail handling. */
final class Wal {
  enum Type { BEGIN, CREATE_TABLE, INSERT, UPDATE, DELETE, COMMIT, ABORT }
  record Record(long lsn, long txId, Type type, String payload) {}

  private static final int MAGIC = 0x46445731; // FDW1
  private static final int MAX_PAYLOAD = 16 * 1024 * 1024;
  private static final int MAX_RECORDS_PER_READ = 1_000_000;
  private final Path file;
  private long nextLsn = 1;
  private long nextTxId = 1;

  Wal(Path file) throws IOException {
    this.file = file;
    Path parent = file.toAbsolutePath().getParent();
    if (parent != null) Files.createDirectories(parent);
    if (Files.exists(file)) {
      for (Record r : readAll()) {
        nextLsn = Math.max(nextLsn, r.lsn() + 1);
        nextTxId = Math.max(nextTxId, r.txId() + 1);
      }
    }
  }

  synchronized long nextLsn() { return nextLsn++; }
  synchronized long nextTransactionId() { return nextTxId++; }

  synchronized void append(Record r) throws IOException {
    if (r.lsn() <= 0 || r.txId() <= 0 || r.type() == null || r.payload() == null) throw new IllegalArgumentException("invalid WAL record");
    byte[] payload = r.payload().getBytes(StandardCharsets.UTF_8);
    if (payload.length > MAX_PAYLOAD) throw new IllegalArgumentException("WAL payload too large");
    int checksum = checksum(r.lsn(), r.txId(), r.type().ordinal(), payload);
    try (FileOutputStream fos = new FileOutputStream(file.toFile(), true);
         DataOutputStream out = new DataOutputStream(new BufferedOutputStream(fos))) {
      out.writeInt(MAGIC); out.writeLong(r.lsn()); out.writeLong(r.txId());
      out.writeByte(r.type().ordinal()); out.writeInt(payload.length); out.write(payload); out.writeInt(checksum);
      out.flush(); fos.getFD().sync();
    }
  }

  synchronized List<Record> readAll() throws IOException {
    if (!Files.exists(file) || Files.size(file) == 0) return List.of();
    List<Record> records = new ArrayList<>();
    long previousLsn = 0;
    try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
      while (true) {
        try {
          int magic = in.readInt();
          if (magic != MAGIC) throw new IOException("invalid WAL magic");
          long lsn = in.readLong();
          long tx = in.readLong();
          if (lsn <= previousLsn) throw new IOException("non-monotonic WAL LSN");
          if (tx <= 0) throw new IOException("invalid WAL transaction id");
          int typeOrdinal = in.readUnsignedByte();
          if (typeOrdinal >= Type.values().length) throw new IOException("invalid WAL record type");
          int len = in.readInt();
          if (len < 0 || len > MAX_PAYLOAD) throw new IOException("invalid WAL record length");
          byte[] bytes = in.readNBytes(len);
          if (bytes.length != len) break; // tolerate only a torn final payload
          int storedChecksum;
          try { storedChecksum = in.readInt(); } catch (EOFException eof) { break; } // tolerate torn final checksum
          int actual = checksum(lsn, tx, typeOrdinal, bytes);
          if (actual != storedChecksum) throw new IOException("WAL checksum mismatch at LSN " + lsn);
          if (records.size() >= MAX_RECORDS_PER_READ) throw new IOException("WAL record count exceeds safety limit");
          records.add(new Record(lsn, tx, Type.values()[typeOrdinal], new String(bytes, StandardCharsets.UTF_8)));
          previousLsn = lsn;
        } catch (EOFException eof) { break; }
      }
    }
    return records;
  }

  synchronized void compact(long checkpointLsn) throws IOException {
    List<Record> keep = new ArrayList<>();
    for (Record r : readAll()) if (r.lsn() > checkpointLsn) keep.add(r);
    Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
    try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(tmp)))) {
      for (Record r : keep) {
        byte[] payload = r.payload().getBytes(StandardCharsets.UTF_8);
        out.writeInt(MAGIC); out.writeLong(r.lsn()); out.writeLong(r.txId()); out.writeByte(r.type().ordinal()); out.writeInt(payload.length); out.write(payload); out.writeInt(checksum(r.lsn(), r.txId(), r.type().ordinal(), payload));
      }
      out.flush();
    }
    try { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
    catch (AtomicMoveNotSupportedException e) { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING); }
  }

  private static int checksum(long lsn, long txId, int type, byte[] payload) {
    CRC32 crc = new CRC32();
    for (int i=7;i>=0;i--) crc.update((int)(lsn >>> (i*8)) & 0xff);
    for (int i=7;i>=0;i--) crc.update((int)(txId >>> (i*8)) & 0xff);
    crc.update(type); crc.update(payload, 0, payload.length);
    return (int) crc.getValue();
  }
}
