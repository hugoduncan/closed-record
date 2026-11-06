🔁 Tests: state each fact once, then derive the rest from it

In task 021, code-shaper review needed five passes (2, 3, 4, 5, 7) to find copied facts in two test files.
Each copy can drift from the source. Then a check compares different things, or it stops working, with no failure.
Do this when you first write the test:

- One map of valid field values (`valid`). Build the base record, the positional values, the `assoc` cases
  and the expected reads from that map. Do not use literals.
- Name key sets once (`non-nil-keys` as `(keys bad)`). Loop over that name, not over `[:i :s]`.
- Expected expansion facts come from the input form (the `:non-nil` symbols of the field vector), not from a
  literal `'[p s]` or a count `2`. Make a variant form from the base form (`strip-meta`). Do not write a second
  literal form.
- Path tables use one shape (`{:label :f :direct?}`). Behaviour depends on data flags, not on label strings.
- Do not copy fixture helper bodies (expand, print, strip). Split the fixture into public steps and reuse them.

Limit: derive expected values from the test input, not from the code under test
(see `mementum/knowledge/development-quality-boundaries.md`, expected scope).
