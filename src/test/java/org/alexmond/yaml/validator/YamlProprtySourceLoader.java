package org.alexmond.yaml.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.util.*;

@Slf4j
public class YamlProprtySourceLoader {

    @ParameterizedTest
    @CsvSource({
//            "src/test/resources/valid.yaml",
            "src/test/resources/validParam.yaml"
    })
    void testSnakeYamlValidation(String yamlPath) throws Exception {

        Resource resource = new FileSystemResource(yamlPath);
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> sources = loader.load("config", resource);

        // Create mutable sources and add custom properties
        MutablePropertySources mutableSources = new MutablePropertySources();
        sources.forEach(mutableSources::addLast);  // YAML sources first
        // Resolve placeholders
        PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(mutableSources);
        resolver.setIgnoreUnresolvableNestedPlaceholders(true);  // Optional: Ignore nested if unresolved

        // Access resolved values (as if from Spring's Environment)
        log.info("sample: {}", resolver.getProperty("sample.boolean-sample").toString());
        log.info("sample2: {}", resolver.getProperty("sample.boolean-sample2").toString());

        JsonNode node;
        PropertiesToJson propertiesToJson = new PropertiesToJson();
        node = propertiesToJson.toJson(resolver,mutableSources);
        log.info("node: {}", node.toString());
    }

}
