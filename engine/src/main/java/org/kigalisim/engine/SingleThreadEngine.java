/**
 * Single-threaded implementation of the Engine interface.
 *
 * <p>This class provides a concrete implementation of the Engine interface that is not
 * designed to be thread-safe. It translates the functionality from the JavaScript
 * Engine implementation to Java, using BigDecimal for numerical stability.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.ConverterStateGetter;
import org.kigalisim.engine.state.OverridingConverterStateGetter;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.engine.state.StreamKeeper;
import org.kigalisim.engine.state.YearMatcher;

/**
 * Single-threaded implementation of the Engine interface.
 *
 * <p>This implementation provides the core simulation engine functionality
 * without thread safety considerations. It manages substance streams, equipment
 * populations, and various calculations related to the Montreal Protocol simulation.</p>
 */
public class SingleThreadEngine implements Engine {

  private static final Set<String> STREAM_NAMES = new HashSet<>();
  private static final boolean OPTIMIZE_RECALCS = true;

  static {
    STREAM_NAMES.add("priorEquipment");
    STREAM_NAMES.add("equipment");
    STREAM_NAMES.add("export");
    STREAM_NAMES.add("import");
    STREAM_NAMES.add("manufacture");
    STREAM_NAMES.add("sales");
  }

  private final int startYear;
  private final int endYear;
  private int currentYear;

  private final ConverterStateGetter stateGetter;
  private final UnitConverter unitConverter;
  private final StreamKeeper streamKeeper;
  private Scope scope;

  /**
   * Create a new SingleThreadEngine instance.
   *
   * @param startYear The starting year of the simulation
   * @param endYear The ending year of the simulation
   */
  public SingleThreadEngine(int startYear, int endYear) {
    // Ensure start year is less than or equal to end year
    int startYearRearrange = Math.min(startYear, endYear);
    int endYearRearrange = Math.max(startYear, endYear);

    this.startYear = startYearRearrange;
    this.endYear = endYearRearrange;
    this.currentYear = this.startYear;

    this.stateGetter = new ConverterStateGetter(this);
    this.unitConverter = new UnitConverter(this.stateGetter);
    this.streamKeeper = new StreamKeeper(
        new OverridingConverterStateGetter(this.stateGetter), this.unitConverter);
    this.scope = new Scope(null, null, null);
  }

  @Override
  public int getStartYear() {
    return startYear;
  }

  @Override
  public int getEndYear() {
    return endYear;
  }

  @Override
  public void setStanza(String newStanza) {
    this.scope = this.scope.getWithStanza(newStanza);
  }

  @Override
  public void setApplication(String newApplication) {
    this.scope = this.scope.getWithApplication(newApplication);
  }

  @Override
  public void setSubstance(String newSubstance, Boolean checkValid) {
    this.scope = this.scope.getWithSubstance(newSubstance);

    boolean checkValidEffective = checkValid != null && checkValid;
    String application = this.scope.getApplication();

    if (checkValidEffective) {
      boolean knownSubstance = this.streamKeeper.hasSubstance(application, newSubstance);
      if (!knownSubstance) {
        throw new RuntimeException("Tried accessing unknown app / substance pair: "
            + application + ", " + newSubstance);
      }
    } else {
      this.streamKeeper.ensureSubstance(application, newSubstance);
    }
  }

  @Override
  public void setSubstance(String newSubstance) {
    setSubstance(newSubstance, false);
  }

  @Override
  public Scope getScope() {
    return scope;
  }

  @Override
  public void incrementYear() {
    if (getIsDone()) {
      throw new RuntimeException("Already completed.");
    }
    this.currentYear += 1;
    this.streamKeeper.incrementYear();
  }

  @Override
  public int getYear() {
    return currentYear;
  }

  @Override
  public boolean getIsDone() {
    return currentYear > endYear;
  }

  @Override
  public void setStream(String name, EngineNumber value, YearMatcher yearMatcher, Scope scope,
      boolean propagateChanges, String unitsToRecord) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    Scope scopeEffective = scope != null ? scope : this.scope;
    String application = scopeEffective.getApplication();
    String substance = scopeEffective.getSubstance();

    if (application == null || substance == null) {
      throw new RuntimeException(
          "Tried setting stream without application and substance specified.");
    }

    this.streamKeeper.setStream(application, substance, name, value);

    // Track the units last used to specify this stream (only for user-initiated calls)
    if (propagateChanges && unitsToRecord != null) {
      String unitsToRecordRealized = unitsToRecord != null ? unitsToRecord : value.getUnits();
      this.streamKeeper.setLastSpecifiedUnits(application, substance, unitsToRecordRealized);
    }

    if (!propagateChanges) {
      return;
    }

    if ("sales".equals(name) || "manufacture".equals(name) || "import".equals(name)) {
      recalcPopulationChange(scopeEffective, !value.hasEquipmentUnits());
      recalcConsumption(scopeEffective);
      if (!OPTIMIZE_RECALCS) {
        recalcSales(scopeEffective);
      }
    } else if ("consumption".equals(name)) {
      recalcSales(scopeEffective);
      recalcPopulationChange(scopeEffective, null);
      if (!OPTIMIZE_RECALCS) {
        recalcConsumption(scopeEffective);
      }
    } else if ("equipment".equals(name)) {
      recalcSales(scopeEffective);
      recalcConsumption(scopeEffective);
      if (!OPTIMIZE_RECALCS) {
        recalcPopulationChange(scopeEffective, null);
      }
    } else if ("priorEquipment".equals(name)) {
      recalcRetire(scopeEffective);
    }
  }

  @Override
  public void setStream(String name, EngineNumber value, YearMatcher yearMatcher) {
    setStream(name, value, yearMatcher, null, true, null);
  }

  @Override
  public EngineNumber getStream(String name, Scope scope, String conversion) {
    Scope scopeEffective = scope != null ? scope : this.scope;
    String application = scopeEffective.getApplication();
    String substance = scopeEffective.getSubstance();
    EngineNumber value = this.streamKeeper.getStream(application, substance, name);

    if (conversion == null) {
      return value;
    } else {
      return this.unitConverter.convert(value, conversion);
    }
  }

  @Override
  public EngineNumber getStream(String name) {
    return getStream(name, null, null);
  }

  @Override
  public EngineNumber getStreamRaw(String application, String substance, String stream) {
    return this.streamKeeper.getStream(application, substance, stream);
  }

  @Override
  public EngineNumber getGhgIntensity(String application, String substance) {
    return this.streamKeeper.getGhgIntensity(application, substance);
  }

  @Override
  public void defineVariable(String name) {
    if ("yearsElapsed".equals(name) || "yearAbsolute".equals(name)) {
      throw new RuntimeException("Cannot override yearsElapsed or yearAbsolute.");
    }
    this.scope.defineVariable(name);
  }

  @Override
  public EngineNumber getVariable(String name) {
    if ("yearsElapsed".equals(name)) {
      return new EngineNumber(BigDecimal.valueOf(this.currentYear - this.startYear), "years");
    } else if ("yearAbsolute".equals(name)) {
      return new EngineNumber(BigDecimal.valueOf(this.currentYear), "year");
    } else {
      return this.scope.getVariable(name);
    }
  }

  @Override
  public void setVariable(String name, EngineNumber value) {
    if ("yearsElapsed".equals(name) || "yearAbsolute".equals(name)) {
      throw new RuntimeException("Cannot set yearsElapsed or yearAbsolute.");
    }
    this.scope.setVariable(name, value);
  }

  // Placeholder implementations for remaining methods - to be completed
  @Override
  public EngineNumber getInitialCharge(String stream) {
    if ("sales".equals(stream)) {
      // For now, implement a simplified version - the full implementation is complex
      // and involves pooling initial charges from manufacture and import
      String application = this.scope.getApplication();
      String substance = this.scope.getSubstance();
      return getRawInitialChargeFor(application, substance, "manufacture");
    } else {
      String application = this.scope.getApplication();
      String substance = this.scope.getSubstance();
      return getRawInitialChargeFor(application, substance, stream);
    }
  }

  @Override
  public EngineNumber getRawInitialChargeFor(String application, String substance, String stream) {
    return this.streamKeeper.getInitialCharge(application, substance, stream);
  }

  @Override
  public void setInitialCharge(EngineNumber value, String stream, YearMatcher yearMatcher) {
    // TODO: Implement setInitialCharge logic
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public EngineNumber getRechargeVolume() {
    String application = this.scope.getApplication();
    String substance = this.scope.getSubstance();
    return this.streamKeeper.getRechargePopulation(application, substance);
  }

  @Override
  public EngineNumber getRechargeIntensity() {
    String application = this.scope.getApplication();
    String substance = this.scope.getSubstance();
    return getRechargeIntensityFor(application, substance);
  }

  @Override
  public EngineNumber getRechargeIntensityFor(String application, String substance) {
    return this.streamKeeper.getRechargeIntensity(application, substance);
  }

  @Override
  public String getLastSpecifiedUnits(String stream) {
    String application = this.scope.getApplication();
    String substance = this.scope.getSubstance();
    return getLastSpecifiedInUnits(application, substance, stream);
  }

  @Override
  public String getLastSpecifiedInUnits(String application, String substance, String stream) {
    return this.streamKeeper.getLastSpecifiedUnits(application, substance);
  }

  @Override
  public void setLastSpecifiedUnits(String stream, String units) {
    String application = this.scope.getApplication();
    String substance = this.scope.getSubstance();

    if (application == null || substance == null) {
      throw new RuntimeException(
          "Tried setting last specified units without application and substance specified.");
    }

    this.streamKeeper.setLastSpecifiedUnits(application, substance, units);
  }

  // Additional placeholder methods for remaining interface methods
  @Override
  public void recharge(EngineNumber volume, EngineNumber intensity, YearMatcher yearMatcher) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public void retire(EngineNumber amount, YearMatcher yearMatcher) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public EngineNumber getRetirementRate() {
    String application = this.scope.getApplication();
    String substance = this.scope.getSubstance();
    return this.streamKeeper.getRetirementRate(application, substance);
  }

  @Override
  public void recycle(EngineNumber recoveryWithUnits, EngineNumber yieldWithUnits,
      EngineNumber displaceLevel, YearMatcher yearMatcher) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public void equals(EngineNumber amount, YearMatcher yearMatcher) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public EngineNumber getEqualsGhgIntensity() {
    String application = this.scope.getApplication();
    String substance = this.scope.getSubstance();
    return getEqualsGhgIntensityFor(application, substance);
  }

  @Override
  public EngineNumber getEqualsGhgIntensityFor(String application, String substance) {
    return this.streamKeeper.getGhgIntensity(application, substance);
  }

  @Override
  public EngineNumber getEqualsEnergyIntensity() {
    String application = this.scope.getApplication();
    String substance = this.scope.getSubstance();
    return getEqualsEnergyIntensityFor(application, substance);
  }

  @Override
  public EngineNumber getEqualsEnergyIntensityFor(String application, String substance) {
    return this.streamKeeper.getEnergyIntensity(application, substance);
  }

  @Override
  public void changeStream(String stream, EngineNumber amount, YearMatcher yearMatcher,
      Scope scope) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    EngineNumber currentValue = getStream(stream, scope, null);
    UnitConverter unitConverter = createUnitConverterWithTotal(stream);

    EngineNumber convertedDelta = unitConverter.convert(amount, currentValue.getUnits());
    BigDecimal newAmount = currentValue.getValue().add(convertedDelta.getValue());
    EngineNumber outputWithUnits = new EngineNumber(newAmount, currentValue.getUnits());

    // Pass the original user-specified units for tracking
    setStream(stream, outputWithUnits, null, scope, true, amount.getUnits());
  }

  @Override
  public void changeStream(String stream, EngineNumber amount, YearMatcher yearMatcher) {
    changeStream(stream, amount, yearMatcher, null);
  }

  @Override
  public void cap(String stream, EngineNumber amount, YearMatcher yearMatcher,
      String displaceTarget) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public void floor(String stream, EngineNumber amount, YearMatcher yearMatcher,
      String displaceTarget) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public void replace(EngineNumber amountRaw, String stream, String destinationSubstance,
      YearMatcher yearMatcher) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public Object[] getResults() {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * Helper method to determine if a year matcher applies to current year.
   *
   * @param yearMatcher The year matcher to check
   * @return True if in range or no matcher provided
   */
  private boolean getIsInRange(YearMatcher yearMatcher) {
    return yearMatcher == null || yearMatcher.getInRange(this.currentYear);
  }

  /**
   * Creates a unit converter with total values initialized.
   *
   * @param stream The stream identifier to create converter for
   * @return A configured unit converter instance
   */
  private UnitConverter createUnitConverterWithTotal(String stream) {
    OverridingConverterStateGetter stateGetter =
        new OverridingConverterStateGetter(this.stateGetter);
    UnitConverter unitConverter = new UnitConverter(stateGetter);

    EngineNumber currentValue = getStream(stream);
    stateGetter.setTotal(stream, currentValue);

    boolean isSalesSubstream = "manufacture".equals(stream) || "import".equals(stream);
    if (isSalesSubstream) {
      EngineNumber initialCharge = getInitialCharge(stream);
      stateGetter.setAmortizedUnitVolume(initialCharge);
    }

    return unitConverter;
  }

  /**
   * Placeholder for population change recalculation.
   *
   * @param scope The scope to recalculate for
   * @param subtractRecharge Whether to subtract recharge
   */
  private void recalcPopulationChange(Scope scope, Boolean subtractRecharge) {
    // TODO: Implement recalc logic
  }

  /**
   * Placeholder for consumption recalculation.
   *
   * @param scope The scope to recalculate for
   */
  private void recalcConsumption(Scope scope) {
    // TODO: Implement recalc logic
  }

  /**
   * Placeholder for sales recalculation.
   *
   * @param scope The scope to recalculate for
   */
  private void recalcSales(Scope scope) {
    // TODO: Implement recalc logic
  }

  /**
   * Placeholder for retirement recalculation.
   *
   * @param scope The scope to recalculate for
   */
  private void recalcRetire(Scope scope) {
    // TODO: Implement recalc logic
  }
}
