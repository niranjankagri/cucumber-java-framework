package com.automation.ui.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Properties;

/**
 * Framework settings read from {@value #FILE_NAME} on the classpath
 * ({@code src/test/resources}).
 * <p>
 * Every value can be overridden with a system property of the same name,
 * e.g. {@code mvn clean test -Dbrowser=firefox -Dheadless=true}, without
 * editing the file.
 */
public final class Config {

	// Settings file, looked up on the classpath
	public static final String FILE_NAME = "config.properties";

	// Loaded once, when the class is first used
	private static final Config INSTANCE = new Config();

	// Values read from the file
	private final Properties properties = new Properties();

	// Reads the settings file; fails fast if it is missing or unreadable
	private Config() {
		try (InputStream in = Config.class.getClassLoader().getResourceAsStream(FILE_NAME)) {
			if (in == null) {
				throw new IllegalStateException(FILE_NAME + " not found on the classpath");
			}
			properties.load(in);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not read " + FILE_NAME, e);
		}
	}

	/**
	 * @return Config   the shared, already loaded settings.
	 */
	public static Config get() {
		return INSTANCE;
	}

	/**
	 * Returns a setting, preferring a {@code -D} system property over the file.
	 *
	 * @param key       the property name.
	 * @return String   the trimmed value.
	 * @throws IllegalStateException if the property is not set anywhere.
	 */
	public String value(String key) {
		String value = System.getProperty(key, properties.getProperty(key));
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("Missing setting '" + key + "' in " + FILE_NAME);
		}
		return value.trim();
	}

	/** Application root URL, e.g. https://opensource-demo.orangehrmlive.com */
	public String baseUrl() {
		return value("baseUrl").replaceAll("/+$", "");
	}

	/** Browser to run: chrome, firefox or edge. */
	public String browser() {
		return value("browser");
	}

	/** Run the browser without a window (needed on CI). */
	public boolean headless() {
		return Boolean.parseBoolean(value("headless"));
	}

	/** Maximum time an explicit wait polls for an element or condition. */
	public Duration timeout() {
		return Duration.ofSeconds(Long.parseLong(value("timeoutSeconds")));
	}

	/** Login name of the admin account. */
	public String username() {
		return value("username");
	}

	/** Password of the admin account. */
	public String password() {
		return value("password");
	}
}
