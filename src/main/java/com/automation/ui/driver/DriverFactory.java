package com.automation.ui.driver;

import java.util.Locale;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

/**
 * Creates and holds one {@link WebDriver} per thread, so scenarios can run in
 * parallel without sharing a browser.
 * <p>
 * Drivers (chromedriver, geckodriver, msedgedriver) are resolved by Selenium
 * Manager, so no driver binary is kept in the project.
 */
public final class DriverFactory {

	// Browser window size; also used in headless mode, where there is no screen to maximise to
	private static final String WIDTH = "1920";
	private static final String HEIGHT = "1080";

	private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

	private DriverFactory() {
	}

	/**
	 * Starts a new browser for the current thread.
	 *
	 * @param browser     chrome, firefox or edge (case-insensitive).
	 * @param headless    true to run without a visible window.
	 * @return WebDriver  the started driver.
	 */
	public static WebDriver start(String browser, boolean headless) {
		WebDriver driver = switch (browser.toLowerCase(Locale.ROOT)) {
		case "chrome" -> new ChromeDriver(chromeOptions(headless));
		case "firefox" -> new FirefoxDriver(firefoxOptions(headless));
		case "edge" -> new EdgeDriver(edgeOptions(headless));
		default -> throw new IllegalArgumentException("Unsupported browser '" + browser + "' (use chrome, firefox or edge)");
		};
		if (!headless) {
			driver.manage().window().maximize();
		}
		DRIVER.set(driver);
		return driver;
	}

	/**
	 * @return WebDriver the driver started on this thread.
	 * @throws IllegalStateException if {@link #start(String, boolean)} was not called.
	 */
	public static WebDriver getDriver() {
		WebDriver driver = DRIVER.get();
		if (driver == null) {
			throw new IllegalStateException("No browser started on this thread");
		}
		return driver;
	}

	/**
	 * Closes the browser of the current thread, if any.
	 */
	public static void quit() {
		WebDriver driver = DRIVER.get();
		if (driver != null) {
			try {
				driver.quit();
			} finally {
				DRIVER.remove();
			}
		}
	}

	private static ChromeOptions chromeOptions(boolean headless) {
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--window-size=" + WIDTH + "," + HEIGHT, "--disable-search-engine-choice-screen");
		if (headless) {
			options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
		}
		return options;
	}

	private static FirefoxOptions firefoxOptions(boolean headless) {
		FirefoxOptions options = new FirefoxOptions();
		options.addArguments("--width=" + WIDTH, "--height=" + HEIGHT);
		if (headless) {
			options.addArguments("-headless");
		}
		return options;
	}

	private static EdgeOptions edgeOptions(boolean headless) {
		EdgeOptions options = new EdgeOptions();
		options.addArguments("--window-size=" + WIDTH + "," + HEIGHT);
		if (headless) {
			options.addArguments("--headless=new");
		}
		return options;
	}
}
