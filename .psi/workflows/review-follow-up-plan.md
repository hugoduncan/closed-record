---
name: review-follow-up-plan
description: Execute newly added plan-review follow-up items for a Munera task (plan profile)
tools:
  - read
  - bash
  - edit
  - write
skills:
  - task-design
  - work-independently
advertise: false
---
For the Munera task identified by {{input}}, execute the unchecked,
actionable plan-review follow-up items in design-steps.md with updates to plan.md and
steps.md. Work independently.

Read the task's design.md as read-only context as needed. The plan profile may update plan.md, steps.md, implementation.md, and referenced code, tests, and docs when a current attributed follow-up item requires it.

Be careful not to introduce new ambiguity or inconsistencies.

If a step is completed, mark it done in steps.md.

When finished, ask yourself: "What would the next review or implementation step
need to know that you had to discover?" Append a minimalist entry to
implementation.md to record important information for subsequent reviews or
implementation.

**What to add:**
- task information that doesn't belong in design.md, but helps with carrying out
  the task
- paths to relevant project (non-task) files
- existing behaviour relevant to implementing the current task

Avoid adding duplicate information already in the task files.

Commit when done.
