Feature: Producer can connect and it's accessible via its id
  Background:
    Given request content request
    Given connected producer with id producer1 and responding response1
    Given connected producer with id producer2 and responding response2
    Given connected producer with id producer3 and responding response3
    Given non existing producer id producer4

  Scenario: Delegating is correct to producer 1
    When request is sent to producer1
    Then received response using producer1 is response1

  Scenario: Delegating is correct to producer 2
    When request is sent to producer2
    Then received response using producer2 is response2

  Scenario: Delegating is correct to producer 3
    When request is sent to producer3
    Then received response using producer3 is response3

  Scenario: Gives expected response to non existing producer
    When request is sent to producer4
    Then received response status using producer4 is 404

  Scenario: Producer 3 disconnects and routing remains intact
    Given producer disconnects producer3
    When request is sent to producer1
    Then received response using producer1 is response1

  Scenario: Producer 3 disconnects and not accessible anymore
    Given producer disconnects producer3
    When request is sent to producer3
    Then received response status using producer3 is 404
