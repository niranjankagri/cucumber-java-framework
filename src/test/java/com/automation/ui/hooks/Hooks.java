package com.automation.ui.hooks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import com.automation.ui.config.Config;
import com.automation.ui.context.TestContext;
import com.automation.ui.driver.DriverFactory;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

/**
 * Opens a fresh browser before each scenario and closes it afterwards.
 * When a scenario fails, a screenshot is attached to the reports and saved
 * under {@value #SCREENSHOT_DIR}.
 */
public class Hooks {

	// Folder for the screenshots of failed scenarios
	public static final String SCREENSHOT_DIR = "target/screenshots";

	// Per-scenario state (config and page objects), injected by PicoContainer
	private final TestContext context;

	/**
	 * @param context the scenario's shared state, created by PicoContainer.
	 */
	public Hooks(TestContext context) {
		this.context = context;
	}

	/**
	 * Starts the browser set in config.properties (or with -Dbrowser) and
	 * logs which browser is used to the reports.
	 *
	 * @param scenario the scenario about to run.
	 */
	@Before
	public void startBrowser(Scenario scenario) {
		Config config = context.config();
		DriverFactory.start(config.browser(), config.headless());
		scenario.log("Browser: " + config.browser() + (config.headless() ? " (headless)" : ""));
	}

	/**
	 * Takes a screenshot if the scenario failed, then always closes the browser.
	 *
	 * @param scenario      the scenario that just finished.
	 * @throws IOException  if the screenshot file cannot be written.
	 */
	@After
	public void stopBrowser(Scenario scenario) throws IOException {
		try {
			if (scenario.isFailed()) {
				byte[] png = ((TakesScreenshot) context.driver()).getScreenshotAs(OutputType.BYTES);
				// Embedded in the HTML reports
				scenario.attach(png, "image/png", "Screenshot at failure");
				// Also kept as a file, named after the scenario
				Path file = Paths.get(SCREENSHOT_DIR, scenario.getName().replaceAll("[^A-Za-z0-9-]+", "_") + ".png");
				Files.createDirectories(file.getParent());
				Files.write(file, png);
			}
		} finally {
			DriverFactory.quit();
		}
	}
}
