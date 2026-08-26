/**
 * Factory for creating InvocationParameters from an HTTP query string map.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.cloud;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Builds {@link InvocationParameters} from a raw query string parameter map.
 *
 * <p>Centralises all raw-map access so that adding new invocation parameters in the
 * future requires changes only to this class and {@link InvocationParameters}.</p>
 */
public class InvocationParametersFactory {

  /**
   * Builds an {@link InvocationParameters} from an incoming Lambda HTTP event.
   *
   * <p>Parameters may be supplied either as GET query string parameters or as a
   * {@code application/x-www-form-urlencoded} POST body, allowing callers with larger
   * scripts to avoid URL length limits by sending the script as a POST parameter instead.
   * When a parameter is present in both the query string and the body, the body value
   * takes precedence.</p>
   *
   * @param event The incoming API Gateway V2 HTTP event.
   * @return A new {@link InvocationParameters} with the extracted values.
   */
  public static InvocationParameters build(APIGatewayV2HTTPEvent event) {
    Map<String, String> combined = new HashMap<>();

    Map<String, String> queryParams = event.getQueryStringParameters();
    if (queryParams != null) {
      combined.putAll(queryParams);
    }

    combined.putAll(parseFormBody(event));

    return build(combined);
  }

  /**
   * Builds an {@link InvocationParameters} from the given query string parameter map.
   *
   * <p>A {@code script} value that is present but blank is treated as absent. A
   * {@code simulation} value is split on commas and trimmed to produce a list of scenario
   * names; an absent or blank value produces an empty list. A {@code replicates} value is
   * parsed as a positive integer; if absent or blank it defaults to 1, and if non-integer it
   * is stored as 0 (the handler treats any value less than 1 as invalid and returns 400).</p>
   *
   * @param params The query string parameter map, or {@code null} if none were provided.
   * @return A new {@link InvocationParameters} with the extracted values.
   */
  public static InvocationParameters build(Map<String, String> params) {
    if (params == null) {
      return new InvocationParameters(Optional.empty(), Collections.emptyList(), 1);
    }

    String script = params.get("script");
    if (script != null && script.isBlank()) {
      script = null;
    }

    String simulationRaw = params.get("simulation");
    List<String> simulations;
    if (simulationRaw == null || simulationRaw.isBlank()) {
      simulations = Collections.emptyList();
    } else {
      simulations = Arrays.stream(simulationRaw.split(","))
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .collect(Collectors.toList());
    }

    String replicatesRaw = params.get("replicates");
    int replicates;
    if (replicatesRaw == null || replicatesRaw.isBlank()) {
      replicates = 1;
    } else {
      try {
        replicates = Integer.parseInt(replicatesRaw.trim());
      } catch (NumberFormatException e) {
        replicates = 0;
      }
    }

    return new InvocationParameters(
        Optional.ofNullable(script),
        simulations,
        replicates
    );
  }

  /**
   * Parses an {@code application/x-www-form-urlencoded} request body into a parameter map.
   *
   * @param event The incoming API Gateway V2 HTTP event.
   * @return A map of parameter names to values parsed from the body, empty if the body is
   *     absent or blank.
   */
  private static Map<String, String> parseFormBody(APIGatewayV2HTTPEvent event) {
    String body = event.getBody();
    if (body == null || body.isBlank()) {
      return Collections.emptyMap();
    }

    if (event.getIsBase64Encoded()) {
      body = new String(Base64.getDecoder().decode(body), StandardCharsets.UTF_8);
    }

    Map<String, String> parsed = new HashMap<>();
    for (String pair : body.split("&")) {
      if (pair.isEmpty()) {
        continue;
      }
      int separatorIndex = pair.indexOf('=');
      String key = separatorIndex < 0 ? pair : pair.substring(0, separatorIndex);
      String value = separatorIndex < 0 ? "" : pair.substring(separatorIndex + 1);
      parsed.put(
          URLDecoder.decode(key, StandardCharsets.UTF_8),
          URLDecoder.decode(value, StandardCharsets.UTF_8)
      );
    }
    return parsed;
  }

}
