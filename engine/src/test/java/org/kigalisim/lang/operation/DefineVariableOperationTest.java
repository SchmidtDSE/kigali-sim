/**
 * Unit tests for the DefineVariableOperation class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.SingleThreadEngine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.lang.machine.PushDownMachine;
import org.kigalisim.lang.machine.SingleThreadPushDownMachine;

/**
 * Tests for the DefineVariableOperation class.
 */
public class DefineVariableOperationTest {

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
   * Test that DefineVariableOperation can be initialized.
   */
  @Test
  public void testInitializes() {
    Operation valueOperation = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(42), "kg"));
    DefineVariableOperation operation = new DefineVariableOperation("testVar", valueOperation);
    assertNotNull(operation, "DefineVariableOperation should be constructable");
  }

  /**
   * Test the execute method with a simple value operation.
   */
  @Test
  public void testExecuteWithSimpleValue() {
    // Create a simple value operation
    EngineNumber expectedValue = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(expectedValue);
    
    // Create and execute the operation
    String variableName = "testVar";
    DefineVariableOperation operation = new DefineVariableOperation(variableName, valueOperation);
    operation.execute(machine);

    // Verify the variable was defined and set in the engine
    EngineNumber result = engine.getVariable(variableName);
    assertEquals(BigDecimal.valueOf(42), result.getValue(), "Variable value should be set correctly");
    assertEquals("kg", result.getUnits(), "Variable units should be set correctly");
    
    // Verify the value was pushed onto the stack
    EngineNumber stackResult = machine.getResult();
    assertEquals(BigDecimal.valueOf(42), stackResult.getValue(), "Stack should have the variable value");
    assertEquals("kg", stackResult.getUnits(), "Stack should have the variable units");
  }

  /**
   * Test the execute method with a complex value operation.
   */
  @Test
  public void testExecuteWithComplexValueOperation() {
    // Create a complex value operation (addition)
    Operation left = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(30), "kg"));
    Operation right = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(12), "kg"));
    Operation valueOperation = new AdditionOperation(left, right); // 30 + 12 = 42
    
    // Create and execute the operation
    String variableName = "testVar";
    DefineVariableOperation operation = new DefineVariableOperation(variableName, valueOperation);
    operation.execute(machine);

    // Verify the variable was defined and set in the engine
    EngineNumber result = engine.getVariable(variableName);
    assertEquals(BigDecimal.valueOf(42), result.getValue(), "Variable value should be calculated and set correctly");
    assertEquals("kg", result.getUnits(), "Variable units should be set correctly");
    
    // Verify the value was pushed onto the stack
    EngineNumber stackResult = machine.getResult();
    assertEquals(BigDecimal.valueOf(42), stackResult.getValue(), "Stack should have the calculated variable value");
    assertEquals("kg", stackResult.getUnits(), "Stack should have the variable units");
  }

  /**
   * Test the execute method with a value that has no units.
   */
  @Test
  public void testExecuteWithNoUnits() {
    // Create a value operation with no units
    EngineNumber expectedValue = new EngineNumber(BigDecimal.valueOf(123), "");
    Operation valueOperation = new PreCalculatedOperation(expectedValue);
    
    // Create and execute the operation
    String variableName = "unitlessVar";
    DefineVariableOperation operation = new DefineVariableOperation(variableName, valueOperation);
    operation.execute(machine);

    // Verify the variable was defined and set in the engine
    EngineNumber result = engine.getVariable(variableName);
    assertEquals(BigDecimal.valueOf(123), result.getValue(), "Variable value should be set correctly");
    assertEquals("", result.getUnits(), "Variable should have no units");
    
    // Verify the value was pushed onto the stack
    EngineNumber stackResult = machine.getResult();
    assertEquals(BigDecimal.valueOf(123), stackResult.getValue(), "Stack should have the variable value");
    assertEquals("", stackResult.getUnits(), "Stack should have no units");
  }

  /**
   * Test redefining an existing variable.
   */
  @Test
  public void testRedefineExistingVariable() {
    // First define a variable
    String variableName = "existingVar";
    EngineNumber initialValue = new EngineNumber(BigDecimal.valueOf(10), "kg");
    engine.defineVariable(variableName);
    engine.setVariable(variableName, initialValue);
    
    // Now redefine it with a new value
    EngineNumber newValue = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(newValue);
    DefineVariableOperation operation = new DefineVariableOperation(variableName, valueOperation);
    operation.execute(machine);

    // Verify the variable was updated in the engine
    EngineNumber result = engine.getVariable(variableName);
    assertEquals(BigDecimal.valueOf(42), result.getValue(), "Variable value should be updated correctly");
    assertEquals("kg", result.getUnits(), "Variable units should be updated correctly");
  }
}