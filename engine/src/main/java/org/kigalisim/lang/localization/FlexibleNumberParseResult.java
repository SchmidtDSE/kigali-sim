/**
 * Result class for flexible number parsing operations.
 *
 * <p>This class encapsulates the result of attempting to parse a number with flexible
 * thousands and decimal separators. It contains either a successfully parsed number
 * or an error message if parsing failed.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.localization;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Result of a flexible number parsing operation.
 */
public class FlexibleNumberParseResult {

  private final Optional<BigDecimal> parsedNumber;
  private final Optional<String> error;

  /**
   * Creates a successful parse result.
   *
   * @param parsedNumber The successfully parsed number
   */
  public FlexibleNumberParseResult(BigDecimal parsedNumber) {
    this.parsedNumber = Optional.of(parsedNumber);
    this.error = Optional.empty();
  }

  /**
   * Creates a failed parse result with an error message.
   *
   * @param error The error message explaining why parsing failed
   */
  public FlexibleNumberParseResult(String error) {
    this.parsedNumber = Optional.empty();
    this.error = Optional.of(error);
  }

  /**
   * Gets the parsed number if parsing was successful.
   *
   * @return Optional containing the parsed number, or empty if parsing failed
   */
  public Optional<BigDecimal> getParsedNumber() {
    return parsedNumber;
  }

  /**
   * Gets the error message if parsing failed.
   *
   * @return Optional containing the error message, or empty if parsing was successful
   */
  public Optional<String> getError() {
    return error;
  }

  /**
   * Returns true if parsing was successful.
   *
   * @return true if a number was successfully parsed
   */
  public boolean isSuccess() {
    return parsedNumber.isPresent();
  }

  /**
   * Returns true if parsing failed.
   *
   * @return true if parsing failed
   */
  public boolean isError() {
    return error.isPresent();
  }
}
