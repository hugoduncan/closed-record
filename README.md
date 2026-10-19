# closed-record

Provides adefrecord like record, that does not permit assoc'ing
additional keys, or dissoc'ing declared keys.

## Rational

This is intended to provide a type that can be used as an implementation
detail, and catches issues with incorrect assoc, dissoc, etc.

It is intend as a complement to clojure's new required :keys!, :select,
:all destructing directives. It is not intended as an antagonist to
these.
