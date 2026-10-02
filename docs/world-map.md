# In-game world maps

World maps are an optional backend of dui for Paper/Minecraft 26.2 and Java 25. They use the normal first-person camera, not a native dialog. Unmodified clients can look to pan, change hotbar slots to zoom, and click to select. A client mod is used only by the demo's automated tests.

## Ownership and public API

`gg.kembel.dui.core.world` holds platform-independent definitions, frames, input, projection and pack metadata. `dui-paper` owns the private display, seat, interaction target, HUD, pack readiness and lifecycle. `dui-pack` builds the optional assets and shader branches. Applications own artwork, permissions, dwell duration, detail screens and actions. There are no built-in warps, economy, target names or SOLO/PUBLIC rules in the library.

A `WorldMapDefinition` identifies a namespaced map and lists immutable layers. Each layer has a stable ID, an image key, its center and size in map pixels, and a coordinate space (`MAP` pixels or normalized `SCREEN` coordinates), explicit clip depth, opacity range and pulse rate. The consumer assigns visual depth. Regions use the same layer rectangles; hit testing follows that visual order. Only MAP-space layers can define clickable map regions.

A `WorldMapFrame` selects visible layer IDs, supplies regions, a view ID and a live, template-driven HUD. A region carries its own stable ID, layer, action/value and enabled flag. Disabled regions are still hoverable so applications can explain locks. Every primary action must be authorized by the application; the enabled flag does not grant permission.

Changing the frame's view ID cancels `viewTasks` and replaces the interaction entity. Changing its regions also replaces that entity. HUD-only updates keep the interaction target. Old entity IDs and duplicate same-tick clicks are rejected. `tasks` lives until session close; neither scope should contain durable business settlement work.

## Build a pack contribution

```java
var definition = new WorldMapDefinition("example:atlas", 600, 300,
    5, 100, 10, 3, 12, List.of(
        new WorldMapDefinition.Layer("island", "island",
            300, 150, 160, 96, WorldMapDefinition.Space.MAP, .2, 1, 1, 0)),
    new WorldMapDefinition.Opening(20, .8, 0, 1, Motion.Easing.EASE_OUT));
PackContribution atlas = WorldMapPack.contribution("example", definition,
    Map.of("island", myBufferedImage));
PackGenerator.generate(verifiedClientJar, ownAssetDirectory, output,
    List.of(atlas));
```

`myBufferedImage` is application-owned. Source images must be at most 254×254 pixels; a one-pixel border fits them into Minecraft's 256×256 font atlas. For a larger illustration use `WorldMapPack.tiles(image, tileWidth, tileHeight)`, then add the returned layers and images to your definition and contribution. The image dimensions need not equal the displayed layer size.

Use one combined `dui.zip` and its matching `dui.json`. Do not apply a second map pack over the dialog pack: both need the same core text shader. `PackContribution(owner, resources, shaders)` can be extended with map definitions, shared shader modules and glyphs. The generator rejects resource collisions, duplicate IDs, ownership mismatches and more than 64 maps or 128 layers per map.

Pack metadata carries `world-map-v1`, per-map fonts, image glyphs, exact advances, geometry hashes and a separate world-map protocol hash. Packs without maps have no map capability. Maps are compiled at pack-build time; changing an illustration or static geometry requires rebuilding and redeploying the combined pack. Visibility, enabled states, HUD text, progress, view selection and zoom change at runtime without a pack reload.

## Open and update

```java
var frame = new WorldMapFrame("overview", List.of("island"), List.of(
    new WorldMapFrame.Region("island", "island", "visit", "island", true)),
    WorldHud.EMPTY);
WorldMapSession session = dui.openWorldMap(player, "example:atlas", frame,
    WorldMapOptions.animated(), input -> {
      // input.type(): AIM, ZOOM, PRIMARY or SECONDARY
      // input.region() is null when aiming outside any active region.
      // Authorize gameplay in this callback; do not trust visual unlock state.
    });
session.onClose(() -> clearTransientApplicationState());
session.update(nextFrame);
```

The returned session can be pending pack acceptance. `isStarted()` indicates that the seat/display presentation has begun; `isActive()` also includes pending sessions. Input callbacks run on Paper's main thread. AIM and selection start after the opening phase; zoom responds to slot changes once the player is mounted. AIM is sampled on server ticks and also supplies a monotonic nanosecond timestamp for application-owned dwell logic. The shader independently pans on every rendered frame. `WorldMapOptions.still()` removes opening/pulse motion. Public Paper operations run on the main thread.

The player must be standing outside a vehicle, flight, sleep, swimming or gliding. An unsupported player state closes the returned session and shows an explanation; check `isActive()` before attaching task scopes or close callbacks if the application can open maps in those states.

Opening a dialog closes an active or pending map; opening a map closes the previous dui surface. Pack failure, quit, death, dismount, external teleport and plugin shutdown dispose the presentation. Normal close restores the saved position/rotation and hotbar slot. Death and external teleport do not snap the player back. Close is idempotent. The application hosts the pack and supplies its `PackDescriptor`, as with dialogs.

## Projection, fonts and shader transport

```text
relativeYaw = wrap(yaw - referenceYaw, -180 .. 180)
cursor = mapSize / 2 + (relativeYaw, pitch) * pixelsPerDegree
visibleHeight = visibleMin + zoom * visibleStep
scale = framebufferHeight / visibleHeight
screenPoint = framebufferCenter + (mapPoint - cursor) * scale
```

The reference yaw is captured when mounting. Projection uses physical framebuffer height and aspect ratio, so FOV and native GUI scale do not change map-space hit coordinates. Yaw is periodic; this is camera-controlled panning, not unbounded pointer dragging. The server cannot read raw wheel deltas: it interprets the shortest path around the nine-slot ring, `floorMod(next - previous + 4, 9) - 4`. Number keys are indistinguishable from slot changes, and sufficiently large wheel bursts cannot be reconstructed exactly.

One TextDisplay carries the whole visible scene. Every bitmap glyph is followed by `GlyphFont.shift(-advance)` in that map's generated font. The font includes the binary spacing providers; the negative advance cancels the pen movement so subsequent images retain the same camera-relative anchor. Advances come from stamped bitmap widths, never guessed character widths.

The world branch requires perspective projection, R=211, vertex length near one metre and valid texture-corner stamps. G stores zoom; B stores layer index and reduced motion; A carries the opening clock or completion. Texture stamps select the map and corner independently of batched vertex-buffer offsets. Geometry comes from the same definition serialized into metadata. Minecraft 26.2 uses reversed depth, so later layers have greater clip depth.

The GUI branch requires dedicated stamped glyph corners (R=212 for paints/text/icons and R=213 for native HUD covers), ASCII glyphs derived from verified vanilla assets and consumer-contributed glyph PNGs, exact negative advances and integer physical-pixel scaling. Full RGB remains available for application colors; zero-advance font pen metadata transports anchor, opacity and vertical sign. Shared row fonts encode the vertical offset. Templates choose surface transparency and screen anchors. Optional small opaque covers hide the native hotbar/health/experience and bossbar footprint; their color and enablement are template properties. Ordinary vanilla text, dui focus guards and player-skin rendering retain their existing paths. The pack integrates all branches rather than replacing another feature's shader. See `protocol/world-map.json` and run `python3 scripts/generate-world-map-protocol.py --check` after protocol changes.

## Client state and limits

The seat, text and click target are nonpersistent and visible only to their owner. A version-pinned outbound packet adapter masks that player's local hand and hotbar/offhand slots; the real inventory, visibility, potion effects, equipment and gamemode remain unchanged. Later inventory/flag synchronizations are also masked. Closing removes the adapter and synchronizes current server state rather than restoring a stale inventory snapshot. Inventory use, swaps, drops and clicks are blocked during the active map.

Rain is suppressed through a per-player weather override and the previous override is restored. World weather is unchanged. This is a first-person backend. Third-person cameras and shader-mod interoperability are not supported acceptance targets. Other plugins replacing the same core shaders need explicit pack integration. HUD text uses the native ASCII subset with unsupported characters replaced by `?`. Templates control all decorative layout and styling; the renderer applies bounded coordinate/image budgets. See the template section below.

The runnable example and independent screenshot checks are in dui-demo: `/dui map`, `/worldmap`, `/worldmap debug` and `/worldmap still`. The map is data-driven through its definition/frame; it is not an HTML browser or an additional dialog template type.

## Consumer-owned HUD templates

The HUD uses a `<dui-hud>` document with up to 32 `<dui-surface>` children. Each surface contains exactly one ordinary `<dui-menu>`, compiled by the same engine as dialog templates. Components, styles, bindings, repeats, conditional nodes, fragments, theme tokens and runtime RGB images work inside that menu. `dui-icon` is a decorative named pixel icon; `dui-image` accepts an application-provided runtime image, including a custom icon.

```xml
<dui-hud native-hud="cover" native-hud-color="#142C35">
  <dui-surface anchor-x="right" anchor-y="bottom"
               offset-x="-12" offset-y="-12" opacity="{{opacity}}">
    <dui-menu width="180" height="108" background="none">
      <dui-column padding="6" gap="0">
        <dui-text height="18" label="{{title}}" color="#FFD477"/>
        <dui-repeat items="entries" as="entry">
          <dui-text height="18" label="{{entry.label}}" color="{{entry.color}}"/>
        </dui-repeat>
      </dui-column>
    </dui-menu>
  </dui-surface>
</dui-hud>
```

```java
WorldHudTemplate hud = dui.compileWorldHud("my-plugin/atlas-hud.html", xml,
    ComponentRegistry.EMPTY);
WorldHud overlay = hud.render(viewData, runtimeImages, null);
var next = new WorldMapFrame(frame.viewId(), frame.visibleLayers(), frame.regions(),
    overlay);
session.update(next);
```

Compile once, render from application data, update the frame. A changed template can be compiled and applied to the existing session; validate it before replacing your previous compiled template. Changing layout, colors, opacity, labels, progress, icon selection or control count does not rebuild/reload the pack or respawn map entities. The demo's `/dui reload` demonstrates this, including rejection of invalid templates. Static map illustrations still require a pack build.

`anchor-x` is left/center/right and `anchor-y` is top/center/bottom. Offsets are signed logical pixels measured from the chosen screen anchor; right/bottom offsets usually use negative values. A surface's width/height follows the normal canvas contract (120..480, 9..360; height multiple of nine). Its origin and extent must fit x=-511..511 and y=-360..360 relative to its anchor. The renderer scales all logical pixels uniformly using `max(1, floor(min(framebufferWidth/480, framebufferHeight/360)))`, independent of FOV and GUI scale. Off-screen surfaces are naturally clipped by the viewport; keep your chosen layout small enough for the supported resolutions.

Opacity is 0..1, quantized to 1/255, and applies to the entire surface. Separate overlapping surfaces allow translucent backgrounds and fully opaque text, or independently styled groups. `visible` is a bound true/false value. Surface attributes and the two root native-HUD properties accept bindings. The menu has no implicit background; `background="none"` also explicitly disables a background supplied through consumer normalization, useful for transparent text surfaces.

Paints, text, icons and sampled runtime images are supported. There is no fixed four-control layout: applications can use rows, columns, grids or positioned layers, with bounded repeats. Input remains camera-based map-region selection. Native dialog actions, native items/heads/player models and dialog shader effects are rejected in HUD surfaces; they need their corresponding backend. Runtime ARGB images preserve alpha; opaque RGB/flattened images remain available when desired. Unsupported features fail rather than disappearing silently.

Limits are 32 surfaces, 2048 paints and 16384 image samples across a HUD. The optional map pack includes shared bitmap assets and row-font definitions for offsets 0..360. They are generated once by dui-pack; consumers never edit the generator or HUD GLSL for a new layout. HUD transport is world-map protocol version 4 and is hash checked against the generated pack. No default HUD/navigation template is bundled; applications provide the full WorldHud.

Equal-sized HUD glyphs and canvas paints share bitmap providers through generated atlases. Cell pixels, advances, baselines and shader corner stamps remain unchanged. Do not expand these into one bitmap provider per character per vertical offset: Vanilla allocates a Unicode lookup table for each provider, even when it contains only one character. A small compressed ZIP can therefore have a very large client heap requirement. The demo's protocol regression runs with a 2 GiB client limit and reloads the complete pack three times.


## Design-neutral layer settings

Each Layer carries id/image, center x/y, size, Space, depth, opacityMin/opacityMax and pulseRadiansPerTick. MAP geometry uses map pixels. SCREEN rectangles use normalized screen coordinates, independent of map zoom. Explicit depth is -1..1; opacity 0..1; pulse zero gives a constant opacity. Opening declares ticks, scaleFrom, opacityFrom/To and easing. These parameters enter generated map geometry; the library has no fixed header height, backdrop opacity, marker pulse or opening design. Changing them is a pack-build input change. Frame visibility and HUD values change at runtime.

Compile HUD templates with the same explicit environment/registry as dialogs when using own skins/glyphs. `WorldHud.EMPTY` emits no custom HUD. `native-hud=keep` is the default; cover requires an explicit color. Each surface chooses its own opacity and anchors. Text has explicit RGB; registered glyph PNGs belong to the consumer. Builtin world-map protocol/font scales and entity transforms remain technical transport constants.

## Runtime layer geometry

`WorldMapFrame` accepts optional `Map<String, WorldLayerState>` for declared MAP-space layer centers, sizes and opacity. Java hit testing and the shared shader use the same values. Consumer art still belongs in its pack contribution. See [dynamic composition](dynamic-composition.md#runtime-world-map-layers) for ranges, private entity transport, lifecycle and current opening/SCREEN-space limits.
