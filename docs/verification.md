# Verification of the abstraction migration

The migration uses portable unit/build checks and actual muted Minecraft 26.2 clients on an isolated localhost Paper server. No production permissions, original running server or user client are changed. Generated artifacts/reports stay out of source control.

The unit/build gate covers all five dui modules, sources/Javadocs, generated component documentation, generated codec consistency, consumer builds and gameplay tests. Client scenarios cover all demo designs; Protocol Lab adds more-than-eight effect batching, an application-owned shader extension, generic model/card motion, motion-off stability and popup coverage over late backends.

See the final local acceptance results below. Component serialization size is not full packet size. Adapter render time excludes network/GPU work. The client is capped at 30 FPS for test stability, so its FPS sample is only evidence of operation under that cap, not proof of production scalability. The default budget remains eight.

## Local acceptance results

Library: 73 tests passed across all five modules. Demo: 65 tests passed. Sources/Javadocs, generated codec check, deterministic component documentation and both source builds passed. No unit test was skipped.

Twelve actual-client scenarios passed: 768 action/check steps and 226 screenshots. Existing scenarios and the targeted final protocol rerun use the same generated ZIP hash `a2ddcb2e76d5f97ec51635e1d9a50e38e99504f1`. The targeted rerun follows the batch/controller optimisation; it verifies the new controller's actual page and profile changes.

| Scenario | Steps | Screenshots | Result |
| --- | ---: | ---: | --- |
| showcase | 241 | 46 | PASS |
| shop | 59 | 11 | PASS |
| rewards | 56 | 14 | PASS |
| advent | 43 | 15 | PASS |
| warps | 61 | 20 | PASS |
| roulette | 43 | 18 | PASS |
| blackjack | 65 | 30 | PASS |
| poker | 66 | 28 | PASS |
| slots | 70 | 22 | PASS |
| confetti | 26 | 8 | PASS |
| videos | 26 | 5 | PASS |
| protocol | 12 | 9 | PASS |

The Protocol Lab renders 12 built-in lights plus one consumer-owned pulse and one tracked procedural card, alongside a genuine native grass-block model. Its final plan uses six total dialog bodies, reduced from ten by separating ordinary and tracked batches without reordering effects. The source scene remains intact when a dropdown covers its image/head/model; pixel checks verify popup readability and generic movement. Motion-off changes zero sampled pixels. Player inventory remains unchanged.

Example measurement: serialized font component 37,412 bytes; initial adapter display 38.11 ms; subsequent sampled displays 5.21–5.96 ms; average sampled client FPS 28.24 under a 30-FPS cap. These are local observations, not a packet measurement, latency guarantee or maximum GPU budget. More bodies also consume native dialog height, so applications must budget their chosen profile.

Shop QR matrices were decoded against the demo URL. Video thumbnails were compared against deterministic runtime fixtures, without adding them to the pack or requiring upstream YouTube availability. Native motion, clipping, hidden card values, monetary ledgers, finite confetti, focus-outline suppression and HTML reload without a pack rebuild are validated by the corresponding scenario reports. Pack generation repeated with identical SHA-1.

Reports are intentionally ignored local artifacts under `dui-demo/build/reports/e2e/<scenario>/`, including galleries, layout metadata, screenshots and verification JSON. The reviewed changes remain uncommitted/unpushed.


## Projected-controller migration for every demo

All twelve menu applications now use actual MenuDefinition projections and typed routes through MenuController.refresh. The prior compatibility presentation wrapper is removed from dui-demo. Showcase setup/form/confirmation views are projected screens; phase advancement, captured native resources, persistence and async work live outside projection. The bootstrap plugin shrank from 1,686 to 556 lines. No library implementation change was required for this consumer migration.

The demo now passes 100 unit tests, including projection immutability in both profiles, registered callbacks, malformed payloads, incomplete native inputs, scoped animation expiry, stale video completion, failed slot reservation and complete native-carousel resource binding. Builds/tests passed in the staging checkout and the actual dui-demo repository; no test was skipped.

All twelve real-client scenarios passed again: 768 steps and 226 screenshots. The final Showcase rerun follows additional missing-input guards; Warps was corrected and rerun after a carrier/resource ID mismatch, with a portable regression check added. The tested final plugin matches the actual repository's built JAR byte for byte. Layouts, assets, pack ZIP and shader codec did not change in this migration.

Slot checks verify exactly-once settlement after close, finite payout particles, real lever clicks, sequential reels, template reload without a pack rebuild and focus suppression. Protocol Lab still renders 14 effects in six bodies, with a consumer-owned effect, native/procedural motion, zero changed pixels in still mode and popup coverage. Its latest samples were 1,801 changed card pixels and 654 native-model pixels; 27.75 FPS under a 30-FPS cap and 39.73 ms initial adapter display are local observations, not performance guarantees.

Current reports and galleries are copied to dui-demo/build/reports/e2e/<scenario>, with an aggregate controller-migration.json. Existing reports are retained under the demo's ignored .cache/review-backups directory. The source changes remain uncommitted for user review.
