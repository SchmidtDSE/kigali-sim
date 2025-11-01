/**
 * Builder for creating ChangeExecutorConfig instances with fluent interface.
 *
 * <p>This builder provides a fluent interface for constructing change executor
 * configuration objects with proper validation.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;

/**
 * Builder for creating ChangeExecutorConfig instances with fluent interface.
 */
public final class ChangeExecutorConfigBuilder {

  private Optional<String> stream;
  private Optional<EngineNumber> amount;
  private Optional<YearMatcher> yearMatcher;
  private Optional<UseKey> useKeyEffective;

  /**
   * Create a new ChangeExecutorConfigBuilder.
   */
  public ChangeExecutorConfigBuilder() {
    this.stream = Optional.empty();
    this.amount = Optional.empty();
    this.yearMatcher = Optional.empty();
    this.useKeyEffective = Optional.empty();
  }

  /**
   * Sets the stream identifier.
   *
   * @param stream the stream identifier to modify
   * @return this builder for method chaining
   */
  public ChangeExecutorConfigBuilder setStream(String stream) {
    this.stream = Optional.of(stream);
    return this;
  }

  /**
   * Sets the change amount.
   *
   * @param amount the change amount (percentage, units, or kg)
   * @return this builder for method chaining
   */
  public ChangeExecutorConfigBuilder setAmount(EngineNumber amount) {
    this.amount = Optional.of(amount);
    return this;
  }

  /**
   * Sets the year matcher.
   *
   * @param yearMatcher matcher to determine if the change applies to current year
   * @return this builder for method chaining
   */
  public ChangeExecutorConfigBuilder setYearMatcher(YearMatcher yearMatcher) {
    this.yearMatcher = Optional.ofNullable(yearMatcher);
    return this;
  }

  /**
   * Sets the effective use key.
   *
   * @param useKeyEffective the effective UseKey for the operation
   * @return this builder for method chaining
   */
  public ChangeExecutorConfigBuilder setUseKeyEffective(UseKey useKeyEffective) {
    this.useKeyEffective = Optional.of(useKeyEffective);
    return this;
  }

  /**
   * Builds the ChangeExecutorConfig.
   *
   * @return the built ChangeExecutorConfig
   * @throws IllegalStateException if required fields are not set
   */
  public ChangeExecutorConfig build() {
    if (stream.isEmpty()) {
      throw new IllegalStateException("Stream is required");
    }
    if (amount.isEmpty()) {
      throw new IllegalStateException("Amount is required");
    }
    if (useKeyEffective.isEmpty()) {
      throw new IllegalStateException("UseKeyEffective is required");
    }

    return new ChangeExecutorConfig(
        stream.get(), amount.get(), yearMatcher.orElse(null), useKeyEffective.get());
  }
}
