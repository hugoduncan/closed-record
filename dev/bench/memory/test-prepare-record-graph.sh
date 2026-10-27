#!/bin/sh
# Real compiler/JAR tests. Run from the repository root with JAVA_HOME set.
set -eu
root=$(mktemp -d "${TMPDIR:-/tmp}/record-graph-build.XXXXXXXX")
trap 'rm -rf "$root"' EXIT HUP INT TERM
prepare=dev/bench/memory/prepare-record-graph.sh
sh "$prepare" "$root/first"
sh "$prepare" "$root/second"
cmp "$root/first/SHA256SUMS" "$root/second/SHA256SUMS"
(cd "$root/first" && shasum -a 256 -c SHA256SUMS)
if sh "$prepare" "$root/first"; then
  echo 'Existing build was accepted' >&2; exit 1
fi
(cd "$root/first" && shasum -a 256 -c SHA256SUMS)
# A changed source must fail before reserving the output directory.
mkdir "$root/changed"
cp "$prepare" "$root/changed/prepare-record-graph.sh"
cp dev/bench/memory/RecordGraphProbe.java "$root/changed/RecordGraphProbe.java"
printf '\n// changed\n' >> "$root/changed/RecordGraphProbe.java"
if sh "$root/changed/prepare-record-graph.sh" "$root/rejected"; then
  echo 'Changed source was accepted' >&2; exit 1
fi
test ! -e "$root/rejected"
# Check that the resulting binary is a usable agent, not only a ZIP archive.
"$JAVA_HOME/bin/java" -javaagent:"$root/first/record-graph-agent.jar" -version
printf '%s\n' 'Record graph agent build tests passed.'
# Reconcile runtime-selected bytes with build output, without a mocked agent.
cat > "$root/IdentityCheck.java" <<'JAVA'
public class IdentityCheck {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            try {
                RecordGraphProbe.identity();
                throw new AssertionError("Inactive probe was accepted");
            } catch (IllegalStateException expected) {
                return;
            }
        }
        var identity = RecordGraphProbe.identity();
        for (String file : new String[]{"record-graph-agent.jar", "classes/RecordGraphProbe.class"}) {
            var bytes = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(args[0], file));
            var hash = java.util.HexFormat.of().formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
            String key = file.endsWith(".jar") ? "jar-sha256" : "class-sha256";
            if (!hash.equals(identity.get(key))) throw new AssertionError(identity);
        }
        var loaded = java.nio.file.Path.of(java.net.URI.create(identity.get("location")));
        if (!java.nio.file.Files.isSameFile(loaded,
                java.nio.file.Path.of(args[0], "record-graph-agent.jar"))) {
            throw new AssertionError(identity);
        }
    }
}
JAVA
"$JAVA_HOME/bin/javac" --release 21 -cp "$root/first/record-graph-agent.jar" -d "$root" "$root/IdentityCheck.java"
"$JAVA_HOME/bin/java" -cp "$root:$root/first/record-graph-agent.jar" IdentityCheck
"$JAVA_HOME/bin/java" -javaagent:"$root/first/record-graph-agent.jar" \
  -cp "$root:$root/first/record-graph-agent.jar" IdentityCheck "$root/first"
printf '%s\n' 'Loaded graph identity tests passed.'
