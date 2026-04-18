---
globs: **/pom.xml
---

# Maven POM Rules

- Define dependency versions as properties in `pom.xml` `<properties>` (unless managed by Spring Boot parent)
- When adding a new dependency, check if Spring Boot already manages its version first
- Don't override Spring Boot managed versions unless explicitly needed
