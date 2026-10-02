# Verification of the design-neutral 0.2 library

Initial local acceptance on 2026-10-02 uses Paper/Minecraft 26.2, Java 25 and muted actual clients on macOS ARM64. Sources are applied to dui and dui-demo for review. No commit or push was made. Tests use isolated loopback ports 25604/25605; the local demo was subsequently deployed. Temporary fixtures never affect production permissions or gamemode. The full acceptance below records the original pack hash; the later memory regression uses a new hash and is recorded separately.

## Build and portable checks

The four library modules pass **79 tests**. The demo passes **169 tests**, comprising 166 application tests and three independent extension-proof tests. No test failed or was skipped. Domain component/game tests belong to the consumer; there is no preset module.

Source builds, Javadocs, generated component documentation and Java/GLSL checks for both protocol-4 contracts pass. Portable checks cover structured immutable properties, intrinsic measurement/child arrangement, affine group composition and unsupported input geometry, RGBA/rich text/font metrics, keyframes and scoped cancellation, player render registration, runtime map geometry/word round trips, custom Paper backend capabilities and single-use callback invalidation. Mutable Number subclasses are rejected in structured snapshots.

The independent extension-proof imports only public dui-core and build-time dui-pack APIs. It contributes a measured component with a list property, fragment defaults, a control skin, a glyph, a bitmap font, a player camera/limb-pose family and a **62-bit shader schema**. The demo supplies its Paper adapter and custom body backend; its art and domain component code are absent from DUI.

The combined ZIP SHA-1 is `4db5b9e2cfc7cb7bbd13ffbc72e86ef8448bb8bc`. Repeated pack builds produce byte-identical ZIP and metadata. Every client scenario below records this same ZIP hash; old-pack reports are excluded from the acceptance aggregate.

## Actual-client acceptance

All **16 scenarios** pass: **960 action/check steps** and **320 screenshots**. Runtime raster checks compare **808,489 pixel probes** in total, in addition to scenario-specific shader, model, projection and animation checks.

| Scenario | Steps | Screenshots | Result |
| --- | ---: | ---: | --- |
| showcase | 241 | 46 | PASS |
| shop | 59 | 11 | PASS |
| rewards | 56 | 14 | PASS |
| advent | 43 | 15 | PASS |
| warps | 61 | 20 | PASS |
| roulette | 64 | 22 | PASS |
| blackjack | 65 | 30 | PASS |
| poker | 66 | 28 | PASS |
| slots | 70 | 22 | PASS |
| confetti | 26 | 8 | PASS |
| videos | 26 | 5 | PASS |
| protocol | 12 | 9 | PASS |
| dynamic | 14 | 9 | PASS |
| casino | 48 | 42 | PASS |
| character | 41 | 23 | PASS |
| map | 68 | 16 | PASS |

Checks cover real click coordinates, Compact/Spacious and Auto GUI scale, runtime image pixels, shader output, finite animation, still-mode stability, popup coverage, native model/skin/armor rendering and application transactions. Shop QR matrices decode to the demo URL. Videos use deterministic runtime fixtures so acceptance does not depend on YouTube availability or pack-baked thumbnails. Reload fixtures change templates without rebuilding the pack.

The new `dynamic` scenario checks a rendered consumer bitmap font/full-body camera, group motion/opacity and a real click during animation. Unchanged logical actions remain clickable across sampled frames; replacing/updating a session cancels the old track. Additional screenshots check runtime map position/size, aiming, alpha blended over static map artwork and scroll zoom. Two actual left clicks advance its marker state; right-click closes it, private entities disappear and real inventory remains unchanged. Maximum measured runtime-layer coordinate error is **2.105 physical pixels**, below the four-pixel bound. Alpha pixels are checked against their actual declared underpaint rather than an opaque preflattened expectation.

Image checks account for the actual backend phases and declared occlusion; quiet views still require visible raster evidence. Calibration checks fit only Vanilla's multiplicative vignette with a tight hue residual; they retain the four-pixel geometry bound. Background checks catch an opaque root rectangle hiding a runtime image. Procedural/native motion must change visible pixels; still mode must remain stable.

## Map and lifecycle

The final map regression uses a **direct loopback connection**, with no added latency proxy in this pass. Six calibration screenshots exercise reference-yaw wrap, FOV 30/110 and GUI 1/2/Auto. Maximum measured coordinate error is **1.474 physical pixels**. 11 HUD screenshots check live text, contributed icons, transparent footer sides and native HUD coverage; 15 tile-edge samples match consumer artwork.

The HUD is replaced by an unrelated three-anchor template with seven controls. Its pixels/colors are checked, an invalid reload retains the previous template, and the original is restored. Entity IDs and pack identity remain unchanged. Input checks cover hover/locks, dwell/click selection, rapid slot changes and 0/8 wrap, details, right-click, dismount, external teleport, death/respawn, dialog/map replacement and shutdown. Real inventory and invisibility state are preserved during ordinary map use.

A simultaneous observer passes **1003 checks across 29 foreign entity IDs**, without seeing another player's private map entities or local appearance flags. Test config, operators and editable template files are restored afterward. Ordinary startup preserves user-edited runtime templates; E2E temporarily installs current source templates.

## Reproduction and limits

### Pack reload memory follow-up

A manual unmodified client crashed with `OutOfMemoryError: Java heap space` while loading the original pack with a 2 GiB heap limit. Its many single-character HUD/alpha providers allocated separate Vanilla Unicode lookup tables. This was a pack-generation problem, not evidence that 2 GiB clients are unsupported.

Equal-sized cells now share generated atlases. Total font providers decrease from **96,112 to 6,984** (bitmap providers: 95,723 to 6,595). Pixel content, cell dimensions, advances, baselines and low-alpha corner stamps are preserved; all ten shader resources and metadata contracts remain byte-identical apart from the ZIP hash. No renderer/world-map protocol change is required.

The optimized ZIP SHA-1 is `7fc4465afeb5ef66aaa1735e7e14bf7bcde8e967`. The library passes **82 unit tests**, including three new atlas regressions. Muted actual clients under an explicit **2 GiB heap limit** pass the protocol, dynamic and map scenarios; the protocol scenario reloads the complete resource pack **three times** and checks the client heap limit. Map projection/HUD pixels and the concurrent private-entity observer pass. `memory-regression.json` and the three regenerated scenario reports record this follow-up. The other thirteen scenario reports above still describe the initial ZIP; do not combine reports for different pack hashes into a new all-scenarios claim.

Run the library checks/componentDocs, then the consumer build/pack and explicit E2E tasks described in dui-demo's README and world-map guide. Reports, layout snapshots and galleries are under `dui-demo/build/reports/e2e/<scenario>/`; `dynamic-composition.json` aggregates this pass and `dynamic-pack-determinism.json` records reproducibility. Existing local reports are backed up when new reports are copied. Generated artifacts remain ignored by Git.

Automated clients inject input/diagnostics but use Minecraft's actual renderer. Manual review in an unmodified client remains separate. These checks establish supported local behavior, not shader-mod interoperability, other GPU platforms, arbitrary depth ordering or production throughput. Serialized component bytes exclude packet framing/native stack payloads; adapter timings exclude network/GPU work. Test FPS is capped at 30.

New assets/fonts/camera families/shader schemas require a consumer pack rebuild. Runtime data/layout/animation parameters and map geometry do not. Group capabilities, nine-unit hit rows, 15-level raster alpha and later-pass runtime map layers remain explicit limits; free runtime model camera angles are not supplied. See [dynamic composition](dynamic-composition.md) and [migration](migration-0.2.md) before deploying a coordinated plugin, ZIP, metadata and template upgrade.
