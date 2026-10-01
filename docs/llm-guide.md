> Start with [application APIs](application-api.md) for the implemented abstraction layer and [compatibility](compatibility.md) for current supported versions. This guide also describes retained low-level APIs and legacy visual aliases.

# dui guide for coding agents

This file is a compact implementation contract for **dui 0.1.0-SNAPSHOT**, a Work in Progress library by Kembel Entertainment (https://kembel.gg). You can supply it directly to an LLM when asking for a Paper plugin using dui. APIs and pack formats are provisional. Follow the checked-out source if a future version differs.

## Runtime and project boundaries

- Java 25, Paper API `26.2.build.129-stable`, unmodified Minecraft 26.2 clients.
- Maven group / Java package prefix: `gg.kembel.dui`.
- Embed `gg.kembel.dui:dui-paper:0.1.0-SNAPSHOT` in the application plugin. It brings dui-core and Gson. Paper and Adventure are provided by the server.
- The WIP artifacts currently resolve through `mavenLocal()` after `./gradlew publishToMavenLocal` in dui. No remote Maven release exists yet.
- Optional `dui-components` supplies `VisualComponents.registry()` and native reels; `dui-test` supplies helpers in `gg.kembel.dui.testing`.
- `dui-core` owns the platform-independent template/layout model. `dui-paper` owns rendering, sessions and callback transport. `dui-pack` generates build-time fonts, model wrappers and shared shaders.
- Application commands, permissions, balances, rewards, network services, files and resource-pack hosting belong to the consumer. Examples and real-client tests are in the separate dui-demo repository.

Use [quickstart.md](quickstart.md) for a complete plugin, Gradle configuration, plugin.yml, config and template. [components.md](components.md) lists supported tags. [architecture.md](architecture.md) explains the rendering transport. Public adapter source is under `dui-paper/src/main/java/gg/kembel/dui/paper/`.

For application work, include [recipes.md](recipes.md): stateful checkbox/dropdown handlers, repeated rows, item/image/link keys, guarded async image updates, native forms, deployment and troubleshooting. The component reference lists actual defaults/bounds and attributes consumed by each tag; ComponentSchemas rejects unknown per-component properties; generated metadata is in docs/generated/components.json. Shared placement/style attributes remain accepted on structural nodes and may be irrelevant there.

For renderer or pack work, also supply [rendering.md](rendering.md). It documents the actual negative-spacing/font protocol, glyph advances, 9-pixel band splitting, hit-first ordering and native-item shader payload. Maintain `shift(x) + glyph advance A + shift(-(x+A)) = 0` for each placement; a bitmap's visible width is not its advance. Invisible positive spans own the clicks; decorations draw after them. These transport details are handled by dui when using templates.

## Public API

```java
PackMetadata metadata = PackMetadata.read(dataDirectory.resolve("pack/dui.json"));
PackDescriptor descriptor = PackDescriptor.of(URI.create(packUrl), metadata);
Dui ui = Dui.create(plugin, descriptor, metadata);
MenuTemplate template = ui.compile(templateSource);
DialogOptions options = DialogOptions.notice("Example", "Close", "close");
DialogSession session = ui.open(player, template, ViewModel.data(data), options, handler);
session.update(template, nextModel, options, handler);
session.onClose(cleanup);
session.close();
// From the plugin's onDisable():
ui.close();
```

The opening and updating examples above are separate operations in the application's lifecycle; do not immediately close a newly opened menu just because the API list includes `close()`.

| Type / member | Contract |
| --- | --- |
| `Dui.create(JavaPlugin, PackDescriptor, PackMetadata)` | Create one adapter per application plugin. Register no standalone dui plugin dependency. |
| `ui.compile(String)` | Compile XML using the generated pack's font metrics. Throws on invalid templates. |
| `ui.compile(name, source, registry)` | Compile with a consumer component registry and named diagnostics. |
| `ui.open(Player, MenuTemplate, ViewModel, ActionHandler)` | Open with the default notice/close options. |
| `ui.open(Player, MenuTemplate, ViewModel, DialogOptions, ActionHandler)` | Open or replace this adapter's session for a player. |
| `ui.open(Player, Canvas, ViewModel, DialogOptions, ActionHandler)` | Optional lower-level canvas overload. Prefer templates for application layout. |
| `session.update(...)` | Replace template/canvas, model, options and handler on an active session. Advance its revision and close the previous view task scope. |
| `session.viewTasks()` / `tasks()` | Named UI jobs with view/session lifetime. `later` replaces a delayed job; `latest` dispatches the latest async result on the server thread. See [composition](composition.md#session-tasks). |
| `session.isActive()` / `revision()` | Server-side lifecycle state and version; useful for guarding asynchronous results. |
| `session.canvas()` / `component()` | Inspect the rendered layout / Adventure component. The component may be null until the pack has loaded. |
| `session.onClose(Runnable)` | Set cleanup for server-known close, replacement, rejection, quit or adapter disposal. |
| `ui.offerPack(Player)` / `packLoaded(Player)` | Offer or inspect this adapter's matching pack. Opening a menu offers it automatically. |
| `ActionContext` | `player()`, `session()`, `hit()`, `response()`, and shortcuts `id()`, `action()`, `value()`. |

Paper-facing creation, open/update/close operations and callback handlers run on the main server thread. Do HTTP, image decoding and file work on a worker; return to Paper's scheduler before changing a session. Use view/session task scopes for UI work; when scheduling directly, guard player online state, `isActive()` and the captured `revision()`. Keep application state scoped to the player.

Accepted custom actions consume the current revision's callback capabilities. If the menu stays open, update/re-render it to issue fresh callbacks, even after an action that leaves the visual state unchanged. URL clicks use native link confirmation and do not invoke the server handler. A native exit action closes the session before invoking its handler: do not update that closed session.

Vanilla does not send this adapter a general notification when the user presses Escape or another plugin replaces the screen. `isActive()` therefore does not guarantee the window is currently visible, and `onClose` cannot promise an Escape notification. A delayed update can reopen an escaped screen. Use explicit close actions and avoid unnecessary late updates when this matters.

## View data and template language

Supply four maps: `Map<String, Object>` for binding data, `Map<String, RasterImage>` for images, `Map<String, ItemStack>` for native items, and `Map<String, URI>` for links:

```java
ViewModel model = new ViewModel(data, images, items, links);
```

`ViewModel.data(map)` is the convenience constructor for ordinary bindings. Top-level maps are copied; ItemStacks are cloned on input and on accessor output. Supply stable nested data yourself.

Templates are well-formed XML using `dui-` tags, even though application files conventionally end in `.html`. They do not support CSS, JavaScript, browser tags, DOM events or arbitrary expressions. Use `label="..."` for text, explicit closing/self-closing tags, and XML escaping for literal `&` and `<`.

```xml
<dui-menu width="300" height="90" padding="9" theme="studio">
  <dui-column gap="9">
    <dui-text label="Hello {{name}}" height="18" />
    <dui-checkbox id="enabled" label="Enabled" checked="{{enabled}}"
                  action="toggle_enabled" height="27" />
  </dui-column>
</dui-menu>
```

Bindings such as `{{user.name}}` traverse nested maps, not Java beans or record accessors. Missing values fail rendering. Lists for `dui-repeat items="rows" as="row"` contain maps; use unique IDs such as `id="row_{{row.id}}"`. Conditions use supplied values, for example `dui-if test="{{visible}}"`; calculate negation/comparisons in Java rather than inventing an expression syntax. No implicit two-way binding exists: toggles, checkboxes and dropdowns send actions, and the application changes state and updates the view.

Use `row`, `column` and `grid` for flow, and `layer` for positioned children. Fixed dimensions, `fill`, `gap` and padding are supported where applicable. Styles are direct `dui-style` children of the root and apply through `class`; explicit attributes override them. Built-in themes are `default`, `studio`, `studio_dark`. Colours are `#RRGGBB` or semantic `$token` values; gap/padding also accept spacing tokens. Use ThemeTokens in the three-argument template render overload.

## Media, controls and effects

- `id` identifies a hit/placement; `action` is the handler route; `value` is application payload. Do not encode business authorization in the label. Controls that implement `locked="true"` keep a tooltip but emit no action. Slots and tree nodes do not consume `locked`; their action must be empty to make them inert. Tree status is visual, and all business authorization belongs in the handler.
- Dropdowns have `open`, `action`, `select`, `dismiss` and `dui-option` children. The application controls opening, selected value and closing. See dui-demo's showcase templates and `ShowcaseState`.
- `dui-item id="example" ... size="36"` and `dui-slot id="example" ...` require `items.get("example")`. Native ItemStacks retain model/profile/pattern/glint components. Vanilla items already have wrappers in the generated pack; custom item definitions require pack additions.
- `dui-head` uses native portrait object components. For a large 3D head, supply a player-head ItemStack through `dui-item` or `dui-slot` instead.
- `dui-image source="{{imageKey}}" ... pixel-size="2"` resolves a raster-map key. It does not fetch a URL. The application downloads/decodes images (`RasterImage.decode(byte[])`) asynchronously. Thumbnails and QR matrices become runtime RGB glyphs; no pack update is needed. Put labels outside image bounds because image paints cover normal text paints.
- HTTP(S) `links` are keyed by the hit ID. The component must also have a nonempty `action` for the hit to be clickable. The link takes precedence over a custom server callback and opens vanilla's URL confirmation.
- Reels, levers, lights and particles use shared shader components. Geometry and parameters live in templates/view data. Set root `animation-start` from `player.getWorld().getGameTime()`, not milliseconds; `motion="false"` disables motion. Use a finite duration and app-owned settlement/cancellation rules. Effects do not require a different shader per menu.
- Native forms use Paper `DialogInput` objects through `DialogOptions`, outside the custom canvas. Construct `DialogOptions(title, inputs, buttons, exit, columns, confirmation)`. Confirmation requires exactly two buttons; a notice with no buttons needs an exit action. Validate values through `context.response()` in the consumer.

## Pack contract and limits

Generate a pack from the verified 26.2 client JAR and deploy its matching `dui.json`. Host the ZIP yourself and preserve the `dui:` namespace. `PackDescriptor.of` derives the pack ID from its hash and sets `required=false`; create a descriptor explicitly if your application needs a different policy. The UI still waits for successful pack loading. Rebuilding your application HTML does not rebuild the pack; adding font/model/texture resources does.

`PackGenerator.generate(clientJar, additionsDirectory, outputDirectory)` accepts application assets under `assets/<your_namespace>/...`. Item definitions under `items/` receive native wrappers and enter the metadata registry. Keep own assets outside `dui:`. Minecraft inputs/generated game-derived assets retain their own terms and stay outside source control.

Bounds: canvas width 120–480 GUI pixels, height 9–360 and divisible by 9; clicks occupy full 9-pixel rows. Eight shader effects by default, opt-in up to 32 with a matching protocol-2 pack, and 16,384 sampled image pixels per canvas; image cells are 1–8 GUI pixels. Templates are bounded to 128,000 characters and nesting depth 20; expanded components/repeats are bounded too. Unsupported tags/attributes, missing bindings, overflow and duplicate hit IDs fail validation/rendering.

The server cannot inspect the client's GUI scale or window dimensions. Offer Compact/Spacious preferences rather than claiming automatic CSS responsiveness. Independently versioned plugins sharing the same resource-pack shader namespace need coordination; multi-version/shared-pack management is not implemented. Third-party packs replacing the same core shaders need integration work.

## Suggested prompt and validation

> Use dui 0.1.0-SNAPSHOT on Paper 26.2 / Java 25. Build a [describe menu] with an unmodified client and matching resource pack. Put layout in dui XML templates, application state and permissions in Java, and use only the documented public APIs. Include plugin.yml, Gradle packaging, view data, action handlers and pack deployment steps. Update active sessions after custom actions, keep blocking work off Paper's main thread, and explain any requested feature that exceeds the documented limits.

For the library run `./gradlew build`; for the consumer run its normal build. dui-demo's explicit E2E commands exercise actual coordinates, screenshots, native models, forms, runtime images and effects. Unit builds must not silently provision or start Minecraft. Keep downloaded media, generated packs and runtime state out of commits.

### Native one-shot motion

`dui-item` supports `transition="pop|bounce|lift|slide"` with a nonnegative `transition-start` world tick, duration 1–127 ticks and distance 0–127 GUI pixels. Root motion controls these transitions too. `dui-particles effect="confetti"` supplies independent full-canvas paper particles. Use [the animated gift recipe](recipes.md#an-animated-gift-assembled-from-reusable-components); bounds/timing are template data, not menu-specific shaders. Rebuild a matching pack for this transport revision. Finish transient events before the 24,000-tick shader clock wraps and guard delayed application updates against closed or replaced sessions.

## Clipped carousels and Vanilla input

`slide` is a generic native-model transition, not a menu-specific shader. Its distance is signed (-127–127 GUI pixels): positive starts right of the final origin and slides left; negative starts left and slides right. All other presets retain their nonnegative distance. Supply a shared start tick for the strip; send an interaction update and one guarded completion update, not a packet per frame.

`clip-x/y/width/height` on `dui-item` define a fixed viewport in canvas coordinates. Supply all four, inside the canvas. Off-canvas item positions are allowed only with this explicit clip; clip-origin minus item-origin must fit -512–511. The mask applies to transformed native pixels and never changes hit regions. Keep hits in bounds, row aligned, and inert while cards are in transit. For stable views remove transient transitions; clipped native models still render without an animation. Regenerate the matching adapter/pack when upgrading.

Vanilla dialog callbacks provide discrete actions, not pointer-down/move/up streams, drag/swipe deltas or the client's GUI scale. Build swipe-like interaction using arrows and clickable cards, and offer Compact/Motion off preferences. Do not promise true touch dragging or wheel events. Runtime `dui-image` rasters do not support native-item transitions; illustrated cards must be consumer-owned textured models registered in the pack, or use real ItemStacks. Teleportation, destinations and permission checks belong in the consumer.

## Reusable card-table visuals

Use `dui-playing-card` and `dui-chip-stack` for reusable procedural 2D cards/chips, with root world-tick timing. Their attributes, encodings and bounds are documented in [components](components.md#procedural-cards-and-chip-transfers) and [rendering](rendering.md#runtime-image-layering-and-procedural-cards). Eight ordinary effects share one native carrier; generic tracked batches carry two. Default budget is eight, with opt-in up to 32. Back-facing data is not secret: omit opponent values until reveal. Bind `RasterImage` artwork using `dui-image image-layer="background"` to put labels/controls on top. Use default foreground images for portraits. All game rules, shuffled decks, action validation, hidden information and exactly-once payouts belong to the consumer, not GLSL or the template. See the companion demo’s `docs/poker.md` and `docs/blackjack.md` for full examples. For shoe-to-slot flight use `animation="fly" card-height="54"`, a rectangle spanning the top-right source and bottom-left destination, duration/delay and the original world-tick start. Card face height is independent of that allocated rectangle. This is the shared card preset, not a menu-specific shader.

## Illustrated wheel tables

Use `dui-wheel variant="european"` as a reusable single-zero visual preset, with numbers 0–36, `previous`, `animation="static|spin"`, `duration` 20–511 ticks, `turns` 1–7 and `palette="walnut|ebony"`. It needs square bounds at least 96 pixels and consumes one effect in the declared budget. Read the exact transport contract in [rendering](rendering.md#single-zero-wheel-preset-and-invisible-hits); legacy kind codes are frozen; new trusted effects use the protocol-2 extension contribution path. Regenerate the matching resource pack when upgrading the shader component.

For a bespoke illustrated board, compose `dui-surface`, `dui-text`, runtime `dui-image` and `dui-hitbox`. Hitboxes need a unique ID, nonempty action and y/height divisible by 9; they create no visual content. Use `bevel=1` for a visible surface border; `bevel=0` is a plain fill. Plain RGB chip images must have corners composited onto their actual cell colour, since rasters have no per-pixel alpha.

The consumer must choose results, reserve stakes, validate every action, prevent mid-spin bet changes, keep original timing through a resize and settle payouts exactly once. Shader animation is presentation, not physics-based randomness. Guard the one completion timer against session replacement, close and event generation. Provide Compact and Motion off controls; there is no automatic client GUI-size reporting. [dui-demo’s Roulette guide](https://github.com/Kembel-Entertainment/dui-demo/blob/master/docs/roulette.md) is a complete consumer example with unbiased live-spin screenshot tests.

## Consumer extension APIs

Use the implemented [composition APIs](composition.md) to register owner-prefixed components, pass typed map/list properties, project caller content and manage view/session UI tasks. Custom Java renderers compose existing Canvas primitives and do not install GPU opcodes. Use Page and RenderBudget for clamped collections and explicit resource priorities. Text wrapping uses the injected font metrics; layers support relative dimensions and anchors. Use [application-api.md](application-api.md) for implemented controllers/catalogues, explicit profiles, token styles, scene planning, resource handles, generic GPU tracks, opt-in batching, optional components and portable tests. See [compatibility](compatibility.md) before generating/deploying a matching pack. Durable game transactions must never depend on a cancellable UI task scope.


For full-body skins and vanilla armor, see [the player model contract](player-model.md). Use `dui-player-model` with `ViewModel.appearances`; appearance capture and inventory changes belong outside projection. Rich popup layers support `cover` and `dismiss`.
