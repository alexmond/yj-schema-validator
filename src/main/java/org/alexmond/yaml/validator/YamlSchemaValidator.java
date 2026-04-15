package org.alexmond.yaml.validator;

import com.networknt.schema.*;
import com.networknt.schema.output.OutputUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alexmond.yaml.validator.config.YamlSchemaValidatorConfig;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
    private final YAMLMapper yamlMapper = YAMLMapper.builder().build();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    Map<String, Schema> schemaCache = new HashMap<>();

    /**
     * Validates a YAML file against a JSON Schema.
     *
     * @param filePath   Path to the YAML file to validate
     * @param schemaPath Path to the JSON Schema file (can be local file path or HTTP URL)
     * @return Map containing validation results where key is the file path and value is the validation output
     */
    public Map<String, OutputUnit> validate(String filePath, String schemaPath) {
        try (InputStream is = new FileInputStream(filePath)) {
            return validate(is, filePath, schemaPath);
        } catch (FileNotFoundException e) {
            log.debug("File not found", e);
            return Map.of(filePath, genericError("NoSuchFileException: " + filePath));
        } catch (YamlValidationException | IOException e) {
            log.debug("Error reading file", e);
            return Map.of(filePath, genericError(e.toString()));
        }
    }

    /**
     * Validates an InputStream against a JSON Schema.
     *
     * @param inputStream InputStream of the content to validate
     * @param sourceName  Name of the source (e.g. file path or "stdin")
     * @param schemaPath  Path to the JSON Schema file
     * @return Map containing validation results
     */
    public Map<String, OutputUnit> validate(InputStream inputStream, String sourceName, String schemaPath) {
        List<JsonNode> fileNodeList;
        try {
            fileNodeList = getYamlJsonNode(sourceName, inputStream);
        } catch (YamlValidationException | IOException e) {
            log.debug("Error reading input stream", e);
            return Map.of(sourceName, genericError(e.toString()));
        }

        return switch (fileNodeList.size()) {
            case 0 -> Map.of(sourceName, genericError("No Nodes found in YAML file"));
            case 1 -> Map.of(sourceName, validateJsonNode(sourceName, schemaPath, fileNodeList.get(0)));
            default -> validateMultipleJsonNodes(sourceName, schemaPath, fileNodeList);
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
                Schema schema = getSchemaByPath(schemaPath);
                schema.initializeValidators();
                return schema.validate(fileNode.toString(), InputFormat.JSON, OutputFormat.LIST);
            }
//            SchemaRegistryConfig config = SchemaRegistryConfig.builder()
//                    .formatAssertionsEnabled(true)  // Treat format failures as errors
//                    .build();
//
//            SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12,
//                    builder -> builder.schemaRegistryConfig(config));
//
//            Schema schema = schemaRegistry.getSchema(SchemaLocation.of("schema.json"));
//
//// Per-run override (in validate)
//            List<Error> errors = schema.validate(input, InputFormat.JSON, executionContext -> {
//                executionContext.executionConfig(ExecutionConfig.builder()
//                        .formatAssertionsEnabled(true)  // Enable here if global is false
//                        .build());
//            });
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
    private Schema getSchemaByPath(String schemaPath) {
        if (schemaCache.containsKey(schemaPath)) {
            return schemaCache.get(schemaPath);
        }
        String schemaString = getSchema(schemaPath);
        // Step 2: Load JSON/YAML Schema
        JsonNode schemaNode = getSchemaYamlJsonNode(schemaPath, schemaString);

//         Step 3: Determine schema version from $schema
        SchemaRegistryConfig config = SchemaRegistryConfig.builder()
                .formatAssertionsEnabled(true)
                .build();

        SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12,
                builder -> builder.schemaRegistryConfig(config));

        // Step 4: Create JsonSchema and cache
        Schema schema = schemaRegistry.getSchema(SchemaLocation.of(schemaPath),schemaNode);
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
        } catch (JacksonException e) {
            log.debug("Error parsing schema as JSON, trying YAML: {}, {}", filePath, e.getMessage());
            try {
                jsonNode = yamlMapper.readTree(content);
            } catch (JacksonException ex) {
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
     * @param filePath    Path to the file being parsed (used for error reporting)
     * @param inputStream InputStream of the content to parse
     * @return Parsed JsonNode list
     * @throws YamlValidationException if content cannot be parsed as either JSON or YAML
     */
    private List<JsonNode> getYamlJsonNode(String filePath, InputStream inputStream) throws YamlValidationException, IOException {
        List<JsonNode> docs = new ArrayList<>();
        byte[] content = inputStream.readAllBytes();
        try {
            return List.of(jsonMapper.readTree(content));
        } catch (JacksonException e) {
            log.debug("Error parsing file as JSON, trying YAML: {}, {}", filePath, e.getMessage());
            try {
                // Jackson 3 approach: use readValues() for multi-document YAML
                docs = yamlMapper.readValues(
                    yamlMapper.createParser(content),
                    JsonNode.class
                ).readAll();
                log.debug("Parsed {} YAML documents from {}", docs.size(), filePath);
            } catch (JacksonException ex) {
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
        if (yamlSchemaNode == null || !StringUtils.hasLength(yamlSchemaNode.textValue())) {
            return null;
        }

        String detectedSchemaPath = yamlSchemaNode.textValue();
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
        } catch (IOException | InterruptedException e) {
            String msg = "Error fetching schema from URL: " + schemaPath;
            Throwable cause = e;
            if (e instanceof IOException && e.getCause() != null) {
                cause = e.getCause();
            }
            log.error("{}, {}", msg, cause.getMessage());
            throw new YamlValidationException(cause, null, schemaPath);
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
        try (InputStream is = new FileInputStream(schemaPath)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {
            String msg = "NoSuchFileException: " + schemaPath;
            log.error(msg);
            throw new YamlValidationException(msg, null, schemaPath);
        } catch (IOException e) {
            String msg = "Error reading schema from file: " + schemaPath;
            log.error("{}, {}", msg, e.getMessage());
            throw new YamlValidationException(e, null, schemaPath);
        }
    }
}

