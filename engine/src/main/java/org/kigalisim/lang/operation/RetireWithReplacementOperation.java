package org.kigalisim.lang.operation;

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
 * same amount, effectively maintaining the equipment population while simulating
 * equipment turnover.</p>
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

  @Override
  public void execute(PushDownMachine machine) {
    amountOperation.execute(machine);
    EngineNumber retireAmount = machine.getResult();

    ParsedDuring parsedDuring = duringMaybe.orElseGet(
        () -> new ParsedDuring(Optional.empty(), Optional.empty())
    );
    YearMatcher yearMatcher = parsedDuring.buildYearMatcher(machine);

    Engine engine = machine.getEngine();

    // Check if this operation should execute in the current year
    if (!EngineSupportUtils.isInRange(yearMatcher, engine.getYear())) {
      return;
    }

    UseKey scope = engine.getScope();

    // Step 1: Determine the target units for replacement
    // Check what units sales were last specified in
    SimulationState simulationState = engine.getStreamKeeper();
    EngineNumber lastSalesValue = simulationState.getLastSpecifiedValue(scope, "sales");

    String targetUnits;
    if (lastSalesValue != null && lastSalesValue.hasEquipmentUnits()) {
      // Sales were specified in units - replacement should be in units
      targetUnits = "units";
    } else {
      // Sales were specified in volume or not at all - replacement should be in kg
      targetUnits = "kg";
    }

    // Step 2: Get equipment level BEFORE retirement
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(engine, "sales");
    EngineNumber equipmentBefore = unitConverter.convert(engine.getStream("equipment"), "units");

    // Step 3: Execute the retirement (reduces equipment population)
    engine.retire(retireAmount, yearMatcher);

    // Step 4: Get equipment level AFTER retirement and calculate actual reduction
    EngineNumber equipmentAfter = unitConverter.convert(engine.getStream("equipment"), "units");
    java.math.BigDecimal actualReduction = equipmentBefore.getValue().subtract(equipmentAfter.getValue());

    // Step 5: Add replacement by increasing sales by the actual reduction amount
    // This increases the equipment population back to compensate for retirement
    // Only add replacement if there was actual retirement (non-zero amount)
    if (actualReduction.compareTo(java.math.BigDecimal.ZERO) > 0) {
      EngineNumber replacementAmount = unitConverter.convert(
          new EngineNumber(actualReduction, "units"),
          targetUnits
      );
      engine.changeStream("sales", replacementAmount, yearMatcher);
    }
  }
}
