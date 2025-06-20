/**
 * Tests for the ParsedApplication class.
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
 * Tests for the ParsedApplication class.
 */
public class ParsedApplicationTest {

  private static final String APPLICATION_NAME = "TestApplication";
  private static final String SUBSTANCE1_NAME = "Substance1";
  private static final String SUBSTANCE2_NAME = "Substance2";
  private static final String NONEXISTENT_SUBSTANCE = "NonexistentSubstance";

  private ParsedSubstance substance1;
  private ParsedSubstance substance2;
  private List<ParsedSubstance> substances;
  private ParsedApplication application;

  /**
   * Set up the test environment before each test.
   */
  @BeforeEach
  public void setUp() {
    // Create test substances
    substance1 = new ParsedSubstance(SUBSTANCE1_NAME, List.of(new TestOperation()));
    substance2 = new ParsedSubstance(SUBSTANCE2_NAME, List.of(new TestOperation()));
    
    // Create list of substances
    substances = new ArrayList<>();
    substances.add(substance1);
    substances.add(substance2);
    
    // Create application with substances
    application = new ParsedApplication(APPLICATION_NAME, substances);
  }

  /**
   * Test that getName returns the correct name.
   */
  @Test
  public void testGetName() {
    assertEquals(APPLICATION_NAME, application.getName(), "Application name should match");
  }

  /**
   * Test that getSubstances returns all substance names.
   */
  @Test
  public void testGetSubstances() {
    Set<String> substanceNames = application.getSubstances();
    assertEquals(2, substanceNames.size(), "Should have 2 substances");
    assertTrue(substanceNames.contains(SUBSTANCE1_NAME), "Should contain substance1");
    assertTrue(substanceNames.contains(SUBSTANCE2_NAME), "Should contain substance2");
  }

  /**
   * Test that getSubstance returns the correct substance.
   */
  @Test
  public void testGetSubstance() {
    ParsedSubstance retrievedSubstance1 = application.getSubstance(SUBSTANCE1_NAME);
    ParsedSubstance retrievedSubstance2 = application.getSubstance(SUBSTANCE2_NAME);
    
    assertSame(substance1, retrievedSubstance1, "Should return the same substance1 instance");
    assertSame(substance2, retrievedSubstance2, "Should return the same substance2 instance");
  }

  /**
   * Test that getSubstance throws an exception for a nonexistent substance.
   */
  @Test
  public void testGetSubstanceNonexistent() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> application.getSubstance(NONEXISTENT_SUBSTANCE),
        "Should throw IllegalArgumentException for nonexistent substance"
    );
    
    assertTrue(exception.getMessage().contains(NONEXISTENT_SUBSTANCE),
        "Exception message should contain the nonexistent substance name");
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