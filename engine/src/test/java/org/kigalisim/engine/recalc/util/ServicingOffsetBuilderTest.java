/**
 * Unit tests for the ServicingOffsetBuilder class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.StateGetter;

/**
 * Tests for the ServicingOffsetBuilder class.
 */
public class ServicingOffsetBuilderTest {

  /**
   * Helper method to create a mock StateGetter with lenient stubbing.
   *
   * @return Mock StateGetter with default values for unit conversions
   */
  private StateGetter createMockStateGetter() {
    StateGetter stateGetter = mock(StateGetter.class);
    lenient().when(stateGetter.getSubstanceConsumption())
        .thenReturn(new EngineNumber(BigDecimal.ONE, "kgCO2e / kg"));
    lenient().when(stateGetter.getEnergyIntensity())
        .thenReturn(new EngineNumber(BigDecimal.ONE, "kwh / kg"));
    lenient().when(stateGetter.getAmortizedUnitVolume())
        .thenReturn(new EngineNumber(BigDecimal.ONE, "kg / unit"));
    lenient().when(stateGetter.getPopulation())
        .thenReturn(new EngineNumber(new BigDecimal("100"), "units"));
    lenient().when(stateGetter.getYearsElapsed())
        .thenReturn(new EngineNumber(BigDecimal.ONE, "years"));
    lenient().when(stateGetter.getGhgConsumption())
        .thenReturn(new EngineNumber(new BigDecimal("50"), "tCO2e"));
    lenient().when(stateGetter.getVolume())
        .thenReturn(new EngineNumber(new BigDecimal("200"), "kg"));
    lenient().when(stateGetter.getAmortizedUnitConsumption())
        .thenReturn(new EngineNumber(new BigDecimal("0.5"), "tCO2e / unit"));
    lenient().when(stateGetter.getEnergyConsumption())
        .thenReturn(new EngineNumber(new BigDecimal("100"), "kwh"));
    lenient().when(stateGetter.getPriorVolume())
        .thenReturn(new EngineNumber(new BigDecimal("200"), "kg"));
    lenient().when(stateGetter.getPriorPopulation())
        .thenReturn(new EngineNumber(new BigDecimal("100"), "units"));
    lenient().when(stateGetter.getPriorGhgConsumption())
        .thenReturn(new EngineNumber(new BigDecimal("50"), "tCO2e"));
    lenient().when(stateGetter.getPriorYearsElapsed())
        .thenReturn(new EngineNumber(BigDecimal.ONE, "years"));
    return stateGetter;
  }

  /**
   * Assert that two BigDecimal values are equal using compareTo (ignoring scale).
   *
   * @param expected The expected value
   * @param actual The actual value
   * @param message The failure message
   */
  private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual, String message) {
    assertEquals(0, expected.compareTo(actual), message);
  }

  /**
   * Test the circular case with percentage-based precharge and volume-based sales.
   *
   * <p>With 50% precharge, 2 kg/unit intensity, 100 kg sales, 10 kg recharge, and 1 kg/unit
   * initial charge: prechargeRatio = 0.5, denominator = 1 + 0.5 * 2 = 2, deltaUnits = 90 / 2 = 45,
   * prechargeKg = 45 * 0.5 * 2 = 45.</p>
   */
  @Test
  public void testOffsetVolumeSalesPercentPrecharge() {
    BigDecimal salesKg = new BigDecimal("100");
    BigDecimal rechargeKg = new BigDecimal("10");
    BigDecimal initialChargeKgUnit = new BigDecimal("1");
    EngineNumber prechargePopRaw = new EngineNumber(new BigDecimal("50"), "%");
    EngineNumber prechargeIntensityRaw = new EngineNumber(new BigDecimal("2"), "kg / unit");

    ServicingOffset offset = new ServicingOffsetBuilder()
        .setSalesKg(salesKg)
        .setRechargeKg(rechargeKg)
        .setInitialChargeKgUnit(initialChargeKgUnit)
        .setPrechargePopRaw(prechargePopRaw)
        .setPrechargeIntensityRaw(prechargeIntensityRaw)
        .setUseExplicitRechargeEffective(true)
        .setImplicitPrechargeKg(BigDecimal.ZERO)
        .build();

    assertBigDecimalEquals(new BigDecimal("45"), offset.getDeltaUnits(),
        "Delta units should be 45 in circular case");
    assertBigDecimalEquals(new BigDecimal("45"), offset.getPrechargeKg(),
        "Precharge kg should be 45 in circular case");
    assertBigDecimalEquals(new BigDecimal("10"), offset.getRechargeKg(),
        "Recharge kg should be returned as 10");
  }

  /**
   * Test the circular case with different values to verify the analytical solution.
   *
   * <p>With 25% precharge, 4 kg/unit intensity, 200 kg sales, 20 kg recharge, and 2 kg/unit
   * initial charge: prechargeRatio = 0.25, denominator = 2 + 0.25 * 4 = 3, deltaUnits = 180 / 3 =
   * 60, prechargeKg = 60 * 0.25 * 4 = 60.</p>
   */
  @Test
  public void testOffsetVolumeSalesPercentPrechargeDifferentValues() {
    BigDecimal salesKg = new BigDecimal("200");
    BigDecimal rechargeKg = new BigDecimal("20");
    BigDecimal initialChargeKgUnit = new BigDecimal("2");
    EngineNumber prechargePopRaw = new EngineNumber(new BigDecimal("25"), "%");
    EngineNumber prechargeIntensityRaw = new EngineNumber(new BigDecimal("4"), "kg / unit");

    ServicingOffset offset = new ServicingOffsetBuilder()
        .setSalesKg(salesKg)
        .setRechargeKg(rechargeKg)
        .setInitialChargeKgUnit(initialChargeKgUnit)
        .setPrechargePopRaw(prechargePopRaw)
        .setPrechargeIntensityRaw(prechargeIntensityRaw)
        .setUseExplicitRechargeEffective(true)
        .setImplicitPrechargeKg(BigDecimal.ZERO)
        .build();

    assertBigDecimalEquals(new BigDecimal("60"), offset.getDeltaUnits(),
        "Delta units should be 60 in circular case");
    assertBigDecimalEquals(new BigDecimal("60"), offset.getPrechargeKg(),
        "Precharge kg should be 60 in circular case");
  }

  /**
   * Test the circular case with 100% precharge population.
   *
   * <p>With 100% precharge, 2 kg/unit intensity, 100 kg sales, 10 kg recharge, and 1 kg/unit
   * initial charge: prechargeRatio = 1.0, denominator = 1 + 1.0 * 2 = 3, deltaUnits = 90 / 3 =
   * 30, prechargeKg = 30 * 1.0 * 2 = 60.</p>
   */
  @Test
  public void testOffsetVolumeSalesFullPrecharge() {
    BigDecimal salesKg = new BigDecimal("100");
    BigDecimal rechargeKg = new BigDecimal("10");
    BigDecimal initialChargeKgUnit = new BigDecimal("1");
    EngineNumber prechargePopRaw = new EngineNumber(new BigDecimal("100"), "%");
    EngineNumber prechargeIntensityRaw = new EngineNumber(new BigDecimal("2"), "kg / unit");

    ServicingOffset offset = new ServicingOffsetBuilder()
        .setSalesKg(salesKg)
        .setRechargeKg(rechargeKg)
        .setInitialChargeKgUnit(initialChargeKgUnit)
        .setPrechargePopRaw(prechargePopRaw)
        .setPrechargeIntensityRaw(prechargeIntensityRaw)
        .setUseExplicitRechargeEffective(true)
        .setImplicitPrechargeKg(BigDecimal.ZERO)
        .build();

    assertBigDecimalEquals(new BigDecimal("30"), offset.getDeltaUnits(),
        "Delta units should be 30 when precharge population is 100%");
    assertBigDecimalEquals(new BigDecimal("60"), offset.getPrechargeKg(),
        "Precharge kg should be 60 when precharge population is 100%");
  }

  /**
   * Test explicit precharge with absolute (units) precharge population and volume-based sales.
   *
   * <p>With 10 units precharge population, 2 kg/unit intensity, 100 kg sales, 10 kg recharge, and
   * 1 kg/unit initial charge: prechargeVolume = 2 * 10 = 20 kg, prechargeKg = 20,
   * availableForNewUnitsKg = 100 - 10 - 20 = 70, deltaUnits = 70 / 1 = 70.</p>
   */
  @Test
  public void testOffsetVolumeSalesExplicitPrecharge() {
    BigDecimal salesKg = new BigDecimal("100");
    BigDecimal rechargeKg = new BigDecimal("10");
    BigDecimal initialChargeKgUnit = new BigDecimal("1");
    EngineNumber prechargePopRaw = new EngineNumber(new BigDecimal("10"), "units");
    EngineNumber prechargeIntensityRaw = new EngineNumber(new BigDecimal("2"), "kg / unit");
    StateGetter stateGetter = createMockStateGetter();

    ServicingOffset offset = new ServicingOffsetBuilder()
        .setSalesKg(salesKg)
        .setRechargeKg(rechargeKg)
        .setInitialChargeKgUnit(initialChargeKgUnit)
        .setPrechargePopRaw(prechargePopRaw)
        .setPrechargeIntensityRaw(prechargeIntensityRaw)
        .setUseExplicitRechargeEffective(true)
        .setImplicitPrechargeKg(BigDecimal.ZERO)
        .setStateGetter(stateGetter)
        .build();

    assertBigDecimalEquals(new BigDecimal("70"), offset.getDeltaUnits(),
        "Delta units should be 70 in explicit precharge case");
    assertBigDecimalEquals(new BigDecimal("20"), offset.getPrechargeKg(),
        "Precharge kg should be 20 in explicit precharge case");
    assertBigDecimalEquals(new BigDecimal("10"), offset.getRechargeKg(),
        "Recharge kg should be returned as 10");
  }

  /**
   * Test units-based tracking with implicit precharge.
   *
   * <p>With 100 kg sales, 10 kg recharge, 1 kg/unit initial charge, and 5 kg implicit precharge:
   * prechargeKg = 5, availableForNewUnitsKg = 100 - 10 - 5 = 85, deltaUnits = 85 / 1 = 85.</p>
   */
  @Test
  public void testOffsetUnitSales() {
    BigDecimal salesKg = new BigDecimal("100");
    BigDecimal rechargeKg = new BigDecimal("10");
    BigDecimal initialChargeKgUnit = new BigDecimal("1");
    BigDecimal implicitPrechargeKg = new BigDecimal("5");

    ServicingOffset offset = new ServicingOffsetBuilder()
        .setSalesKg(salesKg)
        .setRechargeKg(rechargeKg)
        .setInitialChargeKgUnit(initialChargeKgUnit)
        .setUseExplicitRechargeEffective(false)
        .setImplicitPrechargeKg(implicitPrechargeKg)
        .build();

    assertBigDecimalEquals(new BigDecimal("85"), offset.getDeltaUnits(),
        "Delta units should be 85 in units-based tracking");
    assertBigDecimalEquals(new BigDecimal("5"), offset.getPrechargeKg(),
        "Precharge kg should be 5 (implicit) in units-based tracking");
    assertBigDecimalEquals(new BigDecimal("10"), offset.getRechargeKg(),
        "Recharge kg should be returned as 10");
  }

  /**
   * Test that percent precharge with implicit (non-explicit) tracking falls to unit sales.
   *
   * <p>When useExplicitRechargeEffective is false, the circular case is not triggered even with
   * percent precharge, so it falls through to offsetUnitSales.</p>
   */
  @Test
  public void testPercentPrechargeWithImplicitFallsToUnitSales() {
    BigDecimal salesKg = new BigDecimal("100");
    BigDecimal rechargeKg = new BigDecimal("10");
    BigDecimal initialChargeKgUnit = new BigDecimal("1");
    EngineNumber prechargePopRaw = new EngineNumber(new BigDecimal("50"), "%");
    EngineNumber prechargeIntensityRaw = new EngineNumber(new BigDecimal("2"), "kg / unit");
    BigDecimal implicitPrechargeKg = new BigDecimal("5");

    ServicingOffset offset = new ServicingOffsetBuilder()
        .setSalesKg(salesKg)
        .setRechargeKg(rechargeKg)
        .setInitialChargeKgUnit(initialChargeKgUnit)
        .setPrechargePopRaw(prechargePopRaw)
        .setPrechargeIntensityRaw(prechargeIntensityRaw)
        .setUseExplicitRechargeEffective(false)
        .setImplicitPrechargeKg(implicitPrechargeKg)
        .build();

    assertBigDecimalEquals(new BigDecimal("85"), offset.getDeltaUnits(),
        "Delta units should be 85 when percent precharge falls to unit sales");
    assertBigDecimalEquals(new BigDecimal("5"), offset.getPrechargeKg(),
        "Precharge kg should be 5 (implicit) when falling to unit sales");
  }

  /**
   * Test that zero precharge with explicit tracking falls to unit sales.
   *
   * <p>When the precharge population is zero, isServicingEnabled is false so it falls through to
   * offsetUnitSales even with useExplicitRechargeEffective true.</p>
   */
  @Test
  public void testZeroPrechargeFallsToUnitSales() {
    BigDecimal salesKg = new BigDecimal("100");
    BigDecimal rechargeKg = new BigDecimal("10");
    BigDecimal initialChargeKgUnit = new BigDecimal("1");
    EngineNumber prechargePopRaw = new EngineNumber(BigDecimal.ZERO, "%");
    BigDecimal implicitPrechargeKg = new BigDecimal("5");

    ServicingOffset offset = new ServicingOffsetBuilder()
        .setSalesKg(salesKg)
        .setRechargeKg(rechargeKg)
        .setInitialChargeKgUnit(initialChargeKgUnit)
        .setPrechargePopRaw(prechargePopRaw)
        .setUseExplicitRechargeEffective(true)
        .setImplicitPrechargeKg(implicitPrechargeKg)
        .build();

    assertBigDecimalEquals(new BigDecimal("85"), offset.getDeltaUnits(),
        "Delta units should be 85 when precharge is zero");
    assertBigDecimalEquals(new BigDecimal("5"), offset.getPrechargeKg(),
        "Precharge kg should be 5 (implicit) when precharge is zero");
  }

  /**
   * Test that null precharge falls to unit sales.
   *
   * <p>When the precharge population is null, isServicingEnabled is false so it falls through to
   * offsetUnitSales.</p>
   */
  @Test
  public void testNullPrechargeFallsToUnitSales() {
    BigDecimal salesKg = new BigDecimal("100");
    BigDecimal rechargeKg = new BigDecimal("10");
    BigDecimal initialChargeKgUnit = new BigDecimal("1");
    BigDecimal implicitPrechargeKg = new BigDecimal("5");

    ServicingOffset offset = new ServicingOffsetBuilder()
        .setSalesKg(salesKg)
        .setRechargeKg(rechargeKg)
        .setInitialChargeKgUnit(initialChargeKgUnit)
        .setUseExplicitRechargeEffective(true)
        .setImplicitPrechargeKg(implicitPrechargeKg)
        .build();

    assertBigDecimalEquals(new BigDecimal("85"), offset.getDeltaUnits(),
        "Delta units should be 85 when precharge is null");
    assertBigDecimalEquals(new BigDecimal("5"), offset.getPrechargeKg(),
        "Precharge kg should be 5 (implicit) when precharge is null");
  }

  /**
   * Test that the offset with zero sales produces zero delta units.
   *
   * <p>With no sales and no recharge, the delta units should be zero in all branches.</p>
   */
  @Test
  public void testZeroSalesProducesZeroDelta() {
    BigDecimal salesKg = BigDecimal.ZERO;
    BigDecimal rechargeKg = BigDecimal.ZERO;
    BigDecimal initialChargeKgUnit = new BigDecimal("1");
    BigDecimal implicitPrechargeKg = BigDecimal.ZERO;

    ServicingOffset offset = new ServicingOffsetBuilder()
        .setSalesKg(salesKg)
        .setRechargeKg(rechargeKg)
        .setInitialChargeKgUnit(initialChargeKgUnit)
        .setUseExplicitRechargeEffective(false)
        .setImplicitPrechargeKg(implicitPrechargeKg)
        .build();

    assertBigDecimalEquals(BigDecimal.ZERO, offset.getDeltaUnits(),
        "Delta units should be zero with no sales");
    assertBigDecimalEquals(BigDecimal.ZERO, offset.getPrechargeKg(),
        "Precharge kg should be zero with no implicit precharge");
  }

  /**
   * Test that build() fails fast with a clear message when a field required by every branch
   * (such as salesKg) is missing, instead of a NullPointerException surfacing later.
   */
  @Test
  public void testBuildFailsFastWhenAlwaysRequiredFieldMissing() {
    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> new ServicingOffsetBuilder()
            .setRechargeKg(BigDecimal.ZERO)
            .setInitialChargeKgUnit(BigDecimal.ONE)
            .setUseExplicitRechargeEffective(false)
            .setImplicitPrechargeKg(BigDecimal.ZERO)
            .build(),
        "Should throw IllegalStateException when salesKg is not set"
    );
    assertTrue(exception.getMessage().contains("salesKg"),
        "Error message should mention the missing field (salesKg)");
  }

  /**
   * Test that build() fails fast when prechargePopRaw is set but prechargeIntensityRaw is not.
   */
  @Test
  public void testBuildFailsFastWhenPrechargeIntensityMissing() {
    EngineNumber prechargePopRaw = new EngineNumber(new BigDecimal("50"), "%");

    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> new ServicingOffsetBuilder()
            .setSalesKg(new BigDecimal("100"))
            .setRechargeKg(BigDecimal.ZERO)
            .setInitialChargeKgUnit(BigDecimal.ONE)
            .setPrechargePopRaw(prechargePopRaw)
            .setUseExplicitRechargeEffective(true)
            .setImplicitPrechargeKg(BigDecimal.ZERO)
            .build(),
        "Should throw IllegalStateException when prechargeIntensityRaw is not set"
    );
    assertTrue(exception.getMessage().contains("prechargeIntensityRaw"),
        "Error message should mention the missing field (prechargeIntensityRaw)");
  }

  /**
   * Test that build() fails fast when the explicit precharge branch is reached without a
   * stateGetter having been set.
   */
  @Test
  public void testBuildFailsFastWhenStateGetterMissingForExplicitPrecharge() {
    EngineNumber prechargePopRaw = new EngineNumber(new BigDecimal("10"), "units");
    EngineNumber prechargeIntensityRaw = new EngineNumber(new BigDecimal("2"), "kg / unit");

    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> new ServicingOffsetBuilder()
            .setSalesKg(new BigDecimal("100"))
            .setRechargeKg(new BigDecimal("10"))
            .setInitialChargeKgUnit(BigDecimal.ONE)
            .setPrechargePopRaw(prechargePopRaw)
            .setPrechargeIntensityRaw(prechargeIntensityRaw)
            .setUseExplicitRechargeEffective(true)
            .setImplicitPrechargeKg(BigDecimal.ZERO)
            .build(),
        "Should throw IllegalStateException when stateGetter is not set"
    );
    assertTrue(exception.getMessage().contains("stateGetter"),
        "Error message should mention the missing field (stateGetter)");
  }
}
