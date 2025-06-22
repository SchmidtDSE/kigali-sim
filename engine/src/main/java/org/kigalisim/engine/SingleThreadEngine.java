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
import java.util.Set;
import java.util.stream.Collectors;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.recalc.ConsumptionRecalcStrategy;
import org.kigalisim.engine.recalc.EolEmissionsRecalcStrategy;
import org.kigalisim.engine.recalc.PopulationChangeRecalcStrategy;
import org.kigalisim.engine.recalc.RecalcKit;
import org.kigalisim.engine.recalc.RecalcKitBuilder;
import org.kigalisim.engine.recalc.RecalcOperation;
import org.kigalisim.engine.recalc.RecalcOperationBuilder;
import org.kigalisim.engine.recalc.RechargeEmissionsRecalcStrategy;
import org.kigalisim.engine.recalc.RetireRecalcStrategy;
import org.kigalisim.engine.recalc.SalesRecalcStrategy;
import org.kigalisim.engine.serializer.EngineResult;
import org.kigalisim.engine.serializer.EngineResultSerializer;
import org.kigalisim.engine.state.ConverterStateGetter;
import org.kigalisim.engine.state.OverridingConverterStateGetter;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.engine.state.StreamKeeper;
import org.kigalisim.engine.state.SubstanceInApplicationId;
import org.kigalisim.engine.state.YearMatcher;
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
    currentYear = startYear;

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
    String application = scope.getApplication();

    if (checkValidEffective) {
      boolean knownSubstance = streamKeeper.hasSubstance(application, newSubstance);
      if (!knownSubstance) {
        throw new RuntimeException("Tried accessing unknown app / substance pair: "
            + application + ", " + newSubstance);
      }
    } else {
      streamKeeper.ensureSubstance(application, newSubstance);
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
  public void setStream(String name, EngineNumber value, YearMatcher yearMatcher, Scope scope,
      boolean propagateChanges, String unitsToRecord) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    Scope scopeEffective = scope != null ? scope : this.scope;
    String application = scopeEffective.getApplicationOptional().orElse(null);
    String substance = scopeEffective.getSubstanceOptional().orElse(null);

    if (application == null || substance == null) {
      raiseNoAppOrSubstance("setting stream", " specified");
    }

    streamKeeper.setStream(application, substance, name, value);

    // Track the units last used to specify this stream (only for user-initiated calls)
    if (!propagateChanges) {
      return;
    }

    String unitsToRecordRealized = unitsToRecord != null ? unitsToRecord : value.getUnits();
    streamKeeper.setLastSpecifiedUnits(application, substance, unitsToRecordRealized);

    if ("sales".equals(name) || "manufacture".equals(name) || "import".equals(name)) {
      RecalcOperationBuilder builder = new RecalcOperationBuilder()
          .setScopeEffective(scopeEffective)
          .setSubtractRecharge(!value.hasEquipmentUnits())
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
          .setScopeEffective(scopeEffective)
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
          .setScopeEffective(scopeEffective)
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
          .setScopeEffective(scopeEffective)
          .setRecalcKit(createRecalcKit())
          .recalcRetire()
          .build();
      operation.execute(this);
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
    EngineNumber value = streamKeeper.getStream(application, substance, name);

    if (conversion == null) {
      return value;
    } else {
      return unitConverter.convert(value, conversion);
    }
  }

  @Override
  public EngineNumber getStream(String name) {
    return getStream(name, null, null);
  }

  @Override
  public EngineNumber getStreamRaw(String application, String substance, String stream) {
    return streamKeeper.getStream(application, substance, stream);
  }

  @Override
  public EngineNumber getGhgIntensity(String application, String substance) {
    return streamKeeper.getGhgIntensity(application, substance);
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
      String application = scope.getApplication();
      String substance = scope.getSubstance();

      // Get manufacture and import values
      EngineNumber manufactureRaw = getStream("manufacture");
      EngineNumber manufactureValue = unitConverter.convert(manufactureRaw, "kg");

      if (manufactureValue.getValue().compareTo(BigDecimal.ZERO) == 0) {
        return getRawInitialChargeFor(application, substance, "import");
      }

      EngineNumber importRaw = getStream("import");
      EngineNumber importValue = unitConverter.convert(importRaw, "kg");

      if (importValue.getValue().compareTo(BigDecimal.ZERO) == 0) {
        return getRawInitialChargeFor(application, substance, "manufacture");
      }

      // Determine total
      boolean emptyStreams = isEmptyStreams(manufactureValue, importValue);

      // Get initial charges
      EngineNumber manufactureInitialChargeRaw = getRawInitialChargeFor(application, substance, "manufacture");
      EngineNumber manufactureInitialChargeUnbounded = unitConverter.convert(
          manufactureInitialChargeRaw, "kg / unit");

      EngineNumber importInitialChargeRaw = getRawInitialChargeFor(application, substance, "import");
      EngineNumber importInitialChargeUnbounded = unitConverter.convert(
          importInitialChargeRaw, "kg / unit");

      // Get bounded values
      EngineNumber manufactureInitialCharge = manufactureInitialChargeUnbounded.getValue().compareTo(BigDecimal.ZERO) == 0 
          ? importInitialChargeUnbounded 
          : manufactureInitialChargeUnbounded;

      EngineNumber importInitialCharge = importInitialChargeUnbounded.getValue().compareTo(BigDecimal.ZERO) == 0 
          ? manufactureInitialChargeUnbounded 
          : importInitialChargeUnbounded;

      // Calculate units
      BigDecimal manufactureKg = emptyStreams ? BigDecimal.ONE : manufactureValue.getValue();
      BigDecimal importKg = emptyStreams ? BigDecimal.ONE : importValue.getValue();
      BigDecimal manufactureKgUnit = manufactureInitialCharge.getValue();
      BigDecimal importKgUnit = importInitialCharge.getValue();

      BigDecimal manufactureUnits = manufactureKgUnit.compareTo(BigDecimal.ZERO) == 0 
          ? BigDecimal.ZERO 
          : manufactureKg.divide(manufactureKgUnit, MathContext.DECIMAL128);

      BigDecimal importUnits = importKgUnit.compareTo(BigDecimal.ZERO) == 0 
          ? BigDecimal.ZERO 
          : importKg.divide(importKgUnit, MathContext.DECIMAL128);

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
      String application = scope.getApplication();
      String substance = scope.getSubstance();
      return getRawInitialChargeFor(application, substance, stream);
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
  public EngineNumber getRawInitialChargeFor(String application, String substance, String stream) {
    return streamKeeper.getInitialCharge(application, substance, stream);
  }

  @Override
  public void setInitialCharge(EngineNumber value, String stream, YearMatcher yearMatcher) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    String application = scope.getApplication();
    String substance = scope.getSubstance();

    if ("sales".equals(stream)) {
      // For sales, set both manufacture and import but don't recalculate yet
      streamKeeper.setInitialCharge(application, substance, "manufacture", value);
      streamKeeper.setInitialCharge(application, substance, "import", value);
    } else {
      streamKeeper.setInitialCharge(application, substance, stream, value);
    }

    boolean subtractRecharge = getShouldSubtractRecharge(stream);
    RecalcOperation operation = new RecalcOperationBuilder()
        .setSubtractRecharge(subtractRecharge)
        .setRecalcKit(createRecalcKit())
        .recalcPopulationChange()
        .build();
    operation.execute(this);
  }

  /**
   * Determine if recharge should be subtracted based on last specified units.
   *
   * @param stream The stream being set
   * @return true if recharge should be subtracted, false if added on top
   */
  private boolean getShouldSubtractRecharge(String stream) {
    String application = scope.getApplication();
    String substance = scope.getSubstance();

    if ("sales".equals(stream)) {
      // For sales, check if either manufacture or import were last specified in units
      String lastUnits = streamKeeper.getLastSpecifiedUnits(application, substance);
      if (lastUnits != null && lastUnits.startsWith("unit")) {
        return false; // Add recharge on top
      }
    } else if ("manufacture".equals(stream) || "import".equals(stream)) {
      // For manufacture or import, check if that specific channel was last specified in units
      String lastUnits = streamKeeper.getLastSpecifiedUnits(application, substance);
      return lastUnits == null || !lastUnits.startsWith("unit"); // Add recharge on top
    }

    return true;
  }

  @Override
  public EngineNumber getRechargeVolume() {
    String application = scope.getApplication();
    String substance = scope.getSubstance();
    return streamKeeper.getRechargePopulation(application, substance);
  }

  @Override
  public EngineNumber getRechargeIntensity() {
    String application = scope.getApplication();
    String substance = scope.getSubstance();
    return getRechargeIntensityFor(application, substance);
  }

  @Override
  public EngineNumber getRechargeIntensityFor(String application, String substance) {
    return streamKeeper.getRechargeIntensity(application, substance);
  }

  @Override
  public String getLastSpecifiedUnits(String stream) {
    String application = scope.getApplication();
    String substance = scope.getSubstance();
    return getLastSpecifiedInUnits(application, substance, stream);
  }

  @Override
  public String getLastSpecifiedInUnits(String application, String substance, String stream) {
    return streamKeeper.getLastSpecifiedUnits(application, substance);
  }

  @Override
  public void setLastSpecifiedUnits(String stream, String units) {
    String application = scope.getApplication();
    String substance = scope.getSubstance();

    if (application == null || substance == null) {
      raiseNoAppOrSubstance("setting last specified units", " specified");
    }

    streamKeeper.setLastSpecifiedUnits(application, substance, units);
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
    streamKeeper.setRechargePopulation(application, substance, volume);
    streamKeeper.setRechargeIntensity(application, substance, intensity);

    // Recalculate
    RecalcOperation operation = new RecalcOperationBuilder()
        .setRecalcKit(createRecalcKit())
        .recalcPopulationChange()
        .thenPropagateToSales()
        .thenPropagateToConsumption()
        .build();
    operation.execute(this);
  }

  @Override
  public void retire(EngineNumber amount, YearMatcher yearMatcher) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    String application = scope.getApplication();
    String substance = scope.getSubstance();
    streamKeeper.setRetirementRate(application, substance, amount);
    RecalcOperation operation = new RecalcOperationBuilder()
        .setRecalcKit(createRecalcKit())
        .recalcRetire()
        .build();
    operation.execute(this);
  }

  @Override
  public EngineNumber getRetirementRate() {
    String application = scope.getApplication();
    String substance = scope.getSubstance();
    return streamKeeper.getRetirementRate(application, substance);
  }

  @Override
  public void recycle(EngineNumber recoveryWithUnits, EngineNumber yieldWithUnits,
      EngineNumber displaceLevel, YearMatcher yearMatcher) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    String application = scope.getApplication();
    String substance = scope.getSubstance();
    streamKeeper.setRecoveryRate(application, substance, recoveryWithUnits);
    streamKeeper.setYieldRate(application, substance, yieldWithUnits);

    if (displaceLevel != null) {
      streamKeeper.setDisplacementRate(application, substance, displaceLevel);
    }

    RecalcOperation operation = new RecalcOperationBuilder()
        .setRecalcKit(createRecalcKit())
        .recalcSales()
        .thenPropagateToPopulationChange()
        .thenPropagateToConsumption()
        .build();
    operation.execute(this);
  }

  @Override
  public void equals(EngineNumber amount, YearMatcher yearMatcher) {
    if (!getIsInRange(yearMatcher)) {
      return;
    }

    String application = scope.getApplication();
    String substance = scope.getSubstance();

    String units = amount.getUnits();
    boolean isGhg = units.startsWith("tCO2e");
    boolean isKwh = units.startsWith("kwh");

    if (isGhg) {
      streamKeeper.setGhgIntensity(application, substance, amount);
      RecalcOperation operation = new RecalcOperationBuilder()
          .setScopeEffective(scope)
          .setRecalcKit(createRecalcKit())
          .recalcRechargeEmissions()
          .thenPropagateToEolEmissions()
          .build();
      operation.execute(this);
    } else if (isKwh) {
      streamKeeper.setEnergyIntensity(application, substance, amount);
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
  public EngineNumber getEqualsGhgIntensity() {
    String application = scope.getApplication();
    String substance = scope.getSubstance();
    return getEqualsGhgIntensityFor(application, substance);
  }

  @Override
  public EngineNumber getEqualsGhgIntensityFor(String application, String substance) {
    return streamKeeper.getGhgIntensity(application, substance);
  }

  @Override
  public EngineNumber getEqualsEnergyIntensity() {
    String application = scope.getApplication();
    String substance = scope.getSubstance();
    return getEqualsEnergyIntensityFor(application, substance);
  }

  @Override
  public EngineNumber getEqualsEnergyIntensityFor(String application, String substance) {
    return streamKeeper.getEnergyIntensity(application, substance);
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
      EngineNumber rechargeVolume = RechargeVolumeCalculator.calculateRechargeVolume(
          scope,
          stateGetter,
          streamKeeper,
          this
      );
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
    streamKeeper.setLastSpecifiedUnits(application, substance, amount.getUnits());

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
      EngineNumber rechargeVolume = RechargeVolumeCalculator.calculateRechargeVolume(
          scope,
          stateGetter,
          streamKeeper,
          this
      );
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
    streamKeeper.setLastSpecifiedUnits(application, substance, amount.getUnits());

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
    Scope currentScope = scope;
    String application = currentScope.getApplication();
    String currentSubstance = currentScope.getSubstance();
    if (application == null || currentSubstance == null) {
      raiseNoAppOrSubstance("setting stream", " specified");
    }
    streamKeeper.setLastSpecifiedUnits(application, currentSubstance, amountRaw.getUnits());

    // Track the original user-specified units for the destination substance
    String lastUnits = amountRaw.getUnits();
    streamKeeper.setLastSpecifiedUnits(application, destinationSubstance, lastUnits);

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
    return yearMatcher == null || yearMatcher.getInRange(currentYear);
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
    String application = scope.getApplication();
    String substance = scope.getSubstance();

    if (application == null || substance == null) {
      raiseNoAppOrSubstance("calculating recharge volume", "");
    }

    // Get prior population for recharge calculation
    EngineNumber priorPopulationRaw = getStream("priorEquipment");
    EngineNumber priorPopulation = unitConverter.convert(priorPopulationRaw, "units");

    // Get recharge population
    stateGetter.setPopulation(getStream("priorEquipment"));
    EngineNumber rechargePopRaw = streamKeeper.getRechargePopulation(application, substance);
    EngineNumber rechargePop = unitConverter.convert(rechargePopRaw, "units");
    stateGetter.clearPopulation();

    // Switch to recharge population
    stateGetter.setPopulation(rechargePop);

    // Get recharge amount
    EngineNumber rechargeIntensityRaw = streamKeeper.getRechargeIntensity(
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
      Scope currentScope = scope;
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
        Scope destinationScope = scope.getWithSubstance(displaceTarget);

        // Temporarily change scope to destination for unit conversion
        Scope originalScope = scope;
        scope = destinationScope;
        UnitConverter destinationUnitConverter = createUnitConverterWithTotal(stream);
        scope = originalScope;

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
        Scope destinationScope = scope.getWithSubstance(displaceTarget);
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
    // Delegate to strategy with RecalcKit
    PopulationChangeRecalcStrategy strategy = new PopulationChangeRecalcStrategy(
        scope,
        subtractRecharge
    );
    strategy.execute(this, createRecalcKit());
  }

  /**
   * Recalculate the emissions that are accounted for at time of recharge.
   *
   * @param scope The scope in which to set the recharge emissions
   */
  private void recalcRechargeEmissions(Scope scope) {
    // Delegate to strategy with RecalcKit
    RechargeEmissionsRecalcStrategy strategy = new RechargeEmissionsRecalcStrategy(scope);
    strategy.execute(this, createRecalcKit());
  }

  /**
   * Recalculate emissions realized at the end of life for a unit.
   *
   * @param scope The scope in which to recalculate EOL emissions
   */
  private void recalcEolEmissions(Scope scope) {
    // Delegate to strategy with RecalcKit
    EolEmissionsRecalcStrategy strategy = new EolEmissionsRecalcStrategy(scope);
    strategy.execute(this, createRecalcKit());
  }

  /**
   * Recalculates consumption values based on current state.
   *
   * @param scope The scope to recalculate for
   */
  private void recalcConsumption(Scope scope) {
    // Delegate to strategy with RecalcKit
    ConsumptionRecalcStrategy strategy = new ConsumptionRecalcStrategy(scope);
    strategy.execute(this, createRecalcKit());
  }

  /**
   * Recalculates sales values based on current state.
   *
   * @param scope The scope to recalculate for
   */
  private void recalcSales(Scope scope) {
    // Delegate to strategy with RecalcKit
    SalesRecalcStrategy strategy = new SalesRecalcStrategy(scope);
    strategy.execute(this, createRecalcKit());
  }

  /**
   * Recalculates retirement values based on current state.
   *
   * @param scope The scope to recalculate for
   */
  private void recalcRetire(Scope scope) {
    // Delegate to strategy with RecalcKit
    RetireRecalcStrategy strategy = new RetireRecalcStrategy(scope);
    strategy.execute(this, createRecalcKit());
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
   * Helper method to get the application from the current scope.
   *
   * @return The application or null if not set
   */
  private String getApplicationFromScope() {
    return scope.getApplication();
  }

  /**
   * Helper method to get the application from the given scope.
   *
   * @param scope The scope to get the application from
   * @return The application or null if not set
   */
  private String getApplicationFromScope(Scope scope) {
    return scope.getApplication();
  }

  /**
   * Helper method to get the substance from the current scope.
   *
   * @return The substance or null if not set
   */
  private String getSubstanceFromScope() {
    return scope.getSubstance();
  }

  /**
   * Helper method to get the substance from the given scope.
   *
   * @param scope The scope to get the substance from
   * @return The substance or null if not set
   */
  private String getSubstanceFromScope(Scope scope) {
    return scope.getSubstance();
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
}
