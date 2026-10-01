# dui abstraction roadmap

The eight-stage migration is implemented for review. This document records what shipped locally and the concrete boundaries of the vanilla renderer. Nothing is committed or pushed until review is approved.

| Stage | Implementation | Consumer proof |
| --- | --- | --- |
| Foundation | ComponentRegistry composition, typed props/outlets, relative layout, Page, RenderBudget, AnimationTimeline, TaskScope | Shared cart fragment, wrapping, explicit Blackjack priorities, Advent timing, scoped UI work |
| Controllers | MenuDefinition, MenuView, MenuController, ActionRouter, immutable MenuCatalogue | Every demo uses a projected controller and typed actions; resources/aliases/preparation/validation/cleanup register once; consumer menu classes own lifecycle effects |
| Layout | LayoutProfile/Context, GridLayout spans, CollectionView stable keys, Carousel, CardStrip, chrome package | Advent spans, Warps strip, shop/videos paging, Blackjack hand geometry, 12-entry Journal in two profiles |
| Styles | ThemeTokens, StyleResolver, spacing aliases, class state variants | Consumer-defined palettes without enum edits; Journal token styling; legacy palettes preserved |
| Scene | Immutable node/primitive snapshots, parent origins, declared backend capabilities, popup coverage plan, native clipping | Dropdown covers runtime image, head, item and effect without mutating source; Warps clipped animation |
| Transport | Generated schema/Java/GLSL codec, protocol/capability metadata, 32-effect opt-in batching, native/effect Motion tracks | Protocol Lab renders 14 effects, custom consumer pulse, moving native model and procedural card; motion-off and mismatches tested |
| Resources | ResourceHandle/Provider, bounded weighted cache/provider, ImageKey, RGBA composition, defensive ItemStacks, owned pack contributions | Videos and Journal share provider contract; QR decoded in client tests; alpha light/dark and bounds tests; demo-owned shader extension |
| Distribution | Optional dui-components and dui-test, component schemas/generated reference, shared real-client harness, guides/release matrix | Portable consumer tests, all demo fixtures share input/traversal helpers, new menus live only in dui-demo |

## Ownership

Core owns platform-independent presentation and bounded transport data. Paper owns native dialogs, revision-bound callbacks and owner-thread lifecycle. Pack tooling owns verified input generation, wrappers and trusted build-time contributions. Optional components own reusable visual adapters. Consumers own commands, permissions, real rules, persistence, transactions, HTTP policy and assets.

The existing low-level APIs remain available. Demo game engines were not translated into a UI state machine. UI jobs expire with view/session scopes; exactly-once slot settlement deliberately remains application-owned. Business event generations that guard state transitions remain where needed; hand-written UI session-identity checks are replaced by scopes.

## Deliberate renderer boundaries

Implementation satisfies the plan through supported backend contracts, not arbitrary browser features:

- Profiles are explicit preferences. Vanilla sends no GUI-scale/window reports or pointer drag stream.
- Scenes retain fixed backend phases, native viewport clips and whole-object popup coverage. Arbitrary cross-backend z-index, image alpha glyphs, effect/image clip masks and transformed hit testing are unsupported and documented.
- Native and procedural components share Motion tracks. Detailed wheel/ball, card-flip/flight and staggered chip behaviour remains reusable preset presentation. Legacy aliases/arcade symbols are compatibility paths; resource-driven reels and consumer effect contributions provide extensibility without new core tags/opcodes per demo.
- Default effect budget stays eight. Larger explicit budgets (up to 32) require protocol 2 and consume extra bodies. Tracked batches carry two effects. Limits are validated, not silently raised everywhere.
- Typography aliases select semantic text styling. Font geometry is pack-owned; no arbitrary CSS font loading/sizes are promised.
- Resource retention bounds do not replace a network adapter's concurrency/download/origin limits. Core renders snapshots and fetches no template URLs.
- WIP artifacts build/publish locally. No remote Maven release, multi-version shared-pack resolver or third-party core-shader merger is claimed.

## Review evidence

Public API tests cover owner-prefixed registration and immutable package merging; typed action rejection; catalogue aliases; spans and stable collections; profiles/overflow; custom token styles; scene source preservation; weighted cache bounds and mutable admission weights; alpha composition; generated field round trips; capabilities/legacy packs; scoped stale jobs; and unrelated application projections.

The companion client scenarios verify actual widget coordinates, received native models, runtime image pixels, QR decoding, gameplay ledgers, hidden dealer/opponent values, animation deltas, motion-off zero deltas, popup occlusion, effect batches and shader extension pixels. Reports are local artifacts under dui-demo/build/reports/e2e; they are intentionally not source-controlled. See [verification](verification.md) for the reviewed build's results.

## Completion criterion

A new application supplies its own definition, templates, resources, registered components and supported animation parameters using public APIs. New collection sizes, layouts, colours and business actions require no library edits. New underlying GPU operations, unsupported backend capabilities or Minecraft compatibility changes still require coordinated renderer work: a library cannot eliminate those changes.

For implementation use [application-api](application-api.md), [composition](composition.md), [generated component contract](generated/component-contract.md) and [compatibility](compatibility.md). Contributor wire details remain in [rendering](rendering.md).
