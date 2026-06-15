# SuiLearn Policy Gates

## Before Editing

- Load active role file.
- Classify the change as Tiny, Normal, or Major.
- Declare planned file list.
- Record `base_ref`.
- Check locks/worktree.
- Run baseline tests for business-code edits.

## Before Completion

- Run verification or state why not applicable.
- Run `git diff <base_ref> --stat`.
- Check changed files against allowed scope.
- Provide reviewer-style self review.

## Retired Paths

Do not create new files under:

- `docs/proposals/**`
- `docs/superpowers/specs/**`
- `docs/superpowers/plans/**`
