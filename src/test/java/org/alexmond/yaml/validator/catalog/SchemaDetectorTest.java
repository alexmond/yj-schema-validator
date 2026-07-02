package org.alexmond.yaml.validator.catalog;

import org.alexmond.yaml.validator.config.YamlSchemaValidatorConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises path-based detection against the real bundled catalog snapshot, offline
 * (catalogUrl is blank so the live fetch is skipped).
 */
class SchemaDetectorTest {

	private SchemaDetector detector(boolean autoDetect) {
		YamlSchemaValidatorConfig config = new YamlSchemaValidatorConfig();
		config.setAutoDetect(autoDetect);
		config.setCatalogUrl("");
		return new SchemaDetector(config, new SchemaCatalogLoader(config));
	}

	@Test
	void detectsGithubWorkflowByPath() {
		String url = detector(true).detect(".github/workflows/ci.yml");
		assertNotNull(url);
		assertTrue(url.contains("github-workflow"), url);
	}

	@Test
	void detectsDockerComposeByName() {
		assertNotNull(detector(true).detect("docker-compose.yml"));
	}

	@Test
	void detectsHelmChartByBasename() {
		String url = detector(true).detect("charts/mychart/Chart.yaml");
		assertNotNull(url);
		assertTrue(url.toLowerCase().contains("chart"), url);
	}

	@Test
	void returnsNullForUnknownPath() {
		assertNull(detector(true).detect("some/random/file.txt"));
	}

	@Test
	void returnsNullWhenDisabled() {
		assertNull(detector(false).detect(".github/workflows/ci.yml"));
	}

}
