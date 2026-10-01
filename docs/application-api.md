# Application APIs

Use these contracts for a new menu. Negative spacing, model wrappers and wire fields are renderer concerns; application authors supply templates, state, supported motion parameters and resource snapshots.

## Dependencies and ownership

```groovy
implementation 'gg.kembel.dui:dui-paper:0.1.0-SNAPSHOT'
implementation 'gg.kembel.dui:dui-components:0.1.0-SNAPSHOT' // optional visuals and chrome
 testImplementation 'gg.kembel.dui:dui-test:0.1.0-SNAPSHOT'
```

`dui-core` has no Paper or HTTP dependency. `dui-paper` manages callbacks, main-thread presentation and lifecycle. `dui-components` is optional, builds on public core primitives and contains no game rules. `dui-pack` is build tooling; keep it out of the application runtime. `dui-test` contains portable test helpers, without JUnit or Paper in its runtime contract.

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

`present(MenuView, ActionHandler)` is the compatibility entry point for existing Canvas-based applications; its handler explicitly chooses whether to refresh. `onPresented(observer)` observes the completed presentation for diagnostics/application effects, keeping projection pure. `close()` ends the session. `tasks()` and `viewTasks()` delegate to the active session. Paper-facing operations run on the server main thread.

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

`CollectionView.of(entries, requestedPage, capacity, stableKey)` validates unique stable keys and returns a clamped `Page` plus keyed entries. Use IDs derived from those keys rather than list positions. `Carousel.window` supplies cyclic slots and entering slots for discrete arrow/card callbacks. `GridLayout.place` validates spans and overlap before drawing. `CardStrip.layout` computes card slots and reports insufficient capacity; callers deliberately page or allocate through `RenderBudget`.

Install reusable components with:

```java
var registry = ComponentRegistry.builder()
    .include(VisualComponents.registry())
    .template("journal-row", Set.of("label"),
        "<dui-fragment><dui-entry label=\"{{props.label}}\" height=\"18\"/></dui-fragment>")
    .build();
var template = ui.compile("journal.html", source, registry);
```

Registries are immutable snapshots. Merge collisions fail. `dui-chrome-header` accepts `title` and `detail`; `dui-chrome-pagination` accepts `label`, `previous`, `next`, `previous-locked`, `next-locked` and sends payloads -1/+1. Its fixed hit IDs `previous`/`next` mean one instance per canvas. Use your own named fragment for multiple independently paged collections.

`/dui acceptance compact` and `/dui acceptance spacious` demonstrate twelve journal entries, public chrome, pagination, a cached RGBA asset, token styling, typed actions and a controller without changes to dui.

## Token styles

`ThemeTokens` copies RGB colours, nonnegative spacing and typography aliases. Required colour keys: surface, raised, text, muted, accent, border, success, warning, danger, selected, disabled. Additional tokens need no enum edit. `DARK.with(overrides)` creates a palette; construct a pair with your own colours and pass it to `template.render(data, images, tokens)`.

Colour properties accept `$accent` or `#RRGGBB`. Gap/padding accept `$small`, `$medium`, `$large` or custom spacing names. Typography aliases select semantic text colours through `tokens.type(name)`; glyph geometry and font choice remain owned by the generated pack. This API does not promise arbitrary CSS font sizes or downloaded fonts.

Resolution order: widget semantic defaults → supplied theme → classes in declared order → class state properties → explicit node properties. State-prefixed properties use `disabled-`, `active-`, `selected-` for fill/border/color. Explicit `color` wins over a class's disabled colour. `UiTheme` and existing RGB defaults remain compatibility presets; token palettes provide extensible semantic defaults. Texture, raster and skin colours are not recoloured as UI tokens.

## Scene and backend boundaries

`Scene.of(canvas)` records immutable primitive snapshots, node/parent identity, local rectangles, parent origins, native clips and backend layers. `Canvas.renderPlan()` resolves popup coverage without deleting objects from the source scene. Dropdowns register coverage; intersecting earlier runtime images, portraits, native models and effects are suppressed as complete objects for that view. Dismissal reprojects the underlying scene.

The backend has fixed draw phases: background images, font paints/hits, foreground images/heads and native/effect bodies. It does not implement arbitrary cross-backend CSS z-index. Only native models support geometric viewport clipping. Popup coverage is whole-object occlusion, not arbitrary image/effect masking. `Scene.require(capability)` rejects unsupported contracts. Hide nodes with `dui-if`; omitted nodes are absent from the snapshot.

Native clip rectangles are fixed canvas coordinates and mask transformed pixels. Hits remain destination rectangles, with no transformed hit testing. Disable actions during transit. True pointer dragging, wheel streams and automatic GUI scale detection are not provided by vanilla dialogs.

## Motion and effect budgets

`Motion` is one finite GPU contract shared by native models and procedural effects: translation to the destination, scale, rotation, opacity, pivot, duration, delay and four easing curves. `slide` and `pop` compose tracks. `still()` selects the final pose. `AnimationTimeline` composes phase timing; it does not schedule frames or implement game decisions.

```xml
<dui-menu width="300" height="90" animation-start="{{worldTick}}" motion="{{motion}}">
 <dui-layer height="fill">
  <dui-item id="reward" x="126" y="18" width="36" height="36" size="36"
            translate-y="24" scale-from="0" rotate-from="-15"
            opacity-from="0" motion-duration="24" easing="back_out"/>
 </dui-layer>
</dui-menu>
```

Optional `dui-visual-playing-card`, `dui-visual-chip-stack`, `dui-visual-wheel`, `dui-visual-reel`, `dui-visual-lever`, `dui-visual-particles`, `dui-visual-lights` support the same track attributes. Use `canvas.motion(itemId, motion)` or `effectMotion(effectId, motion)` for direct composition. Native models can use separate `motion-start`; tracked effects share the canvas animation start. Runtime images and profile portraits do not support these GPU tracks.

Bounds: translation and rotation -256..255; scale 0..255/64; opacity/pivot 0..1; duration 1..127 ticks; delay 0..127. Rotation uses degrees. Motion-off renders final transforms, not zero transforms. Preserve the original world tick through rerenders; the shader clock wraps at 24,000 ticks, so complete transient events first. Existing card flip/flight, staggered chip, wheel/ball and reel presets remain compatible visual behaviours; they can be composed with tracks rather than requiring a new shader per layout.

Default budget remains eight effects. Explicit `effect-budget="16"` or `32` enables bounded batching with matching protocol-2 packs. EffectBatches preserves order and separates consecutive ordinary/tracked groups. Ordinary batches carry eight effects; tracked batches carry two, so larger budgets cost bodies and payloads. `RenderBudget` reserves required minimums and allocates remaining capacity in priority order. Report or page overflow. Do not transmit hidden card values just because their visible face is down.

`NativeReel.draw` accepts arbitrary registered resource keys and returns carrier IDs to populate with real ItemStacks. Its viewport and vertical movement use the shared native motion contract. The legacy `symbols="arcade"` procedural preset retains its fixed six-symbol design; custom native symbols use NativeReel or a contributed effect, not an edit to the core parser.

## Resources and transparent images

`ResourceProvider<K,T>` returns a `ResourceHandle<T>`. A snapshot is LOADING, READY or ERROR; `available()` returns a ready value or declared fallback. Resolve in a controller/service and subscribe through session tasks. Rendering only reads snapshots and performs no network/file work.

`CachedResourceProvider` shares retained in-flight futures by key and enforces entry/declared-weight limits before starting a loader. Eviction drops retention, not the original future; external services must separately bound simultaneous I/O, downloaded bytes, origins and redirects. Use versioned keys. `ImageKey` includes identity/version, dimensions, fit, sampling and actual background.

`RgbaImage.decode(bytes)` preserves source alpha. `flatten(width,height, COVER|CONTAIN, NEAREST|BILINEAR, backgroundRGB)` explicitly composites alpha and letterboxing onto your real surface. RGB glyphs cannot carry per-pixel alpha. The old `RasterImage.decode(bytes)` keeps its documented dark-background compatibility behaviour; prefer the RGBA path or explicit-background decode for new applications.

Use nearest-neighbour scaling, contrast and a quiet zone for QR codes. Immutable RasterImages and fitted images use bounded retention. ViewModel clones ItemStacks both on input and when returning item maps; model/profile/glint data stays native, never an exhaustive sprite atlas. Generic handles can also hold application-owned item/portrait data; the Paper adapter resolves native heads and does not download arbitrary template URLs.

## Pack contributions and test helpers

`PackContribution(owner, resources, effects)` supplies namespaced build-time assets and trusted GLSL effect functions. Codes 8..15 are available for extensions; IDs, codes, function names and asset paths must not collide. Functions receive local position/size, two bounded 15-bit parameters, time and motion flags, and return RGBA. Runtime templates cannot inject shader code. See DemoPackGenerator for `demo:pulse`, an actual GPU-tested consumer extension.

Pass contributions as the fourth argument to `PackGenerator.generate(clientJar, additions, output, contributions)`. Metadata declares extension IDs/codes and the generated codec hash. Compile consumer renderers through their registry, emitting `ShaderEffect.Extension`; missing capabilities fail before display. Adding textures/models/functions rebuilds the pack; new layouts and supported motion parameters do not.

Portable test APIs live in `gg.kembel.dui.testing`: `FakeScheduler` (`advance`, `drain`, `pending`), `RenderAssertions` (hits/actions/budgets), and `MenuHarness` (pure projection plus typed action sequence). Test stale completion, close, min/max layouts, stable keys and overflow without Paper. Real-client tests are still required for GPU motion, clipping, model wrappers, popup visibility and actual mouse coordinates. A CPU preview cannot prove a shader works.
