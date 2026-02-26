/**
 * Parameters extracted from a cloud simulation invocation request.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.cloud;

import java.util.List;
import java.util.Optional;

/**
 * Holds the parameters for a cloud simulation invocation.
 *
 * <p>Wraps the {@code script} and {@code simulation} query parameters extracted from an
 * incoming HTTP request. The {@code script} field uses {@link Optional} to distinguish
 * an absent script from a blank one. The {@code simulations} field holds a
 * {@link List} of comma-separated scenario names; an empty list indicates a
 * validate-only request (no simulation names were provided).</p>
 */
public class InvocationParameters {

  private final Optional<String> script;
  private final List<String> simulations;

  /**
   * Constructs a new InvocationParameters.
   *
   * @param script An {@link Optional} containing the QubecTalk script, or empty if not provided.
   * @param simulations A {@link List} of simulation names parsed from the {@code simulation}
   *     query parameter; empty if the parameter was absent or blank.
   */
  public InvocationParameters(Optional<String> script, List<String> simulations) {
    this.script = script;
    this.simulations = simulations;
  }

  /**
   * Returns the QubecTalk script parameter.
   *
   * @return An {@link Optional} containing the script, or empty if not provided.
   */
  public Optional<String> getScript() {
    return script;
  }

  /**
   * Returns the list of simulation names to run.
   *
   * <p>Returns an empty list when no {@code simulation} query parameter was provided,
   * indicating that the request is validate-only.</p>
   *
   * @return An unmodifiable {@link List} of simulation names; never {@code null}.
   */
  public List<String> getSimulations() {
    return simulations;
  }

}
