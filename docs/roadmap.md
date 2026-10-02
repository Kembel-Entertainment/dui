# Design-neutral boundary, version 0.2

The refactor separates platform rendering from application design. It intentionally removes the preset module and old numerical effect API; see [migration](migration-0.2.md).

| Library responsibility | Consumer responsibility |
| --- | --- |
| Canvas primitives, native models/heads/player appearance, font placement | Palettes, icon pixels, textures, rarity frames and chrome |
| ComponentRegistry, typed contracts, measure/arrange, fragment defaults/outlets | Cards, chips, wheels, reels, skill trees, inventories and gifts |
| Control behavior and WidgetSkinRegistry geometry contract | Field/button/checkbox/dropdown painting, dimensions and extra properties |
| ShaderSpec, generated decoder/dispatch, schema hashes and transport budgets | Trusted GLSL functions/modules, parameter meaning and procedural artwork |
| Motion, keyframe tracks, sampled scene/input groups and scoped lifetimes | Motion presets, game rules, actions, permissions and persistence |
| Generic camera-map/HUD projection, private entity lifecycle | Map geometry/art, targets, opening/pulse settings and HUD templates |
| Bitmap fonts, RGBA raster transport and registered player render families | Font pixels, model viewport/camera/pose definitions and visual assets |
| RenderPrimitive and trusted Paper BodyBackend SPI | Additional dialog-body lowering and its capability declarations |

The independent extension-proof consumer imports only public library APIs. It supplies measured components with structured properties, fragment defaults, a skin with token-resolved custom properties, a glyph, a bitmap font, a player render family and a typed 62-bit shader. Actual clients sample those contributions, click moving group controls and exercise an additional consumer dialog backend. The regular demos use their own registry and pack contributions.

Adding a supported design, layout, business action, skin, icon, font, model render family or shader function requires consumer changes only. Consumers can also lower their own RenderPrimitive through the Paper backend SPI. A new Minecraft transport, unsupported cross-layer masking or a renderer change can still justify a coordinated library change. No library can promise every future GPU feature without extension work.

Numerical defaults in core describe bounded protocol fields, Minecraft geometry or neutral identity transforms. The library contains no application palettes, theme enumeration, card/reel art or domain shader switch. Protocol 4 metadata rejects mismatches before presentation.

Renderer boundaries remain explicit: GUI profiles are preferences and dialog hits use nine-unit rows. SceneGroup shares sampled geometry with input, while standalone Motion is visual-only. Backend capabilities constrain group clipping/transforms; native models have viewport clips, and popups suppress covered late-backend objects as a whole. Runtime rasters preserve alpha with 15-step alpha quantization. Runtime map geometry/opacity uses a later overlay pass; arbitrary cross-backend ordering is unsupported. Core does not fetch URLs or implement browser CSS/JavaScript.

Use [extensions](extensions.md), [dynamic composition](dynamic-composition.md), [application APIs](application-api.md), [composition](composition.md), [rendering](rendering.md) and [verification](verification.md). Source changes are held for review before commit or push.
