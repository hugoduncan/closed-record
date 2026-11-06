# Value-type records

This page gives the details of the `defclosedrecord` option
`:value-type true`. The [README](../README.md#value-type-records) has an
introduction. The option is experimental and is not released.

## Representation

The option defines the record through
[value-type](https://github.com/hugoduncan/value-type). value-type selects the
representation when the class loads:

- Value output: on a JVM with JEP 401 value classes (a Valhalla early-access
  JDK with `--enable-preview`), the type is a value class.
- Identity fallback: on other JVMs, value-type's fallback policy (system
  property `value-type.fallback`) selects an identity class, a warning, or an
  error.

## Requirements

- Clojure 1.12.6. value-type does not load on Clojure 1.11.
- value-type on the class path at macro expansion and at run time, also for
  AOT classes.
- For value output: a Valhalla early-access JDK and `--enable-preview`.
- For `value-type.array`: also
  `--add-exports=java.base/jdk.internal.value=ALL-UNNAMED`.

Without value-type, macro expansion throws ex-info with
`:type :closed-record/value-type-unavailable` and `:class-name` (the class
name symbol, for example `user.Point`). Without the option, closed-record does
not load value-type.

## Behaviour

The record contract does not change, with these differences:

| Behaviour | Ordinary | Value output | Identity fallback |
|---|---|---|---|
| Hash values | as `defrecord` | same | same |
| Hash caching | yes | no | no |
| `identical?` | reference | field values | reference |
| `locking`, weak references | work | throw `IdentityException` | work |
| Serialization | as `defrecord` | not promised | as `defrecord` |
| Non-nil enforcement | none: `:non-nil` has no effect | native null restriction with a value-class tag on a JVM that supports native fields; otherwise constructor checks | constructor checks |
| Field storage type | primitive tag: the primitive; other fields: `Object` | `:non-nil` field: the tag class; other fields: as Ordinary | `:non-nil` field: the tag class; other fields: as Ordinary |

Each `hash` call computes the hash again. The option makes no performance
claim; see [performance](performance.md#value-type-records).

## Non-nil fields

With `:value-type true`, field metadata `^:non-nil` makes a field non-nil. A
non-nil field must have a tag that names a defined concrete, non-primitive
class. value-type owns these rules; see its
[field contract](https://github.com/hugoduncan/value-type/blob/edc3f79de77bcf044d6234df361e090edc46c813/doc/usage.md#portable-non-nil-fields).
closed-record passes the metadata to value-type with no change.

- Nil gives `NullPointerException`, and a value of the wrong type gives
  `ClassCastException`, on `->Name`, `map->Name`, `assoc` and record literals.
  On `->Name`, `map->Name` and `assoc`, closed-record does not wrap these host
  exceptions, and they have no `ex-data`. On record literals, the reader can
  wrap them in an exception with `ex-data`; the host exception is in the cause
  chain.
- `map->Name` without a key for a non-nil field gives `NullPointerException`.
- Key-set errors come first and do not change: `assoc` of an undeclared key,
  `dissoc` of a declared key and `map->Name` with extra keys throw ex-info as
  before.
- Without `:value-type true`, `:non-nil` has no effect, as in `defrecord`. The
  field accepts nil.

## Arrays and flattening

`value-type.array` makes arrays of value-type records. These arrays reject nil
elements and have atomic element access. See the value-type
[array documentation](https://github.com/hugoduncan/value-type/blob/edc3f79de77bcf044d6234df361e090edc46c813/doc/usage.md).

The JVM can store value objects inline in an array or in another object. This
is flattening. Flattening is a JVM choice, not a guarantee. `va/array-info`
gives the actual layout in `:flat?`.

On Valhalla `27-jep401ea3+1-1`, arrays of closed-record value records are not
flat. A record has hidden reference fields for metadata and extension keys.
These make the object too large to flatten, also for a record with one `long`
field. A `defvaluetype` type with one `long` field is flat on the same JVM. Use
`defvaluetype` directly when you need flat storage.
