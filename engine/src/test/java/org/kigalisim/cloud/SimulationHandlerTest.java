/**
 * Unit tests for the SimulationHandler class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the SimulationHandler Lambda handler.
 */
public class SimulationHandlerTest {

  private SimulationHandler handler;
  private String oneScenarioScript;
  private String twoScenarioScript;

  /**
   * Set up a fresh SimulationHandler and load test scripts before each test.
   *
   * @throws IOException if the example QTA files cannot be read.
   */
  @BeforeEach
  public void setUp() throws IOException {
    handler = new SimulationHandler();
    oneScenarioScript = Files.readString(Paths.get("../examples/cloud_one_scenario.qta"));
    twoScenarioScript = Files.readString(Paths.get("../examples/cloud_two_scenario.qta"));
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
   * Test that a valid script with no simulation param validates only and returns HTTP 200.
   */
  @Test
  public void testOneScenarioNoSimParamReturns200() {
    Map<String, String> params = new HashMap<>();
    params.put("script", oneScenarioScript);
    APIGatewayV2HTTPEvent event = buildEvent(params);
    APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);
    assertEquals(200, response.getStatusCode(),
        "Valid script with no simulation param should return 200");
    assertFalse(response.getBody().isBlank(),
        "Validate-only response should contain CSV header row");
  }

  /**
   * Test that omitting the simulation param always validates only, returning the CSV
   * header row regardless of how many scenarios the script defines.
   */
  @Test
  public void testNoSimParamReturnsHeaderOnlyCsv() {
    Map<String, String> params = new HashMap<>();
    params.put("script", twoScenarioScript);
    APIGatewayV2HTTPEvent event = buildEvent(params);
    APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);
    assertEquals(200, response.getStatusCode(),
        "No simulation param should return 200 regardless of scenario count");
    assertTrue(response.getBody().contains("scenario,trial,year"),
        "Validate-only response body should contain CSV header row");
  }

  /**
   * Test that a valid single-scenario script with an explicit matching simulation param
   * returns HTTP 200 with CSV data.
   */
  @Test
  public void testOneScenarioExplicitSimParamReturns200() {
    Map<String, String> params = new HashMap<>();
    params.put("script", oneScenarioScript);
    params.put("simulation", "Business as Usual");
    APIGatewayV2HTTPEvent event = buildEvent(params);
    APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);
    assertEquals(200, response.getStatusCode(),
        "Explicit matching simulation param should return 200");
  }

  /**
   * Test that a two-scenario script with a valid simulation param returns HTTP 200.
   */
  @Test
  public void testTwoScenariosValidSimParamReturns200() {
    Map<String, String> params = new HashMap<>();
    params.put("script", twoScenarioScript);
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
    params.put("script", oneScenarioScript);
    params.put("simulation", "Nonexistent Scenario");
    APIGatewayV2HTTPEvent event = buildEvent(params);
    APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);
    assertEquals(400, response.getStatusCode(),
        "Unknown simulation name should return 400");
  }

  /**
   * Test that a comma-separated list of two valid simulation names returns HTTP 200.
   */
  @Test
  public void testTwoSimsCommaSeparatedReturns200() {
    Map<String, String> params = new HashMap<>();
    params.put("script", twoScenarioScript);
    params.put("simulation", "Business as Usual,Scenario Two");
    APIGatewayV2HTTPEvent event = buildEvent(params);
    APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);
    assertEquals(200, response.getStatusCode(),
        "Comma-separated valid simulation names should return 200");
  }

  /**
   * Test that results from both scenarios appear in the CSV when two names are requested.
   */
  @Test
  public void testTwoSimsCommaSeparatedContainsBothScenarios() {
    Map<String, String> params = new HashMap<>();
    params.put("script", twoScenarioScript);
    params.put("simulation", "Business as Usual,Scenario Two");
    APIGatewayV2HTTPEvent event = buildEvent(params);
    APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);
    assertEquals(200, response.getStatusCode());
    assertTrue(response.getBody().contains("Business as Usual"),
        "Combined CSV should contain results for first scenario");
    assertTrue(response.getBody().contains("Scenario Two"),
        "Combined CSV should contain results for second scenario");
  }

  /**
   * Test that an unknown simulation name anywhere in a comma-separated list returns HTTP 400.
   */
  @Test
  public void testOneUnknownInCommaSeparatedListReturns400() {
    Map<String, String> params = new HashMap<>();
    params.put("script", twoScenarioScript);
    params.put("simulation", "Business as Usual,Nonexistent");
    APIGatewayV2HTTPEvent event = buildEvent(params);
    APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);
    assertEquals(400, response.getStatusCode(),
        "Unknown simulation name in comma-separated list should return 400");
  }

  /**
   * Test that spaces around commas in the simulation parameter are trimmed correctly.
   */
  @Test
  public void testSimParamWithSpacesAroundCommaReturns200() {
    Map<String, String> params = new HashMap<>();
    params.put("script", twoScenarioScript);
    params.put("simulation", "Business as Usual , Scenario Two");
    APIGatewayV2HTTPEvent event = buildEvent(params);
    APIGatewayV2HTTPResponse response = handler.handleRequest(event, null);
    assertEquals(200, response.getStatusCode(),
        "Spaces around comma delimiter in simulation param should be trimmed");
  }

}
