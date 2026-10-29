# Web API origin review supplement — issue #116 / PR #126

Latest production review implementation: `398f72d5db0fc029150e71b5276d459668de33b0`.
The [initial complete acceptance](WEB_API_ORIGIN_116.md) and its original failures remain historical checkpoints. This supplement does not substitute local evidence for final-ref CI or protected adoption.

## UTF-8 boundary repair

Copilot finding `TextPreviewLoader.kt:14:utf8-truncation-splits-character` was valid. The previous byte slice could manufacture a replacement character when valid UTF-8 crossed the 100 KiB display boundary. The pre-code refinement was committed at `e3558e1`; 19 real JVM cases then produced seven behavioral failures. The fix retains only complete crossing code points, inspects at most one adjacent sequence, and preserves replacement semantics for malformed source bytes and the existing truncation notice. The full-body domain download behavior remains unchanged.

The 19 shared cases cover all six split positions of two-/three-/four-byte characters; complete boundary-aligned characters and actual U+FFFD; overlong, surrogate, out-of-range, incomplete and missing-continuation source; stray/long continuation runs; and malformed content before a valid crossing sequence. All execute on both JVM and Wasm. Focused acceptance passes 27 JVM and 56 Wasm executions, including the existing text behavior, actual Coil fetch/decode tests and browser clipboard regressions.

Complete frontend coverage verification on the clean implementation passes **1,748 desktop + 1,655 browser = 3,403 executions**, with zero failures/errors/skips and all coverage gates/populations retained. Unfiltered JVM frontend coverage is **8,174/22,468 lines**, **1,896/6,483 branches**, across 52 projects / 1,902 classes. TextPreviewLoader covers **27/27 lines and 32/34 branches**; defensive bounded-scan/compiler edges are not presented as complete branch coverage. Backend production/test/build source is unchanged from the earlier complete campaign; this scoped local rerun does not claim a fresh backend execution at the review commit.

`make lint` passes. Android and iOS simulator-target compilation both execute and pass again. A new production Web distribution is served on both operator profiles; all six non-operator resources on each origin match its manifest byte hashes and the JSON overrides are correct. Actual B startup/files/shares, copied clipboard equality and a saved 611-byte prefixed authenticated download pass again. This is package regression evidence; the known file-screen preview no-op still prevents claiming real user-interface UTF-8 preview acceptance.

## Investigated Cache-Control finding

The medium missing-no-store finding is contradicted by the actual transport test and the pinned dependency. Coil 3.6.3's [network request construction](https://github.com/coil-kt/coil/blob/3.6.3/coil-network-core/src/commonMain/kotlin/coil3/network/NetworkFetcher.kt#L190-L214) supplies the no-cache/no-store header when network reads are enabled and disk reads/writes are disabled. The application already selects that disk policy.

No redundant global request-header mutation was added. A maintenance comment identifies the dependency behavior, and the existing real Coil pipeline assertion now checks exact header equality across session rotation and logout. The original reviewed source also passes its complete remote CI: five original artifacts, GitHub digests/ZIP CRC verified, 1,224 raw XML reports independently recounted, **6,762 executions** with zero failures/errors/skips, and the original source tree/module populations verified. This independently refutes the prediction that the existing test fails; it does not certify the subsequent UTF-8 repair.

The finding has an [evidence reply](https://github.com/vypdev/vaultstadio/pull/126#discussion_r4411046964). Per the maintainer's reporting instruction, [vypdev/copilot#784](https://github.com/vypdev/copilot/issues/784) records the false positive, actual action/package version, source, reproducible transport test and suggested dependency-context regression. The consumer Copilot configuration and protections remain unchanged. Fresh final-head review reconciliation remains required.

## Original evidence

| Evidence under `audit/evidence/` | SHA-256 |
| --- | --- |
| `issue116-utf8-red-e3558e1/originals.zip` — 19 actual cases / seven failures; dirty test inputs recorded | `dae5510b27e64cd45a074599c87f7899aa94804eb3f100c4e242dc4dcac5df48` |
| `issue116-review-green-e3558e1/originals.zip` — 27 JVM / 56 Wasm; repair/test input hashes | `8ad90a77383e0386e9b964dc234352231b487cb7c9c9cbf7fa30692766e06f87` |
| `issue116-frontend-review-398f72d/originals.zip` — 2,811 original files; clean complete frontend receipts/classes/execution/coverage | `54bce66c8c799f01a720834d71e3c23072b94bb01b6d54547d6159d9f74b19fb` |

The earlier complete CI originals are retained separately at `pr126-ci-3d095f1-40854037065/`, with each GitHub artifact digest and source-bound coverage receipt. Mobile compilation, lint, production package manifest and repeated runtime assertions remain distinct receipts. No credentials, raw clipboard/capability values or private environment files are published.

The consolidated expected product count is 6,800 after the 19 shared cases add 38 executions. Only a fresh complete final-ref CI campaign can confirm that total on one final source tree. Merge authorization, review reconciliation and post-merge checks remain separate gates.
