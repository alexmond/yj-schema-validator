---
name: test
description: Run tests for the yj-schema-validator project
argument-hint: [TestClass#method]
allowed-tools: Bash(./mvnw *)
---

## Run yj-schema-validator tests

Run tests based on the provided arguments.

### No arguments -- run all tests
```bash
./mvnw test
```

### Specific test class (e.g., `/test YamlSchemaValidatorTest`)
```bash
./mvnw test -Dtest=$ARGUMENTS
```

### Specific test method (e.g., `/test YamlSchemaValidatorTest#shouldValidateYamlSuccessfully`)
```bash
./mvnw test -Dtest=$ARGUMENTS
```

### Key test classes
- `YamlSchemaValidatorTest` -- Core validation logic
- `YamlSchemaValidatorRunnerTest` -- CLI runner integration tests
- `YamlSchemaValidatorStdinTest` -- Stdin handling tests
- `FilesOutputTest` -- Output formatting tests
- `FilesOutputToJunitTest` -- JUnit output tests
- `FilesOutputToSarifTest` -- SARIF output tests

Report test results clearly. On failure, show the failing test name, assertion message, and relevant stack trace.
