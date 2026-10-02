@login
Feature: Login
  As an OrangeHRM user
  I want to sign in with my credentials
  So that only authorised people can use the application

  Background:
    Given the user is on the login page

  @smoke
  Scenario: Successful login with valid credentials
    When the user logs in with valid credentials
    Then the dashboard is displayed

  Scenario Outline: Login is rejected with <case>
    When the user logs in with username "<username>" and password "<password>"
    Then the error message "Invalid credentials" is shown

    Examples:
      | case             | username | password  |
      | a wrong password | Admin    | wrong123  |
      | an unknown user  | nobody42 | admin123  |

  Scenario: Login requires a password
    When the user logs in with username "Admin" and password ""
    Then the field message "Required" is shown

  @smoke
  Scenario: Successful logout
    When the user logs in with valid credentials
    And the user logs out
    Then the login page is displayed
