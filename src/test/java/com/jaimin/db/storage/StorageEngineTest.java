package com.jaimin.db.storage;

import org.junit.jupiter.api.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class StorageEngineTest {
  @Test void pageRoundTripAndChecksum() throws Exception {
    Path p=Files.createTempFile("forgedb-pages","db");
    try(PageFile f=new PageFile(p)){ Page x=f.allocate(); x.writePayload("hello-forgedb".getBytes()); f.write(x); assertEquals("hello-forgedb",new String(f.read(x.pageId()).readPayload())); }
  }
  @Test void bufferPoolUsesLruAndFlushesDirtyPages() throws Exception {
    Path p=Files.createTempFile("forgedb-buffer","db");
    try(PageFile f=new PageFile(p); BufferPool bp=new BufferPool(f,2)){
      Page a=bp.newPage(); a.writePayload("A".getBytes()); Page b=bp.newPage(); b.writePayload("B".getBytes()); bp.get(a.pageId()); Page c=bp.newPage(); c.writePayload("C".getBytes()); bp.flushAll(); assertTrue(bp.evictions()>0); assertEquals("A",new String(f.read(a.pageId()).readPayload())); assertEquals("C",new String(f.read(c.pageId()).readPayload()));
    }
  }
  @Test void slottedPageStoresVariableLengthRecords() throws Exception {
    Page p=new Page(0); SlottedPage s=new SlottedPage(p); int a=s.insert("short".getBytes()); int b=s.insert("a much longer record".getBytes()); assertEquals("short",new String(s.read(a))); assertEquals("a much longer record",new String(s.read(b))); s.delete(a); assertThrows(IllegalArgumentException.class,()->s.read(a));
  }
}
