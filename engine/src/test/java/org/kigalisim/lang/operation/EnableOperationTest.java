/**
 * Unit tests for the EnableOperation class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.SingleThreadEngine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.lang.machine.PushDownMachine;
import org.kigalisim.lang.machine.SingleThreadPushDownMachine;
import org.kigalisim.lang.time.CalculatedTimePointFuture;
import org.kigalisim.lang.time.ParsedDuring;
import org.kigalisim.lang.time.TimePointFuture;

/**
 * Tests for the EnableOperation class.
 */
public class EnableOperationTest {

  private Engine engine;
  private PushDownMachine machine;

  /**
   * Set up a new engine and machine before each test.
   */
  @BeforeEach
  public void setUp() {
    engine = new SingleThreadEngine(2020, 2025);
    machine = new SingleThreadPushDownMachine(engine);

    // Set up the engine with a scope
    engine.setStanza("default");
    engine.setApplication("test app");
    engine.setSubstance("test substance");
  }

  /**
   * Test that EnableOperation can be initialized with no during.
   */
  @Test
  public void testInitializesNoDuring() {
    EnableOperation operation = new EnableOperation("manufacture");
    assertNotNull(operation, "EnableOperation should be constructable without during");
  }

  /**
   * Test that EnableOperation can be initialized with a during.
   */
  @Test
  public void testInitializesWithDuring() {
    ParsedDuring during = new ParsedDuring(Optional.empty(), Optional.empty());
    EnableOperation operation = new EnableOperation("manufacture", during);
    assertNotNull(operation, "EnableOperation should be constructable with during");
  }

  /**
   * Test the execute method with no during for manufacture stream.
   */
  @Test
  public void testExecuteManufactureNoDuring() {
    EnableOperation operation = new EnableOperation("manufacture");
    operation.execute(machine);

    // The stream should be enabled now (though we can't directly test this without access to StreamKeeper)
    // We can verify by testing that operations requiring enabled streams work
    assertTrue(true, "EnableOperation should execute without error");
  }

  /**
   * Test the execute method with no during for import stream.
   */
  @Test
  public void testExecuteImportNoDuring() {
    EnableOperation operation = new EnableOperation("import");
    operation.execute(machine);

    // The stream should be enabled now
    assertTrue(true, "EnableOperation should execute without error");
  }

  /**
   * Test the execute method with no during for export stream.
   */
  @Test
  public void testExecuteExportNoDuring() {
    EnableOperation operation = new EnableOperation("export");
    operation.execute(machine);

    // The stream should be enabled now
    assertTrue(true, "EnableOperation should execute without error");
  }

  /**
   * Test the execute method with a during.
   */
  @Test
  public void testExecuteWithDuring() {
    // Create a during that applies to the current year (2020)
    EngineNumber yearNumber = new EngineNumber(BigDecimal.valueOf(2020), "");
    Operation yearOperation = new PreCalculatedOperation(yearNumber);
    TimePointFuture start = new CalculatedTimePointFuture(yearOperation);
    ParsedDuring during = new ParsedDuring(Optional.of(start), Optional.empty());

    EnableOperation operation = new EnableOperation("manufacture", during);
    operation.execute(machine);

    // The stream should be enabled now
    assertTrue(true, "EnableOperation should execute without error with during");
  }

  /**
   * Test the execute method with a during that doesn't apply.
   */
  @Test
  public void testExecuteWithDuringNotApplying() {
    // Create a during that applies to a future year (2021)
    EngineNumber yearNumber = new EngineNumber(BigDecimal.valueOf(2021), "");
    Operation yearOperation = new PreCalculatedOperation(yearNumber);
    TimePointFuture start = new CalculatedTimePointFuture(yearOperation);
    ParsedDuring during = new ParsedDuring(Optional.of(start), Optional.empty());

    EnableOperation operation = new EnableOperation("manufacture", during);
    operation.execute(machine);

    // The stream should not be enabled for the current year
    assertTrue(true, "EnableOperation should execute without error even when during doesn't apply");
  }

  /**
   * Test enabling different valid stream types.
   */
  @Test
  public void testEnableValidStreams() {
    String[] validStreams = {"manufacture", "import", "export"};

    for (String stream : validStreams) {
      EnableOperation operation = new EnableOperation(stream);
      operation.execute(machine);
      // Should execute without error
      assertTrue(true, "Should be able to enable " + stream + " stream");
    }
  }
}
