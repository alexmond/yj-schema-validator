package org.alexmond.yaml.validator;

import org.alexmond.yaml.validator.config.ReportType;
import org.alexmond.yaml.validator.config.YamlSchemaValidatorConfig;
import org.alexmond.yaml.validator.output.FilesOutput;
import org.alexmond.yaml.validator.output.FilesOutputToJunit;
import org.alexmond.yaml.validator.output.FilesOutputToSarif;
import org.alexmond.yaml.validator.output.junit.Failure;
import org.alexmond.yaml.validator.output.junit.Testcase;
import org.alexmond.yaml.validator.output.junit.Testsuite;
import org.alexmond.yaml.validator.output.junit.Testsuites;
import org.alexmond.yaml.validator.output.sarif.ArtifactContent;
import org.alexmond.yaml.validator.output.sarif.ArtifactLocation;
import org.alexmond.yaml.validator.output.sarif.Invocation;
import org.alexmond.yaml.validator.output.sarif.Location;
import org.alexmond.yaml.validator.output.sarif.Message;
import org.alexmond.yaml.validator.output.sarif.MultiformatMessageString;
import org.alexmond.yaml.validator.output.sarif.PhysicalLocation;
import org.alexmond.yaml.validator.output.sarif.Region;
import org.alexmond.yaml.validator.output.sarif.ReportingConfiguration;
import org.alexmond.yaml.validator.output.sarif.ReportingDescriptor;
import org.alexmond.yaml.validator.output.sarif.Result;
import org.alexmond.yaml.validator.output.sarif.Run;
import org.alexmond.yaml.validator.output.sarif.SarifReport;
import org.alexmond.yaml.validator.output.sarif.Tool;
import org.alexmond.yaml.validator.output.sarif.ToolComponent;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * GraalVM native image hints for reflection, resources, and serialization required by
 * Jackson, NetworkNT json-schema-validator, and XML/Stax processing.
 */
@Configuration
@ImportRuntimeHints(NativeImageHints.Registrar.class)
class NativeImageHints {

	static class Registrar implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			registerOutputModelClasses(hints);
			registerConfigClasses(hints);
			registerNetworkNtClasses(hints);
			registerResources(hints);
		}

		private void registerOutputModelClasses(RuntimeHints hints) {
			// SARIF model classes
			Class<?>[] sarifClasses = { SarifReport.class, Run.class, Tool.class, ToolComponent.class, Result.class,
					Message.class, Location.class, PhysicalLocation.class, ArtifactLocation.class, Region.class,
					ArtifactContent.class, Invocation.class, ReportingDescriptor.class, ReportingConfiguration.class,
					MultiformatMessageString.class };

			// JUnit XML model classes
			Class<?>[] junitClasses = { Testsuites.class, Testsuite.class, Testcase.class, Failure.class };

			// Output wrapper classes
			Class<?>[] outputClasses = { FilesOutput.class, FilesOutputToJunit.class, FilesOutputToSarif.class };

			MemberCategory[] allAccess = { MemberCategory.DECLARED_FIELDS, MemberCategory.INVOKE_DECLARED_METHODS,
					MemberCategory.INVOKE_DECLARED_CONSTRUCTORS };

			for (Class<?> clazz : sarifClasses) {
				hints.reflection().registerType(clazz, allAccess);
			}
			for (Class<?> clazz : junitClasses) {
				hints.reflection().registerType(clazz, allAccess);
			}
			for (Class<?> clazz : outputClasses) {
				hints.reflection().registerType(clazz, allAccess);
			}
		}

		private void registerConfigClasses(RuntimeHints hints) {
			MemberCategory[] allAccess = { MemberCategory.DECLARED_FIELDS, MemberCategory.INVOKE_DECLARED_METHODS,
					MemberCategory.INVOKE_DECLARED_CONSTRUCTORS };

			hints.reflection().registerType(YamlSchemaValidatorConfig.class, allAccess);
			hints.reflection().registerType(ReportType.class, allAccess);
		}

		private void registerNetworkNtClasses(RuntimeHints hints) {
			MemberCategory[] allAccess = { MemberCategory.DECLARED_FIELDS, MemberCategory.INVOKE_DECLARED_METHODS,
					MemberCategory.INVOKE_DECLARED_CONSTRUCTORS };

			// NetworkNT json-schema-validator core classes used reflectively
			String[] networkNtClasses = { "com.networknt.schema.output.OutputUnit",
					"com.networknt.schema.output.OutputFlag", "com.networknt.schema.SpecVersion",
					"com.networknt.schema.SpecVersion$VersionFlag", "com.networknt.schema.JsonSchemaFactory",
					"com.networknt.schema.JsonSchema", "com.networknt.schema.ValidationMessage",
					"com.networknt.schema.SchemaValidatorsConfig",
					"com.networknt.schema.SchemaValidatorsConfig$Builder" };

			for (String className : networkNtClasses) {
				try {
					hints.reflection().registerType(Class.forName(className), allAccess);
				}
				catch (ClassNotFoundException ignored) {
					// Skip if class not available
				}
			}
		}

		private void registerResources(RuntimeHints hints) {
			// Stax/Woodstox XML SPI services (needed for JUnit XML output)
			hints.resources().registerPattern("META-INF/services/javax.xml.stream.*");
			hints.resources().registerPattern("META-INF/services/tools.jackson.*");

			// NetworkNT schema dialect resources
			hints.resources().registerPattern("draftv4/*");
			hints.resources().registerPattern("draft2019-09/*");
			hints.resources().registerPattern("draft2020-12/*");
			hints.resources().registerPattern("draft7/*");
			hints.resources().registerPattern("draft6/*");

			// Spring Boot config
			hints.resources().registerPattern("yj-schema-validator.yaml");
			hints.resources().registerPattern("banner.txt");
		}

	}

}
