# dui components

Templates are well-formed XML with a `dui-menu` root. Bindings use `{{map.path}}`; missing values, duplicate hit IDs, overflow, unknown tags/attributes and external entities are rejected. `dui-repeat items="rows" as="row"` expands a bounded list; `dui-if test="{{visible}}"` includes conditional content.

| Group | Tags |
| --- | --- |
| Flow layout | `menu`, `row`, `column`, `grid`, `panel`, `card`, `spacer` |
| Positioned layout/style | `layer`, `surface`, `rect`, `style` |
| Text/status | `heading`, `text`, `badge`, `stat`, `progress`, `divider`, `empty`, `entry` |
| Controls | `nav`, `button`, `tab`, `choice`, `toggle`, `checkbox`, `dropdown`, `option` |
| Graphs | `tree`, `node` |
| Native media | `head`, `slot`, `item` |
| Runtime media | `image` |
| Shared effects | `reel`, `lever`, `lights`, `particles` |
| Data expansion | `repeat`, `if` |

Every tag uses the `dui-` prefix. Containers accept fixed dimensions or `fill`, and support `gap`. Grids support up to 16 columns. Positioned children of `dui-layer` use `x`, `y`, `width` and `height`; their hit regions still need full 9-pixel rows.

Use top-level `dui-style` declarations with IDs, then apply `class="base accent"`. Styles resolve in order; explicit attributes override them. Colours use `#RRGGBB`. Built-in themes are `default`, `studio` and `studio_dark`.

Controls use `id`, `action`, `value`/`payload`, `tooltip` and `locked`. Locked controls keep tooltips but emit no callback. Dropdowns accept `dui-option` children, `open`, `select` and `dismiss`; their overlay consumes outside clicks before controls behind it.

`dui-slot` is an inventory-style component with label, count and durability. `dui-item` places a real native model directly using `id` and `size`; the ViewModel must provide its ItemStack. A nonnegative `burst-start` enables finite confetti on that item. `dui-head` supports self, a player name, UUID or native skin-resource key.

`dui-image` binds `source` to a server-supplied raster key and samples in `pixel-size` cells, 1–8 GUI pixels. Images are drawn above normal canvas paints, below heads/items; put labels outside image bounds. Generate QR rasters at the exact cell dimensions with integer modules and a quiet zone so they are not resampled.

Effect components use bounded rectangles and typed parameters. The root's `animation-start` is a world game-time tick; `motion="false"` disables motion. Reels expose `value`, `previous`, `duration`, `delay`, `turns`, `symbols` and `symbol-size`. Particles expose `effect`, `count`, `origin-x`, `origin-y`, `delay`, `duration` and `radius`. Move or resize these components through templates without rebuilding the pack.

The native white focus outline is hidden by default; `focus-outline="native"` restores it. Native form inputs are supplied separately through `DialogOptions`.
