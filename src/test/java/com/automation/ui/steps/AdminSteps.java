package com.automation.ui.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.automation.ui.context.TestContext;
import com.automation.ui.pages.SystemUsersPage;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for Admin → User Management → System Users: opening the
 * page, filtering the user list, and checking the results.
 */
public class AdminSteps {

	// Per-scenario state (config and page objects), injected by PicoContainer
	private final TestContext context;

	/**
	 * @param context the scenario's shared state, created by PicoContainer.
	 */
	public AdminSteps(TestContext context) {
		this.context = context;
	}

	// Shortcut to the page every step in this class works on
	private SystemUsersPage page() {
		return context.systemUsersPage();
	}

	/**
	 * Clicks an entry of the side menu.
	 *
	 * @param item the menu label, e.g. "Admin".
	 */
	@When("the admin opens the {string} menu")
	public void theAdminOpensTheMenu(String item) {
		context.dashboardPage().openMenu(item);
	}

	/**
	 * Checks the System Users page is shown (URL, filter form and title).
	 */
	@Then("the System Users page is displayed")
	public void theSystemUsersPageIsDisplayed() {
		assertTrue(page().isDisplayed(), "System Users page is not displayed, URL: " + page().currentUrl());
		assertEquals("Admin", page().title(), "Page title");
	}

	/**
	 * Shortcut for scenarios that start on the System Users page.
	 */
	@Given("the admin is on the System Users page")
	public void theAdminIsOnTheSystemUsersPage() {
		theAdminOpensTheMenu("Admin");
		theSystemUsersPageIsDisplayed();
	}

	/**
	 * Types into the Username filter.
	 *
	 * @param username the username to search for.
	 */
	@When("the admin filters by username {string}")
	public void theAdminFiltersByUsername(String username) {
		page().filterByUsername(username);
	}

	/**
	 * Picks an option of the Status filter.
	 *
	 * @param status "Enabled" or "Disabled".
	 */
	@When("the admin filters by status {string}")
	public void theAdminFiltersByStatus(String status) {
		page().filterByStatus(status);
	}

	/**
	 * Runs the search and waits for the table to reload.
	 */
	@When("the admin runs the search")
	public void theAdminRunsTheSearch() {
		page().search();
	}

	/**
	 * Resets the filters and waits for the table to reload.
	 */
	@When("the admin resets the filters")
	public void theAdminResetsTheFilters() {
		page().reset();
	}

	/**
	 * Checks the table has at least one row and that every row matches the
	 * searched username (case-insensitive, as OrangeHRM matches it) and status.
	 *
	 * @param username  the expected username in every row.
	 * @param status    the expected status in every row.
	 */
	@Then("only users named {string} with status {string} are listed")
	public void onlyMatchingUsersAreListed(String username, String status) {
		List<String> usernames = page().usernames();
		assertFalse(usernames.isEmpty(), "No users found, counter shows: " + page().recordCount());
		usernames.forEach(name -> assertTrue(name.equalsIgnoreCase(username), "Unexpected username in results: " + name));
		page().statuses().forEach(value -> assertEquals(status, value, "Status in results"));
	}

	/**
	 * Checks both filters are back to their empty state after Reset.
	 */
	@Then("the username and status filters are empty")
	public void theFiltersAreEmpty() {
		assertEquals("", page().usernameFilter(), "Username filter after reset");
		assertEquals("-- Select --", page().statusFilter(), "Status filter after reset");
	}
}
