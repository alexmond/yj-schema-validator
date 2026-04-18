---
name: issue-track
description: Post a status comment on a GitHub issue (starting work, plan update, completed)
argument-hint: <issue-number> <status>
allowed-tools: Bash(gh *)
---

## Track Issue Progress via Comments

Post a status update comment on GitHub issue #$ARGUMENTS.

### Usage

The argument format is: `<issue-number> <status>` where status is one of:
- `start` -- Post a "Work started" comment
- `update <text>` -- Post a custom progress update
- `done` -- Post a completion comment

### Behavior

**For `start`:**
```bash
gh issue comment <number> --body "Work started on this issue.

Branch: \`feature/<number>-<slug>\` (will be created shortly)"
```

**For `update <text>`:**
```bash
gh issue comment <number> --body "Progress Update

<text>"
```

**For `done`:**
```bash
gh issue comment <number> --body "Implementation complete -- PR created and CI checks passing."
```
