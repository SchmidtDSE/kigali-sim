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

/**
 * Class responsible for managing / tracking substance streams.
 *
 * <p>State management object for storage and retrieval of substance data, stream
 * values, and associated parameterizations.</p>
 */
public class StreamKeeper {

  private static final boolean CHECK_NAN_STATE = true;
  private static final boolean CHECK_POSITIVE_STREAMS = true;

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
    if (hasSubstance(useKey)) {
      return;
    }

    String key = getKey(useKey);
    substances.put(key, new StreamParameterization());

    // Sales: manufacture, import, recycle
    String manufactureKey = getKey(useKey, "manufacture");
    streams.put(manufactureKey, new EngineNumber(BigDecimal.ZERO, "kg"));
    String importKey = getKey(useKey, "import");
    streams.put(importKey, new EngineNumber(BigDecimal.ZERO, "kg"));
    String recycleKey = getKey(useKey, "recycle");
    streams.put(recycleKey, new EngineNumber(BigDecimal.ZERO, "kg"));

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

    // Emissions
    String rechargeEmissionsKey = getKey(useKey, "rechargeEmissions");
    streams.put(rechargeEmissionsKey, new EngineNumber(BigDecimal.ZERO, "tCO2e"));
    String eolEmissionsKey = getKey(useKey, "eolEmissions");
    streams.put(eolEmissionsKey, new EngineNumber(BigDecimal.ZERO, "tCO2e"));
  }

  /**
   * Set the value for a specific stream using key.
   *
   * @param useKey The key containing application and substance
   * @param name The stream name
   * @param value The value to set
   */
  public void setStream(UseKey useKey, String name, EngineNumber value) {
    String key = getKey(useKey);
    ensureSubstancePresent(key, "setStream");
    ensureStreamKnown(name);

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

    if (getIsSettingVolumeByUnits(name, value)) {
      setStreamForSalesWithUnits(useKey, name, value);
    } else if ("sales".equals(name)) {
      setStreamForSales(useKey, name, value);
    } else {
      setSimpleStream(useKey, name, value);
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
    
    if (substances.get(key) == null) {
      String[] keyPieces = key.split("\t");
      String application = keyPieces.length > 0 ? keyPieces[0] : "";
      String substance = keyPieces.length > 1 ? keyPieces[1] : "";
      StringBuilder message = new StringBuilder();
      message.append("Not a known application substance pair in getStream: ");
      message.append("-".equals(application) ? "null" : application);
      message.append(", ");
      message.append("-".equals(substance) ? "null" : substance);
      throw new IllegalStateException(message.toString());
    }
    
    ensureStreamKnown(name);

    if ("sales".equals(name)) {
      EngineNumber manufactureAmountRaw = getStream(useKey, "manufacture");
      EngineNumber importAmountRaw = getStream(useKey, "import");
      EngineNumber recycleAmountRaw = getStream(useKey, "recycle");

      EngineNumber manufactureAmount = unitConverter.convert(manufactureAmountRaw, "kg");
      EngineNumber importAmount = unitConverter.convert(importAmountRaw, "kg");
      EngineNumber recycleAmount = unitConverter.convert(recycleAmountRaw, "kg");

      BigDecimal manufactureAmountValue = manufactureAmount.getValue();
      BigDecimal importAmountValue = importAmount.getValue();
      BigDecimal recycleAmountValue = recycleAmount.getValue();

      BigDecimal newTotal = manufactureAmountValue.add(importAmountValue).add(recycleAmountValue);

      return new EngineNumber(newTotal, "kg");
    } else {
      String streamKey = getKey(useKey, name);
      EngineNumber result = streams.get(streamKey);
      if (result == null) {
        throw new IllegalArgumentException("Unknown stream: " + name);
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

      substances.get(key).resetInternals();
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
    ensureSubstancePresent(key, "getGhgIntensity");
    StreamParameterization parameterization = substances.get(key);
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
    ensureSubstancePresent(key, "getEnergyIntensity");
    StreamParameterization parameterization = substances.get(key);
    return parameterization.getEnergyIntensity();
  }

  /**
   * Set the initial charge for a key's stream.
   *
   * @param useKey The key containing application and substance
   * @param substream The stream identifier ('manufacture' or 'import')
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
    String key = getKey(useKey);
    StreamParameterization parameterization = getParameterization(key);
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
   * @param useKey The key containing application and substance
   * @param newValue The new recovery rate value
   */
  public void setRecoveryRate(UseKey useKey, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setRecoveryRate(newValue);
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
   * Set the displacement rate percentage for a substance in an application.
   *
   * @param key The key containing application and substance for which displacement rate is to be set
   * @param newValue The new displacement rate value
   */
  public void setDisplacementRate(UseKey key, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(key);
    parameterization.setDisplacementRate(newValue);
  }

  /**
   * Get the displacement rate percentage for a substance in an application.
   *
   * @param key The key containing application and substance for which displacement rate is to be retrieved
   * @return The current displacement rate value
   */
  public EngineNumber getDisplacementRate(UseKey key) {
    StreamParameterization parameterization = getParameterization(key);
    return parameterization.getDisplacementRate();
  }


  /**
   * Set the yield rate percentage for recycling for a key.
   *
   * @param useKey The key containing application and substance
   * @param newValue The new yield rate value
   */
  public void setYieldRate(UseKey useKey, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(useKey);
    parameterization.setYieldRate(newValue);
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
   * Set the last specified units for a key.
   *
   * @param useKey The key containing application and substance
   * @param units The units to set
   */
  public void setLastSpecifiedUnits(UseKey useKey, String units) {
    String key = getKey(useKey);
    ensureSubstancePresent(key, "setLastSpecifiedUnits");
    StreamParameterization parameterization = substances.get(key);
    parameterization.setLastSpecifiedUnits(units);
  }


  /**
   * Get the last specified units for a key.
   *
   * @param useKey The key containing application and substance
   * @return The units string last used to specify a stream
   */
  public String getLastSpecifiedUnits(UseKey useKey) {
    StreamParameterization parameterization = getParameterization(useKey);
    return parameterization.getLastSpecifiedUnits();
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
    ensureSubstancePresent(scope, "getParameterization");
    String key = getKey(scope);
    return substances.get(key);
  }


  private StreamParameterization getParameterization(String key) {
    ensureSubstancePresent(key, "getParameterization");
    return substances.get(key);
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
   * @param scope The Scope to generate a key for
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

  private void setStreamForSales(UseKey useKey, String name, EngineNumber value) {
    EngineNumber manufactureValueRaw = getStream(useKey, "manufacture");
    EngineNumber importValueRaw = getStream(useKey, "import");

    EngineNumber manufactureValue = unitConverter.convert(manufactureValueRaw, "kg");
    EngineNumber importValue = unitConverter.convert(importValueRaw, "kg");

    BigDecimal manufactureAmount = manufactureValue.getValue();
    BigDecimal importAmount = importValue.getValue();

    EngineNumber valueConverted = unitConverter.convert(value, "kg");
    BigDecimal amountKg = valueConverted.getValue();

    // Get stream enabled status
    boolean manufactureEnabled = hasStreamBeenEnabled(useKey, "manufacture");
    boolean importEnabled = hasStreamBeenEnabled(useKey, "import");

    // Build distribution using the new builder
    SalesStreamDistribution distribution = SalesStreamDistributionBuilder.buildDistribution(
        manufactureValue,
        importValue,
        manufactureEnabled,
        importEnabled
    );

    BigDecimal manufacturePercent = distribution.getPercentManufacture();
    BigDecimal importPercent = distribution.getPercentImport();

    BigDecimal newManufactureAmount = amountKg.multiply(manufacturePercent);
    BigDecimal newImportAmount = amountKg.multiply(importPercent);

    EngineNumber manufactureAmountToSet = new EngineNumber(newManufactureAmount, "kg");
    EngineNumber importAmountToSet = new EngineNumber(newImportAmount, "kg");

    setSimpleStream(useKey, "manufacture", manufactureAmountToSet);
    setSimpleStream(useKey, "import", importAmountToSet);
  }

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

    // Set the stream directly to avoid recursion
    String streamKey = getKey(useKey, name);
    streams.put(streamKey, valueConverted);
  }

  /**
   * Verify that a substance exists for a key.
   *
   * @param key The key containing application and substance
   * @param context The context for error reporting
   * @throws IllegalStateException If the substance does not exist for the key
   */
  private void ensureSubstancePresent(UseKey key, String context) {
    if (key == null) {
      throw new IllegalStateException("Scope cannot be null in " + context);
    }
    if (!hasSubstance(key)) {
      StringBuilder message = new StringBuilder();
      message.append("Not a known application substance pair in ");
      message.append(context);
      message.append(": ");
      message.append(key.getApplication());
      message.append(", ");
      message.append(key.getSubstance());
      throw new IllegalStateException(message.toString());
    }
  }


  private void ensureSubstancePresent(String key, String context) {
    if (key == null) {
      throw new IllegalStateException("Key cannot be null in " + context);
    }
    if (!substances.containsKey(key)) {
      String[] keyPieces = key.split("\t");
      String application = keyPieces.length > 0 ? keyPieces[0] : "";
      String substance = keyPieces.length > 1 ? keyPieces[1] : "";
      StringBuilder message = new StringBuilder();
      message.append("Not a known application substance pair in ");
      message.append(context);
      message.append(": ");
      message.append("-".equals(application) ? "null" : application);
      message.append(", ");
      message.append("-".equals(substance) ? "null" : substance);
      throw new IllegalStateException(message.toString());
    }
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
   * Determine if the user is setting a sales component (manufacture / import / sales) by units.
   *
   * @param name The stream name
   * @param value The value to set
   * @return true if the user is setting a sales component by units and false otherwise
   */
  private boolean getIsSettingVolumeByUnits(String name, EngineNumber value) {
    boolean isSalesComponent = "manufacture".equals(name) || "import".equals(name)
                               || "sales".equals(name);
    boolean isUnits = value.getUnits().startsWith("unit");
    return isSalesComponent && isUnits;
  }


}
