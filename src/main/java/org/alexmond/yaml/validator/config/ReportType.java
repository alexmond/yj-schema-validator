package org.alexmond.yaml.validator.config;

/**
 * Represents different types of output report formats for validation results.
 */
public enum ReportType {

	/**
	 * Plain text format report
	 */
	TEXT,
	/**
	 * YAML format report
	 */
	YAML,
	/**
	 * JSON format report
	 */
	JSON,
	/**
	 * JUnit XML format report
	 */
	JUNIT,
	/**
	 * Sarif format report
	 */
	SARIF,
	/**
	 * Compact, agent-friendly report optimised for LLM consumption (structured JSON, or
	 * compiler-style diagnostic lines when {@code compact} is enabled).
	 */
	LLM

}
