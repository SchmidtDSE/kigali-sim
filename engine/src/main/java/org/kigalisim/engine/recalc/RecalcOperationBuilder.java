/**
 * Builder for creating RecalcOperation instances with fluent interface.
 *
 * <p>This builder provides a fluent interface for constructing sequences of
 * recalculation operations while enforcing proper ordering (recalc methods first,
 * then propagate methods).</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.kigalisim.engine.state.UseKey;

/**
 * Builder for creating RecalcOperation instances with fluent interface.
 */
public class RecalcOperationBuilder {

  private Optional<UseKey> scopeEffective;
  private Optional<Boolean> subtractRecharge;
  private Optional<RecalcKit> recalcKit;
  private final List<RecalcStrategy> strategies;
  private boolean hasInitialRecalc;

  /**
   * Create a new RecalcOperationBuilder.
   */
  public RecalcOperationBuilder() {
    scopeEffective = Optional.empty();
    subtractRecharge = Optional.empty();
    recalcKit = Optional.empty();
    this.strategies = new ArrayList<>();
    hasInitialRecalc = false;
  }

  /**
   * Set the scope for all recalculation operations.
   *
   * @param scope The scope to use for calculations
   * @return This builder for method chaining
   */
  public RecalcOperationBuilder setScopeEffective(UseKey scope) {
    scopeEffective = Optional.ofNullable(scope);
    return this;
  }

  /**
   * Set whether to subtract recharge in population calculations.
   *
   * @param subtractRecharge Whether to subtract recharge
   * @return This builder for method chaining
   */
  public RecalcOperationBuilder setSubtractRecharge(Boolean subtractRecharge) {
    this.subtractRecharge = Optional.ofNullable(subtractRecharge);
    return this;
  }

  /**
   * Set the RecalcKit containing dependencies for recalculation operations.
   *
   * @param recalcKit The recalc kit to use
   * @return This builder for method chaining
   */
  public RecalcOperationBuilder setRecalcKit(RecalcKit recalcKit) {
    this.recalcKit = Optional.ofNullable(recalcKit);
    return this;
  }

  /**
   * Add population change recalculation to the operation sequence.
   *
   * @return This builder for method chaining
   */
  public RecalcOperationBuilder recalcPopulationChange() {
    if (hasInitialRecalc) {
      throw new IllegalStateException(
          "Only one recalc method can be called - use thenPropagate methods "
          + "for subsequent operations"
      );
    }
    UseKey keyRealized = scopeEffective.orElse(null);
    Boolean subtractRechargeRealized = subtractRecharge.orElse(null);
    strategies.add(
        new PopulationChangeRecalcStrategy(
            keyRealized,
            subtractRechargeRealized
        )
    );
    hasInitialRecalc = true;
    return this;
  }

  /**
   * Add consumption recalculation to the operation sequence.
   *
   * @return This builder for method chaining
   */
  public RecalcOperationBuilder recalcConsumption() {
    if (hasInitialRecalc) {
      throw new IllegalStateException(
          "Only one recalc method can be called - use thenPropagate methods "
          + "for subsequent operations"
      );
    }
    UseKey scopeEffectiveRealized = scopeEffective.orElse(null);
    strategies.add(new ConsumptionRecalcStrategy(scopeEffectiveRealized));
    hasInitialRecalc = true;
    return this;
  }

  /**
   * Add sales recalculation to the operation sequence.
   *
   * @return This builder for method chaining
   */
  public RecalcOperationBuilder recalcSales() {
    if (hasInitialRecalc) {
      throw new IllegalStateException(
          "Only one recalc method can be called - use thenPropagate methods "
          + "for subsequent operations"
      );
    }
    UseKey scopeEffectiveRealized = scopeEffective.orElse(null);
    strategies.add(new SalesRecalcStrategy(scopeEffectiveRealized));
    hasInitialRecalc = true;
    return this;
  }

  /**
   * Add recharge emissions recalculation to the operation sequence.
   *
   * @return This builder for method chaining
   */
  public RecalcOperationBuilder recalcRechargeEmissions() {
    if (hasInitialRecalc) {
      throw new IllegalStateException(
          "Only one recalc method can be called - use thenPropagate methods "
          + "for subsequent operations"
      );
    }
    UseKey scopeEffectiveRealized = scopeEffective.orElse(null);
    strategies.add(new RechargeEmissionsRecalcStrategy(scopeEffectiveRealized));
    hasInitialRecalc = true;
    return this;
  }

  /**
   * Add end-of-life emissions recalculation to the operation sequence.
   *
   * @return This builder for method chaining
   */
  public RecalcOperationBuilder recalcEolEmissions() {
    if (hasInitialRecalc) {
      throw new IllegalStateException(
          "Only one recalc method can be called - use thenPropagate methods "
          + "for subsequent operations"
      );
    }
    UseKey scopeEffectiveRealized = scopeEffective.orElse(null);
    strategies.add(new EolEmissionsRecalcStrategy(scopeEffectiveRealized));
    hasInitialRecalc = true;
    return this;
  }

  /**
   * Add retirement recalculation to the operation sequence.
   *
   * @return This builder for method chaining
   */
  public RecalcOperationBuilder recalcRetire() {
    if (hasInitialRecalc) {
      throw new IllegalStateException(
          "Only one recalc method can be called - use thenPropagate methods "
          + "for subsequent operations"
      );
    }
    UseKey scopeEffectiveRealized = scopeEffective.orElse(null);
    strategies.add(new RetireRecalcStrategy(scopeEffectiveRealized));
    hasInitialRecalc = true;
    return this;
  }

  /**
   * Propagate changes to population calculation.
   *
   * @return This builder for method chaining
   */
  public RecalcOperationBuilder thenPropagateToPopulationChange() {
    if (!hasInitialRecalc) {
      throw new IllegalStateException(
          "Must call a recalc method before using thenPropagate methods"
      );
    }
    UseKey scopeEffectiveRealized = scopeEffective.orElse(null);
    RecalcStrategy newStrategy = new PopulationChangeRecalcStrategy(
        scopeEffectiveRealized,
        subtractRecharge.orElse(null)
    );
    strategies.add(newStrategy);
    return this;
  }

  /**
   * Propagate changes to consumption calculation.
   *
   * @return This builder for method chaining
   */
  public RecalcOperationBuilder thenPropagateToConsumption() {
    if (!hasInitialRecalc) {
      throw new IllegalStateException(
          "Must call a recalc method before using thenPropagate methods"
      );
    }
    UseKey scopeEffectiveRealized = scopeEffective.orElse(null);
    strategies.add(new ConsumptionRecalcStrategy(scopeEffectiveRealized));
    return this;
  }

  /**
   * Propagate changes to sales calculation.
   *
   * @return This builder for method chaining
   */
  public RecalcOperationBuilder thenPropagateToSales() {
    if (!hasInitialRecalc) {
      throw new IllegalStateException(
          "Must call a recalc method before using thenPropagate methods"
      );
    }
    UseKey scopeEffectiveRealized = scopeEffective.orElse(null);
    strategies.add(new SalesRecalcStrategy(scopeEffectiveRealized));
    return this;
  }

  /**
   * Propagate changes to recharge emissions calculation.
   *
   * @return This builder for method chaining
   */
  public RecalcOperationBuilder thenPropagateToRechargeEmissions() {
    if (!hasInitialRecalc) {
      throw new IllegalStateException(
          "Must call a recalc method before using thenPropagate methods"
      );
    }
    UseKey scopeEffectiveRealized = scopeEffective.orElse(null);
    strategies.add(new RechargeEmissionsRecalcStrategy(scopeEffectiveRealized));
    return this;
  }

  /**
   * Propagate changes to end-of-life emissions calculation.
   *
   * @return This builder for method chaining
   */
  public RecalcOperationBuilder thenPropagateToEolEmissions() {
    if (!hasInitialRecalc) {
      throw new IllegalStateException(
          "Must call a recalc method before using thenPropagate methods"
      );
    }
    UseKey scopeEffectiveRealized = scopeEffective.orElse(null);
    strategies.add(new EolEmissionsRecalcStrategy(scopeEffectiveRealized));
    return this;
  }

  /**
   * Propagate changes to retirement calculation.
   *
   * @return This builder for method chaining
   */
  public RecalcOperationBuilder thenPropagateToRetire() {
    if (!hasInitialRecalc) {
      throw new IllegalStateException(
          "Must call a recalc method before using thenPropagate methods"
      );
    }
    UseKey scopeEffectiveRealized = scopeEffective.orElse(null);
    strategies.add(new RetireRecalcStrategy(scopeEffectiveRealized));
    return this;
  }

  /**
   * Build the RecalcOperation from the configured strategies.
   *
   * @return A RecalcOperation containing all configured strategies
   * @throws IllegalStateException if no strategies have been configured or RecalcKit not provided
   */
  public RecalcOperation build() {
    if (strategies.isEmpty()) {
      throw new IllegalStateException("Must configure at least one recalculation strategy");
    }
    if (!recalcKit.isPresent()) {
      throw new IllegalStateException("RecalcKit is required");
    }
    return new RecalcOperation(new ArrayList<>(strategies), recalcKit.get());
  }
}
