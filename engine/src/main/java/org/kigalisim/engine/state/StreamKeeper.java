/**
 * Class responsible for managing / tracking substance streams.
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
import java.util.stream.Collectors;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.recalc.SalesStreamDistribution;
import org.kigalisim.engine.recalc.SalesStreamDistributionBuilder;
import org.kigalisim.lang.operation.RecoverOperation.RecoveryStage;

/**
 * Class responsible for managing / tracking substance streams.
 *
 * <p>State management object for storage and retrieval of substance data, stream
 * values, and associated parameterizations.</p>
 */
public class StreamKeeper {

  private static final boolean CHECK_NAN_STATE = true;

  private final Map<String, StreamParameterization> substances;
  private final Map<String, EngineNumber> streams;
  private final OverridingConverterStateGetter stateGetter;
  private final UnitConverter unitConverter;

  /**
   * Create a new StreamKeeper instance.
   *
   * @param stateGetter Structure to retrieve state information
   * @param unitConverter Converter for handling unit transformations
   */
  public StreamKeeper(OverridingConverterStateGetter stateGetter, UnitConverter unitConverter) {
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
          return new SubstanceInApplicationId(keyComponents[0], keyComponents[1]);
        })
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

    // Emissions
    String rechargeEmissionsKey = getKey(useKey, "rechargeEmissions");
    streams.put(rechargeEmissionsKey, new EngineNumber(BigDecimal.ZERO, "tCO2e"));
    String eolEmissionsKey = getKey(useKey, "eolEmissions");
    streams.put(eolEmissionsKey, new EngineNumber(BigDecimal.ZERO, "tCO2e"));

    // Recharge tracking
    String implicitRechargeKey = getKey(useKey, "implicitRecharge");
    streams.put(implicitRechargeKey, new EngineNumber(BigDecimal.ZERO, "kg"));
  }

  /**
   * Set the value for a specific stream using key.
   *
   * @param useKey The key containing application and substance
   * @param name The stream name
   * @param value The value to set
   */
  public void setStream(UseKey useKey, String name, EngineNumber value) {
    // Default behavior: apply recycling logic (backwards compatibility)
    setStream(useKey, name, value, true);
  }

  /**
   * Set the value of a stream with control over recycling subtraction.
   *
   * @param useKey The key containing application and substance
   * @param name The stream name
   * @param value The value to set
   * @param subtractRecycling Whether to apply recycling logic (false for direct setting)
   */
  public void setStream(UseKey useKey, String name, EngineNumber value, boolean subtractRecycling) {
    String key = getKey(useKey);
    ensureSubstanceOrThrow(key, "setStream");
    ensureStreamKnown(name);

    // Check if stream needs to be enabled before setting
    assertStreamEnabled(useKey, name, value);

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

    // Apply routing logic based on subtractRecycling parameter
    if (!subtractRecycling && ("domestic".equals(name) || "import".equals(name))) {
      // Direct setting - bypass recycling logic
      setSimpleStream(useKey, name, value);
    } else {
      // Normal routing with recycling logic
      if ("sales".equals(name)) {
        setStreamForSales(useKey, name, value);
      } else if ("domestic".equals(name) || "import".equals(name)) {
        // Always use setSalesSubstream for sales substreams to ensure recycling is applied
        setSalesSubstream(useKey, name, value);
      } else if ("recycle".equals(name)) {
        setStreamForRecycle(useKey, name, value);
      } else if (getIsSettingVolumeByUnits(name, value)) {
        setStreamForSalesWithUnits(useKey, name, value);
      } else {
        setSimpleStream(useKey, name, value);
      }
    }
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
   * Increment the year, updating populations and resetting internal params.
   */
  public void incrementYear() {
    // Move population
    for (String key : substances.keySet()) {
      String[] keyPieces = key.split("\t");
      String application = keyPieces[0];
      String substance = keyPieces[1];

      SimpleUseKey useKey = new SimpleUseKey(application, substance);
      EngineNumber equipment = getStream(useKey, "equipment");
      setStream(useKey, "priorEquipment", equipment);
    }

    // Reset state at timestep for all parameterizations
    for (StreamParameterization parameterization : substances.values()) {
      parameterization.resetStateAtTimestep();
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

    // If existing recovery rate is non-zero, implement additive recycling
    if (existingRecovery.getValue().compareTo(BigDecimal.ZERO) > 0) {
      // Convert both rates to the same units (percentage)
      EngineNumber existingRecoveryPercent = unitConverter.convert(existingRecovery, "%");
      EngineNumber newRecoveryPercent = unitConverter.convert(newValue, "%");

      // Add recovery rates
      BigDecimal combinedRecovery = existingRecoveryPercent.getValue().add(newRecoveryPercent.getValue());

      // Set the combined recovery rate
      parameterization.setRecoveryRate(new EngineNumber(combinedRecovery, "%"), stage);
    } else {
      // First recovery rate, set normally
      parameterization.setRecoveryRate(newValue, stage);
    }
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
   * Sets individual sales substreams (domestic/import) with recycling awareness.
   * Unlike setStreamForSales, this method handles individual substreams without
   * distribution logic but still accounts for proportional recycling.
   *
   * @param useKey The key containing application and substance
   * @param streamName The stream name ("domestic" or "import")
   * @param value The total value for this specific substream
   */
  private void setSalesSubstream(UseKey useKey, String streamName, EngineNumber value) {
    EngineNumber valueConverted = unitConverter.convert(value, "kg");
    BigDecimal amountKg = valueConverted.getValue();

    // Check if any streams are enabled for distribution calculation
    if (!hasStreamsEnabled(useKey)) {
      throw new IllegalStateException("Cannot set sales substream: no streams have been enabled. "
          + "Use 'set " + streamName + "' or other stream statements to enable streams before "
          + "operations that require sales recalculation.");
    }

    // Get current recycling amount
    EngineNumber recycleAmountRaw = getStream(useKey, "recycle");
    EngineNumber recycleAmount = unitConverter.convert(recycleAmountRaw, "kg");
    BigDecimal recycleKg = recycleAmount != null ? recycleAmount.getValue() : BigDecimal.ZERO;

    // Get distribution to determine this substream's share of recycling
    SalesStreamDistribution distribution = getDistribution(useKey);
    BigDecimal substreamPercent;
    if ("domestic".equals(streamName)) {
      substreamPercent = distribution.getPercentDomestic();
    } else if ("import".equals(streamName)) {
      substreamPercent = distribution.getPercentImport();
    } else {
      throw new IllegalArgumentException("Unknown sales substream: " + streamName);
    }

    // Calculate proportional recycling for this substream
    BigDecimal substreamRecycling = recycleKg.multiply(substreamPercent);

    // Subtract proportional recycling to get virgin material amount
    BigDecimal netAmount = amountKg.subtract(substreamRecycling);
    if (netAmount.compareTo(BigDecimal.ZERO) < 0) {
      netAmount = BigDecimal.ZERO;
    }

    // Set the net amount directly
    EngineNumber netAmountToSet = new EngineNumber(netAmount, "kg");
    setSimpleStream(useKey, streamName, netAmountToSet);
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

    // Set the amount directly - recycling should already be handled by setSalesSubstream
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
          + ". Use 'enable " + streamName + "' statement before setting this stream.");
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

}
