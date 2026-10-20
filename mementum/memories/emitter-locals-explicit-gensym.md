🔁 Emitter: every generated local is an explicit gensym

In a `deftype*` method body, the declared field names are locals. A generated local with a literal name
(`this`, `target`, `overage`, `k`) shadows a user field of the same name, or the field shadows it.
`defrecord` has the same defect for a field named `overage` in a type with more than 20 fields.

- Bind each generated local once with `(gensym "name__")` in a `let` outside the syntax quote. Use `~name`.
- ❌ An auto-gensym `x#` does not cross a nested syntax quote or a `~(helper ...)` call. The helper gets a
  different symbol. Use an explicit gensym when a local is shared between forms.
- Test: a type whose fields have the names of the generated locals (for example `[this gtarget]`, and
  `[overage f1 ... f21]` for the positional factory). Each field must give its own value.

Found in task 002 (implementation review and code-shaper passes). Source: `closed-record.impl.jvm/generated-methods`.
