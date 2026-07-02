package org.alexmond.yaml.validator.catalog;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alexmond.yaml.validator.config.YamlSchemaValidatorConfig;

import org.springframework.stereotype.Component;

/**
 * Resolves the JSON Schema for a file from its path, using the JSON Schema Store catalog
 * (the same mechanism IDEs use). Only consulted when no schema was given on the command
 * line and none is declared in the file. When several catalog patterns match, the most
 * specific one (most literal characters) wins.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaDetector {

	private final YamlSchemaValidatorConfig config;

	private final SchemaCatalogLoader catalogLoader;

	private List<CompiledEntry> compiledEntries;

	/**
	 * Detects the schema URL for a file path.
	 * @param path the path of the file being validated
	 * @return the detected schema URL, or null if autodetection is disabled or no catalog
	 * pattern matches
	 */
	public String detect(String path) {
		if (!this.config.isAutoDetect() || path == null) {
			return null;
		}
		String bestUrl = null;
		int bestSpecificity = -1;
		for (CompiledEntry entry : compiled()) {
			if (entry.glob().specificity() > bestSpecificity && entry.glob().matches(path)) {
				bestSpecificity = entry.glob().specificity();
				bestUrl = entry.url();
			}
		}
		if (bestUrl != null) {
			log.debug("Autodetected schema {} for {}", bestUrl, path);
		}
		return bestUrl;
	}

	private List<CompiledEntry> compiled() {
		if (this.compiledEntries == null) {
			List<CompiledEntry> entries = new ArrayList<>();
			SchemaCatalog catalog = this.catalogLoader.load();
			if (catalog.getSchemas() != null) {
				for (CatalogEntry catalogEntry : catalog.getSchemas()) {
					if (catalogEntry.getUrl() == null || catalogEntry.getFileMatch() == null) {
						continue;
					}
					for (String glob : catalogEntry.getFileMatch()) {
						entries.add(new CompiledEntry(GlobPattern.compile(glob), catalogEntry.getUrl()));
					}
				}
			}
			log.debug("Compiled {} catalog file-match patterns for autodetection", entries.size());
			this.compiledEntries = entries;
		}
		return this.compiledEntries;
	}

	private record CompiledEntry(GlobPattern glob, String url) {
	}

}
