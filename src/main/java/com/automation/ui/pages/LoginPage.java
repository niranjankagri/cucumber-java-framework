package com.automation.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * The OrangeHRM login page ({@value #PATH}).
 */
public class LoginPage extends BasePage {

	public static final String PATH = "/web/index.php/auth/login";

	private final By usernameField = By.name("username");
	private final By passwordField = By.name("password");
	private final By loginButton = By.cssSelector("button[type='submit']");
	private final By errorAlert = By.cssSelector(".oxd-alert-content-text");
	private final By requiredMessage = By.cssSelector(".oxd-input-field-error-message");

	public LoginPage(WebDriver driver) {
		super(driver);
	}

	/**
	 * Opens the login page.
	 *
	 * @param baseUrl the application root URL.
	 */
	public LoginPage open(String baseUrl) {
		driver.get(baseUrl + PATH);
		return this;
	}

	/**
	 * Fills in the credentials and submits the form.
	 */
	public void login(String username, String password) {
		type(usernameField, username);
		type(passwordField, password);
		click(loginButton);
	}

	/**
	 * @return boolean true once the URL and the login form are shown.
	 */
	public boolean isDisplayed() {
		return waitForUrl(PATH) && isVisible(usernameField) && isVisible(passwordField);
	}

	/**
	 * @return String the text of the error banner, e.g. "Invalid credentials".
	 */
	public String errorMessage() {
		return textOf(errorAlert);
	}

	/**
	 * @return String the validation message under the first empty field, e.g. "Required".
	 */
	public String fieldMessage() {
		return textOf(requiredMessage);
	}
}
