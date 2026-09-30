# dui component reference

This reference describes **0.1.0-SNAPSHOT / Minecraft 26.2**. Every tag uses the `dui-` prefix. Templates are well-formed XML, including self-closing empty elements, with a single `dui-menu` root. See [recipes](recipes.md) for templates plus their view data and handlers, and [rendering](rendering.md) for the transport.

Unknown tags/attribute names are rejected. Attribute names currently have a **global** allowlist, not a schema per tag: a recognized name may be accepted on a component that does not use it. The tables below list attributes the component actually consumes. For example, `delay` does not delay a reel, and `duration` does not configure particles. Do not infer support merely because a template parses.

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
| `layer` | Children: `x=0`, `y=0`, `width=fill`, `height=<natural>` | Positions children relative to the layer. Explicit `height="fill"` uses the remaining height. Children must fit; later hit regions take precedence. |
| `rect` | `fill` | Flat coloured rectangle. Default fill is the background colour. |
| `surface` | `fill`, `border`, `bevel=1` | Framed surface; bevel 0–4. The rectangle must be large enough for its bevel. |
| `spacer`, `divider` | `height=9` | Empty flow space / horizontal separator. |

`width` and `height` are interpreted by the parent layout, not by CSS. For example, a column gives a child its full inner width; use a row or layer to allocate a narrower control. Ordinary flow content uses 9-pixel-aligned y positions/heights. Positioned paints may use sub-row offsets, but hit regions and heads still require 9-pixel-aligned rows. Overflow fails rendering.

Container padding currently uses the specified value horizontally, and **9 pixels vertically whenever padding is nonzero**. Padding zero gives no vertical inset. Gaps between ordinary flow rows should be multiples of 9. These are layout rules of this version, not browser box-model rules.

Declare empty `dui-style` elements directly under the root with unique `id`s. Apply `class="base accent"`; later classes override earlier ones and explicit attributes override classes. Style properties are `fill`, `border`, `color`, `disabled-fill`, `disabled-border`, `disabled-color`, `highlight`, `bevel`, `padding`. A style only changes a component when that component consumes the property.

Colours are `#RRGGBB`. Themes are `default`, `studio` and `studio_dark`; they remap known palette colours. Arbitrary custom colours remain unchanged. Raster images, native item textures and skins keep their own colours. `tone` selects the default accent or `gold`, `green`, `danger`, `muted`; it does not replace arbitrary backgrounds.

## Text and status

Text is supplied through attributes such as `label`; element body text is not a label. Labels are single-line, fitted to their bounds and normally shortened with `...`. Unsupported glyphs and control characters become `?`; do not assume browser Unicode shaping or multiline wrapping. Production text widths come from the generated pack metrics.

| Tag | Natural height | Consumed content/style |
| --- | ---: | --- |
| `heading` | 18 | `label`, `color` |
| `text` | 18 | `label`, `color`, `tone`, `align="center"` (otherwise left) |
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
| `item` | Required `id`; `size` defaults to the smaller allocated dimension; `burst-start=-1` | Requires `ViewModel.items[id]`. Native transport size 1–127 and the model must fit its canvas bounds. Visual only: an `action` on `dui-item` does not create a hit. |
| `slot` | Required explicit `id`, `label`, `count=1`, `durability=-1`, `active=false`, `action`, `value`, `tooltip`; natural height 63 | At least 44×63. Requires `ViewModel.items[id]`; draws a 36-pixel native item and a full-slot hit. Count 1–99; durability -1 hides the bar, otherwise 0–1. Payload uses `value`, not `payload`. |
| `image` | Required `source`, `id` defaults to source, `pixel-size=3`, optional `action`, `payload`, `tooltip`, `locked=false` | Resolves `ViewModel.images[source]`; no HTTP fetch occurs in this tag. Cell size 1–8; total sampled pixels per canvas ≤16,384. |

ItemStack components supply model/profile/banner/glint data. The `enchanted` template attribute does not configure the native stack. Slot count and durability are visual values; keep them consistent with your own ItemStack and business state. Native wrappers reserve CustomModelData colour entries from index 32 onwards. `__effects` is reserved for the shared effect carrier.

Native item transitions use `transition="pop|bounce|lift"`, required `transition-start` (nonnegative world game-time tick), `transition-duration=24` (1–127 ticks) and `transition-distance=36` (0–127 GUI pixels). `pop` enters with overshoot and upward travel, `bounce` briefly jiggles then settles, and `lift` moves up and holds a rotated final pose. Each item has independent timing. Geometry/hits remain unchanged: reserve enough visual space for travel beyond the original rectangle. Root `motion=false` displays final poses immediately. Do not put `burst-start` and a transition on the same item; use a separate `particles` component. Imperative callers can use `canvas.transition(itemId, new ItemTransition(...))` after placing the item.

A nonnegative `burst-start` starts finite item confetti using a world game-time tick; only one item confetti carrier is supported per canvas. Do not restart that tick on every unrelated update. For a large 3D player head, use a player-head ItemStack through `item`/`slot` rather than enlarging the portrait component.

Runtime rasters are RGB, without per-cell alpha. The raw `RasterImage` constructor accepts dimensions 1–512. `RasterImage.decode` rejects source edges above 4096 or more than 4,000,000 pixels, composites transparency onto `#16171D` and scales the longest edge to at most 256. `image` fills its rectangle with an aspect-preserving centre crop. Keep labels outside its bounds because images draw above normal paints. Supply QR rasters at exactly the sampled dimensions, with integer modules and a quiet zone, to avoid resampling them.

`ViewModel.links` uses the **hit ID**, not the action name. A nonempty action is still needed to create a clickable hit; the URL takes precedence over the custom callback. Only HTTP(S) links are accepted, using vanilla confirmation.

## Trees

`dui-tree` contains only `dui-node` children. Node attributes are `id`, centre `x`, top `y`, optional `parent`, `rank=0`, `limit=1`, `shape=square`, `status=locked`, `icon=star`, `label`/`tooltip`, `action`, `value` (defaults to id). Explicitly size the tree's allocated area.

Node IDs are unique within the tree and their hits must also be unique across the canvas. Centres need an 18-pixel horizontal margin; y is a multiple of 9. A `shape="root"` node occupies 36 pixels in height; other shapes occupy 45 including rank text. Parents must exist and be **below** their children. Status `learned`, `available`, `excluded` or the locked/default style changes colours, not permission. Supply valid rank/limit values and legal actions from application state.

## Shared shader effects

Each effect requires a unique `id` and a bounded allocated rectangle. At most eight effects fit a canvas. They accept optional `action`, `payload`, `tooltip`, `locked`; an actionable rectangle must use full 9-pixel rows. Start timing with root `animation-start="{{startedAt}}"`, from `player.getWorld().getGameTime()`. Tick durations use 20 ticks per second. Outcomes and settlement remain server-side.

| Tag | Consumed parameters / defaults | Bounds |
| --- | --- | --- |
| `reel` | `symbols=arcade`, `value=0`, `previous=value`, `sequence=0`, `turns=18+sequence*6`, `duration=45+sequence*11`, `symbol-size=max(1,min(width*.39,height*.42))` truncated to integer | Only arcade symbols; values 0–5, sequence 0–7, turns 6–63, duration/size 1–127. Supply explicit size for large reels. `sequence` supplies timing/turn defaults, not a delay. |
| `lever` | `duration=18` | 1–127 ticks |
| `particles` | `effect=coins`, `count=24`, `origin-x=width/2`, `origin-y=height-1`, `delay=70` | `coins` or `confetti`; count 0–63, origin inside local bounds, delay 0–126 in **even ticks**. Lifetime is derived internally, not supplied through `duration`. |
| `lights` | `count=12`, `radius=max(1,height/3)` | Count 1–32, radius 1–15. Supply explicit radius for tall light regions. |

Use only the parameters listed for each effect. Reel `delay`, particle `duration`/`radius`, and light `duration` are not consumed. See [EffectComponent](../dui-core/src/main/java/gg/kembel/dui/core/EffectComponent.java) and [ShaderEffect](../dui-core/src/main/java/gg/kembel/dui/core/ShaderEffect.java) for exact encoding/lifetimes; templates can move and resize these effects without rebuilding the pack.

## Data expansion and native forms

`{{user.name}}` traverses maps, not beans/record accessors. Missing bindings fail rendering. Values are substituted as literal attribute data, not parsed again as XML. Static XML still needs `&amp;` / `&lt;` escaping. Use booleans for `checked`, `open`, `active`, `locked` and conditional flags; the true string is exactly `true`.

`dui-repeat items="rows" as="row"` expands a list, at most 200 items. Supply map elements when binding fields such as `{{row.id}}`. Give repeated hits IDs such as `id="row_{{row.id}}"`; pagination/filtering belongs to the consumer. `dui-if test="{{visible}}"` includes its children only when the bound value is true; precompute comparisons/negation in Java. There is no expression language, automatic iteration index, two-way binding or built-in scroll container.

Templates are limited to 128,000 characters, nesting depth 20 and a 512-node expansion budget per render, including expansion directives. Repeats can hit the canvas/node limits long before 200 items. Compile once where possible, and supply small page-sized lists.

Native text/bool/selection/range inputs are Paper `DialogInput` objects supplied through `DialogOptions`, outside the custom canvas. A template `title` does not set the dialog title; use `DialogOptions.title`. Read submitted values through `ActionContext.response()` and validate them. See the [form recipe](recipes.md#native-form-inputs) and the full [LLM API guide](llm-guide.md).
