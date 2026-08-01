# R23 Stable Candidate Preparation

> **Superseded status:** [R24 Stable Gate Closeout](r24-stable-gate-closeout.md)
> records the accepted exact-main evidence and completes all pre-publication
> gates. This document remains the historical candidate-preparation record.

## Status

- Base commit: `d3ff0e59d919b79a03a8ab23123539993ac77d63`
- Version transition: `0.1.0-rc.1 -> 0.1.0`
- Default classification: `internal_stable_candidate`
- Latest public release: `0.1.0-rc.1`
- Stable release: not published

This preparation creates the first unpublished internal `0.1.0` Stable source
candidate. It does not create `v0.1.0` or a GitHub Release.

## Change Boundary

The transition changes no runtime behavior, dependency, asset, registry ID,
persistence schema, recipe, or balance value. The supported gameplay scope is
identical to the accepted RC.

## Candidate Evidence Boundary

The Build workflow produces a temporary internal
`frontier_protocol-0.1.0-release-candidate` bundle. This preparation run can
validate candidate assembly, but retained exact-main Build evidence is a later
step after merge.

English and Japanese graphical smoke, final gate closeout, tag creation, and
Stable publication are also later, separately authorized steps. No incomplete
Stable gate becomes complete from candidate preparation alone.
