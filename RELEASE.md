# ForgeDB Release Guide

## Release contract

A ForgeDB release must have one canonical semantic version across `pom.xml`,
release metadata, CLI metadata, and the packaged JAR.

## Required gates

1. Compile with Java 21.
2. Run the dependency-free release gate.
3. Run the CLI smoke test.
4. Package the application JAR.
5. Verify the JAR exists and is non-empty.
6. Verify the JAR manifest contains the release version.
7. Generate SHA-256 checksums.
8. Generate a release manifest containing source inventory and artifact metadata.
9. Confirm the working tree contains no build output that was accidentally included in the source archive.

## Artifact set

- Source archive
- `forge-db-<version>.jar`
- `forge-db-<version>.jar.sha256`
- `release-manifest.json`

## Versioning

ForgeDB follows Semantic Versioning: `MAJOR.MINOR.PATCH`.

- MAJOR: incompatible public behavior changes.
- MINOR: backwards-compatible functionality.
- PATCH: backwards-compatible fixes.
