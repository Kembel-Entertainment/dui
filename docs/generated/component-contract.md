# Generated component contract

Generated from ComponentSchemas, which also validates templates. Placement and style properties appear on each schema. Bindings resolve at render time. Costs are counts, not network/FPS measurements.

## dui-badge

Nine-pixel hit rows; application authorizes actions

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `checked`, `class`, `color`, `column-span`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `hat`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `payload`, `player`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `tooltip`, `value`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-button

Nine-pixel hit rows; application authorizes actions

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `checked`, `class`, `color`, `column-span`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `hat`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `payload`, `player`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `tooltip`, `value`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-card

Allocated rectangles must fit; hit rows use multiples of nine

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `columns`, `cross-align`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `gap`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `payload`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `title`, `tone`, `tooltip`, `width`, `x`, `y`.

Defaults: {gap=0, locked=false}.

## dui-checkbox

Nine-pixel hit rows; application authorizes actions

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `checked`, `class`, `color`, `column-span`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `hat`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `payload`, `player`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `tooltip`, `value`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-chip-stack

Count 0..31; even delay 0..126; visual anchors only

Backend: shader. Cost: one effect.

Supported properties: `action`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `animation`, `bevel`, `border`, `class`, `color`, `column-span`, `count`, `delay`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `duration`, `fill`, `from`, `height`, `highlight`, `id`, `locked`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `palette`, `payload`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `to`, `tooltip`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-choice

Nine-pixel hit rows; application authorizes actions

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `checked`, `class`, `color`, `column-span`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `hat`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `payload`, `player`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `tooltip`, `value`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-column

Allocated rectangles must fit; hit rows use multiples of nine

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `columns`, `cross-align`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `gap`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `payload`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `title`, `tone`, `tooltip`, `width`, `x`, `y`.

Defaults: {gap=0, locked=false}.

## dui-component

One visual root; scoped properties and outlets

Backend: structural. Cost: expanded nodes.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `max-height`, `max-width`, `min-height`, `min-width`, `name`, `padding`, `props`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `width`, `x`, `y`.

Defaults: {}.

## dui-content

Caller-scoped content

Backend: structural. Cost: expanded nodes.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `max-height`, `max-width`, `min-height`, `min-width`, `name`, `padding`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `width`, `x`, `y`.

Defaults: {}.

## dui-divider

Positive bounded geometry

Backend: font. Cost: paints.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `width`, `x`, `y`.

Defaults: {}.

## dui-dropdown

1..8 distinct options; popup must fit above or below

Backend: font/occlusion. Cost: paints/hits/coverage.

Supported properties: `action`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dismiss`, `dock`, `fill`, `height`, `highlight`, `id`, `label`, `locked`, `max-height`, `max-width`, `min-height`, `min-width`, `open`, `padding`, `row-span`, `select`, `selected-border`, `selected-color`, `selected-fill`, `tooltip`, `value`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-empty

Nine-pixel hit rows; application authorizes actions

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `checked`, `class`, `color`, `column-span`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `hat`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `payload`, `player`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `tooltip`, `value`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-entry

Nine-pixel hit rows; application authorizes actions

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `checked`, `class`, `color`, `column-span`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `hat`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `payload`, `player`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `tooltip`, `value`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-grid

1..16 columns; spanning cells cannot overlap

Backend: font. Cost: paints/hits.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `cell-height`, `class`, `color`, `column-span`, `columns`, `cross-align`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `gap`, `height`, `highlight`, `id`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `width`, `x`, `y`.

Defaults: {gap=0}.

## dui-head

Native 8x8 profile; nine-pixel rows

Backend: head. Cost: one portrait.

Supported properties: `action`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `hat`, `height`, `highlight`, `id`, `label`, `locked`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `payload`, `player`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tooltip`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-heading

Injected font metrics; bounded wrapping and ellipsis

Backend: font. Cost: paints.

Supported properties: `active-border`, `active-color`, `active-fill`, `align`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `label`, `max-height`, `max-lines`, `max-width`, `min-height`, `min-width`, `padding`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `width`, `wrap`, `x`, `y`.

Defaults: {}.

## dui-hitbox

Unique id and action; full nine-pixel rows

Backend: font. Cost: one hit.

Supported properties: `action`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `locked`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `payload`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tooltip`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-if

Boolean binding

Backend: structural. Cost: expanded nodes.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `test`, `width`, `x`, `y`.

Defaults: {}.

## dui-image

Source snapshot required; pixel-size 1..8; total sample budget 16384

Backend: runtime image. Cost: sampled pixels.

Supported properties: `action`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `image-layer`, `locked`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `payload`, `pixel-size`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `source`, `tooltip`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-item

Size 1..127; clip offsets -512..511; motion bounded by protocol

Backend: native/model/clip/motion. Cost: one native body (11 GUI units).

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `burst-start`, `class`, `clip-height`, `clip-width`, `clip-x`, `clip-y`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `easing`, `fill`, `height`, `highlight`, `id`, `max-height`, `max-width`, `min-height`, `min-width`, `motion-delay`, `motion-duration`, `motion-start`, `opacity-from`, `opacity-to`, `padding`, `pivot-x`, `pivot-y`, `rotate-from`, `rotate-to`, `row-span`, `scale-from`, `scale-to`, `selected-border`, `selected-color`, `selected-fill`, `size`, `transition`, `transition-distance`, `transition-duration`, `transition-start`, `translate-x`, `translate-y`, `width`, `x`, `y`.

Defaults: {}.

## dui-layer

Allocated rectangles must fit; hit rows use multiples of nine

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `columns`, `cover`, `cross-align`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dismiss`, `dock`, `fill`, `gap`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `payload`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `title`, `tone`, `tooltip`, `width`, `x`, `y`.

Defaults: {cover=false, gap=0, locked=false}.

## dui-lever

Duration 1..127

Backend: shader. Cost: one effect.

Supported properties: `action`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `duration`, `fill`, `height`, `highlight`, `id`, `locked`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `payload`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tooltip`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-lights

Count 1..32; radius 1..15

Backend: shader. Cost: one effect.

Supported properties: `action`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `count`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `locked`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `payload`, `radius`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tooltip`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-menu

Canvas 120..480 by 9..360; height multiple of nine

Backend: font/native/effects. Cost: canvas.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `animation-start`, `bevel`, `border`, `class`, `color`, `column-span`, `compact`, `compact-height`, `compact-width`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `effect-budget`, `fill`, `focus-outline`, `gap`, `height`, `highlight`, `id`, `max-height`, `max-width`, `min-height`, `min-width`, `motion`, `padding`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `theme`, `width`, `x`, `y`.

Defaults: {effect-budget=8, focus-outline=hidden, gap=0, height=306, width=440}.

## dui-nav

Nine-pixel hit rows; application authorizes actions

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `checked`, `class`, `color`, `column-span`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `hat`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `payload`, `player`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `tooltip`, `value`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-node

Unique identity; valid parent references

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `icon`, `id`, `label`, `limit`, `locked`, `max`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `parent`, `payload`, `rank`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `shape`, `status`, `tone`, `tooltip`, `value`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-option

Distinct nonempty value

Backend: structural. Cost: none.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `label`, `locked`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tooltip`, `value`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-outlet

Declared named content

Backend: structural. Cost: expanded nodes.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `max-height`, `max-width`, `min-height`, `min-width`, `name`, `padding`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `width`, `x`, `y`.

Defaults: {}.

## dui-panel

Allocated rectangles must fit; hit rows use multiples of nine

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `columns`, `cross-align`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `gap`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `payload`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `title`, `tone`, `tooltip`, `width`, `x`, `y`.

Defaults: {gap=0, locked=false}.

## dui-particles

Coins/confetti; count 0..63; even delay

Backend: shader. Cost: one effect.

Supported properties: `action`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `count`, `delay`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `effect`, `fill`, `height`, `highlight`, `id`, `locked`, `max-height`, `max-width`, `min-height`, `min-width`, `origin-x`, `origin-y`, `padding`, `payload`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tooltip`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-player-model

Full-body GPU skin and vanilla armor; source is an appearance key; facing 0..7; contained in 48x72, 72x108, 108x162 or 144x216

Backend: player-model-v1. Cost: player model / nine-pixel skin bands / up to four armor carriers.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `facing`, `fill`, `height`, `highlight`, `id`, `idle`, `max-height`, `max-width`, `min-height`, `min-width`, `outer-layer`, `padding`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `source`, `width`, `x`, `y`.

Defaults: {facing=0, idle=true, outer-layer=true, source=viewer}.

## dui-playing-card

Card -1..51; even delay 0..62; bounded visual modes

Backend: shader. Cost: one effect.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `animation`, `bevel`, `border`, `card-height`, `class`, `color`, `column-span`, `delay`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `duration`, `face-down`, `fill`, `height`, `highlight`, `id`, `lift`, `locked`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `palette`, `payload`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tooltip`, `value`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-progress

Nine-pixel hit rows; application authorizes actions

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `checked`, `class`, `color`, `column-span`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `hat`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `payload`, `player`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `tooltip`, `value`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-rect

Positive bounded geometry

Backend: font. Cost: paints.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `width`, `x`, `y`.

Defaults: {}.

## dui-reel

Six symbols; bounded duration; symbol resources are build-time

Backend: shader. Cost: one effect.

Supported properties: `action`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `duration`, `fill`, `height`, `highlight`, `id`, `locked`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `payload`, `previous`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `sequence`, `symbol-size`, `symbols`, `tooltip`, `turns`, `value`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-repeat

Bound list <=200; stable ids supplied by consumer

Backend: structural. Cost: expanded nodes.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `as`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `items`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `width`, `x`, `y`.

Defaults: {}.

## dui-row

Allocated rectangles must fit; hit rows use multiples of nine

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `columns`, `cross-align`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `gap`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `payload`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `title`, `tone`, `tooltip`, `width`, `x`, `y`.

Defaults: {gap=0, locked=false}.

## dui-slot

Visual slot; count and durability ranges are validated

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `count`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `durability`, `enchanted`, `fill`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `payload`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `tooltip`, `value`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-spacer

Positive bounded geometry

Backend: font. Cost: paints.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `width`, `x`, `y`.

Defaults: {}.

## dui-stat

Nine-pixel hit rows; application authorizes actions

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `checked`, `class`, `color`, `column-span`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `hat`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `payload`, `player`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `tooltip`, `value`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-style

Local style properties

Backend: structural. Cost: none.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `width`, `x`, `y`.

Defaults: {}.

## dui-surface

Positive bounded geometry

Backend: font. Cost: paints.

Supported properties: `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `width`, `x`, `y`.

Defaults: {}.

## dui-tab

Nine-pixel hit rows; application authorizes actions

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `checked`, `class`, `color`, `column-span`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `hat`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `payload`, `player`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `tooltip`, `value`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-text

Injected font metrics; bounded wrapping and ellipsis

Backend: font. Cost: paints.

Supported properties: `active-border`, `active-color`, `active-fill`, `align`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `label`, `max-height`, `max-lines`, `max-width`, `min-height`, `min-width`, `padding`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `width`, `wrap`, `x`, `y`.

Defaults: {}.

## dui-toggle

Nine-pixel hit rows; application authorizes actions

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `checked`, `class`, `color`, `column-span`, `detail`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `hat`, `height`, `highlight`, `icon`, `id`, `label`, `locked`, `max`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `payload`, `player`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tone`, `tooltip`, `value`, `width`, `x`, `y`.

Defaults: {locked=false}.

## dui-tree

Bounded canvas tree

Backend: font. Cost: paints/hits.

Supported properties: `action`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `fill`, `height`, `highlight`, `id`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tooltip`, `width`, `x`, `y`.

Defaults: {}.

## dui-wheel

European wheel; square >=96; value 0..36

Backend: shader. Cost: one effect.

Supported properties: `action`, `active-border`, `active-color`, `active-fill`, `anchor-x`, `anchor-y`, `animation`, `bevel`, `border`, `class`, `color`, `column-span`, `disabled-border`, `disabled-color`, `disabled-fill`, `dock`, `duration`, `fill`, `height`, `highlight`, `id`, `locked`, `max-height`, `max-width`, `min-height`, `min-width`, `padding`, `palette`, `payload`, `previous`, `row-span`, `selected-border`, `selected-color`, `selected-fill`, `tooltip`, `turns`, `value`, `variant`, `width`, `x`, `y`.

Defaults: {locked=false}.

