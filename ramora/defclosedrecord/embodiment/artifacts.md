# Artifacts

| Artifact | Responsibility |
|---|---|
| `src/closed_record/core.cljc` | Public API: the `defclosedrecord` macro and `check`. |
| `src/closed_record/runtime.cljc` | Internal protocol `ClosedRecord` (D-17), class-name helper, and public run-time helper `key-error`, which the generated code calls (D-21). No dependency on the public API or emitter. |
| `src/closed_record/impl/jvm.clj` | JVM emitter. Builds the `deftype*` form and the factories. |
| `test/runtime/closed_record/*_test.clj` | Behaviour, differential, equality, malli, nil, literal and constant tests. |
| `test/runtime/closed_record/compiler_rules_test.clj` | Tests that fix K-06 to K-08 (V-13). |
| `test/runtime/closed_record/hidden_key_test.clj` | Tests that fix hidden keys of a literal and their retention (V-14). |
| `test/runtime/closed_record/property_test.clj` | Property test of random `assoc` and `dissoc` sequences (V-03). |
| `test/runtime/closed_record/differential_test.clj` | Differential property test against `defrecord` after random permitted steps (V-02). |
| `test/runtime/closed_record/malli_test.clj` | Tests of malli map schemas and transformers on a closed record (V-07). |
| `test/runtime/closed_record/bench_report_test.clj` | Tests of the bench pure layer (`closed-record.bench.report`): per-call stats, pass rule, table and exit code (V-11). No benchmark run. |
| `test/runtime/closed_record/bench_fork_test.clj` | Tests of the bench fork layer (`closed-record.bench.fork`): fork failure causes and parent exit codes with short shell commands in place of child JVMs, the fork numbers of a full run (0 to 4, in start order), the child command against the state of the test JVM, and the run order of each fork (V-11). No benchmark run. |
| `dev/bench/` | `closed_record/bench.clj` (`closed-record.bench`): criterium benchmarks of the six S-03 operations against `defrecord` (V-11), over 5 child JVM forks. The child entry point measures and writes its result to a file. `closed_record/bench/fork.clj` (`closed-record.bench.fork`): the run order of a fork, the child command and the parent, which starts the forks, without criterium. `closed_record/bench/report.clj` (`closed-record.bench.report`): the pure layer (per-call stats, encode and decode of the child result, statistics over forks, pass rule, table, exit code), without criterium. |
| `build.clj` | Maintainer build: validates the Git version, clears stale output, and packages runtime source, Maven metadata, and the license in one source-only library JAR. `clean` removes build output without Git metadata. |
| `bb.edn` | Shared formatting, dependency lint setup, lint, Vale, API generation/freshness, and staged-content hook tasks; `lychee:check` invokes the link-check helper. |
| `dev/link_check.clj` | Selects Git-tracked Markdown and runs local fragment and online link checks with lychee. Both passes run; either failure fails the task. |
| `lychee.toml` | Shared local and CI link policy: verified TLS, bounded requests and retries, successful HTTP statuses, and no cache or report writes. |
| `dev/link_check_test.clj` | Real-process contract tests with temporary Git repositories and a loopback HTTP server. Checks selection, links, diagnostics, failure propagation, and unchanged checkout files. |
| `.github/workflows/ci.yml` | Read-only CI orchestration: pinned tool setup, shared formatting and lint, Vale, API freshness, focused tooling tests, three alias-based runtime test entries, separate full-history build verification, structured test-failure uploads, and a separate Markdown links job that runs contract fixtures then `bb lychee:check`. No benchmarks or publication. |
| `LICENSE` | Complete EPL-2.0 license text for the repository and the JAR root. |
| `test/build/build_test.clj` | Separate build tests: version validation, missing-Git failure, repeatable clean, artifact contents and metadata, stale-output removal, and an isolated consumer JVM. Uses real processes and temporary project fixtures. |
| `deps.edn` | Clojure 1.11 runtime dependency. Maintainer aliases `:build` and `:build-test` supply tools.build; `:build` selects the `build` namespace, and `:build-test` adds the repository root and `test/build` paths for use with `:test`. The `:quickdoc` alias supplies Quickdoc and `dev/quality` for API generation. The `:tooling-test` alias supplies scry and the `dev/quality` and `test/tooling` paths for quality-tool tests. Other aliases `:test`, `:bench`, `:nrepl`, `:clj-1.12` and `:clj-1.13`. The `:test` alias has scry, `test.check` and malli (`metosin/malli`), and the path `dev/bench` for the bench report and fork layer tests. The `:bench` alias has criterium (`org.hugoduncan/criterium` 0.5.251-ALPHA). |

Quality and link tooling stay outside runtime dependencies. See
[development quality architecture](../../development-quality.md) for helper,
configuration, generated-file, and hook ownership. See
[Markdown link instructions](../../../doc/development.md#markdown-links) for
pinned installation and commands.
