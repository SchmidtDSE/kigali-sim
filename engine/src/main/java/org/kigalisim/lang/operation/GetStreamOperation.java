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
 *
 * <p>Retrieves a stream value from the engine's current scope or from a specified substance scope.
 * Supports optional unit conversion and optional lookback to prior years via the {@code yearsPast}
 * parameter.</p>
 */
public class GetStreamOperation implements Operation {

  private final String streamName;
  private final Optional<String> units;
  private final Optional<String> targetSubstance;
  private final int yearsPast;

  /**
   * Create a new GetStreamOperation.
   *
   * @param streamName The name of the stream to get.
   */
  public GetStreamOperation(String streamName) {
    this(streamName, 0);
  }

  /**
   * Create a new GetStreamOperation with a lookback period.
   *
   * @param streamName The name of the stream to get.
   * @param yearsPast The number of years to look back. Zero means current year.
   */
  public GetStreamOperation(String streamName, int yearsPast) {
    this.streamName = streamName;
    this.units = Optional.empty();
    this.targetSubstance = Optional.empty();
    this.yearsPast = yearsPast;
  }

  /**
   * Create a new GetStreamOperation with unit conversion.
   *
   * @param streamName The name of the stream to get.
   * @param units The units to convert to.
   */
  public GetStreamOperation(String streamName, String units) {
    this(streamName, 0);
    this.units = Optional.of(units);
  }

  /**
   * Create a new GetStreamOperation with unit conversion and lookback period.
   *
   * @param streamName The name of the stream to get.
   * @param yearsPast The number of years to look back. Zero means current year.
   * @param units The units to convert to.
   */
  public GetStreamOperation(String streamName, int yearsPast, String units) {
    this.streamName = streamName;
    this.units = Optional.of(units);
    this.targetSubstance = Optional.empty();
    this.yearsPast = yearsPast;
  }

  /**
   * Create a new GetStreamOperation with scope resolution and unit conversion.
   *
   * @param streamName The name of the stream to get.
   * @param targetSubstance The substance name to resolve to (for indirect access).
   * @param units The units to convert to.
   */
  public GetStreamOperation(String streamName, String targetSubstance, String units) {
    this(streamName, 0, targetSubstance, units);
  }

  /**
   * Create a new GetStreamOperation with scope resolution, unit conversion, and lookback period.
   *
   * @param streamName The name of the stream to get.
   * @param yearsPast The number of years to look back. Zero means current year.
   * @param targetSubstance The substance name to resolve to (for indirect access).
   * @param units The units to convert to.
   */
  public GetStreamOperation(String streamName, int yearsPast, String targetSubstance, String units) {
    this.streamName = streamName;
    this.units = Optional.of(units);
    this.targetSubstance = Optional.of(targetSubstance);
    this.yearsPast = yearsPast;
  }

  /** {@inheritDoc} */
  @Override
  public void execute(PushDownMachine machine) {
    Engine engine = machine.getEngine();

    EngineNumber value;
    boolean hasOtherScope = targetSubstance.isPresent();
    if (hasOtherScope) {
      value = executeWithOtherScope(engine);
    } else {
      value = executeSameScope(engine);
    }

    machine.push(value);
  }

  /**
   * Get stream value from a different substance scope.
   *
   * <div>
   * Indirect access: get stream from specified substance. Creates a new scope pointing to the target
   * substance within the same application.
   * </div>
   *
   * @param engine The engine instance to query for stream values.
   * @return The stream value with scope resolution and optional unit conversion.
   */
  private EngineNumber executeWithOtherScope(Engine engine) {
    Scope currentScope = engine.getScope();
    Scope targetScope = currentScope.getWithSubstance(targetSubstance.get());

    if (units.isPresent()) {
      return engine.getStream(streamName, Optional.of(targetScope), units, yearsPast);
    } else {
      return engine.getStream(streamName, Optional.of(targetScope), Optional.empty(), yearsPast);
    }
  }

  /**
   * Get stream value from the current scope.
   *
   * <div>
   * Direct access: get stream from the current scope, with optional unit conversion.
   * </div>
   *
   * @param engine The engine instance to query for stream values.
   * @return The stream value with optional unit conversion.
   */
  private EngineNumber executeSameScope(Engine engine) {
    if (units.isPresent()) {
      Scope scope = engine.getScope();
      return engine.getStream(streamName, Optional.of(scope), units, yearsPast);
    } else {
      return engine.getStream(streamName, Optional.empty(), Optional.empty(), yearsPast);
    }
  }
}
