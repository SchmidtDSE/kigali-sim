/**
 * Reference live tests using actual QTA files with "reference" prefix and case study.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.validate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.kigalisim.KigaliSimFacade;
import org.kigalisim.engine.serializer.EngineResult;
import org.kigalisim.lang.program.ParsedProgram;

/**
 * Tests that validate reference QTA files against expected behavior.
 */
public class ReferenceLiveTests {

  /**
   * Test case_study.qta produces expected values.
   */
  @Test
  public void testCaseStudy() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/case_study.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "Business as Usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName);

    // Convert to list for multiple access
    List<EngineResult> resultsList = results.collect(Collectors.toList());

    // Test that the case study simulation completes successfully
    assertTrue(resultsList.size() > 0, "Case study should produce simulation results");

    // Test that at least one stream for one substance/application pair is non-zero in 2030
    EngineResult record = LiveTestsUtil.getResult(resultsList.stream(), 2030, 
        "Domestic Refrigeration", "HFC-134a");
    assertNotNull(record, "Should have result for Domestic Refrigeration/HFC-134a in 2030");
    
    assertTrue(record.getGhgConsumption().getValue().doubleValue() > 0,
        "Should have non-zero consumption for Domestic Refrigeration HFC-134a in 2030");
  }

  /**
   * Test reference.qta produces expected values.
   */
  @Test
  public void testReference() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/reference.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName);

    // Convert to list for multiple access
    List<EngineResult> resultsList = results.collect(Collectors.toList());
    
    // Verify that the simulation completes successfully
    assertTrue(resultsList.size() > 0, "Reference should produce simulation results");
    
    // Check that we have results for each application and substance in year 1
    EngineResult domRefrigHfc134a = LiveTestsUtil.getResult(resultsList.stream(), 1, 
        "dometic refrigeration", "HFC-134a");
    assertNotNull(domRefrigHfc134a, "Should have result for dometic refrigeration/HFC-134a in year 1");
    
    EngineResult comRefrigHfc134a = LiveTestsUtil.getResult(resultsList.stream(), 1, 
        "com refrig", "HFC-134a");
    assertNotNull(comRefrigHfc134a, "Should have result for com refrig/HFC-134a in year 1");
    
    EngineResult resAcR410a = LiveTestsUtil.getResult(resultsList.stream(), 1, 
        "res AC", "R-410A");
    assertNotNull(resAcR410a, "Should have result for res AC/R-410A in year 1");
    
    EngineResult mobileAcHfc134a = LiveTestsUtil.getResult(resultsList.stream(), 1, 
        "mobile AC", "HFC-134a");
    assertNotNull(mobileAcHfc134a, "Should have result for mobile AC/HFC-134a in year 1");
  }

  /**
   * Test reference_simplified.qta produces expected values.
   */
  @Test
  public void testReferenceSimplified() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/reference_simplified.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName);

    // Convert to list for multiple access
    List<EngineResult> resultsList = results.collect(Collectors.toList());
    
    // Verify that the simulation completes successfully
    assertTrue(resultsList.size() > 0, "Reference simplified should produce simulation results");
    
    // Check that we have results for each application and substance in year 1
    EngineResult domRefrigHfc134a = LiveTestsUtil.getResult(resultsList.stream(), 1, 
        "dom refrig", "HFC-134a");
    assertNotNull(domRefrigHfc134a, "Should have result for dom refrig/HFC-134a in year 1");
    
    EngineResult comRefrigHfc134a = LiveTestsUtil.getResult(resultsList.stream(), 1, 
        "com refrig", "HFC-134a");
    assertNotNull(comRefrigHfc134a, "Should have result for com refrig/HFC-134a in year 1");
    
    EngineResult resAcR410a = LiveTestsUtil.getResult(resultsList.stream(), 1, 
        "res AC", "R-410A");
    assertNotNull(resAcR410a, "Should have result for res AC/R-410A in year 1");
    
    EngineResult mobileAcHfc134a = LiveTestsUtil.getResult(resultsList.stream(), 1, 
        "mobile AC", "HFC-134a");
    assertNotNull(mobileAcHfc134a, "Should have result for mobile AC/HFC-134a in year 1");
  }

  /**
   * Test workshop.qta produces expected values.
   */
  @Test
  public void testWorkshop() throws IOException {
    // Load and parse the QTA file
    String qtaPath = "../examples/workshop.qta";
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(qtaPath);
    assertNotNull(program, "Program should not be null");

    // Run the scenario using KigaliSimFacade
    String scenarioName = "business as usual";
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, scenarioName);

    // Convert to list for multiple access
    List<EngineResult> resultsList = results.collect(Collectors.toList());
    
    // Verify that the simulation completes successfully
    assertTrue(resultsList.size() > 0, "Workshop should produce simulation results");
    
    // Check that we have results for each application and substance in year 1
    EngineResult domRefrigHfc134a = LiveTestsUtil.getResult(resultsList.stream(), 1, 
        "dom refrig", "HFC-134a");
    assertNotNull(domRefrigHfc134a, "Should have result for dom refrig/HFC-134a in year 1");
    
    EngineResult comRefrigHfc134a = LiveTestsUtil.getResult(resultsList.stream(), 1, 
        "com refrig", "HFC-134a");
    assertNotNull(comRefrigHfc134a, "Should have result for com refrig/HFC-134a in year 1");
    
    // Check that manufacture values are as expected
    assertEquals(400000.0, domRefrigHfc134a.getManufacture().getValue().doubleValue(), 0.0001,
        "dom refrig/HFC-134a manufacture should be 400000 kg in year 1");
    assertEquals("kg", domRefrigHfc134a.getManufacture().getUnits(),
        "dom refrig/HFC-134a manufacture units should be kg in year 1");
    
    assertEquals(90000.0, comRefrigHfc134a.getManufacture().getValue().doubleValue(), 0.0001,
        "com refrig/HFC-134a manufacture should be 90000 kg in year 1");
    assertEquals("kg", comRefrigHfc134a.getManufacture().getUnits(),
        "com refrig/HFC-134a manufacture units should be kg in year 1");
  }
}