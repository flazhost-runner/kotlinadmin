Feature: User CRUD

  Background:
    Given I am logged in as admin

  Scenario: Admin can view user list
    When I GET "/admin/v1/access/user"
    Then the response status is 200
    And the response contains "User List"

  Scenario: Admin can create a user
    When I POST to "/admin/v1/access/user/store" with CSRF and form fields:
      | code                  | USR001           |
      | name                  | Test User        |
      | email                 | test@example.com |
      | password              | password123      |
      | password_confirmation | password123      |
      | status                | Active           |
    Then the response status is 302
    And I am redirected to "/admin/v1/access/user"

  Scenario: Admin can delete a user via form DELETE method override
    Given a user exists with email "delete-me@example.com" and code "DEL001"
    When I POST to delete the user with method override "_method=DELETE" and CSRF token
    Then the response status is 302
    And I am redirected to "/admin/v1/access/user"

  Scenario: API verbose paths are accessible
    When I GET "/api/v1/access/user" with JWT
    Then the response status is 200

  Scenario: REST-style paths return 404
    When I GET "/api/v1/access/users" with JWT
    Then the response status is 404

  Scenario: delete_selected removes multiple users
    Given a user exists with email "bulk1@example.com" and code "BLK001"
    And a user exists with email "bulk2@example.com" and code "BLK002"
    When I POST to "/admin/v1/access/user/delete_selected" with CSRF and selected user ids
    Then the response status is 302
