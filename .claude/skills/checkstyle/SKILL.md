---
name: checkstyle
description: Check and fix Checkstyle and PMD violations
allowed-tools: Bash(./mvnw *), Bash(python3 *)
---

## Check and Fix Code Style Violations

### Configuration

- **Spring Java Format**: `spring-javaformat-maven-plugin` 0.0.47 (tab indentation)
- **Checkstyle**: `maven-checkstyle-plugin` 3.6.0 with `spring-javaformat-checkstyle`
- **PMD**: `maven-pmd-plugin` 3.28.0
- **Suppressions**: `checkstyle-suppressions.xml` in project root
- **PMD ruleset**: `pmd-ruleset.xml` in project root
- All violations **fail the build** at `validate` phase

### Step 1: Check violations

```bash
./mvnw validate 2>&1 | grep -E "violations|ERROR.*\.java|PMD Failure"
```

### Step 2: Auto-format first

Always run `spring-javaformat:apply` before checking violations:
```bash
./mvnw spring-javaformat:apply
```

### Step 3: Fix FQN violations

```bash
python3 .claude/scripts/fix_fqn.py .
```

### Step 4: Handle remaining violations manually

| Violation | Fix |
|-----------|-----|
| `SpringCatch` | Rename `catch (Exception e)` to `catch (Exception ex)` |
| `NeedBraces` | Add `{ }` to single-line if/for/while |
| `SpringLambda` parens | Wrap single lambda param: `x ->` becomes `(x) ->` |
| `SpringLambda` block | Convert `x -> { return expr; }` to `x -> expr` |
| `SpringTernary` | Wrap condition in parens: `(cond) ? a : b` |
| `AvoidStarImport` | Replace `import pkg.*` with specific imports |
| `SpringHideUtilityClassConstructor` | Add private constructor + `final` class |
| `AnnotationUseStyle` trailing comma | Remove `,` before `})` in annotations |
| `AppendCharacterWithChar` | Use `.append('x')` not `.append("x")` for single chars |
| `MissingOverride` | Add `@Override` on interface/superclass implementations |

### Step 5: Re-format and validate

```bash
./mvnw spring-javaformat:apply && ./mvnw validate
```
