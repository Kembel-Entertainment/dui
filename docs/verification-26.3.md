# 26.3 / two-platform verification

Local acceptance on 2026-10-11, macOS ARM64 / Apple M3 Max / OpenGL 4.1. The official client is Minecraft 26.3 (protocol 777), Java 25; Paper is `26.3.build.170-beta` and Minestom is the immutable official snapshot `26_3-20261007.224755-10`. The test-only Fabric harness uses Loader 0.19.5, API 0.162.0+26.3 and Loom 1.17.21. Ordinary clients need no mod.

| Check | Result |
| --- | --- |
| Library build, Javadocs and generated contracts | 110 tests passed across all six modules; renderer/world-map protocol checks passed |
| Separate demo build | 180 consumer tests and 3 independent extension tests passed; one optional headless-browser integration test was skipped because `DUI_BROWSER_RUNTIME` was not configured |
| Exact vanilla shader compilation | 36 shader stages passed for the base pack and 36 for the combined consumer pack, including OIT variants |
| Shared Paper example | The same ten actual-client steps and screenshot pattern checks passed, including changed text/boolean responses, atlas/video pixels, cleanup and inventory restoration |
| Shared Minestom example | Ten actual-client steps passed: increment, text and changed boolean response, native item/player/armor rendering, private atlas, moving video and inventory restoration; screenshot pattern checks passed |
| Paper equipment integration | 41 actual-client steps passed; all four armor regions visibly differ from the bare skin; eight angles, stable still mode, native inventory quantities/components, binding/full inventory/stale click checks passed |
| Paper spatial integration | 68 actual-client steps passed, including camera/FOV, click/scroll, HUD reload and failure retention, close/dismount/teleport/death/disconnect cleanup and appearance restoration |
| Independent Paper observer | 827 checks passed; 29 private entity identities remained invisible to the observer |
| Paper renderer/protocol integration | Twelve actual-client steps and three pack reloads passed with a 2 GiB heap cap; 1,152 runtime-image probes, effect/native-item motion and popup coverage passed |
| Studio engine and editor | Four Java engine tests, seven frontend tests and ten Chromium/WebKit tests passed; Svelte check/build passed |
| Native Studio GPU previews | All 100 consumer fixtures passed against 26.3 assets, including shader override failures and recovery |
| Final companion checkouts | Applied source hashes matched; demo/extension and Studio engine builds passed at their final paths; Fabric test client compiled; all six DUI modules were published to Maven local and Studio's bundled Java runtime was rebuilt |

The finalized combined pack is SHA-1 `bac0297e4ea5839220b646cd05ee7574276cbec1`. The Paper equipment run and both shared platform runs use this pack. The earlier Paper renderer/protocol and spatial runs used `f74afad3544401937ebdb76dd6a62869a8f900d5`; the subsequent pack change replaces the armor model's removed `shade` field with `shade_direction_override`. Their recorded hashes remain unchanged. Historical 26.2 acceptance in [verification](verification.md) and demo media benchmarks remains tied to those older artifacts.

Reports retain native client/server logs, run start times, screenshot/layout metadata, JSON results and pixel checks under the separate demo's ignored `build/reports/e2e/`. During this port, isolated copies live under DUI's ignored `.cache/port-26.3/workspaces/dui-demo/`. Studio's 100 PNG/JSON pairs are under its ignored `build/preview-checks/`.

Use the commands in [the migration guide](migration-0.3.md), [Minestom setup](minestom.md) and the demo's `docs/platforms.md` to reproduce the checks. Library builds start no game. Actual-client and GPU tests require a desktop display. These results cover the local OpenGL backend; they do not establish Windows/Linux, Vulkan or third-party shader-pack compatibility. Browser editor tests use simulated IPC; native engine rendering is checked separately.

Both host dependencies remain pre-release pins. Studio uses the local DUI 0.3 source checkout until this reviewed port is committed and its new remote source revision can be pinned. No release or deployment is implied by these local checks.
