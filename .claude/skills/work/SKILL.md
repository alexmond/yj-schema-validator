---
name: work
description: Process open GitHub issues from simplest to hardest in a continuous loop
allowed-tools: Bash(gh *), Bash(git *), Bash(./mvnw *)
---

# Sequential Issue Processing

Process open GitHub issues from simplest to hardest in a continuous loop.

## Workflow

### 1. Gather and prioritize
```bash
gh issue list --state open --json number,title,labels --limit 100
```
Sort issues by estimated complexity (simplest first):
- Small bugs / single-file fixes
- Multi-file bugs
- New features / enhancements
- Architecture / refactoring tasks

### 2. For each issue (simplest to hardest):

#### a. Triage complexity

**Trivial / simple** (single-file fix, small bug):
- Implement directly, push, merge, move on.

**Non-trivial** (multi-file, requires design decisions):
- Enter plan mode, write a plan.
- Post the plan as a comment on the issue.
- Move to the next issue while awaiting approval.

#### b. Implement (when approved or trivial)
- Create a feature branch and implement the fix/feature
- Run `./mvnw spring-javaformat:apply` after code changes
- Run `./mvnw validate` to check PMD/checkstyle
- Run tests: `./mvnw test`
- Fix any failures before proceeding

#### c. Push and merge
- Use `/push` skill to create/update the PR
- Wait for CI checks: `gh pr checks <N> --watch`
- If checks pass, merge: `gh pr merge <N> --squash --delete-branch`

#### d. Next issue
- Pick the next issue, repeat from step 2a

### 3. Stop when
- All open issues are resolved, OR
- The user interrupts
