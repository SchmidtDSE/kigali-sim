/**
 * Calculation which retires equipment and immediately replaces it by increasing sales.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import java.math.BigDecimal;
import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.SimulationState;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;
import org.kigalisim.engine.support.EngineSupportUtils;
import org.kigalisim.lang.machine.PushDownMachine;
import org.kigalisim.lang.time.ParsedDuring;


/**
 * Operation that retires equipment and immediately replaces it by increasing sales.
 *
 * <p>This operation executes a normal retirement and then increases sales by the
 * same amount, effectively maintaining the equipment population while simulating equipment
 * turnover.</p>
 */
public class RetireWithReplacementOperation implements Operation {

  private final Operation amountOperation;
  private final Optional<ParsedDuring> duringMaybe;

  /**
   * Create a new RetireWithReplacementOperation that applies to all years.
   *
   * @param amountOperation The operation that calculates the retirement rate.
   */
  public RetireWithReplacementOperation(Operation amountOperation) {
    this.amountOperation = amountOperation;
    duringMaybe = Optional.empty();
  }

  /**
   * Create a new RetireWithReplacementOperation that applies to a specific time period.
   *
   * @param amountOperation The operation that calculates the retirement rate.
   * @param during The time period during which this operation applies.
   */
  public RetireWithReplacementOperation(Operation amountOperation, ParsedDuring during) {
    this.amountOperation = amountOperation;
    duringMaybe = Optional.of(during);
  }

  /**
   * Execute the retire and replacement operation.
   *
   * <div>
 * This operation executes a normal retirement and then increases sales by the actual retirement
 * amount, effectively maintaining the equipment population while simulating equipment turnover. The
 * operation:
   *
   * <ul>
   * <li>Executes the amount operation to calculate the retirement rate</li>
   * <li>Validates that the current year is in the specified time range</li>
   * <li>Checks for mixed retire commands with and without replacement</li>
   * <li>Determines target units for replacement based on last specified sales units</li>
   * <li>Measures equipment before and after retirement to calculate actual reduction</li>
   * <li>Increases sales by the actual reduction amount to replace retired equipment</li>
   * </ul>
   * </div>
   */
  @Override
  public void execute(PushDownMachine machine) {
    amountOperation.execute(machine);

    ParsedDuring parsedDuring = duringMaybe.orElseGet(
        () -> new ParsedDuring(Optional.empty(), Optional.empty())
    );
    YearMatcher yearMatcher = parsedDuring.buildYearMatcher(machine);

    Engine engine = machine.getEngine();

    // Check if this operation should execute in the current year
    if (!EngineSupportUtils.getIsInRange(yearMatcher, engine.getYear())) {
      return;
    }

    handleMixedReplacement(engine);

    String targetUnits = determineTargetUnits(engine);

    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(engine, "sales");
    handleReduction(machine, engine, yearMatcher, unitConverter, targetUnits);
  }

  /**
   * Check for mixed retire commands with and without replacement.
   *
   * <div>
 * Validates that if retirement has already been calculated in this step, the current operation also
 * has replacement enabled. Throws an exception if there's a mix of retire commands with and without
 * replacement in the same step.
   * </div>
   *
   * @param engine The current simulation engine.
   */
  private void handleMixedReplacement(Engine engine) {
    SimulationState simulationState = engine.getStreamKeeper();
    UseKey scope = engine.getScope();
    boolean retireCalculated = simulationState.getRetireCalculatedThisStep(scope);
    if (retireCalculated) {
      boolean currentReplacement = simulationState.getHasReplacementThisStep(scope);
      if (!currentReplacement) {
        throw new RuntimeException(
            "Cannot mix retire commands with and without replacement in same step for "
            + scope.getApplication() + "/" + scope.getSubstance());
      }
    }
    simulationState.setHasReplacementThisStep(scope, true);
  }

  /**
   * Determine the target units for replacement.
   *
   * <div>
 * Checks what units sales were last specified in and returns the appropriate target units for
 * replacement. If sales were specified in equipment units, returns "units"; otherwise returns "kg".
   * </div>
   *
   * @param engine The current simulation engine.
   * @return The target units for replacement ("units" or "kg").
   */
  private String determineTargetUnits(Engine engine) {
    SimulationState simulationState = engine.getStreamKeeper();
    UseKey scope = engine.getScope();
    EngineNumber lastSalesValue = simulationState.getLastSpecifiedValue(scope, "sales");

    if (lastSalesValue != null && lastSalesValue.hasEquipmentUnits()) {
      return "units";
    } else {
      return "kg";
    }
  }

  /**
   * Handle the retirement and replacement of equipment.
   *
   * <div>
 * Gets equipment level before retirement to measure actual reduction, executes the retirement which
 * reduces equipment population, then gets equipment level after retirement and calculates actual
 * reduction. If there was actual retirement (non-zero amount), adds replacement by increasing sales
 * by the actual reduction amount, which increases the equipment population back to compensate for
 * retirement.
   * </div>
   *
   * @param machine The current push down machine.
   * @param engine The current simulation engine.
   * @param yearMatcher The year matcher for this operation.
   * @param unitConverter The unit converter to use for conversions.
   * @param targetUnits The target units for replacement.
   */
  private void handleReduction(PushDownMachine machine, Engine engine, YearMatcher yearMatcher,
      UnitConverter unitConverter, String targetUnits) {
    EngineNumber equipmentBefore = unitConverter.convert(engine.getStream("equipment"), "units");

    final EngineNumber retireAmount = machine.getResult();
    engine.retire(retireAmount, yearMatcher);

    EngineNumber equipmentAfter = unitConverter.convert(engine.getStream("equipment"), "units");
    BigDecimal actualReduction = equipmentBefore.getValue().subtract(equipmentAfter.getValue());

    boolean hadReduction = actualReduction.compareTo(BigDecimal.ZERO) > 0;
    if (hadReduction) {
      EngineNumber replacementAmount = unitConverter.convert(
          new EngineNumber(actualReduction, "units"),
          targetUnits
      );
      engine.changeStream("sales", replacementAmount, yearMatcher);
    }
  }
}
