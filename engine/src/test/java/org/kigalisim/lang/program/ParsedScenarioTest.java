/**
 * Tests for the ParsedScenario class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the ParsedScenario class.
 */
public class ParsedScenarioTest {

  private static final String SCENARIO_NAME = "TestScenario";
  private static final String POLICY1_NAME = "Policy1";
  private static final String POLICY2_NAME = "Policy2";

  private List<String> policies;
  private ParsedScenario scenario;

  /**
   * Set up the test environment before each test.
   */
  @BeforeEach
  public void setUp() {
    // Create list of policy names
    policies = new ArrayList<>();
    policies.add(POLICY1_NAME);
    policies.add(POLICY2_NAME);
    
    // Create scenario with policy names
    scenario = new ParsedScenario(SCENARIO_NAME, policies);
  }

  /**
   * Test that getName returns the correct name.
   */
  @Test
  public void testGetName() {
    assertEquals(SCENARIO_NAME, scenario.getName(), "Scenario name should match");
  }

  /**
   * Test that getPolicies returns all policy names.
   */
  @Test
  public void testGetPolicies() {
    int count = 0;
    for (String policy : scenario.getPolicies()) {
      assertEquals(policies.get(count), policy, "Policy name should match");
      count++;
    }
    assertEquals(policies.size(), count, "Number of policies should match");
  }

  /**
   * Test that the policies are returned in the correct order.
   */
  @Test
  public void testPolicyOrder() {
    List<String> retrievedPolicies = new ArrayList<>();
    scenario.getPolicies().forEach(retrievedPolicies::add);
    
    assertEquals(policies.size(), retrievedPolicies.size(), "Number of policies should match");
    
    for (int i = 0; i < policies.size(); i++) {
      assertEquals(policies.get(i), retrievedPolicies.get(i), 
          "Policies should be in the same order");
    }
  }
}