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
  private final String units;

  /**
   * Create a new GetStreamOperation.
   *
   * @param streamName The name of the stream to get.
   */
  public GetStreamOperation(String streamName) {
    this.streamName = streamName;
    this.units = null;
  }

  /**
   * Create a new GetStreamOperation with unit conversion.
   *
   * @param streamName The name of the stream to get.
   * @param units The units to convert to.
   */
  public GetStreamOperation(String streamName, String units) {
    this.streamName = streamName;
    this.units = units;
  }

  @Override
  public void execute(PushDownMachine machine) {
    // Get the engine
    Engine engine = machine.getEngine();

    // Get the stream value, with or without unit conversion
    EngineNumber value;
    if (units != null) {
      Scope scope = engine.getScope();
      value = engine.getStream(streamName, Optional.of(scope), Optional.of(units));
    } else {
      value = engine.getStream(streamName);
    }

    // Push the value onto the stack
    machine.push(value);
  }
}
