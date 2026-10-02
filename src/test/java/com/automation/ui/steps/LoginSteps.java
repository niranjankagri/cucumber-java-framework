package com.automation.ui.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.automation.ui.context.TestContext;
import com.automation.ui.pages.LoginPage;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for the login page: logging in with valid or invalid
 * credentials, the resulting messages, and logging out.
 * <p>
 * Steps contain no locators; they call page objects from the
 * {@link TestContext} and assert on what the pages return.
 */
public class LoginSteps {

	// Per-scenario state (config and page objects), injected by PicoContainer
	private final TestContext context;

	/**
	 * @param context the scenario's shared state, created by PicoContainer.
	 */
	public LoginSteps(TestContext context) {
		this.context = context;
	}

	/**
	 * Opens the login page and checks that the login form is shown.
	 */
	@Given("the user is on the login page")
	public void theUserIsOnTheLoginPage() {
		LoginPage loginPage = context.loginPage().open(context.config().baseUrl());
		assertTrue(loginPage.isDisplayed(), "Login page is not displayed, URL: " + loginPage.currentUrl());
	}

	/**
	 * Logs in with the credentials from config.properties.
	 */
	@When("the user logs in with valid credentials")
	public void theUserLogsInWithValidCredentials() {
		context.loginPage().login(context.config().username(), context.config().password());
	}

	/**
	 * Logs in with the given credentials (used for the negative cases).
	 *
	 * @param username  the username to type; may be empty.
	 * @param password  the password to type; may be empty.
	 */
	@When("the user logs in with username {string} and password {string}")
	public void theUserLogsInWith(String username, String password) {
		context.loginPage().login(username, password);
	}

	/**
	 * Submits the form with a username and an empty password field.
	 *
	 * @param username the username to type.
	 */
	@When("the user logs in with username {string} and no password")
	public void theUserLogsInWithoutPassword(String username) {
		context.loginPage().login(username, "");
	}

	/**
	 * Setup step for scenarios that need a logged-in user while already on
	 * the login page: logs in and checks the dashboard is shown.
	 */
	@Given("the user has logged in with valid credentials")
	public void theUserHasLoggedIn() {
		theUserLogsInWithValidCredentials();
		theDashboardIsDisplayed();
	}

	/**
	 * Shortcut for scenarios that start after login: opens the login page,
	 * logs in with valid credentials and checks the dashboard is shown.
	 */
	@Given("the admin is logged in")
	public void theAdminIsLoggedIn() {
		theUserIsOnTheLoginPage();
		theUserHasLoggedIn();
	}

	/**
	 * Checks the browser is on the dashboard (URL and page title).
	 */
	@Then("the dashboard is displayed")
	public void theDashboardIsDisplayed() {
		assertTrue(context.dashboardPage().isDisplayed(),
				"Dashboard is not displayed, URL: " + context.dashboardPage().currentUrl());
	}

	/**
	 * Checks the error banner above the login form.
	 *
	 * @param message the expected text, e.g. "Invalid credentials".
	 */
	@Then("the error message {string} is shown")
	public void theErrorMessageIsShown(String message) {
		assertEquals(message, context.loginPage().errorMessage(), "Login error message");
	}

	/**
	 * Checks the validation message under a field of the login form.
	 *
	 * @param field    the field label, "Username" or "Password".
	 * @param message  the expected text, e.g. "Required".
	 */
	@Then("the {string} field shows {string}")
	public void theFieldShows(String field, String message) {
		assertEquals(message, context.loginPage().fieldMessage(field), field + " field message");
	}

	/**
	 * Logs out through the user menu in the top bar.
	 */
	@When("the user logs out")
	public void theUserLogsOut() {
		context.dashboardPage().logout();
	}

	/**
	 * Checks the browser is back on the login page.
	 */
	@Then("the login page is displayed")
	public void theLoginPageIsDisplayed() {
		assertTrue(context.loginPage().isDisplayed(), "Login page is not displayed, URL: " + context.loginPage().currentUrl());
	}
}
