# Contributing

dui is work in progress. Use Java 25, keep application-specific rules in dui-demo, and add a focused regression test when changing library behaviour. Run `./gradlew check` before proposing a change. Client integration scenarios are explicit commands in dui-demo and require a graphical display.

Templates and docs are English. Keep downloaded game assets, runtime state, generated packs and screenshots outside version control. Preserve dependency license notices when packaging runtime dependencies.

Public application/component APIs, backend boundaries and examples are in [application-api](docs/application-api.md). Check [compatibility](docs/compatibility.md) before changing a transport or pack format. Change protocol/renderer.json and regenerate Java/GLSL together; `python3 scripts/generate-protocol.py --check` rejects drift. Component schemas generate the editor JSON/reference through `./gradlew :dui-pack:componentDocs`. Keep generated documents deterministic and checked in.

Run `./gradlew build :dui-pack:componentDocs` (all five modules, sources and Javadocs) and the consumer tests. New visual capabilities require actual-client validation; a fake/CPU test cannot verify GPU rendering. Keep default budgets conservative and report body/payload/server/client measurements separately. Follow the workspace's review-before-commit policy.
