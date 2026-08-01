# R24 Stable Gate Closeout

## Accepted Evidence

- Base commit: `4d81b237af7f1117caa00bc46c0a53512c9956a4`
- [Issue #33](https://github.com/upiscium/FrontierProtocol/issues/33)
- Original evidence comment: `5144235666`
- Focused graphical supplement: `5148923492`
- Independent acceptance: `5148981054`
- Build run: `30619876967`
- Job: `91121675163`
- Artifact: `8789117654`
- Artifact ZIP SHA-256: `a6e8c841462dc466287cc97aa1a4aee935ceba26528dc7ddcaa772ee42752217`
- JAR SHA-256: `719e58ae01e45b290f5e948870a7993ba6ab8cdb4e04afc3b605be2d0d1d5902`

The accepted candidate contains exactly:

```text
LICENSE
frontier_protocol-0.1.0.jar
frontier_protocol-0.1.0.jar.sha256
release-manifest.json
```

Its accepted manifest records:

```text
mod.version: 0.1.0
source.git_commit: 4d81b237af7f1117caa00bc46c0a53512c9956a4
release_status: internal_stable_candidate
```

Focused retained log hashes:

```text
English client: 042054f427e7a6f4a2bd77bb060b9b2368c5f6a37122547b55dae76e754033fb
English Gradle: 73c5d3a284db3f0fb6a43ed7cf919b4f9adb0c5cdd00695c159ed2b6dd497d4c
Japanese client: 71630910c16ca2e80b6f779253aeab8463983124ca1b6c65b64479744434c842
Japanese Gradle: 31c97a802e595235d2ac174984659d02ae45618f932e2af4ffbd077893ecce1c
```

The independently accepted verdict is:

```text
automated exact-main evidence: PASS
English graphical smoke: PASS
Japanese graphical smoke: PASS
overall evidence package: PASS
```

Dependency and environment warnings disclosed in Issue #33 are non-blocking and
dependency-originated. Frontier Protocol ERROR/FATAL and Mixin apply-error counts
are zero.

## Candidate and License Verification

Artifact inspection confirmed the root BSD-3-Clause `LICENSE`, the retained
Create notice, and the retained NeoForge MDK notice. The JAR contains no nested
JAR and no bundled dependency classes.

The closeout branch was verified with both the default
`internal_stable_candidate` candidate and a `public_stable_release` dry-run. The
branch candidate JAR is byte-identical to the accepted exact-main JAR and retains
SHA-256 `719e58ae01e45b290f5e948870a7993ba6ab8cdb4e04afc3b605be2d0d1d5902`.
The public-status manifest changes only release provenance: it records version
`0.1.0`, status `public_stable_release`, and the closeout branch `HEAD` as
`source.git_commit`.

This closeout changes no runtime behavior, dependency or platform version,
production asset, resource, recipe, Mixin, registry ID, persistence schema,
balance default, `mod_version`, or workflow.

## Publication Boundary

All pre-publication Stable gates are complete. Creating tag `v0.1.0`, running the
generic tag-driven publication workflow, creating the Stable GitHub Release and
its four assets, and recording post-publication verification remain separate
operations. This closeout does not perform or claim those operations.
