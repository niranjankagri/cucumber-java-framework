# Login scenarios for the OrangeHRM demo.
# Steps: com.automation.ui.steps.LoginSteps
# Credentials for "valid credentials" come from config.properties.
@login
Feature: Login
  As an OrangeHRM user
  I want to sign in with my credentials
  So that only authorised people can use the application

  # Runs before every scenario in this file
  Background:
    Given the user is on the login page

  @smoke
  Scenario: Successful login with valid credentials
    When the user logs in with valid credentials
    Then the dashboard is displayed

  # One scenario per Examples row; <reason> is replaced in the scenario name
  Scenario Outline: Login is rejected when <reason>
    When the user logs in with username "<username>" and password "<password>"
    Then the error message "Invalid credentials" is shown

    Examples:
      | reason                | username | password |
      | the password is wrong | Admin    | wrong123 |
      | the user is unknown   | nobody42 | admin123 |

  Scenario: Login is not submitted without a password
    When the user logs in with username "Admin" and no password
    Then the "Password" field shows "Required"

  @smoke
  Scenario: Successful logout
    Given the user has logged in with valid credentials
    When the user logs out
    Then the login page is displayed
