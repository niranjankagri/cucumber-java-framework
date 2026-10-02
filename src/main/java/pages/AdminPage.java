package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import browseractions.BrowserAction;

public class AdminPage extends BrowserAction {

	public AdminPage(WebDriver driver) {
		super(driver);
	}

	private By userDropDown = By.xpath("//span[@class='oxd-userdropdown-tab']");
	private By logoutOption = By.xpath("//a[@role='menuitem' and @href='/auth/logout']");
	private By usernameField = By.xpath("//div[2]/input[@class='oxd-input oxd-input--active']");
	private By searchButton = By.xpath("//button[@type='submit' and text() = ' Search ']");
	private By resetButton = By.xpath("//button[@type='button' and text() =' Reset ']");
	private By dropDownArrow = By.xpath("(//div/i[@class='oxd-icon bi-caret-down-fill oxd-select-text--arrow'])[2]");
	private By enabledOption = By.xpath("(//div[@role='listbox']//child::div)[2]");
	private By adminOption = By.xpath("//span[@class='oxd-text oxd-text--span oxd-main-menu-item--name' and text()='Admin']");

    public void admin() {
      click(adminOption);
    }

	public void dropDown() {
		click(userDropDown);
	}

	public void logout() {
		click(logoutOption);
	}

	public void username(String username) {
		sendKeys(usernameField, username);
	}

	public void search() {
		click(searchButton);
	}

	public void reset() {
		click(resetButton);
	}

	public void arrow() {
		click(dropDownArrow);
	}

	public void enabled() {
		click(enabledOption);
	}

	public boolean isUsernameFieldDisplayed() {
		return isElementDisplayed(usernameField);
	}

	public boolean isUsernameFieldEnabled() {
		return isElementEnabled(usernameField);
	}

	public boolean isArrowButtonEnabled() {
		return isElementEnabled(dropDownArrow);
	}

	public boolean isArrowButtonDisplayed() {
		return isElementDisplayed(dropDownArrow);
	}

	public boolean isEnabledOptionEnabled() {
		return isElementEnabled(enabledOption);
	}

	public boolean isEnabledOptionDisplayed() {
		return isElementDisplayed(enabledOption);
	}

	public boolean isSearchButtonEnabled() {
		return isElementEnabled(searchButton);
	}

	public boolean isSearchButtonDisplayed() {
		return isElementDisplayed(searchButton);
	}

	public boolean isResetButtonEnabled() {
		return isElementEnabled(resetButton);
	}

	public boolean isResetButtonDisplayed() {
		return isElementDisplayed(resetButton);
	}

	public boolean isUserDropDownEnabled() {
		return isElementEnabled(userDropDown);
	}

	public boolean isUserDropDownDisplayed() {
		return isElementDisplayed(userDropDown);
	}

	public boolean isLogoutOptionEnabled() {
		return isElementEnabled(logoutOption);
	}

	public boolean isLogoutOptionDisplayed() {
		return isElementDisplayed(logoutOption);
	}

}
