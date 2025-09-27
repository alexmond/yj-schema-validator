package org.alexmond.yaml.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

@Slf4j
public class YamlSchemaValidator {

    public Boolean validate(String yamlPath, String schemaPath) {
        boolean validateSchema = true;
        String schemaString;
        try {
            // Step 1: Parse YAML to JSON
            ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
            JsonNode yamlNode = yamlMapper.readTree(new File(yamlPath));

            if (schemaPath == null) {
                // Check if YAML has schema URL
                JsonNode yamlSchemaNode = yamlNode.get("$schema");
                if (yamlSchemaNode != null && !yamlSchemaNode.asText().isEmpty()) {
                    schemaPath = yamlSchemaNode.asText();
                    log.info("Using schema URL from YAML: {}", schemaPath);
                    if (!isHttpUrl(schemaPath)) {
                        schemaPath = new File(new File(yamlPath).getParentFile(), yamlSchemaNode.asText()).getPath();
                    }
                } else {
                    throw new IllegalArgumentException("No schema found in YAML file or provided as parameter");
                }
            } else {
                log.info("Using schema URL from YAML: {}", schemaPath);
            }

            schemaString = getSchema(schemaPath);
            if (schemaString == null) {
                return false;
            }

            // Step 2: Load JSON Schema
            ObjectMapper jsonMapper = new ObjectMapper();
            ObjectMapper yamlSchemaMapper = new ObjectMapper(new YAMLFactory());
            JsonNode schemaNode;
            try {
                schemaNode = jsonMapper.readTree(schemaString);
            } catch (Exception e) {
                log.error("Error parsing URL as json,trying yaml: {}", schemaPath);
                // If JSON parsing fails, try YAML
                try {
                    schemaNode = yamlSchemaMapper.readTree(schemaString);
                } catch (JsonProcessingException ex) {
                    throw new RuntimeException(ex);
                }
            }


            // Step 3: Determine schema version from $schema
            String schemaVersion = schemaNode.has("$schema") ? schemaNode.get("$schema").asText() : null;
            JsonSchemaFactory schemaFactory;

            if (schemaVersion != null) {
                // Map $schema to SpecVersion
                SpecVersion.VersionFlag versionFlag = switch (schemaVersion) {
                    case "https://json-schema.org/draft-04/schema#" -> SpecVersion.VersionFlag.V4;
                    case "https://json-schema.org/draft-06/schema#" -> SpecVersion.VersionFlag.V6;
                    case "https://json-schema.org/draft-07/schema#" -> SpecVersion.VersionFlag.V7;
                    case "https://json-schema.org/draft/2019-09/schema" -> SpecVersion.VersionFlag.V201909;
                    case "https://json-schema.org/draft/2020-12/schema" -> SpecVersion.VersionFlag.V202012;
                    default -> {
                        log.error("Unsupported $schema version: {}, defaulting to V202012", schemaVersion);
                        yield SpecVersion.VersionFlag.V202012;
                    }
                };
                schemaFactory = JsonSchemaFactory.getInstance(versionFlag);
            } else {
                // Default to Draft 07 if $schema is missing
                log.warn("$schema not specified, defaulting to V202012");
                schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
            }

            // Step 4: Create JsonSchema and validate

            JsonSchema schema = schemaFactory.getSchema(schemaNode);
            Set<ValidationMessage> errors = schema.validate(yamlNode);

            // Step 5: Output results
            if (errors.isEmpty()) {
                log.info("Validation successful: YAML conforms to the JSON Schema : {})", schemaPath);
            } else {
                log.error("Validation failed with the following errors:");
                for (ValidationMessage error : errors) {
                    log.error(error.getMessage());
                }
                log.error("YAML validation failed");
                return false;
            }

        } catch (IOException e) {
            log.error("I/O Error during validation: {}", e.getMessage());
            return false;
        } catch (RuntimeException e) {
            log.error("Runtime error during validation: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during validation: {}", e.getMessage(), e);
            return false;
        }
        return true;
    }

    private static final int HTTP_CONNECTION_TIMEOUT_SECONDS = 10;
    private static final int HTTP_SUCCESS_STATUS = 200;

    private String getSchema(String schemaPath) {
        if (isHttpUrl(schemaPath)) {
            return fetchSchemaFromUrl(schemaPath);
        } else {
            return readSchemaFromFile(schemaPath);
        }
    }

    private boolean isHttpUrl(String schemaPath) {
        return schemaPath.startsWith("http://") || schemaPath.startsWith("https://");
    }

    private String fetchSchemaFromUrl(String schemaPath) {
        try {
            HttpClient httpClient = createHttpClient();
            HttpRequest httpRequest = createHttpRequest(schemaPath);

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != HTTP_SUCCESS_STATUS) {
                log.error("Error fetching schema from URL {} HTTP request failed with status code: {}",schemaPath,response.statusCode());
                return null;
            }

            return response.body();
        } catch (IOException | InterruptedException e) {
            log.error("Error fetching schema from URL: {}, with Error {}", schemaPath, e.getMessage());
            return null;
        }
    }

    private HttpClient createHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(HTTP_CONNECTION_TIMEOUT_SECONDS))
                .build();
    }

    private HttpRequest createHttpRequest(String schemaPath) {
        return HttpRequest.newBuilder()
                .uri(URI.create(schemaPath))
                .GET()
                .build();
    }

    private String readSchemaFromFile(String schemaPath) {
        try {
            return new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(schemaPath)));
        } catch (IOException e) {
            log.error("Error reading schema file: {}, {}", schemaPath, e.getMessage());
            return null;
        }
    }
}

