package com.automation.ui.context;

import org.openqa.selenium.WebDriver;

import com.automation.ui.config.Config;
import com.automation.ui.driver.DriverFactory;
import com.automation.ui.pages.DashboardPage;
import com.automation.ui.pages.LoginPage;
import com.automation.ui.pages.SystemUsersPage;

/**
 * State shared by the hooks and step classes of one scenario.
 * <p>
 * PicoContainer creates a new instance per scenario and passes it to every
 * class that asks for it in its constructor. Pages are created on first use,
 * after {@code Hooks} has started the browser.
 */
public class TestContext {

	private final Config config = Config.get();

	private LoginPage loginPage;
	private DashboardPage dashboardPage;
	private SystemUsersPage systemUsersPage;

	public Config config() {
		return config;
	}

	public WebDriver driver() {
		return DriverFactory.getDriver();
	}

	public LoginPage loginPage() {
		if (loginPage == null) {
			loginPage = new LoginPage(driver());
		}
		return loginPage;
	}

	public DashboardPage dashboardPage() {
		if (dashboardPage == null) {
			dashboardPage = new DashboardPage(driver());
		}
		return dashboardPage;
	}

	public SystemUsersPage systemUsersPage() {
		if (systemUsersPage == null) {
			systemUsersPage = new SystemUsersPage(driver());
		}
		return systemUsersPage;
	}
}
