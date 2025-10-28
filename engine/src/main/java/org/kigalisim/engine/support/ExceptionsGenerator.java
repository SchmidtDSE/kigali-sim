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

  /**
   * Constructs a new ExceptionsGenerator.
   */
  public ExceptionsGenerator() {
  }

  private static final String NO_APP_OR_SUBSTANCE_MESSAGE =
      "Error %s because application and / or substance not%s";

  private static final String SELF_REPLACEMENT_MESSAGE =
      "Cannot replace substance \"%s\" with itself. Please specify a different target substance for replacement.";

  private static final String SELF_DISPLACEMENT_MESSAGE =
      "Cannot displace stream \"%s\" to itself. Please specify a different target stream for displacement.";

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

  /**
   * Raise an exception for attempted self-replacement.
   *
   * @param substanceName The name of the substance being self-replaced
   * @throws RuntimeException Always throws with formatted message
   */
  public static void raiseSelfReplacement(String substanceName) {
    throw new RuntimeException(String.format(SELF_REPLACEMENT_MESSAGE, substanceName));
  }

  /**
   * Raise an exception for attempted self-displacement.
   *
   * @param streamName The name of the stream being self-displaced
   * @throws RuntimeException Always throws with formatted message
   */
  public static void raiseSelfDisplacement(String streamName) {
    throw new RuntimeException(String.format(SELF_DISPLACEMENT_MESSAGE, streamName));
  }
}
