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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;

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
    List<SubstanceInApplicationId> substanceIds = new ArrayList<>();
    for (String key : substances.keySet()) {
      String[] keyComponents = key.split("\t");
      substanceIds.add(new SubstanceInApplicationId(keyComponents[0], keyComponents[1]));
    }
    return substanceIds;
  }

  /**
   * Check if a substance exists for an application.
   *
   * @param application The application name
   * @param substance The substance name
   * @return true if the substance exists for the application
   */
  public boolean hasSubstance(String application, String substance) {
    String key = getKey(application, substance);
    return substances.containsKey(key);
  }

  /**
   * Ensure a substance exists for an application, creating it if needed.
   *
   * @param application The application name
   * @param substance The substance name to initialize with zero values
   */
  public void ensureSubstance(String application, String substance) {
    if (hasSubstance(application, substance)) {
      return;
    }

    String key = getKey(application, substance);
    substances.put(key, new StreamParameterization());

    // Sales: manufacture, import, recycle
    String manufactureKey = getKey(application, substance, "manufacture");
    streams.put(manufactureKey, new EngineNumber(BigDecimal.ZERO, "kg"));
    String importKey = getKey(application, substance, "import");
    streams.put(importKey, new EngineNumber(BigDecimal.ZERO, "kg"));
    String recycleKey = getKey(application, substance, "recycle");
    streams.put(recycleKey, new EngineNumber(BigDecimal.ZERO, "kg"));

    // Consumption: count, conversion
    String consumptionKey = getKey(application, substance, "consumption");
    streams.put(consumptionKey, new EngineNumber(BigDecimal.ZERO, "tCO2e"));

    // Population
    String equipmentKey = getKey(application, substance, "equipment");
    streams.put(equipmentKey, new EngineNumber(BigDecimal.ZERO, "units"));
    String priorEquipmentKey = getKey(application, substance, "priorEquipment");
    streams.put(priorEquipmentKey, new EngineNumber(BigDecimal.ZERO, "units"));
    String newEquipmentKey = getKey(application, substance, "newEquipment");
    streams.put(newEquipmentKey, new EngineNumber(BigDecimal.ZERO, "units"));

    // Emissions
    String rechargeEmissionsKey = getKey(application, substance, "rechargeEmissions");
    streams.put(rechargeEmissionsKey, new EngineNumber(BigDecimal.ZERO, "tCO2e"));
    String eolEmissionsKey = getKey(application, substance, "eolEmissions");
    streams.put(eolEmissionsKey, new EngineNumber(BigDecimal.ZERO, "tCO2e"));
  }

  /**
   * Set the value for a specific stream.
   *
   * @param application The application name
   * @param substance The substance name
   * @param name The stream name
   * @param value The value to set
   */
  public void setStream(String application, String substance, String name, EngineNumber value) {
    ensureSubstancePresent(application, substance, "setStream");
    ensureStreamKnown(name);

    if (CHECK_NAN_STATE && value.getValue().toString().equals("NaN")) {
      String pieces = String.join(" > ", application, substance, name);
      throw new RuntimeException("Encountered NaN to be set for: " + pieces);
    }

    if (getIsSettingVolumeByUnits(name, value)) {
      setStreamForSalesWithUnits(application, substance, name, value);
    } else if ("sales".equals(name)) {
      setStreamForSales(application, substance, name, value);
    } else {
      setSimpleStream(application, substance, name, value);
    }
  }

  /**
   * Get the value of a specific stream.
   *
   * @param application The application name
   * @param substance The substance name
   * @param name The stream name
   * @return The stream value
   */
  public EngineNumber getStream(String application, String substance, String name) {
    ensureSubstancePresent(application, substance, "getStream");
    ensureStreamKnown(name);

    if ("sales".equals(name)) {
      EngineNumber manufactureAmountRaw = getStream(application, substance, "manufacture");
      EngineNumber importAmountRaw = getStream(application, substance, "import");

      EngineNumber manufactureAmount = unitConverter.convert(manufactureAmountRaw, "kg");
      EngineNumber importAmount = unitConverter.convert(importAmountRaw, "kg");

      BigDecimal manufactureAmountValue = manufactureAmount.getValue();
      BigDecimal importAmountValue = importAmount.getValue();

      BigDecimal newTotal = manufactureAmountValue.add(importAmountValue);

      return new EngineNumber(newTotal, "kg");
    } else {
      return streams.get(getKey(application, substance, name));
    }
  }

  /**
   * Check if a stream exists for a substance-application pair.
   *
   * @param application The application name
   * @param substance The substance name
   * @param name The stream name
   * @return true if the stream exists
   */
  public boolean isKnownStream(String application, String substance, String name) {
    return streams.containsKey(getKey(application, substance, name));
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

      EngineNumber equipment = getStream(application, substance, "equipment");
      setStream(application, substance, "priorEquipment", equipment);

      substances.get(key).resetInternals();
    }
  }

  /**
   * Set the greenhouse gas intensity for a substance in an application.
   *
   * @param application The application name
   * @param substance The substance name
   * @param newValue The new GHG intensity value
   */
  public void setGhgIntensity(String application, String substance, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(application, substance);
    parameterization.setGhgIntensity(newValue);
  }

  /**
   * Set the energy intensity for a substance in an application.
   *
   * @param application The application name
   * @param substance The substance name
   * @param newValue The new energy intensity value
   */
  public void setEnergyIntensity(String application, String substance, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(application, substance);
    parameterization.setEnergyIntensity(newValue);
  }

  /**
   * Get the greenhouse gas intensity for a substance in an application.
   *
   * @param application The application name
   * @param substance The substance name
   * @return The current GHG intensity value
   */
  public EngineNumber getGhgIntensity(String application, String substance) {
    StreamParameterization parameterization = getParameterization(application, substance);
    return parameterization.getGhgIntensity();
  }

  /**
   * Get the energy intensity for a substance in an application.
   *
   * @param application The application name
   * @param substance The substance name
   * @return The current energy intensity value
   */
  public EngineNumber getEnergyIntensity(String application, String substance) {
    StreamParameterization parameterization = getParameterization(application, substance);
    return parameterization.getEnergyIntensity();
  }

  /**
   * Set the initial charge for a substance in an application's stream.
   *
   * @param application The application name
   * @param substance The substance name
   * @param substream The stream identifier ('manufacture' or 'import')
   * @param newValue The new initial charge value
   */
  public void setInitialCharge(String application, String substance, String substream,
                              EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(application, substance);
    parameterization.setInitialCharge(substream, newValue);
  }

  /**
   * Get the initial charge for a substance in an application's stream.
   *
   * @param application The application name
   * @param substance The substance name
   * @param substream The stream identifier ('manufacture' or 'import')
   * @return The current initial charge value
   */
  public EngineNumber getInitialCharge(String application, String substance, String substream) {
    StreamParameterization parameterization = getParameterization(application, substance);
    return parameterization.getInitialCharge(substream);
  }

  /**
   * Set the recharge population percentage for a substance in an application.
   *
   * @param application The application name
   * @param substance The substance name
   * @param newValue The new recharge population value
   */
  public void setRechargePopulation(String application, String substance, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(application, substance);
    parameterization.setRechargePopulation(newValue);
  }

  /**
   * Get the recharge population percentage for a substance in an application.
   *
   * @param application The application name
   * @param substance The substance name
   * @return The current recharge population value
   */
  public EngineNumber getRechargePopulation(String application, String substance) {
    StreamParameterization parameterization = getParameterization(application, substance);
    return parameterization.getRechargePopulation();
  }

  /**
   * Set the recharge intensity for a substance in an application.
   *
   * @param application The application name
   * @param substance The substance name
   * @param newValue The new recharge intensity value
   */
  public void setRechargeIntensity(String application, String substance, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(application, substance);
    parameterization.setRechargeIntensity(newValue);
  }

  /**
   * Get the recharge intensity for a substance in an application.
   *
   * @param application The application name
   * @param substance The substance name
   * @return The current recharge intensity value
   */
  public EngineNumber getRechargeIntensity(String application, String substance) {
    StreamParameterization parameterization = getParameterization(application, substance);
    return parameterization.getRechargeIntensity();
  }

  /**
   * Set the recovery rate percentage for a substance in an application.
   *
   * @param application The application name
   * @param substance The substance name
   * @param newValue The new recovery rate value
   */
  public void setRecoveryRate(String application, String substance, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(application, substance);
    parameterization.setRecoveryRate(newValue);
  }

  /**
   * Get the recovery rate percentage for a substance in an application.
   *
   * @param application The application name
   * @param substance The substance name
   * @return The current recovery rate value
   */
  public EngineNumber getRecoveryRate(String application, String substance) {
    StreamParameterization parameterization = getParameterization(application, substance);
    return parameterization.getRecoveryRate();
  }

  /**
   * Set the displacement rate percentage for a substance in an application.
   *
   * @param application The application name
   * @param substance The substance name
   * @param newValue The new displacement rate value
   */
  public void setDisplacementRate(String application, String substance, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(application, substance);
    parameterization.setDisplacementRate(newValue);
  }

  /**
   * Get the displacement rate percentage for a substance in an application.
   *
   * @param application The application name
   * @param substance The substance name
   * @return The current displacement rate value
   */
  public EngineNumber getDisplacementRate(String application, String substance) {
    StreamParameterization parameterization = getParameterization(application, substance);
    return parameterization.getDisplacementRate();
  }

  /**
   * Set the yield rate percentage for recycling a substance in an application.
   *
   * @param application The application name
   * @param substance The substance name
   * @param newValue The new yield rate value
   */
  public void setYieldRate(String application, String substance, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(application, substance);
    parameterization.setYieldRate(newValue);
  }

  /**
   * Get the yield rate percentage for recycling a substance in an application.
   *
   * @param application The application name
   * @param substance The substance name
   * @return The current yield rate value
   */
  public EngineNumber getYieldRate(String application, String substance) {
    StreamParameterization parameterization = getParameterization(application, substance);
    return parameterization.getYieldRate();
  }

  /**
   * Set the retirement rate percentage for a substance in an application.
   *
   * @param application The application name
   * @param substance The substance name
   * @param newValue The new retirement rate value
   */
  public void setRetirementRate(String application, String substance, EngineNumber newValue) {
    StreamParameterization parameterization = getParameterization(application, substance);
    parameterization.setRetirementRate(newValue);
  }

  /**
   * Get the retirement rate percentage for a substance in an application.
   *
   * @param application The application name
   * @param substance The substance name
   * @return The current retirement rate value
   */
  public EngineNumber getRetirementRate(String application, String substance) {
    StreamParameterization parameterization = getParameterization(application, substance);
    return parameterization.getRetirementRate();
  }

  /**
   * Set the last specified units for a substance in an application.
   *
   * @param application The application name
   * @param substance The substance name
   * @param units The units string last used to specify a stream
   */
  public void setLastSpecifiedUnits(String application, String substance, String units) {
    StreamParameterization parameterization = getParameterization(application, substance);
    parameterization.setLastSpecifiedUnits(units);
  }

  /**
   * Get the last specified units for a substance in an application.
   *
   * @param application The application name
   * @param substance The substance name
   * @return The units string last used to specify a stream
   */
  public String getLastSpecifiedUnits(String application, String substance) {
    StreamParameterization parameterization = getParameterization(application, substance);
    return parameterization.getLastSpecifiedUnits();
  }

  /**
   * Retrieve parameterization for a specific application and substance.
   *
   * <p>Verifies the existence of the substance and application combination
   * and returns the associated StreamParameterization object.</p>
   *
   * @param application The name of the application
   * @param substance The name of the substance
   * @return The parameterization for the given application and substance
   */
  private StreamParameterization getParameterization(String application, String substance) {
    ensureSubstancePresent(application, substance, "getParameterization");
    String key = getKey(application, substance);
    return substances.get(key);
  }

  /**
   * Generate a key identifying a stream within a substance and application.
   *
   * @param application The application name
   * @param substance The substance name
   * @param name The stream name (optional)
   * @param substream The substream identifier (optional)
   * @return The generated key
   */
  private String getKey(String application, String substance, String name, String substream) {
    StringJoiner joiner = new StringJoiner("\t");
    joiner.add(application != null ? application : "-");
    joiner.add(substance != null ? substance : "-");
    joiner.add(name != null ? name : "-");
    joiner.add(substream != null ? substream : "-");
    return joiner.toString();
  }

  /**
   * Generate a key identifying a substance within an application.
   *
   * @param application The application name
   * @param substance The substance name
   * @return The generated key
   */
  private String getKey(String application, String substance) {
    return getKey(application, substance, null, null);
  }

  /**
   * Generate a key identifying a stream within a substance and application.
   *
   * @param application The application name
   * @param substance The substance name
   * @param name The stream name
   * @return The generated key
   */
  private String getKey(String application, String substance, String name) {
    return getKey(application, substance, name, null);
  }

  /**
   * Verify that a substance exists for an application.
   *
   * @param application The application name
   * @param substance The substance name
   * @param context The context for error reporting
   * @throws IllegalStateException If the substance does not exist for the application
   */
  private void ensureSubstancePresent(String application, String substance, String context) {
    if (!hasSubstance(application, substance)) {
      StringBuilder message = new StringBuilder();
      message.append("Not a known application substance pair in ");
      message.append(context);
      message.append(": ");
      message.append(application);
      message.append(", ");
      message.append(substance);
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
   * Handle setting the sales stream for an application and substance.
   *
   * <p>Handle setting the sales stream which has two substreams (manufacture and import)
   * which both require modification.</p>
   *
   * @param application The application name
   * @param substance The substance name
   * @param name The stream name
   * @param value The value to set
   */
  private void setStreamForSales(String application, String substance, String name,
                                EngineNumber value) {
    EngineNumber manufactureValueRaw = getStream(application, substance, "manufacture");
    EngineNumber importValueRaw = getStream(application, substance, "import");

    EngineNumber manufactureValue = unitConverter.convert(manufactureValueRaw, "kg");
    EngineNumber importValue = unitConverter.convert(importValueRaw, "kg");

    BigDecimal manufactureAmount = manufactureValue.getValue();
    BigDecimal importAmount = importValue.getValue();

    EngineNumber valueConverted = unitConverter.convert(value, "kg");
    BigDecimal amountKg = valueConverted.getValue();

    BigDecimal totalAmount = manufactureAmount.add(importAmount);
    boolean isZero = totalAmount.compareTo(BigDecimal.ZERO) == 0;
    BigDecimal manufacturePercent = isZero ? new BigDecimal("0.5") :
        manufactureAmount.divide(totalAmount);
    BigDecimal importPercent = isZero ? new BigDecimal("0.5") :
        importAmount.divide(totalAmount);

    BigDecimal manufactureShare = amountKg.multiply(manufacturePercent);
    BigDecimal importShare = amountKg.multiply(importPercent);
    EngineNumber manufactureNewValue = new EngineNumber(manufactureShare, value.getUnits());
    EngineNumber importNewValue = new EngineNumber(importShare, value.getUnits());

    setStream(application, substance, "manufacture", manufactureNewValue);
    setStream(application, substance, "import", importNewValue);
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

  /**
   * Handle setting a stream which only requires simple unit conversion.
   *
   * @param application The application name
   * @param substance The substance name
   * @param name The stream name
   * @param value The value to set
   */
  private void setSimpleStream(String application, String substance, String name,
                              EngineNumber value) {
    String unitsNeeded = getUnits(name);
    EngineNumber valueConverted = unitConverter.convert(value, unitsNeeded);

    if (CHECK_NAN_STATE && valueConverted.getValue().toString().equals("NaN")) {
      String pieces = String.join(" > ", application, substance, name);
      throw new RuntimeException("Encountered NaN after conversion to be set for: " + pieces);
    }

    if (CHECK_POSITIVE_STREAMS && valueConverted.getValue().compareTo(BigDecimal.ZERO) < 0) {
      String pieces = String.join(" > ", application, substance, name);
      throw new RuntimeException("Encountered negative stream to be set for: " + pieces);
    }

    String streamKey = getKey(application, substance, name);
    streams.put(streamKey, valueConverted);
  }

  /**
   * Handle setting volume by units for sales components.
   *
   * <p>Handle setting a sales component (manufacture or import) which requires conversion
   * by way of initial charge specific to that stream.</p>
   *
   * @param application The application name
   * @param substance The substance name
   * @param name The stream name
   * @param value The value to set
   */
  private void setStreamForSalesWithUnits(String application, String substance, String name,
                                         EngineNumber value) {
    OverridingConverterStateGetter overridingStateGetter = new OverridingConverterStateGetter(
        stateGetter
    );
    UnitConverter unitConverter = new UnitConverter(overridingStateGetter);

    EngineNumber initialCharge = getInitialCharge(application, substance, name);
    if (initialCharge.getValue().compareTo(BigDecimal.ZERO) == 0) {
      throw new RuntimeException("Cannot set " + name + " stream with a zero initial charge.");
    }
    
    EngineNumber initialChargeConverted = unitConverter.convert(initialCharge, "kg / unit");
    overridingStateGetter.setAmortizedUnitVolume(initialChargeConverted);

    EngineNumber valueUnitsPlain = unitConverter.convert(value, "units");
    EngineNumber valueConverted = unitConverter.convert(valueUnitsPlain, "kg");

    // Set the stream directly to avoid recursion
    String streamKey = getKey(application, substance, name);
    streams.put(streamKey, valueConverted);
  }
}
