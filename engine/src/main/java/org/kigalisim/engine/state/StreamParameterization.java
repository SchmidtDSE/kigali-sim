/**
 * Class for managing stream-specific parameters and settings.
 *
 * <p>Handles configuration of GHG intensity, initial charge, recharge rates,
 * recovery rates, and other stream-specific values.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.kigalisim.engine.number.EngineNumber;

/**
 * Class for managing stream-specific parameters and settings.
 *
 * <p>Handles configuration of GHG intensity, initial charge, recharge rates,
 * recovery rates, and other stream-specific values.</p>
 */
public class StreamParameterization {

  private EngineNumber ghgIntensity;
  private EngineNumber energyIntensity;
  private final Map<String, EngineNumber> initialCharge;
  private EngineNumber rechargePopulation;
  private EngineNumber rechargeIntensity;
  private EngineNumber recoveryRate;
  private EngineNumber yieldRate;
  private EngineNumber retirementRate;
  private EngineNumber displacementRate;
  private final Map<String, EngineNumber> lastSpecifiedValue;
  private final Set<String> enabledStreams;
  private boolean salesIntentFreshlySet;

  /**
   * Create a new stream parameterization instance.
   */
  public StreamParameterization() {
    this.initialCharge = new HashMap<>();
    this.enabledStreams = new HashSet<>();
    this.lastSpecifiedValue = new HashMap<>();
    this.salesIntentFreshlySet = false;

    // Initialize all parameters with default values
    ghgIntensity = new EngineNumber(BigDecimal.ZERO, "tCO2e / kg");
    energyIntensity = new EngineNumber(BigDecimal.ZERO, "kwh / kg");

    initialCharge.put("manufacture", new EngineNumber(BigDecimal.ONE, "kg / unit"));
    initialCharge.put("import", new EngineNumber(BigDecimal.ONE, "kg / unit"));

    rechargePopulation = new EngineNumber(BigDecimal.ZERO, "%");
    rechargeIntensity = new EngineNumber(BigDecimal.ZERO, "kg / unit");
    recoveryRate = new EngineNumber(BigDecimal.ZERO, "%");
    yieldRate = new EngineNumber(BigDecimal.ZERO, "%");
    retirementRate = new EngineNumber(BigDecimal.ZERO, "%");
    displacementRate = new EngineNumber(new BigDecimal("100"), "%");
  }


  /**
   * Set the greenhouse gas intensity.
   *
   * @param newValue The new GHG intensity value
   */
  public void setGhgIntensity(EngineNumber newValue) {
    ghgIntensity = newValue;
  }

  /**
   * Get the greenhouse gas intensity.
   *
   * @return The current GHG intensity value
   */
  public EngineNumber getGhgIntensity() {
    return ghgIntensity;
  }

  /**
   * Set the energy intensity.
   *
   * @param newValue The new energy intensity value
   */
  public void setEnergyIntensity(EngineNumber newValue) {
    energyIntensity = newValue;
  }

  /**
   * Get the energy intensity.
   *
   * @return The current energy intensity value
   */
  public EngineNumber getEnergyIntensity() {
    return energyIntensity;
  }

  /**
   * Set the initial charge for a stream.
   *
   * @param stream The stream identifier ('manufacture' or 'import')
   * @param newValue The new initial charge value
   */
  public void setInitialCharge(String stream, EngineNumber newValue) {
    ensureSalesStreamAllowed(stream);
    initialCharge.put(stream, newValue);
  }

  /**
   * Get the initial charge for a stream.
   *
   * @param stream The stream identifier ('manufacture' or 'import')
   * @return The initial charge value for the stream
   */
  public EngineNumber getInitialCharge(String stream) {
    ensureSalesStreamAllowed(stream);
    return initialCharge.get(stream);
  }

  /**
   * Set the recharge population percentage.
   *
   * @param newValue The new recharge population value
   */
  public void setRechargePopulation(EngineNumber newValue) {
    rechargePopulation = newValue;
  }

  /**
   * Get the recharge population percentage.
   *
   * @return The current recharge population value
   */
  public EngineNumber getRechargePopulation() {
    return rechargePopulation;
  }

  /**
   * Set the recharge intensity.
   *
   * @param newValue The new recharge intensity value
   */
  public void setRechargeIntensity(EngineNumber newValue) {
    rechargeIntensity = newValue;
  }

  /**
   * Get the recharge intensity.
   *
   * @return The current recharge intensity value
   */
  public EngineNumber getRechargeIntensity() {
    return rechargeIntensity;
  }

  /**
   * Set the recovery rate percentage.
   *
   * @param newValue The new recovery rate value
   */
  public void setRecoveryRate(EngineNumber newValue) {
    recoveryRate = newValue;
  }

  /**
   * Get the recovery rate percentage.
   *
   * @return The current recovery rate value
   */
  public EngineNumber getRecoveryRate() {
    return recoveryRate;
  }

  /**
   * Set the yield rate percentage for recycling.
   *
   * @param newValue The new yield rate value
   */
  public void setYieldRate(EngineNumber newValue) {
    yieldRate = newValue;
  }

  /**
   * Get the yield rate percentage for recycling.
   *
   * @return The current yield rate value
   */
  public EngineNumber getYieldRate() {
    return yieldRate;
  }

  /**
   * Set the displacement rate percentage.
   *
   * @param newValue The new displacement rate value
   */
  public void setDisplacementRate(EngineNumber newValue) {
    displacementRate = newValue;
  }

  /**
   * Get the displacement rate percentage.
   *
   * @return The current displacement rate value
   */
  public EngineNumber getDisplacementRate() {
    return displacementRate;
  }

  /**
   * Set the retirement rate percentage.
   *
   * @param newValue The new retirement rate value
   */
  public void setRetirementRate(EngineNumber newValue) {
    retirementRate = newValue;
  }

  /**
   * Get the retirement rate percentage.
   *
   * @return The current retirement rate value
   */
  public EngineNumber getRetirementRate() {
    return retirementRate;
  }

  /**
   * Set the last specified value for a stream.
   *
   * <p>This tracks the value and units last used when setting streams
   * to preserve user intent across carry-over years.</p>
   *
   * @param streamName The name of the stream
   * @param value The last specified value with units
   */
  public void setLastSpecifiedValue(String streamName, EngineNumber value) {
    // Ignore percentage units to avoid impacting last recorded values
    if (value != null && value.getUnits() != null && value.getUnits().contains("%")) {
      return;
    }
    lastSpecifiedValue.put(streamName, value);

    // Set the flag if this is a sales-related stream
    if ("sales".equals(streamName) || "import".equals(streamName) || "manufacture".equals(streamName)) {
      salesIntentFreshlySet = true;
    }
  }

  /**
   * Get the last specified value for a stream.
   *
   * @param streamName The name of the stream
   * @return The last specified value with units, or null if not set
   */
  public EngineNumber getLastSpecifiedValue(String streamName) {
    return lastSpecifiedValue.get(streamName);
  }

  /**
   * Check if a stream has a last specified value.
   *
   * @param streamName The name of the stream
   * @return true if the stream has a last specified value, false otherwise
   */
  public boolean hasLastSpecifiedValue(String streamName) {
    return lastSpecifiedValue.containsKey(streamName);
  }


  /**
   * Mark a stream as having been enabled (set to non-zero value).
   *
   * @param streamName The name of the stream to mark as enabled
   */
  public void markStreamAsEnabled(String streamName) {
    enabledStreams.add(streamName);
  }

  /**
   * Check if a stream has ever been enabled (set to non-zero value).
   *
   * @param streamName The name of the stream to check
   * @return true if the stream has been enabled, false otherwise
   */
  public boolean hasStreamBeenEnabled(String streamName) {
    return enabledStreams.contains(streamName);
  }

  /**
   * Check if sales intent has been freshly set in the current processing cycle.
   *
   * @return true if sales intent was freshly set, false otherwise
   */
  public boolean isSalesIntentFreshlySet() {
    return salesIntentFreshlySet;
  }

  /**
   * Set the flag indicating whether sales intent has been freshly set.
   *
   * @param freshlySet true if sales intent was freshly set, false otherwise
   */
  public void setSalesIntentFreshlySet(boolean freshlySet) {
    this.salesIntentFreshlySet = freshlySet;
  }

  /**
   * Reset state at the beginning of a timestep.
   *
   * <p>This method resets recovery rate to 0% between years since recycling
   * programs may cease and should not be expected to continue unchanged.</p>
   */
  public void resetStateAtTimestep() {
    // Reset recovery to 0% between years since recycling programs may cease
    recoveryRate = new EngineNumber(BigDecimal.ZERO, "%");
  }

  /**
   * Validate that the given stream name is allowed for sales operations.
   *
   * @param name The stream name to validate
   * @throws IllegalArgumentException If the stream name is not a sales substream
   */
  private void ensureSalesStreamAllowed(String name) {
    if (!"manufacture".equals(name) && !"import".equals(name) && !"export".equals(name) && !"recycle".equals(name)) {
      throw new IllegalArgumentException("Must address a sales substream.");
    }
  }
}
