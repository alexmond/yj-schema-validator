package org.alexmond.yaml.validator;// Java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PropertiesToJson {

    private static final Pattern TOKEN = Pattern.compile("([^.\\[]+)(\\[[0-9]+])*(?:\\.|$)");
    private static final Pattern INDEX = Pattern.compile("\\[([0-9]+)]");

    public  JsonNode toJson(PropertySourcesPropertyResolver resolver, MutablePropertySources sources) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        Collection<String> propertyNames = collectPropertyNames(sources);

        for (String fullKey : propertyNames) {
            String raw = resolver.getProperty(fullKey);
            if (raw == null) continue;

            insert(root, fullKey, coerce(mapper, raw), mapper);
        }
        return root;
    }

    public Collection<String> collectPropertyNames(MutablePropertySources sources) {
        Set<String> names = new LinkedHashSet<>();
        for (PropertySource<?> ps : sources) {
            if (ps instanceof EnumerablePropertySource<?> eps) {
                names.addAll(Arrays.asList(eps.getPropertyNames()));
            }
        }
        return names;
    }

    private  void insert(ObjectNode root, String key, JsonNode value, ObjectMapper mapper) {
        ObjectNode currentObj = root;
        ArrayNode currentArr = null;
        String pendingLeaf = null;

        int pos = 0;
        while (pos < key.length()) {
            Matcher m = TOKEN.matcher(key);
            if (!m.find(pos) || m.start() != pos) return; // invalid key format
            pos = m.end();

            String name = m.group(1);
            String bracketPart = key.substring(m.start(), m.end());

            // Parse all indices for this token
            Matcher idxMatcher = INDEX.matcher(bracketPart);
            boolean hadIndex = false;

            // First, ensure we are on an object to access "name"
            if (currentArr != null) {
                // We just resolved an array element; now we expect a field name after that element
                currentObj = ensureObject(currentArr, -1, mapper); // -1 means don't index; we already pointed to element
                currentArr = null;
            }

            // Ensure object node for the current name
            JsonNode existing = currentObj.get(name);
            ObjectNode baseObjectForName = null;
            ArrayNode baseArrayForName = null;

            hadIndex = idxMatcher.find();
            if (!hadIndex) {
                // No index on this segment
                if (pos < key.length()) {
                    // Intermediate segment must be object
                    if (existing == null || !existing.isObject()) {
                        existing = mapper.createObjectNode();
                        currentObj.set(name, existing);
                    }
                    currentObj = (ObjectNode) existing;
                    pendingLeaf = null;
                } else {
                    // Leaf with no index
                    currentObj.set(name, value);
                    return;
                }
            } else {
                // Segment includes one or more indices, treat the field as an array root
                if (existing == null || !existing.isArray()) {
                    existing = mapper.createArrayNode();
                    currentObj.set(name, existing);
                }
                ArrayNode arr = (ArrayNode) existing;

                // first index already found; process it and any additional indices chained
                int index = Integer.parseInt(idxMatcher.group(1));
                ensureArraySize(arr, index);
                JsonNode element = arr.get(index);
                if (element == null || element.isNull()) {
                    // If more to come, prepare container, else set value directly later
                    element = mapper.createObjectNode(); // default to object container for further navigation
                    arr.set(index, element);
                }

                // Process additional [i] in the same token
                while (idxMatcher.find()) {
                    int nextIdx = Integer.parseInt(idxMatcher.group(1));
                    // The current element must be an array to index into
                    if (!element.isArray()) {
                        ArrayNode newArr = mapper.createArrayNode();
                        // If element had content, we lose it; keys with mixed shapes are ambiguous. Keep simple.
                        arr.set(index, newArr);
                        element = newArr;
                    }
                    ArrayNode nestedArr = (ArrayNode) element;
                    ensureArraySize(nestedArr, nextIdx);
                    JsonNode nestedEl = nestedArr.get(nextIdx);
                    if (nestedEl == null || nestedEl.isNull()) {
                        nestedEl = mapper.createObjectNode();
                        nestedArr.set(nextIdx, nestedEl);
                    }
                    index = nextIdx;
                    element = nestedEl;
                    arr = nestedArr;
                }

                // Now element is the target container for this token
                if (pos < key.length()) {
                    // More tokens follow; element should be an object container
                    if (!element.isObject()) {
                        ObjectNode newObj = mapper.createObjectNode();
                        // Replace only if not object
                        if (arr.get(index) == null || !arr.get(index).isObject()) {
                            arr.set(index, newObj);
                        }
                        element = newObj;
                    }
                    currentArr = null;
                    currentObj = (ObjectNode) element;
                    pendingLeaf = null;
                } else {
                    // Leaf: set the value at this array position
                    arr.set(index, value);
                    return;
                }
            }
        }
        // Fallback in case of unexpected path termination
        if (pendingLeaf != null) currentObj.set(pendingLeaf, value);
    }

    private  void ensureArraySize(ArrayNode array, int index) {
        while (array.size() <= index) {
            array.add(NullNode.getInstance());
        }
    }

    private  ObjectNode ensureObject(ArrayNode array, int index, ObjectMapper mapper) {
        // When index == -1 this method is used only to satisfy flow; return a new object
        if (index < 0) return mapper.createObjectNode();
        ensureArraySize(array, index);
        JsonNode node = array.get(index);
        if (!(node instanceof ObjectNode)) {
            ObjectNode obj = mapper.createObjectNode();
            array.set(index, obj);
            return obj;
        }
        return (ObjectNode) node;
    }

    private  JsonNode coerce(ObjectMapper mapper, String value) {
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return mapper.getNodeFactory().booleanNode(Boolean.parseBoolean(value));
        }
        try {
            if (value.matches("[-+]?\\d+")) {
                return mapper.getNodeFactory().numberNode(Long.parseLong(value));
            }
            if (value.matches("[-+]?\\d*\\.\\d+([eE][-+]?\\d+)?")) {
                return mapper.getNodeFactory().numberNode(Double.parseDouble(value));
            }
        } catch (NumberFormatException ignore) {}
        try {
            if ((value.startsWith("{") && value.endsWith("}")) ||
                (value.startsWith("[") && value.endsWith("]"))) {
                return mapper.readTree(value);
            }
        } catch (Exception ignore) {}
        return mapper.getNodeFactory().textNode(value);
    }
}
