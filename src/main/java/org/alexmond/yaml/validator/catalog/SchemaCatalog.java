package org.alexmond.yaml.validator.catalog;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * The JSON Schema Store catalog: the list of {@link CatalogEntry} known schemas used for
 * path-based schema autodetection.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SchemaCatalog {

	private List<CatalogEntry> schemas;

}
