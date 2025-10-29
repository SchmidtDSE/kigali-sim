/**
 * Manages the state for a single scenario within a single trial.
 *
 * <p>State management object for storage and retrieval of substance data, stream
 * values, and associated parameterizations.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import java.math.BigDecimal;
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
 * Manages the state for a single scenario within a single trial.
 *
 * <p>State management object for storage and retrieval of substance data, stream
 * values, and associated parameterizations.</p>
 */
public class SimulationState {

  private static final boolean CHECK_NAN_STATE = false;
  private static final BigDecimal BASE_CHANGE_TOLERANCE = new BigDecimal("0.0001");

  private final Map<String, StreamParameterization> substances;
  private final Map<String, EngineNumber> streams;
  private final OverridingConverterStateGetter stateGetter;
  private final UnitConverter unitConverter;
  private int currentYear;

  /**
   * Create a new SimulationState instance.
   *
   * @param stateGetter Structure to retrieve state information
   * @param unitConverter Converter for handling unit transformations
   */
  public SimulationState(OverridingConverterStateGetter stateGetter, UnitConverter unitConverter) {
    this.substances = new HashMap<>();
    this.streams = new HashMap<>();
    this.stateGetter = stateGetter;
    this.unitConverter = unitConverter;
  }

  /**
   * Get all registered substance-application pairs.
   *
   * @return Array of substance identifiers
   */
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

  /**
   * Check if a substance exists for a key.
   *
   * @param useKey The key containing application and substance
   * @return true if the substance exists for the key
   */
  public boolean hasSubstance(UseKey useKey) {
    String key = getKey(useKey);
    return substances.containsKey(key);
  }

  /**
   * Ensure a substance exists for a key, creating it if needed.
   *
   * @param useKey The key containing application and substance
   */
  public void ensureSubstance(UseKey useKey) {
    String key = getKey(useKey);

    if (substances.containsKey(key)) {
      return;
    }

    substances.put(key, new StreamParameterization());

    // Sales: domestic, import, export, recycle (split into recycleRecharge and recycleEol)
    String domesticKey = getKey(useKey, "domestic");
    streams.put(domesticKey, new EngineNumber(BigDecimal.ZERO, "kg"));
    String importKey = getKey(useKey, "import");
    streams.put(importKey, new EngineNumber(BigDecimal.ZERO, "kg"));
    String exportKey = getKey(useKey, "export");
    streams.put(exportKey, new EngineNumber(BigDecimal.ZERO, "kg"));
    String recycleRechargeKey = getKey(useKey, "recycleRecharge");
    streams.put(recycleRechargeKey, new EngineNumber(BigDecimal.ZERO, "kg"));
    String recycleEolKey = getKey(useKey, "recycleEol");
    streams.put(recycleEolKey, new EngineNumber(BigDecimal.ZERO, "kg"));
    String inductionEolKey = getKey(useKey, "inductionEol");
    streams.put(inductionEolKey, new EngineNumber(BigDecimal.ZERO, "kg"));
    String inductionRechargeKey = getKey(useKey, "inductionRecharge");
    streams.put(inductionRechargeKey, new EngineNumber(BigDecimal.ZERO, "kg"));

    // Consumption: count, conversion
    String consumptionKey = getKey(useKey, "consumption");
    streams.put(consumptionKey, new EngineNumber(BigDecimal.ZERO, "tCO2e"));

    // Population
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

    // Emissions
    String rechargeEmissionsKey = getKey(useKey, "rechargeEmissions");
    streams.put(rechargeEmissionsKey, new EngineNumber(BigDecimal.ZERO, "tCO2e"));
    String eolEmissionsKey = getKey(useKey, "eolEmissions");
    streams.put(eolEmissionsKey, new EngineNumber(BigDecimal.ZERO, "tCO2e"));

    // Recharge tracking
    String implicitRechargeKey = getKey(useKey, "implicitRecharge");
    streams.put(implicitRechargeKey, new EngineNumber(BigDecimal.ZERO, "kg"));

    // Age tracking
    String ageKey = getKey(useKey, "age");
    streams.put(ageKey, new EngineNumber(BigDecimal.ZERO, "years"));
  }

  /**
   * Set a stream using pre-computed stream data.
   *
   * <p>This method replaces setStream, setOutcomeStream, and setSalesStream
   * with a unified interface that accepts pre-computed stream values.
   * The SimulationStateUpdate object encapsulates all necessary parameters
   * including distribution logic and recycling behavior.</p>
   *
   * <p>This method provides clear architectural separation between calculation
   * instructions (StreamUpdate) and pre-computed results (SimulationStateUpdate).</p>
   *
   * @param stateUpdate Pre-computed stream data with all parameters
   */
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
      String pieces = String.join(" > ",
          "-".equals(application) ? "null" : application,
          "-".equals(substance) ? "null" : substance,
          name);
      throw new RuntimeException("Encountered NaN to be set for: " + pieces);
    }

    // Extract routing parameters when needed
    final boolean subtractRecycling = stateUpdate.getSubtractRecycling();
    final Optional<SalesStreamDistribution> distribution = stateUpdate.getDistribution();

    // Route to appropriate internal method based on stream characteristics and subtractRecycling
    if (!subtractRecycling && ("domestic".equals(name) || "import".equals(name))) {
      // Direct setting for sales streams - bypass recycling logic
      setSimpleStream(useKey, name, value);
    } else {
      // Route based on stream type using the same logic as original setStream method
      if ("sales".equals(name)) {
        setStreamForSales(useKey, name, value);
      } else if ("domestic".equals(name) || "import".equals(name)) {
        // Sales substreams - delegate to organized private method
        setStreamSalesSubstream(useKey, name, value, distribution);
      } else if ("recycle".equals(name)) {
        setStreamForRecycle(useKey, name, value);
      } else if (getIsSettingVolumeByUnits(name, value)) {
        setStreamForSalesWithUnits(useKey, name, value);
      } else {
        // Outcome streams - inline outcome stream logic (direct setting)
        setSimpleStream(useKey, name, value);
      }
    }
  }

  /**
   * Set a sales substream (domestic or import) with recycling displacement logic.
   *
   * <p>This method handles individual sales streams while applying proportional recycling
   * displacement based on distribution percentages. It consolidates the sales substream
   * logic that was previously inlined in the main setStream method.</p>
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

  /**
   * Get the value of a specific stream using key.
   *
   * @param useKey The key containing application and substance
   * @param name The stream name
   * @return The stream value
   */
  public EngineNumber getStream(UseKey useKey, String name) {
    String key = getKey(useKey);

    ensureStreamKnown(name);

    if ("sales".equals(name)) {
      EngineNumber domesticAmountRaw = getStream(useKey, "domestic");
      EngineNumber importAmountRaw = getStream(useKey, "import");
      EngineNumber recycleAmountRaw = getStream(useKey, "recycle");

      EngineNumber domesticAmount = unitConverter.convert(domesticAmountRaw, "kg");
      EngineNumber importAmount = unitConverter.convert(importAmountRaw, "kg");
      EngineNumber recycleAmount = unitConverter.convert(recycleAmountRaw, "kg");

      BigDecimal domesticAmountValue = domesticAmount.getValue();
      BigDecimal importAmountValue = importAmount.getValue();
      BigDecimal recycleAmountValue = recycleAmount.getValue();

      BigDecimal newTotal = domesticAmountValue.add(importAmountValue).add(recycleAmountValue);

      return new EngineNumber(newTotal, "kg");
    } else if ("recycle".equals(name)) {
      EngineNumber recycleRechargeAmountRaw = getStream(useKey, "recycleRecharge");
      EngineNumber recycleEolAmountRaw = getStream(useKey, "recycleEol");

      EngineNumber recycleRechargeAmount = unitConverter.convert(recycleRechargeAmountRaw, "kg");
      EngineNumber recycleEolAmount = unitConverter.convert(recycleEolAmountRaw, "kg");

      BigDecimal recycleRechargeAmountValue = recycleRechargeAmount.getValue();
      BigDecimal recycleEolAmountValue = recycleEolAmount.getValue();

      BigDecimal newTotal = recycleRechargeAmountValue.add(recycleEolAmountValue);

      return new EngineNumber(newTotal, "kg");
    } else if ("induction".equals(name)) {
      return getTotalInductionStream(useKey);
    } else {
      EngineNumber result = streams.get(getKey(useKey, name));
      if (result == null) {
        throwSubstanceMissing(
            "getStream",
            useKey.getApplication(),
            useKey.getSubstance()
        );
      }
      return result;
    }
  }

  /**
   * Check if a stream exists for a key.
   *
   * @param useKey The key containing application and substance
   * @param name The stream name
   * @return true if the stream exists
   */
  public boolean isKnownStream(UseKey useKey, String name) {
    return streams.containsKey(getKey(useKey, name));
  }

  /**
   * Get the induction stream value for a specific recovery stage.
   *
   * @param useKey The key containing application and substance
   * @param stage The recovery stage (EOL or RECHARGE)
   * @return The induction stream value in kg
   */
  public EngineNumber getInductionStream(UseKey useKey, RecoveryStage stage) {
    String streamName = getInductionStreamName(stage);
    return getStream(useKey, streamName);
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

  /**
   * Get total induction across all stages.
   *
   * @param useKey The key containing application and substance
   * @return Total induction in kg
   */
  public EngineNumber getTotalInductionStream(UseKey useKey) {
    EngineNumber inductionEol = getInductionStream(useKey, RecoveryStage.EOL);
    EngineNumber inductionRecharge = getInductionStream(useKey, RecoveryStage.RECHARGE);

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
    return switch (stage) {
      case EOL -> "inductionEol";
      case RECHARGE -> "inductionRecharge";
    };
  }

  /**
   * Get a sales stream distribution for the given substance/application.
   *
   * <p>This method centralizes the logic for creating sales distributions by getting
   * the current domestic and import values, determining their enabled status,
   * and building an appropriate distribution using the builder pattern.
   * Exports are excluded for backward compatibility.</p>
   *
   * @param useKey The key containing application and substance
   * @return A SalesStreamDistribution with appropriate percentages
   */
  public SalesStreamDistribution getDistribution(UseKey useKey) {
    return getDistribution(useKey, false);
  }

  /**
   * Get a sales stream distribution for the given substance/application.
   *
   * <p>This method centralizes the logic for creating sales distributions by getting
   * the current domestic, import, and optionally export values, determining their enabled status,
   * and building an appropriate distribution using the builder pattern.</p>
   *
   * @param useKey The key containing application and substance
   * @param includeExports Whether to include exports in the distribution calculation
   * @return A SalesStreamDistribution with appropriate percentages
   */
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

  /**
   * Check if any sales streams have been enabled for the given substance/application.
   *
   * @param useKey The key containing application and substance
   * @return True if any of domestic, import, or export streams are enabled
   */
  public boolean hasStreamsEnabled(UseKey useKey) {
    return hasStreamBeenEnabled(useKey, "domestic")
        || hasStreamBeenEnabled(useKey, "import")
        || hasStreamBeenEnabled(useKey, "export");
  }

  /**
   * Get the current year for this simulation state.
   *
   * @return The current year
   */
  public int getCurrentYear() {
    return currentYear;
  }

  /**
   * Set the current year for this simulation state.
   *
   * @param year The current year
   */
  public void setCurrentYear(int year) {
    this.currentYear = year;
  }

  /**
   * Increment the year, updating populations and resetting internal params.
   */
  public void incrementYear() {
    // Increment the internal year counter
    currentYear += 1;
    // Move population and retired counts
    for (String key : substances.keySet()) {
      String[] keyPieces = key.split("\t");
      String application = keyPieces[0];
      String substance = keyPieces[1];

      SimpleUseKey useKey = new SimpleUseKey(application, substance);
      EngineNumber equipment = getStream(useKey, "equipment");
      setSimpleStream(useKey, "priorEquipment", equipment);

      EngineNumber retired = getStream(useKey, "retired");
      setSimpleStream(useKey, "priorRetired", retired);

      // Calculate weighted average age for the new year
      EngineNumber priorEquipmentValue = getStream(useKey, "priorEquipment");
      EngineNumber currentEquipmentValue = getStream(useKey, "equipment");
      EngineNumber currentAge = getStream(useKey, "age");

      // Convert to units for calculation
      EngineNumber priorEquipmentUnits = unitConverter.convert(priorEquipmentValue, "units");
      EngineNumber currentEquipmentUnits = unitConverter.convert(currentEquipmentValue, "units");

      // Calculate weights
      BigDecimal priorAgeWeight = priorEquipmentUnits.getValue();
      BigDecimal addedEquipment = currentEquipmentUnits.getValue().subtract(priorEquipmentUnits.getValue());
      BigDecimal addedAgeWeight = addedEquipment.max(BigDecimal.ZERO); // limit to [0,]

      // Calculate weighted ages
      BigDecimal priorAgeYears = currentAge.getValue().add(BigDecimal.ONE); // age + 1 year
      BigDecimal priorAgeWeighted = priorAgeYears.multiply(priorAgeWeight);
      BigDecimal addedAgeWeighted = BigDecimal.ONE.multiply(addedAgeWeight); // 1 year * weight

      // Calculate new average age
      BigDecimal totalWeight = priorAgeWeight.add(addedAgeWeight);
      BigDecimal newAge;
      if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
        newAge = BigDecimal.ZERO; // Avoid division by zero
      } else {
        newAge = priorAgeWeighted.add(addedAgeWeighted).divide(totalWeight, 10, java.math.RoundingMode.HALF_UP);
      }

      setSimpleStream(useKey, "age", new EngineNumber(newAge, "years"));
    }

    // Reset state at timestep for all parameterizations
    for (StreamParameterization parameterization : substances.values()) {
      parameterization.resetStateAtTimestep();
    }

    // Redistribute recycling back to sales streams before clearing to prevent cross-year deficit
    redistributeRecyclingToSales();

    // Subtract induction from virgin streams before year transition
    redistributeInductionFromSales();

    // Reset recycling streams at year boundary to prevent stale values
    // from affecting subsequent cap operations
    for (String key : substances.keySet()) {
      String[] keyPieces = key.split("\t");
      String application = keyPieces[0];
      String substance = keyPieces[1];

      SimpleUseKey useKey = new SimpleUseKey(application, substance);
      setSimpleStream(useKey, "recycleRecharge", new EngineNumber(BigDecimal.ZERO, "kg"));
      setSimpleStream(useKey, "recycleEol", new EngineNumber(BigDecimal.ZERO, "kg"));
      // Reset induction streams at year boundary to prevent cross-year accumulation
      setSimpleStream(useKey, "inductionEol", new EngineNumber(BigDecimal.ZERO, "kg"));
      setSimpleStream(useKey, "inductionRecharge", new EngineNumber(BigDecimal.ZERO, "kg"));
    }

  }

  /**
   * Set the greenhouse gas intensity for a key.
   *
   * @param useKey The key containing application and substance
   * @param newValue The new GHG intensity value
   */
  public void setGhgIntensity(UseKey useKey, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setGhgIntensity(newValue);
  }

  /**
   * Set the energy intensity for a key.
   *
   * @param useKey The key containing application and substance
   * @param newValue The new energy intensity value
   */
  public void setEnergyIntensity(UseKey useKey, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setEnergyIntensity(newValue);
  }

  /**
   * Get the greenhouse gas intensity for a key.
   *
   * @param useKey The key containing application and substance
   * @return The GHG intensity value
   */
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

  /**
   * Get the energy intensity for a key.
   *
   * @param useKey The key containing application and substance
   * @return The energy intensity value
   */
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

  /**
   * Set the initial charge for a key's stream.
   *
   * @param useKey The key containing application and substance
   * @param substream The stream identifier ('domestic' or 'import')
   * @param newValue The new initial charge value
   */
  public void setInitialCharge(UseKey useKey, String substream, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setInitialCharge(substream, newValue);
  }

  /**
   * Get the initial charge for a key.
   *
   * @param useKey The key containing application and substance
   * @param substream The substream name
   * @return The initial charge value
   */
  public EngineNumber getInitialCharge(UseKey useKey, String substream) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getInitialCharge(substream);
  }

  /**
   * Set the recharge population percentage for a key.
   *
   * @param useKey The key containing application and substance
   * @param newValue The new recharge population value
   */
  public void setRechargePopulation(UseKey useKey, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setRechargePopulation(newValue);
  }

  /**
   * Get the recharge population percentage for a key.
   *
   * @param useKey The key containing application and substance
   * @return The current recharge population value
   */
  public EngineNumber getRechargePopulation(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getRechargePopulation();
  }

  /**
   * Set the recharge intensity for a key.
   *
   * @param useKey The key containing application and substance
   * @param newValue The new recharge intensity value
   */
  public void setRechargeIntensity(UseKey useKey, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setRechargeIntensity(newValue);
  }

  /**
   * Get the recharge intensity for a key.
   *
   * @param useKey The key containing application and substance
   * @return The current recharge intensity value
   */
  public EngineNumber getRechargeIntensity(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getRechargeIntensity();
  }

  /**
   * Accumulate recharge parameters. Sets when not previously set, accumulates otherwise.
   *
   * <p>Multiple calls accumulate rates (addition) and intensities (weighted-average).
   * Rates add linearly and intensities use weighted-average with absolute value weights
   * to handle negative adjustments correctly.</p>
   *
   * @param useKey The key containing application and substance
   * @param population The recharge population rate to add
   * @param intensity The recharge intensity for this rate
   */
  public void accumulateRecharge(UseKey useKey, EngineNumber population, EngineNumber intensity) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.accumulateRecharge(population, intensity);
  }

  /**
   * Get the recharge base population for cumulative calculations.
   *
   * @param useKey The key containing application and substance
   * @return The base population, or null if not yet captured this year
   */
  public Optional<EngineNumber> getRechargeBasePopulation(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getRechargeBasePopulation();
  }

  /**
   * Set the recharge base population for cumulative calculations.
   *
   * @param useKey The key containing application and substance
   * @param value The base population value
   */
  public void setRechargeBasePopulation(UseKey useKey, EngineNumber value) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setRechargeBasePopulation(value);
  }

  /**
   * Get the applied recharge amount for cumulative calculations.
   *
   * @param useKey The key containing application and substance
   * @return The total amount already recharged this year in kg
   */
  public Optional<EngineNumber> getAppliedRechargeAmount(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getAppliedRechargeAmount();
  }

  /**
   * Set the applied recharge amount for cumulative calculations.
   *
   * @param useKey The key containing application and substance
   * @param value The total amount recharged this year in kg
   */
  public void setAppliedRechargeAmount(UseKey useKey, EngineNumber value) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setAppliedRechargeAmount(value);
  }

  /**
   * Get whether recycling has been calculated this step.
   *
   * @param useKey The key containing application and substance
   * @return true if recycling was calculated, false otherwise
   */
  public boolean isRecyclingCalculatedThisStep(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.isRecyclingCalculatedThisStep();
  }

  /**
   * Set whether recycling has been calculated this step.
   *
   * @param useKey The key containing application and substance
   * @param calculated true if recycling was calculated, false otherwise
   */
  public void setRecyclingCalculatedThisStep(UseKey useKey, boolean calculated) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setRecyclingCalculatedThisStep(calculated);
  }

  /**
   * Set the recovery rate percentage for a key.
   *
   * <p>If a recovery rate is already set, this method implements additive recycling:
   * - Recovery rates are added together</p>
   *
   * @param useKey The key containing application and substance
   * @param newValue The new recovery rate value
   */
  public void setRecoveryRate(UseKey useKey, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(useKey);

    // Get existing recovery rate
    EngineNumber existingRecovery = parameterization.getRecoveryRate();

    // If existing recovery rate is non-zero, implement additive recycling
    if (existingRecovery.getValue().compareTo(BigDecimal.ZERO) > 0) {
      // Convert both rates to the same units (percentage)
      EngineNumber existingRecoveryPercent = unitConverter.convert(existingRecovery, "%");
      EngineNumber newRecoveryPercent = unitConverter.convert(newValue, "%");

      // Add recovery rates
      BigDecimal combinedRecovery = existingRecoveryPercent.getValue().add(newRecoveryPercent.getValue());

      // Set the combined recovery rate
      parameterization.setRecoveryRate(new EngineNumber(combinedRecovery, "%"));
    } else {
      // First recovery rate, set normally
      parameterization.setRecoveryRate(newValue);
    }
  }

  /**
   * Set the recovery rate percentage for a key with a specific stage.
   *
   * @param useKey The key containing application and substance
   * @param newValue The new recovery rate value
   * @param stage The recovery stage (EOL or RECHARGE)
   */
  public void setRecoveryRate(UseKey useKey, EngineNumber newValue, RecoveryStage stage) {
    StreamParameterization parameterization = getParameterization(useKey);

    // Get existing recovery rate for this stage
    EngineNumber existingRecovery = parameterization.getRecoveryRate(stage);

    // Check if recovery rate is already set - use additive behavior for multiple recover commands
    if (existingRecovery.getValue().compareTo(BigDecimal.ZERO) > 0) {
      // Add the new recovery rate to the existing one (additive behavior)
      BigDecimal newRate = existingRecovery.getValue().add(newValue.getValue());
      EngineNumber combinedRate = new EngineNumber(newRate, "%");
      parameterization.setRecoveryRate(combinedRate, stage);
      return; // Early return to avoid setting the rate again below
    }

    // Set the recovery rate (first one for this timestep)
    parameterization.setRecoveryRate(newValue, stage);
  }

  /**
   * Get the recovery rate percentage for a key.
   *
   * @param useKey The key containing application and substance
   * @return The current recovery rate value
   */
  public EngineNumber getRecoveryRate(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getRecoveryRate();
  }

  /**
   * Get the recovery rate percentage for a key with a specific stage.
   *
   * @param useKey The key containing application and substance
   * @param stage The recovery stage (EOL or RECHARGE)
   * @return The current recovery rate value
   */
  public EngineNumber getRecoveryRate(UseKey useKey, RecoveryStage stage) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getRecoveryRate(stage);
  }


  /**
   * Set the yield rate percentage for recycling for a key.
   *
   * <p>If a yield rate is already set and recovery rate is non-zero, this method implements
   * weighted average yield calculation based on recovery rates.</p>
   *
   * @param useKey The key containing application and substance
   * @param newValue The new yield rate value
   */
  public void setYieldRate(UseKey useKey, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(useKey);

    // Get existing yield and recovery rates
    EngineNumber existingYield = parameterization.getYieldRate();
    EngineNumber existingRecovery = parameterization.getRecoveryRate();

    // If existing yield is non-zero and recovery rate is non-zero, calculate weighted average
    if (existingYield.getValue().compareTo(BigDecimal.ZERO) > 0
        && existingRecovery.getValue().compareTo(BigDecimal.ZERO) > 0) {

      // For weighted average, we need to know the recovery rate components
      // Since recovery rates were already combined in setRecoveryRate, we need to estimate
      // the new recovery rate component from the context

      // Convert yield rates to the same units (percentage)
      EngineNumber existingYieldPercent = unitConverter.convert(existingYield, "%");
      EngineNumber newYieldPercent = unitConverter.convert(newValue, "%");

      // For simplicity, assume equal weighting if we can't determine recovery components
      // This is a reasonable approximation for the weighted average
      BigDecimal combinedYield = existingYieldPercent.getValue().add(newYieldPercent.getValue()).divide(
          BigDecimal.valueOf(2), java.math.MathContext.DECIMAL128);

      // Set the combined yield rate
      parameterization.setYieldRate(new EngineNumber(combinedYield, "%"));
    } else {
      // First yield rate or no existing recovery, set normally
      parameterization.setYieldRate(newValue);
    }
  }

  /**
   * Set the yield rate percentage for recycling for a key with a specific stage.
   *
   * @param useKey The key containing application and substance
   * @param newValue The new yield rate value
   * @param stage The recovery stage (EOL or RECHARGE)
   */
  public void setYieldRate(UseKey useKey, EngineNumber newValue, RecoveryStage stage) {
    StreamParameterization parameterization = getParameterization(useKey);

    // Get existing yield and recovery rates for this stage
    EngineNumber existingYield = parameterization.getYieldRate(stage);
    EngineNumber existingRecovery = parameterization.getRecoveryRate(stage);

    // If existing yield rate is non-zero, implement additive recycling
    if (existingYield.getValue().compareTo(BigDecimal.ZERO) > 0) {
      // Convert both rates to the same units (percentage)
      EngineNumber existingYieldPercent = unitConverter.convert(existingYield, "%");
      EngineNumber newYieldPercent = unitConverter.convert(newValue, "%");

      // For yield rates, we need to handle combining them properly
      // Since they represent efficiency rates, we'll use a weighted average approach
      // This is a reasonable approximation for the weighted average
      BigDecimal combinedYield = existingYieldPercent.getValue().add(newYieldPercent.getValue()).divide(
          BigDecimal.valueOf(2), java.math.MathContext.DECIMAL128);

      // Set the combined yield rate
      parameterization.setYieldRate(new EngineNumber(combinedYield, "%"), stage);
    } else {
      // First yield rate or no existing recovery, set normally
      parameterization.setYieldRate(newValue, stage);
    }
  }

  /**
   * Get the yield rate percentage for recycling for a key.
   *
   * @param useKey The key containing application and substance
   * @return The current yield rate value
   */
  public EngineNumber getYieldRate(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getYieldRate();
  }

  /**
   * Get the yield rate percentage for recycling for a key with a specific stage.
   *
   * @param useKey The key containing application and substance
   * @param stage The recovery stage (EOL or RECHARGE)
   * @return The current yield rate value
   */
  public EngineNumber getYieldRate(UseKey useKey, RecoveryStage stage) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getYieldRate(stage);
  }

  /**
   * Set the induction rate percentage for recycling for a key.
   *
   * @param useKey The key containing application and substance
   * @param newValue The new induction rate value
   */
  public void setInductionRate(UseKey useKey, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setInductionRate(newValue);
  }

  /**
   * Set the induction rate percentage for recycling for a key with a specific stage.
   *
   * @param useKey The key containing application and substance
   * @param newValue The new induction rate value
   * @param stage The recovery stage (EOL or RECHARGE)
   */
  public void setInductionRate(UseKey useKey, EngineNumber newValue, RecoveryStage stage) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setInductionRate(newValue, stage);
  }

  /**
   * Get the induction rate percentage for recycling for a key.
   *
   * @param useKey The key containing application and substance
   * @return The current induction rate value
   */
  public EngineNumber getInductionRate(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getInductionRate();
  }

  /**
   * Get the induction rate percentage for recycling for a key with a specific stage.
   *
   * @param useKey The key containing application and substance
   * @param stage The recovery stage (EOL or RECHARGE)
   * @return The current induction rate value
   */
  public EngineNumber getInductionRate(UseKey useKey, RecoveryStage stage) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getInductionRate(stage);
  }

  /**
   * Set the retirement rate percentage for a key.
   *
   * @param useKey The key containing application and substance
   * @param newValue The new retirement rate value
   */
  public void setRetirementRate(UseKey useKey, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setRetirementRate(newValue);
  }

  /**
   * Get the retirement rate percentage for a key.
   *
   * @param useKey The key containing application and substance
   * @return The current retirement rate value
   */
  public EngineNumber getRetirementRate(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getRetirementRate();
  }

  /**
   * Get the retirement base population for cumulative calculations.
   *
   * @param useKey The key containing application and substance
   * @return The base population, or null if not yet captured
   */
  public Optional<EngineNumber> getRetirementBasePopulation(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getRetirementBasePopulation();
  }

  /**
   * Set the retirement base population for cumulative calculations.
   *
   * @param useKey The key containing application and substance
   * @param value The base population value
   */
  public void setRetirementBasePopulation(UseKey useKey, EngineNumber value) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setRetirementBasePopulation(value);
  }

  /**
   * Get the applied retirement amount for cumulative calculations.
   *
   * @param useKey The key containing application and substance
   * @return The total amount already retired this year
   */
  public Optional<EngineNumber> getAppliedRetirementAmount(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getAppliedRetirementAmount();
  }

  /**
   * Set the applied retirement amount for cumulative calculations.
   *
   * @param useKey The key containing application and substance
   * @param value The total amount retired this year
   */
  public void setAppliedRetirementAmount(UseKey useKey, EngineNumber value) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setAppliedRetirementAmount(value);
  }

  /**
   * Get the replacement mode for retire commands this step.
   *
   * @param useKey The key containing application and substance
   * @return null if no retire yet, true if with replacement, false if without replacement
   */
  public boolean getHasReplacementThisStep(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getHasReplacementThisStep();
  }

  /**
   * Set the replacement mode for retire commands this step.
   *
   * @param useKey The key containing application and substance
   * @param value true for with replacement, false for without replacement
   */
  public void setHasReplacementThisStep(UseKey useKey, boolean value) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setHasReplacementThisStep(value);
  }

  /**
   * Get whether retire has been calculated this step.
   *
   * @param useKey The key containing application and substance
   * @return true if retire was calculated, false otherwise
   */
  public boolean getRetireCalculatedThisStep(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getRetireCalculatedThisStep();
  }

  /**
   * Set whether retire has been calculated this step.
   *
   * @param useKey The key containing application and substance
   * @param calculated true if retire was calculated, false otherwise
   */
  public void setRetireCalculatedThisStep(UseKey useKey, boolean calculated) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setRetireCalculatedThisStep(calculated);
  }

  /**
   * Set the last specified value for a stream.
   *
   * <p>This tracks the value and units last used when setting streams
   * to preserve user intent across carry-over years.</p>
   *
   * @param useKey The key containing application and substance
   * @param streamName The name of the stream
   * @param value The last specified value with units
   */
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

  /**
   * Get the last specified value for a stream.
   *
   * @param useKey The key containing application and substance
   * @param streamName The name of the stream
   * @return The last specified value with units, or null if not set
   */
  public EngineNumber getLastSpecifiedValue(UseKey useKey, String streamName) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getLastSpecifiedValue(streamName);
  }

  /**
   * Check if a stream has a last specified value.
   *
   * @param useKey The key containing application and substance
   * @param streamName The name of the stream
   * @return true if the stream has a last specified value, false otherwise
   */
  public boolean hasLastSpecifiedValue(UseKey useKey, String streamName) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.hasLastSpecifiedValue(streamName);
  }

  /**
   * Check if sales intent has been freshly set for the given scope.
   *
   * @param useKey The key containing application and substance
   * @return true if sales intent was freshly set, false otherwise
   */
  public boolean isSalesIntentFreshlySet(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.isSalesIntentFreshlySet();
  }

  /**
   * Reset the sales intent flag for the given scope.
   *
   * @param useKey The key containing application and substance
   */
  public void resetSalesIntentFlag(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setSalesIntentFreshlySet(false);
  }


  /**
   * Check if a stream has ever been enabled (set to non-zero value).
   *
   * @param useKey The key containing application and substance
   * @param streamName The name of the stream to check
   * @return true if the stream has been enabled, false otherwise
   */
  public boolean hasStreamBeenEnabled(UseKey useKey, String streamName) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.hasStreamBeenEnabled(streamName);
  }

  /**
   * Mark a stream as having been enabled (set to non-zero value).
   *
   * @param useKey The key containing application and substance
   * @param streamName The name of the stream to mark as enabled
   */
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
    String key = getKey(scope);
    StreamParameterization result = substances.get(key);
    if (result == null) {
      throwSubstanceMissing(
          "getParameterization",
          scope.getApplication(),
          scope.getSubstance()
      );
    }
    return result;
  }

  /**
   * Generate a key for a UseKey.
   *
   * @param useKey The UseKey to generate a key for
   * @return The generated key
   */
  private String getKey(UseKey useKey) {
    return useKey.getKey();
  }

  /**
   * Generate a stream key for a Scope and stream name.
   *
   * @param useKey The Scope to generate a key for
   * @param name The stream name
   * @return The generated stream key
   */
  private String getKey(UseKey useKey, String name) {
    StringBuilder keyBuilder = new StringBuilder();
    keyBuilder.append(getKey(useKey));
    keyBuilder.append("\t");
    keyBuilder.append(name != null ? name : "-");
    return keyBuilder.toString();
  }

  /**
   * Sets a simple stream by converting the provided value to the appropriate units and storing it
   * in the streams map with a key generated from the given parameters. If the converted value
   * is NaN, an exception is thrown indicating the source of the issue.
   *
   * @param useKey An instance of UseKey that helps determine stream-specific characteristics
   *               for generating the stream key.
   * @param name A string representing the name of the stream or parameter to be processed.
   * @param value An instance of EngineNumber that contains the numerical value to be converted
   *              and stored in the appropriate stream.
   */
  private void setSimpleStream(UseKey useKey, String name, EngineNumber value) {
    String unitsNeeded = getUnits(name);
    EngineNumber valueConverted = unitConverter.convert(value, unitsNeeded);

    if (CHECK_NAN_STATE && valueConverted.getValue().toString().equals("NaN")) {
      String key = getKey(useKey);
      String[] keyPieces = key.split("\t");
      String application = keyPieces.length > 0 ? keyPieces[0] : "";
      String substance = keyPieces.length > 1 ? keyPieces[1] : "";
      String pieces = String.join(" > ",
          "-".equals(application) ? "null" : application,
          "-".equals(substance) ? "null" : substance,
          name);
      throw new RuntimeException("Encountered NaN after conversion for: " + pieces);
    }

    String streamKey = getKey(useKey, name);
    streams.put(streamKey, valueConverted);
  }

  /**
   * Configures and sets the sales stream distribution for manufacturing and import
   * based on the provided key, name, and engine number value. The provided value
   * is converted to kilograms and further distributed according to pre-defined
   * distribution percentages.
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

    // Get current recycle amounts to determine proportional distribution
    EngineNumber recycleRechargeAmountRaw = getStream(useKey, "recycleRecharge");
    EngineNumber recycleEolAmountRaw = getStream(useKey, "recycleEol");

    EngineNumber recycleRechargeAmount = unitConverter.convert(recycleRechargeAmountRaw, "kg");
    EngineNumber recycleEolAmount = unitConverter.convert(recycleEolAmountRaw, "kg");

    BigDecimal recycleRechargeKg = recycleRechargeAmount != null ? recycleRechargeAmount.getValue() : BigDecimal.ZERO;
    BigDecimal recycleEolKg = recycleEolAmount != null ? recycleEolAmount.getValue() : BigDecimal.ZERO;

    BigDecimal totalExistingRecycle = recycleRechargeKg.add(recycleEolKg);

    // Calculate proportional distribution
    BigDecimal newRecycleRechargeAmount;
    BigDecimal newRecycleEolAmount;

    if (totalExistingRecycle.compareTo(BigDecimal.ZERO) == 0) {
      // If no existing recycle, split equally
      newRecycleRechargeAmount = totalRecycleKg.divide(new BigDecimal("2"));
      newRecycleEolAmount = totalRecycleKg.divide(new BigDecimal("2"));
    } else {
      // Distribute proportionally based on existing amounts
      BigDecimal rechargePercent = recycleRechargeKg.divide(totalExistingRecycle, 10, BigDecimal.ROUND_HALF_UP);
      BigDecimal eolPercent = recycleEolKg.divide(totalExistingRecycle, 10, BigDecimal.ROUND_HALF_UP);

      newRecycleRechargeAmount = totalRecycleKg.multiply(rechargePercent);
      newRecycleEolAmount = totalRecycleKg.multiply(eolPercent);
    }

    EngineNumber recycleRechargeAmountToSet = new EngineNumber(newRecycleRechargeAmount, "kg");
    EngineNumber recycleEolAmountToSet = new EngineNumber(newRecycleEolAmount, "kg");

    setSimpleStream(useKey, "recycleRecharge", recycleRechargeAmountToSet);
    setSimpleStream(useKey, "recycleEol", recycleEolAmountToSet);
  }

  /**
   * Sets the sales stream with units for a specific use key and name. This method
   * converts the initial charge and input value to specified units, validates the charge,
   * and updates the internal state to reflect the conversions. The resulting stream is then
   * stored with the corresponding key.
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
    if (initialCharge.getValue().compareTo(BigDecimal.ZERO) == 0) {
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
    StringBuilder message = new StringBuilder();
    message.append("Not a known application substance pair in ");
    message.append(context);
    message.append(": ");
    message.append(application);
    message.append(", ");
    message.append(substance);
    throw new IllegalStateException(message.toString());
  }

  /**
   * Verify that a stream name is valid.
   *
   * @param name The stream name to verify
   * @throws IllegalArgumentException If the stream name is not recognized
   */
  private void ensureStreamKnown(String name) {
    if (EngineConstants.getBaseUnits(name) == null) {
      throw new IllegalArgumentException("Unknown stream: " + name);
    }
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
    boolean isSalesComponent = "domestic".equals(name) || "import".equals(name)
                               || "sales".equals(name);
    boolean isUnits = value.getUnits().startsWith("unit");
    return isSalesComponent && isUnits;
  }

  /**
   * Calculate the current recycling amount using the current population context.
   * This method replicates the recycling calculation from SalesRecalcStrategy
   * but uses the current population state instead of relying on stale data.
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
          BigDecimal.valueOf(100), java.math.MathContext.DECIMAL128);
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
          BigDecimal.valueOf(100), java.math.MathContext.DECIMAL128);
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
          BigDecimal.valueOf(100), java.math.MathContext.DECIMAL128);
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
   * correctly displaces virgin material in Year N, but the reduced virgin sales
   * baseline incorrectly carries forward to Year N+1, creating cumulative deficit.</p>
   *
   * <p>This fix is applied to all scenarios with configured sales streams including:</p>
   * <ul>
   * <li>"set sales to X [units]" - Total sales specified</li>
   * <li>"set import to X [units]" - Import volume specified</li>
   * <li>"set domestic to X [units]" - Domestic volume specified</li>
   * </ul>
   *
   * <p>The redistribution preserves user expectations that loss of recycling will be
   * back-filled by virgin material to maintain total available material, regardless
   * of whether the original specification was in mass units (kg, mt) or equipment units.</p>
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

      // Set new amounts using direct stream setting to avoid circular dependency
      // Use setSimpleStream since this is internal redistribution logic
      setSimpleStream(useKey, "domestic", new EngineNumber(newDomestic, "kg"));
      setSimpleStream(useKey, "import", new EngineNumber(newImport, "kg"));
    }
  }

  /**
   * Redistribute induction amounts from sales streams before year transition.
   *
   * <p>This method addresses the cross-year induction carryover issue where induction
   * correctly adds to virgin material in Year N, but the increased virgin sales
   * baseline incorrectly carries forward to Year N+1, creating cumulative surplus.</p>
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
   * captured bases are proportionally scaled with applied amounts to maintain
   * cumulative semantics. Retirement and recharge bases scale independently.</p>
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

    // Guard against division by zero
    if (retireBase.getValue().compareTo(BigDecimal.ZERO) == 0) {
      param.setRetirementBasePopulation(newPriorUnits);
      param.setAppliedRetirementAmount(new EngineNumber(BigDecimal.ZERO, "units"));
    } else {
      // Calculate what percentage was already applied
      BigDecimal retirePercent = appliedRetire.getValue().divide(
          retireBase.getValue(), 10, java.math.RoundingMode.HALF_UP);

      // Scale applied amount proportionally to new base
      BigDecimal newApplied = newPriorUnits.getValue().multiply(retirePercent);

      // Update base and applied
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

    // Guard against division by zero
    if (rechargeBase.getValue().compareTo(BigDecimal.ZERO) == 0) {
      param.setRechargeBasePopulation(newPriorUnits);
      param.setAppliedRechargeAmount(new EngineNumber(BigDecimal.ZERO, "kg"));
    } else {
      // Scale both base and applied by the same ratio
      BigDecimal baseRatio = newPriorUnits.getValue().divide(
          rechargeBase.getValue(), 10, java.math.RoundingMode.HALF_UP);

      // Scale applied amount by same ratio
      BigDecimal newApplied = appliedRecharge.getValue().multiply(baseRatio);

      // Update base and applied
      param.setRechargeBasePopulation(newPriorUnits);
      param.setAppliedRechargeAmount(new EngineNumber(newApplied, "kg"));
    }
  }

}
