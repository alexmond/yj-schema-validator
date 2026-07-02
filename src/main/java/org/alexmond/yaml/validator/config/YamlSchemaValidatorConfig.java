package org.alexmond.yaml.validator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Configuration properties for YAML schema validation. Properties are bound to the
 * 'validator' prefix in application properties. Contains settings for schema validation,
 * report generation, and HTTP client configuration.
 *
 * @since 1.0
 */
@Component
@Data
@ConfigurationProperties
public class YamlSchemaValidatorConfig {

	/**
	 * Array of file paths to validate against the schema. Can contain both YAML and JSON
	 * files.
	 *
	 * @since 1.0
	 */
	private List<String> files;

	/**
	 * Path to the JSON schema in either JSON or YAML file format used for validation. Can
	 * be either a local file path or URL.
	 */
	private String schema;

	/**
	 * HTTP timeout in seconds for fetching remote schemas.
	 *
	 * @value 10 seconds
	 * @since 1.0
	 */
	private Duration httpTimeout = Duration.ofSeconds(10);

	/**
	 * Flag to override a schema path specified in the YAML / JSON file. If true, uses
	 * schemaPath property instead of $schema from YAML.
	 *
	 * @value false
	 * @since 1.0
	 */
	private boolean schemaOverride;

	/**
	 * Type of validation report to generate.
	 *
	 * @value TEXT
	 * @since 1.0
	 */
	private ReportType reportType = ReportType.TEXT;

	/**
	 * Name of the file to write the validation report to. Only used when reportType is
	 * not TEXT.
	 *
	 * @since 1.0
	 */
	private String reportFileName;

	/**
	 * Flag to control whether SSL certificate validation errors should be ignored. When
	 * true, SSL certificate validation errors will be ignored during HTTP requests.
	 *
	 * @value true
	 * @since 1.0
	 */
	private boolean ignoreSslErrors;

	/**
	 * Flag to control whether to use colored output in the console. When true, validation
	 * results will be displayed with ANSI color codes.
	 *
	 * @value true
	 * @since 1.0
	 */
	private boolean color = true;

	/**
	 * Flag to enable path-based schema autodetection via the JSON Schema Store catalog.
	 * Only consulted when no schema is supplied and none is declared in the file. Fails
	 * soft: if no catalog pattern matches, validation falls back to the "no schema"
	 * behaviour.
	 *
	 * @value true
	 */
	private boolean autoDetect = true;

	/**
	 * URL of the JSON Schema Store catalog used for autodetection. A snapshot is bundled
	 * in the jar and used as a fallback when this URL cannot be fetched. Set to empty to
	 * skip the live fetch and always use the bundled snapshot.
	 */
	private String catalogUrl = "https://www.schemastore.org/api/json/catalog.json";

	/**
	 * Flag controlling the shape of the {@code LLM} report. When true, emits compact
	 * compiler-style diagnostic lines; when false, emits structured JSON. Ignored for
	 * other report types.
	 *
	 * @value false
	 */
	private boolean compact;

}
