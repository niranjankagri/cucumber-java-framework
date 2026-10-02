package com.automation.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * The OrangeHRM login page ({@value #PATH}).
 */
public class LoginPage extends BasePage {

	// Path of the page, appended to the base URL
	public static final String PATH = "/web/index.php/auth/login";

	// Login form
	private final By usernameField = By.name("username");
	private final By passwordField = By.name("password");
	private final By loginButton = By.cssSelector("button[type='submit']");
	// "Invalid credentials" banner shown above the form
	private final By errorAlert = By.cssSelector(".oxd-alert-content-text");
	// "Required" message shown under an empty field
	private final By requiredMessage = By.cssSelector(".oxd-input-field-error-message");

	/**
	 * @param driver the browser this page works on.
	 */
	public LoginPage(WebDriver driver) {
		super(driver);
	}

	/**
	 * Opens the login page.
	 *
	 * @param baseUrl     the application root URL.
	 * @return LoginPage  this page, for chaining.
	 */
	public LoginPage open(String baseUrl) {
		driver.get(baseUrl + PATH);
		return this;
	}

	/**
	 * Fills in the credentials and submits the form.
	 *
	 * @param username  the username to type; may be empty.
	 * @param password  the password to type; may be empty.
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
