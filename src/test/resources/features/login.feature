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

  @smoke @TC-101
  Scenario: [TC-101] Successful login with valid credentials
    When the user logs in with valid credentials
    Then the dashboard is displayed

  # One scenario per Examples row; <reason> is replaced in the scenario name
  @TC-102
  Scenario Outline: [TC-102] Login is rejected when <reason>
    When the user logs in with username "<username>" and password "<password>"
    Then the error message "Invalid credentials" is shown

    Examples:
      | reason                | username | password |
      | the password is wrong | Admin    | wrong123 |
      | the user is unknown   | nobody42 | admin123 |

  @TC-103
  Scenario: [TC-103] Login is not submitted without a password
    When the user logs in with username "Admin" and no password
    Then the "Password" field shows "Required"

  @smoke @TC-104
  Scenario: [TC-104] Successful logout
    Given the user has logged in with valid credentials
    When the user logs out
    Then the login page is displayed
