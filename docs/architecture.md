# Architecture and ownership

A compiled XML template plus read-only data produces a `Canvas`. It contains geometry, text/rectangles, hits, native placements, rasters and typed shader invocations. `RenderEnvironment` carries caller fonts, tokens, skins, glyph bindings and an optional color transformation. No design is chosen implicitly.

`dui-paper` renders this canvas into Adventure text and ordinary Paper dialog bodies. Negative spacing positions glyphs, invisible positive spans own clicks, and native-model wrappers transport placement/motion to the GPU. The resource pack supplies technical fonts, signatures and shared render transport. Consumer GLSL supplies concrete visuals. [Rendering](rendering.md) explains the actual font/packet/GPU contracts.

| Library owns | Consumer owns |
| --- | --- |
| Layout, bindings, typed validation, generic control state/hit behavior | Palette, typography choices, button/checkbox/dropdown painting and skin metrics |
| Glyph addressing, bands, exact advances | Icon/sprite pixels and whether they can be tinted |
| Shader schema, ABI generation, batching, finite transforms | Card/wheel/reel/particle art and parameter interpretation |
| Native item, portrait and full-body skin rendering | Equipment choices, rarity frames, player/inventory rules |
| Map projection, private seat/display/input, cleanup | Map layers, depths, images, target rules, HUD and opening animation settings |
| Callback capabilities, scoped tasks, pack readiness | Permissions, persistence, transactions, HTTP hosting and feeds |

Compose domain components through public `ComponentRegistry` renderers/fragments. A renderer receives a `ComponentContext` and can draw primitives or emit a registered `ShaderInvocation`. Use `PropertySchema` for typed defaults and validation. Combine immutable packages with `include`; names/IDs and pack resources collide explicitly.

`MenuController`, `MenuCatalogue`, `TaskScope`, `ResourceHandle` and collection/layout helpers separate application orchestration from rendering. Projection must not perform I/O, mutate game state or settle a transaction. [Application APIs](application-api.md) describe these lifetimes.

The `dui-demo` project owns all concrete visual/game components; there is no `dui-components` preset artifact. A new concrete design should add registrations/contributions to its consumer. Consumer `RenderPrimitive` objects can use a registered trusted Paper `BodyBackend`; a new transport or unsupported platform capability can still justify a library change. Neither an application-specific shader opcode nor an application palette belongs in dui.

The independent `dui-demo/extension-proof` uses public dependencies only. Its 62-bit shader, control skin, structured/measured component, glyph/font contribution and model render family prove that extensions are not restricted to demo visual formats. The demo Paper adapter also lowers its custom primitive. Changing values/layout needs a template/data update; adding shader code/glyph/font/model/map build inputs needs a matching pack rebuild. [Dynamic composition](dynamic-composition.md) describes shared group geometry, animation and the capability boundaries.
