package org.alexmond.yaml.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.*;
import com.networknt.schema.output.OutputUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alexmond.yaml.validator.config.YamlSchemaValidatorConfig;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class YamlSchemaValidator {
    private final YamlSchemaValidatorConfig config;
    Map<String, JsonSchema> schemaCache = new HashMap<>();
    
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final ObjectMapper jsonMapper = new ObjectMapper();

    public Boolean validate(String yamlPath, String schemaPath) {
        // Step 1: Parse YAML to JSON
        JsonNode yamlNode;
        try {
            yamlNode = yamlMapper.readTree(new File(yamlPath));
            if (schemaPath != null) {
                log.info("Using schema URL param: {}", schemaPath);
            } else {
                schemaPath = getSchemaPathFromNode(yamlPath, yamlNode);
            }
        } catch (IOException e) {
            log.error("Error reading or parsing YAML file: {}, {}", yamlPath, e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.error("Error determining schema path: {}", e.getMessage());
            return false;
        }
        JsonSchema schema;
        if(!schemaCache.containsKey(schemaPath)) {
            String schemaString = getSchema(schemaPath);
            if (schemaString == null) {
                return false;
            }

            // Step 2: Load JSON Schema
            JsonNode schemaNode = getSchemaNode(schemaPath, schemaString);
            if (schemaNode == null) return false;

//         Step 3: Determine schema version from $schema
            JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(getSchemaVersion(schemaNode));

            // Step 4: Create JsonSchema and validate

            schema = schemaFactory.getSchema(schemaNode);
            schemaCache.put(schemaPath, schema);
        } else {
            schema = schemaCache.get(schemaPath);
        }

        String jsonstring = yamlNode.toString();
        log.error("Validating YAML schema: {}", jsonstring);

        OutputUnit outputUnit = schema.validate(yamlNode.toString(), InputFormat.JSON, OutputFormat.LIST, executionConfiguration -> {
//            executionConfiguration.getExecutionConfig().setAnnotationCollectionEnabled(true);
            executionConfiguration.getExecutionConfig().setAnnotationCollectionFilter(keyword -> true);
            executionConfiguration.getExecutionConfig().setFormatAssertionsEnabled(true);
        });

        Set<ValidationMessage> errors = schema.validate(yamlNode);
        log.warn("YAML schema validation result: {}", outputUnit.toString());

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

        return true;
    }

    public SpecVersion.VersionFlag getSchemaVersion(JsonNode schemaNode) {
        SpecVersion.VersionFlag defaultVersion = SpecVersion.VersionFlag.V202012;
        try {
            return SpecVersionDetector.detect(schemaNode);
        }catch (JsonSchemaException e) {
            log.warn("Error detecting schema version: {}, setting default to {}", e.getMessage(),defaultVersion);
            return defaultVersion;
        }
    }

    private JsonNode getSchemaNode(String schemaPath, String schemaString) {
        JsonNode schemaNode;
        try {
            schemaNode = jsonMapper.readTree(schemaString);
        } catch (JsonProcessingException e) {
            // todo fix message
            log.error("Error parsing URL as json, trying yaml: {}", schemaPath);
            // If JSON parsing fails, try YAML
            try {
                schemaNode = yamlMapper.readTree(schemaString);
            } catch (JsonProcessingException ex) {
                // todo fix message
                log.error("Error parsing schema file as YAML: {}, {}", schemaPath, ex.getMessage());
                return null;
            }
        }
        return schemaNode;
    }

    private String getSchemaPathFromNode(String yamlPath, JsonNode jsonNode) {
        // Check if YAML has schema URL
        JsonNode yamlSchemaNode = jsonNode.get("$schema");
        if (yamlSchemaNode == null && !StringUtils.hasLength(yamlSchemaNode.asText())) {
            throw new IllegalArgumentException("No schema found in YAML file or provided as parameter");
        }

        String schemaPath = yamlSchemaNode.asText();
        log.info("Using schema URL from YAML: {}", schemaPath);
        if (!isHttpUrl(schemaPath)) {
            schemaPath = new File(new File(yamlPath).getParentFile(), schemaPath).getPath();
        }
        return schemaPath;
    }

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

    private static final int HTTP_SUCCESS_STATUS = 200;

    private String fetchSchemaFromUrl(String schemaPath) {
        try {
            HttpClient httpClient = createHttpClient();

            HttpRequest httpRequest = createHttpRequest(schemaPath);

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != HTTP_SUCCESS_STATUS) {
                log.error("Error fetching schema from URL {} HTTP request failed with status code: {}", schemaPath, response.statusCode());
                return null;
            }

            return response.body();
        } catch (IOException | InterruptedException e) {
            log.error("Error fetching schema from URL: {}, with Error {}", schemaPath, e.getMessage());
            return null;
        }
    }

    private HttpClient createHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(config.getHttpTimeout());

        if (config.isIgnoreSslErrors()) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }}, new SecureRandom());

                builder.sslContext(sslContext)
                        .sslParameters(new SSLParameters());
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                log.warn("Failed to initialize SSL context for ignoring certificate validation: {}", e.getMessage());
            }
        }

        return builder.build();
    }

    private HttpRequest createHttpRequest(String schemaPath) {
        return HttpRequest.newBuilder()
                .uri(URI.create(schemaPath))
                .GET()
                .build();
    }

    private String readSchemaFromFile(String schemaPath) {
        try {
            return new String(Files.readAllBytes(Paths.get(schemaPath)));
        } catch (IOException e) {
            log.error("Error reading schema file: {}, {}", schemaPath, e.getMessage());
            return null;
        }
    }
}

