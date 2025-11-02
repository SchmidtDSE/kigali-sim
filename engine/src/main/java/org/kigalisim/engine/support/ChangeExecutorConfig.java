/**
 * Configuration object for ChangeExecutor operations.
 *
 * <p>This class holds all the parameters needed for change execution operations,
 * reducing parameter passing and providing a clean API for change operations.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;

/**
 * Configuration for change executor operations.
 *
 * <p>This immutable class encapsulates all parameters needed for executing
 * change operations on engine streams.</p>
 */
public final class ChangeExecutorConfig {

  private final String stream;
  private final EngineNumber amount;
  private final Optional<YearMatcher> yearMatcher;
  private final UseKey useKeyEffective;

  /**
   * Creates a new ChangeExecutorConfig.
   *
   * @param stream The stream identifier to modify
   * @param amount The change amount (percentage, units, or kg)
   * @param yearMatcher Optional matcher to determine if the change applies to current year
   * @param useKeyEffective The effective UseKey for the operation
   */
  public ChangeExecutorConfig(String stream, EngineNumber amount,
      Optional<YearMatcher> yearMatcher, UseKey useKeyEffective) {
    this.stream = stream;
    this.amount = amount;
    this.yearMatcher = yearMatcher;
    this.useKeyEffective = useKeyEffective;
  }

  /**
   * Creates a new ChangeExecutorConfig with a YearMatcher.
   *
   * @param stream The stream identifier to modify
   * @param amount The change amount (percentage, units, or kg)
   * @param yearMatcher Matcher to determine if the change applies to current year
   * @param useKeyEffective The effective UseKey for the operation
   */
  public ChangeExecutorConfig(String stream, EngineNumber amount, YearMatcher yearMatcher,
      UseKey useKeyEffective) {
    this(stream, amount, Optional.ofNullable(yearMatcher), useKeyEffective);
  }

  /**
   * Creates a new ChangeExecutorConfig without a year matcher.
   *
   * <p>This configuration will apply to all years since no year matching is specified.</p>
   *
   * @param stream The stream identifier to modify
   * @param amount The change amount (percentage, units, or kg)
   * @param useKeyEffective The effective UseKey for the operation
   */
  public ChangeExecutorConfig(String stream, EngineNumber amount, UseKey useKeyEffective) {
    this(stream, amount, Optional.empty(), useKeyEffective);
  }

  /**
   * Gets the stream identifier.
   *
   * @return the stream identifier
   */
  public String getStream() {
    return stream;
  }

  /**
   * Gets the change amount.
   *
   * @return the change amount
   */
  public EngineNumber getAmount() {
    return amount;
  }

  /**
   * Gets the year matcher.
   *
   * @return the year matcher wrapped in Optional, or Optional.empty() if not specified
   */
  public Optional<YearMatcher> getYearMatcher() {
    return yearMatcher;
  }

  /**
   * Gets the effective use key.
   *
   * @return the effective use key
   */
  public UseKey getUseKeyEffective() {
    return useKeyEffective;
  }
}
