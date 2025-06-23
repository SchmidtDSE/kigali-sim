package org.kigalisim.lang.operation;

import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.lang.machine.PushDownMachine;

/**
 * Operation that retrieves a variable's value from the engine.
 *
 * <p>This operation retrieves the value of a specified variable from the engine.
 */
public class GetVariableOperation implements Operation {

  private final String variableName;

  /**
   * Create a new GetVariableOperation.
   *
   * @param variableName The name of the variable to retrieve.
   */
  public GetVariableOperation(String variableName) {
    this.variableName = variableName;
  }

  @Override
  public void execute(PushDownMachine machine) {
    EngineNumber value = machine.getEngine().getVariable(variableName);
    machine.push(value);
  }
}
