# Changelog

## 1.0.0 - Final Release

- Froze the ForgeDB architecture after the complete development roadmap.
- Added final release-gate automation and artifact verification.
- Validated architecture, correctness, storage, query, transaction, recovery, security, CLI and integration paths.
- Verified 2,000 deterministic malformed SQL fuzz cases.
- Published the first stable portfolio release artifact with SHA-256 checksum.

## 1.0.0 - Final README & Portfolio Presentation

- Reworked README around architecture, engineering evidence and project scope.
- Added portfolio and interview guide.
- Added standalone architecture diagram documentation.
- Added feature/evidence matrix and explicit project limitations.
- Unified final release metadata at 1.0.0.

# Historical release-engineering notes

The final 1.0.0 release consolidates the earlier development milestones.

## Release Engineering

### Added
- Semantic-version release metadata and manifest generation.
- Deterministic release validation script.
- SHA-256 checksums for release artifacts.
- Source-tree inventory and version consistency checks.
- Release notes and operator checklist.
- Clean-build verification without relying on Maven being installed.

### Validation
- Java 21 main-source compilation.
- Dependency-free release gate.
- CLI smoke test.
- JAR integrity and manifest validation.
