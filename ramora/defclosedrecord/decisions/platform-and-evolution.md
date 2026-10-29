# Decisions: platform and evolution

## D-13 Platform layout

- Subject: namespace layout.
- Choice:
  - Start with the JVM.
  - Put the public namespace `closed-record.core` in a `.cljc` file.
  - Put the internal protocol and error helpers in `closed-record.runtime`, a `.cljc` file with no dependency on
    the public API or emitters.
  - Put host-specific emitters in internal namespaces behind the macro.
- Rejected: one public namespace for each host. The API can then differ between hosts.
- Rationale: K-02. ClojureScript and babashka can add emitters and keep the same API.
- Qualities: + portability, + local change (one emitter for each host).
- Scope: all hosts.
- Commitment: high for `closed-record.core`. Low for the `impl` namespaces.

## D-14 Clojure version

- Subject: minimum Clojure version on the JVM.
- Choice: Clojure 1.11.
- Rejected: Clojure 1.13 minimum. The 1.13 directives are not necessary, and fewer users can adopt it.
- Rationale: K-01. All the mechanisms that the type uses exist in 1.3 and later.
- Scope: JVM.
- Commitment: medium. A later minimum needs a significant reason (K-01).

## D-17 `ClosedRecord` protocol

- Subject: how `check` reads the extra keys.
- Choice: `(defprotocol ClosedRecord (-extra-keys [r]))` in `closed-record.runtime`. Each generated type implements it
  inline. On the JVM, the protocol interface is `closed_record.runtime.ClosedRecord`.
- Rejected:
  - `definterface`. It is JVM-only, so it cannot be in a portable `.cljc` file (K-02).
  - Read `__extmap` by reflection. It is slow and host-specific.
- Rationale: K-02. One abstraction for all hosts. Inline implementation gives a fast `satisfies?` check.
- Assumptions: `core` loads `runtime` before macro expansion. Generated code refers to the runtime protocol.
- Qualities: + portability. − The generated code depends on the internal runtime namespace.
- Scope: all hosts. Internal: users must not extend the protocol.
- Commitment: medium.

## D-20 API compatibility

- Subject: change to the public API after the first release.
- Public API: the `defclosedrecord` syntax, `check`, the generated `->Name` and `map->Name`, the behaviour table in
  [purpose](../purpose.md#behaviour), and the `ex-data` of D-03.
- Choice: change by accretion only. Do not remove or change a public name or behaviour. A breaking change uses a new name.
- Rejected: semantic versioning with breaking major versions.
- Rationale: lifecycle "long-lived, with a small and stable API". K-03 applies only before the first release.
- Qualities: + stability for users. − Mistakes in the API stay.
- Scope: all hosts, after the first release.
- Commitment: high.
