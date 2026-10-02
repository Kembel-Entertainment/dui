# GPU player model component

`dui-player-model` is a WIP full-body, orthographic cuboid renderer for vanilla Minecraft **26.2** on Paper. It uses the shared pack shaders and live native player-profile glyphs, not baked skin images. Clients need the generated DUI resource pack, with capability `player-model-v2`. No client mod is needed.

## Minimal consumer

Capture the appearance on Paper's server thread before projection:

```java
PlayerAppearance appearance = PlayerAppearance.capture(player);
ViewModel model = new ViewModel(data, images, items, links,
    Map.of("viewer", appearance));
MenuView view = MenuView.of(template, model, options);
```

```html
<dui-player-model id="hero" source="viewer" width="144" height="216"
  facing="1" outer-layer="true" idle="{{motion}}" />
```

`source` refers to `ViewModel.appearances`, not an item key, player name or URL. Capture and any `PlayerProfile.update()` run outside projection. Use a session `TaskScope.latest` for async resolution; publish its result on the owner thread, refresh, and discard it after close. Never call Bukkit inventory APIs from a future's background callback. An unresolved appearance uses the built-in Steve resource explicitly. `PlayerAppearance.skinResource(resource, slim, armor)` supports known, normalized 64×64 client resources for previews; for example `minecraft:entity/player/slim/alex`. Skins from complete profiles remain dynamic and are never inserted in the pack.

The appearance stores defensive profile/stack snapshots. Armor order in its constructor is **head, chest, legs, feet**. `capture(player)` obtains that order automatically. Keep old four-argument `ViewModel` calls for screens without player models. Missing appearance resources and missing pack capability fail before the pack prompt.

## Geometry and supported visuals

- The compatibility family `dui:player` has heights **72, 108, 162, 216** GUI units. Width is always height × 2/3. The component selects the largest preset contained in the allocated rectangle, centres it, and keeps its aspect ratio. Minimum allocation is 48×72. Y and allocated height follow the nine-pixel row grid.
- Compatibility facing: integer **0..7**, increasing in 45-degree steps. 0 is front, 1 is a quarter view, 4 is back. It is a quantized camera yaw, not arbitrary mouse dragging.
- Classic and Slim skin cuboids, hat/jacket/sleeves/trouser layers, directional shading, optional small idle arm/body motion. Both `idle` and canvas motion must be enabled. Still mode does not depend on shader time.
- Vanilla leather (including dye and overlay), chainmail, copper, iron, gold, diamond, netherite and turtle helmet geometry. The four armor layers use captured stacks; skins and equipment can change between view updates.
- This is a DUI renderer, not Minecraft's entity renderer from the E inventory. The current renderer does not render armor trims, enchantment glint on the figure, elytra, capes, custom equipment geometry, held tools, or attack/walk animations. Hand items can still be shown with ordinary native `dui-item` icons. Unsupported armor has no figure overlay; its native slot icon remains usable.

## Transport contract

The native head object binds the client's complete skin texture. Each nine-pixel text row carries one marked, hat-disabled head object. `text.vsh` expands that object's eight-pixel quad into **only that nine-pixel band**, preventing later background rows from overwriting the model. `text.fsh` casts an orthographic ray through six skin cuboids and the optional outer cuboids, then samples the standard skin UV layout. No raster skin, generated skin glyph atlas or repeated animation packets are involved.

The RGB marker is `0xE40000`, masked by `0xFC0000`. Low-byte bits are viewport 0..1, facing 2..4, Slim 5, outer layer 6, idle 7. Bits 8..13 encode the band index; bits 14..17 encode the registered renderer family. These colors are reserved for DUI's marked skin objects; ordinary head components remain unchanged. A band has the native object's eight-pixel **advance**, even though its shader viewport is wider. The row renderer rewinds eight pixels, preserving negative-spacing invariants.

Armor cannot sample the separate skin texture in the same item draw. The pack therefore adds tiny flat texture carriers referencing the client's vanilla armor files. An item-atlas source registers those vanilla resources without copying their PNGs. A distinct four-color frame identifies armor carriers; the native base/header transports the model viewport and the left-edge cells carry the same pose flags plus armor slot bits 8..9. `position_tex_color` then constructs the armor cuboids with the same camera and occlusion rules as the skin shader. Leather uses the native dye tint before this stage. Transport changes affect clones, never the equipped stacks.

Skin bands are rendered in the text canvas; armor carriers follow the existing layer fence and share its native item-body ordering and offset calculation. Ordinary native models retain that placement path. `player-model-v2` is a separate capability within the current protocol-4 pack contract. A matching current pack without this capability rejects screens requiring player models; protocol-1/2/3 packs are rejected entirely.

## Popups and cost

`Canvas.playerModels` and `Scene.Backend.PLAYER_MODEL` expose the presentation snapshot. RenderReport counts models; a model costs height/9 native skin glyphs and up to four native armor bodies. These are counts, not measured frame timings. Texture/profile caching is owned by the native client; idle is GPU-only.

A popup overlapping any part of a model suppresses the entire skin and all its armor, rather than clipping individual cuboids. Declarative rich pickers can use:

```html
<dui-layer id="picker" x="180" y="36" width="120" height="90"
  cover="true" dismiss="dismiss-picker">
  <!-- ordinary surfaces, native items, texts and action buttons -->
</dui-layer>
```

`cover` suppresses previously drawn foreground images, heads, native items, effects and models that intersect the rectangle. Background images remain under the popup. `dismiss` adds an outside-click hit across the canvas; children drawn later take precedence. Position the popup layer after the content it covers. This is whole-object coverage across fixed backends, not a new arbitrary painter-order API.

The DUI library does not equip items or define rarity. See the separate demo's Aster character sheet for inventory fingerprints, authoritative swaps, Curse of Binding, capacity checks, pagination and live attributes.

## Independent camera families

Register `new PlayerRenderSpec("acme:portrait", viewports, poses)` through `PackContribution.playerRenderers()`. A viewport has an independent width (1..480) and height (9..360, divisible by nine). A pose defines yaw/pitch in degrees, world height (1..256), six limb pitches (head, torso, left arm, right arm, left leg, right leg), idle amplitude in degrees and speed. There are at most 16 families including the compatibility family, four viewports and eight poses per family. Runtime `renderer="acme:portrait" facing="{{poseIndex}}"` selects them; unknown family/pose and non-fitting allocation fail explicitly. Camera tables are generated once from the same specifications shipped in metadata. Selecting another registered pose at runtime needs no pack rebuild; editing a pose definition or adding/changing a family does. Free runtime camera angles/custom meshes/held tools are not supplied by this component; consumer backends/GLSL can implement different renderers.

The `0xE40000..0xE7FFFF` RGB interval is reserved for marked GUI profile objects. Avoid tinting external native skin-head objects into this interval. Shader recognition also checks a 64×64 skin texture and face UVs; ordinary white profile glyphs stay native. [Dynamic composition](dynamic-composition.md) documents group support and extension boundaries.
