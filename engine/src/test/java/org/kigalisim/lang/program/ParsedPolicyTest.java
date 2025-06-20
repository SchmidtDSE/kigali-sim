/**
 * Tests for the ParsedPolicy class.
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
 * Tests for the ParsedPolicy class.
 */
public class ParsedPolicyTest {

  private static final String POLICY_NAME = "TestPolicy";
  private static final String APPLICATION1_NAME = "Application1";
  private static final String APPLICATION2_NAME = "Application2";
  private static final String NONEXISTENT_APPLICATION = "NonexistentApplication";
  private static final String SUBSTANCE_NAME = "TestSubstance";

  private ParsedApplication application1;
  private ParsedApplication application2;
  private List<ParsedApplication> applications;
  private ParsedPolicy policy;

  /**
   * Set up the test environment before each test.
   */
  @BeforeEach
  public void setUp() {
    // Create test substances and applications
    ParsedSubstance substance = new ParsedSubstance(SUBSTANCE_NAME, List.of(new TestOperation()));
    application1 = new ParsedApplication(APPLICATION1_NAME, List.of(substance));
    application2 = new ParsedApplication(APPLICATION2_NAME, List.of(substance));

    // Create list of applications
    applications = new ArrayList<>();
    applications.add(application1);
    applications.add(application2);

    // Create policy with applications
    policy = new ParsedPolicy(POLICY_NAME, applications);
  }

  /**
   * Test that getName returns the correct name.
   */
  @Test
  public void testGetName() {
    assertEquals(POLICY_NAME, policy.getName(), "Policy name should match");
  }

  /**
   * Test that getApplications returns all application names.
   */
  @Test
  public void testGetApplications() {
    Set<String> applicationNames = policy.getApplications();
    assertEquals(2, applicationNames.size(), "Should have 2 applications");
    assertTrue(applicationNames.contains(APPLICATION1_NAME), "Should contain application1");
    assertTrue(applicationNames.contains(APPLICATION2_NAME), "Should contain application2");
  }

  /**
   * Test that getApplication returns the correct application.
   */
  @Test
  public void testGetApplication() {
    ParsedApplication retrievedApplication1 = policy.getApplication(APPLICATION1_NAME);
    ParsedApplication retrievedApplication2 = policy.getApplication(APPLICATION2_NAME);

    assertSame(application1, retrievedApplication1, "Should return the same application1 instance");
    assertSame(application2, retrievedApplication2, "Should return the same application2 instance");
  }

  /**
   * Test that getApplication throws an exception for a nonexistent application.
   */
  @Test
  public void testGetApplicationNonexistent() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> policy.getApplication(NONEXISTENT_APPLICATION),
        "Should throw IllegalArgumentException for nonexistent application"
    );

    assertTrue(exception.getMessage().contains(NONEXISTENT_APPLICATION),
        "Exception message should contain the nonexistent application name");
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
