# Project verification runtime — issue #79

This is a provisioning candidate. `VERIFICATION_SETUP` remains `false`.
Fixture tests and a native preflight do not certify the official Linux verifier.

## Contract and source boundary

[SDD-0014 §6.4](../../../specs/SDD-0014-deployment-operations-and-release.md#64-project-verification-runtime-repair--issue-79)
defines the VFY runtime, persisted-fixture and browser-sandbox requirements.
The project owns `.github/actions/copilot-verification-setup/` and its assets.
Copilot implementation, generated workflows, protection, lifecycle and configured
verification commands remain unchanged.

External Action references use official major tags: `actions/setup-java@v5`,
`android-actions/setup-android@v3` and `browser-actions/setup-chrome@v2`.
Do not replace these tags with commit SHAs. Resolved source hashes may be recorded
separately as evidence; they are not Action version selectors.

The candidate provisions Java 17, official `platforms;android-37.0`, build tools
36.0.0 and Chrome 155.0.8059.84. It verifies actual Java/browser versions, rejects
an empty or legacy-only SDK, and requires existing local Docker access. It does
not start a daemon, grant Docker permissions or change a host credential store.
The private `make` adapter preserves literal arguments and real exit status.

## Verification performed on 2026-10-26

| Check | Observed result | Scope |
|---|---|---|
| Runtime tests on macOS ARM64 | 30 pass | Executed fixtures for prerequisites, private output, argument/exit propagation and credential removal |
| Updated runtime and sandbox controls on macOS ARM64 and native hosted Linux | 39 pass on each | Adds helper metadata/output rejection and literal sandbox-environment propagation; actual Linux browser startup remains a separate gate |
| Same tests in Linux container | 30 pass | Existing Python image, network disabled, read-only action source, temporary socket fixtures; no host Docker socket mounted |
| Real native preflight | Pass | Corretto 17.0.9, actual Android 37.0 JAR, actual Chrome 155.0.8059.84, existing local Docker and real GNU Make 3.81 |
| Existing project script controls | 43 pass | Includes actual loopback TLS rejection; not product test coverage |
| Specification catalog | 16 capabilities pass | Format/catalog checks |
| Workflow syntax and whitespace | Pass | Actionlint for the consumer CI; `git diff --check` |
| Action reference inventory | No commit-SHA `uses:` found | All 32 checked GitHub YAML files; official v5/v3/v2 tags exist |
| Complete hosted backend at `6f4cb15` | 1978 pass; zero failures/errors/skips | Fresh PostgreSQL-backed and other backend suites across every applicable root task |
| Complete hosted desktop at `6f4cb15` | 1315 pass; zero failures/errors/skips | Complete root desktop selection on Linux |
| Complete hosted Wasm at `6f4cb15` | 1191 pass; zero failures/errors/skips | Real Chrome155 with sandbox; complete root browser selection |

The initial sandboxed fixture run could not create its temporary sockets; the
subsequent permitted execution passed. No assertion was disabled to obtain that
result. The real native preflight executed `make --version`, not project suites.
The separate complete product campaigns recorded for #80 must not be attributed
to this setup candidate.

Actual Linux composite execution installed Temurin 17.0.21+9, SDK37.0, build
tools36.0.0 and Chrome155.0.8059.84. It exposed a real preflight defect: the official
browser prints a trailing space before its newline (`...155.0.8059.84 \n`). The
original exact-string comparison rejected it with78. The captured process exited0
with empty stderr. One regression failed among30 controls before the fix; all30
pass afterward on macOS and Linux. The corrected comparison permits a trailing
whitespace terminator while retaining complete identity/version validation and
rejecting non-whitespace additions, unknown identities and version suffixes.

The actual `setup-java@v4` run also reported its own deprecation; this candidate
uses the official supported major `@v5`, whose Node24 runtime is compatible with
the installed runner. Its project verification remains subject to the real Linux
campaign; fixture tests do not execute provider JavaScript.

Two emulated Linux attempts were interrupted when OrbStack stopped. Its host
log identifies `StorageFull` / `No space left on device`, followed by block I/O
errors. The corrected composite passed actual provisioning before the later
interruption; complete project suites were not verified. The20 partial XML files
and clean source diff were retained. Only owned emulation resources and the now
accepted #80 build outputs were retired, with all1558 tracked source hashes
unchanged and1734 evidence files preserved in a verified archive. Current GUI
packages and rollback artifacts remain available. These are infrastructure
failures, not successful complete campaigns.

The ordinary `Project runtime verification` workflow exercises the candidate
setup on hosted Ubuntu24.04, independently of Copilot activation. It requires
12GiB of initial storage headroom, uses a private job-owned HOME, runs the four
configured commands and complete backend/desktop/Wasm selections, checks source
integrity and uploads raw reports even when a command fails. Supplementary
Gradle invocations explicitly receive the selected browser. The private profile
is under [runner.temp](https://docs.github.com/en/actions/reference/workflows-and-actions/contexts#runner-context),
which GitHub clears at job boundaries when the runner owns the files; setup post
steps therefore finish before the runner retires the profile. This hosted job
does not certify the self-hosted trusted-main Copilot protocol.

The first complete hosted campaign at `5b5e445`,
[run40331134114](https://github.com/vypdev/vaultstadio/actions/runs/40331134114),
successfully provisioned the actual toolchain and ran the configured and complete
suite selections. Its fresh reports contain1976 backend tests with56 failures and
1315 desktop tests with zero failures. All56 backend failures compare identical
complete storage items except for nanosecond input versus persisted microsecond
timestamps. PostgreSQL16 documents
[microsecond timestamp resolution](https://www.postgresql.org/docs/16/datatype-datetime.html).
The fixture now reads the full persisted baseline after create/update; every
existing full-row authorization/deletion assertion is retained. Two additional
real-PostgreSQL cases use explicit `.123456789Z` inputs and retain exact equality
of every persisted field, plus the byte-backed content checks. The corrected
complete hosted campaign described below passes both cases and the existing
unchanged-state assertions.

The same run's complete Wasm selection failed before browser test cases ran:
Chrome reported `No usable sandbox` on Ubuntu24.04. No Wasm case count is claimed
for that run. Following Chromium's
[documented sandbox-helper option](https://chromium.googlesource.com/chromium/src/+/main/docs/security/apparmor-userns-restrictions.md),
the candidate selects only the operator-provisioned
`/opt/vaultstadio-verification-sandbox/chrome-sandbox`,
checks root ownership, regular-file type, setuid/executable mode and absence of
group/world write access, then forwards `CHROME_DEVEL_SANDBOX` through the private
command adapter. The composite does not install a setuid file or change host security policy.
The native workflow requires actual sandboxed headless DOM rendering before the
complete browser suites. Actual helper availability, startup and all1191 Wasm
cases pass in the corrected hosted campaign described below.

At `2976e86`, the next hosted run rejected the existing helper because its mode
allowed group/world writes. This is a failed prerequisite, not an application
test result. At `1617e81`, reinstalling the system package still left that helper
with mode777, so validation rejected it again. The ordinary hosted workflow now
downloads `google-chrome-stable` through a dedicated authenticated apt source into a
private root-owned directory, extracts it there and installs only the protected
package helper into a fresh root-owned namespace with mode4755. It records the
package version and helper metadata/digest and retires the private extraction.
No existing writable helper is promoted. The composite still requires an already
correct operator-provisioned helper, changes no host permissions/security policy
and fails when that prerequisite is absent or unsafe. This does not install or
alter Chrome on the developer's machine or an official self-hosted runner. An
operator must provision the documented helper before future official activation.

The base image has no configured Google download source, so the hosted operator
script prepares a source-scoped keyring using Google's
[published active primary fingerprint](https://www.google.com/linuxrepositories/),
`EB4C1BFD4F042F6DDDCCEC917721F63BD38B4796`. HTTPS remains verified. The key bundle
must contain only that primary and its subkeys; GPG reads/dearmors public material
in an isolated temporary home without importing into a user keyring. Apt uses a
dedicated `Signed-By` source and fresh lists with update errors fatal. No global
trusted keyring or unauthenticated-download option is used.

An actual isolated Ubuntu24.04 container executed this exact operator script,
verified the signed index and downloaded the real Google package156.0.8078.47-1.
It installed the helper with owner0/group0/mode4755. A second invocation returned78
without replacing the namespace or changing the helper digest. The container was
removed afterward; it had no Docker socket or application data mounted. This
proves package provisioning and the replacement guard, not Chrome155 startup or
the complete native product campaign. The helper's protocol compatibility is
still checked by actual sandboxed Chrome155 startup in hosted CI.

A separate real-GPG negative control requested an unapproved primary fingerprint
against the actual vendor key. It returned78 with the expected diagnostic before
creating any installation namespace. Both disposable Ubuntu containers were
removed after their checks; neither changed the developer's keyring or daemon.

## Complete corrected hosted campaign

[Run40340751805](https://github.com/vypdev/vaultstadio/actions/runs/40340751805),
job121634472890, completed successfully at source
`6f4cb15f386f40852c03705940789037de568247`. The actual provenance records Temurin
17.0.20.1+1 and Chrome for Testing155.0.8059.84 on Linuxx86_64. Signed operator
provisioning, all39 runtime controls, the candidate composite, actual sandboxed
DOM rendering, all four configured commands, all three complete root suite
selections, unchanged tracked source and native post steps passed.

I downloaded the original artifact12659773146, verified its SHA256
`6e05b74849f0707d7b6eaaf9627386c60cf6c1668e2a55e1b728fe465488e427`,
checked its source provenance and parsed all1026 fresh JUnit XML files. The table
above reports each platform separately; these are4484 executions across three
targets, not4484 distinct cross-platform behaviors or a whole-product coverage
percentage. Raw XML, provenance, DOM smoke and the complete original log are
retained in the audit evidence. This resolves the observed56 precision failures
and the Ubuntu sandbox startup failure without dropping assertions or suites.

The ordinary application CI at that revision found one new test display name
over120 characters. A follow-up shortens only that name. Its backend ktlint check
and both real-PostgreSQL precision regressions pass locally. The final follow-up
revision still requires its own application CI and complete hosted campaign;
inspect PR83 and the issue79 checkpoint for current-revision results. No completed
run at an earlier source substitutes for those final checks.

Reproduce the deterministic controls from the repository root:

```sh
python3 -m unittest discover -s .github/actions/copilot-verification-setup/tests -p 'test_*.py' -v
python3 -m unittest discover -s scripts/tests -p 'test_*.py'
python3 scripts/validate-specification-catalog.py
git diff --check
```

## Outstanding operational gates

1. Obtain review of the actual setup and green application/complete hosted CI on
   the final follow-up revision. Run40340751805 establishes the complete corrected
   Linux campaign at6f4cb15; current-revision checks remain mandatory.
2. Merge the reviewed extension and compatible consumer prerequisites onto the
   configured trusted main (`master`). The four installed generated callers
   already expose `verification-setup` and remain unchanged. Provision the
   documented operator sandbox prerequisite on the official Linux runners before
   activation; the successful hosted job does not provision a different runner.
3. Obtain separate explicit maintainer authorization to enable the repository
   variable `VERIFICATION_SETUP=true`. A merge or PR review does not authorize
   activation.
4. Run the official `/copilot sync-branch` on an admitted issue. Retain immutable
   setup source, actual tool versions, all configured command results, private
   profile cleanup, resulting refs and unchanged protection/lifecycle evidence.
   Missing Docker access remains an operator prerequisite, not a bypass target.

The last official attempt, [40318170210](https://github.com/vypdev/vaultstadio/actions/runs/40318170210),
failed because Java was unavailable and published no branch synchronization.
The local reconciliation preserves both branch79 `bb2bfa1` and develop `25a2429`;
it is not a remote recovery authorization.

Existing Makefile selection and failure-propagation debt remains R04 in the
[audit roadmap](../AUDIT_ROADMAP_2026-10-25.md). In particular, the configured
`make test` does not currently select all backend modules and the frontend target
suppresses a Wasm failure. A zero exit from that target cannot certify the complete
product; preserve diagnostics and run the relevant suites explicitly until R04
is corrected through its own hito. This candidate does not weaken or rewrite the
configured verification command list.
