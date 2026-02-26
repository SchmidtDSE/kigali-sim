/**
 * Factory for creating InvocationParameters from an HTTP query string map.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.cloud;

import java.util.Map;
import java.util.Optional;

/**
 * Builds {@link InvocationParameters} from a raw query string parameter map.
 *
 * <p>Centralises all raw-map access so that adding new invocation parameters in the
 * future requires changes only to this class and {@link InvocationParameters}.</p>
 */
public class InvocationParametersFactory {

  /**
   * Builds an {@link InvocationParameters} from the given query string parameter map.
   *
   * <p>A {@code script} value that is present but blank is treated as absent. A
   * {@code simulation} value is taken as-is if present.</p>
   *
   * @param params The query string parameter map, or {@code null} if none were provided.
   * @return A new {@link InvocationParameters} with the extracted values.
   */
  public static InvocationParameters build(Map<String, String> params) {
    if (params == null) {
      return new InvocationParameters(Optional.empty(), Optional.empty());
    }

    String script = params.get("script");
    if (script != null && script.isBlank()) {
      script = null;
    }

    String simulation = params.get("simulation");

    return new InvocationParameters(
        Optional.ofNullable(script),
        Optional.ofNullable(simulation)
    );
  }

}
