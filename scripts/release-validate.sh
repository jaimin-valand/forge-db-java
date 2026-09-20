#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
VERSION="$(sed -n 's/.*<artifactId>forge-db<\/artifactId><version>\([^<]*\)<\/version>.*/\1/p' pom.xml)"
[ -n "$VERSION" ] || { echo 'ERROR: version not found'; exit 1; }
[[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || { echo "ERROR: invalid SemVer: $VERSION"; exit 1; }
rm -rf target/release-validation
mkdir -p target/release-validation/classes
find src/main/java -name '*.java' | sort > target/release-validation/sources.txt
javac --release 21 -d target/release-validation/classes @target/release-validation/sources.txt
bash scripts/test-gate.sh
bash scripts/cli-smoke.sh
mkdir -p target/release
printf 'Manifest-Version: 1.0\nImplementation-Version: %s\nCreated-By: ForgeDB Release Engineering\n\n' "$VERSION" > target/release/MANIFEST.MF
jar --create --file "target/release/forge-db-${VERSION}.jar" --manifest target/release/MANIFEST.MF --main-class com.jaimin.db.ForgeDbCli -C target/release-validation/classes .
ART="target/release/forge-db-${VERSION}.jar"
test -s "$ART"
ACTUAL="$(unzip -p "$ART" META-INF/MANIFEST.MF | sed -n 's/^Implementation-Version: //p' | tr -d '\r')"
if [ -n "$ACTUAL" ] && [ "$ACTUAL" != "$VERSION" ]; then echo "ERROR: manifest version mismatch: $ACTUAL != $VERSION"; exit 1; fi
sha256sum "$ART" > "${ART}.sha256"
python3 scripts/release-manifest.py "$VERSION" "$ART"
echo "RELEASE VALIDATION PASS: ForgeDB $VERSION"
