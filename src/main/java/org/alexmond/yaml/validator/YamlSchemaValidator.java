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

@Slf4j
@Component
@RequiredArgsConstructor
public class YamlSchemaValidator {
    private final YamlSchemaValidatorConfig config;
    Map<String, JsonSchema> schemaCache = new HashMap<>();

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final ObjectMapper jsonMapper = new ObjectMapper();

    public Map<String, OutputUnit> validate(String filePath, String schemaPath) {
        // Step 1: Parse YAML to JSON
        try {
            JsonNode fileNode = getYamlJsonNode(filePath, Files.readString(Paths.get(filePath)));
            if (!config.getSchemaPathOverride()) {
                var schemaPathFromNode = getSchemaPathFromNode(filePath, fileNode);
                if (schemaPathFromNode != null) {
                    schemaPath = schemaPathFromNode;
                }
                if (schemaPath == null) {
                    return genericError(filePath, "No schema found in YAML file or provided as parameter");
                }
            }
            JsonSchema schema = getSchemaByPath(schemaPath);

            OutputUnit outputUnit = schema.validate(fileNode.toString(), InputFormat.JSON, OutputFormat.LIST, executionConfiguration -> {
                executionConfiguration.getExecutionConfig().setAnnotationCollectionFilter(keyword -> true);
                executionConfiguration.getExecutionConfig().setFormatAssertionsEnabled(true);
            });

            log.debug("Validation successful: YAML conforms to the JSON Schema: {}", schemaPath);
            return Map.of(filePath, outputUnit);
        } catch (IOException e) { // <---------- from Files.readString, line 44
            log.debug("Error reading file", e); // TODO: fix debug messaging
            return genericError(filePath, e.toString());
        } catch (IllegalArgumentException | YamlValidationException e) { // TODO: check what it is and where it is coming from and if it is appropriate
            // IllegalArgumentException - from getSchemaPathFromNode
            // YamlValidationException - from getYamlJsonNode, getSchemaByPath
            log.debug("{}", filePath, e);
            return genericError(filePath, e.getMessage());
        } catch (Exception e) {
            log.debug("Unexpected Exception", e);
            return genericError(filePath, e.getMessage());
        }
    }

    private JsonSchema getSchemaByPath(String schemaPath) {
        if (schemaCache.containsKey(schemaPath)) {
            return schemaCache.get(schemaPath);
        }
        String schemaString = getSchema(schemaPath);
        // Step 2: Load JSON/YAML Schema
        JsonNode schemaNode = getYamlJsonNode(schemaPath, schemaString);

//         Step 3: Determine schema version from $schema
        JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(getSchemaVersion(schemaNode));

        // Step 4: Create JsonSchema and cache
        JsonSchema schema = schemaFactory.getSchema(schemaNode);
        schemaCache.put(schemaPath, schema);

        return schema;
    }

    private Map<String, OutputUnit> genericError(String filePath, String message) {
        OutputUnit outputUnit = new OutputUnit();
        outputUnit.setValid(false);
        outputUnit.setErrors(Map.of("error", message));
        return Map.of(filePath, outputUnit);
    }

    public SpecVersion.VersionFlag getSchemaVersion(JsonNode schemaNode) {
        SpecVersion.VersionFlag defaultVersion = SpecVersion.VersionFlag.V202012;
        try {
            return SpecVersionDetector.detect(schemaNode);
        } catch (JsonSchemaException e) {
            log.warn("Error detecting schema version: {}, setting default to {}", e.getMessage(), defaultVersion);
            return defaultVersion;
        }
    }

    private JsonNode getYamlJsonNode(String filePath, String content){
        JsonNode schemaNode;
        try {
            return jsonMapper.readTree(content);
        } catch (JsonProcessingException e) {
            log.debug("Error parsing schema as JSON, trying YAML: {}, {}", filePath, e.getMessage());
            try {
                schemaNode = yamlMapper.readTree(content);
            } catch (JsonProcessingException ex) {
                log.debug("Error parsing schema as YAML: {}, {}", filePath, ex.getMessage());
                throw new YamlValidationException(ex, null, filePath);
            }
        }
        return schemaNode;
    }

    private String getSchemaPathFromNode(String yamlPath, JsonNode jsonNode) {
        JsonNode yamlSchemaNode = jsonNode.get("$schema");
        if (yamlSchemaNode == null || !StringUtils.hasLength(yamlSchemaNode.asText())) {
            throw new IllegalArgumentException("No schema found in YAML file or provided as parameter");
        }

        String detectedSchemaPath = yamlSchemaNode.asText();
        log.debug("Using schema URL from YAML: {}", detectedSchemaPath);
        if (!isHttpUrl(detectedSchemaPath)) {
            detectedSchemaPath = new File(new File(yamlPath).getParentFile(), detectedSchemaPath).getPath();
        }
        return detectedSchemaPath;
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
                String msg = "HTTP request failed with status code " + response.statusCode() + " for " + schemaPath;
                log.debug(msg);
                throw new YamlValidationException(msg, null, schemaPath);
            }

            return response.body();
        } catch (IOException | InterruptedException e) { // TODO: fix debug messaging
            String msg = "Error fetching schema from URL: " + schemaPath;
            log.error("{}, {}", msg, e.getMessage());
            throw new YamlValidationException(e, null, schemaPath);
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
            return Files.readString(Paths.get(schemaPath));
        } catch (IOException e) {
            String msg = "Error reading schema file";
            log.error("{}, {}", msg, e.getMessage());
            throw new YamlValidationException(e, null, schemaPath);
        }
    }
}

