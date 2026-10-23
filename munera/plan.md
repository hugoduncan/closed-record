# Plan

Task order. Tasks are in `munera/open/`. This file gives the recommended order.

## Order

1. [012-inlinable-methods](open/012-inlinable-methods/design.md): make the generated `assoc`, `without`,
   `containsKey` and `entryAt` as small as in `defrecord`, so that C2 inlines them; the `assoc` bench gap (V-11).
