Feature: Automate OrangeHRM test cases using cucumber-java 

  	Background:
	    Given the user is on the login page
	    When the user enters valid username and password
	    And the user clicks the login button
	    Then the user should be redirected to the home page with the correct URL
    
    Scenario: Navigate to Admin Page
	    Given the user is logged in
	    When the user clicks the Admin option
	    Then the user should be redirected to the admin page with the correct URL
    
    Scenario: Admin searches for a username
	    Given the admin is on the admin page
	    When the admin enters a username to search
	    And the admin clicks the status dropdowm
	    And the admin selects the enabled option
	    And the admin clicks the search button
	    Then the search results should be displayed
	    And the admin clicks the reset button
	    Then the user should be redirected to the admin page with the correct URL
	    
	  Scenario: Successful Logout
	    Given the user is on the admin page
	    When the user clicks the logout option
	    Then the user should be redirected to the login page with the correct URL
