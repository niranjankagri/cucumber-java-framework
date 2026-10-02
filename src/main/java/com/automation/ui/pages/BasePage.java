package com.automation.ui.pages;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
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
	// How long to wait for the spinner to appear; fast responses may never show it
	private static final Duration LOADER_APPEAR_TIMEOUT = Duration.ofSeconds(2);

	// The browser this page works on
	protected final WebDriver driver;
	// Explicit wait with the timeout from config.properties
	protected final WebDriverWait wait;

	/**
	 * @param driver the browser this page works on.
	 */
	protected BasePage(WebDriver driver) {
		this.driver = driver;
		this.wait = new WebDriverWait(driver, Config.get().timeout());
		// OrangeHRM re-renders parts of the page after loads; an element replaced
		// mid-wait is looked up again on the next poll instead of failing the step
		this.wait.ignoring(StaleElementReferenceException.class);
	}

	/**
	 * Waits until the element is clickable, then clicks it.
	 *
	 * @param locator the element to click.
	 */
	protected void click(By locator) {
		wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
	}

	/**
	 * Waits until the field is visible, clears it and types the text.
	 *
	 * @param locator  the input field.
	 * @param text     the text to type; may be empty.
	 */
	protected void type(By locator, String text) {
		WebElement field = waitVisible(locator);
		field.clear();
		field.sendKeys(text);
	}

	/**
	 * @param locator      the element to wait for.
	 * @return WebElement  the element, once it is visible.
	 */
	protected WebElement waitVisible(By locator) {
		return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
	}

	/**
	 * @param locator  the element to read.
	 * @return String  the visible text of the element, trimmed.
	 */
	protected String textOf(By locator) {
		return waitVisible(locator).getText().trim();
	}

	/**
	 * Waits for the element to become visible.
	 *
	 * @param locator   the element to wait for.
	 * @return boolean  true if it appeared within the timeout, false otherwise.
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
	 * @param locator  the elements to find.
	 * @return List    all elements currently matching the locator (may be empty).
	 */
	protected List<WebElement> findAll(By locator) {
		return driver.findElements(locator);
	}

	/**
	 * Waits for a load started by the previous action to finish: first gives
	 * the spinner a moment to appear (so the wait cannot pass before loading
	 * has even started), then waits until it is gone.
	 */
	protected void waitForLoader() {
		try {
			new WebDriverWait(driver, LOADER_APPEAR_TIMEOUT).until(ExpectedConditions.presenceOfElementLocated(LOADER));
		} catch (TimeoutException e) {
			// The response came back before the spinner was shown
		}
		wait.until(ExpectedConditions.invisibilityOfElementLocated(LOADER));
	}

	/**
	 * Locates an element inside the OrangeHRM form field with the given label.
	 *
	 * @param label     the visible field label, e.g. "Username".
	 * @param inner     XPath of the element within the field, e.g. "//input".
	 * @return By       locator of that element.
	 */
	protected By inField(String label, String inner) {
		return By.xpath("//label[normalize-space()='" + label + "']/ancestor::div[contains(@class,'oxd-input-group')]" + inner);
	}

	/**
	 * Waits until the browser URL contains the given path.
	 *
	 * @param path      part of the URL to wait for.
	 * @return boolean  true if the URL matched within the timeout, false otherwise.
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
