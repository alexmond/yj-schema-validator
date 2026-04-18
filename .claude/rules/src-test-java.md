---
globs: **/src/test/java/**/*.java
---

# Test Code Rules

- Use JUnit 5 (`org.junit.jupiter.api`) with `Assertions` — NOT AssertJ
- Prefer real test data (YAML/JSON files, `@TempDir` files) over Mockito mocks
- Use `@TempDir` for temporary files — never hardcode temp paths
- Use `@ParameterizedTest` with `@CsvSource` or `@MethodSource` to avoid test duplication
- Descriptive test method names: `testValidateSuccess`, `testValidateWithError`
- Test data lives in `src/test/resources/testdata/`
- Use `@SpringBootTest` with `@ActiveProfiles("test")` for integration tests
