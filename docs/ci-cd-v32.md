# CI/CD - ForgeDB 1.0.0

ForgeDB uses GitHub Actions as the continuous-integration entry point and keeps a dependency-free release gate for environments where Maven dependencies are unavailable.

## Pipeline

Every push to `main`/`master` and every pull request runs:

1. Checkout
2. Java 21 setup with Maven dependency caching
3. Main-source compilation
4. Maven/JUnit test suite
5. Dependency-free release gate
6. CLI smoke test
7. Maven package
8. Release JAR validation
9. Final release validation
10. Artifact upload

## Reproducibility

- Java distribution: Eclipse Temurin 21
- Maven compiler release: 21
- Build encoding: UTF-8
- Maven is run in batch mode with transfer logging reduced (`-ntp`).
- The dependency-free gate does not require JUnit or network access after source checkout.

## Local CI equivalent

```bash
bash scripts/ci-local.sh
```

The local pipeline must finish with:

```text
CI LOCAL PASS: ForgeDB 1.0.0
```

## Artifact contract

The CI job validates `target/forge-db-1.0.0.jar` exists, is non-empty, and contains the core `Database` class before publishing it as a workflow artifact.

## Failure policy

A failed Maven test, release gate, CLI smoke test, package step, or artifact validation fails the workflow. No release artifact is considered validated unless all preceding gates pass.
