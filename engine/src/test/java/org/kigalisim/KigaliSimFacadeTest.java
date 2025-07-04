/**
 * Unit tests for the KigaliSimFacade class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kigalisim.engine.serializer.EngineResult;
import org.kigalisim.lang.parse.ParseResult;
import org.kigalisim.lang.program.ParsedProgram;

/**
 * Tests for the KigaliSimFacade class.
 */
public class KigaliSimFacadeTest {

  /**
   * Test that parse method returns a valid parse result.
   */
  @Test
  public void testParse() {
    String code = "start default\nend default";
    ParseResult parseResult = KigaliSimFacade.parse(code);
    assertNotNull(parseResult, "Parse result should not be null");
    assertFalse(parseResult.hasErrors(), "Parse result should not have errors");
    assertTrue(parseResult.getProgram().isPresent(), "Parse result should have a program");
  }

  /**
   * Test that interpret method returns a valid program.
   */
  @Test
  public void testInterpret() {
    String code = "start default\nend default";
    ParseResult parseResult = KigaliSimFacade.parse(code);
    ParsedProgram program = KigaliSimFacade.interpret(parseResult);
    assertNotNull(program, "Program should not be null");
  }

  /**
   * Test that validate method returns true for valid code.
   */
  @Test
  public void testValidateValidCode(@TempDir Path tempDir) throws IOException {
    String code = "start default\nend default";
    File file = tempDir.resolve("valid.qta").toFile();
    Files.writeString(file.toPath(), code);

    boolean isValid = KigaliSimFacade.validate(file.getPath());
    assertTrue(isValid, "Valid code should validate successfully");
  }

  /**
   * Test that validate method returns false for invalid code.
   */
  @Test
  public void testValidateInvalidCode(@TempDir Path tempDir) throws IOException {
    String code = "invalid code";
    File file = tempDir.resolve("invalid.qta").toFile();
    Files.writeString(file.toPath(), code);

    boolean isValid = KigaliSimFacade.validate(file.getPath());
    assertFalse(isValid, "Invalid code should fail validation");
  }

  /**
   * Test that parseAndInterpret method returns a valid program.
   */
  @Test
  public void testParseAndInterpret(@TempDir Path tempDir) throws IOException {
    String code = "start default\nend default";
    File file = tempDir.resolve("test.qta").toFile();
    Files.writeString(file.toPath(), code);

    ParsedProgram program = KigaliSimFacade.parseAndInterpret(file.getPath());
    assertNotNull(program, "Program should not be null");
  }

  /**
   * Test that runScenario method executes without errors and iterates through years.
   */
  @Test
  public void testRunScenario(@TempDir Path tempDir) throws IOException {
    String code = "start default\nend default\n\nstart simulations\n  simulate \"test\" from years 1 to 3\nend simulations";
    File file = tempDir.resolve("simulation.qta").toFile();
    Files.writeString(file.toPath(), code);

    ParsedProgram program = KigaliSimFacade.parseAndInterpret(file.getPath());
    assertNotNull(program, "Program should not be null");

    // This should not throw an exception and should iterate through years
    // Use a no-op progress callback
    KigaliSimFacade.runScenario(program, "test", progress -> {});
  }

  /**
   * Test that runScenario properly iterates through years with policy changes.
   */
  @Test
  public void testRunScenarioIteratesThroughYears(@TempDir Path tempDir) throws IOException {
    // Use the example file from examples directory
    String examplePath = "../examples/test_year_iteration.qta";
    File exampleFile = new File(examplePath);
    assertTrue(exampleFile.exists(), "Example file should exist: " + examplePath);

    // Parse and interpret the example file
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(exampleFile.getPath());
    assertNotNull(program, "Program should not be null");

    // This should run through all years without throwing an exception
    // Use a no-op progress callback
    KigaliSimFacade.runScenario(program, "yeartest", progress -> {});
  }

  /**
   * Test that runScenarioWithResults method executes and returns a stream.
   */
  @Test
  public void testRunScenarioWithResults(@TempDir Path tempDir) throws IOException {
    // Use the example file from examples directory
    String examplePath = "../examples/test_year_iteration.qta";
    File exampleFile = new File(examplePath);
    assertTrue(exampleFile.exists(), "Example file should exist: " + examplePath);

    // Parse and interpret the example file
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(exampleFile.getPath());
    assertNotNull(program, "Program should not be null");

    // This should run through all years and return a stream (may be empty for basic examples)
    // Use a no-op progress callback
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, "yeartest", progress -> {});
    assertNotNull(results, "Results stream should not be null");

    // Collect results - may be empty for basic examples that don't have all required streams
    List<EngineResult> resultsList = results.collect(java.util.stream.Collectors.toList());
    assertNotNull(resultsList, "Results list should not be null");

    // The method should complete successfully even if no results are collected
    // This tests that the infrastructure works
  }

  /**
   * Test that convertResultsToCsv outputs individual metrics without 'all' aggregations.
   */
  @Test
  public void testConvertResultsToCsvIndividualMetrics(@TempDir Path tempDir) throws IOException {
    // Use a comprehensive example file
    String examplePath = "../examples/minimal_interpreter.qta";
    File exampleFile = new File(examplePath);
    assertTrue(exampleFile.exists(), "Example file should exist: " + examplePath);

    // Parse and interpret the example file
    ParsedProgram program = KigaliSimFacade.parseAndInterpret(exampleFile.getPath());
    assertNotNull(program, "Program should not be null");

    // Run scenario and collect results
    // Use a no-op progress callback
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, "business as usual", progress -> {});
    List<EngineResult> resultsList = results.collect(java.util.stream.Collectors.toList());

    // Convert to CSV
    String csvOutput = KigaliSimFacade.convertResultsToCsv(resultsList);
    assertNotNull(csvOutput, "CSV output should not be null");

    // Verify CSV header contains individual metrics only
    String[] lines = csvOutput.split("\n");
    assertTrue(lines.length > 0, "CSV should have at least a header line");
    
    String header = lines[0];
    
    // Verify individual metrics are present
    assertTrue(header.contains("manufacture"), "CSV should contain manufacture column");
    assertTrue(header.contains("import"), "CSV should contain import column");
    assertTrue(header.contains("recycle"), "CSV should contain recycle column");
    assertTrue(header.contains("domesticConsumption"), "CSV should contain domesticConsumption column");
    assertTrue(header.contains("importConsumption"), "CSV should contain importConsumption column");
    assertTrue(header.contains("recycleConsumption"), "CSV should contain recycleConsumption column");
    assertTrue(header.contains("rechargeEmissions"), "CSV should contain rechargeEmissions column");
    assertTrue(header.contains("eolEmissions"), "CSV should contain eolEmissions column");
    
    // Verify 'all' aggregated columns are NOT present (they shouldn't exist in Java)
    assertFalse(header.contains("allSales"), "CSV should not contain allSales column");
    assertFalse(header.contains("totalSales"), "CSV should not contain totalSales column");
    assertFalse(header.contains("allEmissions"), "CSV should not contain allEmissions column");
    assertFalse(header.contains("totalEmissions"), "CSV should not contain totalEmissions column");
    assertFalse(header.contains("allConsumption"), "CSV should not contain allConsumption column");
    
    // Verify proper CSV structure for non-empty results
    if (resultsList.size() > 0) {
      assertTrue(lines.length > 1, "CSV should have data rows for non-empty results");
    }
  }

  /**
   * Test that getNumberTotalTrials calculates the correct total.
   */
  @Test
  public void testGetNumberTotalTrials(@TempDir Path tempDir) throws IOException {
    // Create a program with multiple scenarios and different trial counts
    String code = "start default\nend default\n\n"
                  + "start simulations\n"
                  + "  simulate \"scenario1\" from years 1 to 3 across 5 trials\n"
                  + "  simulate \"scenario2\" from years 1 to 3 across 3 trials\n"
                  + "  simulate \"scenario3\" from years 1 to 3 across 2 trials\n"
                  + "end simulations";
    File file = tempDir.resolve("multi_trial.qta").toFile();
    Files.writeString(file.toPath(), code);

    ParsedProgram program = KigaliSimFacade.parseAndInterpret(file.getPath());
    assertNotNull(program, "Program should not be null");

    // Test that total trials is correctly calculated (5 + 3 + 2 = 10)
    int totalTrials = KigaliSimFacade.getNumberTotalTrials(program);
    assertEquals(10, totalTrials, "Total trials should be 10");
  }

  /**
   * Test that getNumberTotalTrials works with single scenario.
   */
  @Test
  public void testGetNumberTotalTrialsSingleScenario(@TempDir Path tempDir) throws IOException {
    String code = "start default\nend default\n\n"
                  + "start simulations\n"
                  + "  simulate \"only\" from years 1 to 3 across 7 trials\n"
                  + "end simulations";
    File file = tempDir.resolve("single_trial.qta").toFile();
    Files.writeString(file.toPath(), code);

    ParsedProgram program = KigaliSimFacade.parseAndInterpret(file.getPath());
    int totalTrials = KigaliSimFacade.getNumberTotalTrials(program);
    assertEquals(7, totalTrials, "Total trials should be 7");
  }

  /**
   * Test that progress callback is invoked with correct values.
   */
  @Test
  public void testProgressCallbackInvocation(@TempDir Path tempDir) throws IOException {
    // Create a program with multiple trials
    String code = "start default\nend default\n\n"
                  + "start simulations\n"
                  + "  simulate \"test\" from years 1 to 2 across 3 trials\n"
                  + "end simulations";
    File file = tempDir.resolve("progress_test.qta").toFile();
    Files.writeString(file.toPath(), code);

    ParsedProgram program = KigaliSimFacade.parseAndInterpret(file.getPath());
    
    // Track progress calls
    List<Double> progressValues = new ArrayList<>();
    ProgressReportCallback callback = progress -> progressValues.add(progress);
    
    // Run scenario with progress tracking
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, "test", callback);
    results.collect(java.util.stream.Collectors.toList()); // Force stream evaluation
    
    // Verify progress was reported for each trial
    assertEquals(3, progressValues.size(), "Progress should be reported 3 times (once per trial)");
    
    // Verify progress values are correct (1/3, 2/3, 3/3)
    assertEquals(1.0 / 3.0, progressValues.get(0), 0.001, "First progress should be ~0.333");
    assertEquals(2.0 / 3.0, progressValues.get(1), 0.001, "Second progress should be ~0.667");
    assertEquals(1.0, progressValues.get(2), 0.001, "Final progress should be 1.0");
  }

  /**
   * Test progress callback with multiple scenarios.
   */
  @Test
  public void testProgressCallbackMultipleScenarios(@TempDir Path tempDir) throws IOException {
    String code = "start default\nend default\n\n"
                  + "start simulations\n"
                  + "  simulate \"first\" from years 1 to 2 across 2 trials\n"
                  + "  simulate \"second\" from years 1 to 2 across 3 trials\n"
                  + "end simulations";
    File file = tempDir.resolve("multi_scenario_progress.qta").toFile();
    Files.writeString(file.toPath(), code);

    ParsedProgram program = KigaliSimFacade.parseAndInterpret(file.getPath());
    
    // Track progress for second scenario
    List<Double> progressValues = new ArrayList<>();
    ProgressReportCallback callback = progress -> progressValues.add(progress);
    
    // Run second scenario - should account for completed trials from first scenario
    Stream<EngineResult> results = KigaliSimFacade.runScenario(program, "second", callback);
    results.collect(java.util.stream.Collectors.toList());
    
    // Progress should be reported 3 times (trials for second scenario)
    assertEquals(3, progressValues.size(), "Progress should be reported 3 times");
    
    // Progress values should account for total trials (5) and already completed (2)
    // So: 3/5, 4/5, 5/5
    assertEquals(3.0 / 5.0, progressValues.get(0), 0.001, "First progress should be 0.6");
    assertEquals(4.0 / 5.0, progressValues.get(1), 0.001, "Second progress should be 0.8");
    assertEquals(1.0, progressValues.get(2), 0.001, "Final progress should be 1.0");
  }
}
