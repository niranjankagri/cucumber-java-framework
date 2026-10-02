package runners;

import org.junit.runner.RunWith;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/java/features", // Path to your feature files
    glue = {"stepdefinitions", "hooks"},     // Packages for step definitions and hooks
    plugin = {
        "pretty",                            // For console output formatting
        "html:target/cucumber-reports.html", // HTML report
        "json:target/cucumber.json",         // JSON report
        "junit:target/cucumber.xml"          // JUnit XML report
    }
)
public class TestRunner {
}
