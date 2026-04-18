---
name: push
description: Create/update GitHub issue, branch, commit, PR, then continue implementing
argument-hint: [issue-number or description]
allowed-tools: Bash(gh *), Bash(git *)
---

## Push Workflow: Issue -> Branch -> PR -> Continue

Argument `$ARGUMENTS` is either:
- An **existing issue number** (e.g. `42`) -- update it and wire up the branch/PR
- A **short description** (e.g. `add retry logic`) -- create a new issue first

---

### Step 1: Resolve or create the GitHub issue

**If `$ARGUMENTS` is a number** -- view the existing issue:
```bash
gh issue view $ARGUMENTS
```

**If `$ARGUMENTS` is a description** -- create a new issue:
```bash
gh issue create \
  --title "$ARGUMENTS" \
  --body "## Context
<fill in context from current conversation>

## Acceptance criteria
- [ ] <criterion 1>
- [ ] <criterion 2>"
```
Capture the new issue number from the output.

---

### Step 1.5: Shortcut -- skill-only changes go directly to main

If the **only** changed files are under `.claude/` (skill updates, no source code changes):

```bash
git diff --stat
```

If all changed files are `.claude/**`:
1. Skip Steps 2-6
2. Commit directly on `main`:
   ```bash
   git add .claude/
   git commit -m "<imperative summary>

   Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
   git push
   ```
3. Report the commit SHA and stop.

---

### Step 2: Ensure we're on main and up to date
```bash
git checkout main && git pull
```

### Step 3: Create a feature branch
```bash
git checkout -b feature/<issue-number>-<short-slug>
```

### Step 4: Stage and commit
```bash
git add <changed-files>
git commit -m "<imperative summary>

Closes #<issue-number>

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

### Step 5: Push the branch
```bash
git push -u origin HEAD
```

### Step 6: Create the pull request
```bash
gh pr create \
  --title "<concise title>" \
  --body "## Summary
- <bullet 1>
- <bullet 2>

## Test plan
- [ ] Run \`./mvnw test\`
- [ ] Verify <key behavior>

Closes #<issue-number>

Generated with [Claude Code](https://claude.com/claude-code)"
```

Report the PR URL to the user.

### Step 7: Continue implementing

Implement the work described in the issue. Run `./mvnw test` after each meaningful change.
