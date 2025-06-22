/**
 * Unit tests for the RechargeOperation class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.SingleThreadEngine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.YearMatcher;
import org.kigalisim.lang.machine.PushDownMachine;
import org.kigalisim.lang.machine.SingleThreadPushDownMachine;
import org.kigalisim.lang.time.CalculatedTimePointFuture;
import org.kigalisim.lang.time.ParsedDuring;
import org.kigalisim.lang.time.TimePointFuture;

/**
 * Tests for the RechargeOperation class.
 */
public class RechargeOperationTest {

  private Engine engine;
  private PushDownMachine machine;
  private YearMatcher allYearsMatcher;

  /**
   * Set up a new engine and machine before each test.
   */
  @BeforeEach
  public void setUp() {
    engine = new SingleThreadEngine(2020, 2025);
    machine = new SingleThreadPushDownMachine(engine);

    // Set up the engine with a scope
    engine.setStanza("default");
    engine.setApplication("test app");
    engine.setSubstance("test substance");

    // Create a matcher that matches all years
    allYearsMatcher = YearMatcher.unbounded();
  }

  /**
   * Test that RechargeOperation can be initialized with no during.
   */
  @Test
  public void testInitializesNoDuring() {
    EngineNumber volume = new EngineNumber(BigDecimal.valueOf(100), "kg");
    EngineNumber intensity = new EngineNumber(BigDecimal.valueOf(0.1), "");
    Operation volumeOperation = new PreCalculatedOperation(volume);
    Operation intensityOperation = new PreCalculatedOperation(intensity);
    RechargeOperation operation = new RechargeOperation(volumeOperation, intensityOperation);
    assertNotNull(operation, "RechargeOperation should be constructable without during");
  }

  /**
   * Test that RechargeOperation can be initialized with a during.
   */
  @Test
  public void testInitializesWithDuring() {
    EngineNumber volume = new EngineNumber(BigDecimal.valueOf(100), "kg");
    EngineNumber intensity = new EngineNumber(BigDecimal.valueOf(0.1), "");
    Operation volumeOperation = new PreCalculatedOperation(volume);
    Operation intensityOperation = new PreCalculatedOperation(intensity);
    ParsedDuring during = new ParsedDuring(Optional.empty(), Optional.empty());
    RechargeOperation operation = new RechargeOperation(volumeOperation, intensityOperation, during);
    assertNotNull(operation, "RechargeOperation should be constructable with during");
  }

  /**
   * Test the execute method with no during.
   */
  @Test
  public void testExecuteNoDuring() {
    // Set up prior equipment population
    EngineNumber priorEquipment = new EngineNumber(BigDecimal.valueOf(1000), "units");
    engine.setStream("priorEquipment", priorEquipment, allYearsMatcher);

    // Set up recharge values
    EngineNumber volume = new EngineNumber(BigDecimal.valueOf(10), "%");
    EngineNumber intensity = new EngineNumber(BigDecimal.valueOf(5), "kg / unit");
    Operation volumeOperation = new PreCalculatedOperation(volume);
    Operation intensityOperation = new PreCalculatedOperation(intensity);
    RechargeOperation operation = new RechargeOperation(volumeOperation, intensityOperation);

    operation.execute(machine);

    // Verify the recharge values were set in the engine
    EngineNumber resultVolume = engine.getRechargeVolume();
    EngineNumber resultIntensity = engine.getRechargeIntensity();
    assertEquals(BigDecimal.valueOf(10), resultVolume.getValue(), "Recharge volume should be set correctly");
    assertEquals("%", resultVolume.getUnits(), "Recharge volume units should be set correctly");
    assertEquals(BigDecimal.valueOf(5), resultIntensity.getValue(), "Recharge intensity should be set correctly");
    assertEquals("kg / unit", resultIntensity.getUnits(), "Recharge intensity units should be set correctly");
  }

  /**
   * Test the execute method with a during.
   */
  @Test
  public void testExecuteWithDuring() {
    // Set up prior equipment population
    EngineNumber priorEquipment = new EngineNumber(BigDecimal.valueOf(1000), "units");
    engine.setStream("priorEquipment", priorEquipment, allYearsMatcher);

    // Set up recharge values with a during that applies to the current year (2020)
    EngineNumber volume = new EngineNumber(BigDecimal.valueOf(10), "%");
    EngineNumber intensity = new EngineNumber(BigDecimal.valueOf(5), "kg / unit");
    Operation volumeOperation = new PreCalculatedOperation(volume);
    Operation intensityOperation = new PreCalculatedOperation(intensity);

    EngineNumber yearNumber = new EngineNumber(BigDecimal.valueOf(2020), "");
    Operation yearOperation = new PreCalculatedOperation(yearNumber);
    TimePointFuture start = new CalculatedTimePointFuture(yearOperation);
    ParsedDuring during = new ParsedDuring(Optional.of(start), Optional.empty());

    RechargeOperation operation = new RechargeOperation(volumeOperation, intensityOperation, during);

    operation.execute(machine);

    // Verify the recharge values were set in the engine
    EngineNumber resultVolume = engine.getRechargeVolume();
    EngineNumber resultIntensity = engine.getRechargeIntensity();
    assertEquals(BigDecimal.valueOf(10), resultVolume.getValue(), "Recharge volume should be set correctly");
    assertEquals("%", resultVolume.getUnits(), "Recharge volume units should be set correctly");
    assertEquals(BigDecimal.valueOf(5), resultIntensity.getValue(), "Recharge intensity should be set correctly");
    assertEquals("kg / unit", resultIntensity.getUnits(), "Recharge intensity units should be set correctly");
  }

  /**
   * Test the execute method with a during that doesn't apply.
   */
  @Test
  public void testExecuteWithDuringNotApplying() {
    // Set up prior equipment population
    EngineNumber priorEquipment = new EngineNumber(BigDecimal.valueOf(1000), "units");
    engine.setStream("priorEquipment", priorEquipment, allYearsMatcher);

    // Set up initial recharge values
    EngineNumber initialVolume = new EngineNumber(BigDecimal.valueOf(5), "%");
    EngineNumber initialIntensity = new EngineNumber(BigDecimal.valueOf(3), "kg / unit");
    engine.recharge(initialVolume, initialIntensity, allYearsMatcher);

    // Set up recharge values with a during that applies to a future year (2021)
    EngineNumber volume = new EngineNumber(BigDecimal.valueOf(10), "%");
    EngineNumber intensity = new EngineNumber(BigDecimal.valueOf(5), "kg / unit");
    Operation volumeOperation = new PreCalculatedOperation(volume);
    Operation intensityOperation = new PreCalculatedOperation(intensity);

    EngineNumber yearNumber = new EngineNumber(BigDecimal.valueOf(2021), "");
    Operation yearOperation = new PreCalculatedOperation(yearNumber);
    TimePointFuture start = new CalculatedTimePointFuture(yearOperation);
    ParsedDuring during = new ParsedDuring(Optional.of(start), Optional.empty());

    RechargeOperation operation = new RechargeOperation(volumeOperation, intensityOperation, during);

    operation.execute(machine);

    // Verify the recharge values were not changed in the engine
    EngineNumber resultVolume = engine.getRechargeVolume();
    EngineNumber resultIntensity = engine.getRechargeIntensity();
    assertEquals(BigDecimal.valueOf(5), resultVolume.getValue(), 
        "Recharge volume should not be changed when during doesn't apply");
    assertEquals("%", resultVolume.getUnits(), "Recharge volume units should remain the same");
    assertEquals(BigDecimal.valueOf(3), resultIntensity.getValue(), 
        "Recharge intensity should not be changed when during doesn't apply");
    assertEquals("kg / unit", resultIntensity.getUnits(), "Recharge intensity units should remain the same");
  }

  /**
   * Test the execute method with complex value operations.
   */
  @Test
  public void testExecuteWithComplexValueOperations() {
    // Set up prior equipment population
    EngineNumber priorEquipment = new EngineNumber(BigDecimal.valueOf(1000), "units");
    engine.setStream("priorEquipment", priorEquipment, allYearsMatcher);

    // Set up recharge values with complex operations
    Operation volumeLeft = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(6), "%"));
    Operation volumeRight = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(4), "%"));
    Operation volumeOperation = new AdditionOperation(volumeLeft, volumeRight); // 6% + 4% = 10%

    Operation intensityLeft = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(3), "kg / unit"));
    Operation intensityRight = new PreCalculatedOperation(new EngineNumber(BigDecimal.valueOf(2), "kg / unit"));
    Operation intensityOperation = new AdditionOperation(intensityLeft, intensityRight); // 3 + 2 = 5 kg/unit

    RechargeOperation operation = new RechargeOperation(volumeOperation, intensityOperation);

    operation.execute(machine);

    // Verify the recharge values were set in the engine
    EngineNumber resultVolume = engine.getRechargeVolume();
    EngineNumber resultIntensity = engine.getRechargeIntensity();
    assertEquals(BigDecimal.valueOf(10), resultVolume.getValue(), 
        "Recharge volume should be calculated and set correctly");
    assertEquals("%", resultVolume.getUnits(), "Recharge volume units should be set correctly");
    assertEquals(BigDecimal.valueOf(5), resultIntensity.getValue(), 
        "Recharge intensity should be calculated and set correctly");
    assertEquals("kg / unit", resultIntensity.getUnits(), "Recharge intensity units should be set correctly");
  }
}
