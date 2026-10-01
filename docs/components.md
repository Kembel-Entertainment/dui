> Per-component accepted attributes/defaults/constraints/costs are generated from [ComponentSchemas](generated/component-contract.md), with [JSON editor metadata](generated/components.json). Optional `dui-visual-*`/chrome components require `VisualComponents.registry()`; see [application APIs](application-api.md). Legacy visual aliases below remain supported. Default effect budget is eight; explicit protocol-2 budgets may reach 32.

# dui component reference

This reference describes **0.1.0-SNAPSHOT / Minecraft 26.2**. Every tag uses the `dui-` prefix. Public custom components and reusable template fragments are described in [composition](composition.md). Templates are well-formed XML, including self-closing empty elements, with a single `dui-menu` root. See [recipes](recipes.md) for templates plus their view data and handlers, and [rendering](rendering.md) for the transport.

Unknown tags/attribute names are rejected. Built-in attribute names currently have a **global** allowlist, not a schema per tag: a recognized name may be accepted on a component that does not use it. The tables below list attributes the component actually consumes. For example, `delay` does not delay a reel, and `duration` does not configure particles. Do not infer support merely because a template parses. Registered custom renderers/components have their own declared attribute/property set plus the shared placement attributes.

## Root, layout and styling

| Tag | Attributes and defaults | Behaviour / bounds |
| --- | --- | --- |
| `menu` | `width=440`, `height=306`, `padding=0`, `gap=0`, `theme=default` | Width 120–480, height 9–360 divisible by 9. Children flow horizontally; use one `column` for a vertical menu. |
| Root preferences | `compact=false`, `compact-width`, `compact-height` | When compact is true, select the alternate canvas dimensions, defaulting to normal dimensions. This does not automatically reflow children. |
| Root effects | `animation-start=-1`, `motion=true` | Pass a nonnegative world game-time tick to start effects. Motion false shows final native transition poses and disables effect animation. |
| Root focus | `focus-outline=hidden` | `hidden` or `native`. This affects the custom canvas, not native form controls. |
| `row` | `gap=0`, `padding=0` | Fixed child widths consume space first; remaining width is shared by children without a width or with `width="fill"`. Children receive the row's height. |
| `column` | `gap=0`, `padding=0` | Fixed/natural child heights consume space first. Children with `height="fill"` share the remainder in full 9-pixel rows. Children receive the column's width. |
| `grid` | `columns=2`, `gap=0`, `padding=0` | 1–16 columns of equal width. Child natural heights determine each grid row's height. |
| `panel`, `card` | `gap=0`, `padding=6` | Vertical containers with a frame. A panel's natural height is its children's heights/gaps plus 18; a card defaults to 72. |
| `layer` | Children: `x=0`, `y=0`, `width=fill`, `height=<natural>`, `anchor-x=left`, `anchor-y=top` | Relative dimensions: `fill`, `50%`, `fill-12`, `50%-6`, resolved against parent extent minus inset. Anchors: left/center/right and top/center/bottom; vertical centering snaps to 9-pixel rows. Children must fit; later hit regions take precedence. |
| `rect` | `fill` | Flat coloured rectangle. Default fill is the background colour. |
| `surface` | `fill`, `border`, `bevel=1` | Framed surface; bevel 0–4. The rectangle must be large enough for its bevel. |
| `spacer`, `divider` | `height=9` | Empty flow space / horizontal separator. |

`width` and `height` are interpreted by the parent layout, not by CSS. For example, a column gives a child its full inner width; use a row or layer to allocate a narrower control. Ordinary flow content uses 9-pixel-aligned y positions/heights. Positioned paints may use sub-row offsets, but hit regions and heads still require 9-pixel-aligned rows. Overflow fails rendering.

Container padding currently uses the specified value horizontally, and **9 pixels vertically whenever padding is nonzero**. Padding zero gives no vertical inset. Gaps between ordinary flow rows should be multiples of 9. These are layout rules of this version, not browser box-model rules.

Declare empty `dui-style` elements directly under the root with unique `id`s. Apply `class="base accent"`; later classes override earlier ones and explicit attributes override classes. Style properties are `fill`, `border`, `color`, `disabled-fill`, `disabled-border`, `disabled-color`, `highlight`, `bevel`, `padding`. A style only changes a component when that component consumes the property.

Colours are `#RRGGBB`. Themes are `default`, `studio` and `studio_dark`; they remap known palette colours. Arbitrary custom colours remain unchanged. Raster images, native item textures and skins keep their own colours. `tone` selects the default accent or `gold`, `green`, `danger`, `muted`; it does not replace arbitrary backgrounds.

## Text and status

Text is supplied through attributes such as `label`; element body text is not a label. Labels normally remain single-line, fitted to their bounds and shortened with `...`. `dui-text wrap="true"` supports bounded word wrapping with `max-lines=3`, also capped by the allocated height/9; lines start at the box top and the last line is fitted with an ellipsis. Unsupported glyphs and control characters become `?`; do not assume browser Unicode shaping. Production text widths come from the generated pack metrics.

| Tag | Natural height | Consumed content/style |
| --- | ---: | --- |
| `heading` | 18 | `label`, `color` |
| `text` | 18 | `label`, `color`, `tone`, `align="left\|center\|right"`, `wrap=false`, `max-lines=3` |
| `badge` | 9 | `label`, `tone` |
| `stat` | 45 | `label`, `value`, optional `icon`, `tone` |
| `progress` | 9 | Numeric `value`, `max=100`, `tone`; displayed fraction is clamped to 0–1 |
| `empty` | 72 | `label`, `detail`, optional `icon=book` |
| `entry` | 27 | `label`, `value`, `icon` or `player`, `hat=true`; can also emit an action |

Keep enough height for a component's content; shrinking a multi-row component below its content can overflow. Built-in icon names are `settings`, `grid`, `diamond`, `star`, `users`, `book`, `lock`, `check`, `coin`, `leaf`, `sun`, `moon`, `previous`, `next`. The finite `item/...` sprite registry is available through `ItemGlyphs.names()`; real items use the native media components below.

## Actions and controls

Set explicit, unique `id`s for hit regions. `action` is a route consumed by your handler, not a command automatically executed by dui. Most ordinary controls send `payload` if supplied, otherwise `value`; tooltips default to their label. An empty action can keep a hover region without a callback where the component emits such a region. Every accepted custom action consumes that revision's callbacks, so update or close the session afterwards.

| Tag | Natural height | State / actions |
| --- | ---: | --- |
| `hitbox` | Explicit height | Required unique `id`, nonempty `action`; `payload`, `tooltip`, `locked=false`. Invisible hit rectangle, no paint or model carrier. Use a positioned layer; y and height must be multiples of 9. |
| `button`, `tab` | 18 | `label` or an icon when label is empty, `active=false`, `locked=false`, `action`, `value`/`payload`, `tooltip`. Label align is center by default or `left`. |
| Styled `button` | 18 | Supplying `fill` enables `border`, `color`, disabled colour variants, `highlight`, `bevel=1`; this branch renders its label rather than its icon. |
| `nav` | 27 | `label`, optional `icon`, `active=false`, `locked=false`, ordinary action/payload. |
| `toggle`, `checkbox` | 18 | `label`, `checked=false`, `locked=false`, ordinary action/payload. Checkbox needs at least 30×18. Clicking does not change `checked` automatically. |
| `choice` | 18 | `label`, displayed `value`, `action`, `locked=false`. Emits `<id>_previous` / `<id>_next` with payload `-1` / `1`; the consumer cycles options. Allow enough width for its arrows and value. |
| `dropdown` | 27 | Required `id`, `value`, fallback `label`, `open=false`, `locked=false`, header `action`, option `select`, outside `dismiss`, `tooltip`. At least 45×18. |
| `option` | 18 per popup row | Dropdown child with distinct nonempty `value`, `label`, `tooltip`, `locked=false`. 1–8 options. |

An open dropdown requires nonempty `select` and `dismiss` actions. Its popup occupies `optionCount × 18` pixels below the header, or above if it cannot fit below. Rendering fails if neither position fits. Header payload is empty; selection sends the option value with ID `<id>_option_<index>`; outside clicks send `<id>_dismiss`. The popup captures outside clicks before controls behind it. Native heads/items covered by the popup are suppressed. Keep your selected value and open flag in the view data, and update them in the handler.

`locked` disables callbacks on the controls above, dropdown options, actionable images and effects. It is not business authorization. **Slots and tree nodes do not consume `locked`**; give them an empty action when they should be inert and still re-check permissions/business state in the handler. Tree `status` is visual too.

## Media

| Tag | Attributes / defaults | Data and limits |
| --- | --- | --- |
| `head` | `player=self`, `hat=true`, optional `label`; natural height 18 | Native 8×8 portrait. Source can be player name, `uuid:<uuid>` or `texture:<namespace:key>`. Place on a 9-pixel row. |
| `item` | Required `id`; `size` defaults to the smaller allocated dimension; `burst-start=-1` | Requires `ViewModel.items[id]`. Native transport size 1–127; the placement must fit the canvas unless an explicit clip is supplied. Visual only: an `action` on `dui-item` does not create a hit. |
| `slot` | Required explicit `id`, `label`, `count=1`, `durability=-1`, `active=false`, `action`, `value`, `tooltip`; natural height 63 | At least 44×63. Requires `ViewModel.items[id]`; draws a 36-pixel native item and a full-slot hit. Count 1–99; durability -1 hides the bar, otherwise 0–1. Payload uses `value`, not `payload`. |
| `image` | Required `source`, `id` defaults to source, `pixel-size=3`, `image-layer=foreground`, optional `action`, `payload`, `tooltip`, `locked=false` | Resolves `ViewModel.images[source]`; no HTTP fetch occurs in this tag. Cell size 1–8; total sampled pixels per canvas ≤16,384. |

ItemStack components supply model/profile/banner/glint data. The `enchanted` template attribute does not configure the native stack. Slot count and durability are visual values; keep them consistent with your own ItemStack and business state. Native wrappers reserve CustomModelData colour entries from index 32 onwards. `__effects` is reserved for the shared effect carrier.

Native item transitions use `transition="pop|bounce|lift|slide"`, required `transition-start` (nonnegative world game-time tick), `transition-duration=24` (1–127 ticks) and `transition-distance=36` (0–127 GUI pixels). `pop` enters with overshoot and upward travel, `bounce` briefly jiggles then settles, and `lift` moves up and holds a rotated final pose. `slide` starts at `finalX + distance` and eases horizontally to `finalX`; its signed distance is -127–127. Positive distance enters from the right, negative from the left. Each item has independent timing. Geometry/hits remain unchanged: reserve enough visual space for travel beyond the original rectangle. Root `motion=false` displays final poses immediately. Do not put `burst-start` and a transition on the same item; use a separate `particles` component. Imperative callers can use `canvas.transition(itemId, new ItemTransition(...))` after placing the item.

Supply all four `clip-x`, `clip-y`, `clip-width`, `clip-height` attributes on `dui-item` to mask its native pixels to a **fixed canvas-space** viewport. The viewport must fit the canvas. A clipped item in a positioned layer can have an off-canvas origin; clip minus item-origin offsets must fit -512–511. The mask also applies to the item's animated/rotated pixels. Omitting a transition keeps a static clipped item. This does not clip or move controls: place separate fixed, bounded 9-pixel-row hits and disable them while sliding. A clipped item cannot carry legacy `burst-start`; use a separate particles component. Imperative placement: `canvas.item(id, x, y, size, new ItemClip(...))`. See [the carousel recipe](recipes.md#a-clipped-horizontal-carousel).

A nonnegative `burst-start` starts finite item confetti using a world game-time tick; only one item confetti carrier is supported per canvas. Do not restart that tick on every unrelated update. For a large 3D player head, use a player-head ItemStack through `item`/`slot` rather than enlarging the portrait component.

Runtime rasters are RGB, without per-cell alpha. The raw `RasterImage` constructor accepts dimensions 1–512. `RasterImage.decode` rejects source edges above 4096 or more than 4,000,000 pixels, composites transparency onto `#16171D` and scales the longest edge to at most 256. `image` fills its rectangle with an aspect-preserving centre crop. Foreground images draw above normal paints. Set `image-layer="background"` to draw the raster beneath surfaces/text/controls. Layer order is background images, normal paints, foreground images, native heads/items/effects; this is not arbitrary CSS z-index. Supply QR rasters at exactly the sampled dimensions, with integer modules and a quiet zone, to avoid resampling them.

`ViewModel.links` uses the **hit ID**, not the action name. A nonempty action is still needed to create a clickable hit; the URL takes precedence over the custom callback. Only HTTP(S) links are accepted, using vanilla confirmation.

## Trees

`dui-tree` contains only `dui-node` children. Node attributes are `id`, centre `x`, top `y`, optional `parent`, `rank=0`, `limit=1`, `shape=square`, `status=locked`, `icon=star`, `label`/`tooltip`, `action`, `value` (defaults to id). Explicitly size the tree's allocated area.

Node IDs are unique within the tree and their hits must also be unique across the canvas. Centres need an 18-pixel horizontal margin; y is a multiple of 9. A `shape="root"` node occupies 36 pixels in height; other shapes occupy 45 including rank text. Parents must exist and be **below** their children. Status `learned`, `available`, `excluded` or the locked/default style changes colours, not permission. Supply valid rank/limit values and legal actions from application state.

## Shared shader effects

Each effect requires a unique `id` and a bounded allocated rectangle. The default canvas budget is eight; explicit protocol-2 budgets allow up to 32. They accept optional `action`, `payload`, `tooltip`, `locked`; an actionable rectangle must use full 9-pixel rows. Start timing with root `animation-start="{{startedAt}}"`, from `player.getWorld().getGameTime()`. Tick durations use 20 ticks per second. Outcomes and settlement remain server-side.

| Tag | Consumed parameters / defaults | Bounds |
| --- | --- | --- |
| `wheel` | `variant=european`, `value=0`, `previous=value`, `animation=static`, `duration=140`, `turns=4`, `palette=walnut` | Number 0–36; `static` or `spin`; duration 20–511 ticks; turns 1–7; palettes `walnut`, `ebony`. Square bounds at least 96×96. The single-zero rendering preset supplies correct pockets, radial numbers, counter-rotating ball, drop and capture. |
| `reel` | `symbols=arcade`, `value=0`, `previous=value`, `sequence=0`, `turns=18+sequence*6`, `duration=45+sequence*11`, `symbol-size=max(1,min(width*.39,height*.42))` truncated to integer | Only arcade symbols; values 0–5, sequence 0–7, turns 6–63, duration/size 1–127. Supply explicit size for large reels. `sequence` supplies timing/turn defaults, not a delay. |
| `lever` | `duration=18` | 1–127 ticks |
| `particles` | `effect=coins`, `count=24`, `origin-x=width/2`, `origin-y=height-1`, `delay=70` | `coins` or `confetti`; count 0–63, origin inside local bounds, delay 0–126 in **even ticks**. Lifetime is derived internally, not supplied through `duration`. |
| `playing-card` | `value=-1`, `face-down=false`, `active=false`, `animation=static`, `duration=18`, `delay=0`, `lift=12`, `card-height=48` (fly only), `palette=mint` | Card ID 0–51, or -1 for an empty slot; suit × 13 + rank − 2 (C,D,H,S, ranks 2–14). `static`, `deal`, `flip`, `fly`; duration 1–127 ticks; delay 0–62 **even ticks**; lift 0–63 GUI pixels. Width ≥12; height ≥lift+18, or ≥card-height+6 for `fly`. Fly uses card-height 18–63 and travels from the allocated rectangle’s top-right to bottom-left; allocate enough width/height for the path. Other modes ignore card-height. Palettes `classic`, `mint`, `coral`, `violet` tint the back. `active` highlights the border. |
| `chip-stack` | `count=0`, `animation=static`, `duration=18`, `delay=0`, `palette=gold`, `from=bottom`, `to=top` | Count 0–31 (visual layers cap at seven); `static` or `transfer`; duration 1–127 ticks; delay 0–126 **even ticks**. `gold`, `mint`, `coral`, `violet`. Anchors: `top-left`, `top-right`, `bottom-left`, `bottom-right`, `top`, `bottom`, `left`, `right`. Static stack is centred; transfer travels between inset anchors. Transfer lifetime includes the stagger of visible layers: delay + duration + ceil((min(count,7)−1)×0.7) ticks; count=0 is inert. |
| `lights` | `count=12`, `radius=max(1,height/3)` | Count 1–32, radius 1–15. Supply explicit radius for tall light regions. |

Use only the parameters listed for each effect. Reel `delay`, particle `duration`/`radius`, and light `duration` are not consumed. See [EffectComponent](../dui-core/src/main/java/gg/kembel/dui/core/EffectComponent.java) and [ShaderEffect](../dui-core/src/main/java/gg/kembel/dui/core/ShaderEffect.java) for exact encoding/lifetimes; templates can move and resize these effects without rebuilding the pack.

## Data expansion and native forms

`{{user.name}}` traverses maps, not beans/record accessors. Missing bindings fail rendering. Values are substituted as literal attribute data, not parsed again as XML. Static XML still needs `&amp;` / `&lt;` escaping. Use booleans for `checked`, `open`, `active`, `locked` and conditional flags; the true string is exactly `true`.

`dui-repeat items="rows" as="row"` expands a list, at most 200 items. Supply map elements when binding fields such as `{{row.id}}`. Give repeated hits IDs such as `id="row_{{row.id}}"`; pagination/filtering belongs to the consumer. `dui-if test="{{visible}}"` includes its children only when the bound value is true; precompute comparisons/negation in Java. There is no expression language, automatic iteration index, two-way binding or built-in scroll container.

Templates are limited to 128,000 characters, authored nesting depth 20, component expansion depth 32 and a 512-node expansion budget per render, including expansion directives and projected content. Repeats can hit the canvas/node limits long before 200 items. Compile once where possible, and supply small page-sized lists.

Native text/bool/selection/range inputs are Paper `DialogInput` objects supplied through `DialogOptions`, outside the custom canvas. A template `title` does not set the dialog title; use `DialogOptions.title`. Read submitted values through `ActionContext.response()` and validate them. See the [form recipe](recipes.md#native-form-inputs) and the full [LLM API guide](llm-guide.md).

### Procedural cards and chip transfers

These are visual primitives, independent of poker state. `playing-card` draws an ivory face with rank/suit, an ornate back, rounded edges/shadow and an optional highlighted border. Ten is abbreviated **T** in its compact glyph. A flip starts on the opposite face and finishes on `face-down`; a deal enters from above and finishes at its inset resting position. `lift` reserves top travel space inside the rectangle; it does not move the hit. `value=-1` draws an empty placeholder. Never send a hidden opponent card ID: a face-down effect still carries its value in client-visible data. Send -1 for unknown cards, or use your own anonymous back artwork.

```xml
<dui-menu width="240" height="108" animation-start="{{tick}}" motion="{{motion}}">
 <dui-layer height="fill">
  <dui-playing-card id="ace" x="24" y="9" width="42" height="81"
    value="38" animation="deal" duration="28" lift="15" palette="mint" />
  <dui-chip-stack id="bet" x="90" y="18" width="120" height="72"
    count="5" animation="transfer" from="bottom" to="top" duration="18" />
 </dui-layer>
</dui-menu>
```

All effects share one root clock and bounded native carriers. Keep already-settled components `animation="static"` when a different event starts; otherwise changing the root tick replays their animations. Rendering does not advance game state or grant payouts. Replace finite modes with `static`/`animation-start=-1` using one guarded completion task. Root `motion=false` immediately shows final poses. Reserve bounds for the full card/flight; clipping happens at those bounds. A seven-card board plus a chip transfer uses all eight effect slots. For more simultaneous effects declare an explicit effect-budget up to 32 and deploy the matching protocol-2 pack.

## Wheels and illustrated hit regions

```xml
<dui-menu width="480" height="324" animation-start="{{startedAt}}" motion="{{motion}}">
  <dui-layer height="fill">
    <dui-wheel id="wheel" x="9" y="36" width="216" height="216"
        variant="european" value="{{result}}" previous="{{previous}}"
        animation="{{mode}}" duration="160" turns="4" palette="walnut" />
    <dui-surface x="234" y="72" width="16" height="27"
        fill="#357660" border="#A7AC83" bevel="1" />
    <dui-text x="234" y="81" width="16" height="9" label="0" align="center" />
    <dui-hitbox id="zero" action="place" payload="n:0"
        x="234" y="72" width="16" height="27" locked="{{busy}}"
        tooltip="Straight zero" />
  </dui-layer>
</dui-menu>
```

`wheel` is a visual preset, not a roulette game engine. The consumer chooses the outcome, owns the ledger, locks bets and settles exactly once. Keep the original start tick when replacing a view during a spin. The settled ball/pocket sit beneath the fixed twelve-o'clock marker. `turns` controls rotor travel; ball counter-rotation/deceleration are derived from that preset. `animation=static`, root `motion=false`, or an inactive event show the final pose. Explicitly changing Motion in a game still requires app-owned settlement/cancellation logic. Rebuild and deploy a matching pack when first adopting this component; moving it or changing its documented values needs no pack rebuild.

An invisible `hitbox` lets custom artwork, small labels and input geometry compose independently. It does not enlarge native items, track drag gestures, move with animated pixels, or bypass the 9-pixel click-row constraint. Overlapping hits follow normal canvas order (last wins). `locked` leaves the tooltip but clears the callback. Always re-check application state in your handler. The eight legacy effect codes remain assigned. Protocol-2 extensions use codes 8..15 through validated pack contributions, preserving the legacy three-bit payload.
