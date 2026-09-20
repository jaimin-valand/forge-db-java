# Reliability and Integrity

ForgeDB treats corruption differently from a torn final write. A truncated final WAL record can be ignored during recovery, but a completed record with an invalid magic value, record type, length, LSN sequence, or CRC32 checksum is rejected.

Page files must contain whole 4096-byte pages. Page payloads include a CRC32 checksum and page IDs are validated when read.

These checks are designed to fail closed rather than silently accepting damaged state.
