# Migrating to the design-neutral 0.2 API

Upgrade dui-core/dui-paper/dui-pack/dui-test together to `0.2.0-SNAPSHOT`, rebuild the combined resource pack and deploy its matching metadata. Old protocol 1/2/3 metadata is rejected. No compatibility preset artifact is published.

| Former library feature | New owner/API |
| --- | --- |
| UiTheme, built-in palette and RGB remapping | Consumer `ThemeTokens` and optional `RenderEnvironment.colorTransform` |
| Built-in controls' art and sizing | Consumer `WidgetSkinRegistry` with generic library control behavior |
| Cards, wheel, reels, lever, lights, confetti, skill-tree and equipment-slot art | Consumer namespaced components and `PackContribution` shaders/assets |
| Built-in icon pixels / ItemGlyphs sprite list | Consumer `GlyphSpec`/PNG contributions; tintable or full-color |
| `dui-components`, VisualComponents / shared chrome | Consumer `ComponentRegistry`, fragments and content outlets |
| Effect kind ordinal / two packed integer parameters | `ShaderSpec`, typed parameter map, `ShaderInvocation`, pack-assigned ABI |
| ItemTransition presets / Canvas.confetti | Explicit `Motion`; concrete presets and particle events belong to the consumer |
| NativeReel / CardStrip naming | `NativeCarousel` / `HorizontalStrip` generic geometry |
| Bundled map HUD/navigation helper | Consumer `WorldHudTemplate` and `WorldMapFrame(..., WorldHud)` |
| Map HEADER/BACKGROUND/PULSE enum | MAP/SCREEN coordinate spaces plus explicit depth/opacity/pulse/opening data |
| Dark alpha-decoding default | `RasterImage.decode(bytes, background)` |
| Fixed dark focus outline cover | `focus-outline-color`, or explicit menu background; default is native |

Compile templates with an explicit `RenderEnvironment` and immutable component registry. Set menu width/height and every required design value. Use independent `padding-x/y`; preserve nine-pixel row geometry. Text/icon primitives require explicit colors unless a consumer skin supplies them.

Register owned shader schemas and code at pack-build time. Runtime IDs resolve through metadata; never copy assigned numeric codes into application code. Parameter field order/type/range/step/enum order is an ABI change. The generator emits typed GLSL structs and decoding from the same specification used by Java. Consumer GLSL receives q/size/p/t/live.

Generic motion, native models, clipping, session/input lifecycle and runtime RGB/RGBA images remain library features. Concrete visual styles now reside entirely in the consumer. The migrated dui-demo shows these ownership decisions; its independent extension-proof exercises public APIs with a new 62-bit shader.

Existing template files can be updated without changing the library: install a skin, register a component or contribute a glyph/shader/model. Rebuild a pack only when those build inputs change, not for ordinary runtime data/layout edits.

Deploy migrated XML files along with plugin/pack/metadata. The demo intentionally preserves existing `plugins/dui-demo/ui/*.html` copies on ordinary startup; replace those copies with its updated `src/main/resources/ui` templates during this breaking upgrade. Do this with the server stopped after review. Normal runtime reloads then use those editable copies. Full-surface runtime images require `background="none"` to avoid an opaque root rectangle above them. The E2E runner temporarily installs current source fixtures and restores previous copies afterward.

The final extensibility pass adds optional metadata for bitmap fonts/glyph pixels and registered player renderer families. Rebuild all consumer packs for renderer protocol 4 and world-map protocol 4; do not deploy previous protocol-3 packs with the new runtime. Existing opaque RGB calls and legacy scalar component registrations remain source compatible. New APIs and limits are documented in [dynamic-composition.md](dynamic-composition.md).
