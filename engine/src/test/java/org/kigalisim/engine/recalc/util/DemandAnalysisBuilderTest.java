/**
 * Unit tests for the DemandAnalysisBuilder class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;

/**
 * Tests for the DemandAnalysisBuilder class.
 */
public class DemandAnalysisBuilderTest {

  /**
   * Test that build() fails fast with a clear message when a required field (such as
   * rechargeVolume) is missing, instead of a NullPointerException surfacing later.
   */
  @Test
  public void testBuildFailsFastWhenRequiredFieldMissing() {
    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> new DemandAnalysisBuilder()
            .setPrechargeVolume(new EngineNumber(BigDecimal.ZERO, "kg"))
            .setVolumeForNew(new EngineNumber(BigDecimal.ZERO, "kg"))
            .setImplicitRechargeKg(BigDecimal.ZERO)
            .setImplicitPrechargeKg(BigDecimal.ZERO)
            .setEolRecycledKg(BigDecimal.ZERO)
            .setRechargeRecycledKg(BigDecimal.ZERO)
            .build(),
        "Should throw IllegalStateException when rechargeVolume is not set"
    );
    assertTrue(exception.getMessage().contains("rechargeVolume"),
        "Error message should mention the missing field (rechargeVolume)");
  }
}
