/**
 * Generator for exceptions used in recalculation operations.
 *
 * <p>This class encapsulates exception generation logic previously found
 * in SingleThreadEngine.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

/**
 * Generator for exceptions used in recalculation operations.
 */
public class ExceptionsGenerator {

  private static final String NO_APP_OR_SUBSTANCE_MESSAGE =
      "Error %s because application and / or substance not%s";

  /**
   * Raise an exception for missing application or substance.
   *
   * @param operation The operation being attempted
   * @param suffix Additional suffix for the error message (usually " specified")
   * @throws RuntimeException Always throws with formatted message
   */
  public static void raiseNoAppOrSubstance(String operation, String suffix) {
    throw new RuntimeException(String.format(NO_APP_OR_SUBSTANCE_MESSAGE, operation, suffix));
  }
}
