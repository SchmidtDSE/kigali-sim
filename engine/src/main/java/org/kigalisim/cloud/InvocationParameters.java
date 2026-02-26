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

  private final String script;
  private final String simulation;

  /**
   * Constructs a new InvocationParameters.
   *
   * @param script The QubecTalk script string, or {@code null} if not provided.
   * @param simulation The simulation name, or {@code null} if not provided.
   */
  public InvocationParameters(String script, String simulation) {
    this.script = script;
    this.simulation = simulation;
  }

  /**
   * Returns the QubecTalk script parameter.
   *
   * @return An {@link Optional} containing the script, or empty if not provided.
   */
  public Optional<String> getScript() {
    return Optional.ofNullable(script);
  }

  /**
   * Returns the simulation name parameter.
   *
   * @return An {@link Optional} containing the simulation name, or empty if not provided.
   */
  public Optional<String> getSimulation() {
    return Optional.ofNullable(simulation);
  }

}
