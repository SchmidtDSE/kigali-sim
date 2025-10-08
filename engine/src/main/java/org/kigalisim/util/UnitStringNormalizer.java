/**
 * Utility for normalizing unit strings by removing whitespace.
 *
 * <p>This class provides high-performance unit string normalization using literal
 * string replacement. It is used to ensure consistent unit string formatting throughout
 * the simulation engine.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.util;

/**
 * Utility class for normalizing unit strings.
 *
 * <p>Normalization involves removing all whitespace from unit strings to ensure
 * consistent comparison and processing. This class uses literal string replacement
 * for optimal performance.</p>
 */
public final class UnitStringNormalizer {

  /**
   * Private constructor to prevent instantiation of utility class.
   */
  private UnitStringNormalizer() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }

  /**
   * Remove all whitespace from a unit string.
   *
   * <p>This method uses literal string replacement to remove spaces, which is
   * significantly faster than regex pattern matching for this simple case.</p>
   *
   * @param unitString The unit string to normalize
   * @return The normalized unit string with all whitespace removed
   */
  public static String normalize(String unitString) {
    return unitString.replace(" ", "");
  }
}
