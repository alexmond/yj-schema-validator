package org.alexmond.yaml.validator.output;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.networknt.schema.output.OutputUnit;
import tools.jackson.databind.json.JsonMapper;

/**
 * Renders validation results in a format optimised for consumption by an LLM or coding
 * agent. Two shapes are produced:
 * <ul>
 * <li><b>JSON</b> (default): a leading {@code summary} (the verdict in the first tokens)
 * followed by flat per-file {@code errors}, each localised by a JSON Pointer with the
 * failing {@code keyword}, human-readable {@code message} and {@code schemaLocation} —
 * the triple an agent needs to apply a fix.</li>
 * <li><b>compact</b>: one compiler-style diagnostic line per error, the most
 * token-efficient form.</li>
 * </ul>
 */
public class FilesOutputToLlm {

	private final Map<String, OutputUnit> files;

	public FilesOutputToLlm(Map<String, OutputUnit> files) {
		this.files = files;
	}

	/**
	 * Renders the results.
	 * @param compact true for compiler-style diagnostic lines, false for structured JSON
	 * @return the rendered report
	 */
	public String toLlmString(boolean compact) {
		return compact ? toCompact() : toJson();
	}

	private String toJson() {
		int valid = 0;
		int invalid = 0;
		int totalErrors = 0;
		List<Map<String, Object>> results = new ArrayList<>();
		for (Map.Entry<String, OutputUnit> entry : this.files.entrySet()) {
			OutputUnit unit = entry.getValue();
			List<Map<String, Object>> errors = collectErrors(unit);
			totalErrors += errors.size();
			if (unit.isValid()) {
				valid++;
			}
			else {
				invalid++;
			}
			Map<String, Object> fileResult = new LinkedHashMap<>();
			fileResult.put("file", entry.getKey());
			fileResult.put("valid", unit.isValid());
			if (!errors.isEmpty()) {
				fileResult.put("errors", errors);
			}
			results.add(fileResult);
		}

		Map<String, Object> summary = new LinkedHashMap<>();
		summary.put("files", this.files.size());
		summary.put("valid", valid);
		summary.put("invalid", invalid);
		summary.put("errors", totalErrors);

		Map<String, Object> root = new LinkedHashMap<>();
		root.put("summary", summary);
		root.put("results", results);

		JsonMapper jsonMapper = JsonMapper.builder().build();
		try {
			return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Error converting to LLM JSON", ex);
		}
	}

	private String toCompact() {
		int valid = 0;
		int totalErrors = 0;
		List<String> lines = new ArrayList<>();
		for (Map.Entry<String, OutputUnit> entry : this.files.entrySet()) {
			OutputUnit unit = entry.getValue();
			if (unit.isValid()) {
				valid++;
				continue;
			}
			for (Map<String, Object> error : collectErrors(unit)) {
				String pointer = String.valueOf(error.get("pointer"));
				String location = pointer.isEmpty() ? "" : " " + pointer;
				lines.add(entry.getKey() + ": [" + error.get("keyword") + "]" + location + ": " + error.get("message"));
				totalErrors++;
			}
		}
		StringBuilder result = new StringBuilder();
		result.append("# ")
			.append(valid)
			.append('/')
			.append(this.files.size())
			.append(" files valid, ")
			.append(totalErrors)
			.append(" errors\n");
		lines.forEach((line) -> result.append(line).append('\n'));
		return result.toString();
	}

	private List<Map<String, Object>> collectErrors(OutputUnit unit) {
		List<Map<String, Object>> errors = new ArrayList<>();
		if (unit.isValid()) {
			return errors;
		}
		// File-level errors (parse failure, no schema found, fetch error, ...)
		if (unit.getErrors() != null) {
			unit.getErrors().forEach((keyword, message) -> errors.add(error("", keyword, message, null)));
		}
		// Per-instance schema violations
		if (unit.getDetails() != null) {
			for (OutputUnit detail : unit.getDetails()) {
				if (detail.getErrors() == null) {
					continue;
				}
				detail.getErrors()
					.forEach((keyword, message) -> errors
						.add(error(detail.getInstanceLocation(), keyword, message, detail.getSchemaLocation())));
			}
		}
		return errors;
	}

	private Map<String, Object> error(Object pointer, Object keyword, Object message, Object schemaLocation) {
		Map<String, Object> error = new LinkedHashMap<>();
		error.put("pointer", (pointer != null) ? String.valueOf(pointer) : "");
		error.put("keyword", String.valueOf(keyword));
		error.put("message", String.valueOf(message));
		if (schemaLocation != null) {
			error.put("schemaLocation", String.valueOf(schemaLocation));
		}
		return error;
	}

}
