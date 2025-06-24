/**
 * Tests for the ParsedProgram class.
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
import org.kigalisim.lang.machine.PushDownMachine;
import org.kigalisim.lang.operation.Operation;

/**
 * Tests for the ParsedProgram class.
 */
public class ParsedProgramTest {

  private static final String POLICY1_NAME = "Policy1";
  private static final String POLICY2_NAME = "Policy2";
  private static final String SCENARIO1_NAME = "Scenario1";
  private static final String SCENARIO2_NAME = "Scenario2";
  private static final String NONEXISTENT_POLICY = "NonexistentPolicy";
  private static final String NONEXISTENT_SCENARIO = "NonexistentScenario";
  private static final String APPLICATION_NAME = "TestApplication";
  private static final String SUBSTANCE_NAME = "TestSubstance";

  private ParsedPolicy policy1;
  private ParsedPolicy policy2;
  private ParsedScenario scenario1;
  private ParsedScenario scenario2;
  private List<ParsedPolicy> policies;
  private List<ParsedScenario> scenarios;
  private ParsedProgram program;

  /**
   * Set up the test environment before each test.
   */
  @BeforeEach
  public void setUp() {
    // Create test substances, applications, policies, and scenarios
    ParsedSubstance substance = new ParsedSubstance(SUBSTANCE_NAME, List.of(new TestOperation()));
    ParsedApplication application = new ParsedApplication(APPLICATION_NAME, List.of(substance));

    policy1 = new ParsedPolicy(POLICY1_NAME, List.of(application));
    policy2 = new ParsedPolicy(POLICY2_NAME, List.of(application));

    scenario1 = new ParsedScenario(SCENARIO1_NAME, List.of(POLICY1_NAME), 1, 3, 1);
    scenario2 = new ParsedScenario(SCENARIO2_NAME, List.of(POLICY2_NAME), 1, 3, 1);

    // Create lists of policies and scenarios
    policies = new ArrayList<>();
    policies.add(policy1);
    policies.add(policy2);

    scenarios = new ArrayList<>();
    scenarios.add(scenario1);
    scenarios.add(scenario2);

    // Create program with policies and scenarios
    program = new ParsedProgram(policies, scenarios);
  }

  /**
   * Test that getPolicies returns all policy names.
   */
  @Test
  public void testGetPolicies() {
    Set<String> policyNames = program.getPolicies();
    assertEquals(2, policyNames.size(), "Should have 2 policies");
    assertTrue(policyNames.contains(POLICY1_NAME), "Should contain policy1");
    assertTrue(policyNames.contains(POLICY2_NAME), "Should contain policy2");
  }

  /**
   * Test that getPolicy returns the correct policy.
   */
  @Test
  public void testGetPolicy() {
    ParsedPolicy retrievedPolicy1 = program.getPolicy(POLICY1_NAME);
    ParsedPolicy retrievedPolicy2 = program.getPolicy(POLICY2_NAME);

    assertSame(policy1, retrievedPolicy1, "Should return the same policy1 instance");
    assertSame(policy2, retrievedPolicy2, "Should return the same policy2 instance");
  }

  /**
   * Test that getPolicy throws an exception for a nonexistent policy.
   */
  @Test
  public void testGetPolicyNonexistent() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> program.getPolicy(NONEXISTENT_POLICY),
        "Should throw IllegalArgumentException for nonexistent policy"
    );

    assertTrue(exception.getMessage().contains(NONEXISTENT_POLICY),
        "Exception message should contain the nonexistent policy name");
  }

  /**
   * Test that getScenarios returns all scenario names.
   */
  @Test
  public void testGetScenarios() {
    Set<String> scenarioNames = program.getScenarios();
    assertEquals(2, scenarioNames.size(), "Should have 2 scenarios");
    assertTrue(scenarioNames.contains(SCENARIO1_NAME), "Should contain scenario1");
    assertTrue(scenarioNames.contains(SCENARIO2_NAME), "Should contain scenario2");
  }

  /**
   * Test that getScenario returns the correct scenario.
   */
  @Test
  public void testGetScenario() {
    ParsedScenario retrievedScenario1 = program.getScenario(SCENARIO1_NAME);
    ParsedScenario retrievedScenario2 = program.getScenario(SCENARIO2_NAME);

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
        () -> program.getScenario(NONEXISTENT_SCENARIO),
        "Should throw IllegalArgumentException for nonexistent scenario"
    );

    assertTrue(exception.getMessage().contains(NONEXISTENT_SCENARIO),
        "Exception message should contain the nonexistent scenario name");
  }

  /**
   * Simple implementation of Operation for testing.
   */
  private static class TestOperation implements Operation {
    /**
     * Minimal implementation for testing.
     *
     * @param machine The machine in which to execute the calculation if needed.
     */
    @Override
    public void execute(PushDownMachine machine) {
      // Do nothing, this is just a stub for testing
    }
  }
}
