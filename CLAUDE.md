# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

YJ Schema Validator is a Java CLI tool for validating YAML and JSON files against JSON Schema definitions (drafts 2019-09/2020-12). Built with Spring Boot 4.0.2, it uses Jackson 3.x for parsing and NetworkNT `json-schema-validator` 3.0.0 for validation. Current version: 2.0.2-SNAPSHOT.

## Build & Test Commands

```bash
# Build (includes tests + JaCoCo coverage)
./mvnw clean package

# Run tests only
./mvnw test

# Run a specific test class
./mvnw test -Dtest=YamlSchemaValidatorTest

# Run a specific test method
./mvnw test -Dtest=YamlSchemaValidatorTest#shouldValidateYamlSuccessfully

# CI build (used by GitHub Actions)
./mvnw -B package --file pom.xml -Pdefault --no-transfer-progress

# Run the application
java -jar target/yj-schema-validator-2.0.2-SNAPSHOT.jar [options] [files...]
```

## Architecture

The application follows a Spring Boot CLI runner pattern:

1. **`YamlSchemaValidatorApplication`** — Spring Boot entry point
2. **`YamlSchemaValidatorRunner`** — `ApplicationRunner` that processes CLI arguments and orchestrates validation
3. **`YamlSchemaValidator`** — Core validation logic: loads schemas (local/remote), parses YAML/JSON via Jackson, validates against JSON Schema using NetworkNT, supports multi-document YAML and stdin
4. **`YamlSchemaValidatorConfig`** — Spring `@ConfigurationProperties` bean holding all CLI options (files, schema, reportType, httpTimeout, schemaOverride, ignoreSslErrors, color, reportFileName)

**Output pipeline** (`output/` package):
- `FilesOutput` — Wraps validation results; formats as colored text, JSON, or YAML
- `FilesOutputToJunit` — Generates JUnit XML reports
- `FilesOutputToSarif` — Generates SARIF JSON reports (17 model classes in `sarif/` subpackage)

**Report types** defined in `config/ReportType` enum: `TEXT`, `YAML`, `JSON`, `JUNIT`, `SARIF`

## Code Style & Quality

Three tools enforce code quality at the `validate` phase (all fail the build):

- **Spring Java Format** (`spring-javaformat-maven-plugin` 0.0.47) — tab indentation, Spring formatting conventions
- **Checkstyle** (`maven-checkstyle-plugin` 3.6.0 + `spring-javaformat-checkstyle`) — file max 800 lines, method max 80 lines
- **PMD** (`maven-pmd-plugin` 3.28.0) — best practices, code style, design, error prone, multithreading, performance rules

```bash
# Auto-format code
./mvnw spring-javaformat:apply

# Check all style violations
./mvnw validate

# Check PMD violations only
./mvnw validate 2>&1 | grep "PMD Failure"

# Check Checkstyle violations only
./mvnw validate 2>&1 | grep -E "violations|ERROR.*\.java"

# Format then validate
./mvnw spring-javaformat:apply && ./mvnw validate
```

**Config files**: `checkstyle.xml`, `checkstyle-suppressions.xml`, `pmd-ruleset.xml` at project root.

### Coding Standards
- **Indentation**: Tabs (enforced by spring-javaformat)
- **Imports**: Always use `import` statements — never use fully qualified class names inline
- **Lombok**: `@Data` for POJOs, `@Getter`/`@Setter` for fields, `@Slf4j` for logging
- **Logging**: SLF4J via `@Slf4j` — no `System.out.println`
- **Error handling**: Descriptive exceptions; include original cause when rethrowing

## Key Technical Details

- **Java 17+** required
- **Jackson 3.x** — uses `tools.jackson` package namespace (not `com.fasterxml.jackson`)
- **Lombok** — `@Data`, `@RequiredArgsConstructor`, `@Slf4j` used throughout
- **JaCoCo** enforces minimum 80% line coverage
- Test data lives in `src/test/resources/testdata/` (valid/invalid YAML/JSON files and sample schemas)
- Tests use `@SpringBootTest` with `@ActiveProfiles("test")` and parameterized tests (`@CsvSource`, `@MethodSource`)
