/**
 * Single-threaded implementation of the Engine interface for Montreal Protocol simulations.
 *
 * <p>This class provides a concrete implementation of the Engine interface that is not
 * designed to be thread-safe. It manages substance streams, equipment populations, and calculations
 * related to the Montreal Protocol simulation using BigDecimal for numerical stability.</p>
 * 
 * <p>Note that, as described in the JavaDoc for the class, this does not prevent concurrency
 * within Kigali Sim which is achieved through parallelization outside of Engine, often by having
 * multiple Engine instances.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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
import org.kigalisim.engine.support.DisplaceExecutor;
import org.kigalisim.engine.support.EngineSupportUtils;
import org.kigalisim.engine.support.EquipmentChangeUtil;
import org.kigalisim.engine.support.ExceptionsGenerator;
import org.kigalisim.engine.support.LimitExecutor;
import org.kigalisim.engine.support.ReplaceExecutor;
import org.kigalisim.engine.support.SetExecutor;
import org.kigalisim.engine.support.StreamUpdateExecutor;
import org.kigalisim.engine.support.StreamUpdateShortcuts;
import org.kigalisim.lang.operation.RecoverOperation.RecoveryStage;

/**
 * Single-threaded implementation of the Engine interface.
 * 
 * <p>At this time the only implementation of Engine which does not guarantee thread safety
 * within a single scenario. However, note that Engine only evaluates one scenario at a time so
 * Kigali Sim still achieves concurency by having multiple Engines and evaluating scenarios or Monte
 * Carlo trials in parallel.</p>
 */
public class SingleThreadEngine implements Engine {

  private static final boolean OPTIMIZE_RECALCS = true;
  private static final String NO_APP_OR_SUBSTANCE_MESSAGE =
      "Tried %s without application and substance%s.";

  private final int startYear;
  private final int endYear;
  private String scenarioName;
  private int trialNumber;

  private final ConverterStateGetter stateGetter;
  private final UnitConverter unitConverter;
  private final SimulationState simulationState;
  private final ChangeExecutor changeExecutor;
  private final EquipmentChangeUtil equipmentChangeUtil;
  private final StreamUpdateExecutor streamUpdateExecutor;
  private final StreamUpdateShortcuts streamUpdateShortcuts;
  private final ReplaceExecutor replaceExecutor;
  private final DisplaceExecutor displaceExecutor;
  private final LimitExecutor limitExecutor;
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
    scenarioName = "";
    trialNumber = 0;

    stateGetter = new ConverterStateGetter(this);
    unitConverter = new UnitConverter(stateGetter);
    simulationState = new SimulationState(
        new OverridingConverterStateGetter(stateGetter),
        unitConverter
    );
    simulationState.setCurrentYear(startYear);
    changeExecutor = new ChangeExecutor(this);
    equipmentChangeUtil = new EquipmentChangeUtil(this);
    streamUpdateExecutor = new StreamUpdateExecutor(this);
    streamUpdateShortcuts = new StreamUpdateShortcuts(this);
    replaceExecutor = new ReplaceExecutor(this);
    displaceExecutor = new DisplaceExecutor(this);
    limitExecutor = new LimitExecutor(this);
    scope = new Scope();
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
  public void setSubstance(String newSubstance, boolean checkValid) {
    scope = scope.getWithSubstance(newSubstance);

    if (checkValid) {
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

  @Override
  public ConverterStateGetter getStateGetter() {
    return stateGetter;
  }

  @Override
  public UnitConverter getUnitConverter() {
    return unitConverter;
  }

  @Override
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
    final Optional<YearMatcher> yearMatcher = update.getYearMatcher();

    if (!getIsInRange(yearMatcher)) {
      return;
    }

    final Optional<UseKey> key = update.getKey();
    UseKey keyEffective = key.orElse(scope);
    String application = keyEffective.getApplication();
    String substance = keyEffective.getSubstance();

    if (application == null || substance == null) {
      raiseNoAppOrSubstance("setting stream", " specified");
    }

    streamUpdateExecutor.execute(update);
  }


  @Override
  public void setStream(String name, EngineNumber value, Optional<YearMatcher> yearMatcher) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    if ("equipment".equals(name)) {
      equipmentChangeUtil.handleSet(value);
    } else if ("sales".equals(name)) {
      simulationState.clearLastSpecifiedValue(scope, name);
      SetExecutor setExecutor = new SetExecutor(this);
      setExecutor.handleSalesSet(scope, name, value, yearMatcher);
    } else {
      if (EngineSupportUtils.isSalesSubstream(name)) {
        simulationState.clearLastSpecifiedValue(scope, name);
      }
      StreamUpdate update = new StreamUpdateBuilder()
          .setName(name)
          .setValue(value)
          .setYearMatcher(yearMatcher)
          .inferSubtractRecycling()
          .build();
      executeStreamUpdate(update);
    }
  }

  @Override
  public void enable(String name, Optional<YearMatcher> yearMatcher) {
    if (!getIsInRange(yearMatcher)) {
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
    return switch (name) {
      case "yearsElapsed" -> new EngineNumber(BigDecimal.valueOf(simulationState.getCurrentYear() - startYear), "years");
      case "yearAbsolute" -> new EngineNumber(BigDecimal.valueOf(simulationState.getCurrentYear()), "year");
      default -> scope.getVariable(name);
    };
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
        return getSalesWeightedInitialCharge();
      } catch (IllegalStateException e) {
        // Fallback: if no streams are enabled, return zero
        return new EngineNumber(BigDecimal.ZERO, "kg / unit");
      }
    } else {
      return getRawInitialChargeFor(scope, stream);
    }
  }

  private EngineNumber getSalesWeightedInitialCharge() {
    SalesStreamDistribution distribution = simulationState.getDistribution(scope, false);

    BigDecimal domesticWeight = distribution.getPercentDomestic();
    BigDecimal importWeight = distribution.getPercentImport();

    EngineNumber domesticInitialChargeRaw = getRawInitialChargeFor(scope, "domestic");
    EngineNumber domesticInitialCharge = unitConverter.convert(domesticInitialChargeRaw, "kg / unit");

    EngineNumber importInitialChargeRaw = getRawInitialChargeFor(scope, "import");
    EngineNumber importInitialCharge = unitConverter.convert(importInitialChargeRaw, "kg / unit");

    // Calculate weighted values
    BigDecimal domesticWeighted = domesticInitialCharge.getValue().multiply(domesticWeight);
    BigDecimal importWeighted = importInitialCharge.getValue().multiply(importWeight);

    BigDecimal weightedSum = domesticWeighted.add(importWeighted);

    return new EngineNumber(weightedSum, "kg / unit");
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
   * <p>For sales streams, checks if either domestic or import were last specified in units.
   * If specified in units, recharge is added on top. For domestic or import streams, checks if that
   * specific channel was last specified in units. If not specified in units, recharge is
   * subtracted.</p>
   *
   * @param stream The stream being set
   * @return true if recharge should be subtracted, false if added on top
   */
  private boolean getShouldUseExplicitRecharge(String stream) {
    return switch (stream) {
      case "sales" -> {
        Optional<String> lastUnits = getLastSalesUnits(scope);
        yield lastUnits.isEmpty() || !lastUnits.get().startsWith("unit");
      }
      case "domestic", "import" -> {
        Optional<String> lastUnits = getLastSalesUnits(scope);
        yield !lastUnits.isPresent() || !lastUnits.get().startsWith("unit");
      }
      default -> true;
    };
  }

  @Override
  public EngineNumber getRechargeVolume() {
    return simulationState.getRechargePopulation(scope);
  }

  @Override
  public EngineNumber getRechargeIntensity() {
    return simulationState.getRechargeIntensity(scope);
  }

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

    boolean isCarryOver = getIsCarryOver(scope);

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
      return;
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
      // Default behavior - reset to 100% (induced demand behavior)
      resetInductionRate(stage);
    }
  }

  @Override
  public void resetInductionRate(RecoveryStage stage) {
    EngineNumber defaultInductionRate = new EngineNumber(new BigDecimal("100"), "%");
    simulationState.setInductionRate(scope, defaultInductionRate, stage);
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
   * before delegating to the equipment change utility. This ensures consistent year checking behavior
   * across all equipment operations.</p>
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

    limitExecutor.executeCap(stream, amount, yearMatcher, displaceTarget);
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

    limitExecutor.executeFloor(stream, amount, yearMatcher, displaceTarget);
  }

  @Override
  public void replace(EngineNumber amountRaw, String stream, String destinationSubstance,
      YearMatcher yearMatcher) {
    replaceExecutor.execute(amountRaw, stream, destinationSubstance, yearMatcher);
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

  @Override
  public boolean getOptimizeRecalcs() {
    return OPTIMIZE_RECALCS;
  }

  /**
   * Helper method to determine if a year matcher applies to current year.
   *
   * @param yearMatcher The year matcher to check
   * @return True if in range or no matcher provided
   */
  private boolean getIsInRange(YearMatcher yearMatcher) {
    return EngineSupportUtils.getIsInRange(yearMatcher, simulationState.getCurrentYear());
  }

  /**
   * Helper method to determine if a year matcher applies to current year.
   *
   * @param yearMatcher The optional year matcher to check
   * @return True if in range or no matcher provided
   */
  private boolean getIsInRange(Optional<YearMatcher> yearMatcher) {
    return EngineSupportUtils.getIsInRange(yearMatcher, simulationState.getCurrentYear());
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
   * Determines if unit-based sales from a previous year carry over into the current year.
   *
   * <p>Carry over occurs when equipment sales were previously specified in units
   * (e.g., "800 units") but the current year has no fresh specification. When detected during
   * recharge operations, the engine re-applies the previous sales value and adds implicit recharge on
   * top, preserving the user's intent that unit counts represent new equipment sales, not total
   * substance volume.</p>
   *
   * @param scope The scope (application/substance) to check for carry over state
   * @return true if this is a carry-over scenario (unit-based sales without fresh
   *     specification), false otherwise
   */
  private boolean getIsCarryOver(UseKey scope) {
    // Check if we have a previous unit-based sales specification and no fresh input this year
    return !simulationState.isSalesIntentFreshlySet(scope)
           && EngineSupportUtils.hasUnitBasedSalesSpecifications(simulationState, scope);
  }

  /**
   * Check if sales streams were specified in equipment units.
   * When streams are specified in units, retirement changes population which changes recharge
   * requirements, so sales recalc is needed to update implicitRecharge.
   *
   * @return true if sales streams were specified in units
   */
  private boolean hasUnitBasedSalesSpecifications() {
    return EngineSupportUtils.hasUnitBasedSalesSpecifications(simulationState, scope);
  }

  /**
   * Calculate the available recycling volume for the current timestep.
   * This method replicates the recycling calculation logic to determine how much recycling material
   * is available to avoid double counting.
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


}
