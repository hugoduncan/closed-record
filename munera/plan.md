# Plan

Task order. Tasks are in `munera/open/`. This file gives the recommended order.

1. `022-value-type-closed-shape`: no `__meta` and `__extmap` on value output,
   so value records can flatten. Ready: value-type `4ed8d0a3` provides
   `:value-type/closed` (see the task Prerequisite). `plan.md` and
   `steps.md` exist. Slices 1 to 5 done. Next: Slice 6 (final
   verification: the value-type suite on Valhalla, local or CI, then all
   CI jobs). Not blocked: the branch is pushed and matches origin.
