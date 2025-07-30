/**
 * Builder for creating ChangeExecutorConfig instances with fluent interface.
 *
 * <p>This builder provides a fluent interface for constructing change executor
 * configuration objects with proper validation.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;

/**
 * Builder for creating ChangeExecutorConfig instances with fluent interface.
 */
public final class ChangeExecutorConfigBuilder {

  private String stream;
  private EngineNumber amount;
  private YearMatcher yearMatcher;
  private UseKey useKeyEffective;

  /**
   * Create a new ChangeExecutorConfigBuilder.
   */
  public ChangeExecutorConfigBuilder() {
    // Initialize with defaults
  }

  /**
   * Sets the stream identifier.
   *
   * @param stream the stream identifier to modify
   * @return this builder for method chaining
   */
  public ChangeExecutorConfigBuilder setStream(String stream) {
    this.stream = stream;
    return this;
  }

  /**
   * Sets the change amount.
   *
   * @param amount the change amount (percentage, units, or kg)
   * @return this builder for method chaining
   */
  public ChangeExecutorConfigBuilder setAmount(EngineNumber amount) {
    this.amount = amount;
    return this;
  }

  /**
   * Sets the year matcher.
   *
   * @param yearMatcher matcher to determine if the change applies to current year
   * @return this builder for method chaining
   */
  public ChangeExecutorConfigBuilder setYearMatcher(YearMatcher yearMatcher) {
    this.yearMatcher = yearMatcher;
    return this;
  }

  /**
   * Sets the effective use key.
   *
   * @param useKeyEffective the effective UseKey for the operation
   * @return this builder for method chaining
   */
  public ChangeExecutorConfigBuilder setUseKeyEffective(UseKey useKeyEffective) {
    this.useKeyEffective = useKeyEffective;
    return this;
  }

  /**
   * Builds the ChangeExecutorConfig.
   *
   * @return the built ChangeExecutorConfig
   * @throws IllegalStateException if required fields are not set
   */
  public ChangeExecutorConfig build() {
    if (stream == null) {
      throw new IllegalStateException("Stream is required");
    }
    if (amount == null) {
      throw new IllegalStateException("Amount is required");
    }
    if (useKeyEffective == null) {
      throw new IllegalStateException("UseKeyEffective is required");
    }
    // yearMatcher can be null (Optional in the original design)

    return new ChangeExecutorConfig(stream, amount, yearMatcher, useKeyEffective);
  }
}
