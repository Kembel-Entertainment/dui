# Generated component contract

Generated from ComponentSchemas, which also validates templates. Placement and style properties appear on each schema. Bindings resolve at render time. Costs are counts, not network/FPS measurements.

## dui-badge

Nine-pixel hit rows; application authorizes actions

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `checked`, `class`, `color`, `column-span`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `hat`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max`, `max-height`, `max-width`, `min-height`, `min-width`, `next-tooltip`, `padding`, `padding-x`, `padding-y`, `payload`, `player`, `previous-tooltip`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `tooltip`, `value`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-button

Nine-pixel hit rows; application authorizes actions

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `checked`, `class`, `color`, `column-span`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `hat`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max`, `max-height`, `max-width`, `min-height`, `min-width`, `next-tooltip`, `padding`, `padding-x`, `padding-y`, `payload`, `player`, `previous-tooltip`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `tooltip`, `value`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-checkbox

Nine-pixel hit rows; application authorizes actions

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `checked`, `class`, `color`, `column-span`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `hat`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max`, `max-height`, `max-width`, `min-height`, `min-width`, `next-tooltip`, `padding`, `padding-x`, `padding-y`, `payload`, `player`, `previous-tooltip`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `tooltip`, `value`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-choice

Nine-pixel hit rows; application authorizes actions

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `checked`, `class`, `color`, `column-span`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `hat`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max`, `max-height`, `max-width`, `min-height`, `min-width`, `next-tooltip`, `padding`, `padding-x`, `padding-y`, `payload`, `player`, `previous-tooltip`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `tooltip`, `value`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-column

Allocated rectangles must fit; hit rows use multiples of nine

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `columns`, `cross-align`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `gap`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `padding-x`, `padding-y`, `payload`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `title`, `tone`, `tooltip`, `width`, `x`, `y`.

Defaults: {gap=0, locked=false}.

## dui-component

One visual root; scoped properties and outlets

Backend: structural. Cost: expanded nodes.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `max-height`, `max-width`, `min-height`, `min-width`, `name`, `padding`, `padding-x`, `padding-y`, `props`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `width`, `x`, `y`.

Defaults: {}.

## dui-content

Caller-scoped content

Backend: structural. Cost: expanded nodes.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `max-height`, `max-width`, `min-height`, `min-width`, `name`, `padding`, `padding-x`, `padding-y`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `width`, `x`, `y`.

Defaults: {}.

## dui-divider

Positive bounded geometry

Backend: font. Cost: paints.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `padding-x`, `padding-y`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `width`, `x`, `y`.

Defaults: {}.

## dui-dropdown

1..8 distinct options; popup must fit above or below

Backend: font/occlusion. Cost: paints/hits/coverage.

Supported properties: `action`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dismiss`, `dismiss-tooltip`, `dock`, `fill`, `height`, `highlight`, `id`, `label`, `locked`, `max-height`, `max-width`, `min-height`, `min-width`, `open`, `padding`, `padding-x`, `padding-y`, `row-span`, `select`, `selected-border`, `selected-color`, `selected-fill`, `tooltip`, `value`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-grid

1..16 columns; spanning cells cannot overlap

Backend: font. Cost: paints/hits.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `cell-height`, `class`, `color`, `column-span`, `columns`, `cross-align`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `gap`, `height`, `highlight`, `id`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `padding-x`, `padding-y`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `width`, `x`, `y`.

Defaults: {gap=0}.

## dui-group

Bounded affine raster composition; hit/native capabilities checked

Backend: group. Cost: raster/native/hits.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `clip-height`, `clip-width`, `clip-x`, `clip-y`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `gap`, `height`, `highlight`, `id`, `max-height`, `max-width`, `min-height`, `min-width`, `opacity`, `padding`, `padding-x`, `padding-y`, `rotate`, `row-span`, `scale-x`, `scale-y`, `selected-border`, `selected-color`, `selected-fill`, `translate-x`, `translate-y`, `width`, `x`, `y`.

Defaults: {gap=0}.

## dui-head

Native 8x8 profile; nine-pixel rows

Backend: head. Cost: one portrait.

Supported properties: `action`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `hat`, `height`, `highlight`, `id`, `label`, `locked`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `padding-x`, `padding-y`, `payload`, `player`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tooltip`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-heading

Injected font metrics; bounded wrapping and ellipsis

Backend: font. Cost: paints.

Supported properties: `active-border`, `active-color`, `active-fill`, `align`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `font`, `height`, `highlight`, `id`, `label`, `max-height`, `max-lines`, `max-width`, `min-height`, `min-width`, `padding`, `padding-x`, `padding-y`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `width`, `wrap`, `x`, `y`.

Defaults: {}.

## dui-hitbox

Unique id and action; full nine-pixel rows

Backend: font. Cost: one hit.

Supported properties: `action`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `locked`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `padding-x`, `padding-y`, `payload`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tooltip`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-icon

Contributed glyph dimensions 1..32 by 1..18; caller tint policy

Backend: font. Cost: paints.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `max-height`, `max-width`, `min-height`, `min-width`, `name`, `padding`, `padding-x`, `padding-y`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `width`, `x`, `y`.

Defaults: {}.

## dui-if

Boolean binding

Backend: structural. Cost: expanded nodes.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `padding-x`, `padding-y`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `test`, `width`, `x`, `y`.

Defaults: {}.

## dui-image

Source snapshot required; pixel-size 1..8; total sample budget 16384

Backend: runtime image. Cost: sampled pixels.

Supported properties: `action`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `image-layer`, `locked`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `padding-x`, `padding-y`, `payload`, `pixel-size`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `source`, `tooltip`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-item

Size 1..127; clip offsets -512..511; motion bounded by protocol

Backend: native/model/clip/motion. Cost: one native body (11 GUI units).

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `clip-height`, `clip-width`, `clip-x`, `clip-y`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `easing`, `fill`, `height`, `highlight`, `id`, `max-height`, `max-width`, `min-height`, `min-width`, `motion-delay`, `motion-duration`, `motion-start`, `opacity-from`, `opacity-to`, `padding`, `padding-x`, `padding-y`, `pivot-x`, `pivot-y`, `rotate-from`, `rotate-to`, `row-span`, `scale-from`, `scale-to`, `selected-border`, `selected-color`, `selected-fill`, `size`, `translate-x`, `translate-y`, `width`, `x`, `y`.

Defaults: {}.

## dui-layer

Allocated rectangles must fit; hit rows use multiples of nine

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `columns`, `cover`, `cross-align`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dismiss`, `dock`, `fill`, `gap`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `padding-x`, `padding-y`, `payload`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `title`, `tone`, `tooltip`, `width`, `x`, `y`.

Defaults: {cover=false, gap=0, locked=false}.

## dui-menu

Canvas 120..480 by 9..360; height multiple of nine

Backend: font/native/effects. Cost: canvas.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `animation-start`, `background`, `bevel`, `border`, `class`, `color`, `column-span`, `compact`, `compact-height`, `compact-width`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `effect-budget`, `fill`, `focus-outline`, `focus-outline-color`, `gap`, `height`, `highlight`, `id`, `max-height`, `max-width`, `min-height`, `min-width`, `motion`, `padding`, `padding-x`, `padding-y`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `width`, `x`, `y`.

Defaults: {background=none, effect-budget=8, focus-outline=native, gap=0}.

## dui-option

Distinct nonempty value

Backend: structural. Cost: none.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `label`, `locked`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `padding-x`, `padding-y`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tooltip`, `value`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-outlet

Declared named content

Backend: structural. Cost: expanded nodes.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `max-height`, `max-width`, `min-height`, `min-width`, `name`, `padding`, `padding-x`, `padding-y`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `width`, `x`, `y`.

Defaults: {}.

## dui-panel

Allocated rectangles must fit; hit rows use multiples of nine

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `columns`, `cross-align`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `gap`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `padding-x`, `padding-y`, `payload`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `title`, `tone`, `tooltip`, `width`, `x`, `y`.

Defaults: {gap=0, locked=false}.

## dui-player-model

Full-body GPU skin and vanilla armor; source is an appearance key; registered renderer selects up to four viewports/eight poses

Backend: player-model-v2. Cost: player model / nine-pixel skin bands / up to four armor carriers.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `facing`, `fill`, `height`, `highlight`, `id`, `idle`, `max-height`, `max-width`, `min-height`, `min-width`, `outer-layer`, `padding`, `padding-x`, `padding-y`, `renderer`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `source`, `width`, `x`, `y`.

Defaults: {facing=0, idle=true, outer-layer=true, source=viewer}.

## dui-progress

Nine-pixel hit rows; application authorizes actions

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `checked`, `class`, `color`, `column-span`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `hat`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max`, `max-height`, `max-width`, `min-height`, `min-width`, `next-tooltip`, `padding`, `padding-x`, `padding-y`, `payload`, `player`, `previous-tooltip`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `tooltip`, `value`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-rect

Positive bounded geometry

Backend: font. Cost: paints.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `padding-x`, `padding-y`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `width`, `x`, `y`.

Defaults: {}.

## dui-repeat

Bound list <=200; stable ids supplied by consumer

Backend: structural. Cost: expanded nodes.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `as`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `items`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `padding-x`, `padding-y`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `width`, `x`, `y`.

Defaults: {}.

## dui-row

Allocated rectangles must fit; hit rows use multiples of nine

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `columns`, `cross-align`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `gap`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `padding-x`, `padding-y`, `payload`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `title`, `tone`, `tooltip`, `width`, `x`, `y`.

Defaults: {gap=0, locked=false}.

## dui-spacer

Positive bounded geometry

Backend: font. Cost: paints.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `padding-x`, `padding-y`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `width`, `x`, `y`.

Defaults: {}.

## dui-style

Local style properties

Backend: structural. Cost: none.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `padding-x`, `padding-y`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `width`, `x`, `y`.

Defaults: {}.

## dui-surface

Positive bounded geometry

Backend: font. Cost: paints.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `padding-x`, `padding-y`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `width`, `x`, `y`.

Defaults: {}.

## dui-tab

Nine-pixel hit rows; application authorizes actions

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `checked`, `class`, `color`, `column-span`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `hat`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max`, `max-height`, `max-width`, `min-height`, `min-width`, `next-tooltip`, `padding`, `padding-x`, `padding-y`, `payload`, `player`, `previous-tooltip`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `tooltip`, `value`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-text

Injected font metrics; bounded wrapping and ellipsis

Backend: font. Cost: paints.

Supported properties: `active-border`, `active-color`, `active-fill`, `align`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `font`, `height`, `highlight`, `id`, `label`, `max-height`, `max-lines`, `max-width`, `min-height`, `min-width`, `padding`, `padding-x`, `padding-y`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `width`, `wrap`, `x`, `y`.

Defaults: {}.

## dui-toggle

Nine-pixel hit rows; application authorizes actions

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `checked`, `class`, `color`, `column-span`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `hat`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max`, `max-height`, `max-width`, `min-height`, `min-width`, `next-tooltip`, `padding`, `padding-x`, `padding-y`, `payload`, `player`, `previous-tooltip`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `tooltip`, `value`, `width`, `x`, `y`.

Defaults: {locked=false}.

