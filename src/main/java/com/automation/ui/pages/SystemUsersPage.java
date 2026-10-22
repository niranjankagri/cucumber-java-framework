package com.automation.ui.pages;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

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
	 * @return boolean true once the page URL, the filter form and the loaded
	 *         user table are shown. Filters typed while the table is still
	 *         loading can be cleared when the page finishes rendering.
	 */
	public boolean isDisplayed() {
		if (!waitForUrl(PATH) || !isVisible(filterInput("Username"))) {
			return false;
		}
		waitForLoader();
		return isVisible(recordCount);
	}

	/**
	 * Types a username into the Username filter.
	 *
	 * @param username the username to search for.
	 */
	public void filterByUsername(String username) {
		type(filterInput("Username"), username);
		wait.until(d -> username.equals(usernameFilter()));
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
	 * <p>
	 * The unfiltered table and its counter are already on screen, so waiting
	 * for them alone can read the old rows when the spinner is missed. This
	 * also waits until the old first row is replaced or the counter changes.
	 * If neither happens, the search returned the rows already shown.
	 */
	public void search() {
		List<WebElement> oldRows = findAll(rows);
		String oldCount = currentCount();
		click(searchButton);
		waitForLoader();
		if (!oldRows.isEmpty()) {
			try {
				wait.until(d -> {
					String count = currentCount();
					return ExpectedConditions.stalenessOf(oldRows.get(0)).apply(d)
							|| (!count.isEmpty() && !count.equals(oldCount));
				});
			} catch (TimeoutException e) {
				// Same rows and counter as before: the table already shows the result
			}
		}
		waitVisible(recordCount);
	}

	// Counter text without waiting; "" while it is absent or being re-rendered
	private String currentCount() {
		try {
			return findAll(recordCount).stream().findFirst().map(e -> e.getText().trim()).orElse("");
		} catch (StaleElementReferenceException e) {
			return "";
		}
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
