package org.alexmond.yaml.validator;

import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import org.alexmond.yaml.validator.config.ReportType;
import org.alexmond.yaml.validator.config.YamlSchemaValidatorConfig;
import org.alexmond.yaml.validator.output.FilesOutput;
import org.alexmond.yaml.validator.output.sarif.SarifReport;
import org.alexmond.yaml.validator.output.sarif.Run;
import org.alexmond.yaml.validator.output.sarif.Result;
import org.alexmond.yaml.validator.output.sarif.Tool;
import org.alexmond.yaml.validator.output.junit.Testsuites;
import org.alexmond.yaml.validator.output.junit.Testsuite;
import org.alexmond.yaml.validator.output.junit.Testcase;
import org.alexmond.yaml.validator.output.junit.Failure;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeImageHintsTest {

	@Test
	void testReflectionHintsRegistered() {
		RuntimeHints hints = new RuntimeHints();
		new NativeImageHints.Registrar().registerHints(hints, getClass().getClassLoader());

		// SARIF model classes
		assertTrue(RuntimeHintsPredicates.reflection().onType(SarifReport.class).test(hints));
		assertTrue(RuntimeHintsPredicates.reflection().onType(Run.class).test(hints));
		assertTrue(RuntimeHintsPredicates.reflection().onType(Result.class).test(hints));
		assertTrue(RuntimeHintsPredicates.reflection().onType(Tool.class).test(hints));

		// JUnit model classes
		assertTrue(RuntimeHintsPredicates.reflection().onType(Testsuites.class).test(hints));
		assertTrue(RuntimeHintsPredicates.reflection().onType(Testsuite.class).test(hints));
		assertTrue(RuntimeHintsPredicates.reflection().onType(Testcase.class).test(hints));
		assertTrue(RuntimeHintsPredicates.reflection().onType(Failure.class).test(hints));

		// Output and config classes
		assertTrue(RuntimeHintsPredicates.reflection().onType(FilesOutput.class).test(hints));
		assertTrue(RuntimeHintsPredicates.reflection().onType(YamlSchemaValidatorConfig.class).test(hints));
		assertTrue(RuntimeHintsPredicates.reflection().onType(ReportType.class).test(hints));
	}

	@Test
	void testResourceHintsRegistered() {
		RuntimeHints hints = new RuntimeHints();
		new NativeImageHints.Registrar().registerHints(hints, getClass().getClassLoader());

		assertTrue(RuntimeHintsPredicates.resource().forResource("yj-schema-validator.yaml").test(hints));
		assertTrue(RuntimeHintsPredicates.resource().forResource("banner.txt").test(hints));
	}

}
