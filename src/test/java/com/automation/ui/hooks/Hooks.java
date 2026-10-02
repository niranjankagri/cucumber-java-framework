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

	public static final String SCREENSHOT_DIR = "target/screenshots";

	private final TestContext context;

	public Hooks(TestContext context) {
		this.context = context;
	}

	@Before
	public void startBrowser(Scenario scenario) {
		Config config = context.config();
		DriverFactory.start(config.browser(), config.headless());
		scenario.log("Browser: " + config.browser() + (config.headless() ? " (headless)" : ""));
	}

	@After
	public void stopBrowser(Scenario scenario) throws IOException {
		try {
			if (scenario.isFailed()) {
				byte[] png = ((TakesScreenshot) context.driver()).getScreenshotAs(OutputType.BYTES);
				scenario.attach(png, "image/png", "Screenshot at failure");
				Path file = Paths.get(SCREENSHOT_DIR, scenario.getName().replaceAll("[^A-Za-z0-9-]+", "_") + ".png");
				Files.createDirectories(file.getParent());
				Files.write(file, png);
			}
		} finally {
			DriverFactory.quit();
		}
	}
}
