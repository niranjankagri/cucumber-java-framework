package com.automation.ui.pages;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.automation.ui.config.Config;

/**
 * Parent of all page objects. Wraps the common WebDriver actions with
 * explicit waits, so pages never need {@code Thread.sleep} or implicit waits.
 */
public abstract class BasePage {

	// OrangeHRM shows this spinner while a form or table is loading
	private static final By LOADER = By.cssSelector(".oxd-loading-spinner");

	protected final WebDriver driver;
	protected final WebDriverWait wait;

	protected BasePage(WebDriver driver) {
		this.driver = driver;
		this.wait = new WebDriverWait(driver, Config.get().timeout());
	}

	/**
	 * Waits until the element is clickable, then clicks it.
	 */
	protected void click(By locator) {
		wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
	}

	/**
	 * Waits until the field is visible, clears it and types the text.
	 */
	protected void type(By locator, String text) {
		WebElement field = waitVisible(locator);
		field.clear();
		field.sendKeys(text);
	}

	/**
	 * @return WebElement the element, once it is visible.
	 */
	protected WebElement waitVisible(By locator) {
		return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
	}

	/**
	 * @return String the visible text of the element, trimmed.
	 */
	protected String textOf(By locator) {
		return waitVisible(locator).getText().trim();
	}

	/**
	 * Waits for the element to become visible.
	 *
	 * @return boolean true if it appeared within the timeout, false otherwise.
	 */
	protected boolean isVisible(By locator) {
		try {
			waitVisible(locator);
			return true;
		} catch (TimeoutException e) {
			return false;
		}
	}

	/**
	 * @return List all elements currently matching the locator (may be empty).
	 */
	protected List<WebElement> findAll(By locator) {
		return driver.findElements(locator);
	}

	/**
	 * Waits until the OrangeHRM loading spinner is gone.
	 */
	protected void waitForLoader() {
		wait.until(ExpectedConditions.invisibilityOfElementLocated(LOADER));
	}

	/**
	 * Waits until the browser URL contains the given path.
	 *
	 * @return boolean true if the URL matched within the timeout, false otherwise.
	 */
	protected boolean waitForUrl(String path) {
		try {
			return wait.until(ExpectedConditions.urlContains(path));
		} catch (TimeoutException e) {
			return false;
		}
	}

	/**
	 * @return String the URL currently shown in the browser.
	 */
	public String currentUrl() {
		return driver.getCurrentUrl();
	}
}
