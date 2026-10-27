# Design follow-ups

- [x] Clarify the hook acceptance contract within the existing check scope:
  do formatting and lint check staged content or working-tree content?
  State the expected hook result when staged content has a lint defect but
  an unstaged correction passes, and when formatting repairs a selected file
  without staging the repair. Specify whether the commit must stop for review
  and manual staging. Keep the no-staging and partial-staging safety rules;
  leave the implementation mechanism to planning. Make these outcomes explicit
  in the hook acceptance tests.
