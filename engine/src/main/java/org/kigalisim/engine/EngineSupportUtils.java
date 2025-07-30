/**
 * Utility methods for engine support operations.
 *
 * <p>This class provides static utility methods that can be used by various
 * engine classes without requiring access to private engine methods.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine;

import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.YearMatcher;

/**
 * Static utility methods for engine operations.
 */
public final class EngineSupportUtils {

  private EngineSupportUtils() {
    // Utility class - prevent instantiation
  }

  /**
   * Check if a year matcher is in range for the given current year.
   *
   * @param yearMatcher The year matcher to check (can be null)
   * @param currentYear The current year to check against
   * @return True if in range or no matcher provided
   */
  public static boolean isInRange(YearMatcher yearMatcher, int currentYear) {
    return yearMatcher == null || yearMatcher.getInRange(currentYear);
  }

  /**
   * Check if a stream name represents a sales substream (domestic or import).
   *
   * @param name The stream name to check
   * @return true if the stream is domestic or import
   */
  public static boolean isSalesSubstream(String name) {
    return "domestic".equals(name) || "import".equals(name);
  }

  /**
   * Creates a unit converter with total values initialized.
   *
   * @param engine The SingleThreadEngine instance to get state from
   * @param stream The stream identifier to create converter for
   * @return A configured unit converter instance
   */
  public static UnitConverter createUnitConverterWithTotal(SingleThreadEngine engine, String stream) {
    return engine.createUnitConverterWithTotal(stream);
  }
}
