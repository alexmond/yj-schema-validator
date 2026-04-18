package org.alexmond.yaml.validator.testimpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Alternative SnakyYaml Multi Yaml paraser
 */

public class MultiYamlParser {

	private final Yaml yaml = new Yaml(); // Thread-safe; reuse instance

	private final ObjectMapper objectMapper = new ObjectMapper(); // For converting to
																	// JsonNode

	/**
	 * Parses multi-document YAML content (as a string) into a list of JsonNode documents.
	 * Each document is converted to a JsonNode for easy JSON/YAML handling.
	 * @param yamlContent The full YAML content as a string
	 * @return List of parsed JsonNode documents
	 */
	public List<JsonNode> parseMultiYaml(String yamlContent) {
		try {
			Iterator<Object> iterator = yaml.loadAll(new StringReader(yamlContent)).iterator();
			// Convert iterator to list, filtering out null/empty docs
			List<JsonNode> docs = new ArrayList<>();
			while (iterator.hasNext()) {
				Object doc = iterator.next();
				if (doc != null && !isEmptyDoc(doc)) {
					docs.add(objectMapper.valueToTree(doc));
				}
			}
			return docs;
		}
		catch (YAMLException ex) {
			// Handle parsing errors (e.g., log or throw)
			throw new RuntimeException("YAML parsing error: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Checks if a document is empty (e.g., just ---).
	 */
	private boolean isEmptyDoc(Object doc) {
		if (doc instanceof Map && ((Map<?, ?>) doc).isEmpty()) {
			return true;
		}
		if (doc instanceof List && ((List<?>) doc).isEmpty()) {
			return true;
		}
		return false;
	}

	@Test
	public void LocatorTest() throws Exception {
		MultiYamlParser parser = new MultiYamlParser();
		String yaml = "src/test/resources/valid.yaml";
		try {
			String yamlContent = java.nio.file.Files.readString(java.nio.file.Paths.get(yaml));
			List<JsonNode> docs = parser.parseMultiYaml(yamlContent);
			assertEquals(2, docs.size());
		}
		catch (IOException ex) {
			System.err.println("Error reading file: " + ex.getMessage());
		}
	}

}
