# Secret scanning verification — issue 87

Contract: [SDD-0014 §6.5](../../../specs/SDD-0014-deployment-operations-and-release.md), issue [87](https://github.com/vypdev/vaultstadio/issues/87).

## Observed failure

On `9f21f1e`, Security run `40353446268`, job `121679433586`, reported success although TruffleHog failed with missing Docker (127) and the Gitleaks organization Action aborted without a license. Both steps allowed errors. The new standalone `Secret scanning` workflow replaces that job and runs on same-repository PRs/pushes to develop/master/main and weekly schedules. It uses an ephemeral Ubuntu 24.04 runner, read-only repository permission, exact source checkout and no persisted GitHub credentials. It does not change Copilot or branch protection.

The actual Gitleaks 8.30.1 binary also returned zero with **zero scanned commits** after Apple Git emitted an internal error under local process isolation. The adapter rejects internal error logs, missing scan-completion evidence and zero commits; a real controlled Git failure reproduces this behavior. An exit code alone is insufficient evidence.

## Tool identity and trust

- Gitleaks 8.30.1: [official release](https://github.com/gitleaks/gitleaks/releases/tag/v8.30.1).
- TruffleHog 3.99.7: [official release](https://github.com/trufflesecurity/trufflehog/releases/tag/v3.99.7).
- [`secret_tools.json`](../../../scripts/ci/secret_tools.json) records reviewed release-asset SHA256 digests for Linux/macOS arm64/x86_64. These were compared to upstream release metadata. They are integrity pins, not a claim of an independently verified publisher signature.
- [`install_secret_tools.py`](../../../scripts/ci/install_secret_tools.py) downloads only HTTPS, bounds redirects/size/time, verifies the digest before reading the archive and extracts only the one regular executable. It refuses unsupported hosts, links, duplicate/missing binaries, unsafe/nonempty output and replacement of existing files. Failure retires partial installation and private downloads.

The CLI requires Python 3.11+ and Git/curl; CI selects Python 3.13 through `actions/setup-python@v6`. No Docker, organization license or global software installation is required. Action references use major tags.

## Scope and privacy

Both scanners inspect **all commits reachable from the checked-out HEAD**, including subsequently deleted content; they ignore unrelated refs. Source identity and complete, non-shallow history are preconditions. The actual immutable source and tool identities accompany each summary.

Both upstream ordinary Git scanners missed a synthetic secret introduced only during a merge and deleted later. [`secret_history.py`](../../../scripts/ci/secret_history.py) therefore inventories the reachable blobs and each merge's parents, identifies content absent from ordinary ancestor diffs and supplies those exact blobs to both filesystem scanners. It uses ancestry at the original merge: a later rename or mode change cannot hide the earlier content. Only validated object IDs become private regular filenames; repository names/symlinks are never materialized as paths. Limits are 100,000 reachable objects, 64MiB per supplemental blob and 512MiB total; exceeding a budget fails visibly. Raw materialization is retired with the scan's private state.

This covers tracked blob contents, including merge-only history. It does not certify credential stores, submodule repositories or commit-message/filename content. Binary/encoding detector limitations remain those of the pinned upstream tools.

Gitleaks uses its built-in rules with an explicit default configuration; repository ignore files and inline allow comments cannot disable detection. TruffleHog uses `--no-verification`, all result classes, no ignore tags, no update check and failure-on-scan-errors. Suspected credentials are not authenticated against external services. A match is not proof of a live credential, and a clean scan is not proof of absence of every secret or dependency vulnerability.

Detector subprocesses receive a private HOME/TMPDIR and a narrow environment without inherited credentials or scanner/Git configuration. Each process group has a timeout. Raw output remains in private temporary state and is retired on success/failure; it is never printed or uploaded. The artifact contains only source/tool identities, outcome/counts and stable safe error categories. It intentionally omits raw matches, snippets, provider responses and paths. Maintainers investigating a new finding must use a private, redacted local investigation rather than publish raw scanner output.

The old dependency job lists Gradle dependencies and suppresses failures. That is advisory dependency inventory, not a vulnerability gate; this issue does not certify or repair it. CodeQL/container analysis and full product verification retain their distinct scopes.

## Reviewed historical non-secrets

The complete native parent scan found **13 Gitleaks + 19 TruffleHog matches**. The 32 matches occupy 31 immutable coordinates; two JDBC detections share one historical line. They are explicitly retained as reviewed non-secret counts, not reported as zero detections. Earlier results under broken Apple Git isolation were incomplete and are not accepted as the baseline.

| Matches | Evidence and reason |
|---|---|
| 18 TruffleHog JDBC | Database endpoints with no username, password or query parameters, including local/test/example configuration. |
| 1 TruffleHog Postgres | Documented user/password/host placeholders in SECURITY.md. |
| 1 Gitleaks | Public `productionAuthScreenSha256` build evidence digest. |
| 1 Gitleaks | `CURSOR_API_KEY` shell variable reference in generated credential-health YAML; no embedded value. The generated file is unchanged. |
| 1 Gitleaks | SDD prose listing credential/security responsibilities. |
| 1 Gitleaks | Static logging-test sentinel only compared with fabricated masked strings; no authentication operation. |
| 3 Gitleaks | Static S3 `storageKey` object identifiers in adapter tests; not AWS authentication keys. |
| 1 Gitleaks | Configuration guide's explicit example/your placeholder. |
| 3 Gitleaks | API response JWT examples truncated with an ellipsis. |
| 2 Gitleaks | Static twelve-character share-token examples in illustrative API JSON. |

[`reviewed_nonsecrets.json`](../../../scripts/ci/reviewed_nonsecrets.json) records exact tool/version/rule/commit/path/line decisions and reasons. No rule, directory, filename glob or current/future commit is globally excluded. The same content in another commit remains blocking. Changing tool version requires a new review. The public summary distinguishes `totalMatches`, `reviewedNonSecrets` and unreviewed `findings`; only an otherwise successful scan with zero unreviewed findings may pass. An execution error is never accepted by a historical decision.

## Verification inventory

The issue's sixteen risk cases expand into installer integrity/platform/archive/cleanup controls, credential-free process/timeout/error controls, source/history/report/privacy/continuation cases, exact historical-decision controls and real upstream binary/Git fixtures. The real suite requires both executables and errors if absent; it does not silently skip.

Real controls include a clean history, a generated detector-shaped marker, a marker deleted in a later commit, malformed scanner configuration, complete entrypoint failure/privacy, wrong source identity and Gitleaks' real Git-error/zero-scan case. TruffleHog's additional synthetic custom detector is local and offline; it validates the production adapter's result handling without pretending to authenticate a real provider credential. No synthetic marker is committed into the product repository.

The local inventory expands to 37 unittest methods (including twelve real binary/history controls and additional helper/installer cases; subcases are not counted as independent product behaviors). It includes merge-only additions followed by deletion and by rename/deletion, plus real small-budget rejection. On parent `9f21f1e`, the full scan inventoried 10,266 objects / 4,021 blobs / 26 merges and checked 15 supplemental blobs (211,597 bytes) with both CLIs: 32 reviewed historical non-secrets, zero unreviewed matches and no scanner errors. This is parent/source-specific evidence; the final committed candidate still requires its own complete scan and CI.

```sh
scanner_state=$(mktemp -d)
mkdir -m 700 "$scanner_state/tools"
python3 scripts/ci/install_secret_tools.py "$scanner_state/tools"
PYTHONDONTWRITEBYTECODE=1 SECRET_SCANNER_TOOLS="$scanner_state/tools" \
  python3 -m unittest discover -s scripts/tests -p 'test_secret_*.py' -v
python3 scripts/ci/run_secret_scan.py --repository "$PWD" \
  --tools "$scanner_state/tools" --head "$(git rev-parse HEAD)" \
  --report "$scanner_state/summary.json"
# Retire only this freshly created owned directory after retaining the safe summary.
```

On macOS, restrictive process isolation may prevent Apple Git's developer-tool bootstrap. Use the normal authorized local runtime, or accept the visible failed scan; do not suppress its stderr to manufacture success. CI's ordinary hosted Linux execution independently verifies its own source/toolchain. Local controls are not a substitute for final candidate CI/review.

## Rollout and remaining gates

This changes project verification only, with no data migration or application behavior change. Scanner installation and raw output are ephemeral; cancellation/timeout leaves no application mutation. The workflow validates ownership before retiring its own state. A regression should be corrected through the linked Copilot branch and reviewed PR; do not restore fail-open flags or rewrite historical source to silence detections.

Implementation verification must record exact final commit, executed control count, safe full-history summary, workflow validation and same-source CI before merge. Application/architecture coverage percentages are not inferred from scanner-control counts. Master promotion, official runtime activation and the product findings remain separate gates.
