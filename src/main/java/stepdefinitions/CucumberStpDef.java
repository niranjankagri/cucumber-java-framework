package stepdefinitions;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class CucumberStpDef {
	WebDriver driver;
	
	@Given("Open the browser window")
	public void open_the_browser_window() {
        
	 	driver = new ChromeDriver();
	 	
	 	driver.manage().window().maximize();
	}

	@When("Enter the url of website")
	public void enter_the_url_of_website() {
		driver.get("https://www.amazon.in/");
	}


	@Then("Click on signin button")
	public void click_on_signin_button() {
		driver.findElement(By.xpath("//a[@id='nav-link-accountList']")).click();
	}

	@When("Enter Username")
	public void enter_username() {
		driver.findElement(By.xpath("//input[@id='ap_email']")).sendKeys("dummyem123@gmail.com");
	}

	@When("Click on Continue")
	public void click_on_continue() {
		driver.findElement(By.xpath("//span[@id='continue']")).click();
	}

	@When("Enter Password")
	public void enter_password() {
	    driver.findElement(By.xpath("//input[@id='ap_password']")).sendKeys("dm@12345");
	}

	@Then("Click on Submit")
	public void click_on_submit() {
		driver.findElement(By.xpath("//input[@id='signInSubmit']")).submit();
		
	}

	


}
