package com.jaimin.db.index;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BPlusTreeSmokeTest {
  @Test void splitAndLookup() {
    BPlusTree<Integer,String> t = new BPlusTree<>();
    for(int i=0;i<100;i++) t.put(i, "v"+i);
    assertEquals(List.of("v42"), t.get(42));
    assertTrue(t.contains(99));
    assertEquals(100, t.size());
  }
}
