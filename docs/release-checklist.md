# Release Checklist

Before tagging a ForgeDB release:

- [ ] JDK 21 compilation passes
- [ ] Dependency-free release gate passes
- [ ] Storage integrity scenarios pass
- [ ] Transaction/recovery scenarios pass
- [ ] Security regression tests pass
- [ ] CLI smoke test passes
- [ ] Observability regression passes
- [ ] README reflects supported capabilities
- [ ] SQL reference reflects parser support
- [ ] Architecture documentation reflects actual implementation
- [ ] Version in `pom.xml` matches the release
- [ ] ZIP/source package contains docs and scripts

A release is not considered complete merely because the project compiles; the release gate and documentation must describe the same system.
