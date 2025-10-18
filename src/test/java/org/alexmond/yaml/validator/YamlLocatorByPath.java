package org.alexmond.yaml.validator;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.parser.Parser;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.resolver.Resolver;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public  class YamlLocatorByPath {

    public static final class Location {
        public final int line;     // 1-based
        public final int column;   // 1-based
        public Location(int line, int column) { this.line = line; this.column = column; }
        @Override public String toString() { return "line=" + line + ", col=" + column; }
    }

    public Optional<Location> find(String dottedPath, Path yamlFile) throws Exception {
        try (Reader r = Files.newBufferedReader(yamlFile)) {
            // SnakeYAML 2.x: create ParserImpl from StreamReader and LoaderOptions
            LoaderOptions options = new LoaderOptions();
            StreamReader reader = new StreamReader(r);
            Parser parser = new ParserImpl(reader, options);

            // Composer requires Parser, Resolver, and LoaderOptions in SnakeYAML 2.x
            Composer composer = new Composer(parser, new Resolver(), options);

            Node root = composer.getSingleNode();
            if (root == null) return Optional.empty();
            String[] parts = dottedPath.split("\\.");

            Node cur = root;
            for (String part : parts) {
                if (!(cur instanceof MappingNode)) return Optional.empty();
                MappingNode map = (MappingNode) cur;
                Node next = null;
                for (NodeTuple t : map.getValue()) {
                    Node keyNode = t.getKeyNode();
                    if (keyNode instanceof ScalarNode scalar && scalar.getValue().equals(part)) {
                        next = t.getValueNode();
                        break;
                    }
                }
                if (next == null) return Optional.empty();
                cur = next;
            }

            Mark start = cur.getStartMark();
            if (start == null) return Optional.empty();
            // SnakeYAML marks are 0-based; convert to 1-based
            return Optional.of(new Location(start.getLine() + 1, start.getColumn() + 1));
        }
    }

    @Test
    public void LocatorTest() throws Exception {
        Path yaml = Path.of("src/test/resources/invalid.yaml");
        String path = "sample.boolean-sample";
        Optional<Location> loc = find(path, yaml);
        System.out.println(loc.map(Object::toString).orElse("Not found"));
    }
}