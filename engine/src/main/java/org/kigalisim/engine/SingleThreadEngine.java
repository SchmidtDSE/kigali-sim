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
import java.math.MathContext;
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
import org.kigalisim.engine.serializer.EngineResult;
import org.kigalisim.engine.serializer.EngineResultSerializer;
import org.kigalisim.engine.state.ConverterStateGetter;
import org.kigalisim.engine.state.OverridingConverterStateGetter;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.engine.state.SimpleUseKey;
import org.kigalisim.engine.state.StreamKeeper;
import org.kigalisim.engine.state.SubstanceInApplicationId;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;
import org.kigalisim.engine.support.DivisionHelper;
import org.kigalisim.engine.support.ExceptionsGenerator;
import org.kigalisim.engine.support.RechargeVolumeCalculator;

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
    STREAM_NAMES.add("sales"); // ⚠️ Added for uniform displacement - monitor for side effects
  }

  private static final String RECYCLE_RECOVER_STREAM = "sales";

  private final int startYear;
  private final int endYear;
  private int currentYear;
  private String scenarioName;
  private int trialNumber;

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
    currentYear = startYear;
    this.scenarioName = "";
    this.trialNumber = 0;

    stateGetter = new ConverterStateGetter(this);
    unitConverter = new UnitConverter(stateGetter);
    this.streamKeeper = new StreamKeeper(
        new OverridingConverterStateGetter(stateGetter), unitConverter);
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
      boolean knownSubstance = streamKeeper.hasSubstance(scope);
      if (!knownSubstance) {
        throw new RuntimeException("Tried accessing unknown app / substance pair: "
            + scope.getApplication() + ", " + newSubstance);
      }
    } else {
      streamKeeper.ensureSubstance(scope);
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

  /**
   * Get the unit converter instance.
   *
   * @return The unit converter
   */
  public UnitConverter getUnitConverter() {
    return unitConverter;
  }

  /**
   * Get the stream keeper instance.
   *
   * @return The stream keeper
   */
  public StreamKeeper getStreamKeeper() {
    return streamKeeper;
  }

  @Override
  public void incrementYear() {
    if (getIsDone()) {
      throw new RuntimeException("Already completed.");
    }
    currentYear += 1;
    streamKeeper.incrementYear();
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
  public void setStreamFor(String name, EngineNumber value, Optional<YearMatcher> yearMatcher, Optional<UseKey> key,
                           boolean propagateChanges, Optional<String> unitsToRecord) {
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
    
    EngineNumber valueToSet = value;
    if (isSales && isUnits) {
      // Convert to kg and add recharge on top
      UnitConverter unitConverter = createUnitConverterWithTotal(name);
      EngineNumber valueInKg = unitConverter.convert(value, "kg");
      EngineNumber rechargeVolume = RechargeVolumeCalculator.calculateRechargeVolume(
          keyEffective,
          stateGetter,
          streamKeeper,
          this
      );
      
      BigDecimal totalWithRecharge = valueInKg.getValue().add(rechargeVolume.getValue());
      valueToSet = new EngineNumber(totalWithRecharge, "kg");
      
      
      // Set implicit recharge to indicate we've added recharge automatically
      streamKeeper.setStream(keyEffective, "implicitRecharge", rechargeVolume);
    } else if (isSales) {
      // Sales stream without units - clear implicit recharge
      streamKeeper.setStream(keyEffective, "implicitRecharge", new EngineNumber(BigDecimal.ZERO, "kg"));
    }

    streamKeeper.setStream(keyEffective, name, valueToSet);

    // Track the units last used to specify this stream (only for user-initiated calls)
    if (!propagateChanges) {
      return;
    }

    if (isSales) {
      // Track the last specified value for sales-related streams
      // This preserves user intent across carry-over years
      streamKeeper.setLastSpecifiedValue(keyEffective, name, value);
      
      // Handle stream combinations for unit preservation
      updateSalesCarryOver(keyEffective, name, value);
    }
    
    if ("sales".equals(name) || "manufacture".equals(name) || "import".equals(name)) {
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
  public void setStream(String name, EngineNumber value, Optional<YearMatcher> yearMatcher) {
    setStreamFor(name, value, yearMatcher, Optional.empty(), true, Optional.empty());
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
    if ("manufacture".equals(name) || "import".equals(name) || "export".equals(name)) {
      streamKeeper.markStreamAsEnabled(keyEffective, name);
    }
  }

  @Override
  public EngineNumber getStream(String name, Optional<UseKey> useKey, Optional<String> conversion) {
    UseKey effectiveKey = useKey.orElse(scope);
    EngineNumber value = streamKeeper.getStream(effectiveKey, name);

    return conversion.map(conv -> unitConverter.convert(value, conv)).orElse(value);
  }

  @Override
  public EngineNumber getStream(String name) {
    return getStream(name, Optional.of(scope), Optional.empty());
  }

  @Override
  public EngineNumber getStreamFor(UseKey key, String stream) {
    return streamKeeper.getStream(key, stream);
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
      return new EngineNumber(BigDecimal.valueOf(currentYear - startYear), "years");
    } else if ("yearAbsolute".equals(name)) {
      return new EngineNumber(BigDecimal.valueOf(currentYear), "year");
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

  // Placeholder implementations for remaining methods - to be completed
  @Override
  public EngineNumber getInitialCharge(String stream) {
    if ("sales".equals(stream)) {

      // Get manufacture and import values
      EngineNumber manufactureRaw = getStream("manufacture");
      EngineNumber manufactureValue = unitConverter.convert(manufactureRaw, "kg");

      if (manufactureValue.getValue().compareTo(BigDecimal.ZERO) == 0) {
        return getRawInitialChargeFor(scope, "import");
      }

      EngineNumber importRaw = getStream("import");
      EngineNumber importValue = unitConverter.convert(importRaw, "kg");

      if (importValue.getValue().compareTo(BigDecimal.ZERO) == 0) {
        return getRawInitialChargeFor(scope, "manufacture");
      }

      // Determine total
      boolean emptyStreams = isEmptyStreams(manufactureValue, importValue);

      // Get initial charges
      EngineNumber manufactureInitialChargeRaw = getRawInitialChargeFor(scope, "manufacture");
      EngineNumber manufactureChargeUnbound = unitConverter.convert(
          manufactureInitialChargeRaw, "kg / unit");

      EngineNumber importInitialChargeRaw = getRawInitialChargeFor(scope, "import");
      EngineNumber importChargeUnbound = unitConverter.convert(
          importInitialChargeRaw, "kg / unit");

      // Get bounded values
      EngineNumber manufactureCharge = useIfZeroOrElse(
          manufactureChargeUnbound,
          importChargeUnbound,
          manufactureChargeUnbound
      );
      EngineNumber importInitialCharge = useIfZeroOrElse(
          importChargeUnbound,
          manufactureChargeUnbound,
          importChargeUnbound
      );

      // Calculate units
      BigDecimal manufactureKg = emptyStreams ? BigDecimal.ONE : manufactureValue.getValue();
      BigDecimal importKg = emptyStreams ? BigDecimal.ONE : importValue.getValue();
      BigDecimal manufactureKgUnit = manufactureCharge.getValue();
      BigDecimal importKgUnit = importInitialCharge.getValue();

      BigDecimal manufactureUnits = DivisionHelper.divideWithZero(
          manufactureKg,
          manufactureKgUnit
      );

      BigDecimal importUnits = DivisionHelper.divideWithZero(
          importKg,
          importKgUnit
      );

      boolean emptyPopulation = manufactureUnits.compareTo(BigDecimal.ZERO) == 0
          && importUnits.compareTo(BigDecimal.ZERO) == 0;

      if (emptyPopulation) {
        return new EngineNumber(BigDecimal.ZERO, "kg / unit");
      } else {
        BigDecimal newSumWeighted = manufactureKgUnit.multiply(manufactureUnits)
            .add(importKgUnit.multiply(importUnits));
        BigDecimal newSumWeight = manufactureUnits.add(importUnits);
        BigDecimal pooledKgUnit = newSumWeighted.divide(newSumWeight, MathContext.DECIMAL128);
        return new EngineNumber(pooledKgUnit, "kg / unit");
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
    return streamKeeper.getInitialCharge(useKey, stream);
  }

  @Override
  public void setInitialCharge(EngineNumber value, String stream, YearMatcher yearMatcher) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    if ("sales".equals(stream)) {
      // For sales, set both manufacture and import but don't recalculate yet
      streamKeeper.setInitialCharge(scope, "manufacture", value);
      streamKeeper.setInitialCharge(scope, "import", value);
    } else {
      streamKeeper.setInitialCharge(scope, stream, value);
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
    EngineNumber lastValue = streamKeeper.getLastSpecifiedValue(useKey, "sales");
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
    } else if ("manufacture".equals(stream) || "import".equals(stream)) {
      // For manufacture or import, check if that specific channel was last specified in units
      Optional<String> lastUnits = getLastSalesUnits(scope);
      return !lastUnits.isPresent() || !lastUnits.get().startsWith("unit"); // Add recharge on top
    }

    return true;
  }

  @Override
  public EngineNumber getRechargeVolume() {
    return streamKeeper.getRechargePopulation(scope);
  }

  @Override
  public EngineNumber getRechargeIntensity() {
    return streamKeeper.getRechargeIntensity(scope);
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

    // Set values
    streamKeeper.setRechargePopulation(scope, volume);
    streamKeeper.setRechargeIntensity(scope, intensity);

    boolean isCarryOver = isCarryOver(scope);
    
    if (isCarryOver) {
      // Preserve user's original unit-based intent
      // Use setStreamFor with the original value - this will automatically add recharge on top
      EngineNumber lastSalesValue = streamKeeper.getLastSpecifiedValue(scope, "sales");
      setStreamFor("sales", lastSalesValue, Optional.empty(), Optional.of(scope), true, Optional.empty());
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
        streamKeeper.setStream(scope, "implicitRecharge", new EngineNumber(BigDecimal.ZERO, "kg"));
      }
    }
  }

  @Override
  public void retire(EngineNumber amount, YearMatcher yearMatcher) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }
    streamKeeper.setRetirementRate(scope, amount);
    RecalcOperation operation = new RecalcOperationBuilder()
        .setRecalcKit(createRecalcKit())
        .recalcRetire()
        .build();
    operation.execute(this);
  }

  @Override
  public EngineNumber getRetirementRate() {
    return streamKeeper.getRetirementRate(scope);
  }

  @Override
  public void recycle(EngineNumber recoveryWithUnits, EngineNumber yieldWithUnits,
      YearMatcher yearMatcher) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    streamKeeper.setRecoveryRate(scope, recoveryWithUnits);
    streamKeeper.setYieldRate(scope, yieldWithUnits);

    RecalcOperation operation = new RecalcOperationBuilder()
        .setRecalcKit(createRecalcKit())
        .recalcSales()
        .thenPropagateToPopulationChange()
        .thenPropagateToConsumption()
        .build();
    operation.execute(this);
  }

  @Override
  public void recycle(EngineNumber recoveryWithUnits, EngineNumber yieldWithUnits,
      YearMatcher yearMatcher, String displacementTarget) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    streamKeeper.setRecoveryRate(scope, recoveryWithUnits);
    streamKeeper.setYieldRate(scope, yieldWithUnits);

    // Apply the recovery through normal recycle operation
    RecalcOperation operation = new RecalcOperationBuilder()
        .setRecalcKit(createRecalcKit())
        .recalcSales()
        .thenPropagateToPopulationChange()
        .thenPropagateToConsumption()
        .build();
    operation.execute(this);

    // Handle displacement using the existing displacement logic
    UnitConverter unitConverter = createUnitConverterWithTotal(RECYCLE_RECOVER_STREAM);
    EngineNumber recoveryInKg = unitConverter.convert(recoveryWithUnits, "kg");
    handleDisplacement(RECYCLE_RECOVER_STREAM, recoveryWithUnits, recoveryInKg.getValue(), displacementTarget);
  }

  @Override
  public void equals(EngineNumber amount, YearMatcher yearMatcher) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    String units = amount.getUnits();
    boolean isGhg = units.startsWith("tCO2e");
    boolean isKwh = units.startsWith("kwh");

    if (isGhg) {
      streamKeeper.setGhgIntensity(scope, amount);
      RecalcOperation operation = new RecalcOperationBuilder()
          .setScopeEffective(scope)
          .setRecalcKit(createRecalcKit())
          .recalcRechargeEmissions()
          .thenPropagateToEolEmissions()
          .build();
      operation.execute(this);
    } else if (isKwh) {
      streamKeeper.setEnergyIntensity(scope, amount);
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
    return streamKeeper.getGhgIntensity(useKey);
  }

  @Override
  public EngineNumber getEqualsGhgIntensity() {
    return streamKeeper.getGhgIntensity(scope);
  }

  @Override
  public EngineNumber getEqualsGhgIntensityFor(UseKey useKey) {
    return streamKeeper.getGhgIntensity(useKey);
  }

  @Override
  public EngineNumber getEqualsEnergyIntensity() {
    return streamKeeper.getEnergyIntensity(scope);
  }

  @Override
  public void changeStream(String stream, EngineNumber amount, YearMatcher yearMatcher) {
    changeStream(stream, amount, yearMatcher, null);
  }

  @Override
  public void changeStream(String stream, EngineNumber amount, YearMatcher yearMatcher,
      UseKey useKey) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    UseKey useKeyEffective = useKey == null ? scope : useKey;
    EngineNumber currentValue = getStream(stream, Optional.of(useKeyEffective), Optional.empty());
    UnitConverter unitConverter = createUnitConverterWithTotal(stream);

    EngineNumber convertedDelta = unitConverter.convert(amount, currentValue.getUnits());
    BigDecimal newAmount = currentValue.getValue().add(convertedDelta.getValue());
    EngineNumber outputWithUnits = new EngineNumber(newAmount, currentValue.getUnits());

    // Need to convert UseKey to Scope for setStream call since setStream requires scope for variable management
    setStreamFor(stream, outputWithUnits, Optional.empty(), Optional.of(useKeyEffective), true, Optional.of(amount.getUnits()));
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

    // Handle percentage caps differently - use change approach
    if ("%".equals(amount.getUnits())) {
      // Convert percentage to kg
      EngineNumber convertedMax = unitConverter.convert(amount, "kg");
      
      BigDecimal changeAmountRaw = convertedMax.getValue().subtract(currentValue.getValue());
      BigDecimal changeAmount = changeAmountRaw.min(BigDecimal.ZERO);
      
      if (changeAmount.compareTo(BigDecimal.ZERO) < 0) {
        EngineNumber changeWithUnits = new EngineNumber(changeAmount, "kg");
        changeStreamWithoutReportingUnits(stream, changeWithUnits, null, null);
        handleDisplacement(stream, amount, changeAmount, displaceTarget);
      }
    } else {
      // For non-percentage caps, use setStream approach
      EngineNumber currentValueInAmountUnits = unitConverter.convert(currentValueRaw, amount.getUnits());
      
      if (currentValueInAmountUnits.getValue().compareTo(amount.getValue()) > 0) {
        // Get current value in kg before capping
        EngineNumber currentInKg = unitConverter.convert(currentValueRaw, "kg");
        
        // Current exceeds cap, so set to the cap value
        setStream(stream, amount, Optional.empty());
        
        // Calculate displacement if needed
        if (displaceTarget != null) {
          // Get the new value in kg after capping (includes recharge if units)
          EngineNumber cappedInKg = getStream(stream);
          // Calculate the actual change in kg (negative for reduction, positive for increase)
          BigDecimal changeInKg = cappedInKg.getValue().subtract(currentInKg.getValue());
          handleDisplacement(stream, amount, changeInKg, displaceTarget);
        }
      }
    }
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

    // Handle percentage floors differently - use change approach
    if ("%".equals(amount.getUnits())) {
      // Convert percentage to kg
      EngineNumber convertedMin = unitConverter.convert(amount, "kg");
      
      BigDecimal changeAmountRaw = convertedMin.getValue().subtract(currentValue.getValue());
      BigDecimal changeAmount = changeAmountRaw.max(BigDecimal.ZERO);
      
      if (changeAmount.compareTo(BigDecimal.ZERO) > 0) {
        EngineNumber changeWithUnits = new EngineNumber(changeAmount, "kg");
        changeStreamWithoutReportingUnits(stream, changeWithUnits, null, null);
        handleDisplacement(stream, amount, changeAmount, displaceTarget);
      }
    } else {
      // For non-percentage floors, use setStream approach
      EngineNumber currentValueInAmountUnits = unitConverter.convert(currentValueRaw, amount.getUnits());
      
      if (currentValueInAmountUnits.getValue().compareTo(amount.getValue()) < 0) {
        // Get current value in kg before flooring
        EngineNumber currentInKg = unitConverter.convert(currentValueRaw, "kg");
        
        // Current is below floor, so set to the floor value
        setStream(stream, amount, Optional.empty());
        
        // Calculate displacement if needed
        if (displaceTarget != null) {
          // Get the new value in kg after flooring (includes recharge if units)
          EngineNumber newInKg = getStream(stream);
          // Calculate the actual change in kg (positive for increase)
          BigDecimal changeInKg = newInKg.getValue().subtract(currentInKg.getValue());
          handleDisplacement(stream, amount, changeInKg, displaceTarget);
        }
      }
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
    
    if (isSalesStream(stream)) {
      // Track the specific stream and amount for the current substance
      streamKeeper.setLastSpecifiedValue(currentScope, stream, amountRaw);

      // Track the specific stream and amount for the destination substance
      SimpleUseKey destKey = new SimpleUseKey(application, destinationSubstance);
      streamKeeper.setLastSpecifiedValue(destKey, stream, amountRaw);
    }

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
      Scope destinationScope = scope.getWithSubstance(destinationSubstance);
      Scope originalScope = scope;
      scope = destinationScope;
      UnitConverter destinationUnitConverter = createUnitConverterWithTotal(stream);
      scope = originalScope;

      EngineNumber destinationVolumeChange = destinationUnitConverter.convert(unitsToReplace, "kg");
      changeStreamWithoutReportingUnits(stream, destinationVolumeChange, null, destinationScope);
    } else {
      // For volume units, use the original logic
      UnitConverter unitConverter = createUnitConverterWithTotal(stream);
      EngineNumber amount = unitConverter.convert(amountRaw, "kg");

      EngineNumber amountNegative = new EngineNumber(amount.getValue().negate(), amount.getUnits());
      changeStreamWithoutReportingUnits(stream, amountNegative, null, null);

      Scope destinationScope = scope.getWithSubstance(destinationSubstance);
      changeStreamWithoutReportingUnits(stream, amount, null, destinationScope);
    }
  }

  @Override
  public List<EngineResult> getResults() {
    List<SubstanceInApplicationId> substances = streamKeeper.getRegisteredSubstances();
    EngineResultSerializer serializer = new EngineResultSerializer(this, stateGetter);

    return substances.stream()
        .map(substanceId -> {
          String application = substanceId.getApplication();
          String substance = substanceId.getSubstance();
          int year = currentYear;
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
    return yearMatcher == null || yearMatcher.getInRange(currentYear);
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

    // Check if this is a stream-based displacement (moved to top to avoid duplication)
    boolean isStream = STREAM_NAMES.contains(displaceTarget);
    
    // Automatic recycling addition: if recovery creates recycled material from sales stream,
    // always add it back to sales first before applying targeted displacement
    boolean displacementAutomatic = isStream && RECYCLE_RECOVER_STREAM.equals(stream);
    if (displacementAutomatic) {
      // Add recycled material back to sales to maintain total material balance
      EngineNumber recycledAddition = new EngineNumber(changeAmount, "kg");
      changeStreamWithoutReportingUnits(RECYCLE_RECOVER_STREAM, recycledAddition, null, null);
    }

    EngineNumber displaceChange;

    if (amount.hasEquipmentUnits()) {
      // For equipment units, displacement should be unit-based, not volume-based
      UnitConverter currentUnitConverter = createUnitConverterWithTotal(stream);

      // Convert the volume change back to units in the original substance
      EngineNumber volumeChangeFlip = new EngineNumber(changeAmount.negate(), "kg");
      EngineNumber unitsChanged = currentUnitConverter.convert(volumeChangeFlip, "units");

      if (isStream) {
        // Same substance, same stream - use volume displacement
        displaceChange = new EngineNumber(changeAmount.negate(), "kg");
        
        changeStreamWithoutReportingUnits(displaceTarget, displaceChange, null, null);
      } else {
        // Different substance - apply the same number of units to the destination substance
        Scope destinationScope = scope.getWithSubstance(displaceTarget);

        // Temporarily change scope to destination for unit conversion
        final Scope originalScope = scope;
        scope = destinationScope;
        UnitConverter destinationUnitConverter = createUnitConverterWithTotal(stream);

        // Convert units to destination substance volume using destination's initial charge
        EngineNumber destinationVolumeChange = destinationUnitConverter.convert(unitsChanged, "kg");
        displaceChange = new EngineNumber(destinationVolumeChange.getValue(), "kg");
        
        changeStreamWithoutReportingUnits(stream, displaceChange, null, destinationScope);

        // Restore original scope
        scope = originalScope;
      }
    } else {
      // For volume units, use volume-based displacement as before
      displaceChange = new EngineNumber(changeAmount.negate(), "kg");

      if (isStream) {
        changeStreamWithoutReportingUnits(displaceTarget, displaceChange, null, null);
      } else {
        Scope destinationScope = scope.getWithSubstance(displaceTarget);
        changeStreamWithoutReportingUnits(stream, displaceChange, null, destinationScope);
      }
    }
  }

  /**
   * Check if a stream is a sales-related stream that influences recharge displacement.
   *
   * @param stream The stream name to check
   * @return true if the stream is sales, manufacture, import, or export
   */
  private boolean isSalesStream(String stream) {
    return "sales".equals(stream) || "manufacture".equals(stream) || "import".equals(stream) || "export".equals(stream);
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

    EngineNumber currentValue = getStream(stream, Optional.ofNullable(scope), Optional.empty());
    UnitConverter unitConverter = createUnitConverterWithTotal(stream);

    EngineNumber convertedDelta = unitConverter.convert(amount, currentValue.getUnits());
    BigDecimal newAmount = currentValue.getValue().add(convertedDelta.getValue());
    BigDecimal newAmountBound = newAmount.max(BigDecimal.ZERO);
    
    // Warn when negative values are clamped to zero
    if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
      StringBuilder warning = new StringBuilder();
      warning.append("WARNING: Negative stream value clamped to zero for stream '");
      warning.append(stream);
      warning.append("': ");
      warning.append(newAmount);
      warning.append(" ");
      warning.append(currentValue.getUnits());
      warning.append(" -> 0 ");
      warning.append(currentValue.getUnits());
      System.err.println(warning.toString());
    }
    
    EngineNumber outputWithUnits = new EngineNumber(newAmountBound, currentValue.getUnits());

    // Allow propagation but don't track units (since units tracking was handled by the caller)
    setStreamFor(stream, outputWithUnits, Optional.empty(), Optional.ofNullable(scope), true, Optional.empty());
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
   * @return A RecalcKit containing this engine's streamKeeper, unitConverter, and stateGetter
   */
  private RecalcKit createRecalcKit() {
    return new RecalcKitBuilder()
        .setStreamKeeper(streamKeeper)
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
    if (!"manufacture".equals(streamName) && !"import".equals(streamName)) {
      return;
    }

    // When setting manufacture or import, combine with the other to create sales intent
    String otherStream = "manufacture".equals(streamName) ? "import" : "manufacture";
    EngineNumber otherValue = streamKeeper.getLastSpecifiedValue(useKey, otherStream);
    
    if (otherValue != null && otherValue.hasEquipmentUnits()) {
      // Both streams have unit-based values - combine them
      // Convert both to the same units (prefer the current stream's units)
      String targetUnits = value.getUnits();
      UnitConverter converter = createUnitConverterWithTotal(streamName);
      EngineNumber otherConverted = converter.convert(otherValue, targetUnits);
      
      // Create combined sales value
      BigDecimal combinedValue = value.getValue().add(otherConverted.getValue());
      EngineNumber salesIntent = new EngineNumber(combinedValue, targetUnits);
      
      // Track the combined sales intent
      streamKeeper.setLastSpecifiedValue(useKey, "sales", salesIntent);
    } else {
      // Only one stream has units - use it as the sales intent
      streamKeeper.setLastSpecifiedValue(useKey, "sales", value);
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
    return !streamKeeper.isSalesIntentFreshlySet(scope) 
           && streamKeeper.hasLastSpecifiedValue(scope, "sales")
           && streamKeeper.getLastSpecifiedValue(scope, "sales").hasEquipmentUnits();
  }

  /**
   * Calculate the available recycling volume for the current timestep.
   * This method replicates the recycling calculation logic to determine
   * how much recycling material is available to avoid double counting.
   *
   * @param scope the scope to calculate recycling for
   * @return the amount of recycling available in kg
   */
  private BigDecimal calculateAvailableRecycling(UseKey scope) {
    try {
      // Get current prior population
      EngineNumber priorPopulationRaw = streamKeeper.getStream(scope, "priorEquipment");
      if (priorPopulationRaw == null) {
        return BigDecimal.ZERO;
      }
      
      // Get rates from parameterization
      EngineNumber retirementRate = streamKeeper.getRetirementRate(scope);
      EngineNumber recoveryRate = streamKeeper.getRecoveryRate(scope);
      EngineNumber yieldRate = streamKeeper.getYieldRate(scope);
      EngineNumber displacementRate = streamKeeper.getDisplacementRate(scope);
      
      // Convert everything to proper units
      UnitConverter unitConverter = createUnitConverterWithTotal("sales");
      EngineNumber priorPopulation = unitConverter.convert(priorPopulationRaw, "units");
      
      // Calculate rates as decimals
      BigDecimal retirementRateDecimal = retirementRate.getValue().divide(BigDecimal.valueOf(100));
      BigDecimal recoveryRateDecimal = recoveryRate.getValue().divide(BigDecimal.valueOf(100));
      BigDecimal yieldRateDecimal = yieldRate.getValue().divide(BigDecimal.valueOf(100));
      BigDecimal displacementRateDecimal = displacementRate.getValue().divide(BigDecimal.valueOf(100));
      
      // Calculate recycling chain
      BigDecimal retiredUnits = priorPopulation.getValue().multiply(retirementRateDecimal);
      BigDecimal recoveredUnits = retiredUnits.multiply(recoveryRateDecimal);
      BigDecimal recycledUnits = recoveredUnits.multiply(yieldRateDecimal);
      
      // Convert to kg
      EngineNumber initialCharge = streamKeeper.getInitialCharge(scope, "import");
      EngineNumber initialChargeKg = unitConverter.convert(initialCharge, "kg / unit");
      BigDecimal recycledKg = recycledUnits.multiply(initialChargeKg.getValue());
      
      // Apply displacement rate
      BigDecimal recyclingAvailable = recycledKg.multiply(displacementRateDecimal);
      
      return recyclingAvailable;
    } catch (Exception e) {
      // If any error occurs, return 0 to avoid breaking the flow
      return BigDecimal.ZERO;
    }
  }

}
