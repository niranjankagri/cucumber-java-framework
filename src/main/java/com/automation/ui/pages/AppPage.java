package com.automation.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Parent of the pages shown after login. Holds the parts of the layout every
 * such page shares: the side menu, the page title in the top bar and the user
 * menu with the logout link.
 */
public abstract class AppPage extends BasePage {

	private final By pageTitle = By.cssSelector(".oxd-topbar-header-breadcrumb h6");
	private final By userMenu = By.cssSelector(".oxd-userdropdown-tab");
	private final By logoutLink = By.xpath("//a[@role='menuitem' and normalize-space()='Logout']");

	protected AppPage(WebDriver driver) {
		super(driver);
	}

	/**
	 * Clicks an entry of the side menu, e.g. "Admin" or "PIM".
	 */
	public void openMenu(String item) {
		click(By.xpath("//a[contains(@class,'oxd-main-menu-item')][normalize-space()='" + item + "']"));
	}

	/**
	 * @return String the module name in the top bar, e.g. "Dashboard" or "Admin".
	 */
	public String title() {
		return textOf(pageTitle);
	}

	/**
	 * Opens the user menu and clicks Logout.
	 */
	public void logout() {
		click(userMenu);
		click(logoutLink);
	}
}
