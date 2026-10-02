package hooks;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import pages.BaseClass;

import java.io.IOException;

public class Hooks extends BaseClass {

    @Before
    public void beforeScenario() {
        // Setup method from BaseClass to initialize the driver and load properties
        System.out.println("Setting up the driver and loading properties...");
        Setup();
    }

    @After
    public void afterScenario(Scenario scenario) throws IOException {
        // Check if the scenario has failed, take a screenshot
        if (scenario.isFailed()) {
            // Take screenshot with method name as scenario name
            System.out.println("Scenario failed! Taking a screenshot...");
            String screenshotPath = takeScreenshot(scenario.getName());
            System.out.println("Screenshot taken: " + screenshotPath);
        }

        // Teardown method from BaseClass to quit the driver
        System.out.println("Tearing down the driver...");
        teardown();
    }
}

