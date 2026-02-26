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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.kigalisim.KigaliSimFacade;
import org.kigalisim.engine.serializer.EngineResult;
import org.kigalisim.lang.parse.ParseResult;
import org.kigalisim.lang.program.ParsedProgram;

/**
 * Lambda handler that accepts a QubecTalk script via query string and returns CSV output.
 *
 * <p>Implements the AWS Lambda Function URL / API Gateway HTTP API contract. If a
 * {@code simulation} parameter is provided, the handler runs exactly one replicate of
 * the named scenario and returns the results as a CSV string with
 * {@code Content-Type: text/csv}. If {@code simulation} is omitted, the script is
 * validated only and a header-only CSV is returned with status 200.</p>
 */
public class SimulationHandler
    implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

  private static final int STATUS_OK = 200;
  private static final int STATUS_BAD_REQUEST = 400;
  private static final int STATUS_UNPROCESSABLE = 422;
  private static final int STATUS_SERVER_ERROR = 500;

  private static final String CONTENT_TYPE_CSV = "text/csv";
  private static final String CONTENT_TYPE_TEXT = "text/plain";

  private final CloudQubectalkPreprocessor preprocessor;

  /**
   * Constructs a new SimulationHandler with a default {@link CloudQubectalkPreprocessor}.
   */
  public SimulationHandler() {
    this.preprocessor = new CloudQubectalkPreprocessor();
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
      return handleRequestUnsafe(event);
    } catch (Exception e) {
      return buildResponse(
          STATUS_SERVER_ERROR,
          CONTENT_TYPE_TEXT,
          e.getMessage()
      );
    }
  }

  /**
   * Handles an incoming Lambda HTTP event, allowing exceptions to propagate.
   *
   * @param event The API Gateway V2 HTTP event containing query string parameters.
   * @return An API Gateway V2 HTTP response with either CSV output or a plain-text error.
   * @throws Exception if an unexpected error occurs during processing.
   */
  private APIGatewayV2HTTPResponse handleRequestUnsafe(
      APIGatewayV2HTTPEvent event) throws Exception {
    InvocationParameters params = InvocationParametersFactory.build(
        event.getQueryStringParameters()
    );

    Optional<String> script = params.getScript();
    if (!script.isPresent()) {
      return buildResponse(
          STATUS_BAD_REQUEST,
          CONTENT_TYPE_TEXT,
          "Missing required parameter: script"
      );
    }

    String processedScript = preprocessor.preprocess(script.get());
    ParseResult parseResult = KigaliSimFacade.parse(processedScript);
    if (parseResult.hasErrors()) {
      return buildResponse(
          STATUS_UNPROCESSABLE,
          CONTENT_TYPE_TEXT,
          KigaliSimFacade.getDetailedErrorMessage(parseResult)
      );
    }

    ParsedProgram program = KigaliSimFacade.interpret(parseResult);

    Optional<String> simulation = params.getSimulation();
    if (!simulation.isPresent()) {
      String csv = KigaliSimFacade.convertResultsToCsv(Collections.emptyList());
      return buildResponse(STATUS_OK, CONTENT_TYPE_CSV, csv);
    }

    String scenarioName;
    try {
      scenarioName = resolveScenarioName(program, simulation.get());
    } catch (IllegalArgumentException e) {
      return buildResponse(
          STATUS_BAD_REQUEST,
          CONTENT_TYPE_TEXT,
          e.getMessage()
      );
    }

    List<EngineResult> results = KigaliSimFacade.runScenario(
        program,
        scenarioName,
        null
    ).collect(Collectors.toList());

    String csv = KigaliSimFacade.convertResultsToCsv(results);
    return buildResponse(STATUS_OK, CONTENT_TYPE_CSV, csv);
  }

  /**
   * Resolves the scenario name to use for the simulation.
   *
   * <p>The provided {@code simulationParam} must match an existing scenario name;
   * otherwise an {@link IllegalArgumentException} is thrown with a descriptive message
   * listing the available scenario names.</p>
   *
   * @param program The parsed program whose scenario names are consulted.
   * @param simulationParam The value of the {@code simulation} query parameter.
   * @return The resolved scenario name.
   * @throws IllegalArgumentException if the name cannot be resolved.
   */
  private String resolveScenarioName(ParsedProgram program, String simulationParam) {
    Set<String> scenarios = program.getScenarios();
    if (!scenarios.contains(simulationParam)) {
      StringBuilder message = new StringBuilder();
      message.append("Unknown simulation: ");
      message.append(simulationParam);
      message.append(". Available: ");
      message.append(String.join(", ", scenarios));
      throw new IllegalArgumentException(message.toString());
    }
    return simulationParam;
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
