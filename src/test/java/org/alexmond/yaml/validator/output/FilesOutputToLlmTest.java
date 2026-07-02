package org.alexmond.yaml.validator.output;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.networknt.schema.output.OutputUnit;
import org.alexmond.yaml.validator.YamlSchemaValidator;
import org.alexmond.yaml.validator.catalog.SchemaCatalogLoader;
import org.alexmond.yaml.validator.catalog.SchemaDetector;
import org.alexmond.yaml.validator.config.YamlSchemaValidatorConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilesOutputToLlmTest {

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	@Test
	void jsonReportHasSummaryAndFlatErrors() {
		OutputUnit validUnit = new OutputUnit();
		validUnit.setValid(true);
		OutputUnit errorUnit = new OutputUnit();
		errorUnit.setValid(false);
		errorUnit.setErrors(Map.of("error", "boom"));

		Map<String, OutputUnit> files = new LinkedHashMap<>();
		files.put("a.yaml", validUnit);
		files.put("b.yaml", errorUnit);

		JsonNode root = jsonMapper.readTree(new FilesOutput(files).toLlmString(false));

		assertEquals(2, root.get("summary").get("files").asInt());
		assertEquals(1, root.get("summary").get("valid").asInt());
		assertEquals(1, root.get("summary").get("invalid").asInt());
		assertEquals(1, root.get("summary").get("errors").asInt());

		JsonNode second = root.get("results").get(1);
		assertEquals("b.yaml", second.get("file").asString());
		assertFalse(second.get("valid").asBoolean());
		assertEquals("error", second.get("errors").get(0).get("keyword").asString());
		assertEquals("boom", second.get("errors").get(0).get("message").asString());
	}

	@Test
	void compactReportEmitsSummaryHeaderAndOneLinePerError() {
		OutputUnit validUnit = new OutputUnit();
		validUnit.setValid(true);
		OutputUnit errorUnit = new OutputUnit();
		errorUnit.setValid(false);
		errorUnit.setErrors(Map.of("error", "boom"));

		Map<String, OutputUnit> files = new LinkedHashMap<>();
		files.put("a.yaml", validUnit);
		files.put("b.yaml", errorUnit);

		String compact = new FilesOutput(files).toLlmString(true);

		assertTrue(compact.startsWith("# 1/2 files valid, 1 errors"), compact);
		assertTrue(compact.contains("b.yaml: [error]: boom"), compact);
	}

	@Test
	void jsonReportLocalisesSchemaViolations(@TempDir Path dir) throws Exception {
		Path schema = dir.resolve("schema.json");
		Files.writeString(schema, """
				{ "type": "object", "required": ["age"],
				  "properties": { "name": { "type": "string" }, "age": { "type": "integer" } } }
				""");
		Path doc = dir.resolve("bad.yaml");
		Files.writeString(doc, "name: 42\n");

		Map<String, OutputUnit> result = newValidator().validate(doc.toString(), schema.toString());
		String json = new FilesOutput(result).toLlmString(false);
		JsonNode root = jsonMapper.readTree(json);

		assertEquals(1, root.get("summary").get("invalid").asInt());
		JsonNode errors = root.get("results").get(0).get("errors");
		assertTrue(errors.size() > 0, json);
		// per-instance violations carry a JSON pointer, a keyword and the schema location
		assertTrue(json.contains("schemaLocation"), json);
		assertTrue(errors.get(0).has("pointer"), json);
		assertTrue(errors.get(0).has("keyword"), json);
	}

	private YamlSchemaValidator newValidator() {
		YamlSchemaValidatorConfig config = new YamlSchemaValidatorConfig();
		config.setAutoDetect(false);
		return new YamlSchemaValidator(config, new SchemaDetector(config, new SchemaCatalogLoader(config)));
	}

}
