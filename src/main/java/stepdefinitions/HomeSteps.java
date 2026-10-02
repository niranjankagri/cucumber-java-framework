package stepdefinitions;

import org.testng.Assert;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import pages.BaseClass;
import pages.HomePage;

public class HomeSteps {

    HomePage homePage = new HomePage(BaseClass.getDriver());

    @Given("the user is logged in")
    public void theUserIsLoggedIn() {
        // Assumes login process has been successfully completed
        Assert.assertTrue(homePage.isAdminOptionDisplayed(), "User is not logged in");
    }

    @When("the user clicks the Admin option")
    public void theUserClicksAdminOption() {
        Assert.assertTrue(homePage.isAdminOptionEnabled(), "Admin option is not enabled");
        Assert.assertTrue(homePage.isAdminOptionDisplayed(), "Admin option is not displayed");
        homePage.admin();
    }

    @Then("the user should be redirected to the admin page with the correct URL")
    public void theUserIsRedirectedToAdminPage() {
        Assert.assertEquals(homePage.getCurrentUrl(), BaseClass.properties.getProperty("adminPageURL"), "Invalid Admin Page URL");
    }
}


