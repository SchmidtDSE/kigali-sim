/**
 * Calculation which defines a variable and sets its value in the engine.
 *
 * @license BSD-3-Clause
 */

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

  /**
   * Executes the variable definition operation.
   *
   * <p>This method executes the value operation to obtain the value, defines the variable if it
   * doesn't exist, and then sets the variable value in the engine.
   *
   * @param machine The push-down machine on which to execute the operation.
   */
  @Override
  public void execute(PushDownMachine machine) {
    valueOperation.execute(machine);
    EngineNumber value = machine.getResult();

    machine.getEngine().defineVariable(variableName);

    machine.getEngine().setVariable(variableName, value);
  }
}
