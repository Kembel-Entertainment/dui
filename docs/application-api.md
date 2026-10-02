# Application APIs

Use these contracts for a new menu. Negative spacing, model wrappers and wire fields are renderer concerns; application authors supply templates, state, supported motion parameters and resource snapshots.

## Dependencies and ownership

```groovy
implementation 'gg.kembel.dui:dui-paper:0.2.0-SNAPSHOT'
 testImplementation 'gg.kembel.dui:dui-test:0.2.0-SNAPSHOT'
```

`dui-core` has no Paper or HTTP dependency. `dui-paper` manages callbacks, main-thread presentation and lifecycle. `dui-pack` is build tooling; keep it out of the application runtime. `dui-test` contains portable test helpers, without JUnit or Paper in its runtime contract.

Permission checks, bets, balances, hidden cards, persistence, bot decisions, pack hosting and network policy remain application-owned. A disabled control is presentation, not authorization.

## Controllers and typed actions

```java
var actions = new ActionRouter<State>((state, action) -> state.message = "Invalid action");
actions.on("page", Integer::parseInt, (state, delta) -> state.page += delta);
var definition = new MenuDefinition<State>(
    "journal",
    state -> {
      var model = new ViewModel(data(state), images(state), Map.of(), Map.of());
      return MenuView.of(template, model,
          DialogOptions.notice("Journal", "Close", "close"));
    },
    (state, context) -> actions.dispatch(state, context.action(), context.value()),
    state -> state.closed = true);
var controller = new MenuController<>(ui, player, state, definition);
controller.refresh();
```

The controller stores application state and projects it without I/O. A custom action invokes the consumer and then refreshes an active controller, issuing fresh revision-bound callbacks. Decoder `IllegalArgumentException` invokes the rejection policy; exceptions from business handlers propagate. Native exit closes before its action and therefore does not refresh.

`present(MenuView, ActionHandler)` is the direct entry point for Canvas-based applications; its handler explicitly chooses whether to refresh. `onPresented(observer)` observes the completed presentation for diagnostics/application effects, keeping projection pure. `close()` ends the session. `tasks()` and `viewTasks()` delegate to the active session. Paper-facing operations run on the server main thread.

A consumer `MenuCatalogue<T>` stores immutable `Definition(id, aliases, templates, factory, validate)` entries. The generic factory field is consumer-owned; dui imposes no command framework. Resolve aliases with `find`, enumerate `templates`, and run `validate`. The demo registers each menu's preparation, dispatch, resources, validation and cleanup once, rather than maintaining separate lists.

## Three work lifetimes

| Lifetime | API | Ends when |
| --- | --- | --- |
| Current view | `controller.viewTasks()` | Every presentation update or server-known close |
| Menu session | `controller.tasks()` | Session replacement or server-known close |
| Application transaction | Consumer scheduler/service | Application's settlement/persistence policy |

```java
controller.viewTasks().later("finish-animation", remainingTicks, controller::refresh);
controller.tasks().latest("thumbnail", handle.completion(), (image, error) -> {
  state.image = error == null ? image : fallback;
  controller.refresh();
});
```

Named jobs replace previous work under the same key. Call scope APIs on their owner thread. Future completions dispatch through `UiScheduler`; stale results cannot update a replaced scope. Closing a UI scope never cancels a shared provider future. Scope cancellation is not a durable transaction mechanism: the slot demo's exactly-once settlement remains application-owned.

Vanilla does not notify Paper of every Escape or screen replacement by another plugin. An active session is server-known state, not proof that its dialog is visible. Avoid unnecessary late updates that could reopen an escaped view.

## Layout and collections

`LayoutProfile.COMPACT` and `WIDE` describe explicit GUI-unit preferences (320×180 and 480×360). `choose(boolean)` selects one; this is not client viewport detection. `requiredDialogHeight()` includes declared native bodies at eleven units each, before vanilla title/footer/input chrome. Set `nativeBodies` from the actual carrier plan, not only visible items; multiple effect batches need multiple bodies.

`LayoutContext` provides injected glyph measurements and validated min/max constraints. Templates support relative layer dimensions (`fill`, `fill-27`, percentages), anchors, docking, cross alignment, min/max dimensions and grid spans (`column-span`, `row-span`). Hit y/height remain multiples of nine; decorative coordinates need not be. Text wrapping/ellipsis use pack metrics. Unsupported constraints and overflow fail rather than silently omitting required content.

`CollectionView.of(entries, requestedPage, capacity, stableKey)` validates unique stable keys and returns a clamped `Page` plus keyed entries. Use IDs derived from those keys rather than list positions. `Carousel.window` supplies cyclic slots and entering slots for discrete arrow/card callbacks. `GridLayout.place` validates spans and overlap before drawing. `HorizontalStrip.layout` computes card slots and reports insufficient capacity; callers deliberately page or allocate through `RenderBudget`.

Install consumer components and presentation explicitly:

```java
var registry = ComponentRegistry.builder()
    .template("journal-row", Set.of("label"),
        "<dui-fragment><dui-text color=\"#FFFFFF\" label=\"{{props.label}}\" height=\"18\"/></dui-fragment>")
    .build();
var environment = RenderEnvironment.plain(metadata.font());
var template = ui.compile("journal.html", source, registry, environment);
```

Registries are immutable snapshots and merge collisions fail. Use `PropertySchema` for typed renderer/fragment defaults, ranges, enums, resources and colors. Content outlets compose consumer chrome without a library design package. See [extensions](extensions.md) for complete registrations, a custom control skin and shader/glyph contributions.

## Token styles and control skins

`ThemeTokens` copies caller-supplied RGB colors, nonnegative spacing and typography aliases. No keys or palette are required by core. Color properties accept `$accent` or `#RRGGBB`; spacing properties accept your named spacing tokens. Typography aliases select semantic text colors, not arbitrary CSS font sizes or downloaded fonts.

`RenderEnvironment` supplies font metrics, tokens, `WidgetSkinRegistry`, contributed glyph bindings and an optional color transform. Control behavior is library-owned; painting and named hit rectangles are supplied by the skin. Text can render with an explicit color without a skin. Buttons, checkboxes, toggles, choices, dropdowns and other styled controls require an installed caller skin. Skin properties have their own `PropertySchema` and can resolve color tokens. Do not hardcode application palette colors in a core shader.

Resolution order is caller skin defaults, declared classes, class state properties, then explicit node properties. Explicit color wins over a class state color. Runtime `template.render(data, images, tokens)` may override tokens. The optional color transform handles a consumer's legacy palette policy; textures and native model pixels keep their own color path.

## Scene and backend boundaries

`Scene.of(canvas)` records immutable primitive snapshots, node/parent identity, local rectangles, parent origins, native clips and backend layers. `Canvas.renderPlan()` resolves popup coverage without deleting objects from the source scene. Dropdowns register coverage; intersecting earlier runtime images, portraits, native models and effects are suppressed as complete objects for that view. Dismissal reprojects the underlying scene.

The backend has fixed draw phases: background images, font paints/hits, foreground images/heads and native/effect bodies. It does not implement arbitrary cross-backend CSS z-index. Only native models support geometric viewport clipping. Popup coverage is whole-object occlusion, not arbitrary image/effect masking. `Scene.require(capability)` rejects unsupported contracts. Hide nodes with `dui-if`; omitted nodes are absent from the snapshot.

Native clip rectangles are fixed canvas coordinates and mask transformed pixels. Hits remain destination rectangles, with no transformed hit testing. Disable actions during transit. True pointer dragging, wheel streams and automatic GUI scale detection are not provided by vanilla dialogs.

## Motion and effect budgets

`Motion` is one finite GPU contract shared by native models and procedural effects: translation to the destination, scale, rotation, opacity, pivot, duration, delay and four easing curves. `slide` composes a horizontal track; consumer factories may build any supported track combination. `still()` selects the final pose. `AnimationTimeline` composes phase timing; it does not schedule frames or implement game decisions.

```xml
<dui-menu width="300" height="90" animation-start="{{worldTick}}" motion="{{motion}}">
 <dui-layer height="fill">
  <dui-item id="reward" x="126" y="18" width="36" height="36" size="36"
            translate-y="24" scale-from="0" rotate-from="-15"
            opacity-from="0" motion-duration="24" easing="back_out"/>
 </dui-layer>
</dui-menu>
```

Consumer procedural renderers can parse the same attributes with `Motion.from` and attach the result to their invocation. Use `canvas.motion(itemId, motion)` or `effectMotion(effectId, motion)` for direct composition. Native models can use separate `motion-start`; tracked effects share the canvas animation start. Runtime images and profile portraits do not support these GPU tracks.

Bounds: translation and rotation -256..255; scale 0..255/64; opacity/pivot 0..1; duration 1..127 ticks; delay 0..127. Rotation uses degrees. Motion-off renders final transforms, not zero transforms. Preserve the original world tick through rerenders; the shader clock wraps at 24,000 ticks, so complete transient events first. Card flips/flights, wheel/ball motion, reel art and particles are consumer shaders and parameters, composed with the generic tracks.

Default budget is eight invocations; an explicit `effect-budget` up to 32 opts into additional batches. `EffectBatches` preserves draw order and separates consecutive ordinary/tracked groups. Each batch has a 576-bit payload and at most eight calls. Actual capacity depends on the declared schema bit width and the 99-bit motion cost per tracked call, not a fixed preset count. More batches consume dialog bodies. `RenderBudget` reserves minimums and allocates remaining capacity in priority order. Reject or page overflow. Never transmit a hidden card value merely because its visible face is down.

`NativeCarousel.draw` accepts arbitrary resource keys and returns carrier IDs to populate with real ItemStacks. Its viewport and movement use native clipping/Motion. Game-specific symbols and selection logic belong to the consumer.

## Resources and transparent images

`ResourceProvider<K,T>` returns a `ResourceHandle<T>`. A snapshot is LOADING, READY or ERROR; `available()` returns a ready value or declared fallback. Resolve in a controller/service and subscribe through session tasks. Rendering only reads snapshots and performs no network/file work.

`CachedResourceProvider` shares retained in-flight futures by key and enforces entry/declared-weight limits before starting a loader. Eviction drops retention, not the original future; external services must separately bound simultaneous I/O, downloaded bytes, origins and redirects. Use versioned keys. `ImageKey` includes identity/version, dimensions, fit, sampling and actual background.

`RgbaImage.decode(bytes)` preserves source alpha; `raster(width,height,fit,sampling)` supplies an alpha-preserving RasterImage. Paper quantizes its alpha to 15 nonzero levels. `flatten(width,height, COVER|CONTAIN, NEAREST|BILINEAR, backgroundRGB)` explicitly composites alpha and letterboxing onto your real surface when an opaque result is desired. `RasterImage.decode(bytes, backgroundRGB)` requires an explicit background; there is no implicit dark palette. The original RGB constructor remains opaque; `RasterImage.argb(...)` opts into per-pixel alpha.

Use nearest-neighbour scaling, contrast and a quiet zone for QR codes. Immutable RasterImages and fitted images use bounded retention. ViewModel clones ItemStacks both on input and when returning item maps; model/profile/glint data stays native, never an exhaustive sprite atlas. Generic handles can also hold application-owned item/portrait data; the Paper adapter resolves native heads and does not download arbitrary template URLs.

## Pack contributions and test helpers

`PackContribution` supplies namespaced resources, shader functions, shared GLSL modules, glyph PNGs, bitmap fonts, player render families, feature declarations and optional world-map definitions. GLSL is trusted build-time code; runtime templates cannot inject it. `ShaderSpec` declares names, kinds/ranges/quantization and up to 240 parameter bits. IDs are sorted to assign up to 64 codes; codes and schema fingerprints come from generated metadata, never a consumer-selected numerical opcode. Generated structs expose typed values to the function. See [extensions](extensions.md) and the independent `dui-demo/extension-proof` project.

Pass contributions as the fourth argument to `PackGenerator.generate(clientJar, additions, output, contributions)`. Runtime `ShaderInvocation` references the registered logical ID and exact specification. Pack metadata checks the renderer/world-map protocol 4 codecs, capabilities and per-shader schema fingerprints before display. Glyph bindings are assigned from namespaced `GlyphSpec` inputs. Adding GLSL, static glyphs, models or map art requires rebuilding the pack; supported parameter/layout/frame changes do not.

Portable test APIs live in `gg.kembel.dui.testing`: `FakeScheduler` (`advance`, `drain`, `pending`), `RenderAssertions` (hits/actions/budgets), and `MenuHarness` (pure projection plus typed action sequence). Test stale completion, close, min/max layouts, stable keys and overflow without Paper. Real-client tests are still required for GPU motion, clipping, model wrappers, popup visibility and actual mouse coordinates. A CPU preview cannot prove a shader works.
