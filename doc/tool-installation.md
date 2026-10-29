# Development tool installation

Use Bash for these commands. Supported native installations below are Linux
x86-64 and macOS arm64. Install Git, curl, tar, unzip, and Python 3.12 first.
Linux also needs the Python `venv` package. Checksum commands use `sha256sum`
on Linux or `shasum` on macOS. Run each block only after the preceding block
succeeds. Downloads need network access.

## Java and Clojure CLI

CI uses Temurin Java 21. The archive below pins the local patch version to
21.0.10+7. It installs under your home directory, without changing system Java.

```bash
(
  set -euo pipefail
  case "$(uname -s)-$(uname -m)" in
    Linux-x86_64) platform=linux; arch=x64 ;;
    Darwin-arm64) platform=mac; arch=aarch64 ;;
    *) echo "Unsupported platform" >&2; exit 1 ;;
  esac
  tmp=$(mktemp -d)
  trap 'rm -rf "$tmp"' EXIT
  cd "$tmp"
  base="https://api.adoptium.net/v3"
  curl -fsSL "$base/binary/version/jdk-21.0.10%2B7/$platform/$arch/jdk/hotspot/normal/eclipse" -o java.tar.gz
  curl -fsSL "$base/checksum/version/jdk-21.0.10%2B7/$platform/$arch/jdk/hotspot/normal/eclipse" -o digest
  printf '%s  java.tar.gz\n' "$(awk '{print $1}' digest)" > checksum
  if command -v sha256sum >/dev/null; then
    sha256sum --check checksum
  else
    shasum -a 256 --check checksum
  fi
  mkdir -p "$HOME/.local/share/closed-record-java"
  tar -xzf java.tar.gz --strip-components=1 -C "$HOME/.local/share/closed-record-java"
) || exit $?
export JAVA_HOME="$HOME/.local/share/closed-record-java"
if [ -d "$JAVA_HOME/Contents/Home" ]; then
  export JAVA_HOME="$JAVA_HOME/Contents/Home"
fi
export PATH="$JAVA_HOME/bin:$HOME/.local/bin:$PATH"
java -version
```

Install Clojure CLI `1.12.6.1673` from its versioned archive:

```bash
(
  set -euo pipefail
  tmp=$(mktemp -d)
  trap 'rm -rf "$tmp"' EXIT
  cd "$tmp"
  curl -fsSL https://download.clojure.org/install/clojure-tools-1.12.6.1673.tar.gz -o clojure.tar.gz
  tar -xzf clojure.tar.gz
  cd clojure-tools
  prefix="$HOME/.local"
  lib="$prefix/lib/clojure"
  mkdir -p "$lib/libexec" "$prefix/bin" "$prefix/share/man/man1"
  install -m 644 deps.edn example-deps.edn tools.edn "$lib/"
  install -m 644 exec.jar clojure-tools-1.12.6.1673.jar "$lib/libexec/"
  sed "s@PREFIX@$lib@g" clojure > "$prefix/bin/clojure"
  sed "s@BINDIR@$prefix/bin@g" clj > "$prefix/bin/clj"
  chmod 755 "$prefix/bin/clojure" "$prefix/bin/clj"
  install -m 644 clojure.1 clj.1 "$prefix/share/man/man1/"
) || exit $?
clojure -Sdescribe
```

The CLI archive is downloaded over HTTPS; this block does not verify a separate
checksum. `clj` also needs `rlwrap`; project commands use `clojure` instead.

## Format, lint, and prose tools

Install cljfmt `0.16.4`, clj-kondo `2026.04.15`, and Vale `3.17.1`.
Verify each archive against its release checksum before extraction.

```bash
(
  set -euo pipefail
  case "$(uname -s)-$(uname -m)" in
    Linux-x86_64)
      fmt=linux-amd64-static; kondo=linux-static-amd64; vale_platform=Linux_64-bit ;;
    Darwin-arm64)
      fmt=darwin-aarch64; kondo=macos-aarch64; vale_platform=macOS_arm64 ;;
    *) echo "Unsupported platform" >&2; exit 1 ;;
  esac
  tmp=$(mktemp -d)
  trap 'rm -rf "$tmp"' EXIT
  cd "$tmp"
  fmt_base=https://github.com/weavejester/cljfmt/releases/download/0.16.4
  kondo_base=https://github.com/clj-kondo/clj-kondo/releases/download/v2026.04.15
  vale_base=https://github.com/errata-ai/vale/releases/download/v3.17.1
  for url in "$fmt_base/cljfmt-0.16.4-$fmt.tar.gz" \
             "$kondo_base/clj-kondo-2026.04.15-$kondo.zip" \
             "$vale_base/vale_3.17.1_$vale_platform.tar.gz"; do
    archive=${url##*/}
    curl -fsSL "$url" -o "$archive"
    case "$archive" in
      vale_*)
        curl -fsSL "$vale_base/vale_3.17.1_checksums.txt" -o checksums
        awk -v name="$archive" '$2 == name {print}' checksums > checksum ;;
      *)
        curl -fsSL "$url.sha256" -o digest
        printf '%s  %s\n' "$(awk '{print $1}' digest)" "$archive" > checksum ;;
    esac
    test -s checksum
    if command -v sha256sum >/dev/null; then
      sha256sum --check checksum
    else
      shasum -a 256 --check checksum
    fi
    case "$archive" in
      *.zip) unzip -q "$archive" ;;
      *) tar -xzf "$archive" ;;
    esac
  done
  mkdir -p "$HOME/.local/bin"
  install -m 755 cljfmt clj-kondo vale "$HOME/.local/bin/"
) || exit $?
cljfmt --version
clj-kondo --version
vale --version
```

Install [Babashka and lychee](development.md#markdown-links) and the
[pinned pre-commit runner](quality-checks.md#commit-hooks). Keep the PATH and
JAVA_HOME exports in your shell configuration. Quickdoc and the test runner
resolve from `deps.edn`; no separate installation is needed. `bb vale` downloads
the pinned proselint styles from `.vale.ini`.

Run the [quality commands and focused tests](quality-checks.md) from the
repository root. No copied dependency imports or user Clojure configuration
are required. The shared commands use explicit project configuration.
