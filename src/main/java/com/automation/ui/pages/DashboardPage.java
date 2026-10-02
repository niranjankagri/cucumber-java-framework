package com.automation.ui.pages;

import org.openqa.selenium.WebDriver;

/**
 * The dashboard ({@value #PATH}), shown right after a successful login.
 */
public class DashboardPage extends AppPage {

	public static final String PATH = "/web/index.php/dashboard/index";

	public DashboardPage(WebDriver driver) {
		super(driver);
	}

	/**
	 * @return boolean true once the dashboard URL and title are shown.
	 */
	public boolean isDisplayed() {
		return waitForUrl(PATH) && "Dashboard".equals(title());
	}
}
