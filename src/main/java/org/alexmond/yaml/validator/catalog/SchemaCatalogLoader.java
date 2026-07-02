package org.alexmond.yaml.validator.catalog;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alexmond.yaml.validator.config.YamlSchemaValidatorConfig;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Loads the JSON Schema Store catalog used for path-based schema autodetection. Prefers a
 * live fetch from the configured catalog URL and falls back to the snapshot bundled in
 * the jar, so detection keeps working offline. The result is cached for the process
 * lifetime.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaCatalogLoader {

	private static final String BUNDLED_CATALOG = "/schema-catalog.json";

	private static final int HTTP_SUCCESS_STATUS = 200;

	private final YamlSchemaValidatorConfig config;

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	private SchemaCatalog cached;

	/**
	 * Returns the catalog, loading it on first use.
	 * @return the schema catalog (never null; empty if nothing could be loaded)
	 */
	public SchemaCatalog load() {
		if (this.cached == null) {
			SchemaCatalog catalog = fetchLive();
			if (catalog == null || catalog.getSchemas() == null || catalog.getSchemas().isEmpty()) {
				catalog = loadBundled();
			}
			this.cached = (catalog != null) ? catalog : new SchemaCatalog();
		}
		return this.cached;
	}

	private SchemaCatalog fetchLive() {
		String url = this.config.getCatalogUrl();
		if (!StringUtils.hasText(url)) {
			return null;
		}
		try {
			HttpClient httpClient = HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NORMAL)
				.connectTimeout(this.config.getHttpTimeout())
				.build();
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == HTTP_SUCCESS_STATUS) {
				log.debug("Loaded live schema catalog from {}", url);
				return this.jsonMapper.readValue(response.body(), SchemaCatalog.class);
			}
			log.debug("Catalog fetch returned status {} for {}; using bundled snapshot", response.statusCode(), url);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			log.debug("Live catalog fetch interrupted; using bundled snapshot");
		}
		catch (RuntimeException | java.io.IOException ex) {
			log.debug("Live catalog fetch from {} failed ({}); using bundled snapshot", url, ex.getMessage());
		}
		return null;
	}

	private SchemaCatalog loadBundled() {
		try (InputStream is = getClass().getResourceAsStream(BUNDLED_CATALOG)) {
			if (is == null) {
				log.warn("Bundled schema catalog {} not found on classpath", BUNDLED_CATALOG);
				return null;
			}
			return this.jsonMapper.readValue(is.readAllBytes(), SchemaCatalog.class);
		}
		catch (RuntimeException | java.io.IOException ex) {
			log.warn("Failed to load bundled schema catalog: {}", ex.getMessage());
			return null;
		}
	}

}
