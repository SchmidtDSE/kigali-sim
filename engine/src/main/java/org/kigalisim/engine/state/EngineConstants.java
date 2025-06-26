/**
 * Constants used across the engine state management.
 *
 * <p>This class provides constants for context levels and stream base units
 * used throughout the engine state management system.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import java.util.HashMap;
import java.util.Map;

/**
 * Constants for engine state management.
 *
 * <p>Defines context levels for variable scoping and base units for different
 * stream types in the simulation engine.</p>
 */
public final class EngineConstants {

  /**
   * Global context level for variables accessible throughout the system.
   */
  public static final int GLOBAL_CONTEXT = 0;

  /**
   * Stanza context level for variables accessible within a stanza.
   */
  public static final int STANZA_CONTEXT = 1;

  /**
   * Application context level for variables accessible within an application.
   */
  public static final int APPLICATION_CONTEXT = 2;

  /**
   * Substance context level for variables accessible within a substance.
   */
  public static final int SUBSTANCE_CONTEXT = 3;

  /**
   * Base units for different stream types in the engine.
   */
  private static final Map<String, String> STREAM_BASE_UNITS = createStreamBaseUnits();

  /**
   * Create the map of stream base units.
   *
   * @return Map of stream names to their base units
   */
  private static Map<String, String> createStreamBaseUnits() {
    Map<String, String> units = new HashMap<>();
    units.put("manufacture", "kg");
    units.put("import", "kg");
    units.put("export", "kg");
    units.put("sales", "kg");
    units.put("energy", "kwh");
    units.put("recycle", "kg");
    units.put("consumption", "tCO2e");
    units.put("rechargeEmissions", "tCO2e");
    units.put("eolEmissions", "tCO2e");
    units.put("equipment", "units");
    units.put("priorEquipment", "units");
    units.put("newEquipment", "units");
    return units;
  }

  /**
   * Get the base units for a given stream name.
   *
   * @param streamName The name of the stream
   * @return The base units for the stream, or null if not found
   */
  public static String getBaseUnits(String streamName) {
    return STREAM_BASE_UNITS.get(streamName);
  }

  /**
   * Private constructor to prevent instantiation of utility class.
   */
  private EngineConstants() {
    throw new UnsupportedOperationException("Utility class should not be instantiated");
  }
}
