package browseractions;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class BrowserAction {

	private WebDriver driver;

	public BrowserAction(WebDriver driver) {
		this.driver = driver;
	}

	public void click(By element) {
		driver.findElement(element).click();
	}

	public void sendKeys(By element, String data) {
		driver.findElement(element).sendKeys(data);
	}

	public boolean isElementDisplayed(By element) {
		return driver.findElement(element).isDisplayed();
	}

	public boolean isElementEnabled(By element) {
		return driver.findElement(element).isEnabled();
	}

	public String getCurrentUrl() {
		return driver.getCurrentUrl();
	}
}
