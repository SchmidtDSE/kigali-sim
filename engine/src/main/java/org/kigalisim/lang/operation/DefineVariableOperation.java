package org.kigalisim.lang.operation;

import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.lang.machine.PushDownMachine;

/**
 * Operation that defines a variable and sets its value in the engine.
 *
 * <p>This operation defines a variable with the specified name and sets its value in the engine.
 */
public class DefineVariableOperation implements Operation {

  private final String variableName;
  private final Operation valueOperation;

  /**
   * Create a new DefineVariableOperation.
   *
   * @param variableName The name of the variable to define.
   * @param valueOperation The operation that calculates the value to set.
   */
  public DefineVariableOperation(String variableName, Operation valueOperation) {
    this.variableName = variableName;
    this.valueOperation = valueOperation;
  }

  @Override
  public void execute(PushDownMachine machine) {
    // Execute the value operation to get the value
    valueOperation.execute(machine);
    EngineNumber value = machine.getResult();

    // Define the variable if it doesn't exist already
    machine.getEngine().defineVariable(variableName);

    // Set the variable value in the engine
    machine.getEngine().setVariable(variableName, value);

    // Push the value back onto the stack
    machine.push(value);
  }
}
