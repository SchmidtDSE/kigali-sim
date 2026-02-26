/**
 * Parameters extracted from a cloud simulation invocation request.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.cloud;

import java.util.Optional;

/**
 * Holds the parameters for a cloud simulation invocation.
 *
 * <p>Wraps the {@code script} and {@code simulation} query parameters extracted from an
 * incoming HTTP request, providing typed access with {@link Optional} to avoid null
 * handling at call sites.</p>
 */
public class InvocationParameters {

  private final Optional<String> script;
  private final Optional<String> simulation;

  /**
   * Constructs a new InvocationParameters.
   *
   * @param script An {@link Optional} containing the QubecTalk script, or empty if not provided.
   * @param simulation An {@link Optional} containing the simulation name, or empty if not provided.
   */
  public InvocationParameters(Optional<String> script, Optional<String> simulation) {
    this.script = script;
    this.simulation = simulation;
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
   * Returns the simulation name parameter.
   *
   * @return An {@link Optional} containing the simulation name, or empty if not provided.
   */
  public Optional<String> getSimulation() {
    return simulation;
  }

}
