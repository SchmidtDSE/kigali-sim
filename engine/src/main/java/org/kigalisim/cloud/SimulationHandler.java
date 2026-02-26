/**
 * AWS Lambda handler for running QubecTalk simulations via HTTP.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.cloud;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.kigalisim.KigaliSimFacade;
import org.kigalisim.engine.serializer.EngineResult;
import org.kigalisim.lang.parse.ParseResult;
import org.kigalisim.lang.program.ParsedProgram;

/**
 * Lambda handler that accepts a QubecTalk script via query string and returns CSV output.
 *
 * <p>Implements the AWS Lambda Function URL / API Gateway HTTP API contract. The handler
 * runs exactly one replicate of the named (or sole) scenario and returns the results as
 * a CSV string with {@code Content-Type: text/csv}.</p>
 */
public class SimulationHandler
    implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

  private static final int STATUS_OK = 200;
  private static final int STATUS_BAD_REQUEST = 400;
  private static final int STATUS_UNPROCESSABLE = 422;
  private static final int STATUS_SERVER_ERROR = 500;

  private static final String CONTENT_TYPE_CSV = "text/csv";
  private static final String CONTENT_TYPE_TEXT = "text/plain";

  /**
   * Constructs a new SimulationHandler.
   */
  public SimulationHandler() {
  }

  /**
   * Handles an incoming Lambda HTTP event by running a QubecTalk simulation.
   *
   * @param event The API Gateway V2 HTTP event containing query string parameters.
   * @param context The Lambda execution context (unused).
   * @return An API Gateway V2 HTTP response with either CSV output or a plain-text error.
   */
  @Override
  public APIGatewayV2HTTPResponse handleRequest(
      APIGatewayV2HTTPEvent event, Context context) {
    try {
      String script = extractScript(event);
      if (script == null) {
        return buildResponse(STATUS_BAD_REQUEST, CONTENT_TYPE_TEXT,
            "Missing required parameter: script");
      }

      ParseResult parseResult = KigaliSimFacade.parse(script);
      if (parseResult.hasErrors()) {
        return buildResponse(STATUS_UNPROCESSABLE, CONTENT_TYPE_TEXT,
            KigaliSimFacade.getDetailedErrorMessage(parseResult));
      }

      ParsedProgram program = KigaliSimFacade.interpret(parseResult);

      String simulationParam = null;
      Map<String, String> params = event.getQueryStringParameters();
      if (params != null) {
        simulationParam = params.get("simulation");
      }

      String scenarioName;
      try {
        scenarioName = resolveScenarioName(program, simulationParam);
      } catch (IllegalArgumentException e) {
        return buildResponse(STATUS_BAD_REQUEST, CONTENT_TYPE_TEXT, e.getMessage());
      }

      List<EngineResult> results =
          KigaliSimFacade.runScenario(program, scenarioName, null)
              .collect(Collectors.toList());

      String csv = KigaliSimFacade.convertResultsToCsv(results);
      return buildResponse(STATUS_OK, CONTENT_TYPE_CSV, csv);

    } catch (Exception e) {
      return buildResponse(STATUS_SERVER_ERROR, CONTENT_TYPE_TEXT, e.getMessage());
    }
  }

  /**
   * Extracts the {@code script} query parameter from the event.
   *
   * @param event The incoming API Gateway event.
   * @return The script string, or {@code null} if the parameter is absent or blank.
   */
  private String extractScript(APIGatewayV2HTTPEvent event) {
    Map<String, String> params = event.getQueryStringParameters();
    if (params == null) {
      return null;
    }
    String script = params.get("script");
    if (script == null || script.isBlank()) {
      return null;
    }
    return script;
  }

  /**
   * Resolves the scenario name to use for the simulation.
   *
   * <p>If {@code simulationParam} is non-null it must match an existing scenario name. If
   * it is null and the program defines exactly one scenario, that scenario's name is used.
   * Otherwise an {@link IllegalArgumentException} is thrown with a descriptive message
   * listing the available scenario names.</p>
   *
   * @param program The parsed program whose scenario names are consulted.
   * @param simulationParam The value of the {@code simulation} query parameter, or
   *     {@code null} if the parameter was not provided.
   * @return The resolved scenario name.
   * @throws IllegalArgumentException if the name cannot be resolved.
   */
  private String resolveScenarioName(ParsedProgram program, String simulationParam) {
    Set<String> scenarios = program.getScenarios();
    String available = String.join(", ", scenarios);

    if (simulationParam != null) {
      if (!scenarios.contains(simulationParam)) {
        throw new IllegalArgumentException(
            "Unknown simulation: " + simulationParam + ". Available: " + available);
      }
      return simulationParam;
    }

    if (scenarios.size() == 1) {
      return scenarios.iterator().next();
    }

    throw new IllegalArgumentException(
        "Parameter 'simulation' is required when the script defines multiple simulations."
            + " Available: " + available);
  }

  /**
   * Constructs an {@link APIGatewayV2HTTPResponse} with the given status, content type, and
   * body.
   *
   * @param statusCode HTTP status code.
   * @param contentType Value for the {@code Content-Type} response header.
   * @param body Response body string.
   * @return A fully constructed response object.
   */
  private APIGatewayV2HTTPResponse buildResponse(
      int statusCode, String contentType, String body) {
    return APIGatewayV2HTTPResponse.builder()
        .withStatusCode(statusCode)
        .withHeaders(Map.of("Content-Type", contentType))
        .withBody(body)
        .build();
  }

}
