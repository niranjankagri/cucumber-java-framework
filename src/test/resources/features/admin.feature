@admin
Feature: Admin user management
  As an OrangeHRM admin
  I want to find system users by their details
  So that I can manage their accounts

  Background:
    Given the admin is logged in

  @smoke
  Scenario: Open the System Users page from the menu
    When the user opens the "Admin" menu
    Then the System Users page is displayed

  Scenario: Search users by username and status
    Given the admin is on the System Users page
    When the admin filters by username "Admin"
    And the admin filters by status "Enabled"
    And the admin clicks Search
    Then every result has username "Admin" and status "Enabled"

  Scenario: Reset clears the search filters
    Given the admin is on the System Users page
    When the admin filters by username "Admin"
    And the admin filters by status "Enabled"
    And the admin clicks Reset
    Then the filters are cleared
