Feature: Producer can connect and it's accessible via its id
  Scenario: Single producer request flow test
    Given example id producerId
    Given connected producer
    Given request payload string payload
    Given response string response
    When http request sent with payload
    Then http response received with response