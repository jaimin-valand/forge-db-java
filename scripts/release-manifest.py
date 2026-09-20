import hashlib, json, pathlib, sys
version, artifact = sys.argv[1], pathlib.Path(sys.argv[2])
root = pathlib.Path(__file__).resolve().parents[1]
files=[]
for p in sorted(root.rglob('*')):
    if p.is_file() and '.git' not in p.parts and 'target' not in p.parts:
        files.append(str(p.relative_to(root)))
h=hashlib.sha256(artifact.read_bytes()).hexdigest()
out={"project":"ForgeDB","version":version,"java":"21","artifact":{"path":str(artifact.resolve().relative_to(root)),"sha256":h,"bytes":artifact.stat().st_size},"source_file_count":len(files),"release_contract":"SemVer + Java 21 + release gate + CLI smoke test + artifact checksum"}
path=root/'target/release/release-manifest.json'; path.write_text(json.dumps(out,indent=2)+"\n")
print(path)
