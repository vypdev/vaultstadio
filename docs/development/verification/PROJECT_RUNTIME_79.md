# Project verification runtime — issue #79

This is a provisioning candidate. `VERIFICATION_SETUP` remains `false`.
Fixture tests and a native preflight do not certify the official Linux verifier.

## Contract and source boundary

[SDD-0014 §6.4](../../../specs/SDD-0014-deployment-operations-and-release.md#64-project-verification-runtime-repair--issue-79)
defines VFY-JAVA/TOOLS/DOCKER/ENV/FAIL/TRUST/LIFE/ACTION-01.
The project owns `.github/actions/copilot-verification-setup/` and its assets.
Copilot implementation, generated workflows, protection, lifecycle and configured
verification commands remain unchanged.

External Action references use official major tags: `actions/setup-java@v4`,
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
| Runtime tests on macOS ARM64 | 25 pass | Executed fixtures for prerequisites, private output, argument/exit propagation and credential removal |
| Same tests in Linux container | 25 pass | Existing Python image, network disabled, read-only action source, temporary socket fixtures; no host Docker socket mounted |
| Real native preflight | Pass | Corretto 17.0.9, actual Android 37.0 JAR, actual Chrome 155.0.8059.84, existing local Docker and real GNU Make 3.81 |
| Existing project script controls | 43 pass | Includes actual loopback TLS rejection; not product test coverage |
| Specification catalog | 16 capabilities pass | Format/catalog checks |
| Workflow syntax and whitespace | Pass | Actionlint for the consumer CI; `git diff --check` |
| Action reference inventory | No commit-SHA `uses:` found | All 31 checked GitHub YAML files; official v4/v3/v2 tags exist |

The initial sandboxed fixture run could not create its temporary sockets; the
subsequent permitted execution passed. No assertion was disabled to obtain that
result. The real native preflight executed `make --version`, not project suites.
The separate complete product campaigns recorded for #80 must not be attributed
to this setup candidate.

Reproduce the deterministic controls from the repository root:

```sh
python3 -m unittest discover -s .github/actions/copilot-verification-setup/tests -p 'test_*.py' -v
python3 -m unittest discover -s scripts/tests -p 'test_*.py'
python3 scripts/validate-specification-catalog.py
git diff --check
```

## Outstanding operational gates

1. Obtain review of the actual setup and retain real backend/frontend execution
   under its prepared environment. Linux package installation and the exact
   browser returned by the Action remain unverified by the fixture campaign.
   A browser version check alone does not prove its UI contracts pass.
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
