package org.alexmond.yaml.validator.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobPatternTest {

	@ParameterizedTest
	@CsvSource({ "'**/.github/workflows/*.yml', '.github/workflows/ci.yml', true",
			"'**/.github/workflows/*.yml', 'repo/.github/workflows/ci.yml', true",
			"'**/.github/workflows/*.yml', '/abs/repo/.github/workflows/ci.yml', true",
			"'**/.github/workflows/*.yml', '.github/workflows/ci.yaml', false",
			"'**/docker-compose.yml', 'docker-compose.yml', true",
			"'**/docker-compose.yml', 'sub/dir/docker-compose.yml', true",
			"'Chart.yaml', 'charts/foo/Chart.yaml', true", "'Chart.yaml', 'Chart.yaml', true",
			"'Chart.yaml', 'Chart.yaml.bak', false", "'*.yaml', 'foo.yaml', true", "'*.yaml', 'nested/foo.yaml', true",
			"'a?c.yml', 'abc.yml', true", "'a?c.yml', 'ac.yml', false" })
	void matches(String glob, String path, boolean expected) {
		assertEquals(expected, GlobPattern.compile(glob).matches(path), () -> glob + " vs " + path);
	}

	@Test
	void specificityFavoursMoreLiteralPatterns() {
		assertTrue(GlobPattern.compile("**/.github/workflows/ci.yml").specificity() > GlobPattern.compile("*.yml")
			.specificity());
	}

}
