# Compatibility and release contract

This is a source-reviewed WIP, not a stable Maven release. Current artifact version is `0.1.0-SNAPSHOT`; Java packages and Maven group are `gg.kembel.dui`.

| Layer | Supported contract |
| --- | --- |
| Java | 25, bytecode release 25 |
| Minecraft inputs/client | Verified pinned official 26.2 client JAR |
| Paper compile API | 26.2.build.129-stable |
| Paper runtime | Pinned Paper 26.2 build 129 with verified SHA-256 in demo scripts/demo.py |
| Renderer protocol | 2 with matching codec SHA-256 and declared capabilities |
| Legacy metadata | Missing/zero protocol interpreted as 1; original native/effects-8/clips paths only |
| Pack identity | Exact generated ZIP SHA-1; deploy matching dui.json |
| Client validation | Muted real Minecraft renderer on macOS ARM64; other OS/GPU combinations unverified |
| Publication | Local source/composite build or publishToMavenLocal; no remote Maven release yet |

Protocol 2 requires native, effects-8, clips, motion-tracks and effects-32 capabilities. Contributed effects add `effect:namespace:id@code`. Unknown protocol/capability, extension collisions, wrong codec hash, unsupported Minecraft/library version or mismatched ZIP identity fail validation. Legacy metadata cannot claim modern capabilities. Legacy POP/SLIDE fall back to the original preset transport; explicit generic tracks and larger budgets require protocol 2.

Update adapter, generator, ZIP and metadata together for this migration. Existing low-level Canvas, DialogSession, UiTheme and legacy component aliases remain available. Preferred optional component names are `dui-visual-*`; legacy aliases are frozen compatibility presets. No protocol negotiation with an unknown pack, automatic version conversion, shared-pack dependency resolution or core-shader merging with third-party packs is implemented.

## Contributor checks

1. Change `protocol/renderer.json` first for wire changes; run `python3 scripts/generate-protocol.py`. Never hand-edit generated RendererProtocol.java or protocol.glsl. Run with `--check` in validation.
2. Run `./gradlew build :dui-pack:componentDocs` and build sources/Javadocs. Regenerate component reference from ComponentSchemas when attributes change.
3. Run the demo's unit suites and explicit real-client scenarios against the same source build and matching pack. Assert motion-off stability, hidden information, invalid actions and transaction ledgers as well as screenshots.
4. Repeat pack generation from identical inputs and compare ZIP hashes. Capture render counts/component serialization size, adapter render time and client FPS before changing defaults. Component bytes are not full packet bytes; a capped FPS sample is not a performance ceiling.
5. Keep game inputs, generated packs, media, runtime files and reports outside commits. Present changes for review before commit/push under this workspace's AGENTS.md policy.

Source compatibility is checked by retaining existing templates/tests and consumer builds, not promised across future snapshots. A stable release needs independent platform validation, artifact publication and a frozen API policy.
