/**
 * Integration tests for the ConsumptionCalculator class.
 */

package org.kigalisim.engine.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.SingleThreadEngine;
import org.kigalisim.engine.number.EngineNumber;

/**
 * Integration tests for the ConsumptionCalculator class using real SingleThreadEngine.
 */
public class ConsumptionCalculatorIntegrationTest {

  /**
   * Test that execution fails when consumptionRaw is not set.
   */
  @Test
  public void testExecuteFailsWithoutConsumptionRaw() {
    ConsumptionCalculator calculator = new ConsumptionCalculator();
    SingleThreadEngine engine = new SingleThreadEngine(2020, 2030);

    // Configure calculator without consumptionRaw
    calculator.setStreamName("consumption");

    // Execute and verify exception
    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> calculator.execute(engine)
    );
    assertEquals("consumptionRaw must be set", exception.getMessage());
  }

  /**
   * Test that execution fails when streamName is not set.
   */
  @Test
  public void testExecuteFailsWithoutStreamName() {
    ConsumptionCalculator calculator = new ConsumptionCalculator();
    SingleThreadEngine engine = new SingleThreadEngine(2020, 2030);

    // Configure calculator without streamName
    EngineNumber ghgIntensity = new EngineNumber(BigDecimal.valueOf(2.5), "tCO2e/kg");
    calculator.setConsumptionRaw(ghgIntensity);

    // Execute and verify exception
    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> calculator.execute(engine)
    );
    assertEquals("streamName must be set", exception.getMessage());
  }
}
