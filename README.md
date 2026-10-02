# dui

**Work in progress.** A design-neutral, template-driven UI library for Paper 26.2 / Java 25 and unmodified Minecraft clients. Built by [Kembel Entertainment](https://kembel.gg); packages and Maven group: `gg.kembel.dui`.

Applications own their designs, components, assets and rules. The library provides layout, rendering, generic control behavior, native models, animation transport, sessions and input. A playing card, wheel, equipment slot or gift animation is an application component. None is bundled in the library.

| Module | Responsibility |
| --- | --- |
| `dui-core` | XML templates, typed properties, layout, skins, glyph/shader contracts, motion, resources, world-map geometry |
| `dui-paper` | Pack readiness, Adventure rendering, dialogs, native items/skins, callbacks, camera-map input and lifecycle |
| `dui-pack` | Verified vanilla input, font bands, native-model wrappers, consumer asset/shader compilation, deterministic pack metadata |
| `dui-test` | Render assertions, fake scheduler and portable consumer test harness |

The separate [dui-demo](https://github.com/Kembel-Entertainment/dui-demo) owns all showcase and game implementations. Its independent `extension-proof` module demonstrates a new component, control skin, glyph and typed shader without importing demo internals or changing dui.

## Build and embed

```sh
./gradlew build :dui-pack:componentDocs
./gradlew publishToMavenLocal
```

Java 25 is required. Unit builds do not start Minecraft. No public Maven release exists yet; embed `gg.kembel.dui:dui-paper:0.2.0-SNAPSHOT` and its runtime dependencies in your own plugin. Paper/Adventure remain server-provided. Use `dui-pack` only at build time.

Start with [quickstart](docs/quickstart.md), then [extensions](docs/extensions.md), [application APIs](docs/application-api.md) and [components](docs/components.md). Supply [the LLM guide](docs/llm-guide.md) and [llms.txt](llms.txt) to a coding agent. Existing consumers must follow [the 0.2 migration](docs/migration-0.2.md).

## Pack contract

```sh
./gradlew :dui-pack:generatePack -PminecraftJar=/path/to/verified/minecraft-26.2-client.jar
```

The bare generator builds technical rendering assets, with no design preset. To add your glyphs, shader components, models or map imagery, call `PackGenerator.generate(clientJar, ownAssets, output, contributions)`. Deploy the generated ZIP and matching `dui.json` together and host the ZIP yourself. Runtime image pixels and ordinary template changes do not need a pack rebuild.

Renderer and world-map protocols are version 4. Parameter schemas, addresses, geometry and hashes are compiled into the pack and validated by the runtime. Definitions have deterministic addresses; application code uses logical IDs. A schema mismatch fails explicitly.

## Supported boundaries

- Canvas: 120–480 GUI pixels wide, 9–360 high in nine-pixel rows. Hits use the same row grid.
- Caller-owned controls use `WidgetSkinRegistry`; there are no built-in palettes, icon pictures or preset animation styles.
- Generic native-model and procedural motion, clipping and whole-object popup coverage are available. The native full-body skin component remains a generic rendering primitive.
- Eight shader invocations by default, explicit budgets up to 32, packed by actual bit cost. A shader schema may use up to 240 parameter bits.
- Camera maps have caller-owned geometry, opening transforms, depths, pulse rates and HUD templates. No default map legend is bundled.
- The vanilla server cannot read GUI scale/window size or raw dialog drag events. Offer explicit layout preferences.
- Core-shader merging with third-party packs and arbitrary cross-backend painter ordering are not supported.

Technical geometry, signatures, units and capacity limits remain fixed to the supported Minecraft version. These are renderer contracts, not application design. Details and validation limits are in [rendering](docs/rendering.md), [world maps](docs/world-map.md), and [compatibility](docs/compatibility.md). Code is [MIT](LICENSE); see [third-party notices](THIRD_PARTY_NOTICES.md) for vanilla-derived build inputs.

See [dynamic composition](docs/dynamic-composition.md) for measured/typed components, shared group geometry, RGBA/fonts, keyframes, runtime map layers, registered model cameras and trusted Paper backend extensions. Renderer/world-map protocol 4 packs must be rebuilt together with metadata.
