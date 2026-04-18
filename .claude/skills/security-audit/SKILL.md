---
name: security-audit
description: Scan code for security vulnerabilities (OWASP patterns, path traversal, injection risks)
argument-hint: [file path]
allowed-tools: Bash(./mvnw *), Read, Glob, Grep
---

## Security Audit for yj-schema-validator

Scan the codebase (or specific file) for common security vulnerabilities.

### Scope

If `$ARGUMENTS` is provided, limit the scan to that path. Otherwise, scan all source code.

### Checks to Perform

#### 1. Path Traversal
Search for file path construction from untrusted input:
```
Grep for: new File(.*getName|Path.of(.*input|Paths.get(.*input
```
Verify each hit has canonical path validation or sanitization.

#### 2. Command Injection
Search for Runtime.exec, ProcessBuilder with user input:
```
Grep for: Runtime.getRuntime|ProcessBuilder|\.exec\(
```

#### 3. YAML Deserialization
Search for unsafe YAML parsing:
```
Grep for: new Yaml\(\)|Yaml.load\(|ObjectMapper.*readValue.*untrusted
```
Verify Jackson is configured safely.

#### 4. XML External Entity (XXE)
Search for XML parsing without disabling external entities:
```
Grep for: DocumentBuilder|SAXParser|XMLReader|TransformerFactory
```

#### 5. Sensitive Data Exposure
Search for credentials, tokens, or keys in code:
```
Grep for: password|secret|token|apiKey|private.key (case-insensitive, exclude test files)
```

#### 6. SSL/TLS Bypass
Check the SSL ignore feature (`--ignoreSslErrors`) for proper scoping:
- Verify it only affects the specific HTTP client, not global SSL context
- Check for certificate validation bypass patterns

### Report Format

| Severity | Location | Issue | Recommendation |
|----------|----------|-------|----------------|
| CRITICAL | file:line | Description | Fix suggestion |

Report only confirmed findings, not theoretical risks. If clean, say "No vulnerabilities found."
