---
name: update-deps
description: Check for dependency and plugin updates, review and apply selected upgrades
allowed-tools: Bash(./mvnw *), Bash(python3 *)
---

## Dependency & Plugin Update Workflow

### Step 1: Scan for updates

```bash
./mvnw versions:display-property-updates 2>&1 | grep '\->' > /tmp/yjsv-property-updates.txt
./mvnw versions:display-plugin-updates 2>&1 | grep '\->' | grep -v 'reactor\|Help\|Could not' > /tmp/yjsv-plugin-updates.txt
```

### Step 2: Parse and filter results

Exclude Spring Boot managed dependencies (jackson, spring-*, snakeyaml, logback, slf4j, junit, mockito, lombok, etc.) and internal modules.

Present a table:
```
#  Type       Name                                              Current         New
```

### Step 3: Ask for confirmation

Show updates to the user. Flag major version jumps as potentially breaking.

### Step 4: Apply selected updates

Edit `pom.xml` properties for selected updates, then:
```bash
./mvnw spring-javaformat:apply
```

### Step 5: Validate the build

```bash
./mvnw clean package 2>&1 | tail -30
```

### Notes

- **Never override Spring Boot managed versions** unless explicitly asked
- **`checkstyle.version`** is pinned to `9.3` for `spring-javaformat-checkstyle` compatibility
- **`spring-javaformat.version`** affects both formatter and checkstyle -- test both after upgrading
- When upgrading **`maven-pmd-plugin`**, check for deprecated/renamed PMD rules in `pmd-ruleset.xml`
