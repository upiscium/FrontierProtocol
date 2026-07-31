# R21 RC.1 Candidate Preparation

## Status

Source version `0.1.0-rc.1` is prepared as an internal RC candidate. The latest
public version remains `0.1.0-alpha.1`; no RC tag or Release is created here.

The six-status contract distinguishes internal/public Alpha, RC, and Stable
candidates. RC publication metadata is `public_rc_prerelease` with GitHub
`prerelease=true`, title `Frontier Protocol 0.1.0-rc.1`, notes
`docs/releases/0.1.0-rc.1.md`, and the exact JAR/checksum names derived from the
version. Stable gates run only for Stable classification.

## Tagged Verification

The tag Release workflow retains exact tag/version, main ancestry, unpublished
tag, exact assets, anonymous checksum, byte identity, and manifest checks. RC
and Stable tags rerun `stableStartupMatrix` and
`alpha1WorldUpgradeSmoke --offline` before candidate assembly, retaining only
bounded failure evidence.

## Soak Contract

`releaseCandidateSoak` starts the current packaged production JAR with pinned
runtime dependencies and a fresh committed R20 fixture extraction. Evidence
mode requires at least 120 steady-state minutes after a five-minute warm-up.
It monitors liveness, performs normal shutdown, analyzes bounded post-warm-up
log rate, and directly rechecks seed, Stabilizers, exact items, spawn, cleanup,
and all required region data. Normal config requires debug logging false and
Cell capacities 8/32/64.

The explicit two-minute short mode is CI infrastructure validation only. It
cannot complete `rc-log-volume-soak`, which remains `INCOMPLETE`. Normal-operation
log volume remains `NOT VERIFIED` until the read-only manual workflow is run for
at least 120 minutes against the merged exact RC/final candidate. That workflow
was not run during candidate preparation.

## Publication Boundary

No `v0.1.0-rc.1` or `v0.1.0` tag is created, no GitHub Release is created or
modified, and immutable Alpha assets are untouched. Stable compatibility
guarantees begin only after final `0.1.0` publication.

## Expected Negative Results

- `public_alpha_prerelease` fails because it does not match an RC version.
- `public_stable_release` fails because it does not match an RC version.
- A two-minute soak without `allowShortSoak=true` fails before server launch
  because evidence mode requires at least 120 minutes.
- `verifyStableReleaseGates` fails because final-candidate rows, including
  `rc-log-volume-soak`, remain `INCOMPLETE`.
