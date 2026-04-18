---
name: precommit
description: Format, validate checkstyle/PMD, and run tests before committing
allowed-tools: Bash(./mvnw *)
---

## Pre-commit Check: Format -> Validate -> Test

Run this before every commit to catch format and style issues early.

---

### Step 1: Auto-format

```bash
./mvnw spring-javaformat:apply
```

---

### Step 2: Validate (checkstyle + PMD)

```bash
./mvnw validate 2>&1 | grep -E "^\[ERROR\]|violations|PMD Failure"
```

If violations remain after auto-format, fix them manually. Do **not** proceed to Step 3 until `validate` passes cleanly.

---

### Step 3: Run tests

```bash
./mvnw test 2>&1 | tail -20
```

---

### Outcome

Report a summary:

| Step | Result |
|------|--------|
| Format | Applied / Already clean |
| Validate | 0 violations / N violations (list them) |
| Tests | X passed, 0 failures / list failures |

If everything is green, the working tree is ready to commit.
If anything fails, fix it before committing.
