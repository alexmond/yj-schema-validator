package org.alexmond.yaml.validator;

import com.networknt.schema.output.OutputUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public class YamlSchemaValidatorStdinTest {

    @Autowired
    private YamlSchemaValidator yamlSchemaValidator;

    @Test
    void shouldValidateStdinSuccessfully() {
        String yamlContent = """
                name: "test"
                version: 1.0
                """;
        String schemaPath = "src/test/resources/testdata/sample-schema.json";
        
        ByteArrayInputStream bais = new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8));
        
        Map<String, OutputUnit> results = yamlSchemaValidator.validate(bais, "stdin", schemaPath);
        
        assertTrue(results.containsKey("stdin"));
        assertTrue(results.get("stdin").isValid());
    }
}
