# Release Engineering - 1.0.0

ForgeDB now has a repeatable release contract instead of treating packaging as a manual final step.

## Controls

- Canonical SemVer from `pom.xml`.
- Java 21 clean compilation.
- Dependency-free release gate.
- CLI smoke test.
- Packaged JAR integrity validation.
- SHA-256 artifact checksum.
- Machine-readable release manifest.
- Release checklist and artifact contract.

## Reproducibility

`release-validate.sh` rebuilds the main source tree from scratch into an isolated validation directory and produces the release artifact from those classes. It does not depend on a pre-existing `target/classes` directory.

## Limitation

The local environment used for this release engineering pass does not provide Maven, so Maven/JUnit execution is represented by the existing CI workflow and the dependency-free release gate remains the local correctness gate.
