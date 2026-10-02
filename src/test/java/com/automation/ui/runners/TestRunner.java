package com.automation.ui.runners;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PUBLISH_QUIET_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * JUnit Platform suite that runs every feature file under
 * {@code src/test/resources/features} with the Cucumber engine (one test per
 * scenario). Surefire picks it up on {@code mvn test}; it can also be run
 * from the IDE like any JUnit test class.
 * <p>
 * Run a subset by tag with {@code -Dcucumber.filter.tags="@smoke"}: JUnit
 * reads any {@code cucumber.*} setting from system properties as well.
 */
@Suite
// Hand the selected resources to the Cucumber engine only
@IncludeEngines("cucumber")
// Feature files on the classpath (src/test/resources/features)
@SelectClasspathResource("features")
// Packages scanned for step definitions and hooks
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.automation.ui")
// Report plugins: console output, standard Cucumber reports and the custom HTML report
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, summary, "
		+ "html:target/cucumber-reports/cucumber.html, "
		+ "json:target/cucumber-reports/cucumber.json, "
		+ "junit:target/cucumber-reports/cucumber.xml, "
		+ "com.automation.ui.report.HtmlReportPlugin")
// Hide the "publish your report to reports.cucumber.io" banner
@ConfigurationParameter(key = PLUGIN_PUBLISH_QUIET_PROPERTY_NAME, value = "true")
public class TestRunner {
}
