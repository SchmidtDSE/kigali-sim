/**
 * Unit tests for the SimulationHandler class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the SimulationHandler Lambda handler.
 */
public class SimulationHandlerTest {

  private SimulationHandler handler;

  // Minimal valid QubecTalk script defining exactly one simulation scenario.
  private static final String ONE_SCENARIO_SCRIPT =
      "start default\n"
      + "  define application \"Test App\"\n"
      + "    uses substance \"HFC-134a\"\n"
      + "      enable domestic\n"
      + "      initial charge with 1 kg / unit for domestic\n"
      + "      initial charge with 0 kg / unit for import\n"
      + "      initial charge with 0 kg / unit for export\n"
      + "      equals 1430 kgCO2e / kg\n"
      + "      equals 1 kwh / unit\n"
      + "      set sales to 1 mt during year 1\n"
      + "      retire 5 % / year\n"
      + "    end substance\n"
      + "  end application\n"
      + "end default\n"
      + "\n"
      + "start simulations\n"
      + "  simulate \"Business as Usual\"\n"
      + "  from years 1 to 3\n"
      + "end simulations\n";

  // Valid QubecTalk script defining two simulation scenarios.
  private static final String TWO_SCENARIO_SCRIPT =
      "start default\n"
      + "  define application \"Test App\"\n"
      + "    uses substance \"HFC-134a\"\n"
      + "      enable domestic\n"
      + "      initial charge with 1 kg / unit for domestic\n"
      + "      initial charge with 0 kg / unit for import\n"
      + "      initial charge with 0 kg / unit for export\n"
      + "      equals 1430 kgCO2e / kg\n"
      + "      equals 1 kwh / unit\n"
      + "      set sales to 1 mt during year 1\n"
      + "      retire 5 % / year\n"
      + "    end substance\n"
      + "  end application\n"
      + "end default\n"
      + "\n"
      + "start simulations\n"
      + "  simulate \"Business as Usual\"\n"
      + "  from years 1 to 3\n"
      + "\n"
      + "  simulate \"Scenario Two\"\n"
      + "  from years 1 to 3\n"
      + "end simulations\n";

  /**
   * Set up a fresh SimulationHandler before each test.
   */
  @BeforeEach
  public void setUp() {
    handler = new SimulationHandler();
  }

  /**
   * Builds a minimal APIGatewayV2HTTPEvent with the given query string parameters.
   *
   * @param params Query string parameters map, or null to simulate no parameters.
   * @return A constructed event object.
   */
  private APIGatewayV2HTTPEvent buildEvent(Map<String, String> params) {
    return APIGatewayV2HTTPEvent.builder()
        .withQueryStringParameters(params)
        .build();
  }

  /**
   * Test that a missing script parameter returns HTTP 400.
   */
  @Test
  public void testMissingScriptReturns400() {
    APIGatewayV2HTTPEvent event = buildEvent(null);
    APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);
    assertEquals(400, response.getStatusCode(),
        "Missing script param should return 400");
  }

  /**
   * Test that a blank script parameter returns HTTP 400.
   */
  @Test
  public void testBlankScriptReturns400() {
    Map<String, String> params = new HashMap<>();
    params.put("script", "   ");
    APIGatewayV2HTTPEvent event = buildEvent(params);
    APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);
    assertEquals(400, response.getStatusCode(),
        "Blank script param should return 400");
  }

  /**
   * Test that an invalid QubecTalk script returns HTTP 422 with a parse error message.
   */
  @Test
  public void testInvalidQubecTalkReturns422() {
    Map<String, String> params = new HashMap<>();
    params.put("script", "not valid qubectalk");
    APIGatewayV2HTTPEvent event = buildEvent(params);
    APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);
    assertEquals(422, response.getStatusCode(),
        "Invalid QubecTalk should return 422");
    assertFalse(response.getBody().isBlank(),
        "Response body should contain a parse error message");
  }

  /**
   * Test that a valid single-scenario script with no simulation param returns HTTP 200 with CSV.
   */
  @Test
  public void testOneScenarioNoSimParamReturns200() {
    Map<String, String> params = new HashMap<>();
    params.put("script", ONE_SCENARIO_SCRIPT);
    APIGatewayV2HTTPEvent event = buildEvent(params);
    APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);
    assertEquals(200, response.getStatusCode(),
        "Valid single-scenario script should return 200");
    assertFalse(response.getBody().isBlank(),
        "Response body should contain CSV output");
  }

  /**
   * Test that a valid single-scenario script with an explicit matching simulation param
   * returns HTTP 200.
   */
  @Test
  public void testOneScenarioExplicitSimParamReturns200() {
    Map<String, String> params = new HashMap<>();
    params.put("script", ONE_SCENARIO_SCRIPT);
    params.put("simulation", "Business as Usual");
    APIGatewayV2HTTPEvent event = buildEvent(params);
    APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);
    assertEquals(200, response.getStatusCode(),
        "Explicit matching simulation param should return 200");
  }

  /**
   * Test that a two-scenario script with no simulation param returns HTTP 400.
   */
  @Test
  public void testTwoScenariosNoSimParamReturns400() {
    Map<String, String> params = new HashMap<>();
    params.put("script", TWO_SCENARIO_SCRIPT);
    APIGatewayV2HTTPEvent event = buildEvent(params);
    APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);
    assertEquals(400, response.getStatusCode(),
        "Two-scenario script without simulation param should return 400");
  }

  /**
   * Test that a two-scenario script with a valid simulation param returns HTTP 200.
   */
  @Test
  public void testTwoScenariosValidSimParamReturns200() {
    Map<String, String> params = new HashMap<>();
    params.put("script", TWO_SCENARIO_SCRIPT);
    params.put("simulation", "Scenario Two");
    APIGatewayV2HTTPEvent event = buildEvent(params);
    APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);
    assertEquals(200, response.getStatusCode(),
        "Two-scenario script with valid simulation param should return 200");
  }

  /**
   * Test that a script with an unknown simulation param returns HTTP 400.
   */
  @Test
  public void testUnknownSimParamReturns400() {
    Map<String, String> params = new HashMap<>();
    params.put("script", ONE_SCENARIO_SCRIPT);
    params.put("simulation", "Nonexistent Scenario");
    APIGatewayV2HTTPEvent event = buildEvent(params);
    APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);
    assertEquals(400, response.getStatusCode(),
        "Unknown simulation name should return 400");
  }

}
