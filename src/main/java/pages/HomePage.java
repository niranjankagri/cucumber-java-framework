package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import browseractions.BrowserAction;

public class HomePage extends BrowserAction {
    
	public HomePage(WebDriver driver) {
        super(driver);
    }

    private By adminOption = By.xpath("//span[@class='oxd-text oxd-text--span oxd-main-menu-item--name' and text()='Admin']");

    public void admin() {
      click(adminOption);
    }

    public boolean isAdminOptionEnabled() {
        return isElementEnabled(adminOption);
    }

    public boolean isAdminOptionDisplayed() {
        return isElementDisplayed(adminOption);
    }
}
