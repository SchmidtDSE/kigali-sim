/**
 * Tests for the ParsedScenarios class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the ParsedScenarios class.
 */
public class ParsedScenariosTest {

  private static final String SCENARIO1_NAME = "Scenario1";
  private static final String SCENARIO2_NAME = "Scenario2";
  private static final String NONEXISTENT_SCENARIO = "NonexistentScenario";
  private static final String POLICY_NAME = "TestPolicy";

  private ParsedScenario scenario1;
  private ParsedScenario scenario2;
  private List<ParsedScenario> scenariosList;
  private ParsedScenarios scenarios;

  /**
   * Set up the test environment before each test.
   */
  @BeforeEach
  public void setUp() {
    // Create test scenarios
    List<String> policies = new ArrayList<>();
    policies.add(POLICY_NAME);
    
    scenario1 = new ParsedScenario(SCENARIO1_NAME, policies, 1, 3, 1);
    scenario2 = new ParsedScenario(SCENARIO2_NAME, policies, 1, 3, 1);

    // Create list of scenarios
    scenariosList = new ArrayList<>();
    scenariosList.add(scenario1);
    scenariosList.add(scenario2);

    // Create scenarios with the list
    scenarios = new ParsedScenarios(scenariosList);
  }

  /**
   * Test that getScenarios returns all scenario names.
   */
  @Test
  public void testGetScenarios() {
    Set<String> scenarioNames = scenarios.getScenarios();
    assertEquals(2, scenarioNames.size(), "Should have 2 scenarios");
    assertTrue(scenarioNames.contains(SCENARIO1_NAME), "Should contain scenario1");
    assertTrue(scenarioNames.contains(SCENARIO2_NAME), "Should contain scenario2");
  }

  /**
   * Test that getScenario returns the correct scenario.
   */
  @Test
  public void testGetScenario() {
    ParsedScenario retrievedScenario1 = scenarios.getScenario(SCENARIO1_NAME);
    ParsedScenario retrievedScenario2 = scenarios.getScenario(SCENARIO2_NAME);

    assertSame(scenario1, retrievedScenario1, "Should return the same scenario1 instance");
    assertSame(scenario2, retrievedScenario2, "Should return the same scenario2 instance");
  }

  /**
   * Test that getScenario throws an exception for a nonexistent scenario.
   */
  @Test
  public void testGetScenarioNonexistent() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> scenarios.getScenario(NONEXISTENT_SCENARIO),
        "Should throw IllegalArgumentException for nonexistent scenario"
    );

    assertTrue(exception.getMessage().contains(NONEXISTENT_SCENARIO),
        "Exception message should contain the nonexistent scenario name");
  }
}