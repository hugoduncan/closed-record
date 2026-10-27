# Feature gate

## Discovery: 2026-10-27

API selection is resolved below by design commit `adfa652`. No benchmark matrix
is fixed, and no capability or performance result is claimed. The discovery
findings come from upstream documentation and source, not executable probes.

## Distinct candidate APIs

1. [JEP 401: Value Objects](https://openjdk.org/jeps/401) is integrated for
   JDK 28 as a preview. It introduces `value class` and permits reference
   flattening, including nullable array elements and fields. Its documented
   compile command uses `javac --release 28 --enable-preview`; execution uses
   `java --enable-preview`. Flattening is a JVM choice, not a semantic guarantee.
   An array of value objects need not be null-restricted.
2. [Null-Restricted Value Class Types](https://openjdk.org/jeps/8316779)
   is a separate draft, updated 2026-05-28. It proposes `Point!` types and
   `Point![]` arrays. It explicitly excludes null-restricted types for identity
   classes and classes without a default value. A draft is not proof that a
   downloadable runtime implements the syntax.
3. The Valhalla `lworld` branch has internal experimental APIs. At revision
   `12e87d09246d366cc74cb409e4c914351256e98b`,
   [ValueClass.java](https://github.com/openjdk/valhalla/blob/12e87d09246d366cc74cb409e4c914351256e98b/src/java.base/share/classes/jdk/internal/value/ValueClass.java)
   exposes `newNullRestrictedAtomicArray`, `newNullRestrictedNonAtomicArray`,
   `isFlatArray`, `isNullRestrictedArray`, and `isNullRestrictedField`.
   [NullRestricted.java](https://github.com/openjdk/valhalla/blob/12e87d09246d366cc74cb409e4c914351256e98b/src/java.base/share/classes/jdk/internal/vm/annotation/NullRestricted.java)
   defines an internal field annotation. Its documentation says the annotation
   is ignored unless the field type is a concrete value type that can be
   preloaded. It is for internal experimental use, not specified Java SE
   behaviour. Atomic and non-atomic arrays are distinct comparison contracts.

The [Valhalla project page](https://openjdk.org/projects/valhalla/) lists value
objects, null-restricted storage, and array enhancements separately. The
[Valhalla download page](https://jdk.java.net/valhalla/) currently offers
`27-jep401ea3+1-1` (2026-03-11), including macOS AArch64. That build predates
both the current draft update and JDK 28 integration. Its availability does not
establish support for the current internal APIs or draft syntax. No experimental
runtime was installed or selected in this pass.

## Applicability and next probes

Closed records currently use the Clojure record layout, not Java `value class`
declarations. Do not label an external Java value class as a closed record.
Potential comparisons include an external value class array, or a closed-record
field that holds an external array/value object. Those are different claims.
A Clojure type hint does not request null restriction.

With API selection resolved, probes must establish construction, read, replacement,
null rejection, and actual layout independently. Ordinary baseline remains
Clojure 1.11.4 with Temurin 21, without preview flags. Exact runtime builds,
module exports, and layout flags for experimental probes remain to be verified.
No support limit has yet been demonstrated by execution.

## Selected path and remaining gate

Design commit `adfa652` permits internal Valhalla APIs with atomic arrays.
Use `ValueClass.newNullRestrictedAtomicArray` for null-restricted value arrays
and `jdk.internal.vm.annotation.NullRestricted` for fields on external value-type
fixtures where required. The references above identify the API candidates to
verify against the selected build; they do not prove that the listed EA binary
contains them. Do not require public APIs, non-atomic arrays, or draft `!` syntax.

Runtime selection, download, and isolated setup may proceed without another
user decision. Pin the actual JDK build, Clojure version, internal API signatures,
compile/run commands, module access flags, and layout settings. Keep this setup
in benchmark tooling and disclose its experimental status in the performance
report. Verify and record atomicity and memory-ordering guarantees for each
compared operation; atomicity alone does not prove flat storage.

The capability gate remains open. Run executable operation, null-rejection,
and layout checks before fixing the matrix. Establish closed-record applicability
separately from external comparison types. A missing runtime is an availability
limit, not proof that the feature is unsupported. If a selected build lacks the
APIs, record that result and seek a suitable build within this path; do not
silently substitute nullable arrays or draft syntax. Ask for a user decision
only if a remaining API interpretation would change the approved scope.
