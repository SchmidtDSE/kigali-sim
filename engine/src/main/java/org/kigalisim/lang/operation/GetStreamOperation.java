/**
 * Operation to get a stream value.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.lang.machine.PushDownMachine;

/**
 * Operation to get a stream value.
 */
public class GetStreamOperation implements Operation {

  private final String streamName;
  private final Optional<String> units;
  private final Optional<String> targetSubstance;

  /**
   * Create a new GetStreamOperation.
   *
   * @param streamName The name of the stream to get.
   */
  public GetStreamOperation(String streamName) {
    this.streamName = streamName;
    this.units = Optional.empty();
    this.targetSubstance = Optional.empty();
  }

  /**
   * Create a new GetStreamOperation with unit conversion.
   *
   * @param streamName The name of the stream to get.
   * @param units The units to convert to.
   */
  public GetStreamOperation(String streamName, String units) {
    this.streamName = streamName;
    this.units = Optional.of(units);
    this.targetSubstance = Optional.empty();
  }

  /**
   * Create a new GetStreamOperation with scope resolution and unit conversion.
   *
   * @param streamName The name of the stream to get.
   * @param targetSubstance The substance name to resolve to (for indirect access).
   * @param units The units to convert to.
   */
  public GetStreamOperation(String streamName, String targetSubstance, String units) {
    this.streamName = streamName;
    this.targetSubstance = Optional.of(targetSubstance);
    this.units = Optional.of(units);
  }

  @Override
  public void execute(PushDownMachine machine) {
    // Get the engine
    Engine engine = machine.getEngine();

    // Get the stream value, with or without unit conversion and scope resolution
    EngineNumber value;
    boolean hasOtherScope = targetSubstance.isPresent();
    if (hasOtherScope) {
      // Indirect access: get stream from specified substance
      Scope currentScope = engine.getScope();

      // Create a new scope pointing to the target substance within the same application
      Scope targetScope = currentScope.getWithSubstance(targetSubstance.get());

      if (units.isPresent()) {
        value = engine.getStream(streamName, Optional.of(targetScope), units);
      } else {
        value = engine.getStream(streamName, Optional.of(targetScope), Optional.empty());
      }
    } else {
      if (units.isPresent()) {
        Scope scope = engine.getScope();
        value = engine.getStream(streamName, Optional.of(scope), units);
      } else {
        value = engine.getStream(streamName);
      }
    }

    // Push the value onto the stack
    machine.push(value);
  }
}
