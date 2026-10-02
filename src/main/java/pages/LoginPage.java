package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import browseractions.BrowserAction;

public class LoginPage extends BrowserAction {
	
	public LoginPage(WebDriver driver) {
		super(driver);
	}

	private By usernameField = By.xpath("//input[@name='username']");
	private By passwordField = By.xpath("//input[@name='password']");
	private By loginButton = By.xpath("//button[@type='submit']");

	public void enterUsername(String username) {
		sendKeys(usernameField, username);
	}

	public void enterPassword(String password) {
		sendKeys(passwordField, password);
	}

	public void clickLogin() {
		click(loginButton);
	}

	public boolean isUsernameFieldDisplayed() {
		return isElementDisplayed(usernameField);
	}

	public boolean isUsernameFieldEnabled() {
		return isElementEnabled(usernameField);
	}

	public boolean isPasswordFieldDisplayed() {
		return isElementDisplayed(passwordField);
	}

	public boolean isPasswordFieldEnabled() {
		return isElementEnabled(passwordField);
	}

	public boolean isLoginButtonEnabled() {
		return isElementEnabled(loginButton);
	}

	public boolean isLoginButtonDisplayed() {
		return isElementDisplayed(loginButton);
	}

}
