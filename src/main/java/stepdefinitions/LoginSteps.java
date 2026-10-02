package stepdefinitions;

import org.testng.Assert;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import pages.BaseClass;
import pages.LoginPage;

public class LoginSteps {

    LoginPage loginPage = new LoginPage(BaseClass.getDriver());

    @Given("the user is on the login page")
    public void theUserIsOnLoginPage() {
        Assert.assertTrue(loginPage.isUsernameFieldDisplayed(), "Username field is not displayed");
        Assert.assertTrue(loginPage.isUsernameFieldEnabled(), "Username field is not enabled");
    }

    @When("the user enters valid username and password")
    public void theUserEntersValidCredentials() throws InterruptedException {
        loginPage.enterUsername(BaseClass.properties.getProperty("userName"));
        Thread.sleep(1000);
        Assert.assertTrue(loginPage.isPasswordFieldDisplayed(), "Password field is not displayed");
        Assert.assertTrue(loginPage.isPasswordFieldEnabled(), "Password field is not enabled");
        loginPage.enterPassword(BaseClass.properties.getProperty("password"));
    }

    @And("the user clicks the login button")
    public void theUserClicksLoginButton() {
        Assert.assertTrue(loginPage.isLoginButtonDisplayed(), "Login button is not displayed");
        Assert.assertTrue(loginPage.isLoginButtonEnabled(), "Login button is not enabled");
        loginPage.clickLogin();
    }

    @Then("the user should be redirected to the home page with the correct URL")
    public void theUserShouldBeRedirectedToHomePage() {
        Assert.assertEquals(loginPage.getCurrentUrl(), BaseClass.properties.getProperty("actualURL"), "Invalid Home Page URL");
    }
}

