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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.recalc.RecalcKit;
import org.kigalisim.engine.recalc.RecalcKitBuilder;
import org.kigalisim.engine.recalc.RecalcOperation;
import org.kigalisim.engine.recalc.RecalcOperationBuilder;
import org.kigalisim.engine.recalc.SalesStreamDistribution;
import org.kigalisim.engine.recalc.StreamUpdate;
import org.kigalisim.engine.recalc.StreamUpdateBuilder;
import org.kigalisim.engine.serializer.EngineResult;
import org.kigalisim.engine.serializer.EngineResultSerializer;
import org.kigalisim.engine.state.ConverterStateGetter;
import org.kigalisim.engine.state.OverridingConverterStateGetter;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.engine.state.SimpleUseKey;
import org.kigalisim.engine.state.SimulationState;
import org.kigalisim.engine.state.SimulationStateUpdate;
import org.kigalisim.engine.state.SimulationStateUpdateBuilder;
import org.kigalisim.engine.state.SubstanceInApplicationId;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;
import org.kigalisim.engine.support.ChangeExecutor;
import org.kigalisim.engine.support.EngineSupportUtils;
import org.kigalisim.engine.support.EquipmentChangeUtil;
import org.kigalisim.engine.support.ExceptionsGenerator;
import org.kigalisim.engine.support.RechargeVolumeCalculator;
import org.kigalisim.engine.support.SetExecutor;
import org.kigalisim.lang.operation.RecoverOperation.RecoveryStage;

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
    STREAM_NAMES.add("domestic");
    STREAM_NAMES.add("sales");
  }

  private static final String RECYCLE_RECOVER_STREAM = "sales";

  private final int startYear;
  private final int endYear;
  private String scenarioName;
  private int trialNumber;

  private final ConverterStateGetter stateGetter;
  private final UnitConverter unitConverter;
  private final SimulationState simulationState;
  private final ChangeExecutor changeExecutor;
  private final EquipmentChangeUtil equipmentChangeUtil;
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
    this.scenarioName = "";
    this.trialNumber = 0;

    stateGetter = new ConverterStateGetter(this);
    unitConverter = new UnitConverter(stateGetter);
    this.simulationState = new SimulationState(
        new OverridingConverterStateGetter(stateGetter), unitConverter);
    this.simulationState.setCurrentYear(startYear);
    this.changeExecutor = new ChangeExecutor(this);
    this.equipmentChangeUtil = new EquipmentChangeUtil(this);
    scope = new Scope(null, null, null);
  }

  @Override
  public int getStartYear() {
    return startYear;
  }

  @Override
  public int getEndYear() {
    return endYear;
  }

  /**
   * Get the scenario name.
   *
   * @return The name of the scenario being run
   */
  public String getScenarioName() {
    return scenarioName;
  }

  /**
   * Set the scenario name.
   *
   * @param scenarioName The name of the scenario being run
   */
  public void setScenarioName(String scenarioName) {
    this.scenarioName = scenarioName;
  }

  /**
   * Get the trial number.
   *
   * @return The trial number of the current run
   */
  public int getTrialNumber() {
    return trialNumber;
  }

  /**
   * Set the trial number.
   *
   * @param trialNumber The trial number of the current run
   */
  public void setTrialNumber(int trialNumber) {
    this.trialNumber = trialNumber;
  }

  @Override
  public void setStanza(String newStanza) {
    scope = scope.getWithStanza(newStanza);
  }

  @Override
  public void setApplication(String newApplication) {
    scope = scope.getWithApplication(newApplication);
  }

  @Override
  public void setSubstance(String newSubstance, Boolean checkValid) {
    scope = scope.getWithSubstance(newSubstance);

    boolean checkValidEffective = checkValid != null && checkValid;

    if (checkValidEffective) {
      boolean knownSubstance = simulationState.hasSubstance(scope);
      if (!knownSubstance) {
        throw new RuntimeException("Tried accessing unknown app / substance pair: "
            + scope.getApplication() + ", " + newSubstance);
      }
    } else {
      simulationState.ensureSubstance(scope);
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
   * {@inheritDoc}
   */
  public ConverterStateGetter getStateGetter() {
    return stateGetter;
  }

  /**
   * {@inheritDoc}
   */
  public UnitConverter getUnitConverter() {
    return unitConverter;
  }

  /**
   * {@inheritDoc}
   */
  public SimulationState getStreamKeeper() {
    return simulationState;
  }

  @Override
  public void incrementYear() {
    if (getIsDone()) {
      throw new RuntimeException("Already completed.");
    }
    simulationState.incrementYear();
  }

  @Override
  public int getYear() {
    return simulationState.getCurrentYear();
  }

  @Override
  public boolean getIsDone() {
    return simulationState.getCurrentYear() > endYear;
  }

  @Override
  public void executeStreamUpdate(StreamUpdate update) {
    final String name = update.getName();
    final EngineNumber value = update.getValue();
    final Optional<YearMatcher> yearMatcher = update.getYearMatcher();
    final Optional<UseKey> key = update.getKey();
    final boolean propagateChanges = update.getPropagateChanges();
    final Optional<String> unitsToRecord = update.getUnitsToRecord();
    final boolean subtractRecycling = update.getSubtractRecycling();

    if (!getIsInRange(yearMatcher.orElse(null))) {
      return;
    }

    UseKey keyEffective = key.orElse(scope);
    String application = keyEffective.getApplication();
    String substance = keyEffective.getSubstance();

    if (application == null || substance == null) {
      raiseNoAppOrSubstance("setting stream", " specified");
    }

    // Check if this is a sales stream with units - if so, add recharge on top
    boolean isSales = isSalesStream(name);
    boolean isUnits = value.hasEquipmentUnits();
    boolean isSalesSubstream = getIsSalesSubstream(name);

    EngineNumber valueToSet = value;
    if (isSales && isUnits) {
      // Convert to kg and add recharge on top
      UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(this, name);
      EngineNumber valueInKg = unitConverter.convert(value, "kg");
      EngineNumber rechargeVolume = RechargeVolumeCalculator.calculateRechargeVolume(
          keyEffective,
          stateGetter,
          simulationState,
          this
      );

      // Set implicit recharge BEFORE distribution (always full amount)
      SimulationStateUpdate implicitRechargeStream = new SimulationStateUpdateBuilder()
          .setUseKey(keyEffective)
          .setName("implicitRecharge")
          .setValue(rechargeVolume)
          .setSubtractRecycling(false)
          .build();
      simulationState.update(implicitRechargeStream);

      // Distribute recharge proportionally for domestic/import streams
      BigDecimal rechargeToAdd;
      if (isSalesSubstream) {
        // Use distributed recharge for individual substreams
        rechargeToAdd = getDistributedRecharge(name, rechargeVolume, keyEffective);
      } else {
        // Sales stream gets full recharge
        rechargeToAdd = rechargeVolume.getValue();
      }

      BigDecimal totalWithRecharge = valueInKg.getValue().add(rechargeToAdd);
      valueToSet = new EngineNumber(totalWithRecharge, "kg");
    } else if (isSales) {
      // Sales stream without units - clear implicit recharge
      SimulationStateUpdate clearImplicitRechargeStream = new SimulationStateUpdateBuilder()
          .setUseKey(keyEffective)
          .setName("implicitRecharge")
          .setValue(new EngineNumber(BigDecimal.ZERO, "kg"))
          .setSubtractRecycling(false)
          .build();
      simulationState.update(clearImplicitRechargeStream);
    }

    // Use the subtractRecycling parameter when setting the stream
    SimulationStateUpdate simulationStateUpdate = new SimulationStateUpdateBuilder()
        .setUseKey(keyEffective)
        .setName(name)
        .setValue(valueToSet)
        .setSubtractRecycling(subtractRecycling)
        .setDistribution(update.getDistribution().orElse(null))
        .build();
    simulationState.update(simulationStateUpdate);

    // Track the units last used to specify this stream (only for user-initiated calls)
    if (!propagateChanges) {
      return;
    }

    if (isSales) {
      // Track the last specified value for sales-related streams
      // This preserves user intent across carry-over years
      simulationState.setLastSpecifiedValue(keyEffective, name, value);

      // Handle stream combinations for unit preservation
      updateSalesCarryOver(keyEffective, name, value);
    }

    if ("sales".equals(name) || getIsSalesSubstream(name)) {
      // Use implicit recharge only if we added recharge (units were used)
      boolean useImplicitRecharge = isSales && isUnits;
      RecalcOperationBuilder builder = new RecalcOperationBuilder()
          .setScopeEffective(keyEffective)
          .setUseExplicitRecharge(!useImplicitRecharge)
          .setRecalcKit(createRecalcKit())
          .recalcPopulationChange()
          .thenPropagateToConsumption();

      if (!OPTIMIZE_RECALCS) {
        builder = builder.thenPropagateToSales();
      }

      RecalcOperation operation = builder.build();
      operation.execute(this);
    } else if ("consumption".equals(name)) {
      RecalcOperationBuilder builder = new RecalcOperationBuilder()
          .setScopeEffective(keyEffective)
          .setRecalcKit(createRecalcKit())
          .recalcSales()
          .thenPropagateToPopulationChange();

      if (!OPTIMIZE_RECALCS) {
        builder = builder.thenPropagateToConsumption();
      }

      RecalcOperation operation = builder.build();
      operation.execute(this);
    } else if ("equipment".equals(name)) {
      RecalcOperationBuilder builder = new RecalcOperationBuilder()
          .setScopeEffective(keyEffective)
          .setRecalcKit(createRecalcKit())
          .recalcSales()
          .thenPropagateToConsumption();

      if (!OPTIMIZE_RECALCS) {
        builder = builder.thenPropagateToPopulationChange();
      }

      RecalcOperation operation = builder.build();
      operation.execute(this);
    } else if ("priorEquipment".equals(name)) {
      RecalcOperation operation = new RecalcOperationBuilder()
          .setScopeEffective(keyEffective)
          .setRecalcKit(createRecalcKit())
          .recalcRetire()
          .build();
      operation.execute(this);
    }
  }


  @Override
  public void fulfillSetCommand(String name, EngineNumber value, Optional<YearMatcher> yearMatcher) {

    // Check year range before proceeding
    if (!getIsInRange(yearMatcher.orElse(null))) {
      return;
    }

    // Handle equipment stream with special logic
    if ("equipment".equals(name)) {
      equipmentChangeUtil.handleSet(value);
      return;
    }

    // Delegate sales streams to SetExecutor for proper component distribution
    if ("sales".equals(name)) {
      SetExecutor setExecutor = new SetExecutor(this);
      setExecutor.handleSalesSet(scope, name, value, yearMatcher);
      return;
    }

    // For non-sales streams, use executeStreamUpdate with builder
    StreamUpdate update = new StreamUpdateBuilder()
        .setName(name)
        .setValue(value)
        .setYearMatcher(yearMatcher)
        .inferSubtractRecycling()
        .build();
    executeStreamUpdate(update);
  }

  @Override
  public void enable(String name, Optional<YearMatcher> yearMatcher) {
    if (!getIsInRange(yearMatcher.orElse(null))) {
      return;
    }

    UseKey keyEffective = scope;
    String application = keyEffective.getApplication();
    String substance = keyEffective.getSubstance();

    if (application == null || substance == null) {
      raiseNoAppOrSubstance("enabling stream", " specified");
    }

    // Only allow enabling of manufacture, import, and export streams
    if ("domestic".equals(name) || "import".equals(name) || "export".equals(name)) {
      simulationState.markStreamAsEnabled(keyEffective, name);
    }
  }

  @Override
  public EngineNumber getStream(String name, Optional<UseKey> useKey, Optional<String> conversion) {
    UseKey effectiveKey = useKey.orElse(scope);
    EngineNumber value = simulationState.getStream(effectiveKey, name);
    return conversion.map(conv -> unitConverter.convert(value, conv)).orElse(value);
  }

  @Override
  public EngineNumber getStream(String name) {
    return getStream(name, Optional.of(scope), Optional.empty());
  }

  @Override
  public EngineNumber getStreamFor(UseKey key, String stream) {
    return simulationState.getStream(key, stream);
  }

  @Override
  public void defineVariable(String name) {
    if ("yearsElapsed".equals(name) || "yearAbsolute".equals(name)) {
      throw new RuntimeException("Cannot override yearsElapsed or yearAbsolute.");
    }
    scope.defineVariable(name);
  }

  @Override
  public EngineNumber getVariable(String name) {
    if ("yearsElapsed".equals(name)) {
      return new EngineNumber(BigDecimal.valueOf(simulationState.getCurrentYear() - startYear), "years");
    } else if ("yearAbsolute".equals(name)) {
      return new EngineNumber(BigDecimal.valueOf(simulationState.getCurrentYear()), "year");
    } else {
      return scope.getVariable(name);
    }
  }

  @Override
  public void setVariable(String name, EngineNumber value) {
    if ("yearsElapsed".equals(name) || "yearAbsolute".equals(name)) {
      throw new RuntimeException("Cannot set yearsElapsed or yearAbsolute.");
    }
    scope.setVariable(name, value);
  }

  @Override
  public EngineNumber getInitialCharge(String stream) {
    if ("sales".equals(stream)) {
      try {
        // Use SalesStreamDistributionBuilder to get the correct weights for enabled streams
        SalesStreamDistribution distribution = simulationState.getDistribution(scope, false);

        BigDecimal domesticWeight = distribution.getPercentDomestic();
        BigDecimal importWeight = distribution.getPercentImport();

        // Get raw initial charges for each stream
        EngineNumber domesticInitialChargeRaw = getRawInitialChargeFor(scope, "domestic");
        EngineNumber domesticInitialCharge = unitConverter.convert(domesticInitialChargeRaw, "kg / unit");

        EngineNumber importInitialChargeRaw = getRawInitialChargeFor(scope, "import");
        EngineNumber importInitialCharge = unitConverter.convert(importInitialChargeRaw, "kg / unit");

        // Calculate weighted average of initial charges using distribution percentages
        BigDecimal weightedSum = domesticInitialCharge.getValue().multiply(domesticWeight)
            .add(importInitialCharge.getValue().multiply(importWeight));

        return new EngineNumber(weightedSum, "kg / unit");
      } catch (IllegalStateException e) {
        // Fallback: if no streams are enabled, return zero
        return new EngineNumber(BigDecimal.ZERO, "kg / unit");
      }
    } else {
      return getRawInitialChargeFor(scope, stream);
    }
  }

  private static boolean isEmptyStreams(EngineNumber manufactureValue, EngineNumber importValue) {
    BigDecimal manufactureRawValue = manufactureValue.getValue();
    BigDecimal importRawValue = importValue.getValue();
    BigDecimal total;

    // Check for finite values (BigDecimal doesn't have infinity, but we can check for very large values)
    if (manufactureRawValue.abs().compareTo(new BigDecimal("1E+100")) > 0) {
      total = importRawValue;
    } else if (importRawValue.abs().compareTo(new BigDecimal("1E+100")) > 0) {
      total = manufactureRawValue;
    } else {
      total = manufactureRawValue.add(importRawValue);
    }

    return total.compareTo(BigDecimal.ZERO) == 0;
  }

  @Override
  public EngineNumber getRawInitialChargeFor(UseKey useKey, String stream) {
    return simulationState.getInitialCharge(useKey, stream);
  }

  @Override
  public void setInitialCharge(EngineNumber value, String stream, YearMatcher yearMatcher) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    if ("sales".equals(stream)) {
      // For sales, set both manufacture and import but don't recalculate yet
      simulationState.setInitialCharge(scope, "domestic", value);
      simulationState.setInitialCharge(scope, "import", value);
    } else {
      simulationState.setInitialCharge(scope, stream, value);
    }

    boolean useExplicitRecharge = getShouldUseExplicitRecharge(stream);
    RecalcOperation operation = new RecalcOperationBuilder()
        .setUseExplicitRecharge(useExplicitRecharge)
        .setRecalcKit(createRecalcKit())
        .recalcPopulationChange()
        .build();
    operation.execute(this);
  }

  /**
   * Get the last sales units for a given key.
   *
   * @param useKey The key to look up
   * @return Optional containing the units string, or empty if not found
   */
  private Optional<String> getLastSalesUnits(UseKey useKey) {
    EngineNumber lastValue = simulationState.getLastSpecifiedValue(useKey, "sales");
    return lastValue != null ? Optional.of(lastValue.getUnits()) : Optional.empty();
  }

  /**
   * Determine if recharge should be subtracted based on last specified units.
   *
   * @param stream The stream being set
   * @return true if recharge should be subtracted, false if added on top
   */
  private boolean getShouldUseExplicitRecharge(String stream) {
    if ("sales".equals(stream)) {
      // For sales, check if either manufacture or import were last specified in units
      Optional<String> lastUnits = getLastSalesUnits(scope);
      if (lastUnits.isPresent() && lastUnits.get().startsWith("unit")) {
        return false; // Add recharge on top
      }
    } else if ("domestic".equals(stream) || "import".equals(stream)) {
      // For manufacture or import, check if that specific channel was last specified in units
      Optional<String> lastUnits = getLastSalesUnits(scope);
      return !lastUnits.isPresent() || !lastUnits.get().startsWith("unit"); // Add recharge on top
    }

    return true;
  }

  @Override
  public EngineNumber getRechargeVolume() {
    return simulationState.getRechargePopulation(scope);
  }

  @Override
  public EngineNumber getRechargeIntensity() {
    return simulationState.getRechargeIntensity(scope);
  }

  // Additional placeholder methods for remaining interface methods
  @Override
  public void recharge(EngineNumber volume, EngineNumber intensity, YearMatcher yearMatcher) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    // Setup
    String application = scope.getApplication();
    String substance = scope.getSubstance();

    // Check allowed
    if (application == null || substance == null) {
      raiseNoAppOrSubstance("recalculating population change", " specified");
    }

    simulationState.accumulateRecharge(scope, volume, intensity);

    boolean isCarryOver = isCarryOver(scope);

    if (isCarryOver) {
      // Preserve user's original unit-based intent
      // Use executeStreamUpdate with the original value - this will automatically add recharge on top
      EngineNumber lastSalesValue = simulationState.getLastSpecifiedValue(scope, "sales");
      StreamUpdate update = new StreamUpdateBuilder()
          .setName("sales")
          .setValue(lastSalesValue)
          .setKey(scope)
          .build();
      executeStreamUpdate(update);
      return; // Skip normal recalc to avoid accumulation
    } else {
      // Fall back to kg-based or untracked values
      Optional<String> lastUnits = getLastSalesUnits(scope);
      boolean useExplicitRecharge = !lastUnits.isPresent() || !lastUnits.get().startsWith("unit");

      // Recalculate
      RecalcOperation operation = new RecalcOperationBuilder()
          .setUseExplicitRecharge(useExplicitRecharge)
          .setRecalcKit(createRecalcKit())
          .recalcPopulationChange()
          .thenPropagateToSales()
          .thenPropagateToConsumption()
          .build();
      operation.execute(this);

      // Only clear implicit recharge if NOT using explicit recharge (i.e., when units were used)
      // This ensures implicit recharge persists for carried-over values
      if (useExplicitRecharge) {
        SimulationStateUpdate clearImplicitRechargeStream = new SimulationStateUpdateBuilder()
            .setUseKey(scope)
            .setName("implicitRecharge")
            .setValue(new EngineNumber(BigDecimal.ZERO, "kg"))
            .setSubtractRecycling(false)
            .build();
        simulationState.update(clearImplicitRechargeStream);
      }
    }
  }

  @Override
  public void retire(EngineNumber amount, YearMatcher yearMatcher) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }
    simulationState.setRetirementRate(scope, amount);

    RecalcOperationBuilder builder = new RecalcOperationBuilder()
        .setRecalcKit(createRecalcKit())
        .recalcRetire();

    // Add sales recalc if streams were specified in units (recharge needs updating)
    if (hasUnitBasedSalesSpecifications()) {
      builder = builder.thenPropagateToSales();
    }

    RecalcOperation operation = builder.build();
    operation.execute(this);
  }

  @Override
  public EngineNumber getRetirementRate() {
    return simulationState.getRetirementRate(scope);
  }

  @Override
  public void recycle(EngineNumber recoveryWithUnits, EngineNumber yieldWithUnits,
      YearMatcher yearMatcher, RecoveryStage stage) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    simulationState.setRecoveryRate(scope, recoveryWithUnits, stage);
    simulationState.setYieldRate(scope, yieldWithUnits, stage);

    RecalcOperation operation = new RecalcOperationBuilder()
        .setRecalcKit(createRecalcKit())
        .recalcSales()
        .thenPropagateToPopulationChange()
        .thenPropagateToConsumption()
        .build();
    operation.execute(this);

    // Update lastSpecifiedValue after recycling for volume-based specs
    // updateLastSpecifiedValueAfterRecycling();
  }

  @Override
  public void setInductionRate(EngineNumber inductionRate, RecoveryStage stage) {
    if (inductionRate != null) {
      // Validate the induction rate is a percentage between 0 and 100
      if (!"%".equals(inductionRate.getUnits())) {
        throw new IllegalArgumentException("Induction rate must have percentage units, got: " + inductionRate.getUnits());
      }
      double ratePercent = inductionRate.getValue().doubleValue();
      if (ratePercent < 0.0 || ratePercent > 100.0) {
        throw new IllegalArgumentException("Induction rate must be between 0% and 100%, got: " + ratePercent + "%");
      }
      simulationState.setInductionRate(scope, inductionRate, stage);
    } else {
      // Default behavior - set to 100% (induced demand behavior)
      EngineNumber defaultInductionRate = new EngineNumber(new BigDecimal("100"), "%");
      simulationState.setInductionRate(scope, defaultInductionRate, stage);
    }
  }

  @Override
  public void equals(EngineNumber amount, YearMatcher yearMatcher) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    String units = amount.getUnits();
    boolean isGhg = units.startsWith("tCO2e") || units.startsWith("kgCO2e");
    boolean isKwh = units.startsWith("kwh");

    if (isGhg) {
      simulationState.setGhgIntensity(scope, amount);
      RecalcOperation operation = new RecalcOperationBuilder()
          .setScopeEffective(scope)
          .setRecalcKit(createRecalcKit())
          .recalcRechargeEmissions()
          .thenPropagateToEolEmissions()
          .build();
      operation.execute(this);
    } else if (isKwh) {
      simulationState.setEnergyIntensity(scope, amount);
    } else {
      throw new RuntimeException("Cannot equals " + amount.getUnits());
    }

    RecalcOperation operation = new RecalcOperationBuilder()
        .setRecalcKit(createRecalcKit())
        .recalcConsumption()
        .build();
    operation.execute(this);
  }

  @Override
  public EngineNumber getGhgIntensity(UseKey useKey) {
    return simulationState.getGhgIntensity(useKey);
  }

  @Override
  public EngineNumber getEqualsGhgIntensity() {
    return simulationState.getGhgIntensity(scope);
  }

  @Override
  public EngineNumber getEqualsGhgIntensityFor(UseKey useKey) {
    return simulationState.getGhgIntensity(useKey);
  }

  @Override
  public EngineNumber getEqualsEnergyIntensity() {
    return simulationState.getEnergyIntensity(scope);
  }

  @Override
  public EngineNumber getEqualsEnergyIntensityFor(UseKey useKey) {
    return simulationState.getEnergyIntensity(useKey);
  }

  @Override
  public void changeStream(String stream, EngineNumber amount, YearMatcher yearMatcher) {
    changeStream(stream, amount, yearMatcher, null);
  }

  @Override
  public void changeStream(String stream, EngineNumber amount, YearMatcher yearMatcher,
      UseKey useKey) {
    if ("equipment".equals(stream)) {
      handleEquipmentChange(amount, yearMatcher);
      return;
    }

    UseKey useKeyEffective = useKey == null ? scope : useKey;
    changeExecutor.executeChange(stream, amount, yearMatcher, useKeyEffective);
  }

  /**
   * Handle equipment change with year range checking.
   *
   * <p>This method checks if the current year is within the specified range
   * before delegating to the equipment change utility. This ensures consistent
   * year checking behavior across all equipment operations.</p>
   *
   * @param amount The amount to change equipment by
   * @param yearMatcher The year matcher to check range against
   */
  private void handleEquipmentChange(EngineNumber amount, YearMatcher yearMatcher) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }
    equipmentChangeUtil.handleChange(amount);
  }

  @Override
  public void cap(String stream, EngineNumber amount, YearMatcher yearMatcher,
      String displaceTarget) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    // Handle equipment stream with special logic
    if ("equipment".equals(stream)) {
      equipmentChangeUtil.handleCap(amount, displaceTarget);
      return;
    }

    if ("%".equals(amount.getUnits())) {
      capWithPercent(stream, amount, displaceTarget);
    } else {
      capWithValue(stream, amount, displaceTarget);
    }
  }

  @Override
  public void floor(String stream, EngineNumber amount, YearMatcher yearMatcher,
      String displaceTarget) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    // Handle equipment stream with special logic
    if ("equipment".equals(stream)) {
      equipmentChangeUtil.handleFloor(amount, displaceTarget);
      return;
    }

    if ("%".equals(amount.getUnits())) {
      floorWithPercent(stream, amount, displaceTarget);
    } else {
      floorWithValue(stream, amount, displaceTarget);
    }
  }

  @Override
  public void replace(EngineNumber amountRaw, String stream, String destinationSubstance,
      YearMatcher yearMatcher) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    // Track the original user-specified units for the current substance
    Scope currentScope = scope;
    String application = currentScope.getApplication();
    String currentSubstance = currentScope.getSubstance();
    if (application == null || currentSubstance == null) {
      raiseNoAppOrSubstance("setting stream", " specified");
    }

    // Validate that we're not attempting to replace substance with itself
    if (currentSubstance.equals(destinationSubstance)) {
      ExceptionsGenerator.raiseSelfReplacement(currentSubstance);
    }

    if (isSalesStream(stream)) {
      // Track the specific stream and amount for the current substance
      simulationState.setLastSpecifiedValue(currentScope, stream, amountRaw);

      // Track the specific stream and amount for the destination substance
      SimpleUseKey destKey = new SimpleUseKey(application, destinationSubstance);
      simulationState.setLastSpecifiedValue(destKey, stream, amountRaw);
    }

    // For percentage operations, check lastSpecified value to determine unit type
    EngineNumber effectiveAmount = amountRaw;
    if (amountRaw.getUnits().equals("%")) {
      EngineNumber lastSpecified = simulationState.getLastSpecifiedValue(scope, stream);

      if (lastSpecified != null) {
        BigDecimal percentageValue = lastSpecified.getValue().multiply(amountRaw.getValue()).divide(new BigDecimal("100"));
        effectiveAmount = new EngineNumber(percentageValue, lastSpecified.getUnits());
      } else {
        // Use current value units to determine if unit-based logic should apply
        EngineNumber currentValue = getStream(stream);
        BigDecimal percentageValue = currentValue.getValue().multiply(amountRaw.getValue()).divide(new BigDecimal("100"));
        effectiveAmount = new EngineNumber(percentageValue, currentValue.getUnits());
      }
    }

    if (effectiveAmount.hasEquipmentUnits()) {
      // For equipment units, convert to units first, then handle each substance separately
      UnitConverter sourceUnitConverter = EngineSupportUtils.createUnitConverterWithTotal(this, stream);
      EngineNumber unitsToReplace = sourceUnitConverter.convert(effectiveAmount, "units");

      // Remove from source substance using source's initial charge
      EngineNumber sourceVolumeChange = sourceUnitConverter.convert(unitsToReplace, "kg");
      EngineNumber sourceAmountNegative = new EngineNumber(
          sourceVolumeChange.getValue().negate(),
          sourceVolumeChange.getUnits()
      );
      changeStreamWithoutReportingUnits(stream, sourceAmountNegative, Optional.empty(), Optional.empty());

      // Add to destination substance: convert the same number of units to destination's kg amount
      Scope destinationScope = scope.getWithSubstance(destinationSubstance);
      Scope originalScope = scope;
      scope = destinationScope;

      // Get the destination substance's initial charge for sales
      EngineNumber destinationInitialCharge = getInitialCharge("sales");

      // Create a state getter that uses the destination substance's initial charge
      OverridingConverterStateGetter destinationStateGetter =
          new OverridingConverterStateGetter(getStateGetter());
      destinationStateGetter.setAmortizedUnitVolume(destinationInitialCharge);
      UnitConverter destinationUnitConverter = new UnitConverter(destinationStateGetter);

      scope = originalScope;

      EngineNumber destinationVolumeChange = destinationUnitConverter.convert(unitsToReplace, "kg");
      changeStreamWithDisplacementContext(stream, destinationVolumeChange, destinationScope);
    } else {
      // For volume units, use the original logic
      UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(this, stream);
      EngineNumber amount = unitConverter.convert(effectiveAmount, "kg");

      EngineNumber amountNegative = new EngineNumber(amount.getValue().negate(), amount.getUnits());
      changeStreamWithoutReportingUnits(stream, amountNegative, Optional.empty(), Optional.empty());

      Scope destinationScope = scope.getWithSubstance(destinationSubstance);
      changeStreamWithDisplacementContext(stream, amount, destinationScope);
    }
  }

  @Override
  public List<EngineResult> getResults() {
    List<SubstanceInApplicationId> substances = simulationState.getRegisteredSubstances();
    EngineResultSerializer serializer = new EngineResultSerializer(this, stateGetter);

    return substances.stream()
        .map(substanceId -> {
          String application = substanceId.getApplication();
          String substance = substanceId.getSubstance();
          int year = simulationState.getCurrentYear();
          return serializer.getResult(new SimpleUseKey(application, substance), year);
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
    return EngineSupportUtils.isInRange(yearMatcher, simulationState.getCurrentYear());
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

    // Validate that we're not attempting to displace stream to itself
    if (stream.equals(displaceTarget)) {
      ExceptionsGenerator.raiseSelfDisplacement(stream);
    }

    // Check if this is a stream-based displacement (moved to top to avoid duplication)
    boolean isStream = STREAM_NAMES.contains(displaceTarget);

    // Automatic recycling addition: if recovery creates recycled material from sales stream,
    // always add it back to sales first before applying targeted displacement
    boolean displacementAutomatic = isStream && RECYCLE_RECOVER_STREAM.equals(stream);
    if (displacementAutomatic) {
      // Add recycled material back to sales to maintain total material balance
      EngineNumber recycledAddition = new EngineNumber(changeAmount, "kg");
      changeStreamWithoutReportingUnits(RECYCLE_RECOVER_STREAM, recycledAddition, Optional.empty(), Optional.empty());
    }

    EngineNumber displaceChange;

    if (amount.hasEquipmentUnits()) {
      // For equipment units, displacement should be unit-based, not volume-based
      UnitConverter currentUnitConverter = EngineSupportUtils.createUnitConverterWithTotal(this, stream);

      // Convert the volume change back to units in the original substance
      EngineNumber volumeChangeFlip = new EngineNumber(changeAmount.negate(), "kg");
      EngineNumber unitsChanged = currentUnitConverter.convert(volumeChangeFlip, "units");

      if (isStream) {
        // Same substance, same stream - use volume displacement
        displaceChange = new EngineNumber(changeAmount.negate(), "kg");

        changeStreamWithoutReportingUnits(displaceTarget, displaceChange, Optional.empty(), Optional.empty());
      } else {
        // Different substance - apply the same number of units to the destination substance
        Scope destinationScope = scope.getWithSubstance(displaceTarget);

        // Temporarily change scope to destination for unit conversion
        final Scope originalScope = scope;
        scope = destinationScope;
        UnitConverter destinationUnitConverter = EngineSupportUtils.createUnitConverterWithTotal(this, stream);

        // Convert units to destination substance volume using destination's initial charge
        EngineNumber destinationVolumeChange = destinationUnitConverter.convert(unitsChanged, "kg");
        displaceChange = new EngineNumber(destinationVolumeChange.getValue(), "kg");

        // Use custom recalc kit with destination substance's properties for correct GWP calculation
        changeStreamWithDisplacementContext(stream, displaceChange, destinationScope);

        // Restore original scope
        scope = originalScope;
      }
    } else {
      // For volume units, use volume-based displacement as before
      displaceChange = new EngineNumber(changeAmount.negate(), "kg");

      if (isStream) {
        changeStreamWithoutReportingUnits(displaceTarget, displaceChange, Optional.empty(), Optional.empty());
      } else {
        Scope destinationScope = scope.getWithSubstance(displaceTarget);
        // Use custom recalc kit with destination substance's properties for correct GWP calculation
        changeStreamWithDisplacementContext(stream, displaceChange, destinationScope);
      }
    }
  }

  /**
   * Change a stream value with proper displacement context for correct GWP calculations.
   *
   * <p>This method creates a custom recalc kit that uses the destination substance's
   * properties (GWP, initial charge, energy intensity) to ensure correct emissions
   * calculations during displacement operations.</p>
   *
   * @param stream The stream identifier to modify
   * @param amount The amount to change the stream by
   * @param destinationScope The scope for the destination substance
   */
  private void changeStreamWithDisplacementContext(String stream, EngineNumber amount, Scope destinationScope) {
    changeStreamWithDisplacementContext(stream, amount, destinationScope, false);
  }

  /**
   * Change a stream value with proper displacement context for correct GWP calculations.
   *
   * <p>This method creates a custom recalc kit that uses the destination substance's
   * properties (GWP, initial charge, energy intensity) to ensure correct emissions
   * calculations during displacement operations.</p>
   *
   * @param stream The stream identifier to modify
   * @param amount The amount to change the stream by
   * @param destinationScope The scope for the destination substance
   * @param negativeAllowed If true, negative stream values are permitted
   */
  private void changeStreamWithDisplacementContext(String stream, EngineNumber amount, Scope destinationScope, boolean negativeAllowed) {
    // Store original scope
    final Scope originalScope = scope;

    // Temporarily switch engine scope to destination substance
    scope = destinationScope;

    // Get current value and calculate new value (now using correct scope)
    EngineNumber currentValue = getStream(stream);
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(this, stream);

    EngineNumber convertedDelta = unitConverter.convert(amount, currentValue.getUnits());
    BigDecimal newAmount = currentValue.getValue().add(convertedDelta.getValue());

    BigDecimal newAmountBound;
    if (!negativeAllowed && newAmount.compareTo(BigDecimal.ZERO) < 0) {
      // Negative value and not allowed - clamp to zero and warn
      System.err.println("WARNING: Negative stream value clamped to zero for stream " + stream);
      newAmountBound = BigDecimal.ZERO;
    } else {
      // Either negative is allowed, or value is already non-negative
      newAmountBound = newAmount;
    }

    EngineNumber outputWithUnits = new EngineNumber(newAmountBound, currentValue.getUnits());

    // Set the stream value without triggering standard recalc to avoid double calculation
    StreamUpdate update = new StreamUpdateBuilder()
        .setName(stream)
        .setValue(outputWithUnits)
        .setPropagateChanges(false)
        .build();

    executeStreamUpdate(update);

    // Update lastSpecifiedValue for sales substreams since propagateChanges=false skips this
    if (isSalesStream(stream, false)) {
      UseKey destKey = new SimpleUseKey(destinationScope.getApplication(), destinationScope.getSubstance());
      simulationState.setLastSpecifiedValue(destKey, stream, outputWithUnits);
    }

    // Only recalculate for streams that affect equipment populations
    if (!isSalesStream(stream, false)) {
      scope = originalScope;
      return;
    }

    // Create standard recalc operation - engine scope is now correctly set to destination
    boolean useImplicitRecharge = false; // Displacement operations don't add recharge

    RecalcOperationBuilder builder = new RecalcOperationBuilder()
        .setRecalcKit(createRecalcKit()) // Use standard recalc kit - scope is correct now
        .setUseExplicitRecharge(!useImplicitRecharge)
        .recalcPopulationChange()
        .thenPropagateToConsumption();

    if (!OPTIMIZE_RECALCS) {
      builder = builder.thenPropagateToSales();
    }

    RecalcOperation operation = builder.build();
    operation.execute(this);

    // Restore original scope
    scope = originalScope;
  }

  /**
   * Check if a stream is a sales-related stream that influences recharge displacement.
   *
   * @param stream The stream name to check
   * @return true if the stream is sales, manufacture, import, or export
   */
  private boolean isSalesStream(String stream) {
    return isSalesStream(stream, true);
  }

  /**
   * Check if a stream is a sales-related stream.
   *
   * @param stream The stream name to check
   * @param includeExports Whether to include exports in the sales streams
   * @return true if the stream matches the sales-related criteria
   */
  private boolean isSalesStream(String stream, boolean includeExports) {
    boolean isCoreStream = "sales".equals(stream) || getIsSalesSubstream(stream);
    return isCoreStream || (includeExports && "export".equals(stream));
  }

  /**
   * Check if a stream name represents a sales substream (domestic or import).
   *
   * @param name The stream name to check
   * @return true if the stream is domestic or import
   */
  private boolean getIsSalesSubstream(String name) {
    return EngineSupportUtils.isSalesSubstream(name);
  }

  /**
   * Gets the distributed recharge amount for a specific stream.
   *
   * @param streamName The name of the stream
   * @param totalRecharge The total recharge amount
   * @param keyEffective The effective use key
   * @return The distributed recharge amount based on stream percentages
   */
  private BigDecimal getDistributedRecharge(String streamName, EngineNumber totalRecharge, UseKey keyEffective) {
    if ("sales".equals(streamName)) {
      // Sales stream gets 100% - setStreamForSales will distribute it
      return totalRecharge.getValue();
    } else if (getIsSalesSubstream(streamName)) {
      SalesStreamDistribution distribution = simulationState.getDistribution(keyEffective);
      BigDecimal percentage;
      if ("domestic".equals(streamName)) {
        percentage = distribution.getPercentDomestic();
      } else if ("import".equals(streamName)) {
        percentage = distribution.getPercentImport();
      } else {
        throw new IllegalArgumentException("Unknown sales substream: " + streamName);
      }
      return totalRecharge.getValue().multiply(percentage);
    } else {
      // Export and other streams get no recharge
      return BigDecimal.ZERO;
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
      Optional<YearMatcher> yearMatcher, Optional<UseKey> scope) {
    changeStreamWithoutReportingUnits(stream, amount, yearMatcher, scope, false);
  }

  /**
   * Change a stream value without reporting units to the last units tracking system.
   *
   * <p>This method is similar to changeStreamWithDisplacementContext but without the displacement
   * context. It allows for consistent handling of negative stream values across both methods.</p>
   *
   * @param stream The stream identifier to modify
   * @param amount The amount to change the stream by
   * @param yearMatcher Matcher to determine if the change applies to current year
   * @param scope The scope in which to make the change
   * @param negativeAllowed If true, negative stream values are permitted (useful for tests with
   *                         negative retire/recharge adjustments)
   */
  private void changeStreamWithoutReportingUnits(String stream, EngineNumber amount,
      Optional<YearMatcher> yearMatcher, Optional<UseKey> scope, boolean negativeAllowed) {
    if (!getIsInRange(yearMatcher.orElse(null))) {
      return;
    }

    EngineNumber currentValue = getStream(stream, scope, Optional.empty());
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(this, stream);

    EngineNumber convertedDelta = unitConverter.convert(amount, currentValue.getUnits());
    BigDecimal newAmount = currentValue.getValue().add(convertedDelta.getValue());

    BigDecimal newAmountBound;
    if (!negativeAllowed && newAmount.compareTo(BigDecimal.ZERO) < 0) {
      // Negative value and not allowed - clamp to zero and warn
      System.err.println("WARNING: Negative stream value clamped to zero for stream " + stream);
      newAmountBound = BigDecimal.ZERO;
    } else {
      // Either negative is allowed, or value is already non-negative
      newAmountBound = newAmount;
    }

    EngineNumber outputWithUnits = new EngineNumber(newAmountBound, currentValue.getUnits());

    // Allow propagation but don't track units (since units tracking was handled by the caller)
    StreamUpdateBuilder builder = new StreamUpdateBuilder()
        .setName(stream)
        .setValue(outputWithUnits);

    if (scope.isPresent()) {
      builder.setKey(scope.get());
    }

    StreamUpdate update = builder.build();
    executeStreamUpdate(update);
  }


  /**
   * Helper method to raise exception for missing application or substance.
   *
   * @param operation The operation being attempted
   * @param suffix Additional suffix for the error message (usually " specified")
   */
  private void raiseNoAppOrSubstance(String operation, String suffix) {
    ExceptionsGenerator.raiseNoAppOrSubstance(operation, suffix);
  }

  /**
   * Create a RecalcKit with this engine's dependencies.
   *
   * @return A RecalcKit containing this engine's simulationState, unitConverter, and stateGetter
   */
  private RecalcKit createRecalcKit() {
    return new RecalcKitBuilder()
        .setStreamKeeper(simulationState)
        .setUnitConverter(unitConverter)
        .setStateGetter(stateGetter)
        .build();
  }

  /**
   * Determine which value to use based on a branching value being zero.
   *
   * @param branchVal The value to branch on.
   * @param trueVal The value to use if the branch value is zero.
   * @param falseVal The value to use if the branch value is not zero.
   * @return The value based on branching.
   */
  private EngineNumber useIfZeroOrElse(EngineNumber branchVal, EngineNumber trueVal,
      EngineNumber falseVal) {
    boolean valueIsZero = isZero(branchVal);
    return valueIsZero ? trueVal : falseVal;
  }

  /**
   * Determine if the target is zero.
   *
   * @param target The number to check.
   * @return True if zero and false otherwise.
   */
  private boolean isZero(EngineNumber target) {
    return isZero(target.getValue());
  }

  /**
   * Determine if the target is zero.
   *
   * @param target The number to check.
   * @return True if zero and false otherwise.
   */
  private boolean isZero(BigDecimal target) {
    return target.compareTo(BigDecimal.ZERO) == 0;
  }

  /**
   * Updates sales carry-over tracking when setting manufacture, import, or sales.
   * This tracks user intent across carry-over years.
   *
   * @param useKey The key containing application and substance
   * @param streamName The name of the stream being set (manufacture, import, or sales)
   * @param value The value being set with units
   */
  private void updateSalesCarryOver(UseKey useKey, String streamName, EngineNumber value) {
    // Only process unit-based values for combination tracking
    if (!value.hasEquipmentUnits()) {
      return;
    }

    // Only handle manufacture and import streams for combination
    if (!getIsSalesSubstream(streamName)) {
      return;
    }

    // When setting manufacture or import, combine with the other to create sales intent
    String otherStream = "domestic".equals(streamName) ? "import" : "domestic";
    EngineNumber otherValue = simulationState.getLastSpecifiedValue(useKey, otherStream);

    if (otherValue != null && otherValue.hasEquipmentUnits()) {
      // Both streams have unit-based values - combine them
      // Convert both to the same units (prefer the current stream's units)
      String targetUnits = value.getUnits();
      UnitConverter converter = EngineSupportUtils.createUnitConverterWithTotal(this, streamName);
      EngineNumber otherConverted = converter.convert(otherValue, targetUnits);

      // Create combined sales value
      BigDecimal combinedValue = value.getValue().add(otherConverted.getValue());
      EngineNumber salesIntent = new EngineNumber(combinedValue, targetUnits);

      // Track the combined sales intent
      simulationState.setLastSpecifiedValue(useKey, "sales", salesIntent);
    } else {
      // Only one stream has units - use it as the sales intent
      simulationState.setLastSpecifiedValue(useKey, "sales", value);
    }
  }

  /**
   * Determines if current operations represent a carry-over situation.
   *
   * @param scope the scope to check
   * @return true if this is a carry-over situation, false otherwise
   */
  private boolean isCarryOver(UseKey scope) {
    // Check if we have a previous unit-based sales specification and no fresh input this year
    return !simulationState.isSalesIntentFreshlySet(scope)
           && EngineSupportUtils.hasUnitBasedSalesSpecifications(simulationState, scope);
  }

  /**
   * Check if sales streams were specified in equipment units.
   * When streams are specified in units, retirement changes population which changes
   * recharge requirements, so sales recalc is needed to update implicitRecharge.
   *
   * @return true if sales streams were specified in units
   */
  private boolean hasUnitBasedSalesSpecifications() {
    return EngineSupportUtils.hasUnitBasedSalesSpecifications(simulationState, scope);
  }

  /**
   * Calculate the available recycling volume for the current timestep.
   * This method replicates the recycling calculation logic to determine
   * how much recycling material is available to avoid double counting.
   * Now supports both EOL and recharge recycling stages.
   *
   * @param scope the scope to calculate recycling for
   * @return the amount of recycling available in kg
   */
  private BigDecimal calculateAvailableRecycling(UseKey scope) {
    // Get current prior population
    EngineNumber priorPopulationRaw = simulationState.getStream(scope, "priorEquipment");
    if (priorPopulationRaw == null) {
      return BigDecimal.ZERO;
    }

    // Get rates from parameterization
    EngineNumber retirementRate = simulationState.getRetirementRate(scope);
    EngineNumber rechargePopulation = simulationState.getRechargePopulation(scope);

    // Convert everything to proper units
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(this, "sales");
    EngineNumber priorPopulation = unitConverter.convert(priorPopulationRaw, "units");

    // Calculate rates as decimals
    BigDecimal retirementRateDecimal = retirementRate.getValue().divide(BigDecimal.valueOf(100));
    BigDecimal rechargePopulationDecimal = rechargePopulation.getValue().divide(BigDecimal.valueOf(100));

    // Calculate EOL recycling (from actual retired equipment)
    EngineNumber retiredPopulationRaw = simulationState.getStream(scope, "retired");
    EngineNumber retiredPopulation = unitConverter.convert(retiredPopulationRaw, "units");
    BigDecimal retiredUnits = retiredPopulation.getValue();
    BigDecimal eolRecycling = calculateRecyclingForStage(scope, retiredUnits, RecoveryStage.EOL, unitConverter);

    // Calculate recharge recycling (after recharge population)
    BigDecimal rechargedUnits = priorPopulation.getValue().multiply(rechargePopulationDecimal);
    BigDecimal rechargeRecycling = calculateRecyclingForStage(scope, rechargedUnits, RecoveryStage.RECHARGE, unitConverter);

    // Combine both recycling amounts
    BigDecimal totalRecycling = eolRecycling.add(rechargeRecycling);

    // Recycling does not apply cross-substance displacement
    return totalRecycling;
  }

  /**
   * Calculate recycling for a specific stage (EOL or RECHARGE).
   *
   * @param scope the scope to calculate recycling for
   * @param availableUnits the units available for recycling at this stage
   * @param stage the recovery stage (EOL or RECHARGE)
   * @param unitConverter the unit converter to use
   * @return the amount of recycling available in kg for this stage
   */
  private BigDecimal calculateRecyclingForStage(UseKey scope, BigDecimal availableUnits, RecoveryStage stage, UnitConverter unitConverter) {
    // Get stage-specific rates
    EngineNumber recoveryRate = simulationState.getRecoveryRate(scope, stage);
    EngineNumber yieldRate = simulationState.getYieldRate(scope, stage);

    // Calculate rates as decimals
    BigDecimal recoveryRateDecimal = recoveryRate.getValue().divide(BigDecimal.valueOf(100));
    BigDecimal yieldRateDecimal = yieldRate.getValue().divide(BigDecimal.valueOf(100));

    // Calculate recycling chain for this stage
    BigDecimal recoveredUnits = availableUnits.multiply(recoveryRateDecimal);
    BigDecimal recycledUnits = recoveredUnits.multiply(yieldRateDecimal);

    // Convert to kg
    EngineNumber initialCharge = simulationState.getInitialCharge(scope, "import");
    EngineNumber initialChargeKg = unitConverter.convert(initialCharge, "kg / unit");
    BigDecimal recycledKg = recycledUnits.multiply(initialChargeKg.getValue());

    return recycledKg;
  }

  /**
   * Updates lastSpecifiedValue for domestic and import streams after recycling.
   * This ensures that subsequent change operations use the recycling-adjusted values as their base.
   * Only applies to volume-based (mt/kg) specifications, not units-based ones.
   */
  private void updateLastSpecifiedValueAfterRecycling() {
    // Only update for volume-based specifications
    EngineNumber lastDomestic = simulationState.getLastSpecifiedValue(scope, "domestic");
    EngineNumber lastImport = simulationState.getLastSpecifiedValue(scope, "import");

    // Check if we have volume-based lastSpecifiedValues
    boolean hasVolumeDomestic = lastDomestic != null && !lastDomestic.hasEquipmentUnits();
    boolean hasVolumeImport = lastImport != null && !lastImport.hasEquipmentUnits();

    // If neither stream has volume-based specs, nothing to update
    if (!hasVolumeDomestic && !hasVolumeImport) {
      return;
    }

    // Get current recycling amount
    EngineNumber recycleAmount = getStream("recycle");
    if (recycleAmount == null || recycleAmount.getValue().compareTo(BigDecimal.ZERO) == 0) {
      return; // No recycling occurred
    }

    // Update domestic lastSpecifiedValue if it's volume-based
    if (hasVolumeDomestic) {
      EngineNumber currentDomestic = getStream("domestic");
      if (currentDomestic != null) {
        // Convert current value to the original units of lastSpecifiedValue
        UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(this, "domestic");
        EngineNumber domesticInOriginalUnits = unitConverter.convert(currentDomestic, lastDomestic.getUnits());
        simulationState.setLastSpecifiedValue(scope, "domestic", domesticInOriginalUnits);
      }
    }

    // Update import lastSpecifiedValue if it's volume-based
    if (hasVolumeImport) {
      EngineNumber currentImport = getStream("import");
      if (currentImport != null) {
        // Convert current value to the original units of lastSpecifiedValue
        UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(this, "import");
        EngineNumber importInOriginalUnits = unitConverter.convert(currentImport, lastImport.getUnits());
        simulationState.setLastSpecifiedValue(scope, "import", importInOriginalUnits);
      }
    }
  }

  /**
   * Apply percentage-based cap operation using lastSpecifiedValue for compounding effect.
   *
   * @param stream the stream name to cap
   * @param amount the percentage cap amount
   * @param displaceTarget the target substance for displacement, or null if no displacement
   */
  private void capWithPercent(String stream, EngineNumber amount, String displaceTarget) {
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(this, stream);
    EngineNumber currentValueRaw = getStream(stream);
    EngineNumber currentValue = unitConverter.convert(currentValueRaw, "kg");

    SimulationState simulationState = getStreamKeeper();
    EngineNumber lastSpecified = simulationState.getLastSpecifiedValue(scope, stream);

    if (lastSpecified != null) {
      BigDecimal capValue = lastSpecified.getValue().multiply(amount.getValue()).divide(new BigDecimal("100"));
      EngineNumber newCappedValue = new EngineNumber(capValue, lastSpecified.getUnits());

      EngineNumber currentInKg = unitConverter.convert(currentValueRaw, "kg");
      EngineNumber newCappedInKg = unitConverter.convert(newCappedValue, "kg");

      if (currentInKg.getValue().compareTo(newCappedInKg.getValue()) > 0) {
        StreamUpdate update = new StreamUpdateBuilder()
            .setName(stream)
            .setValue(newCappedValue)
            .setYearMatcher(Optional.empty())
            .inferSubtractRecycling()
            .build();
        executeStreamUpdate(update);

        if (displaceTarget != null) {
          EngineNumber finalInKg = getStream(stream);
          BigDecimal changeInKg = finalInKg.getValue().subtract(currentInKg.getValue());
          handleDisplacement(stream, amount, changeInKg, displaceTarget);
        }
      }
    } else {
      EngineNumber convertedMax = unitConverter.convert(amount, "kg");
      BigDecimal changeAmountRaw = convertedMax.getValue().subtract(currentValue.getValue());
      BigDecimal changeAmount = changeAmountRaw.min(BigDecimal.ZERO);

      if (changeAmount.compareTo(BigDecimal.ZERO) < 0) {
        EngineNumber changeWithUnits = new EngineNumber(changeAmount, "kg");
        changeStreamWithoutReportingUnits(stream, changeWithUnits, Optional.empty(), Optional.empty());
        handleDisplacement(stream, amount, changeAmount, displaceTarget);
      }
    }
  }

  /**
   * Apply absolute value-based cap operation.
   *
   * @param stream the stream name to cap
   * @param amount the absolute cap amount
   * @param displaceTarget the target substance for displacement, or null if no displacement
   */
  private void capWithValue(String stream, EngineNumber amount, String displaceTarget) {
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(this, stream);
    EngineNumber currentValueRaw = getStream(stream);
    EngineNumber currentValueInAmountUnits = unitConverter.convert(currentValueRaw, amount.getUnits());

    if (currentValueInAmountUnits.getValue().compareTo(amount.getValue()) > 0) {
      EngineNumber currentInKg = unitConverter.convert(currentValueRaw, "kg");
      StreamUpdate update = new StreamUpdateBuilder()
          .setName(stream)
          .setValue(amount)
          .setYearMatcher(Optional.empty())
          .inferSubtractRecycling()
          .build();
      executeStreamUpdate(update);

      if (displaceTarget != null) {
        EngineNumber cappedInKg = getStream(stream);
        BigDecimal changeInKg = cappedInKg.getValue().subtract(currentInKg.getValue());
        handleDisplacement(stream, amount, changeInKg, displaceTarget);
      }
    }
  }

  /**
   * Apply percentage-based floor operation using lastSpecifiedValue for compounding effect.
   *
   * @param stream the stream name to floor
   * @param amount the percentage floor amount
   * @param displaceTarget the target substance for displacement, or null if no displacement
   */
  private void floorWithPercent(String stream, EngineNumber amount, String displaceTarget) {
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(this, stream);
    EngineNumber currentValueRaw = getStream(stream);
    EngineNumber currentValue = unitConverter.convert(currentValueRaw, "kg");

    SimulationState simulationState = getStreamKeeper();
    EngineNumber lastSpecified = simulationState.getLastSpecifiedValue(scope, stream);

    if (lastSpecified != null) {
      BigDecimal floorValue = lastSpecified.getValue().multiply(amount.getValue()).divide(new BigDecimal("100"));
      EngineNumber newFloorValue = new EngineNumber(floorValue, lastSpecified.getUnits());

      EngineNumber currentInKg = unitConverter.convert(currentValueRaw, "kg");
      EngineNumber newFloorInKg = unitConverter.convert(newFloorValue, "kg");

      if (currentInKg.getValue().compareTo(newFloorInKg.getValue()) < 0) {
        StreamUpdate update = new StreamUpdateBuilder()
            .setName(stream)
            .setValue(newFloorValue)
            .setYearMatcher(Optional.empty())
            .inferSubtractRecycling()
            .build();
        executeStreamUpdate(update);

        if (displaceTarget != null) {
          EngineNumber finalInKg = getStream(stream);
          BigDecimal changeInKg = finalInKg.getValue().subtract(currentInKg.getValue());
          handleDisplacement(stream, amount, changeInKg, displaceTarget);
        }
      }
    } else {
      EngineNumber convertedMin = unitConverter.convert(amount, "kg");
      BigDecimal changeAmountRaw = convertedMin.getValue().subtract(currentValue.getValue());
      BigDecimal changeAmount = changeAmountRaw.max(BigDecimal.ZERO);

      if (changeAmount.compareTo(BigDecimal.ZERO) > 0) {
        EngineNumber changeWithUnits = new EngineNumber(changeAmount, "kg");
        changeStreamWithoutReportingUnits(stream, changeWithUnits, Optional.empty(), Optional.empty());
        handleDisplacement(stream, amount, changeAmount, displaceTarget);
      }
    }
  }

  /**
   * Apply absolute value-based floor operation.
   *
   * @param stream the stream name to floor
   * @param amount the absolute floor amount
   * @param displaceTarget the target substance for displacement, or null if no displacement
   */
  private void floorWithValue(String stream, EngineNumber amount, String displaceTarget) {
    UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(this, stream);
    EngineNumber currentValueRaw = getStream(stream);
    EngineNumber currentValueInAmountUnits = unitConverter.convert(currentValueRaw, amount.getUnits());

    if (currentValueInAmountUnits.getValue().compareTo(amount.getValue()) < 0) {
      EngineNumber currentInKg = unitConverter.convert(currentValueRaw, "kg");
      StreamUpdate update = new StreamUpdateBuilder()
          .setName(stream)
          .setValue(amount)
          .setYearMatcher(Optional.empty())
          .inferSubtractRecycling()
          .build();
      executeStreamUpdate(update);

      if (displaceTarget != null) {
        EngineNumber newInKg = getStream(stream);
        BigDecimal changeInKg = newInKg.getValue().subtract(currentInKg.getValue());
        handleDisplacement(stream, amount, changeInKg, displaceTarget);
      }
    }
  }

}
