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
| Updated runtime and sandbox controls on macOS ARM64 | 39 pass | Adds helper metadata/output rejection and literal sandbox-environment propagation; actual Linux browser startup remains a separate gate |
| Same tests in Linux container | 30 pass | Existing Python image, network disabled, read-only action source, temporary socket fixtures; no host Docker socket mounted |
| Real native preflight | Pass | Corretto 17.0.9, actual Android 37.0 JAR, actual Chrome 155.0.8059.84, existing local Docker and real GNU Make 3.81 |
| Existing project script controls | 43 pass | Includes actual loopback TLS rejection; not product test coverage |
| Specification catalog | 16 capabilities pass | Format/catalog checks |
| Workflow syntax and whitespace | Pass | Actionlint for the consumer CI; `git diff --check` |
| Action reference inventory | No commit-SHA `uses:` found | All 32 checked GitHub YAML files; official v5/v3/v2 tags exist |

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
of every persisted field, plus the byte-backed content checks. Their execution
and the corrected complete backend campaign remain pending.

The same run's complete Wasm selection failed before browser test cases ran:
Chrome reported `No usable sandbox` on Ubuntu24.04. No Wasm case count is claimed
for that run. Following Chromium's
[documented sandbox-helper option](https://chromium.googlesource.com/chromium/src/+/main/docs/security/apparmor-userns-restrictions.md),
the candidate selects only the existing `/opt/google/chrome/chrome-sandbox`,
checks root ownership, regular-file type, setuid/executable mode and absence of
group/world write access, then forwards `CHROME_DEVEL_SANDBOX` through the private
command adapter. It does not install a setuid file or change host security policy.
The native workflow requires actual sandboxed headless DOM rendering before the
complete browser suites. Actual helper availability, startup and Wasm results
remain pending in the corrected campaign.

At `2976e86`, the next hosted run rejected the existing helper because its mode
allowed group/world writes. This is a failed prerequisite, not an application
test result. The ordinary hosted workflow now reinstalls `google-chrome-stable`
through apt's existing signed repository before executing candidate scripts,
and records its package version and helper metadata/digest. It does not elevate
a writable helper with chmod/chown. The composite still requires an already
correct operator-provisioned helper, changes no host permissions/security policy
and fails when that prerequisite is absent or unsafe. This does not install or
alter Chrome on the developer's machine or an official self-hosted runner.

Reproduce the deterministic controls from the repository root:

```sh
python3 -m unittest discover -s .github/actions/copilot-verification-setup/tests -p 'test_*.py' -v
python3 -m unittest discover -s scripts/tests -p 'test_*.py'
python3 scripts/validate-specification-catalog.py
git diff --check
```

## Outstanding operational gates

1. Obtain review of the actual setup and a successful complete corrected Linux
   campaign. Actual installation is verified by run40331134114, but its backend
   fixture and browser startup failures block readiness. Retain actual sandboxed
   startup and fresh backend/desktop/Wasm reports; fixture controls alone cannot
   certify those application suites.
2. Merge the reviewed extension and compatible consumer prerequisites onto the
   configured trusted main (`master`). Verify the installed generated workflows
   support the trusted setup input; use the supported installer for any required
   workflow update. Do not hand-edit generated Copilot files.
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
