# Development

Use the Clojure CLI. Run all commands from the repository root.
See [tool installation](tool-installation.md) for pinned Java, Clojure CLI,
format, lint, and prose tools.

Test suites share the `test/` parent directory:

- `test/runtime/`: library and benchmark support tests.
- `test/build/`: build and artifact tests.
- `test/release/`: release tooling tests.
- `test/tooling/`: development tool tests.
- `test/bench/`: measurement tests with separate dependency and JVM requirements.
- `test/value-type/`: `:value-type true` tests. They need the
  `:value-type-test` alias, Clojure 1.12 and a representation property.
- `test/value-type-clj-1.11/`: the `:value-type true` failure on Clojure
  1.11. It needs the `:value-type-test` alias without `:clj-1.12`.

Each suite has a separate source path. Select the suite directory explicitly;
The default scry discovery scans all of `test/`.

Run the runtime tests on Clojure 1.11:

    clojure -M:test -m scry.cli --dir test/runtime

Run the tests on Clojure 1.12 and on Clojure 1.13:

    clojure -M:test:clj-1.12 -m scry.cli --dir test/runtime
    clojure -M:test:clj-1.13 -m scry.cli --dir test/runtime

Run the value-type tests on Temurin 21 (identity fallback). Put `-J` options
before `-M`:

    clojure -J-Dclosed-record.test.representation=identity -M:test:value-type-test:clj-1.12 -m scry.cli --dir test/value-type

Run them on a Valhalla early-access JDK (value output). `JAVA_CMD` selects the
JDK:

    JAVA_CMD=<Valhalla java> clojure -J--enable-preview -J--add-exports=java.base/jdk.internal.value=ALL-UNNAMED -J-Dclosed-record.test.representation=value -M:test:value-type-test:clj-1.12 -m scry.cli --dir test/value-type

The property `closed-record.test.representation` (`identity` or `value`) is
the expected representation. Without it, the suite fails.

Run the Clojure 1.11 value-type case:

    clojure -M:test:value-type-test -m scry.cli --dir test/value-type-clj-1.11

Start a REPL. The nREPL server writes the port to `.nrepl-port`:

    clojure -M:test:nrepl

Start a REPL for `:value-type true` records on a Valhalla early-access JDK.
The `:enable-preview` alias adds the JVM options `--enable-preview` and
`--add-exports=java.base/jdk.internal.value=ALL-UNNAMED`. `value-type.array`
needs the export:

    JAVA_CMD=<Valhalla java> clojure -M:test:value-type-test:clj-1.12:enable-preview:nrepl

Run the tests in the REPL:

    (require '[scry.core :as scry])
    (scry/run {:dirs ["test/runtime"]})

Run the benchmarks against `defrecord` (about 6 minutes):

    sh dev/bench/timing/run.sh

The launcher prepares a checksum-verified local correction to Criterium
0.5.251-ALPHA before starting the benchmark. It retains the source overlay under
`target/` for child JVMs. The correction prevents a zero planning duration; it
does not change raw samples. Direct `clojure -M:bench` bypasses this preparation
and selects the uncorrected upstream tool.

See [performance measurements](performance.md) for the fixed matrix, memory
metrics, experimental runtime requirements, and saved evidence.

### Continuous integration

[CI](../.github/workflows/ci.yml) runs on pushes to `master` and pull requests
against any branch, including forks. All jobs use Ubuntu and read-only repository permissions.
All Clojure jobs use Clojure CLI `1.12.6.1673`. The Valhalla value-type job
uses the Valhalla early-access JDK `27-jep401ea3+1-1` (Linux x64), with a
SHA-256 check of the archive. The other Clojure jobs use Temurin Java 21.
The separate Markdown links job uses Babashka and lychee, not Java or Clojure CLI.
The [release workflow](releases.md) calls CI, so a CI failure stops a release.

The code-quality job uses cljfmt `0.16.4` and clj-kondo `2026.04.15`.
Run the same non-writing checks locally:

    bb clj-fmt:check
    bb clj-kondo:lint
    bb vale
    bb api-docs --check

See [quality checks](quality-checks.md) for scopes, generated files, focused
tests, and hook installation. The quality job also uses Vale `3.17.1` and
pre-commit `4.6.0`, and runs the focused tooling tests.

Formatting differences, lint warnings, lint errors, prose warnings and errors,
and stale API output fail the job.
The runtime matrix runs the three `test/runtime` commands above; `deps.edn` owns
the Clojure versions. A separate job runs the build tests and JAR command
below once, with full Git history. A separate job runs the release tests.
The job `Value-type tests (Temurin 21)` runs the Temurin 21 value-type command
and the Clojure 1.11 value-type command. The job `Value-type tests (Valhalla)`
runs the Valhalla value-type command. Performance benchmarks remain manual.

Failed tests upload `.scry-results/` as `scry-runtime-1.11`,
`scry-runtime-1.12`, `scry-runtime-1.13`, `scry-value-type-temurin-21`,
`scry-value-type-clj-1.11`, `scry-value-type-valhalla`, `scry-build`,
`scry-release`, or `scry-tooling`. These artifacts
contain structured failure data. Missing result files produce a warning;
the test failure still fails the job. CI does not publish releases or
configure branch protection.

### Build a library JAR

Build the source-only JAR for `org.hugoduncan/closed-record`:

    clojure -T:build jar

The build writes `target/closed-record-<version>.jar` and generates the POM
at `target/classes/META-INF/maven/org.hugoduncan/closed-record/pom.xml`.
The JAR contains runtime source, Maven metadata, and the EPL-2.0 license.
Build tools are not runtime dependencies.

The version is `0.1.<count>`, where `<count>` is the output of
`git rev-list --count HEAD`. Git and repository history are required;
version lookup failure stops the build. Use a full checkout for a
release-quality build. A shallow checkout can give a smaller count.
Each build removes stale output from `target/` before packaging.

Run the separate build tests, including a consumer JVM that loads the JAR:

    clojure -M:test:build-test -m scry.cli --dir test/build

Remove build output:

    clojure -T:build clean

`clean` is safe to repeat and does not require Git metadata. Generated
output under `target/` is ignored by Git. These build and clean commands do not install or
deploy the artifact; there is no published Clojars release yet.

Run the separate release tooling tests (pure notes and real Git fixtures):

    clojure -M:test:release:release-test -m scry.cli --dir test/release

### Markdown links

Install Git, Babashka `1.13.225`, and lychee `0.24.2`. These are development
tools, not library dependencies. The following Bash commands select the pinned
archives for Linux x86-64 or macOS arm64/x86-64, verify SHA-256 checksums,
and install the binaries in `$HOME/.local/bin`. They require curl and tar;
checksum verification uses `shasum` (macOS) or `sha256sum` (Linux).

```bash
(
  set -euo pipefail
  case "$(uname -s)-$(uname -m)" in
    Linux-x86_64) bb_platform=linux-amd64-static; lychee_platform=x86_64-unknown-linux-gnu ;;
    Darwin-arm64) bb_platform=macos-aarch64; lychee_platform=aarch64-apple-darwin ;;
    Darwin-x86_64) bb_platform=macos-amd64; lychee_platform=x86_64-apple-darwin ;;
    *) echo "Unsupported platform" >&2; exit 1 ;;
  esac
  tmp=$(mktemp -d)
  trap 'rm -rf "$tmp"' EXIT
  cd "$tmp"
  bb_archive="babashka-1.13.225-$bb_platform.tar.gz"
  lychee_archive="lychee-$lychee_platform.tar.gz"
  bb_base=https://github.com/babashka/babashka/releases/download/v1.13.225
  lychee_base=https://github.com/lycheeverse/lychee/releases/download/lychee-v0.24.2
  for url in "$bb_base/$bb_archive" "$lychee_base/$lychee_archive"; do
    archive=${url##*/}
    curl -fsSL "$url" -o "$archive"
    curl -fsSL "$url.sha256" -o "$archive.sha256"
    # Babashka supplies only the digest; lychee also supplies the filename.
    printf '%s  %s\n' "$(awk '{print $1}' "$archive.sha256")" "$archive" > checksum
    if command -v sha256sum >/dev/null; then
      sha256sum --check checksum
    else
      shasum -a 256 --check checksum
    fi
    tar -xzf "$archive"
  done
  mkdir -p "$HOME/.local/bin"
  install -m 755 bb "lychee-$lychee_platform/lychee" "$HOME/.local/bin/"
) || exit $?
export PATH="$HOME/.local/bin:$PATH"
bb --version       # babashka v1.13.225
lychee --version   # lychee 0.24.2
```

From the repository root, run:

```bash
bb dev/link_check_test.clj
bb lychee:check
```

The fixture suite uses temporary Git repositories, real tools, and a loopback
HTTP server. The default check needs network access. It checks all Git-tracked
`*.md` files, including hidden directories and historical tasks. Untracked files
and generated output are not input documents. Relative links use their source
document; root-relative links use the checkout root. Local files, directories,
and Markdown heading fragments are checked. HTTP and HTTPS checks follow
redirects and verify TLS; external fragments, email, and other protocols are
outside scope.

The command runs a local fragment pass and an online pass. Missing tools,
invalid configuration, missing targets or headings, and failed web requests
produce a nonzero exit. Request timeouts and retries are bounded. Diagnostics
identify the source and failed link. Neither pass rewrites documents or writes
reports or caches. CI runs the fixtures and the same task once, outside the
runtime matrix.

Shared policy is in `lychee.toml` and `dev/link_check.clj`. No URL exceptions
are currently needed. If a legitimate URL cannot be checked, add only an exact,
anchored URL exclusion in `lychee.toml`, with a comment that explains why.
Do not exclude document trees or accept error status codes to make checks pass.
