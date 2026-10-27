# Upstream timing collector inspection

Inspected upstream HEAD on 2026-10-27, commit
`4e91647c30b820c8a4d799522e67a1c6b618744e` (changelog for 0.5.253-ALPHA).
Both collection files are byte-identical to the pinned Maven artifact. Thus
this upstream revision does not correct the diagnosed zero-duration failure.
No dependency or measurement code was changed.

## Verification

```sh
git ls-remote https://github.com/hugoduncan/criterium.git HEAD
git clone --depth 1 https://github.com/hugoduncan/criterium.git /tmp/criterium-screen
git -C /tmp/criterium-screen rev-parse HEAD
for f in collect.clj collect_plan.clj; do
  unzip -p "$HOME/.m2/repository/org/hugoduncan/criterium/0.5.251-ALPHA/criterium-0.5.251-ALPHA.jar" "criterium/$f" |
    cmp - "/tmp/criterium-screen/bases/criterium/src/criterium/$f"
  shasum -a 256 "/tmp/criterium-screen/bases/criterium/src/criterium/$f"
done
```

Both `cmp` exits were 0. SHA-256 values match `result.edn`. Inspection used
an external temporary clone, not a project dependency. Future reproduction
must check out the commit above if HEAD has moved. No test or timing run was
needed to establish byte equality; the prior deterministic diagnostic applies
to these exact source bytes.

## Configuration and extension boundary

At this commit, `bases/criterium/src/criterium/collect_plan/config.clj:34–59`
lists the built-in plans and options. The warmed plan permits sample counts,
time limits, batch time, GC attempts, and thread priority. It has no option for
an initial duration floor or a supplied estimator. Changing sample counts or
batch time does not bypass the initial division.

`collect_plan/impl.clj:5–12` exposes multimethod dispatch for required stages
and collection. `collect_plan/config.clj:34` also dispatches configuration.
These are extension mechanisms, not an existing corrected collector. A new
plan would own the warmup, sampling, and output contracts; it must not be
labelled an upstream fix. Replacing global methods or rebinding the estimator
would conceal the original failure and is not selected.

Next evaluate a minimal, separately pinned upstream-source correction for
**both** initial and subsequent estimation boundaries. Verify deterministic
zero-duration behavior and positive-duration behavior before the original six
case smoke run. Do not upgrade the project dependency until that run passes.
A local candidate must remain explicitly distinct from a released artifact;
retain its diff and source revision with diagnostic evidence. No acceptance
claim follows from this inspection.
