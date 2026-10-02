package com.automation.ui.steps;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.automation.ui.context.TestContext;
import com.automation.ui.pages.LoginPage;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps for the login page and logging in or out.
 */
public class LoginSteps {

	private final TestContext context;

	public LoginSteps(TestContext context) {
		this.context = context;
	}

	@Given("the user is on the login page")
	public void theUserIsOnTheLoginPage() {
		LoginPage loginPage = context.loginPage().open(context.config().baseUrl());
		assertTrue(loginPage.isDisplayed(), "Login page is not displayed, URL: " + loginPage.currentUrl());
	}

	@When("the user logs in with valid credentials")
	public void theUserLogsInWithValidCredentials() {
		context.loginPage().login(context.config().username(), context.config().password());
	}

	@When("the user logs in with username {string} and password {string}")
	public void theUserLogsInWith(String username, String password) {
		context.loginPage().login(username, password);
	}

	@Given("the admin is logged in")
	public void theAdminIsLoggedIn() {
		theUserIsOnTheLoginPage();
		theUserLogsInWithValidCredentials();
		theDashboardIsDisplayed();
	}

	@Then("the dashboard is displayed")
	public void theDashboardIsDisplayed() {
		assertTrue(context.dashboardPage().isDisplayed(),
				"Dashboard is not displayed, URL: " + context.dashboardPage().currentUrl());
	}

	@Then("the error message {string} is shown")
	public void theErrorMessageIsShown(String message) {
		assertEquals(context.loginPage().errorMessage(), message, "Login error message");
	}

	@Then("the field message {string} is shown")
	public void theFieldMessageIsShown(String message) {
		assertEquals(context.loginPage().fieldMessage(), message, "Login field message");
	}

	@When("the user logs out")
	public void theUserLogsOut() {
		context.dashboardPage().logout();
	}

	@Then("the login page is displayed")
	public void theLoginPageIsDisplayed() {
		assertTrue(context.loginPage().isDisplayed(), "Login page is not displayed, URL: " + context.loginPage().currentUrl());
	}
}
