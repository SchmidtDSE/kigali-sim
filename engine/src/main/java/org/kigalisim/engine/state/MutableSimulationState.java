/**
 * Live, mutable state for a single scenario within a single trial.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.recalc.SalesStreamDistribution;
import org.kigalisim.engine.recalc.SalesStreamDistributionBuilder;
import org.kigalisim.lang.operation.RecoverOperation.RecoveryStage;

/**
 * Live, mutable manager for the state for a single scenario within a single trial.
 *
 * <p>State management object for storage and retrieval of substance data, stream
 * values, and associated parameterizations.</p>
 */
public class MutableSimulationState implements SimulationState {

  private static final boolean CHECK_NAN_STATE = false;
  private static final BigDecimal BASE_CHANGE_TOLERANCE = new BigDecimal("0.0001");
  private static final BigDecimal HUNDRED_PERCENT = BigDecimal.valueOf(100);

  private final Map<String, StreamParameterization> substances;
  private final Map<String, EngineNumber> streams;
  private Optional<SimulationState> priorState;
  private final OverridingConverterStateGetter stateGetter;
  private final UnitConverter unitConverter;
  private int currentYear;

  /**
   * Create a new MutableSimulationState instance.
   *
   * @param stateGetter Structure to retrieve state information
   * @param unitConverter Converter for handling unit transformations
   */
  public MutableSimulationState(OverridingConverterStateGetter stateGetter,
      UnitConverter unitConverter) {
    this.substances = new HashMap<>();
    this.streams = new HashMap<>();
    this.priorState = Optional.empty();
    this.stateGetter = stateGetter;
    this.unitConverter = unitConverter;
  }

  /** {@inheritDoc} */
  @Override
  public List<SubstanceInApplicationId> getRegisteredSubstances() {
    return substances.keySet().stream()
        .map(key -> {
          String[] keyComponents = key.split("\t");
          boolean correctKeyLength = keyComponents.length == 2;
          if (!correctKeyLength) {
            return null;
          }

          boolean firstKeyOk = keyComponents[0] != null && !keyComponents[0].trim().isEmpty();
          boolean secondKeyOk = keyComponents[1] != null && !keyComponents[1].trim().isEmpty();
          boolean bothKeysOk = firstKeyOk && secondKeyOk;
          if (!bothKeysOk) {
            return null;
          }

          return new SubstanceInApplicationId(keyComponents[0], keyComponents[1]);
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasSubstance(UseKey useKey) {
    String key = getKey(useKey);
    return substances.containsKey(key);
  }

  /** {@inheritDoc} */
  @Override
  public void ensureSubstance(UseKey useKey) {
    String key = getKey(useKey);

    if (substances.containsKey(key)) {
      return;
    }

    StreamParameterization parameterization = new MutableStreamParameterization();
    substances.put(key, parameterization);

    ensureSubstanceSales(useKey);
    ensureSubstanceConsumption(useKey);
    ensureSubstancePopulation(useKey);
    ensureSubstanceEmissions(useKey);
    ensureSubstanceRecharge(useKey);
    ensureSubstanceAge(useKey);
  }

  /**
   * Ensure sales streams are initialized.
   *
   * <p>Initializes domestic, import, export, and recycle streams (split into recycleRecharge and
   * recycleEol) along with induction streams (inductionEol and inductionRecharge).</p>
   *
   * @param useKey The key containing application and substance.
   */
  private void ensureSubstanceSales(UseKey useKey) {
    String domesticKey = getKey(useKey, "domestic");
    streams.put(domesticKey, ZERO_VOLUME);

    String importKey = getKey(useKey, "import");
    streams.put(importKey, ZERO_VOLUME);

    String exportKey = getKey(useKey, "export");
    streams.put(exportKey, ZERO_VOLUME);

    String recycleRechargeKey = getKey(useKey, "recycleRecharge");
    streams.put(recycleRechargeKey, new EngineNumber(BigDecimal.ZERO, "kg"));

    String recycleEolKey = getKey(useKey, "recycleEol");
    streams.put(recycleEolKey, new EngineNumber(BigDecimal.ZERO, "kg"));

    String inductionEolKey = getKey(useKey, "inductionEol");
    streams.put(inductionEolKey, new EngineNumber(BigDecimal.ZERO, "kg"));

    String inductionRechargeKey = getKey(useKey, "inductionRecharge");
    streams.put(inductionRechargeKey, new EngineNumber(BigDecimal.ZERO, "kg"));
  }

  /**
   * Ensure consumption streams are initialized.
   *
   * <p>Initializes the consumption stream used for tracking greenhouse gas impacts, defaulting to
   * 0 tCO2e.</p>
   *
   * @param useKey The key containing application and substance
   */
  private void ensureSubstanceConsumption(UseKey useKey) {
    String consumptionKey = getKey(useKey, "consumption");
    streams.put(consumptionKey, new EngineNumber(BigDecimal.ZERO, "tCO2e"));
  }

  /**
   * Ensure population streams are initialized.
   *
   * <p>Initializes equipment, priorEquipment, newEquipment, retired, and priorRetired streams
   * for tracking equipment populations and their lifecycle transitions.</p>
   *
   * @param useKey The key containing application and substance
   */
  private void ensureSubstancePopulation(UseKey useKey) {
    String equipmentKey = getKey(useKey, "equipment");
    streams.put(equipmentKey, new EngineNumber(BigDecimal.ZERO, "units"));

    String priorEquipmentKey = getKey(useKey, "priorEquipment");
    streams.put(priorEquipmentKey, new EngineNumber(BigDecimal.ZERO, "units"));

    String newEquipmentKey = getKey(useKey, "newEquipment");
    streams.put(newEquipmentKey, new EngineNumber(BigDecimal.ZERO, "units"));

    String retiredKey = getKey(useKey, "retired");
    streams.put(retiredKey, new EngineNumber(BigDecimal.ZERO, "units"));

    String priorRetiredKey = getKey(useKey, "priorRetired");
    streams.put(priorRetiredKey, new EngineNumber(BigDecimal.ZERO, "units"));
  }

  /**
   * Ensure emissions streams are initialized.
   *
   * <p>Initializes rechargeEmissions and eolEmissions streams for tracking emissions
   * from recharge and end-of-life recovery operations.</p>
   *
   * @param useKey The key containing application and substance
   */
  private void ensureSubstanceEmissions(UseKey useKey) {
    String rechargeEmissionsKey = getKey(useKey, "rechargeEmissions");
    streams.put(rechargeEmissionsKey, new EngineNumber(BigDecimal.ZERO, "tCO2e"));

    String eolEmissionsKey = getKey(useKey, "eolEmissions");
    streams.put(eolEmissionsKey, new EngineNumber(BigDecimal.ZERO, "tCO2e"));
  }

  /**
   * Ensure recharge tracking streams are initialized.
   *
   * <p>Initializes the implicitRecharge stream for tracking accumulated recharge amounts.</p>
   *
   * @param useKey The key containing application and substance
   */
  private void ensureSubstanceRecharge(UseKey useKey) {
    String implicitRechargeKey = getKey(useKey, "implicitRecharge");
    streams.put(implicitRechargeKey, new EngineNumber(BigDecimal.ZERO, "kg"));

    String implicitPrechargeKey = getKey(useKey, "implicitPrecharge");
    streams.put(implicitPrechargeKey, new EngineNumber(BigDecimal.ZERO, "kg"));
  }

  /**
   * Ensure age tracking streams are initialized.
   *
   * <p>Initializes the age stream for tracking the weighted average age of equipment.</p>
   *
   * @param useKey The key containing application and substance
   */
  private void ensureSubstanceAge(UseKey useKey) {
    String ageKey = getKey(useKey, "age");
    streams.put(ageKey, new EngineNumber(BigDecimal.ZERO, "years"));
  }

  /** {@inheritDoc} */
  @Override
  public void update(SimulationStateUpdate stateUpdate) {
    UseKey useKey = stateUpdate.getUseKey();
    String name = stateUpdate.getName();
    EngineNumber value = stateUpdate.getValue();

    String key = getKey(useKey);
    ensureSubstanceOrThrow(key, "update(SimulationStateUpdate)");
    ensureStreamKnown(name);

    // Check if stream needs to be enabled before setting
    assertStreamEnabled(useKey, name, value);

    // Conditionally invalidate bases if priorEquipment manually modified
    if (stateUpdate.getInvalidatesPriorEquipment()) {
      updatePriorEquipmentBase(useKey, name, value);
    }

    if (CHECK_NAN_STATE && value.getValue().toString().equals("NaN")) {
      String[] keyPieces = key.split("\t");
      String application = keyPieces.length > 0 ? keyPieces[0] : "";
      String substance = keyPieces.length > 1 ? keyPieces[1] : "";
      String pieces = String.join(
          " > ",
          "-".equals(application) ? "null" : application,
          "-".equals(substance) ? "null" : substance,
          name
      );
      throw new RuntimeException("Encountered NaN to be set for: " + pieces);
    }

    // Extract routing parameters when needed
    final boolean subtractRecycling = stateUpdate.getSubtractRecycling();
    final Optional<SalesStreamDistribution> distribution = stateUpdate.getDistribution();

    boolean isUsedSalesSubstream = "domestic".equals(name) || "import".equals(name);
    boolean isSimpleSet = !subtractRecycling && isUsedSalesSubstream;

    if (isSimpleSet) {
      setSimpleStream(useKey, name, value);
      return;
    }

    // Route based on stream type using a new-style switch
    switch (name) {
      case "sales" -> setStreamForSales(useKey, name, value);
      case "virgin" -> setStreamForVirgin(useKey, name, value);
      case "domestic", "import" -> setStreamSalesSubstream(useKey, name, value, distribution);
      case "recycle" -> setStreamForRecycle(useKey, name, value);
      default -> {
        if (getIsSettingVolumeByUnits(name, value)) {
          setStreamForSalesWithUnits(useKey, name, value);
        } else {
          setSimpleStream(useKey, name, value);
        }
      }
    }
  }

  /**
   * Set a sales substream (domestic or import) with recycling displacement logic.
   *
   * <p>This method handles individual sales streams while applying proportional recycling
   * displacement based on distribution percentages. It consolidates the sales substream logic that
   * was previously inlined in the main setStream method.</p>
   *
   * @param useKey The key containing application and substance
   * @param name The stream name ("domestic" or "import")
   * @param value The total value for this specific sales stream
   * @param distribution Optional pre-calculated distribution for recycling allocation
   */
  private void setStreamSalesSubstream(UseKey useKey, String name, EngineNumber value,
      Optional<SalesStreamDistribution> distribution) {
    EngineNumber valueConverted = unitConverter.convert(value, "kg");
    final BigDecimal amountKg = valueConverted.getValue();

    // Check if any streams are enabled for distribution calculation
    if (!hasStreamsEnabled(useKey)) {
      throw new IllegalStateException("Cannot set sales stream: no streams have been enabled. "
          + "Use 'set " + name + "' or other stream statements to enable streams before "
          + "operations that require sales recalculation.");
    }

    // Get current recycling amount
    EngineNumber recycleAmountRaw = getStream(useKey, "recycle");
    EngineNumber recycleAmount = unitConverter.convert(recycleAmountRaw, "kg");
    BigDecimal recycleKg = recycleAmount != null ? recycleAmount.getValue() : BigDecimal.ZERO;

    // Determine distribution to use
    SalesStreamDistribution streamDistribution;
    if (distribution.isPresent()) {
      streamDistribution = distribution.get();
    } else {
      streamDistribution = getDistribution(useKey);
    }

    // Use distribution to determine this sales stream's share of recycling
    BigDecimal substreamPercent;
    if ("domestic".equals(name)) {
      substreamPercent = streamDistribution.getPercentDomestic();
    } else {
      substreamPercent = streamDistribution.getPercentImport();
    }

    // Calculate proportional recycling for this sales stream
    BigDecimal substreamRecycling = recycleKg.multiply(substreamPercent);

    // Subtract proportional recycling to get virgin material amount
    BigDecimal netAmount = amountKg.subtract(substreamRecycling);
    if (netAmount.compareTo(BigDecimal.ZERO) < 0) {
      netAmount = BigDecimal.ZERO;
    }

    // Set the net amount directly
    EngineNumber netAmountToSet = new EngineNumber(netAmount, "kg");
    setSimpleStream(useKey, name, netAmountToSet);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getStream(UseKey useKey, String name) {
    return getStream(useKey, name, false);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getStream(UseKey useKey, String name, boolean priorYear) {
    return SimulationStateSupport.getStream(
        this, streams, priorState, unitConverter, useKey, name, priorYear);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isKnownStream(UseKey useKey, String name) {
    return streams.containsKey(getKey(useKey, name));
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getInductionStream(UseKey useKey, RecoveryStage stage) {
    return getInductionStream(useKey, stage, false);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getInductionStream(UseKey useKey, RecoveryStage stage, boolean priorYear) {
    String streamName = getInductionStreamName(stage);
    return getStream(useKey, streamName, priorYear);
  }

  /**
   * Set the induction stream value for a specific recovery stage.
   *
   * @param useKey The key containing application and substance
   * @param stage The recovery stage (EOL or RECHARGE)
   * @param value The induction value in kg
   */
  private void setInductionStream(UseKey useKey, RecoveryStage stage, EngineNumber value) {
    String streamName = getInductionStreamName(stage);
    String key = getKey(useKey);
    ensureSubstanceOrThrow(key, "setInductionStream");
    ensureStreamKnown(streamName);
    assertStreamEnabled(useKey, streamName, value);
    setSimpleStream(useKey, streamName, value);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getTotalInductionStream(UseKey useKey) {
    return getTotalInductionStream(useKey, false);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getTotalInductionStream(UseKey useKey, boolean priorYear) {
    EngineNumber inductionEol = getInductionStream(useKey, RecoveryStage.EOL, priorYear);
    EngineNumber inductionRecharge = getInductionStream(useKey, RecoveryStage.RECHARGE, priorYear);

    EngineNumber eolConverted = unitConverter.convert(inductionEol, "kg");
    EngineNumber rechargeConverted = unitConverter.convert(inductionRecharge, "kg");

    BigDecimal total = eolConverted.getValue().add(rechargeConverted.getValue());
    return new EngineNumber(total, "kg");
  }

  /**
   * Get the stream name for an induction stage.
   *
   * @param stage The recovery stage
   * @return The corresponding stream name
   */
  private String getInductionStreamName(RecoveryStage stage) {
    return SimulationStateSupport.getInductionStreamName(stage);
  }

  /** {@inheritDoc} */
  @Override
  public SalesStreamDistribution getDistribution(UseKey useKey) {
    return getDistribution(useKey, false);
  }

  /** {@inheritDoc} */
  @Override
  public SalesStreamDistribution getDistribution(UseKey useKey, boolean includeExports) {
    EngineNumber domesticValueRaw = getStream(useKey, "domestic");
    EngineNumber importValueRaw = getStream(useKey, "import");
    EngineNumber exportValueRaw = getStream(useKey, "export");

    EngineNumber domesticValue = unitConverter.convert(domesticValueRaw, "kg");
    EngineNumber importValue = unitConverter.convert(importValueRaw, "kg");
    EngineNumber exportValue;
    if (exportValueRaw == null) {
      exportValue = new EngineNumber(BigDecimal.ZERO, "kg");
    } else {
      exportValue = unitConverter.convert(exportValueRaw, "kg");
    }

    boolean domesticEnabled = hasStreamBeenEnabled(useKey, "domestic");
    boolean importEnabled = hasStreamBeenEnabled(useKey, "import");
    boolean exportEnabled = hasStreamBeenEnabled(useKey, "export");

    return new SalesStreamDistributionBuilder()
        .setDomesticSales(domesticValue)
        .setImportSales(importValue)
        .setExportSales(exportValue)
        .setDomesticEnabled(domesticEnabled)
        .setImportEnabled(importEnabled)
        .setExportEnabled(exportEnabled)
        .setIncludeExports(includeExports)
        .build();
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasStreamsEnabled(UseKey useKey) {
    return hasStreamBeenEnabled(useKey, "domestic")
        || hasStreamBeenEnabled(useKey, "import")
        || hasStreamBeenEnabled(useKey, "export");
  }

  /** {@inheritDoc} */
  @Override
  public int getCurrentYear() {
    return currentYear;
  }

  /** {@inheritDoc} */
  @Override
  public void setCurrentYear(int year) {
    this.currentYear = year;
  }

  /** {@inheritDoc} */
  @Override
  public Optional<SimulationState> getAtPrior(int years) {
    if (years < 0) {
      return Optional.empty();
    } else if (years == 0) {
      return Optional.of(this);
    } else if (priorState.isEmpty()) {
      return Optional.empty();
    } else {
      return priorState.get().getAtPrior(years - 1);
    }
  }


  /**
   * {@inheritDoc}
   *
   * <p>The frozen snapshot captures {@code priorState} at freeze time, so the linked
   * list is already correctly formed without any further mutation of the snapshot.</p>
   */
  @Override
  public void incrementYear() {
    // Freeze the current state BEFORE modifications; the snapshot already
    // captures this.priorState, so the linked list needs no further re-linking.
    SimulationState priorCopy = this.freeze();
    advanceYearAndResetTimestepState();
    this.priorState = Optional.of(priorCopy);
  }

  /**
   * Advance the current year and perform year-end population and recycling bookkeeping.
   */
  private void advanceYearAndResetTimestepState() {
    currentYear += 1;

    for (String key : substances.keySet()) {
      String[] keyPieces = key.split("\t");
      String application = keyPieces[0];
      String substance = keyPieces[1];
      SimpleUseKey useKey = new SimpleUseKey(application, substance);
      updateStreamPopulation(useKey);
    }

    for (StreamParameterization parameterization : substances.values()) {
      parameterization.resetStateAtTimestep();
    }

    redistributeRecyclingToSales();
    redistributeInductionFromSales();

    for (String key : substances.keySet()) {
      String[] keyPieces = key.split("\t");
      String application = keyPieces[0];
      String substance = keyPieces[1];
      SimpleUseKey useKey = new SimpleUseKey(application, substance);
      resetStreamRecycling(useKey);
    }
  }

  /**
   * Update population and age streams for a substance-application pair.
   *
   * <p>This method performs year-end population bookkeeping by:
   * <ul>
   *   <li>Moving current equipment count to priorEquipment</li>
   *   <li>Moving current retired count to priorRetired</li>
   *   <li>Calculating weighted average age for the equipment population</li>
   * </ul></p>
   *
   * <p>The age calculation accounts for both existing equipment (aging one year)
   * and newly added equipment (starting at age 1), weighted by their respective
   * population sizes.</p>
   *
   * @param useKey The key identifying the substance-application pair
   */
  private void updateStreamPopulation(UseKey useKey) {
    EngineNumber equipment = getStream(useKey, "equipment");
    setSimpleStream(useKey, "priorEquipment", equipment);

    EngineNumber retired = getStream(useKey, "retired");
    setSimpleStream(useKey, "priorRetired", retired);

    EngineNumber priorEquipmentValue = getStream(useKey, "priorEquipment");
    EngineNumber currentEquipmentValue = getStream(useKey, "equipment");
    EngineNumber currentAge = getStream(useKey, "age");

    EngineNumber priorEquipmentUnits = unitConverter.convert(priorEquipmentValue, "units");
    EngineNumber currentEquipmentUnits = unitConverter.convert(currentEquipmentValue, "units");

    BigDecimal priorAgeWeight = priorEquipmentUnits.getValue();
    BigDecimal addedEquipment = currentEquipmentUnits.getValue().subtract(priorEquipmentUnits.getValue());
    BigDecimal addedAgeWeight = addedEquipment.max(BigDecimal.ZERO);

    BigDecimal priorAgeYears = currentAge.getValue().add(BigDecimal.ONE);
    BigDecimal priorAgeWeighted = priorAgeYears.multiply(priorAgeWeight);
    BigDecimal addedAgeWeighted = addedAgeWeight;

    BigDecimal totalWeight = priorAgeWeight.add(addedAgeWeight);
    boolean isZero = totalWeight.compareTo(BigDecimal.ZERO) == 0;
    BigDecimal newAge;
    if (isZero) {
      newAge = BigDecimal.ZERO;
    } else {
      newAge = priorAgeWeighted.add(addedAgeWeighted).divide(totalWeight, MathContext.DECIMAL128);
    }

    setSimpleStream(useKey, "age", new EngineNumber(newAge, "years"));
  }

  /**
   * Reset recycling and induction streams to zero for year boundary.
   *
   * <p>This method prevents stale recycling values from affecting subsequent
   * cap operations and cross-year accumulation by resetting both recycling
   * substreams (recycleRecharge and recycleEol) and induction substreams
   * (inductionEol and inductionRecharge) to zero.</p>
   *
   * @param useKey The key identifying the substance-application pair
   */
  private void resetStreamRecycling(UseKey useKey) {
    setSimpleStream(useKey, "recycleRecharge", new EngineNumber(BigDecimal.ZERO, "kg"));
    setSimpleStream(useKey, "recycleEol", new EngineNumber(BigDecimal.ZERO, "kg"));

    setSimpleStream(useKey, "inductionEol", new EngineNumber(BigDecimal.ZERO, "kg"));
    setSimpleStream(useKey, "inductionRecharge", new EngineNumber(BigDecimal.ZERO, "kg"));
  }

  /** {@inheritDoc} */
  @Override
  public void setGhgIntensity(UseKey useKey, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setGhgIntensity(newValue);
  }

  /** {@inheritDoc} */
  @Override
  public void setEnergyIntensity(UseKey useKey, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setEnergyIntensity(newValue);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getGhgIntensity(UseKey useKey) {
    String key = getKey(useKey);
    StreamParameterization parameterization = substances.get(key);
    if (parameterization == null) {
      throwSubstanceMissing(
          "getGhgIntensity",
          useKey.getApplication(),
          useKey.getSubstance()
      );
    }
    return parameterization.getGhgIntensity();
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getEnergyIntensity(UseKey useKey) {
    String key = getKey(useKey);
    StreamParameterization parameterization = substances.get(key);
    if (parameterization == null) {
      throwSubstanceMissing(
          "getEnergyIntensity",
          useKey.getApplication(),
          useKey.getSubstance()
      );
    }
    return parameterization.getEnergyIntensity();
  }

  /** {@inheritDoc} */
  @Override
  public void setInitialCharge(UseKey useKey, String substream, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setInitialCharge(substream, newValue);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getInitialCharge(UseKey useKey, String substream) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getInitialCharge(substream);
  }

  /** {@inheritDoc} */
  @Override
  public void setRechargePopulation(UseKey useKey, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setRechargePopulation(newValue);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getRechargePopulation(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getRechargePopulation();
  }

  /** {@inheritDoc} */
  @Override
  public void setRechargeIntensity(UseKey useKey, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setRechargeIntensity(newValue);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getRechargeIntensity(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getRechargeIntensity();
  }

  /** {@inheritDoc} */
  @Override
  public void accumulateRecharge(UseKey useKey, EngineNumber population, EngineNumber intensity) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.accumulateRecharge(population, intensity);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getRechargeBasePopulation(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getRechargeBasePopulation();
  }

  /** {@inheritDoc} */
  @Override
  public void setRechargeBasePopulation(UseKey useKey, EngineNumber value) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setRechargeBasePopulation(value);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getAppliedRechargeAmount(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getAppliedRechargeAmount();
  }

  /** {@inheritDoc} */
  @Override
  public void setAppliedRechargeAmount(UseKey useKey, EngineNumber value) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setAppliedRechargeAmount(value);
  }

  /** {@inheritDoc} */
  @Override
  public void setPrechargePopulation(UseKey useKey, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setPrechargePopulation(newValue);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getPrechargePopulation(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getPrechargePopulation();
  }

  /** {@inheritDoc} */
  @Override
  public void setPrechargeIntensity(UseKey useKey, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setPrechargeIntensity(newValue);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getPrechargeIntensity(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getPrechargeIntensity();
  }

  /** {@inheritDoc} */
  @Override
  public void accumulatePrecharge(UseKey useKey, EngineNumber population, EngineNumber intensity) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.accumulatePrecharge(population, intensity);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getPrechargeBasePopulation(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getPrechargeBasePopulation();
  }

  /** {@inheritDoc} */
  @Override
  public void setPrechargeBasePopulation(UseKey useKey, EngineNumber value) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setPrechargeBasePopulation(value);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getAppliedPrechargeAmount(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getAppliedPrechargeAmount();
  }

  /** {@inheritDoc} */
  @Override
  public void setAppliedPrechargeAmount(UseKey useKey, EngineNumber value) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setAppliedPrechargeAmount(value);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isRecyclingCalculatedThisStep(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.isRecyclingCalculatedThisStep();
  }

  /** {@inheritDoc} */
  @Override
  public void setRecyclingCalculatedThisStep(UseKey useKey, boolean calculated) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setRecyclingCalculatedThisStep(calculated);
  }

  /** {@inheritDoc} */
  @Override
  public void setRecoveryRate(UseKey useKey, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(useKey);
    EngineNumber existingRecovery = parameterization.getRecoveryRate();

    if (existingRecovery.getValue().compareTo(BigDecimal.ZERO) > 0) {
      EngineNumber existingRecoveryPercent = unitConverter.convert(existingRecovery, "%");
      EngineNumber newRecoveryPercent = unitConverter.convert(newValue, "%");
      BigDecimal combinedRecovery = existingRecoveryPercent.getValue().add(newRecoveryPercent.getValue());
      parameterization.setRecoveryRate(new EngineNumber(combinedRecovery, "%"));
    } else {
      parameterization.setRecoveryRate(newValue);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void setRecoveryRate(UseKey useKey, EngineNumber newValue, RecoveryStage stage) {
    StreamParameterization parameterization = getParameterization(useKey);
    EngineNumber existingRecovery = parameterization.getRecoveryRate(stage);

    if (existingRecovery.getValue().compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal newRate = existingRecovery.getValue().add(newValue.getValue());
      EngineNumber combinedRate = new EngineNumber(newRate, "%");
      parameterization.setRecoveryRate(combinedRate, stage);
      return;
    }

    parameterization.setRecoveryRate(newValue, stage);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getRecoveryRate(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getRecoveryRate();
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getRecoveryRate(UseKey useKey, RecoveryStage stage) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getRecoveryRate(stage);
  }


  /** {@inheritDoc} */
  @Override
  public void setYieldRate(UseKey useKey, EngineNumber newValue) {
    setYieldRate(useKey, newValue, RecoveryStage.RECHARGE);
  }

  /** {@inheritDoc} */
  @Override
  public void setYieldRate(UseKey useKey, EngineNumber newValue, RecoveryStage stage) {
    StreamParameterization parameterization = getParameterization(useKey);
    EngineNumber existingYield = parameterization.getYieldRate(stage);

    if (existingYield.getValue().compareTo(BigDecimal.ZERO) > 0) {
      EngineNumber existingYieldPercent = unitConverter.convert(existingYield, "%");
      EngineNumber newYieldPercent = unitConverter.convert(newValue, "%");

      BigDecimal combinedYield = existingYieldPercent.getValue()
          .add(newYieldPercent.getValue())
          .divide(
              BigDecimal.valueOf(2),
              java.math.MathContext.DECIMAL128
          );

      parameterization.setYieldRate(new EngineNumber(combinedYield, "%"), stage);
    } else {
      parameterization.setYieldRate(newValue, stage);
    }
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getYieldRate(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getYieldRate();
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getYieldRate(UseKey useKey, RecoveryStage stage) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getYieldRate(stage);
  }

  /** {@inheritDoc} */
  @Override
  public void setInductionRate(UseKey useKey, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setInductionRate(newValue);
  }

  /** {@inheritDoc} */
  @Override
  public void setInductionRate(UseKey useKey, EngineNumber newValue, RecoveryStage stage) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setInductionRate(newValue, stage);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getInductionRate(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getInductionRate();
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getInductionRate(UseKey useKey, RecoveryStage stage) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getInductionRate(stage);
  }

  /** {@inheritDoc} */
  @Override
  public void setRetirementRate(UseKey useKey, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setRetirementRate(newValue);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getRetirementRate(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getRetirementRate();
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getRetirementBasePopulation(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getRetirementBasePopulation();
  }

  /** {@inheritDoc} */
  @Override
  public void setRetirementBasePopulation(UseKey useKey, EngineNumber value) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setRetirementBasePopulation(value);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<EngineNumber> getAppliedRetirementAmount(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getAppliedRetirementAmount();
  }

  /** {@inheritDoc} */
  @Override
  public void setAppliedRetirementAmount(UseKey useKey, EngineNumber value) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setAppliedRetirementAmount(value);
  }

  /** {@inheritDoc} */
  @Override
  public boolean getHasReplacementThisStep(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getHasReplacementThisStep();
  }

  /** {@inheritDoc} */
  @Override
  public void setHasReplacementThisStep(UseKey useKey, boolean value) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setHasReplacementThisStep(value);
  }

  /** {@inheritDoc} */
  @Override
  public boolean getRetireCalculatedThisStep(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getRetireCalculatedThisStep();
  }

  /** {@inheritDoc} */
  @Override
  public void setRetireCalculatedThisStep(UseKey useKey, boolean calculated) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setRetireCalculatedThisStep(calculated);
  }

  /** {@inheritDoc} */
  @Override
  public void setLastSpecifiedValue(UseKey useKey, String streamName, EngineNumber value) {
    String key = getKey(useKey);
    StreamParameterization parameterization = substances.get(key);
    if (parameterization == null) {
      throwSubstanceMissing(
          "setLastSpecifiedValue",
          useKey.getApplication(),
          useKey.getSubstance()
      );
    }
    parameterization.setLastSpecifiedValue(streamName, value);
  }

  /** {@inheritDoc} */
  @Override
  public EngineNumber getLastSpecifiedValue(UseKey useKey, String streamName) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getLastSpecifiedValue(streamName);
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasLastSpecifiedValue(UseKey useKey, String streamName) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.hasLastSpecifiedValue(streamName);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isSalesIntentFreshlySet(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.isSalesIntentFreshlySet();
  }

  /** {@inheritDoc} */
  @Override
  public void resetSalesIntentFlag(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setSalesIntentFreshlySet(false);
  }


  /** {@inheritDoc} */
  @Override
  public boolean hasStreamBeenEnabled(UseKey useKey, String streamName) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.hasStreamBeenEnabled(streamName);
  }

  /** {@inheritDoc} */
  @Override
  public void markStreamAsEnabled(UseKey useKey, String streamName) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.markStreamAsEnabled(streamName);
  }

  /**
   * Retrieve parameterization for a specific key.
   *
   * <p>Verifies the existence of the substance and application combination
   * and returns the associated StreamParameterization object.</p>
   *
   * @param scope The key containing application and substance
   * @return The parameterization for the given key
   */
  private StreamParameterization getParameterization(UseKey scope) {
    return SimulationStateSupport.getParameterization(substances, scope);
  }

  /**
   * Generate a key for a UseKey.
   *
   * @param useKey The UseKey to generate a key for
   * @return The generated key
   */
  private String getKey(UseKey useKey) {
    return SimulationStateSupport.getKey(useKey);
  }

  /**
   * Generate a stream key for a Scope and stream name.
   *
   * @param useKey The Scope to generate a key for
   * @param name The stream name
   * @return The generated stream key
   */
  private String getKey(UseKey useKey, String name) {
    return SimulationStateSupport.getKey(useKey, name);
  }

  /**
   * Sets a simple stream by converting the provided value to the appropriate units and storing it in
   * the streams map with a key generated from the given parameters. If the converted value is NaN, an
   * exception is thrown indicating the source of the issue.
   *
   * @param useKey An instance of UseKey that helps determine stream-specific characteristics
   *               for generating the stream key.
   * @param name A string representing the name of the stream or parameter to be processed.
   * @param value An instance of EngineNumber that contains the numerical value to be converted
   *              and stored in the appropriate stream. Example: units might be kg.
   */
  private void setSimpleStream(UseKey useKey, String name, EngineNumber value) {
    String unitsNeeded = getUnits(name);
    EngineNumber valueConverted = unitConverter.convert(value, unitsNeeded);

    if (CHECK_NAN_STATE && valueConverted.getValue().toString().equals("NaN")) {
      String key = getKey(useKey);
      String[] keyPieces = key.split("\t");
      String application = keyPieces.length > 0 ? keyPieces[0] : "";
      String substance = keyPieces.length > 1 ? keyPieces[1] : "";
      String pieces = String.join(
          " > ",
          "-".equals(application) ? "null" : application,
          "-".equals(substance) ? "null" : substance,
          name
      );
      throw new RuntimeException("Encountered NaN after conversion for: " + pieces);
    }

    String streamKey = getKey(useKey, name);
    streams.put(streamKey, valueConverted);
  }

  /**
   * Configures and sets the sales stream distribution for manufacturing and import based on the
   * provided key, name, and engine number value. The provided value is converted to kilograms and
   * further distributed according to pre-defined distribution percentages.
   *
   * @param useKey A key object representing the context or identifier for the sales stream to be
   *               set.
   * @param name The name associated with the sales stream being configured.
   * @param value The engine number input value to be converted and distributed into manufacturing
   *              and import streams.
   */
  private void setStreamForSales(UseKey useKey, String name, EngineNumber value) {
    EngineNumber valueConverted = unitConverter.convert(value, "kg");
    BigDecimal amountKg = valueConverted.getValue();

    // Get current recycle amount to avoid double counting
    EngineNumber recycleAmountRaw = getStream(useKey, "recycle");
    EngineNumber recycleAmount = unitConverter.convert(recycleAmountRaw, "kg");
    BigDecimal recycleKg = recycleAmount != null ? recycleAmount.getValue() : BigDecimal.ZERO;

    // Calculate virgin material needed (sales - recycling)
    BigDecimal virginMaterialKg = amountKg.subtract(recycleKg);

    if (virginMaterialKg.compareTo(BigDecimal.ZERO) < 0) {
      virginMaterialKg = BigDecimal.ZERO;
    }

    // Get distribution using centralized method
    SalesStreamDistribution distribution = getDistribution(useKey);

    BigDecimal domesticPercent = distribution.getPercentDomestic();
    BigDecimal importPercent = distribution.getPercentImport();

    // Distribute only the virgin material between domestic and import
    BigDecimal newDomesticAmount = virginMaterialKg.multiply(domesticPercent);
    BigDecimal newImportAmount = virginMaterialKg.multiply(importPercent);

    EngineNumber domesticAmountToSet = new EngineNumber(newDomesticAmount, "kg");
    EngineNumber importAmountToSet = new EngineNumber(newImportAmount, "kg");

    setSimpleStream(useKey, "domestic", domesticAmountToSet);
    setSimpleStream(useKey, "import", importAmountToSet);
  }

  /**
   * Distribute a virgin stream value to domestic and import without recycling subtraction.
   *
   * <p>Unlike sales which subtracts recycling before distributing, virgin distributes
   * the full amount to domestic and import. This is the key difference: virgin
   * represents only domestic + import (excluding recycling).</p>
   *
   * @param useKey A key object representing the context for the virgin stream
   * @param name The name associated with the stream being configured
   * @param value The engine number input value to be distributed into domestic and import
   */
  private void setStreamForVirgin(UseKey useKey, String name, EngineNumber value) {
    EngineNumber valueConverted = unitConverter.convert(value, "kg");
    BigDecimal amountKg = valueConverted.getValue();

    SalesStreamDistribution distribution = getDistribution(useKey);

    BigDecimal domesticPercent = distribution.getPercentDomestic();
    BigDecimal importPercent = distribution.getPercentImport();

    BigDecimal newDomesticAmount = amountKg.multiply(domesticPercent);
    BigDecimal newImportAmount = amountKg.multiply(importPercent);

    EngineNumber domesticAmountToSet = new EngineNumber(newDomesticAmount, "kg");
    EngineNumber importAmountToSet = new EngineNumber(newImportAmount, "kg");

    setSimpleStream(useKey, "domestic", domesticAmountToSet);
    setSimpleStream(useKey, "import", importAmountToSet);
  }

  /**
   * Sets the recycle stream by distributing the value proportionally between recycleRecharge and recycleEol.
   * Similar to sales distribution, this method uses the prior sizes of recycleRecharge and recycleEol
   * to determine the proportional distribution.
   *
   * @param useKey The key containing application and substance
   * @param name The stream name (should be "recycle")
   * @param value The total recycle value to be distributed
   */
  private void setStreamForRecycle(UseKey useKey, String name, EngineNumber value) {
    EngineNumber valueConverted = unitConverter.convert(value, "kg");
    BigDecimal totalRecycleKg = valueConverted.getValue();

    EngineNumber recycleRechargeAmountRaw = getStream(useKey, "recycleRecharge");
    EngineNumber recycleEolAmountRaw = getStream(useKey, "recycleEol");

    EngineNumber recycleRechargeAmount = unitConverter.convert(recycleRechargeAmountRaw, "kg");
    EngineNumber recycleEolAmount = unitConverter.convert(recycleEolAmountRaw, "kg");

    BigDecimal recycleRechargeKg = recycleRechargeAmount != null ? recycleRechargeAmount.getValue() : BigDecimal.ZERO;
    BigDecimal recycleEolKg = recycleEolAmount != null ? recycleEolAmount.getValue() : BigDecimal.ZERO;

    BigDecimal totalExistingRecycle = recycleRechargeKg.add(recycleEolKg);
    boolean noExistingRecycling = totalExistingRecycle.compareTo(BigDecimal.ZERO) == 0;

    BigDecimal newRecycleRechargeAmount;
    BigDecimal newRecycleEolAmount;

    if (noExistingRecycling) {
      newRecycleRechargeAmount = totalRecycleKg.divide(new BigDecimal("2"));
      newRecycleEolAmount = totalRecycleKg.divide(new BigDecimal("2"));
    } else {
      BigDecimal rechargePercent = recycleRechargeKg.divide(totalExistingRecycle, MathContext.DECIMAL128);
      BigDecimal eolPercent = recycleEolKg.divide(totalExistingRecycle, MathContext.DECIMAL128);

      newRecycleRechargeAmount = totalRecycleKg.multiply(rechargePercent);
      newRecycleEolAmount = totalRecycleKg.multiply(eolPercent);
    }

    EngineNumber recycleRechargeAmountToSet = new EngineNumber(newRecycleRechargeAmount, "kg");
    EngineNumber recycleEolAmountToSet = new EngineNumber(newRecycleEolAmount, "kg");

    setSimpleStream(useKey, "recycleRecharge", recycleRechargeAmountToSet);
    setSimpleStream(useKey, "recycleEol", recycleEolAmountToSet);
  }

  /**
   * Sets the sales stream with units for a specific use key and name. This method converts the
   * initial charge and input value to specified units, validates the charge, and updates the internal
   * state to reflect the conversions. The resulting stream is then stored with the corresponding key.
   *
   * @param useKey The identifier representing the context or use case for which the stream is being set.
   * @param name The name associated with the stream to be updated.
   * @param value The value to be converted and used for updating the stream, typically representing sales units.
   */
  private void setStreamForSalesWithUnits(UseKey useKey, String name, EngineNumber value) {
    OverridingConverterStateGetter overridingStateGetter = new OverridingConverterStateGetter(
        stateGetter
    );
    UnitConverter unitConverter = new UnitConverter(overridingStateGetter);

    EngineNumber initialCharge = getInitialCharge(useKey, name);
    boolean noInitialCharge = initialCharge.getValue().compareTo(BigDecimal.ZERO) == 0;
    if (noInitialCharge) {
      throw new RuntimeException("Cannot set " + name + " stream with a zero initial charge.");
    }

    EngineNumber initialChargeConverted = unitConverter.convert(initialCharge, "kg / unit");
    overridingStateGetter.setAmortizedUnitVolume(initialChargeConverted);

    EngineNumber valueUnitsPlain = unitConverter.convert(value, "units");
    EngineNumber valueConverted = unitConverter.convert(valueUnitsPlain, "kg");
    BigDecimal amountKg = valueConverted.getValue();

    // Set the amount directly - recycling should already be handled by setSalesStream
    String streamKey = getKey(useKey, name);
    EngineNumber amountToSet = new EngineNumber(amountKg, "kg");
    streams.put(streamKey, amountToSet);
  }

  /**
   * Verify that a substance exists for a key.
   *
   * @param key The key containing application and substance
   * @param context The context for error reporting
   * @throws IllegalStateException If the substance does not exist for the key
   */
  private void ensureSubstanceOrThrow(String key, String context) {
    if (key == null) {
      throw new IllegalStateException("Key cannot be null in " + context);
    }
    if (!substances.containsKey(key)) {
      throwSubstanceMissing(context, key.split("\t")[0], key.split("\t")[1]);
    }
  }

  /**
   * Indicate that a substance / application was not found.
   *
   * <p>Throw an IllegalStateException when an unknown application-substance pair is encountered
   * in the specified context.</p>
   *
   * @param context the context in which the application-substance pair is unknown
   * @param application the name of the application being checked
   * @param substance the name of the substance being checked
   */
  private void throwSubstanceMissing(String context, String application, String substance) {
    SimulationStateSupport.throwSubstanceMissing(context, application, substance);
  }

  /**
   * Verify that a stream name is valid.
   *
   * @param name The stream name to verify
   * @throws IllegalArgumentException If the stream name is not recognized
   */
  private void ensureStreamKnown(String name) {
    SimulationStateSupport.ensureStreamKnown(name);
  }

  /**
   * Assert that a stream has been enabled for the given use key.
   * Only checks domestic, import, and export streams.
   *
   * @param useKey The key containing application and substance
   * @param streamName The name of the stream to check
   * @param value The value being set (no assertion needed if zero)
   * @throws RuntimeException If the stream has not been enabled and value is non-zero
   */
  private void assertStreamEnabled(UseKey useKey, String streamName, EngineNumber value) {
    // Only check enabling for sales streams that require explicit enabling
    if (!"domestic".equals(streamName) && !"import".equals(streamName) && !"export".equals(streamName)) {
      return;
    }

    // Don't require enabling if setting to zero
    if (value.getValue().compareTo(BigDecimal.ZERO) == 0) {
      return;
    }

    StreamParameterization parameterization = getParameterization(useKey);
    if (!parameterization.hasStreamBeenEnabled(streamName)) {
      throw new RuntimeException("Stream '" + streamName + "' has not been enabled for "
          + useKey.getApplication() + "/" + useKey.getSubstance()
          + ". Check if you still have a command on this stream which may be erroneous or "
          + "enable the stream.");
    }
  }

  /**
   * Get the base units for a stream.
   *
   * @param name The stream name
   * @return The base units for the stream
   */
  private String getUnits(String name) {
    ensureStreamKnown(name);
    return EngineConstants.getBaseUnits(name);
  }

  /**
   * Determine if the user is setting a sales component (domestic / import / sales) by units.
   *
   * @param name The stream name
   * @param value The value to set
   * @return true if the user is setting a sales component by units and false otherwise
   */
  private boolean getIsSettingVolumeByUnits(String name, EngineNumber value) {
    boolean isSalesComponent = switch (name) {
      case "domestic", "import", "sales", "virgin" -> true;
      default -> false;
    };
    boolean isUnits = value.getUnits().startsWith("unit");
    return isSalesComponent && isUnits;
  }

  /**
   * Calculate the current recycling amount using the current population context.
   * This method replicates the recycling calculation from SalesRecalcStrategy but uses the current
   * population state instead of relying on stale data.
   *
   * @param useKey The key containing application and substance
   * @return The amount of recycling available in kg
   */
  private BigDecimal calculateCurrentRecyclingAmount(UseKey useKey) {
    // Get current prior population (this is the population available for recycling)
    EngineNumber priorPopulationRaw = getStream(useKey, "priorEquipment");
    if (priorPopulationRaw == null) {
      return BigDecimal.ZERO;
    }
    EngineNumber priorPopulation = unitConverter.convert(priorPopulationRaw, "units");

    // Get retirement rate
    StreamParameterization parameterization = getParameterization(useKey);
    EngineNumber retirementRate = parameterization.getRetirementRate();

    // Handle different retirement rate units
    BigDecimal retirementRateRatio;
    if (retirementRate.getUnits().contains("%")) {
      retirementRateRatio = retirementRate.getValue().divide(
          HUNDRED_PERCENT, java.math.MathContext.DECIMAL128);
    } else {
      // If units are not percentage, assume it's already a ratio
      retirementRateRatio = retirementRate.getValue();
    }

    // Calculate retired units
    BigDecimal retiredUnits = priorPopulation.getValue().multiply(retirementRateRatio);

    // Get recovery rate
    EngineNumber recoveryRate = parameterization.getRecoveryRate();
    BigDecimal recoveryRateRatio;
    if (recoveryRate.getUnits().contains("%")) {
      recoveryRateRatio = recoveryRate.getValue().divide(
          HUNDRED_PERCENT, java.math.MathContext.DECIMAL128);
    } else {
      recoveryRateRatio = recoveryRate.getValue();
    }

    // Calculate recovered units
    BigDecimal recoveredUnits = retiredUnits.multiply(recoveryRateRatio);

    // Get yield rate
    EngineNumber yieldRate = parameterization.getYieldRate();
    BigDecimal yieldRateRatio;
    if (yieldRate.getUnits().contains("%")) {
      yieldRateRatio = yieldRate.getValue().divide(
          HUNDRED_PERCENT, java.math.MathContext.DECIMAL128);
    } else {
      yieldRateRatio = yieldRate.getValue();
    }

    // Calculate recycled material volume
    BigDecimal recycledUnits = recoveredUnits.multiply(yieldRateRatio);

    // Convert to kg using initial charge
    EngineNumber initialCharge = parameterization.getInitialCharge("import");
    EngineNumber initialChargeConverted = unitConverter.convert(initialCharge, "kg / unit");
    BigDecimal recycledKg = recycledUnits.multiply(initialChargeConverted.getValue());

    // Recycling does not apply cross-substance displacement
    return recycledKg;
  }

  /**
   * Redistribute recycling amounts back to sales streams before year transition.
   *
   * <p>This method addresses the cross-year state carryover issue where recycling
   * correctly displaces virgin material in Year N, but the reduced virgin sales baseline incorrectly
   * carries forward to Year N+1, creating cumulative deficit.</p>
   *
   * <div>This fix is applied to all scenarios with configured sales streams including:
   * <ul>
   *   <li>"set sales to X [units]" - Total sales specified</li>
   *   <li>"set import to X [units]" - Import volume specified</li>
   *   <li>"set domestic to X [units]" - Domestic volume specified</li>
   * </ul></div>
   *
   * <p>The redistribution preserves user expectations that loss of recycling will be
   * back-filled by virgin material to maintain total available material, regardless of whether the
   * original specification was in mass units (kg, mt) or equipment units.</p>
   */
  private void redistributeRecyclingToSales() {
    for (String key : substances.keySet()) {
      String[] keyPieces = key.split("\t");
      String application = keyPieces[0];
      String substance = keyPieces[1];

      SimpleUseKey useKey = new SimpleUseKey(application, substance);

      // Skip if no streams are enabled (nothing to redistribute to)
      if (!hasStreamsEnabled(useKey)) {
        continue;
      }

      // Apply redistribution to all scenarios with configured sales streams
      // When recycling is lost, back-fill with virgin material regardless of units
      StreamParameterization parameterization = getParameterization(useKey);
      boolean salesWasSet = parameterization.hasLastSpecifiedValue("sales");
      boolean domesticWasSet = parameterization.hasLastSpecifiedValue("domestic");
      boolean importWasSet = parameterization.hasLastSpecifiedValue("import");

      // Skip if no sales streams were configured (nothing to redistribute to)
      if (!salesWasSet && !domesticWasSet && !importWasSet) {
        continue;
      }

      // Get total recycling amount for this substance/application
      EngineNumber totalRecycling = getStream(useKey, "recycle");
      EngineNumber recyclingKg = unitConverter.convert(totalRecycling, "kg");

      // Skip if no recycling to redistribute
      if (recyclingKg.getValue().compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }

      // Get current sales distribution for proportional allocation (BEFORE modifying streams)
      SalesStreamDistribution distribution = getDistribution(useKey, false); // Exclude exports for compatibility

      // Calculate redistribution amounts
      BigDecimal domesticAdd = recyclingKg.getValue().multiply(distribution.getPercentDomestic());
      BigDecimal importAdd = recyclingKg.getValue().multiply(distribution.getPercentImport());

      // Add recycling back to sales streams (preserve baseline for next year)
      EngineNumber currentDomestic = getStream(useKey, "domestic");
      EngineNumber currentImport = getStream(useKey, "import");

      EngineNumber domesticConverted = unitConverter.convert(currentDomestic, "kg");
      EngineNumber importConverted = unitConverter.convert(currentImport, "kg");

      BigDecimal newDomestic = domesticConverted.getValue().add(domesticAdd);
      BigDecimal newImport = importConverted.getValue().add(importAdd);

      // Cap the restored baseline so it never exceeds the true target (see
      // getRedistributionCapKg for why).
      Optional<BigDecimal> domesticCapKg = getRedistributionCapKg(
          parameterization,
          "domestic",
          distribution.getPercentDomestic()
      );
      if (domesticCapKg.isPresent()) {
        newDomestic = newDomestic.min(domesticCapKg.get());
      }

      Optional<BigDecimal> importCapKg = getRedistributionCapKg(
          parameterization,
          "import",
          distribution.getPercentImport()
      );
      if (importCapKg.isPresent()) {
        newImport = newImport.min(importCapKg.get());
      }

      // Set new amounts using direct stream setting to avoid circular dependency
      // Use setSimpleStream since this is internal redistribution logic
      setSimpleStream(useKey, "domestic", new EngineNumber(newDomestic, "kg"));
      setSimpleStream(useKey, "import", new EngineNumber(newImport, "kg"));
    }
  }

  /**
   * Determine the mass-based cap, in kg, that a redistributed stream should never exceed.
   *
   * <p>Falls back to the stream's own last-specified value when mass-based (kg/mt), or to the
   * "sales" last-specified value (scaled by this stream's share of the sales distribution) when
   * the stream itself was not directly specified. Returns empty when neither is available or
   * both are equipment-unit-based, since unit-based specifications are not reduced by recycling
   * in the first place (see DemandAnalysisBuilder#calculateRequiredVirginMaterialUnitsBased) and
   * so need no cap here.</p>
   *
   * @param parameterization The stream parameterization to read last-specified values from
   * @param stream The stream to compute a cap for ("domestic" or "import")
   * @param percentShare This stream's share of the sales distribution, used when falling back
   *     to the "sales" last-specified value
   * @return The cap in kg, or empty if no mass-based target is available
   */
  private Optional<BigDecimal> getRedistributionCapKg(StreamParameterization parameterization,
      String stream, BigDecimal percentShare) {
    if (parameterization.hasLastSpecifiedValue(stream)) {
      EngineNumber lastSpecified = parameterization.getLastSpecifiedValue(stream);
      if (!lastSpecified.hasEquipmentUnits()) {
        return Optional.of(unitConverter.convert(lastSpecified, "kg").getValue());
      }
    } else if (parameterization.hasLastSpecifiedValue("sales")) {
      EngineNumber lastSpecifiedSales = parameterization.getLastSpecifiedValue("sales");
      if (!lastSpecifiedSales.hasEquipmentUnits()) {
        BigDecimal salesKg = unitConverter.convert(lastSpecifiedSales, "kg").getValue();
        return Optional.of(salesKg.multiply(percentShare));
      }
    }
    return Optional.empty();
  }

  /**
   * Redistribute induction amounts from sales streams before year transition.
   *
   * <p>This method addresses the cross-year induction carryover issue where induction
   * correctly adds to virgin material in Year N, but the increased virgin sales baseline incorrectly
   * carries forward to Year N+1, creating cumulative surplus.</p>
   */
  private void redistributeInductionFromSales() {
    for (String key : substances.keySet()) {
      String[] keyPieces = key.split("\t");
      String application = keyPieces[0];
      String substance = keyPieces[1];

      SimpleUseKey useKey = new SimpleUseKey(application, substance);

      // Skip if no streams are enabled
      if (!hasStreamsEnabled(useKey)) {
        continue;
      }

      // Get total induction amount for this substance/application
      EngineNumber totalInduction = getTotalInductionStream(useKey);
      EngineNumber inductionKg = unitConverter.convert(totalInduction, "kg");

      // Skip if no induction to redistribute
      if (inductionKg.getValue().compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }

      // Get current sales distribution for proportional allocation
      SalesStreamDistribution distribution = getDistribution(useKey, false);

      // Calculate redistribution amounts (subtract induction from virgin streams)
      BigDecimal domesticSubtract = inductionKg.getValue().multiply(distribution.getPercentDomestic());
      BigDecimal importSubtract = inductionKg.getValue().multiply(distribution.getPercentImport());

      // Subtract induction from sales streams (normalize baseline for next year)
      EngineNumber currentDomestic = getStream(useKey, "domestic");
      EngineNumber currentImport = getStream(useKey, "import");

      EngineNumber domesticConverted = unitConverter.convert(currentDomestic, "kg");
      EngineNumber importConverted = unitConverter.convert(currentImport, "kg");

      BigDecimal newDomestic = domesticConverted.getValue().subtract(domesticSubtract).max(BigDecimal.ZERO);
      BigDecimal newImport = importConverted.getValue().subtract(importSubtract).max(BigDecimal.ZERO);

      // Set new amounts using direct stream setting
      setSimpleStream(useKey, "domestic", new EngineNumber(newDomestic, "kg"));
      setSimpleStream(useKey, "import", new EngineNumber(newImport, "kg"));
    }
  }

  /**
   * Update cumulative bases when priorEquipment is manually modified.
   *
   * <p>When priorEquipment changes via user commands (set/change/floor/ceiling),
   * captured bases are proportionally scaled with applied amounts to maintain cumulative semantics.
   * Retirement and recharge bases scale independently.</p>
   *
   * @param useKey The key containing application and substance
   * @param streamName The name of the stream being modified
   * @param newValue The new value being set for priorEquipment
   */
  private void updatePriorEquipmentBase(UseKey useKey, String streamName, EngineNumber newValue) {
    // Only process priorEquipment changes
    if (!"priorEquipment".equals(streamName)) {
      return;
    }

    String key = getKey(useKey);
    StreamParameterization param = substances.get(key);
    boolean noParameterizationYet = param == null;
    if (noParameterizationYet) {
      return;
    }

    // Convert new value to units for consistency
    EngineNumber newPriorUnits = unitConverter.convert(newValue, "units");

    Optional<EngineNumber> retireBaseOpt = param.getRetirementBasePopulation();
    Optional<EngineNumber> rechargeBaseOpt = param.getRechargeBasePopulation();

    boolean retireBaseActive = retireBaseOpt.isPresent();
    boolean rechargeBaseActive = rechargeBaseOpt.isPresent();
    boolean nothingToUpdate = !retireBaseActive && !rechargeBaseActive;

    if (nothingToUpdate) {
      return;
    }

    // Get current priorEquipment value to check if it's actually changing
    EngineNumber currentPriorRaw = getStream(useKey, "priorEquipment");
    EngineNumber currentPriorUnits = unitConverter.convert(currentPriorRaw, "units");

    BigDecimal currentPriorValue = currentPriorUnits.getValue();
    BigDecimal newPriorValue = newPriorUnits.getValue();
    BigDecimal diff = currentPriorValue.subtract(newPriorValue).abs();

    boolean withinTolerance = diff.compareTo(BASE_CHANGE_TOLERANCE) <= 0;
    if (withinTolerance) {
      return;
    }

    if (retireBaseActive) {
      updateRetireBase(useKey, newValue, retireBaseOpt.get(), param);
    }

    if (rechargeBaseActive) {
      updateRechargeBase(useKey, newValue, rechargeBaseOpt.get(), param);
    }
  }

  /**
   * Scale retirement base and applied amount when priorEquipment changes.
   *
   * <p>Maintains the percentage of population already retired by scaling both
   * the base population and applied amount proportionally to the new priorEquipment value.</p>
   *
   * @param useKey The key containing application and substance
   * @param newValue The new priorEquipment value
   * @param retireBase The current retirement base population
   * @param param The parameterization containing retirement state
   */
  private void updateRetireBase(UseKey useKey, EngineNumber newValue, EngineNumber retireBase,
      StreamParameterization param) {
    EngineNumber newPriorUnits = unitConverter.convert(newValue, "units");
    Optional<EngineNumber> appliedRetireOpt = param.getAppliedRetirementAmount();
    EngineNumber appliedRetire = appliedRetireOpt.orElse(new EngineNumber(BigDecimal.ZERO, "units"));

    boolean noPriorBase = retireBase.getValue().compareTo(BigDecimal.ZERO) == 0;
    if (noPriorBase) {
      param.setRetirementBasePopulation(newPriorUnits);
      param.setAppliedRetirementAmount(new EngineNumber(BigDecimal.ZERO, "units"));
    } else {
      BigDecimal retirePercent = appliedRetire.getValue().divide(
          retireBase.getValue(), MathContext.DECIMAL128);

      BigDecimal newApplied = newPriorUnits.getValue().multiply(retirePercent);

      param.setRetirementBasePopulation(newPriorUnits);
      param.setAppliedRetirementAmount(new EngineNumber(newApplied, "units"));
    }
  }

  /**
   * Scale recharge base and applied amount when priorEquipment changes.
   *
   * <p>Scales the recharge base and applied amount by the ratio of new to old base value.
   * This maintains the cumulative semantics while adjusting for the new population base.</p>
   *
   * @param useKey The key containing application and substance
   * @param newValue The new priorEquipment value
   * @param rechargeBase The current recharge base population
   * @param param The parameterization containing recharge state
   */
  private void updateRechargeBase(UseKey useKey, EngineNumber newValue, EngineNumber rechargeBase,
      StreamParameterization param) {
    EngineNumber newPriorUnits = unitConverter.convert(newValue, "units");
    Optional<EngineNumber> appliedRechargeOpt = param.getAppliedRechargeAmount();
    EngineNumber appliedRecharge = appliedRechargeOpt.orElse(new EngineNumber(BigDecimal.ZERO, "kg"));

    boolean noPriorBase = rechargeBase.getValue().compareTo(BigDecimal.ZERO) == 0;
    if (noPriorBase) {
      param.setRechargeBasePopulation(newPriorUnits);
      param.setAppliedRechargeAmount(new EngineNumber(BigDecimal.ZERO, "kg"));
    } else {
      BigDecimal baseRatio = newPriorUnits.getValue().divide(
          rechargeBase.getValue(), MathContext.DECIMAL128);

      BigDecimal newApplied = appliedRecharge.getValue().multiply(baseRatio);

      param.setRechargeBasePopulation(newPriorUnits);
      param.setAppliedRechargeAmount(new EngineNumber(newApplied, "kg"));
    }
  }

  /** {@inheritDoc} */
  @Override
  public void clearLastSpecifiedValue(UseKey useKey, String stream) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.clearLastSpecifiedValue(stream);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Container fields (the substances and streams maps) are copied so that later
   * additions and removals on this instance do not affect the snapshot. Each
   * substance's {@link StreamParameterization} is frozen recursively. EngineNumber
   * instances are immutable, so their references can be shared. {@code priorState}
   * is captured by reference: the prior chain is already frozen, so sharing it is
   * safe and O(1) regardless of how many years deep the chain is. The state getter
   * and unit converter dependencies are shared (not copied) as they are
   * configuration objects.</p>
   */
  @Override
  public SimulationState freeze() {
    Map<String, StreamParameterization> frozenSubstances = new HashMap<>();
    for (Map.Entry<String, StreamParameterization> entry : substances.entrySet()) {
      frozenSubstances.put(entry.getKey(), entry.getValue().freeze());
    }

    return new FrozenSimulationState(
        frozenSubstances,
        new HashMap<>(streams),
        priorState,
        stateGetter,
        unitConverter,
        currentYear);
  }
}
