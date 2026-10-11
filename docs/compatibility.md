# Compatibility and release contract

This is a source-reviewed WIP. Version **0.3.0-SNAPSHOT** requires rebuilt 26.3 packs and introduces a shared runtime with Paper and Minestom adapters; see [migration](migration-0.3.md). Existing Paper entry points remain available. There are no protocol conversion paths.

| Layer | Contract |
| --- | --- |
| Java | 25 / release 25 |
| Minecraft | Pinned official 26.3 client, resource-pack format 97.1 |
| Paper | API/runtime 26.3.build.170-beta |
| Minestom | Immutable official 26.3 snapshot 26_3-20261007.224755-10; published 26.2 release is incompatible |
| Shared API | dui-runtime; platform-native values enter through adapter snapshots |
| Renderer | Protocol 4, generated codec SHA-256, matching capability set |
| World maps | Protocol 4, exact geometry/protocol hashes |
| Shader components | Logical namespaced ID, exact ordered schema, SHA-256 fingerprint and pack-assigned address |
| Glyphs | Namespaced ID, dimensions/tint policy and matching pack address |
| Pack identity | Exact ZIP SHA-1; matching dui.json deployed with the plugin |
| Distribution | Composite source build or publishToMavenLocal; no public Maven release yet |

Required renderer capabilities: `native`, `shader-components-v1`, `clips`, `motion-tracks`, `effects-32`. Generated alpha rasters declare `rgba-raster-v1`; full-body models use `player-model-v2`. Optional maps and consumer backends declare separate capabilities. Metadata rejects unsupported versions, wrong codec/schema hashes, duplicate addresses/IDs and mismatched map data. The adapter also validates each emitted shader/glyph/model family against the selected pack.

Technical transport constants are pinned to Minecraft, including nine-pixel font bands, native body offsets, clock wrapping, model frame signatures and capacity limits. Consumers configure design values and animation endpoints through public registrations/data; changing a transport constant is a backend migration.

For wire changes edit protocol JSON, regenerate Java/GLSL and use generator `--check`. Run library and consumer tests, build Javadocs/component docs, compare deterministic pack outputs and run real-client scenarios against the same artifacts. Coordinate plugin, ZIP and metadata deployment together. Template-only reloads do not rebuild the pack.

Client validation uses the actual Minecraft renderer on macOS ARM64. Other GPU/platform combinations, shader-mod interoperability and coordinated packs across independently versioned plugins need separate validation. Unit tests do not establish native draw ordering or GPU correctness. Capped FPS samples and serialized component size are diagnostics, not production throughput guarantees.
