Feature: Authentication

  Scenario: Admin can login with valid credentials
    Given the application is running
    When I POST to "/auth/login" with email "admin@admin.com" and password "12345678"
    Then the response status is 302
    And I am redirected to "/admin/v1/dashboard"

  Scenario: Login fails with wrong password
    Given the application is running
    When I POST to "/auth/login" with email "admin@admin.com" and password "wrongpassword"
    Then the response status is 302
    And I am redirected to "/auth/login"

  Scenario: API login returns JWT token
    Given the application is running
    When I POST to "/api/v1/auth/login" with JSON body email "admin@admin.com" password "12345678"
    Then the response status is 200
    And the response JSON has field "token"

  Scenario: API logout blacklists token
    Given I am logged in via API as "admin@admin.com" with password "12345678"
    When I GET "/api/v1/auth/me"
    Then the response status is 200
    When I POST to "/api/v1/auth/logout"
    Then the response status is 200
    When I GET "/api/v1/auth/me" with the same token
    Then the response status is 401

  Scenario: Unauthenticated web access redirects to login
    Given the application is running
    When I GET "/admin/v1/dashboard" without authentication
    Then the response status is 302
    And I am redirected to "/auth/login"
