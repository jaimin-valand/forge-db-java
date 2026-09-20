# ForgeDB Storage Engine

ForgeDB uses fixed 4 KiB pages as its durability boundary. A page contains a small header with a magic value, page identifier, payload length and CRC32 checksum. The checksum lets the engine reject silently corrupted page contents instead of returning bad records.

## Slotted pages

Variable-length records are represented by a slot directory followed by record bytes. Deleted records retain their slot identity, which gives higher layers a stable record reference model even when records are variable length.

## Buffer pool

The buffer pool is an LRU cache. Reads update recency; dirty pages are written before eviction. Metrics expose cache hits, misses and evictions for later benchmark work.

## Design decision

The storage engine intentionally separates page I/O from record layout. That makes it possible to replace the record manager or index layer without rewriting file I/O. The next stage will add a B+ tree and a query planner that can choose indexed access over a table scan.
