#!/bin/bash
# PostCompact hook: re-inject critical project context after context compaction
cat <<'CONTEXT'
## yj-schema-validator Project Quick Reference (re-injected after compaction)

**Build:** `./mvnw clean package` | **Test:** `./mvnw test`
**Format:** `./mvnw spring-javaformat:apply` | **FQN fix:** `python3 .claude/scripts/fix_fqn.py .`
**Validate:** `./mvnw validate` (runs Spring Java Format + Checkstyle + PMD — all fail the build)

**Style:** Tabs, imports (never inline FQN), @Slf4j for logging, Lombok annotations
**Testing:** JUnit 5 Assertions, prefer real data over mocks, @TempDir for temp files
**Coverage:** JaCoCo minimum 80%
**Tech:** Java 17, Spring Boot 4.0.2, Jackson 3.x (tools.jackson namespace), NetworkNT json-schema-validator 3.0.0
CONTEXT
