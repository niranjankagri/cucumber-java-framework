package com.automation.ui.steps;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.List;

import com.automation.ui.context.TestContext;
import com.automation.ui.pages.SystemUsersPage;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps for Admin → User Management → System Users.
 */
public class AdminSteps {

	private final TestContext context;

	public AdminSteps(TestContext context) {
		this.context = context;
	}

	private SystemUsersPage page() {
		return context.systemUsersPage();
	}

	@When("the user opens the {string} menu")
	public void theUserOpensTheMenu(String item) {
		context.dashboardPage().openMenu(item);
	}

	@Then("the System Users page is displayed")
	public void theSystemUsersPageIsDisplayed() {
		assertTrue(page().isDisplayed(), "System Users page is not displayed, URL: " + page().currentUrl());
		assertEquals(page().title(), "Admin", "Page title");
	}

	@Given("the admin is on the System Users page")
	public void theAdminIsOnTheSystemUsersPage() {
		theUserOpensTheMenu("Admin");
		theSystemUsersPageIsDisplayed();
	}

	@When("the admin filters by username {string}")
	public void theAdminFiltersByUsername(String username) {
		page().filterByUsername(username);
	}

	@When("the admin filters by status {string}")
	public void theAdminFiltersByStatus(String status) {
		page().filterByStatus(status);
	}

	@When("the admin clicks Search")
	public void theAdminClicksSearch() {
		page().search();
	}

	@When("the admin clicks Reset")
	public void theAdminClicksReset() {
		page().reset();
	}

	@Then("every result has username {string} and status {string}")
	public void everyResultHas(String username, String status) {
		List<String> usernames = page().usernames();
		assertFalse(usernames.isEmpty(), "No users found, counter shows: " + page().recordCount());
		usernames.forEach(name -> assertTrue(name.equalsIgnoreCase(username), "Unexpected username in results: " + name));
		page().statuses().forEach(value -> assertEquals(value, status, "Status in results"));
	}

	@Then("the filters are cleared")
	public void theFiltersAreCleared() {
		assertEquals(page().usernameFilter(), "", "Username filter after reset");
		assertEquals(page().statusFilter(), "-- Select --", "Status filter after reset");
	}
}
