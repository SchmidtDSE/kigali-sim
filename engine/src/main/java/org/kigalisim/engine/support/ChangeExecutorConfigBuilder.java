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
public final class ChangeExecutorConfigBuilder extends ValidatedBuilder<ChangeExecutorConfig> {

  private Optional<String> stream;
  private Optional<EngineNumber> amount;
  private Optional<YearMatcher> yearMatcher;
  private Optional<UseKey> useKeyEffective;

  /**
   * Create a new ChangeExecutorConfigBuilder.
   */
  public ChangeExecutorConfigBuilder() {
    super("ChangeExecutorConfig");
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
   * Check that all required fields are set before construction.
   *
   * @throws IllegalStateException if required fields are not set
   */
  @Override
  protected void validate() {
    requireField(stream, "stream");
    requireField(amount, "amount");
    requireField(useKeyEffective, "useKeyEffective");
  }

  /**
   * Builds the ChangeExecutorConfig.
   *
   * @return the built ChangeExecutorConfig
   */
  @Override
  protected ChangeExecutorConfig buildInternal() {
    return new ChangeExecutorConfig(
        stream.get(),
        amount.get(),
        yearMatcher,
        useKeyEffective.get()
    );
  }
}
