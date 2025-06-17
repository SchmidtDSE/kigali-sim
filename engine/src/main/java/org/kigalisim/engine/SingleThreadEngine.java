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
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.serializer.EngineResult;
import org.kigalisim.engine.serializer.EngineResultSerializer;
import org.kigalisim.engine.state.ConverterStateGetter;
import org.kigalisim.engine.state.OverridingConverterStateGetter;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.engine.state.StreamKeeper;
import org.kigalisim.engine.state.SubstanceInApplicationId;
import org.kigalisim.engine.state.YearMatcher;
import org.kigalisim.engine.support.ConsumptionCalculator;

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
  private static final String NO_APP_OR_SUBSTANCE_MESSAGE =
      "Tried %s without application and substance%s.";

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

  /**
   * Get the state getter used by this engine instance.
   *
   * @return The ConverterStateGetter instance
   */
  public ConverterStateGetter getStateGetter() {
    return stateGetter;
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
      raiseNoAppOrSubstance("setting stream", " specified");
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
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    String application = this.scope.getApplication();
    String substance = this.scope.getSubstance();

    if ("sales".equals(stream)) {
      // For sales, set both manufacture and import but don't recalculate yet
      this.streamKeeper.setInitialCharge(application, substance, "manufacture", value);
      this.streamKeeper.setInitialCharge(application, substance, "import", value);
    } else {
      this.streamKeeper.setInitialCharge(application, substance, stream, value);
    }

    boolean subtractRecharge = getShouldSubtractRecharge(stream);
    recalcPopulationChange(null, subtractRecharge);
  }

  /**
   * Determine if recharge should be subtracted based on last specified units.
   *
   * @param stream The stream being set
   * @return true if recharge should be subtracted, false if added on top
   */
  private boolean getShouldSubtractRecharge(String stream) {
    String application = this.scope.getApplication();
    String substance = this.scope.getSubstance();

    if ("sales".equals(stream)) {
      // For sales, check if either manufacture or import were last specified in units
      String lastUnits = this.streamKeeper.getLastSpecifiedUnits(application, substance);
      if (lastUnits != null && lastUnits.startsWith("unit")) {
        return false; // Add recharge on top
      }
    } else if ("manufacture".equals(stream) || "import".equals(stream)) {
      // For manufacture or import, check if that specific channel was last specified in units
      String lastUnits = this.streamKeeper.getLastSpecifiedUnits(application, substance);
      if (lastUnits != null && lastUnits.startsWith("unit")) {
        return false; // Add recharge on top
      }
    }

    return true;
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
      raiseNoAppOrSubstance("setting last specified units", " specified");
    }

    this.streamKeeper.setLastSpecifiedUnits(application, substance, units);
  }

  // Additional placeholder methods for remaining interface methods
  @Override
  public void recharge(EngineNumber volume, EngineNumber intensity, YearMatcher yearMatcher) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    // Setup
    String application = this.scope.getApplication();
    String substance = this.scope.getSubstance();

    // Check allowed
    if (application == null || substance == null) {
      raiseNoAppOrSubstance("recalculating population change", " specified");
    }

    // Set values
    this.streamKeeper.setRechargePopulation(application, substance, volume);
    this.streamKeeper.setRechargeIntensity(application, substance, intensity);

    // Recalculate
    recalcPopulationChange(null, null);
    recalcSales(null);
    recalcConsumption(null);
  }

  @Override
  public void retire(EngineNumber amount, YearMatcher yearMatcher) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    String application = this.scope.getApplication();
    String substance = this.scope.getSubstance();
    this.streamKeeper.setRetirementRate(application, substance, amount);
    recalcRetire(null);
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
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    String application = this.scope.getApplication();
    String substance = this.scope.getSubstance();
    this.streamKeeper.setRecoveryRate(application, substance, recoveryWithUnits);
    this.streamKeeper.setYieldRate(application, substance, yieldWithUnits);

    if (displaceLevel != null) {
      this.streamKeeper.setDisplacementRate(application, substance, displaceLevel);
    }

    recalcSales(null);
    recalcPopulationChange(null, null);
    recalcConsumption(null);
  }

  @Override
  public void equals(EngineNumber amount, YearMatcher yearMatcher) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    String application = this.scope.getApplication();
    String substance = this.scope.getSubstance();

    String units = amount.getUnits();
    boolean isGhg = units.startsWith("tCO2e");
    boolean isKwh = units.startsWith("kwh");

    if (isGhg) {
      this.streamKeeper.setGhgIntensity(application, substance, amount);
      recalcRechargeEmissions(this.scope);
      recalcEolEmissions(this.scope);
    } else if (isKwh) {
      this.streamKeeper.setEnergyIntensity(application, substance, amount);
    } else {
      throw new RuntimeException("Cannot equals " + amount.getUnits());
    }

    recalcConsumption(null);
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
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    UnitConverter unitConverter = createUnitConverterWithTotal(stream);

    EngineNumber currentValueRaw = getStream(stream);
    EngineNumber currentValue = unitConverter.convert(currentValueRaw, "kg");

    // Calculate the converted maximum value, adding recharge volume if equipment units are used
    EngineNumber convertedMax;
    if (amount.hasEquipmentUnits()) {
      // For equipment units, convert to kg and add recharge volume on top
      EngineNumber amountInKg = unitConverter.convert(amount, "kg");
      EngineNumber rechargeVolume = calculateRechargeVolume();
      BigDecimal totalWithRecharge = amountInKg.getValue().add(rechargeVolume.getValue());
      convertedMax = new EngineNumber(totalWithRecharge, "kg");
    } else {
      // For volume units, use as-is
      convertedMax = unitConverter.convert(amount, "kg");
    }

    BigDecimal changeAmountRaw = convertedMax.getValue().subtract(currentValue.getValue());
    BigDecimal changeAmount = changeAmountRaw.min(BigDecimal.ZERO);

    // Record units regardless of whether change is made
    Scope scope = this.scope;
    String application = scope.getApplication();
    String substance = scope.getSubstance();
    if (application == null || substance == null) {
      raiseNoAppOrSubstance("setting stream", " specified");
    }
    this.streamKeeper.setLastSpecifiedUnits(application, substance, amount.getUnits());

    EngineNumber changeWithUnits = new EngineNumber(changeAmount, "kg");
    // Use internal changeStream that doesn't override the units tracking
    changeStreamWithoutReportingUnits(stream, changeWithUnits, null, null);

    handleDisplacement(stream, amount, changeAmount, displaceTarget);
  }

  @Override
  public void floor(String stream, EngineNumber amount, YearMatcher yearMatcher,
      String displaceTarget) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    UnitConverter unitConverter = createUnitConverterWithTotal(stream);

    EngineNumber currentValueRaw = getStream(stream);
    EngineNumber currentValue = unitConverter.convert(currentValueRaw, "kg");

    // Calculate the converted minimum value, adding recharge volume if equipment units are used
    EngineNumber convertedMin;
    if (amount.hasEquipmentUnits()) {
      // For equipment units, convert to kg and add recharge volume on top
      EngineNumber amountInKg = unitConverter.convert(amount, "kg");
      EngineNumber rechargeVolume = calculateRechargeVolume();
      BigDecimal totalWithRecharge = amountInKg.getValue().add(rechargeVolume.getValue());
      convertedMin = new EngineNumber(totalWithRecharge, "kg");
    } else {
      // For volume units, use as-is
      convertedMin = unitConverter.convert(amount, "kg");
    }

    BigDecimal changeAmountRaw = convertedMin.getValue().subtract(currentValue.getValue());
    BigDecimal changeAmount = changeAmountRaw.max(BigDecimal.ZERO);

    // Record units regardless of whether change is made
    Scope scope = this.scope;
    String application = scope.getApplication();
    String substance = scope.getSubstance();
    if (application == null || substance == null) {
      raiseNoAppOrSubstance("setting stream", " specified");
    }
    this.streamKeeper.setLastSpecifiedUnits(application, substance, amount.getUnits());

    EngineNumber changeWithUnits = new EngineNumber(changeAmount, "kg");
    changeStreamWithoutReportingUnits(stream, changeWithUnits, null, null);

    handleDisplacement(stream, amount, changeAmount, displaceTarget);
  }

  @Override
  public void replace(EngineNumber amountRaw, String stream, String destinationSubstance,
      YearMatcher yearMatcher) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    // Track the original user-specified units for the current substance
    Scope currentScope = this.scope;
    String application = currentScope.getApplication();
    String currentSubstance = currentScope.getSubstance();
    if (application == null || currentSubstance == null) {
      raiseNoAppOrSubstance("setting stream", " specified");
    }
    this.streamKeeper.setLastSpecifiedUnits(application, currentSubstance, amountRaw.getUnits());

    // Track the original user-specified units for the destination substance
    String lastUnits = amountRaw.getUnits();
    this.streamKeeper.setLastSpecifiedUnits(application, destinationSubstance, lastUnits);

    if (amountRaw.hasEquipmentUnits()) {
      // For equipment units, convert to units first, then handle each substance separately
      UnitConverter sourceUnitConverter = createUnitConverterWithTotal(stream);
      EngineNumber unitsToReplace = sourceUnitConverter.convert(amountRaw, "units");

      // Remove from source substance using source's initial charge
      EngineNumber sourceVolumeChange = sourceUnitConverter.convert(unitsToReplace, "kg");
      EngineNumber sourceAmountNegative = new EngineNumber(
          sourceVolumeChange.getValue().negate(),
          sourceVolumeChange.getUnits()
      );
      changeStreamWithoutReportingUnits(stream, sourceAmountNegative, null, null);

      // Add to destination substance using destination's initial charge
      Scope destinationScope = this.scope.getWithSubstance(destinationSubstance);
      Scope originalScope = this.scope;
      this.scope = destinationScope;
      UnitConverter destinationUnitConverter = createUnitConverterWithTotal(stream);
      this.scope = originalScope;

      EngineNumber destinationVolumeChange = destinationUnitConverter.convert(unitsToReplace, "kg");
      changeStreamWithoutReportingUnits(stream, destinationVolumeChange, null, destinationScope);
    } else {
      // For volume units, use the original logic
      UnitConverter unitConverter = createUnitConverterWithTotal(stream);
      EngineNumber amount = unitConverter.convert(amountRaw, "kg");

      EngineNumber amountNegative = new EngineNumber(amount.getValue().negate(), amount.getUnits());
      changeStreamWithoutReportingUnits(stream, amountNegative, null, null);

      Scope destinationScope = this.scope.getWithSubstance(destinationSubstance);
      changeStreamWithoutReportingUnits(stream, amount, null, destinationScope);
    }
  }

  @Override
  public List<EngineResult> getResults() {
    List<SubstanceInApplicationId> substances = this.streamKeeper.getRegisteredSubstances();
    EngineResultSerializer serializer = new EngineResultSerializer(this, this.stateGetter);

    return substances.stream()
        .map(substanceId -> {
          String application = substanceId.getApplication();
          String substance = substanceId.getSubstance();
          int year = this.currentYear;
          return serializer.getResult(application, substance, year);
        })
        .collect(Collectors.toList());
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
   * Calculate the recharge volume for the current application and substance.
   *
   * @return The recharge volume in kg
   */
  private EngineNumber calculateRechargeVolume() {
    OverridingConverterStateGetter stateGetter =
        new OverridingConverterStateGetter(this.stateGetter);
    UnitConverter unitConverter = new UnitConverter(stateGetter);
    String application = this.scope.getApplication();
    String substance = this.scope.getSubstance();

    if (application == null || substance == null) {
      raiseNoAppOrSubstance("calculating recharge volume", "");
    }

    // Get prior population for recharge calculation
    EngineNumber priorPopulationRaw = getStream("priorEquipment");
    EngineNumber priorPopulation = unitConverter.convert(priorPopulationRaw, "units");

    // Get recharge population
    stateGetter.setPopulation(getStream("priorEquipment"));
    EngineNumber rechargePopRaw = this.streamKeeper.getRechargePopulation(application, substance);
    EngineNumber rechargePop = unitConverter.convert(rechargePopRaw, "units");
    stateGetter.clearPopulation();

    // Switch to recharge population
    stateGetter.setPopulation(rechargePop);

    // Get recharge amount
    EngineNumber rechargeIntensityRaw = this.streamKeeper.getRechargeIntensity(
        application,
        substance
    );
    EngineNumber rechargeVolume = unitConverter.convert(rechargeIntensityRaw, "kg");

    // Return to prior population
    stateGetter.setPopulation(priorPopulation);

    return rechargeVolume;
  }

  /**
   * Handle displacement logic for cap and floor operations.
   *
   * @param stream The stream identifier being modified
   * @param amount The amount used for the operation
   * @param changeAmount The actual change amount in kg
   * @param displaceTarget Optional target for displaced amount
   */
  private void handleDisplacement(String stream, EngineNumber amount, 
      BigDecimal changeAmount, String displaceTarget) {
    if (displaceTarget == null) {
      return;
    }

    EngineNumber displaceChange;

    if (amount.hasEquipmentUnits()) {
      // For equipment units, displacement should be unit-based, not volume-based
      Scope currentScope = this.scope;
      UnitConverter currentUnitConverter = createUnitConverterWithTotal(stream);

      // Convert the volume change back to units in the original substance
      EngineNumber volumeChangePositive = new EngineNumber(changeAmount.abs(), "kg");
      EngineNumber unitsChanged = currentUnitConverter.convert(volumeChangePositive, "units");

      boolean isStream = STREAM_NAMES.contains(displaceTarget);
      if (isStream) {
        // Same substance, same stream - use volume displacement
        displaceChange = new EngineNumber(changeAmount.negate(), "kg");
        changeStreamWithoutReportingUnits(displaceTarget, displaceChange, null, null);
      } else {
        // Different substance - apply the same number of units to the destination substance
        Scope destinationScope = this.scope.getWithSubstance(displaceTarget);

        // Temporarily change scope to destination for unit conversion
        Scope originalScope = this.scope;
        this.scope = destinationScope;
        UnitConverter destinationUnitConverter = createUnitConverterWithTotal(stream);
        this.scope = originalScope;

        // Convert units to destination substance volume using destination's initial charge
        EngineNumber destinationVolumeChange = destinationUnitConverter.convert(unitsChanged, "kg");
        displaceChange = new EngineNumber(destinationVolumeChange.getValue(), "kg");
        changeStreamWithoutReportingUnits(stream, displaceChange, null, destinationScope);
      }
    } else {
      // For volume units, use volume-based displacement as before
      displaceChange = new EngineNumber(changeAmount.negate(), "kg");
      boolean isStream = STREAM_NAMES.contains(displaceTarget);

      if (isStream) {
        changeStreamWithoutReportingUnits(displaceTarget, displaceChange, null, null);
      } else {
        Scope destinationScope = this.scope.getWithSubstance(displaceTarget);
        changeStreamWithoutReportingUnits(stream, displaceChange, null, destinationScope);
      }
    }
  }

  /**
   * Change a stream value without reporting units to the last units tracking system.
   *
   * @param stream The stream identifier to modify
   * @param amount The amount to change the stream by
   * @param yearMatcher Matcher to determine if the change applies to current year
   * @param scope The scope in which to make the change
   */
  private void changeStreamWithoutReportingUnits(String stream, EngineNumber amount,
      YearMatcher yearMatcher, Scope scope) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    EngineNumber currentValue = getStream(stream, scope, null);
    UnitConverter unitConverter = createUnitConverterWithTotal(stream);

    EngineNumber convertedDelta = unitConverter.convert(amount, currentValue.getUnits());
    BigDecimal newAmount = currentValue.getValue().add(convertedDelta.getValue());
    EngineNumber outputWithUnits = new EngineNumber(newAmount, currentValue.getUnits());

    // Allow propagation but don't track units (since units tracking was handled by the caller)
    setStream(stream, outputWithUnits, null, scope, true, null);
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
   * Recalculates population changes based on current state.
   *
   * @param scope The scope to recalculate for
   * @param subtractRecharge Whether to subtract recharge
   */
  private void recalcPopulationChange(Scope scope, Boolean subtractRecharge) {
    OverridingConverterStateGetter stateGetter =
        new OverridingConverterStateGetter(this.stateGetter);
    UnitConverter unitConverter = new UnitConverter(stateGetter);
    Scope scopeEffective = scope != null ? scope : this.scope;
    boolean subtractRechargeEffective = subtractRecharge != null ? subtractRecharge : true;
    String application = scopeEffective.getApplication();
    String substance = scopeEffective.getSubstance();

    if (application == null || substance == null) {
      raiseNoAppOrSubstance("recalculating population change", "");
    }

    // Get prior population
    EngineNumber priorPopulationRaw = getStream("priorEquipment", scopeEffective, null);
    EngineNumber priorPopulation = unitConverter.convert(priorPopulationRaw, "units");
    stateGetter.setPopulation(priorPopulation);

    // Get substance sales
    EngineNumber substanceSalesRaw = getStream("sales", scopeEffective, null);
    EngineNumber substanceSales = unitConverter.convert(substanceSalesRaw, "kg");

    // Get recharge volume
    EngineNumber rechargeVolume = calculateRechargeVolume();

    // Get total volume available for new units
    BigDecimal salesKg = substanceSales.getValue();
    BigDecimal rechargeKg = subtractRechargeEffective ? rechargeVolume.getValue() : BigDecimal.ZERO;
    BigDecimal availableForNewUnitsKg = salesKg.subtract(rechargeKg);

    // Convert to unit delta
    EngineNumber initialChargeRaw = getInitialCharge("sales");
    EngineNumber initialCharge = unitConverter.convert(initialChargeRaw, "kg / unit");
    BigDecimal initialChargeKgUnit = initialCharge.getValue();
    BigDecimal deltaUnitsRaw = divideWithZero(availableForNewUnitsKg, initialChargeKgUnit);
    BigDecimal deltaUnits = deltaUnitsRaw;
    EngineNumber newUnitsMarginal = new EngineNumber(
        deltaUnits.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : deltaUnits, "units");

    // Find new total
    BigDecimal priorPopulationUnits = priorPopulation.getValue();
    BigDecimal newUnits = priorPopulationUnits.add(deltaUnits);
    boolean newUnitsNegative = newUnits.compareTo(BigDecimal.ZERO) < 0;
    BigDecimal newUnitsAllowed = newUnitsNegative ? BigDecimal.ZERO : newUnits;
    EngineNumber newUnitsEffective = new EngineNumber(newUnitsAllowed, "units");

    // Save
    setStream("equipment", newUnitsEffective, null, scopeEffective, false, null);
    setStream("newEquipment", newUnitsMarginal, null, scopeEffective, false, null);

    recalcRechargeEmissions(scopeEffective);
  }

  /**
   * Recalculate the emissions that are accounted for at time of recharge.
   *
   * @param scope The scope in which to set the recharge emissions
   */
  private void recalcRechargeEmissions(Scope scope) {
    Scope scopeEffective = scope != null ? scope : this.scope;
    EngineNumber rechargeVolume = calculateRechargeVolume();
    EngineNumber rechargeGhg = this.unitConverter.convert(rechargeVolume, "tCO2e");
    setStream("rechargeEmissions", rechargeGhg, null, scopeEffective, false, null);
  }

  /**
   * Recalculate emissions realized at the end of life for a unit.
   *
   * @param scope The scope in which to recalculate EOL emissions
   */
  private void recalcEolEmissions(Scope scope) {
    // Setup
    OverridingConverterStateGetter stateGetter =
        new OverridingConverterStateGetter(this.stateGetter);
    UnitConverter unitConverter = new UnitConverter(stateGetter);
    Scope scopeEffective = scope != null ? scope : this.scope;
    String application = scopeEffective.getApplication();
    String substance = scopeEffective.getSubstance();

    // Check allowed
    if (application == null || substance == null) {
      raiseNoAppOrSubstance("recalculating EOL emissions change", "");
    }

    // Calculate change
    EngineNumber currentPriorRaw = this.streamKeeper.getStream(
        application,
        substance,
        "priorEquipment"
    );
    EngineNumber currentPrior = unitConverter.convert(currentPriorRaw, "units");

    stateGetter.setPopulation(currentPrior);
    EngineNumber amountRaw = this.streamKeeper.getRetirementRate(application, substance);
    EngineNumber amount = unitConverter.convert(amountRaw, "units");
    stateGetter.clearPopulation();

    // Update GHG accounting
    EngineNumber eolGhg = unitConverter.convert(amount, "tCO2e");
    this.streamKeeper.setStream(application, substance, "eolEmissions", eolGhg);
  }

  /**
   * Recalculates consumption values based on current state.
   *
   * @param scope The scope to recalculate for
   */
  private void recalcConsumption(Scope scope) {
    Scope scopeEffective = scope != null ? scope : this.scope;

    String application = scopeEffective.getApplication();
    String substance = scopeEffective.getSubstance();

    if (application == null || substance == null) {
      raiseNoAppOrSubstance("recalculating consumption", "");
    }

    // Update streams using ConsumptionCalculator
    ConsumptionCalculator calculator = new ConsumptionCalculator();

    // Get GHG intensity and calculate consumption
    EngineNumber ghgIntensity = this.streamKeeper.getGhgIntensity(application, substance);
    calculator.setConsumptionRaw(ghgIntensity);
    calculator.setStreamName("consumption");
    calculator.execute(this);

    // Get energy intensity and calculate energy
    calculator = new ConsumptionCalculator();
    EngineNumber energyIntensity = this.streamKeeper.getEnergyIntensity(application, substance);
    calculator.setConsumptionRaw(energyIntensity);
    calculator.setStreamName("energy");
    calculator.execute(this);
  }

  /**
   * Recalculates sales values based on current state.
   *
   * @param scope The scope to recalculate for
   */
  private void recalcSales(Scope scope) {
    Scope scopeEffective = scope != null ? scope : this.scope;

    OverridingConverterStateGetter stateGetter =
        new OverridingConverterStateGetter(this.stateGetter);
    UnitConverter unitConverter = new UnitConverter(stateGetter);
    String application = scopeEffective.getApplication();
    String substance = scopeEffective.getSubstance();

    if (application == null || substance == null) {
      raiseNoAppOrSubstance("recalculating sales", "");
    }

    // Get recharge population
    EngineNumber basePopulation = getStream("priorEquipment", scopeEffective, null);
    stateGetter.setPopulation(basePopulation);
    EngineNumber rechargePopRaw = this.streamKeeper.getRechargePopulation(application, substance);
    EngineNumber rechargePop = unitConverter.convert(rechargePopRaw, "units");
    stateGetter.clearPopulation();

    // Switch into recharge population
    stateGetter.setPopulation(rechargePop);

    // Get recharge amount
    EngineNumber rechargeIntensityRaw = this.streamKeeper.getRechargeIntensity(
        application,
        substance
    );
    EngineNumber rechargeVolume = unitConverter.convert(rechargeIntensityRaw, "kg");

    // Determine initial charge
    EngineNumber initialChargeRaw = getInitialCharge("sales");
    EngineNumber initialCharge = unitConverter.convert(initialChargeRaw, "kg / unit");

    // Get recycling volume
    stateGetter.setVolume(rechargeVolume);
    EngineNumber recoveryVolumeRaw = this.streamKeeper.getRecoveryRate(application, substance);
    EngineNumber recoveryVolume = unitConverter.convert(recoveryVolumeRaw, "kg");
    stateGetter.clearVolume();

    // Get recycling amount
    stateGetter.setVolume(recoveryVolume);
    EngineNumber recycledVolumeRaw = this.streamKeeper.getYieldRate(application, substance);
    EngineNumber recycledVolume = unitConverter.convert(recycledVolumeRaw, "kg");
    stateGetter.clearVolume();

    // Get recycling displaced
    BigDecimal recycledKg = recycledVolume.getValue();

    EngineNumber displacementRateRaw = this.streamKeeper.getDisplacementRate(
        application,
        substance
    );
    EngineNumber displacementRate = unitConverter.convert(displacementRateRaw, "%");
    BigDecimal displacementRateRatio = displacementRate.getValue().divide(BigDecimal.valueOf(100));
    final BigDecimal recycledDisplacedKg = recycledKg.multiply(displacementRateRatio);

    // Switch out of recharge population
    stateGetter.clearPopulation();

    // Determine needs for new equipment deployment
    stateGetter.setAmortizedUnitVolume(initialCharge);
    EngineNumber populationChangeRaw = stateGetter.getPopulationChange(this.unitConverter);
    EngineNumber populationChange = unitConverter.convert(populationChangeRaw, "units");
    EngineNumber volumeForNew = unitConverter.convert(populationChange, "kg");

    // Get prior population
    EngineNumber priorPopulationRaw = getStream("priorEquipment", scopeEffective, null);
    EngineNumber priorPopulation = unitConverter.convert(priorPopulationRaw, "units");
    stateGetter.setPopulation(priorPopulation);

    // Determine sales prior to recycling
    final BigDecimal kgForRecharge = rechargeVolume.getValue();
    final BigDecimal kgForNew = volumeForNew.getValue();

    // Return to original initial charge
    stateGetter.clearAmortizedUnitVolume();

    // Return original
    stateGetter.clearVolume();

    // Determine how much to offset domestic and imports
    EngineNumber manufactureRaw = getStream("manufacture", scopeEffective, null);
    EngineNumber importRaw = getStream("import", scopeEffective, null);
    EngineNumber priorRecycleRaw = getStream("recycle", scopeEffective, null);

    EngineNumber manufactureSalesConverted = unitConverter.convert(manufactureRaw, "kg");
    EngineNumber importSalesConverted = unitConverter.convert(importRaw, "kg");
    EngineNumber priorRecycleSalesConverted = unitConverter.convert(priorRecycleRaw, "kg");

    BigDecimal manufactureSalesKg = manufactureSalesConverted.getValue();
    BigDecimal importSalesKg = importSalesConverted.getValue();
    BigDecimal priorRecycleSalesKg = priorRecycleSalesConverted.getValue();
    BigDecimal totalNonRecycleKg = manufactureSalesKg.add(importSalesKg);

    // Get stream percentages for allocation
    BigDecimal percentManufacture;
    BigDecimal percentImport;

    if (totalNonRecycleKg.compareTo(BigDecimal.ZERO) == 0) {
      EngineNumber manufactureInitialCharge = getInitialCharge("manufacture");
      EngineNumber importInitialCharge = getInitialCharge("import");
      BigDecimal manufactureInitialChargeVal = manufactureInitialCharge.getValue();
      BigDecimal importInitialChargeVal = unitConverter
          .convert(importInitialCharge, manufactureInitialCharge.getUnits()).getValue();
      BigDecimal totalInitialChargeVal = manufactureInitialChargeVal.add(importInitialChargeVal);

      if (totalInitialChargeVal.compareTo(BigDecimal.ZERO) == 0) {
        percentManufacture = BigDecimal.ONE;
        percentImport = BigDecimal.ZERO;
      } else {
        percentManufacture = divideWithZero(manufactureInitialChargeVal, totalInitialChargeVal);
        percentImport = divideWithZero(importInitialChargeVal, totalInitialChargeVal);
      }
    } else {
      percentManufacture = divideWithZero(manufactureSalesKg, totalNonRecycleKg);
      percentImport = divideWithZero(importSalesKg, totalNonRecycleKg);
    }

    // Recycle
    EngineNumber newRecycleValue = new EngineNumber(recycledDisplacedKg, "kg");
    this.streamKeeper.setStream(application, substance, "recycle", newRecycleValue);

    // New values
    BigDecimal requiredKgUnbound = kgForRecharge.add(kgForNew);
    boolean requiredKgNegative = requiredKgUnbound.compareTo(BigDecimal.ZERO) < 0;
    BigDecimal requiredKg = requiredKgNegative ? BigDecimal.ZERO : requiredKgUnbound;
    BigDecimal newManufactureKg = percentManufacture.multiply(requiredKg);
    BigDecimal newImportKg = percentImport.multiply(requiredKg);
    EngineNumber newManufacture = new EngineNumber(newManufactureKg, "kg");
    EngineNumber newImport = new EngineNumber(newImportKg, "kg");
    this.streamKeeper.setStream(application, substance, "manufacture", newManufacture);
    this.streamKeeper.setStream(application, substance, "import", newImport);
  }

  /**
   * Divide with a check for division by zero.
   *
   * @param numerator The numerator to use in the operation.
   * @param denominator The numerator to use in the operation.
   * @return Zero if denominator is zero, otherwise the result of regular division.
   */
  private BigDecimal divideWithZero(BigDecimal numerator, BigDecimal denominator) {
    boolean denominatorIsZero = denominator.compareTo(BigDecimal.ZERO) == 0;
    if (denominatorIsZero) {
      return BigDecimal.ZERO;
    } else {
      return numerator.divide(denominator, 10, RoundingMode.HALF_UP);
    }
  }

  /**
   * Recalculates retirement values based on current state.
   *
   * @param scope The scope to recalculate for
   */
  private void recalcRetire(Scope scope) {
    // Setup
    OverridingConverterStateGetter stateGetter =
        new OverridingConverterStateGetter(this.stateGetter);
    UnitConverter unitConverter = new UnitConverter(stateGetter);
    Scope scopeEffective = scope != null ? scope : this.scope;
    String application = scopeEffective.getApplication();
    String substance = scopeEffective.getSubstance();

    // Check allowed
    if (application == null || substance == null) {
      raiseNoAppOrSubstance("recalculating population change", "");
    }

    // Calculate change
    EngineNumber currentPriorRaw = this.streamKeeper.getStream(
        application,
        substance,
        "priorEquipment"
    );
    EngineNumber currentPrior = unitConverter.convert(currentPriorRaw, "units");

    EngineNumber currentEquipmentRaw = this.streamKeeper.getStream(
        application,
        substance,
        "equipment"
    );
    EngineNumber currentEquipment = unitConverter.convert(currentEquipmentRaw, "units");

    stateGetter.setPopulation(currentPrior);
    EngineNumber amountRaw = this.streamKeeper.getRetirementRate(application, substance);
    EngineNumber amount = unitConverter.convert(amountRaw, "units");
    stateGetter.clearPopulation();

    // Calculate new values
    BigDecimal newPriorValue = currentPrior.getValue().subtract(amount.getValue());
    BigDecimal newEquipmentValue = currentEquipment.getValue().subtract(amount.getValue());

    EngineNumber newPrior = new EngineNumber(newPriorValue, "units");
    EngineNumber newEquipment = new EngineNumber(newEquipmentValue, "units");

    // Update equipment streams
    this.streamKeeper.setStream(application, substance, "priorEquipment", newPrior);
    this.streamKeeper.setStream(application, substance, "equipment", newEquipment);

    // Update GHG accounting
    recalcEolEmissions(scopeEffective);

    // Propagate
    recalcPopulationChange(null, null);
    recalcSales(null);
    recalcConsumption(null);
  }

  /**
   * Helper method to raise exception for missing application or substance.
   *
   * @param operation The operation being attempted
   * @param suffix Additional suffix for the error message (usually " specified")
   */
  private void raiseNoAppOrSubstance(String operation, String suffix) {
    throw new RuntimeException(String.format(NO_APP_OR_SUBSTANCE_MESSAGE, operation, suffix));
  }
}
