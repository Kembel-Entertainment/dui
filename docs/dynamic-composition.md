# Dynamic composition and extension contracts

DUI supplies geometry, transport and lifecycle. Consumers supply visuals, domain components, state and authorization. A new application may register components, fonts, model cameras, GLSL and Paper backends without modifying a DUI source file. Minecraft still imposes finite transport and rendering limits; an entirely new GPU feature is not created by a template property.

## Measurement and structured input

```java
var contract = new ComponentContract(new PropertySchema(Map.of()), Map.of(
    "rows", new ComponentContract.Value(
        ValueCodec.list(ValueCodec.string(), 8), true, null)));
var registry = ComponentRegistry.builder().renderer("acme-list", contract,
    context -> new Measure.Size(context.constraints().maxWidth(),
        context.node().value("rows", List.class).size() * 18),
    context -> { /* draw in the allocated bounds; add grid-aligned hits */ })
    .build();
```

`<dui-acme-list rows="{{rows}}"/>` preserves a bound list as a typed value. Only an exact binding preserves objects; interpolation in surrounding text remains a string. `ValueCodec` validates types and bounds. Its built-in object/list codecs copy recursively, limit depth/size and reject unknown object types. A custom codec is trusted Java and must return a stable immutable snapshot. No template Java, script or reflection expressions execute.

The measurer receives the resolved node, constraints, environment and child measurement callback. A custom container measures its children and arranges them through `context.children().draw(...)`. Use the same widths during measurement and arrangement. Scalar/default properties still use `PropertySchema`. `registry.describe()` exposes scalar and structured contracts for tooling; it describes schemas, not a consumer's Java implementation. Built-in layout cannot infer a component's content height without a measurer or explicit height.

## Groups and shared geometry

```html
<dui-group x="18" y="54" width="126" height="72"
  translate-x="{{offset}}" translate-y="0" opacity="{{opacity}}"
  clip-x="18" clip-y="54" clip-width="162" clip-height="72">
  <dui-acme-list rows="{{rows}}"/>
</dui-group>
```

The group lays out a column in its allocated rectangle. Clip coordinates are canvas coordinates after transformation. Rotation and scale pivot around the allocation origin. `Canvas.group(id, SceneGroup, drawer)` is the imperative equivalent. `Transform2D` supports affine composition/inversion. Nested group IDs prefix child hit/model IDs; item-resource keys must match the resulting IDs. Raster art follows draw order *inside the group*. The resulting raster, native objects and hits retain the fixed backend phases of the parent canvas.

| Backend | Group support |
| --- | --- |
| Rectangles, bitmap text, glyphs, RGBA images | Affine transform, opacity, rectangular output clipping |
| Native items | Translation, positive uniform scale, rotation, opacity, canvas-space clip; no reflection |
| Native item with a local clip | Axis-aligned positive transform; no rotated local clipping |
| Contributed procedural shader | Translation, positive axis scale, opacity; partial generic clipping and rotation require consumer GLSL |
| Native heads / full-body models | Translation only; partial clipping fails explicitly |
| Dialog hits | Positive axis-aligned transforms and rectangular clipping; Y and height stay multiples of nine |
| Custom backend primitive | Only the capabilities declared by its backend |

Unsupported composition fails before merging into the canvas. For off-grid decorative motion, omit/disable its hits until a valid layout frame. Nine-pixel hit rows are a vanilla text constraint, not a skin preference. Existing per-item GPU `Motion` must be sampled before placing that item inside a group; double animation transforms are rejected. Popup coverage belongs outside transformed groups. For arbitrary native/effect painter ordering or polygon clipping use a backend with a deliberate transport; adding a group does not provide those guarantees.

Opacity is visual state, not an input or authorization policy. Remove/disable hits and map regions explicitly when a fading or transparent element should stop accepting actions.

Group text/icons need the bitmap font/glyph pixels from matching pack metadata. `dui.compile(...)` injects them automatically. Direct `Canvas`/`MenuTemplate` users construct a `RenderEnvironment` with `metadata.bitmapFonts()`, `metadata.glyphPixels()` and `metadata.playerRenderers()`. The generated default bitmap font is measured from the verified client JAR. Consumer bitmap fonts are registered in `PackContribution.fonts`; rich text uses runtime rasters, not additional native font objects.

## Animation and alpha

`AnimationTrack` is a pure scalar timeline: increasing keyframes from tick zero, optional cubic Bezier easing, ONCE/LOOP/PING_PONG and `retarget(...)` from its current value. Sample one age for every visual and hit property in a frame. Consumer code owns presets and state transitions.

```java
session.animate(80, 4, age -> new DialogSession.Frame(
    project(track.sample(age, true)), resources));
```

`DialogSession.animate` publishes complete frames on the server thread. The first frame is age zero on the next tick, followed by the requested interval and an exact final frame. Duration zero explicitly requests an unbounded loop. An optional fourth argument observes a frame after display (for diagnostics); a normal update, another animation, replacement or close cancels the previous job. Unchanged logical actions keep their single-use callbacks across frames of that animation, so an input in flight is not lost to the next sample. Removed/changed actions invalidate their tokens; external updates and replacement still invalidate the entire prior action epoch. A consumed token and its siblings cannot be replayed. Always recheck application permissions and business state in the handler. `TaskScope.sample` provides the underlying named sampler, latest-job semantics and cancellation. These are UI jobs; persistent rewards/settlement must not depend on their completion. Sampling every four ticks is five network updates per second, not a 60 FPS shader animation. Existing native `Motion` and consumer GLSL remain per-frame client animations with no repeated animation packets.

`RasterImage.argb(...)`, `RgbaImage.raster(...)` and `RichText` retain transparency. The old `RasterImage(width,height,rgb)` constructor stays opaque. Paper dialog rasters quantize alpha to 15 nonzero levels; zero-alpha pixels only advance the pen. The shared generated alpha glyphs require `rgba-raster-v1`. Images/group rasters/fonts share the existing 16,384-sample canvas budget. Use bounded raster sizes, not a full-screen bitmap for every animation frame. Colors, fonts and text-span opacity are consumer data.

## Runtime world-map layers

```java
var frame = new WorldMapFrame("overview", visibleIds, regions, hud,
    Map.of("marker", new WorldLayerState(640, 352, 192, 96, .5)));
session.update(frame);
```

Map definitions/assets remain pack-build inputs. `WorldLayerState` changes a declared MAP-space layer's center, size and opacity at runtime. The common Java geometry uses these same bounds for selection. Visibility, zoom, reference direction and permissions remain session/application state. SCREEN-space geometry is currently build-time; template HUDs already support runtime dimensions/data.

Each visible overridden layer has one viewer-private TextDisplay. Its RGB carries two signed 12-bit coordinates; a float-exact 24-bit translation word carries size, zoom and reduced-motion state. A generated texture stamp selects the declared layer. The shared shader decodes this data; consumer code contains no entity, packet or projection implementation. Position range is -2048..2047, width/height 1..512, opacity 0..1. These are transport bounds, not fixed art dimensions. Static layers stay in the existing combined display. Runtime layers use the later see-through overlay pass so their alpha blends over static map artwork. This pass does not provide arbitrary depth interleaving with static layers or a contractual painter order between overlapping runtime entities; use non-overlapping runtime markers, or compose overlapping art into a single layer. Dynamic layers currently appear immediately; the pack's opening animation still applies to static layers. Updates are state transmissions; mouse panning remains entirely local/per-frame in the shader. Hide/remove/session close cleans up the extra entities.

## Consumer model cameras and platform backends

`PlayerRenderSpec` registers a namespaced full-body renderer with up to four viewport sizes and eight poses. Each pose defines camera yaw/pitch/world height, six limb pitches and idle angles/speed. Register through `PackContribution.playerRenderers`; runtime `renderer` and `facing` select family and pose. See [player-model.md](player-model.md) for transport/skin/armor constraints. No application camera profile is embedded in DUI.

For platform lowering beyond built-ins, register a namespaced `BodyBackend` on `Dui` before opening sessions. It declares required pack capabilities and geometry support and converts a `RenderPrimitive` plus `ViewModel` to Paper dialog bodies. `PackContribution.features` advertises matching consumer features. Unknown backends, missing capabilities, unsupported affine/clip/opacity and body-count overflow fail before display. Backends append after built-in body phases; their `bounds` describe consumer geometry, not an automatic Paper placement operation. The backend owns placement and resource validation. This is trusted server Java, not untrusted template code or arbitrary client shader injection.

The standalone `dui-demo/extension-proof` project exercises these contracts without importing demo code. `/dui extensions` shows it; `/dui extensions play` animates it; `/worldmap dynamic` exercises runtime map bounds. Its camera, font, components, shader and styles live entirely in the consumer. The muted `dynamic` client scenario checks rendering, moved input, cancellation, map clicks/zoom and cleanup.

New data, layouts, tracks and registered parameter values are runtime changes. New assets, fonts, camera families, shader schemas/code and pack features require rebuilding the consumer's combined pack and metadata. Minecraft changes or new transport capabilities still require library work. Keep plugin and pack ABI in sync: renderer protocol 4 and world-map protocol 4.
