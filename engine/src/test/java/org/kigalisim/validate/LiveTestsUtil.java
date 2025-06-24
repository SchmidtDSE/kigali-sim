/**
 * Utility class for live tests.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.validate;

import java.util.stream.Stream;
import org.kigalisim.engine.serializer.EngineResult;

/**
 * Utility methods for tests that validate QTA files against expected behavior.
 */
public class LiveTestsUtil {

  /**
   * Utility function to get a result for a specific year, application, and substance.
   * Similar to the getResult function in test_compiler.js.
   *
   * @param results Stream of engine results from running a scenario
   * @param year The year to find results for
   * @param application The application name
   * @param substance The substance name
   * @return The matching EngineResult or null if not found
   */
  public static EngineResult getResult(Stream<EngineResult> results, int year,
      String application, String substance) {
    return results
        .filter(r -> r.getYear() == year)
        .filter(r -> r.getApplication().equals(application))
        .filter(r -> r.getSubstance().equals(substance))
        .findFirst()
        .orElse(null);
  }
}
