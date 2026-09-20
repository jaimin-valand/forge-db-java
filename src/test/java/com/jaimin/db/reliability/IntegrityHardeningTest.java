package com.jaimin.db.reliability;

import com.jaimin.db.storage.Page;
import com.jaimin.db.storage.PageFile;
import java.nio.file.*;
import java.io.*;
import java.util.*;

/** Dependency-free negative tests for page and file integrity. */
public final class IntegrityHardeningTest {
  public static void main(String[] args) throws Exception {
    testPageChecksum();
    testPartialPageRejected();
    testMissingPageRejected();
    System.out.println("PASS: integrity hardening tests");
  }

  static void testPageChecksum() {
    Page p = new Page(0); p.writePayload("hello".getBytes());
    byte[] corrupt = p.bytes(); corrupt[24] ^= 0x55;
    boolean failed=false;
    try { new Page(0, corrupt).readPayload(); } catch (IllegalStateException e) { failed=true; }
    if(!failed) throw new AssertionError("corrupted page was accepted");
  }
  static void testPartialPageRejected() throws Exception {
    Path f=Files.createTempFile("forge-partial", ".pages");
    try { Files.write(f,new byte[17]); boolean failed=false; try(PageFile ignored=new PageFile(f)){}catch(IOException e){failed=true;} if(!failed)throw new AssertionError("partial page accepted"); }
    finally { Files.deleteIfExists(f); }
  }
  static void testMissingPageRejected() throws Exception {
    Path f=Files.createTempFile("forge-empty", ".pages");
    try { try(PageFile pf=new PageFile(f)){ boolean failed=false; try{pf.read(0);}catch(EOFException e){failed=true;} if(!failed)throw new AssertionError("missing page accepted"); } }
    finally { Files.deleteIfExists(f); }
  }
}
