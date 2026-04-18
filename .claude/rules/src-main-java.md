---
globs: **/src/main/java/**/*.java
---

# Production Java Code Rules

- Use `@Slf4j` for logging — never `System.out.println`
- Use `import` statements — never fully-qualified class names inline
- Use Lombok: `@Getter`/`@Setter` for fields, `@Data` for POJOs, `@Builder` for complex objects
- Use modern Java 17: streams, try-with-resources for resource streams
- Throw descriptive exceptions; always include original cause when rethrowing
- No Mockito imports in production code
- Validate inputs at system boundaries only (user input, external APIs) — trust internal code
- Jackson 3.x uses `tools.jackson` package namespace (not `com.fasterxml.jackson`)
