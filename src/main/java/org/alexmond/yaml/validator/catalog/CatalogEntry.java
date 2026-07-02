package org.alexmond.yaml.validator.catalog;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * A single entry in the JSON Schema Store catalog: a named schema with the file-name glob
 * patterns it applies to. Mirrors the structure served at
 * {@code https://www.schemastore.org/api/json/catalog.json}.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogEntry {

	private String name;

	private String description;

	/**
	 * Glob patterns (e.g. {@code **}{@code /.github/workflows/*.yml}) that a file path is
	 * matched against to select this schema.
	 */
	private List<String> fileMatch;

	/**
	 * URL of the JSON Schema to validate matching files against.
	 */
	private String url;

}
