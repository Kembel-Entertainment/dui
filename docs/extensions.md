# Building independent components and designs

A new design belongs in your plugin/project. Register immutable components, skins and pack contributions through public APIs. The library does not import your code and has no application opcode registry.

## Components and typed templates

```java
var schema = new PropertySchema(Map.of(
    "label", PropertySchema.Property.string(true, null),
    "padding", PropertySchema.Property.integer(0, 60, "6"),
    "color", PropertySchema.Property.color("#D6F5EB")));
var components = ComponentRegistry.builder()
    .template("acme-label", schema,
        "<dui-fragment><dui-text height=\"18\" label=\"{{props.label}}\" "
        + "color=\"{{props.color}}\"/></dui-fragment>")
    .build();
```

Use `<dui-acme-label label="Hello"/>`; omitted typed defaults are resolved before expansion. Names must be owner-prefixed, e.g. acme-label. Inline component declarations remain untyped string contracts with required props. Bindings inside a fragment use `props.*`; named outlets project caller content without evaluating Java expressions.

`builder.renderer(name, schema, naturalHeight, renderer)` registers a Java component. Its `ComponentContext` contains canvas/node/bounds/images/child renderer/deferred overlays. Draw existing primitives, add hits, place native models or emit shader calls. You own its visual policy. All drawing/hit resources retain canvas bounds and backend budgets. Custom native components declaring all four clip properties can compose clipped placements outside their allocated layer bounds; the native clip must still be in the canvas.

## Behavior and consumer skins

```java
var skin = new WidgetSkinRegistry.Skin(27, 0, 0, 18,
    (ctx, part) -> {
        int color = Integer.parseInt(ctx.node().s("skin-tint", "#16303F").substring(1), 16);
        ctx.canvas().rect(ctx.x(), ctx.y(), ctx.width(), ctx.height(), color);
        TextLayout.draw(ctx.canvas(), ctx.node().s("label", ""),
            ctx.x(), ctx.y(), ctx.width(), ctx.height(), "center", false, 1, 0xD6F5EB);
    }, (node, width, height) -> Map.of(),
    new PropertySchema(Map.of("skin-tint", PropertySchema.Property.color("#16303F"))));
var skins = new WidgetSkinRegistry(Map.of("button", skin));
var environment = new RenderEnvironment(metadata.font(), ThemeTokens.EMPTY, skins);
var template = dui.compile("acme/menu.html", xml, components, environment);
```

The library routes control behavior; the painter supplies the appearance. Skin fields are natural height, horizontal/vertical padding, option row height, painter, geometry and custom property schema. A choice skin supplies contained `previous`/`next` rectangles. Dropdown painters handle `field`, `popup` and `option`; library layout uses the skin's option height and supplies selected/locked state. Changing art or dimensions does not require changing generic behavior.

Skin custom color properties resolve `$tokens` before painting, as do typed component color properties. Template styles can contain registered skin/component fields. Unregistered properties fail parsing; range/default validation occurs after data binding. Rendering must be pure.

## Typed shader components

Declare one ordered `ShaderSpec` used by build tools and runtime code:

```java
var spec = new ShaderSpec("acme:ring", List.of(
    ShaderSpec.Parameter.rgb("inner"),
    ShaderSpec.Parameter.rgb("outer"),
    ShaderSpec.Parameter.decimal("thickness", .05, .5, .01),
    ShaderSpec.Parameter.integer("segments", 3, 24),
    ShaderSpec.Parameter.enumeration("mode", "solid", "pulse", "orbit"),
    ShaderSpec.Parameter.bool("invert")));
```

This schema uses 62 bits. Each scalar field uses at most 30 bits, but a whole schema may use up to 240 bits. Runtime invocations require exactly the declared keys, finite values within range and representable quantization. Decimal bounds/steps must fit a GLSL float; shader arithmetic has float precision. Java normalizes through encode/decode; no silent clamping. Enum order is part of the ABI. Ordered fields and all bounds/types/steps/choices are included in the SHA-256 schema fingerprint.

```glsl
vec4 acmeRing(vec2 q, vec2 size, DUI_PARAMETERS p, float t, bool live) {
    float radius = length((q - size * .5) / min(size.x, size.y) * 2.0);
    float ink = step(.7 - p.thickness, radius) * step(radius, .7);
    return vec4(mix(p.inner, p.outer, .5), ink);
}
```

The generator replaces `DUI_PARAMETERS` with a typed struct and emits matching decoders. INTEGER/ENUM fields become int, BOOLEAN becomes bool, DECIMAL float, RGB vec3 in 0..1. `q` is local GUI coordinate, `size` allocated bounds, `t` age in seconds and `live` indicates active motion. The renderer clips each invocation to `0 <= q < size`, after applying inverse generic motion, so an opaque shader cannot paint neighboring components. Allocate a larger rectangle when an animation needs more room. Return straight-alpha RGBA. Honor `live=false`; application-specific still poses belong in your function.

```java
var shader = new PackContribution.Shader(spec, "acmeRing", glslSource);
var contribution = new PackContribution("acme", Map.of(), List.of(shader));
PackGenerator.generate(clientJar, ownAssets, output, List.of(contribution));
canvas.effect(new ShaderInvocation("ring_1", spec, 12, 27, 54, 54,
    Map.of("inner", 0x4DDCCA, "outer", 0xCD5293, "thickness", .18,
        "segments", 12, "mode", "solid", "invert", false), 0));
```

Use a fresh world-tick start for transient invocations and provide lifetimeTicks for cleanup. Lifetimes are 0..11999 ticks; zero means static/no finite expiry. Pack metadata binds logical IDs to sorted six-bit addresses; never hardcode them. Each carrier holds at most eight invocations and 576 payload bits. A call costs 42 bits of address/bounds plus its actual schema, plus 99 if tracked with generic Motion. Batches preserve order and separate tracked/untracked calls.

Add trusted shared GLSL helpers through the contribution's namespaced `shaderModules` map. Modules are emitted before generated definitions, sorted by ID. Consumers must organize helper dependencies and choose globally unique GLSL function names. Templates cannot inject arbitrary GLSL. Unknown or mismatched runtime schemas fail pack validation before a dialog is sent.

## Glyphs and other build assets

`PackContribution.Glyph(new GlyphSpec("acme:icon", 9, 9, true), pngBytes)` supplies a glyph. Dimensions: width 1..32, height 1..18 GUI pixels; up to 256 registrations. Set tintable=false for full-color pictures. PNG dimensions must match the specification. Addresses are deterministically assigned; bands and width+1 advance are generated. Runtime `canvas.icon` or `dui-icon` references the ID, not Unicode numbers.

The six-field contribution accepts owner/resources/shaders/worldMaps/shaderModules/glyphs. Resource paths belong to `assets/<owner>/...`; collisions and owner mismatches fail. Add custom native item definitions under items/ for generated wrappers. Use one combined pack with the library transport; rebuilding design assets does not mean editing library shaders.

Layout, data, tokens, visibility, shader parameter values and registered glyph selection change at runtime. New glyph pixels, GLSL/schema, model files or static map geometry/images require a pack rebuild and matching metadata deployment.

## Independent proof and validation

`dui-demo/extension-proof` imports only dui-core and build-time dui-pack. It declares its own component, defaults, skin properties, glyph and the 62-bit ring shader above. Unit tests validate composition and schema round trips; the Protocol Lab samples the actual contributed shader pixels. This is a source build example, not a required preset dependency.

Run unit tests with fake state/scheduler, build the deterministic combined pack, then test real-client pixels and actions. Library tests cannot establish GPU compilation or native font hit ordering. See [rendering](rendering.md), [migration](migration-0.2.md), and [verification](verification.md).

See [dynamic composition](dynamic-composition.md) for measured/typed components, shared group geometry, RGBA/fonts, keyframes, runtime map layers, registered model cameras and trusted Paper backend extensions. Renderer/world-map protocol 4 packs must be rebuilt together with metadata.
