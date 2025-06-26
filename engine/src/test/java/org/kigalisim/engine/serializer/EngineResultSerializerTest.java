/**
 * Unit tests for the EngineResultSerializer class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.serializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.ConverterStateGetter;
import org.kigalisim.engine.state.UseKey;

/**
 * Tests for the EngineResultSerializer class.
 */
public class EngineResultSerializerTest {

  /**
   * Tests for main body functionality of EngineResultSerializer.
   */
  @Nested
  public class MainBodyTests {

    private Engine mockEngine;
    private ConverterStateGetter stateGetter;
    private EngineResultSerializer serializer;
    private EngineResult result;

    /**
     * Set up test fixtures before each test.
     */
    @BeforeEach
    public void setUp() {
      // Create mock engine with test data
      mockEngine = createMockEngine();
      stateGetter = new ConverterStateGetter(mockEngine);
      serializer = new EngineResultSerializer(mockEngine, stateGetter);
      result = serializer.getResult("commercialRefrigeration", "HFC-134a", 1);
    }

    /**
     * Test that serializer gets manufacture value correctly.
     */
    @Test
    public void testGetsManufactureValue() {
      // Expected: 1600 mt - (10 mt * (1600/(1600+400))) = 1600 - 8 = 1592 mt = 1,592,000 kg
      assertEquals(0, result.getManufacture().getValue().compareTo(BigDecimal.valueOf(1592000)),
          "Manufacture value should be 1,592,000 kg");
      assertEquals("kg", result.getManufacture().getUnits(), "Manufacture units should be kg");
    }

    /**
     * Test that serializer gets import value correctly.
     */
    @Test
    public void testGetsImportValue() {
      // Expected: 400 mt - (10 mt * (400/(1600+400))) = 400 - 2 = 398 mt = 398,000 kg
      assertEquals(0, result.getImport().getValue().compareTo(BigDecimal.valueOf(398000)),
          "Import value should be 398,000 kg");
      assertEquals("kg", result.getImport().getUnits(), "Import units should be kg");
    }

    /**
     * Test that serializer gets recycle value correctly.
     */
    @Test
    public void testGetsRecycleValue() {
      // Expected: 10 mt = 10,000 kg
      assertEquals(0, result.getRecycle().getValue().compareTo(BigDecimal.valueOf(10000)),
          "Recycle value should be 10,000 kg");
      assertEquals("kg", result.getRecycle().getUnits(), "Recycle units should be kg");
    }

    /**
     * Test that serializer gets domestic consumption correctly.
     */
    @Test
    public void testGetsDomesticConsumption() {
      // Expected: 1,592,000 kg * 500 tCO2e / mt * (1mt/1,000kg) = 796,000 tCO2e
      assertEquals(0, result.getDomesticConsumption().getValue()
          .compareTo(BigDecimal.valueOf(796000)),
          "Domestic consumption should be 796,000 tCO2e");
      assertEquals("tCO2e", result.getDomesticConsumption().getUnits(),
          "Domestic consumption units should be tCO2e");
    }

    /**
     * Test that serializer gets import consumption correctly.
     */
    @Test
    public void testGetsImportConsumption() {
      // Expected: 398,000 kg * 500 tCO2e / mt * (1mt/1,000kg) = 199,000 tCO2e
      assertEquals(0, result.getImportConsumption().getValue()
          .compareTo(BigDecimal.valueOf(199000)),
          "Import consumption should be 199,000 tCO2e");
      assertEquals("tCO2e", result.getImportConsumption().getUnits(),
          "Import consumption units should be tCO2e");
    }

    /**
     * Test that serializer gets recycle consumption correctly.
     */
    @Test
    public void testGetsRecycleConsumption() {
      // Expected: 10,000 kg * 500 tCO2e / mt * (1mt/1,000kg) = 5,000 tCO2e
      assertEquals(0, result.getRecycleConsumption().getValue()
          .compareTo(BigDecimal.valueOf(5000)),
          "Recycle consumption should be 5,000 tCO2e");
      assertEquals("tCO2e", result.getRecycleConsumption().getUnits(),
          "Recycle consumption units should be tCO2e");
    }

    /**
     * Test that serializer gets population value correctly.
     */
    @Test
    public void testGetsPopulationValue() {
      // Expected: 1000 units (from prior equipment)
      assertEquals(0, result.getPopulation().getValue().compareTo(BigDecimal.valueOf(1000)),
          "Population should be 1,000 units");
    }

    /**
     * Test that serializer gets population new value correctly.
     */
    @Test
    public void testGetsPopulationNew() {
      // Expected: 1000 units (from new equipment)
      assertEquals(0, result.getPopulationNew().getValue().compareTo(BigDecimal.valueOf(1000)),
          "Population new should be 1,000 units");
    }

    /**
     * Test that serializer gets recharge emissions correctly.
     */
    @Test
    public void testGetsRechargeEmissions() {
      // Expected: 1000 mt converted to tCO2e (500,000) - 5000 tCO2e (recycle consumption)
      // = 495,000 tCO2e
      assertEquals(0, result.getRechargeEmissions().getValue()
          .compareTo(BigDecimal.valueOf(495000)),
          "Recharge emissions should be 495,000 tCO2e");
      assertEquals("tCO2e", result.getRechargeEmissions().getUnits(),
          "Recharge emissions units should be tCO2e");
    }

    /**
     * Test that serializer gets end-of-life emissions correctly.
     */
    @Test
    public void testGetsEolEmissions() {
      // Expected: 100 tCO2e
      assertEquals(0, result.getEolEmissions().getValue().compareTo(BigDecimal.valueOf(100)),
          "EOL emissions should be 100 tCO2e");
      assertEquals("tCO2e", result.getEolEmissions().getUnits(),
          "EOL emissions units should be tCO2e");
    }

    /**
     * Test that serializer gets energy consumption correctly.
     */
    @Test
    public void testGetsEnergyConsumption() {
      // Expected: 5 kwh / kg
      assertEquals(0, result.getEnergyConsumption().getValue().compareTo(BigDecimal.valueOf(5)),
          "Energy consumption should be 5 kwh / kg");
      assertEquals("kwh / kg", result.getEnergyConsumption().getUnits(),
          "Energy consumption units should be kwh / kg");
    }
  }

  /**
   * Tests for import supplement functionality of EngineResultSerializer.
   */
  @Nested
  public class TradeSupplementTests {

    private Engine mockEngine;
    private ConverterStateGetter stateGetter;
    private EngineResultSerializer serializer;
    private EngineResult result;
    private TradeSupplement tradeSupplement;

    /**
     * Set up test fixtures before each test.
     */
    @BeforeEach
    public void setUp() {
      // Create mock engine with test data
      mockEngine = createMockEngineForTradeSupplement();
      stateGetter = new ConverterStateGetter(mockEngine);
      serializer = new EngineResultSerializer(mockEngine, stateGetter);
      result = serializer.getResult("commercialRefrigeration", "HFC-134a", 1);
      tradeSupplement = result.getTradeSupplement();
    }

    /**
     * Test that serializer generates import supplement correctly.
     */
    @Test
    public void testGeneratesTradeSupplement() {
      assertNotNull(tradeSupplement, "Trade supplement should not be null");

      // Calculate: total import (400 mt = 400,000 kg) - import recharge portion
      // Import proportion: 400,000 / (1,600,000 + 400,000) = 0.2
      // Import recharge: 0.2 * 1,000,000 kg = 200,000 kg
      // Import for initial charge: 400,000 - 200,000 = 200,000 kg
      assertEquals(0, tradeSupplement.getImportInitialChargeValue().getValue()
          .compareTo(BigDecimal.valueOf(200000)),
          "Import supplement value should be 200,000 kg");
      assertEquals("kg", tradeSupplement.getImportInitialChargeValue().getUnits(),
          "Import supplement value units should be kg");
    }

    /**
     * Test that serializer generates import supplement consumption correctly.
     */
    @Test
    public void testGeneratesTradeSupplementConsumption() {
      // 200,000 kg * 500 tCO2e/mt * (1 mt/1000 kg) = 100,000 tCO2e
      assertEquals(0, tradeSupplement.getImportInitialChargeConsumption().getValue()
          .compareTo(BigDecimal.valueOf(100000)),
          "Import supplement consumption should be 100,000 tCO2e");
      assertEquals("tCO2e", tradeSupplement.getImportInitialChargeConsumption().getUnits(),
          "Import supplement consumption units should be tCO2e");
    }

    /**
     * Test that serializer generates import supplement units correctly.
     */
    @Test
    public void testGeneratesTradeSupplementUnits() {
      // 200,000 kg / 200 kg/unit = 1,000 units
      assertEquals(0, tradeSupplement.getImportPopulation().getValue()
          .compareTo(BigDecimal.valueOf(1000)),
          "Import supplement units should be 1,000 units");
      assertEquals("units", tradeSupplement.getImportPopulation().getUnits(),
          "Import supplement units should be units");
    }
  }

  /**
   * Create a mock engine with test data matching the JavaScript tests.
   *
   * @return Mock engine configured with test data
   */
  private static Engine createMockEngine() {
    Engine engine = mock(Engine.class);

    // Test data matching JavaScript tests
    EngineNumber manufacture = new EngineNumber(1600, "mt");
    EngineNumber importMt = new EngineNumber(400, "mt");
    EngineNumber exportMt = new EngineNumber(200, "mt");
    EngineNumber recharge = new EngineNumber(1000, "mt");
    EngineNumber valueToConsumption = new EngineNumber(500, "tCO2e / mt");
    EngineNumber initialChargeImport = new EngineNumber(200, "kg / unit");
    EngineNumber initialChargeDomestic = new EngineNumber(150, "kg / unit");
    EngineNumber recycling = new EngineNumber(10, "mt");
    EngineNumber energyIntensity = new EngineNumber(5, "kwh / kg");
    EngineNumber priorEquipment = new EngineNumber(1000, "units");
    EngineNumber eolEmissions = new EngineNumber(100, "tCO2e");


    // Configure mock responses for getStreamRaw with UseKey
    when(engine.getStreamFor(any(UseKey.class), any(String.class))).thenAnswer(invocation -> {
      UseKey useKey = invocation.getArgument(0);
      String stream = invocation.getArgument(1);

      if ("commercialRefrigeration".equals(useKey.getApplication())
          && "HFC-134a".equals(useKey.getSubstance())) {
        switch (stream) {
          case "manufacture": return manufacture;
          case "import": return importMt;
          case "export": return exportMt;
          case "recycle": return recycling;
          case "energy": return energyIntensity;
          case "equipment": return priorEquipment;
          case "newEquipment": return priorEquipment;
          case "rechargeEmissions": return recharge;
          case "eolEmissions": return eolEmissions;
          default: return null;
        }
      }
      return null;
    });

    // Configure GHG intensity methods
    when(engine.getGhgIntensity(any(UseKey.class))).thenAnswer(invocation -> {
      UseKey useKey = invocation.getArgument(0);
      if ("commercialRefrigeration".equals(useKey.getApplication())
          && "HFC-134a".equals(useKey.getSubstance())) {
        return valueToConsumption;
      }
      return null;
    });
    when(engine.getEqualsGhgIntensityFor(any(UseKey.class))).thenAnswer(invocation -> {
      UseKey useKey = invocation.getArgument(0);
      if ("commercialRefrigeration".equals(useKey.getApplication())
          && "HFC-134a".equals(useKey.getSubstance())) {
        return valueToConsumption;
      }
      return null;
    });
    when(engine.getEqualsGhgIntensity()).thenReturn(valueToConsumption);

    // Configure initial charge methods
    when(engine.getRawInitialChargeFor(any(UseKey.class), any(String.class))).thenAnswer(invocation -> {
      UseKey useKey = invocation.getArgument(0);
      String stream = invocation.getArgument(1);

      if ("commercialRefrigeration".equals(useKey.getApplication())
          && "HFC-134a".equals(useKey.getSubstance())) {
        switch (stream) {
          case "import": return initialChargeImport;
          case "manufacture": return initialChargeDomestic;
          default: return null;
        }
      }
      return null;
    });
    when(engine.getInitialCharge("sales")).thenReturn(initialChargeImport);

    // Configure stream methods that ConverterStateGetter might call
    when(engine.getStream("equipment")).thenReturn(priorEquipment);
    when(engine.getStream("consumption")).thenReturn(valueToConsumption);
    when(engine.getStream("energy")).thenReturn(energyIntensity);

    // Configure energy intensity
    when(engine.getEqualsEnergyIntensity()).thenReturn(energyIntensity);

    // Configure scenario name and trial number
    when(engine.getScenarioName()).thenReturn("Test Scenario");
    when(engine.getTrialNumber()).thenReturn(1);

    return engine;
  }

  /**
   * Create a mock engine specifically for import supplement tests.
   *
   * @return Mock engine configured with import supplement test data
   */
  private static Engine createMockEngineForTradeSupplement() {
    Engine engine = mock(Engine.class);

    // Test data for import supplement tests - use the same data as main tests
    EngineNumber manufacture = new EngineNumber(1600, "mt");
    EngineNumber importMt = new EngineNumber(400, "mt");
    EngineNumber exportMt = new EngineNumber(200, "mt");
    EngineNumber recharge = new EngineNumber(1000, "mt");
    EngineNumber valueToConsumption = new EngineNumber(500, "tCO2e / mt");
    EngineNumber initialChargeImport = new EngineNumber(200, "kg / unit");
    EngineNumber initialChargeDomestic = new EngineNumber(150, "kg / unit");
    EngineNumber recycling = new EngineNumber(10, "mt");
    EngineNumber energyIntensity = new EngineNumber(5, "kwh / kg");
    EngineNumber priorEquipment = new EngineNumber(1000, "units");
    EngineNumber eolEmissions = new EngineNumber(100, "tCO2e");


    // Configure mock responses for getStreamRaw with UseKey
    when(engine.getStreamFor(any(UseKey.class), any(String.class))).thenAnswer(invocation -> {
      UseKey useKey = invocation.getArgument(0);
      String stream = invocation.getArgument(1);

      if ("commercialRefrigeration".equals(useKey.getApplication())
          && "HFC-134a".equals(useKey.getSubstance())) {
        switch (stream) {
          case "manufacture": return manufacture;
          case "import": return importMt;
          case "export": return exportMt;
          case "recycle": return recycling;
          case "energy": return energyIntensity;
          case "equipment": return priorEquipment;
          case "newEquipment": return priorEquipment;
          case "rechargeEmissions": return recharge;
          case "eolEmissions": return eolEmissions;
          default: return null;
        }
      }
      return null;
    });

    // Configure GHG intensity methods
    when(engine.getGhgIntensity(any(UseKey.class))).thenAnswer(invocation -> {
      UseKey useKey = invocation.getArgument(0);
      if ("commercialRefrigeration".equals(useKey.getApplication())
          && "HFC-134a".equals(useKey.getSubstance())) {
        return valueToConsumption;
      }
      return null;
    });
    when(engine.getEqualsGhgIntensityFor(any(UseKey.class))).thenAnswer(invocation -> {
      UseKey useKey = invocation.getArgument(0);
      if ("commercialRefrigeration".equals(useKey.getApplication())
          && "HFC-134a".equals(useKey.getSubstance())) {
        return valueToConsumption;
      }
      return null;
    });
    when(engine.getEqualsGhgIntensity()).thenReturn(valueToConsumption);

    // Configure initial charge methods
    when(engine.getRawInitialChargeFor(any(UseKey.class), any(String.class))).thenAnswer(invocation -> {
      UseKey useKey = invocation.getArgument(0);
      String stream = invocation.getArgument(1);

      if ("commercialRefrigeration".equals(useKey.getApplication())
          && "HFC-134a".equals(useKey.getSubstance())) {
        switch (stream) {
          case "import": return initialChargeImport;
          case "manufacture": return initialChargeDomestic;
          default: return null;
        }
      }
      return null;
    });
    when(engine.getInitialCharge("sales")).thenReturn(initialChargeImport);

    // Configure stream methods that ConverterStateGetter might call
    when(engine.getStream("equipment")).thenReturn(priorEquipment);
    when(engine.getStream("consumption")).thenReturn(valueToConsumption);
    when(engine.getStream("energy")).thenReturn(energyIntensity);

    // Configure energy intensity
    when(engine.getEqualsEnergyIntensity()).thenReturn(energyIntensity);

    // Configure scenario name and trial number
    when(engine.getScenarioName()).thenReturn("Test Scenario");
    when(engine.getTrialNumber()).thenReturn(1);

    return engine;
  }
}
