#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
VERSION="$(sed -n 's/.*<artifactId>forge-db<\/artifactId><version>\([^<]*\)<\/version>.*/\1/p' pom.xml)"
[ "$VERSION" = "1.0.0" ] || { echo "ERROR: expected 1.0.0, found $VERSION"; exit 1; }
rm -rf target/final-release
bash scripts/release-validate.sh
ART="target/release/forge-db-${VERSION}.jar"
sha256sum -c "${ART}.sha256"
test "$(unzip -p "$ART" META-INF/MANIFEST.MF | sed -n 's/^Implementation-Version: //p' | tr -d '\r')" = "$VERSION"
test -s "$ART"
mkdir -p target/final-release
cp "$ART" "${ART}.sha256" target/final-release/
cp FINAL_RELEASE.md target/final-release/
printf 'ForgeDB FINAL RELEASE GATE: PASS\nVersion: %s\nArtifact: %s\n' "$VERSION" "$ART"
