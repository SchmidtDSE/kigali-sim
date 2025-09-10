/**
 * Live tests for duplicate validation functionality using actual QTA files.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.validate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.kigalisim.KigaliSimFacade;
import org.kigalisim.lang.program.ParsedProgram;
import org.kigalisim.lang.validation.DuplicateValidationException;

/**
 * Tests that validate duplicate detection functionality using QTA files.
 * 
 * <p>These tests verify that the duplicate validation system correctly identifies
 * and reports duplicate names in various contexts with informative error messages.</p>
 */
public class DuplicateValidationLiveTests {

  /**
   * Test that duplicate scenario names are detected and reported with a clear error message.
   */
  @Test
  public void testDuplicateScenarioNames() {
    String qtaPath = "../examples/duplicate_scenarios.qta";
    
    DuplicateValidationException exception = assertThrows(
        DuplicateValidationException.class,
        () -> KigaliSimFacade.parseAndInterpret(qtaPath),
        "Should throw DuplicateValidationException for duplicate scenario names"
    );
    
    assertEquals("scenario", exception.getDuplicateType(),
        "Exception should identify 'scenario' as the duplicate type");
    assertEquals("BAU", exception.getDuplicateName(),
        "Exception should identify 'BAU' as the duplicate name");
    assertEquals("simulations stanza", exception.getContext(),
        "Exception should identify 'simulations stanza' as the context");
    
    String expectedMessage = "Duplicate scenario name 'BAU' found in simulations stanza. " 
        + "Each scenario must have a unique name.";
    assertEquals(expectedMessage, exception.getMessage(),
        "Exception message should be informative and user-friendly");
  }

  /**
   * Test that duplicate application names within a policy are detected.
   */
  @Test
  public void testDuplicateApplicationNames() {
    String qtaPath = "../examples/duplicate_applications.qta";
    
    DuplicateValidationException exception = assertThrows(
        DuplicateValidationException.class,
        () -> KigaliSimFacade.parseAndInterpret(qtaPath),
        "Should throw DuplicateValidationException for duplicate application names"
    );
    
    assertEquals("application", exception.getDuplicateType(),
        "Exception should identify 'application' as the duplicate type");
    assertEquals("Test", exception.getDuplicateName(),
        "Exception should identify 'Test' as the duplicate name");
    assertEquals("policy 'default'", exception.getContext(),
        "Exception should identify the policy context");
    
    String expectedMessage = "Duplicate application name 'Test' found in policy 'default'. " 
        + "Each application must have a unique name within its policy.";
    assertEquals(expectedMessage, exception.getMessage(),
        "Exception message should include policy context");
  }

  /**
   * Test that duplicate substance names within an application are detected.
   */
  @Test
  public void testDuplicateSubstanceNames() {
    String qtaPath = "../examples/duplicate_substances.qta";
    
    DuplicateValidationException exception = assertThrows(
        DuplicateValidationException.class,
        () -> KigaliSimFacade.parseAndInterpret(qtaPath),
        "Should throw DuplicateValidationException for duplicate substance names"
    );
    
    assertEquals("substance", exception.getDuplicateType(),
        "Exception should identify 'substance' as the duplicate type");
    assertEquals("HFC-134a", exception.getDuplicateName(),
        "Exception should identify 'HFC-134a' as the duplicate name");
    assertEquals("application 'Test'", exception.getContext(),
        "Exception should identify the application context");
    
    String expectedMessage = "Duplicate substance name 'HFC-134a' found in application 'Test'. " 
        + "Each substance must have a unique name within its application.";
    assertEquals(expectedMessage, exception.getMessage(),
        "Exception message should include application context");
  }

  /**
   * Test that duplicate policy names are detected.
   */
  @Test
  public void testDuplicatePolicyNames() {
    String qtaPath = "../examples/duplicate_policies.qta";
    
    DuplicateValidationException exception = assertThrows(
        DuplicateValidationException.class,
        () -> KigaliSimFacade.parseAndInterpret(qtaPath),
        "Should throw DuplicateValidationException for duplicate policy names"
    );
    
    assertEquals("policy", exception.getDuplicateType(),
        "Exception should identify 'policy' as the duplicate type");
    assertEquals("default", exception.getDuplicateName(),
        "Exception should identify 'default' as the duplicate name");
    assertEquals("program", exception.getContext(),
        "Exception should identify 'program' as the context");
    
    String expectedMessage = "Duplicate policy name 'default' found in program. " 
        + "Each policy must have a unique name.";
    assertEquals(expectedMessage, exception.getMessage(),
        "Exception message should be informative for policy duplicates");
  }

  /**
   * Test that the substance+equipment validation doesn't interfere with valid scenarios.
   * Different substances can enable the same equipment types in the same application.
   */
  @Test
  public void testValidSubstanceEquipmentCombinations() throws Exception {
    String qtaPath = "../examples/duplicate_substance_equipment.qta";
    
    // This should parse successfully since different substances can enable the same equipment type
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should parse successfully with different substances enabling same equipment types");
    
    // Verify we can access the parsed elements
    assertTrue(program.getScenarios().size() > 0, "Should have at least one scenario");
    assertTrue(program.getPolicies().size() > 0, "Should have at least one policy");
  }

  /**
   * Test that a valid QTA file with all unique names parses successfully.
   */
  @Test
  public void testValidUniqueNames() throws IOException {
    // Use an existing valid QTA file
    String qtaPath = "../examples/basic.qta";
    
    // This should not throw any exceptions
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should parse successfully with unique names");
    
    // Verify we can access the parsed elements
    assertTrue(program.getScenarios().size() > 0, "Should have at least one scenario");
    assertTrue(program.getPolicies().size() > 0, "Should have at least one policy");
  }

  /**
   * Test that error messages follow the expected format consistently.
   */
  @Test
  public void testErrorMessageFormat() {
    String qtaPath = "../examples/duplicate_scenarios.qta";
    
    DuplicateValidationException exception = assertThrows(
        DuplicateValidationException.class,
        () -> KigaliSimFacade.parseAndInterpret(qtaPath)
    );
    
    String message = exception.getMessage();
    
    // Verify message follows the expected pattern:
    // "Duplicate {type} name '{name}' found in {context}. Each {type} must have a unique name{qualifier}."
    assertTrue(message.startsWith("Duplicate scenario name 'BAU' found in simulations stanza."),
        "Message should start with the standard duplicate format");
    assertTrue(message.contains("Each scenario must have a unique name."),
        "Message should explain the uniqueness requirement");
    assertTrue(message.endsWith("."), "Message should end with a period");
  }
}