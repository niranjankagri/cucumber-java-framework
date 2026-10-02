# System Users scenarios (Admin -> User Management).
# Steps: com.automation.ui.steps.AdminSteps (+ LoginSteps for the Background)
@admin
Feature: Admin user management
  As an OrangeHRM admin
  I want to find system users by their details
  So that I can manage their accounts

  # Runs before every scenario in this file
  Background:
    Given the admin is logged in

  @smoke
  Scenario: Open the System Users page from the menu
    When the admin opens the "Admin" menu
    Then the System Users page is displayed

  Scenario: Search users by username and status
    Given the admin is on the System Users page
    When the admin filters by username "Admin"
    And the admin filters by status "Enabled"
    And the admin runs the search
    Then only users named "Admin" with status "Enabled" are listed

  Scenario: Reset clears the search filters
    Given the admin is on the System Users page
    When the admin filters by username "Admin"
    And the admin filters by status "Enabled"
    And the admin resets the filters
    Then the username and status filters are empty
