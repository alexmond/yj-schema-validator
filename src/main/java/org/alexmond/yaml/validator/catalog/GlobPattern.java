package org.alexmond.yaml.validator.catalog;

import java.util.regex.Pattern;

/**
 * A compiled file-name glob as used by the JSON Schema Store catalog. Supports the only
 * metacharacters that appear in the catalog: {@code *} (any run of non-separator
 * characters), {@code ?} (a single non-separator character) and {@code **} (any number of
 * path segments). A pattern that contains no {@code /} is matched against the file's base
 * name only (e.g. {@code Chart.yaml}); otherwise it is matched against the full path,
 * with a leading {@code **}{@code /} allowing a match at any depth.
 */
public final class GlobPattern {

	private final Pattern pattern;

	private final boolean basenameOnly;

	private final int specificity;

	private GlobPattern(Pattern pattern, boolean basenameOnly, int specificity) {
		this.pattern = pattern;
		this.basenameOnly = basenameOnly;
		this.specificity = specificity;
	}

	/**
	 * Compiles a catalog glob into a matcher.
	 * @param glob the glob pattern
	 * @return the compiled pattern
	 */
	public static GlobPattern compile(String glob) {
		boolean basenameOnly = glob.indexOf('/') < 0;
		Pattern compiled = Pattern.compile("^" + globToRegex(glob) + "$");
		int literalChars = 0;
		for (int i = 0; i < glob.length(); i++) {
			char c = glob.charAt(i);
			if (c != '*' && c != '?') {
				literalChars++;
			}
		}
		return new GlobPattern(compiled, basenameOnly, literalChars);
	}

	/**
	 * Tests whether the given path matches this glob.
	 * @param path the file path (any separator style)
	 * @return true if the path matches
	 */
	public boolean matches(String path) {
		String normalized = path.replace('\\', '/');
		String target = this.basenameOnly ? basename(normalized) : normalized;
		return this.pattern.matcher(target).matches();
	}

	/**
	 * Number of literal (non-wildcard) characters, used to prefer the most specific match
	 * when several patterns match the same path.
	 * @return the specificity score
	 */
	public int specificity() {
		return this.specificity;
	}

	private static String basename(String path) {
		int slash = path.lastIndexOf('/');
		return (slash >= 0) ? path.substring(slash + 1) : path;
	}

	private static String globToRegex(String glob) {
		StringBuilder regex = new StringBuilder(glob.length() * 2);
		int i = 0;
		while (i < glob.length()) {
			char c = glob.charAt(i);
			if (c == '*') {
				if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
					i++;
					if (i + 1 < glob.length() && glob.charAt(i + 1) == '/') {
						i++;
						regex.append("(?:.*/)?");
					}
					else {
						regex.append(".*");
					}
				}
				else {
					regex.append("[^/]*");
				}
			}
			else if (c == '?') {
				regex.append("[^/]");
			}
			else if ("\\.[]{}()+-^$|".indexOf(c) >= 0) {
				regex.append('\\').append(c);
			}
			else {
				regex.append(c);
			}
			i++;
		}
		return regex.toString();
	}

}
