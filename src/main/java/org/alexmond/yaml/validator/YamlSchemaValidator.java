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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * A validator for YAML files against JSON Schema definitions.
 * This component provides functionality to validate YAML/JSON content against JSON Schema specifications,
 * supporting both local file system and HTTP(S) schema sources.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YamlSchemaValidator {
    private static final int HTTP_SUCCESS_STATUS = 200;
    private final YamlSchemaValidatorConfig config;
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final ObjectMapper jsonMapper = new ObjectMapper();
    Map<String, JsonSchema> schemaCache = new HashMap<>();

    /**
     * Validates a YAML file against a JSON Schema.
     *
     * @param filePath   Path to the YAML file to validate
     * @param schemaPath Path to the JSON Schema file (can be local file path or HTTP URL)
     * @return Map containing validation results where key is the file path and value is the validation output
     */
    public Map<String, OutputUnit> validate(String filePath, String schemaPath) {

        List<JsonNode> fileNodeList;
        try {
            fileNodeList = getYamlJsonNode(filePath, Files.readString(Paths.get(filePath)));
        } catch (YamlValidationException | IOException e) { // <---------- from Files.readString, line 44
            log.debug("Error reading file", e); // TODO: fix debug messaging
            return Map.of(filePath, genericError(e.toString()));
        }
        return switch (fileNodeList.size()) {
            case 0 -> Map.of(filePath, genericError("No Nodes found in YAML file"));
            case 1 -> Map.of(filePath, validateJsonNode(filePath, schemaPath, fileNodeList.get(0)));
            default -> validateMultipleJsonNodes(filePath, schemaPath, fileNodeList);
        };
    }

    private Map<String, OutputUnit> validateMultipleJsonNodes(String filePath, String schemaPath, List<JsonNode> fileNodeList) {
        Map<String, OutputUnit> outputUnitMap = new HashMap<>();
        int fileIndex = 0;
        for (JsonNode fileNode : fileNodeList) {
            fileIndex++;
            outputUnitMap.put(filePath + "-" + fileIndex, validateJsonNode(filePath, schemaPath, fileNode));
        }
        return outputUnitMap;
    }

    private OutputUnit validateJsonNode(String filePath, String schemaPath, JsonNode fileNode) {
        try {
            if (!config.isSchemaOverride()) {
                var schemaPathFromNode = getSchemaPathFromNode(filePath, fileNode);
                if (schemaPathFromNode != null) {
                    schemaPath = schemaPathFromNode;
                }
            }
            if (schemaPath == null) {
                return genericError("No schema found in YAML file or provided as parameter");
            } else {
                JsonSchema schema = getSchemaByPath(schemaPath);
                return schema.validate(fileNode.toString(), InputFormat.JSON, OutputFormat.LIST,
                        executionConfiguration -> {
                            executionConfiguration.getExecutionConfig().setAnnotationCollectionFilter(keyword -> true);
                            executionConfiguration.getExecutionConfig().setFormatAssertionsEnabled(true);
                        });
            }
        } catch (IllegalArgumentException |
                 YamlValidationException e) { // TODO: check what it is and where it is coming from and if it is appropriate
            // IllegalArgumentException - from getSchemaPathFromNode
            // YamlValidationException - from getYamlJsonNode, getSchemaByPath
            log.debug("{}", filePath, e);
            return genericError(e.getMessage());
        } catch (Exception e) {
            log.debug("Unexpected Exception", e);
            return genericError(e.getMessage());
        }
    }

    /**
     * Retrieves or creates a JsonSchema instance for the given schema path.
     * Uses cached schema if available, otherwise loads and caches the new schema.
     *
     * @param schemaPath Path to the schema file
     * @return JsonSchema instance for validation
     * @throws YamlValidationException if schema cannot be loaded or parsed
     */
    private JsonSchema getSchemaByPath(String schemaPath) {
        if (schemaCache.containsKey(schemaPath)) {
            return schemaCache.get(schemaPath);
        }
        String schemaString = getSchema(schemaPath);
        // Step 2: Load JSON/YAML Schema
        JsonNode schemaNode = getSchemaYamlJsonNode(schemaPath, schemaString);

//         Step 3: Determine schema version from $schema
        JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(getSchemaVersion(schemaNode));

        // Step 4: Create JsonSchema and cache
        JsonSchema schema = schemaFactory.getSchema(schemaNode);
        schemaCache.put(schemaPath, schema);

        return schema;
    }

    /**
     * Creates a generic error output for validation failures.
     *
     * @param message Error message to include in the output
     * @return Map containing the error output
     */
    private OutputUnit genericError(String message) {
        OutputUnit outputUnit = new OutputUnit();
        outputUnit.setValid(false);
        outputUnit.setErrors(Map.of("error", message));
        return outputUnit;
    }

    /**
     * Detects the JSON Schema version from the schema node.
     * Falls back to V202012 if version cannot be detected.
     *
     * @param schemaNode JSON node containing the schema
     * @return Detected schema version or default version if detection fails
     */
    public SpecVersion.VersionFlag getSchemaVersion(JsonNode schemaNode) {
        SpecVersion.VersionFlag defaultVersion = SpecVersion.VersionFlag.V202012;
        try {
            return SpecVersionDetector.detect(schemaNode);
        } catch (JsonSchemaException e) {
            log.warn("Error detecting schema version: {}, setting default to {}", e.getMessage(), defaultVersion);
            return defaultVersion;
        }
    }

    /**
     * Parses content as either JSON or YAML into a JsonNode.
     * Attempts JSON parsing first, falls back to YAML if JSON parsing fails.
     *
     * @param filePath Path to the file being parsed (used for error reporting)
     * @param content  String content to parse
     * @return Parsed JsonNode
     * @throws YamlValidationException if content cannot be parsed as either JSON or YAML
     */
    private JsonNode getSchemaYamlJsonNode(String filePath, String content) {
        JsonNode jsonNode;
        try {
            return jsonMapper.readTree(content);
        } catch (JsonProcessingException e) {
            log.debug("Error parsing schema as JSON, trying YAML: {}, {}", filePath, e.getMessage());
            try {
                jsonNode = yamlMapper.readTree(content);
            } catch (JsonProcessingException ex) {
                log.debug("Error parsing schema as YAML: {}, {}", filePath, ex.getMessage());
                throw new YamlValidationException(ex, null, filePath);
            }
        }
        return jsonNode;
    }

    /**
     * Parses content as either JSON or YAML into a JsonNode.
     * Attempts JSON parsing first, falls back to YAML if JSON parsing fails.
     *
     * @param filePath Path to the file being parsed (used for error reporting)
     * @param content  String content to parse
     * @return Parsed JsonNode
     * @throws YamlValidationException if content cannot be parsed as either JSON or YAML
     */
    private List<JsonNode> getYamlJsonNode(String filePath, String content) throws YamlValidationException, IOException {
        List<JsonNode> docs = new ArrayList<>();
        try {
            return List.of(jsonMapper.readTree(content));
        } catch (JsonProcessingException e) {
            log.debug("Error parsing file as JSON, trying YAML: {}, {}", filePath, e.getMessage());
            try (var parser = yamlMapper.createParser(content)) {
                while (parser.nextToken() != null) {
                    JsonNode doc = yamlMapper.readTree(parser);
                    if (doc != null && !doc.isMissingNode()) docs.add(doc);
                }
            } catch (JsonProcessingException ex) {
                log.debug("Error parsing file as YAML: {}, {}", filePath, ex.getMessage());
                throw new YamlValidationException(ex, null, filePath);
            }
        }
        return docs;
    }

    /**
     * Extracts schema path from the $schema field in the YAML/JSON content.
     * Resolves relative paths against the YAML file location.
     *
     * @param yamlPath Path to the YAML file being validated
     * @param jsonNode Parsed content of the YAML file
     * @return Resolved schema path or null if no schema specified
     */
    private String getSchemaPathFromNode(String yamlPath, JsonNode jsonNode) {
        JsonNode yamlSchemaNode = jsonNode.get("$schema");
        if (yamlSchemaNode == null || !StringUtils.hasLength(yamlSchemaNode.asText())) {
            return null;
        }

        String detectedSchemaPath = yamlSchemaNode.asText();
        log.debug("Using schema URL from YAML: {}", detectedSchemaPath);
        if (!isHttpUrl(detectedSchemaPath)) {
            detectedSchemaPath = new File(new File(yamlPath).getParentFile(), detectedSchemaPath).getPath();
        }
        return detectedSchemaPath;
    }

    /**
     * Retrieves schema content from either a local file or HTTP URL.
     *
     * @param schemaPath Path or URL to the schema
     * @return Schema content as string
     * @throws YamlValidationException if schema cannot be retrieved
     */
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

    /**
     * Fetches schema content from a HTTP(S) URL.
     * Supports SSL certificate validation bypass if configured.
     *
     * @param schemaPath URL to fetch the schema from
     * @return Schema content as string
     * @throws YamlValidationException if schema cannot be fetched
     */
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

    /**
     * Reads schema content from a local file.
     *
     * @param schemaPath Path to the schema file
     * @return Schema content as string
     * @throws YamlValidationException if file cannot be read
     */
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

