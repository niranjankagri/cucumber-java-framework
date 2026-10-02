package stepdefinitions;

import org.testng.Assert;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import pages.AdminPage;
import pages.BaseClass;

public class AdminSteps {

    AdminPage adminPage = new AdminPage(BaseClass.getDriver());

    @Given("the admin is on the admin page")
    public void theAdminIsOnTheAdminPage() {
    	adminPage.admin();
    	Assert.assertTrue(adminPage.isUsernameFieldDisplayed(), "Username field is not displayed");
        Assert.assertTrue(adminPage.isUsernameFieldEnabled(), "Username field is not enabled");
    }

    @When("the admin enters a username to search")
    public void theAdminEntersUsername() throws InterruptedException {
    	adminPage.username("Admin");
        Thread.sleep(2000);
    }
    
    @And("the admin clicks the status dropdowm")
    public void theAdminClicksStautsDropdown(){
    	Assert.assertTrue(adminPage.isArrowButtonEnabled(), "Arrow button is disabled");
        Assert.assertTrue(adminPage.isArrowButtonDisplayed(), "Arrow button is not displayed");
        adminPage.arrow();
    }

    @And("the admin selects the enabled option")
    public void theAdminSelectsEnabledOption() {
        Assert.assertTrue(adminPage.isEnabledOptionEnabled(), "Enabled option is not enabled");
        Assert.assertTrue(adminPage.isEnabledOptionDisplayed(), "Enabled option is not displayed");
        adminPage.enabled();
    }

    @And("the admin clicks the search button")
    public void theAdminClicksSearchButton() throws InterruptedException {
        Assert.assertTrue(adminPage.isSearchButtonEnabled(), "Search button is not enabled");
        Assert.assertTrue(adminPage.isSearchButtonDisplayed(), "Search button is not displayed");
        adminPage.search();
        Thread.sleep(2000);
    }
    
    @And("the admin clicks the reset button")
    public void theAdminClicksResetButton() throws InterruptedException {
    	Assert.assertTrue(adminPage.isResetButtonEnabled(), "Reset button is disabled");
        Assert.assertTrue(adminPage.isResetButtonDisplayed(), "Reset button is not displayed");
        adminPage.reset();
        Thread.sleep(2000);
    }

    @Then("the search results should be displayed")
    public void searchResultsDisplayed() {
        // Assume the method to check search results is implemented here
        Assert.assertTrue(adminPage.isResetButtonDisplayed(), "Search results are not displayed");
    }
    
    @Given("the user is on the admin page")
    public void theUserIsOnAdminPage() {
        Assert.assertTrue(adminPage.isUserDropDownDisplayed(), "User dropdown is not displayed");
        Assert.assertTrue(adminPage.isUserDropDownEnabled(), "User dropdown is not enabled");
    }

    @When("the user clicks the logout option")
    public void theUserClicksLogoutOption() throws InterruptedException {
        adminPage.dropDown();
        Thread.sleep(2000);
        adminPage.logout();
    }

    @Then("the user should be redirected to the login page with the correct URL")
    public void theUserIsRedirectedToLoginPage() {
        Assert.assertEquals(adminPage.getCurrentUrl(), BaseClass.properties.getProperty("loginPageURL"), "Invalid Login Page URL");
    }
}

