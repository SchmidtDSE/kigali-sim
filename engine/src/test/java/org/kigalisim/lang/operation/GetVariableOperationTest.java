/**
 * Unit tests for the GetVariableOperation class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.SingleThreadEngine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.lang.machine.PushDownMachine;
import org.kigalisim.lang.machine.SingleThreadPushDownMachine;

/**
 * Tests for the GetVariableOperation class.
 */
public class GetVariableOperationTest {

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
   * Test that GetVariableOperation can be initialized.
   */
  @Test
  public void testInitializes() {
    GetVariableOperation operation = new GetVariableOperation("testVar");
    assertNotNull(operation, "GetVariableOperation should be constructable");
  }

  /**
   * Test the execute method when the variable exists.
   */
  @Test
  public void testExecuteVariableExists() {
    // Define and set a variable in the engine
    String variableName = "testVar";
    EngineNumber expectedValue = new EngineNumber(BigDecimal.valueOf(42), "kg");
    engine.defineVariable(variableName);
    engine.setVariable(variableName, expectedValue);

    // Create and execute the operation
    GetVariableOperation operation = new GetVariableOperation(variableName);
    operation.execute(machine);

    // Verify the result
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(42), result.getValue(), "Variable value should be retrieved correctly");
    assertEquals("kg", result.getUnits(), "Variable units should be retrieved correctly");
  }

  /**
   * Test the execute method when the variable doesn't exist.
   */
  @Test
  public void testExecuteVariableDoesNotExist() {
    // Create and execute the operation for a non-existent variable
    GetVariableOperation operation = new GetVariableOperation("nonExistentVar");

    // This should throw an exception
    assertThrows(RuntimeException.class, () -> operation.execute(machine),
        "Should throw exception when variable doesn't exist");
  }

  /**
   * Test the execute method with a variable that has no units.
   */
  @Test
  public void testExecuteVariableWithNoUnits() {
    // Define and set a variable with no units in the engine
    String variableName = "unitlessVar";
    EngineNumber expectedValue = new EngineNumber(BigDecimal.valueOf(123), "");
    engine.defineVariable(variableName);
    engine.setVariable(variableName, expectedValue);

    // Create and execute the operation
    GetVariableOperation operation = new GetVariableOperation(variableName);
    operation.execute(machine);

    // Verify the result
    EngineNumber result = machine.getResult();
    assertEquals(BigDecimal.valueOf(123), result.getValue(), "Variable value should be retrieved correctly");
    assertEquals("", result.getUnits(), "Variable should have no units");
  }
}
