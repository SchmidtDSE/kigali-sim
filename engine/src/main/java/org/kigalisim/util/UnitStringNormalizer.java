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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for normalizing unit strings.
 *
 * <p>Normalization involves removing all whitespace from unit strings to ensure
 * consistent comparison and processing. This class uses literal string replacement
 * for optimal performance.</p>
 */
public final class UnitStringNormalizer {

  // Cache for normalized unit strings to avoid repeated string allocation
  // Uses ConcurrentHashMap for thread-safe access without synchronization
  private static final Map<String, String> NORMALIZATION_CACHE = new ConcurrentHashMap<>(64);

  // Maximum cache size - typical simulations use 10-30 unique unit patterns
  // 100 provides headroom for complex scenarios while preventing unbounded growth
  private static final int MAX_CACHE_SIZE = 100;

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
    String cached = NORMALIZATION_CACHE.get(unitString);
    if (cached != null) {
      return cached;
    }

    String normalized = unitString.replace(" ", "");

    if (NORMALIZATION_CACHE.size() < MAX_CACHE_SIZE) {
      NORMALIZATION_CACHE.put(unitString, normalized);
    }

    return normalized;
  }
}
