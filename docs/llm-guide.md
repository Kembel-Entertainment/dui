# Guide for coding agents

Use **dui 0.2.0-SNAPSHOT**, Java 25 and Paper/Minecraft 26.2 with one matching generated resource pack. This is a strict XML template engine, not HTML/CSS/JavaScript. Start with [quickstart](quickstart.md), [components](components.md) and [extensions](extensions.md). For transport work include [rendering](rendering.md); for camera maps include [world-map](world-map.md).

## Ownership rules

Keep domain components, designs and rules in the consumer. Playing cards, wheels, reels, gift/confetti artwork, palettes, icon pixels, rarity/slot frames, chrome and motion presets are not library APIs. Compose them from Canvas primitives, typed component registrations, consumer skins, generic Motion and PackContribution shaders/assets. Do not add application-specific types, branches or numerical opcodes to dui.

Library features are layout/binding/schema validation, generic controls, glyph/parameter transport, native item/profile models, clipping, finite motion, sessions/callbacks/tasks/resources and camera-map projection/input. Native geometry, protocol signatures, nine-pixel rows and capacity limits are platform contracts; they are not arbitrary UI design defaults.

## Building a consumer

1. Define caller `ThemeTokens`, `WidgetSkinRegistry` and immutable owner-prefixed `ComponentRegistry` packages. Supply a `RenderEnvironment` to `ui.compile(name, source, registry, environment)`; the adapter binds its pack font metrics and glyph registry. Plain compile has no skins.
2. Put geometry/bindings in XML templates. Root width/height are required. No automatic palette/background is applied. Text colors are explicit or supplied by skins. Padding-x/y are independent; preserve whole nine-pixel rows for hits and flow. `$tokens` resolve from the supplied environment.
3. Pass read-only map data and resource keys in ViewModel. Bindings traverse maps, not Java bean methods. Compute comparisons/negation/lists in Java. Rendering is pure; fetch/capture/prepare resources outside projection.
4. For new visuals register a typed PropertySchema and component renderer/fragment. For control appearance supply skin painters/geometry/metrics; custom skin properties supplement builtin contracts. Skin painters do not authorize business actions.
5. Register ShaderSpec and trusted GLSL at pack-build time. Use logical namespaced IDs and typed value maps at runtime. The generator owns numeric addresses and decoders. Exact ordered schemas/hashes must match the deployed metadata. Return straight-alpha RGBA and handle live=false.
6. Add caller PNG glyphs/fonts/model render families/map images to contributions. Runtime thumbnails/QR pixels are RasterImage values and do not go into the pack. Use RgbaImage.raster or RasterImage.argb to preserve alpha; flatten only when an explicit opaque surface is desired. Alpha transport is quantized.
7. Use MenuController/MenuView with typed ActionRouter for stateful menus. Authorize actions and update/close the displayed revision. Durable transactions must not depend on cancellable UI jobs. Scope UI timers and late async completion to session/view lifetimes.

## API lifecycle

`Dui.create(plugin, descriptor, metadata)` owns pack readiness and presentation. `ui.open(player, template, viewModel, options, handler)` returns a DialogSession. `session.update(...)`, `onClose(...)`, `close()` and `ui.close()` run on Paper's main thread. ItemStacks are cloned; stable nested view data belongs to the caller. Accepted callbacks consume random capabilities scoped to player/session/revision. Re-render after an action that leaves the menu open.

HTTP(S) links keyed by hit ID take precedence and use vanilla confirmation. Native forms/exit buttons remain outside the custom canvas. The server does not receive every Escape/screen replacement notification, so isActive represents server-known lifecycle. Unnecessary delayed refreshes can reopen an escaped dialog; use explicit close actions and guarded tasks.

## Renderer invariants

Horizontal positioning uses `shift(x) + glyphAdvance + shift(-(x+glyphAdvance)) = 0`. Visible width is not advance; generated rectangles/glyphs usually advance width+1. A font's Unicode address only works with its matching generated provider. Nine-pixel bands split shifted text/images vertically. Invisible positive spans own hits and precede decorative negative-rewound geometry. Preserve these rules; do not invent CSS pixel positioning.

Native items are rendered by Minecraft, through generated model wrappers and shared placement/motion transport. No exhaustive item screenshot atlas is needed. Full-color font sprites are optional consumer-owned decoration, distinct from native models. Full-body skin/vanilla armor uses the generic player-model component; inventory/equipment rules belong in the application.

Canvas width 120..480, height 9..360 (multiple of 9). Eight shader calls by default, opt-in 32; each carrier has at most eight calls/576 bits; schema parameters up to 240 bits. Runtime rasters allow at most 16384 samples per canvas. Generic Motion bounds and quantization are documented in rendering.md. World-clock age wraps; finish/remove finite effects before wrap.

The backend has fixed phases and whole-object popup suppression, not arbitrary z-index, masks or cross-backend painter order. Native clipping applies after transforms and does not move click hits. GUI scale/window size and raw dialog drag/wheel streams are unavailable to a vanilla server; offer explicit Compact/Spacious and Motion off settings.

## Maps and verification

Camera maps use `WorldMapDefinition` build inputs and runtime `WorldMapFrame(..., WorldHud)`. MAP/SCREEN spaces, depth, opening transform and opacity/pulse values are caller-owned. Build HUDs with `WorldHudTemplate` and explicit anchored surfaces; there is no bundled legend. `Dui.openWorldMap` owns the private seat/display/input entities and cleanup; consumers own dwell/permissions/navigation.

Run library/consumer unit tests, deterministic pack generation and explicit muted real-client scenarios. Builds must not silently start Minecraft. Use published public APIs and fail clearly for unsupported features. Deploy plugin/ZIP/metadata together after review. No legacy preset/protocol compatibility is retained; follow migration-0.2.md.

See [dynamic composition](dynamic-composition.md) for measured/typed components, shared group geometry, RGBA/fonts, keyframes, runtime map layers, registered model cameras and trusted Paper backend extensions. Renderer/world-map protocol 4 packs must be rebuilt together with metadata.
