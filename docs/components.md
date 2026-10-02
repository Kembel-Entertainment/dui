# Template and component contracts

Current API: **0.2.0-SNAPSHOT / Paper 26.2 / Java 25**. The DSL is strict XML with a `dui-menu` root, not a browser. Accepted attributes and technical costs are generated in [the component contract](generated/component-contract.md) and [editor JSON](generated/components.json). Skin/extension properties supplement the corresponding component's contract; unknown attributes fail.

## Explicit environment and layout

`MenuTemplate.parse(source, environment, registry)` uses caller-supplied font metrics, tokens, skins and glyph bindings. `Dui.compile(name, source, registry, environment)` binds the actual pack's metrics and addresses. The plain compile overload has no skins. There are no implicit dimensions, background, palette or theme enum.

```xml
<dui-menu width="300" height="90" background="#132736">
  <dui-column padding-x="9" padding-y="9" gap="9">
    <dui-text label="Hello {{name}}" color="#E2EDF5" height="18"/>
    <dui-button id="run" action="run" label="Run" height="27"/>
  </dui-column>
</dui-menu>
```

The button needs a registered consumer skin. Text with an explicit color needs no skin. Data bindings traverse maps (`{{user.name}}`), not beans; calculate expressions in application code. Missing bindings fail. Conditions and repeats use supplied booleans/lists; stable IDs belong to the caller. Labels live in attributes, not element body text.

| Layout | Behavior |
| --- | --- |
| `menu`, `row` | Horizontal flow; explicit widths consume space first, `fill` shares the remainder |
| `column`, `panel` | Vertical flow; explicit/natural heights consume space first, `fill` shares full nine-pixel rows |
| `grid` | Equal-width cells with explicit spanning and bounded columns |
| `layer` | Positioned children; `fill`, percentages/insets, anchors and docking |
| `rect`, `surface` | Explicit fill; surface bevel defaults to zero; nonzero bevel requires a border |
| `spacer` | Empty layout space |

`padding` is the horizontal fallback; `padding-x` overrides it. `padding-y` is independent. Container skin metrics supply defaults; absent a skin both axes default to zero. Vertical flow insets/gaps must preserve the nine-pixel grid. Positioned paints may use sub-row offsets; hits and portraits still use whole rows. Layout overflow fails with source/component/id/bounds.

`dui-style` definitions are scoped direct children. Later classes override earlier classes; explicit properties win. `StyleResolver` applies selected/active/disabled variants. RGB colors and `$token` colors are accepted; spacing properties may reference caller spacing tokens. Skin painters decide which visual properties to consume. No legacy RGB palette is remapped by the library.

## Controls and skins

`button`, `tab`, `toggle`, `checkbox`, `choice`, `dropdown`, `badge` and `progress` delegate painting and natural dimensions to registered skins. `checked`, `active`, `value`, `open` and `locked` are application data. Actions do not mutate them automatically. `choice` gets `previous`/`next` geometry from its skin and emits -1/+1. The generic dropdown validates options, fits its popup, registers selection/dismissal hits and defers popup painting; its skin supplies field/popup/option art and option row height.

Hits use unique IDs, action routes, values and tooltips. `locked` controls preserve hover geometry and emit no callback. Applications must still authorize actions. `dui-hitbox` creates no paint and requires an ID, action and whole nine-pixel rows. Accepted dialog actions consume the revision's capabilities; update or close the session afterwards.

Custom properties/defaults are declared through `PropertySchema` on components or skins. Types: string, integer, decimal, boolean, enum, color and namespaced resource. Required missing properties, invalid ranges and unknown enum values fail before painting. See [extensions](extensions.md).

## Media and generic motion

- `dui-icon name="owner:key"` selects a contributed `GlyphSpec`; pictures and tint policy are caller-owned. No built-in icon/item-sprite list exists.
- `dui-head player="..." hat="true"` renders a native 8x8 portrait. A skin can add text/layout. No skin texture is baked into the pack.
- `dui-item id="..." size="36"` resolves `ViewModel.items[id]`, retaining the native ItemStack's components. Custom model definitions need pack contributions. Direct items create no hit; compose a hitbox or a consumer slot component.
- `dui-player-model` renders a full-body native profile with optional vanilla armor. See [player-model](player-model.md).
- `dui-image source="..." pixel-size="3"` resolves bounded runtime `RasterImage` pixels. It never downloads a URL or changes the resource pack. Use `RasterImage.decode(bytes, background)` for explicit alpha composition.

Native item motion uses explicit `motion-start`, `motion-duration`, `motion-delay`, `translate-x/y`, `scale-from/to`, `rotate-from/to`, `opacity-from/to`, `pivot-x/y`, and `easing`. Starts are world ticks, not milliseconds. The library has no pop/bounce/lift artwork presets. All four `clip-x/y/width/height` fields define a fixed canvas-space viewport. Off-canvas native origins require that explicit clip. Motion never moves click rectangles. Root `motion=false` selects final poses.

Root `animation-start` times procedural invocations. Effects are consumer components registered through `ShaderSpec`/`PackContribution`, not built-in tags. Capacity is eight by default, opt-in up to 32, with bit-cost batching. Back-facing card data is not a secrecy mechanism: consumers must omit unrevealed values.

`focus-outline` defaults to `native`. With `hidden`, provide `focus-outline-color` or an explicit solid background. A size-stamped technical glyph covers the native padded stroke using caller RGB; no library palette color is used. This is a cover, not a globally transparent removal of vanilla borders.

Background runtime images are painted before font rectangles and text. Set root `background="none"` when the image is the full menu surface; an opaque root fill would cover that image. Foreground rasters are painted after font paints and before heads/native/effect bodies. These are fixed backend phases, not arbitrary CSS z-index.

Native inputs and exit buttons live outside the custom canvas in `DialogOptions`. Vanilla dialogs do not expose raw pointer drag or client GUI-scale events; use explicit layout choices.

See [dynamic composition](dynamic-composition.md) for measured/typed components, shared group geometry, RGBA/fonts, keyframes, runtime map layers, registered model cameras and trusted Paper backend extensions. Renderer/world-map protocol 4 packs must be rebuilt together with metadata.
