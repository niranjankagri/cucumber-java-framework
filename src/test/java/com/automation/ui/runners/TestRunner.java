package com.automation.ui.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * Runs every feature under {@code src/test/resources/features} as TestNG
 * tests (one test per scenario). Picked up by Surefire on {@code mvn test}.
 * <p>
 * Run a subset by tag with {@code -Dcucumber.filter.tags="@smoke"}.
 */
@CucumberOptions(
		features = "classpath:features",
		glue = "com.automation.ui",
		plugin = {
				"pretty",
				"summary",
				"com.automation.ui.report.HtmlReportPlugin",
				"html:target/cucumber-reports/cucumber.html",
				"json:target/cucumber-reports/cucumber.json",
				"junit:target/cucumber-reports/cucumber.xml"
		})
public class TestRunner extends AbstractTestNGCucumberTests {
}
