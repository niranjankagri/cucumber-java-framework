package com.automation.ui.pages;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Admin → User Management → System Users ({@value #PATH}): a filter form
 * and a table of the matching user accounts.
 */
public class SystemUsersPage extends AppPage {

	// Path of the page, appended to the base URL
	public static final String PATH = "/web/index.php/admin/viewSystemUsers";

	// Table columns (0 is the row checkbox)
	private static final int USERNAME_COLUMN = 1;
	private static final int STATUS_COLUMN = 4;

	// Filter form buttons
	private final By searchButton = By.xpath("//button[@type='submit'][normalize-space()='Search']");
	private final By resetButton = By.xpath("//button[@type='button'][normalize-space()='Reset']");
	// Result table: one card per user, one cell per column
	private final By rows = By.cssSelector(".oxd-table-body .oxd-table-card");
	private final By cells = By.cssSelector(".oxd-table-cell");
	// Counter above the table, e.g. "(1) Record Found"
	private final By recordCount = By.xpath("//span[contains(normalize-space(),'Found')]");

	/**
	 * @param driver the browser this page works on.
	 */
	public SystemUsersPage(WebDriver driver) {
		super(driver);
	}

	/**
	 * @return boolean true once the page URL and the filter form are shown.
	 */
	public boolean isDisplayed() {
		return waitForUrl(PATH) && isVisible(filterInput("Username"));
	}

	/**
	 * Types a username into the Username filter.
	 *
	 * @param username the username to search for.
	 */
	public void filterByUsername(String username) {
		type(filterInput("Username"), username);
	}

	/**
	 * Opens the Status filter and picks an option.
	 *
	 * @param status the option label, e.g. "Enabled".
	 */
	public void filterByStatus(String status) {
		click(filterSelect("Status"));
		click(By.xpath("//div[@role='listbox']//span[normalize-space()='" + status + "']"));
	}

	/**
	 * Clicks Search and waits for the table to reload.
	 */
	public void search() {
		click(searchButton);
		waitForLoader();
		waitVisible(recordCount);
	}

	/**
	 * Clicks Reset and waits for the table to reload.
	 */
	public void reset() {
		click(resetButton);
		waitForLoader();
		waitVisible(recordCount);
	}

	/**
	 * @return String the current value of the Username filter.
	 */
	public String usernameFilter() {
		return waitVisible(filterInput("Username")).getDomProperty("value");
	}

	/**
	 * @return String the current value of the Status filter, e.g. "-- Select --".
	 */
	public String statusFilter() {
		return textOf(filterSelect("Status"));
	}

	/**
	 * @return String the record counter above the table, e.g. "(1) Record Found".
	 */
	public String recordCount() {
		return textOf(recordCount);
	}

	/**
	 * @return List the Username column of every row in the table.
	 */
	public List<String> usernames() {
		return column(USERNAME_COLUMN);
	}

	/**
	 * @return List the Status column of every row in the table.
	 */
	public List<String> statuses() {
		return column(STATUS_COLUMN);
	}

	// Text of one column for every row of the table. The table can re-render
	// while it is read; the wait ignores stale rows, so such a read is retried.
	private List<String> column(int index) {
		return wait.until(d -> findAll(rows).stream()
						.map(row -> row.findElements(cells))
						.map(rowCells -> rowCells.get(index))
						.map(WebElement::getText)
						.map(String::trim)
						.toList());
	}

	// Text input of the filter form with the given label
	private By filterInput(String label) {
		return inField(label, "//input");
	}

	// Dropdown of the filter form with the given label
	private By filterSelect(String label) {
		return inField(label, "//div[contains(@class,'oxd-select-text-input')]");
	}
}
