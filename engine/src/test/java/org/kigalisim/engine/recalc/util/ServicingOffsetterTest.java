/**
 * Unit tests for the ServicingOffsetter class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.recalc.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.StateGetter;

/**
 * Tests for the ServicingOffsetter class.
 */
public class ServicingOffsetterTest {

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
    ServicingOffsetter offsetter = new ServicingOffsetter();
    BigDecimal salesKg = new BigDecimal("100");
    BigDecimal rechargeKg = new BigDecimal("10");
    BigDecimal initialChargeKgUnit = new BigDecimal("1");
    EngineNumber prechargePopRaw = new EngineNumber(new BigDecimal("50"), "%");
    EngineNumber prechargeIntensityRaw = new EngineNumber(new BigDecimal("2"), "kg / unit");

    ServicingOffset offset = offsetter.offset(salesKg, rechargeKg, initialChargeKgUnit,
        prechargePopRaw, prechargeIntensityRaw, true, BigDecimal.ZERO, null);

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
    ServicingOffsetter offsetter = new ServicingOffsetter();
    BigDecimal salesKg = new BigDecimal("200");
    BigDecimal rechargeKg = new BigDecimal("20");
    BigDecimal initialChargeKgUnit = new BigDecimal("2");
    EngineNumber prechargePopRaw = new EngineNumber(new BigDecimal("25"), "%");
    EngineNumber prechargeIntensityRaw = new EngineNumber(new BigDecimal("4"), "kg / unit");

    ServicingOffset offset = offsetter.offset(salesKg, rechargeKg, initialChargeKgUnit,
        prechargePopRaw, prechargeIntensityRaw, true, BigDecimal.ZERO, null);

    assertBigDecimalEquals(new BigDecimal("60"), offset.getDeltaUnits(),
        "Delta units should be 60 in circular case");
    assertBigDecimalEquals(new BigDecimal("60"), offset.getPrechargeKg(),
        "Precharge kg should be 60 in circular case");
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
    ServicingOffsetter offsetter = new ServicingOffsetter();
    BigDecimal salesKg = new BigDecimal("100");
    BigDecimal rechargeKg = new BigDecimal("10");
    BigDecimal initialChargeKgUnit = new BigDecimal("1");
    EngineNumber prechargePopRaw = new EngineNumber(new BigDecimal("10"), "units");
    EngineNumber prechargeIntensityRaw = new EngineNumber(new BigDecimal("2"), "kg / unit");
    StateGetter stateGetter = createMockStateGetter();

    ServicingOffset offset = offsetter.offset(salesKg, rechargeKg, initialChargeKgUnit,
        prechargePopRaw, prechargeIntensityRaw, true, BigDecimal.ZERO, stateGetter);

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
    ServicingOffsetter offsetter = new ServicingOffsetter();
    BigDecimal salesKg = new BigDecimal("100");
    BigDecimal rechargeKg = new BigDecimal("10");
    BigDecimal initialChargeKgUnit = new BigDecimal("1");
    BigDecimal implicitPrechargeKg = new BigDecimal("5");

    ServicingOffset offset = offsetter.offset(salesKg, rechargeKg, initialChargeKgUnit,
        null, null, false, implicitPrechargeKg, null);

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
    ServicingOffsetter offsetter = new ServicingOffsetter();
    BigDecimal salesKg = new BigDecimal("100");
    BigDecimal rechargeKg = new BigDecimal("10");
    BigDecimal initialChargeKgUnit = new BigDecimal("1");
    EngineNumber prechargePopRaw = new EngineNumber(new BigDecimal("50"), "%");
    EngineNumber prechargeIntensityRaw = new EngineNumber(new BigDecimal("2"), "kg / unit");
    BigDecimal implicitPrechargeKg = new BigDecimal("5");

    ServicingOffset offset = offsetter.offset(salesKg, rechargeKg, initialChargeKgUnit,
        prechargePopRaw, prechargeIntensityRaw, false, implicitPrechargeKg, null);

    assertBigDecimalEquals(new BigDecimal("85"), offset.getDeltaUnits(),
        "Delta units should be 85 when percent precharge falls to unit sales");
    assertBigDecimalEquals(new BigDecimal("5"), offset.getPrechargeKg(),
        "Precharge kg should be 5 (implicit) when falling to unit sales");
  }

  /**
   * Test that zero precharge with explicit tracking falls to unit sales.
   *
   * <p>When the precharge population is zero, hasPrecharge is false so it falls through to
   * offsetUnitSales even with useExplicitRechargeEffective true.</p>
   */
  @Test
  public void testZeroPrechargeFallsToUnitSales() {
    ServicingOffsetter offsetter = new ServicingOffsetter();
    BigDecimal salesKg = new BigDecimal("100");
    BigDecimal rechargeKg = new BigDecimal("10");
    BigDecimal initialChargeKgUnit = new BigDecimal("1");
    EngineNumber prechargePopRaw = new EngineNumber(BigDecimal.ZERO, "%");
    BigDecimal implicitPrechargeKg = new BigDecimal("5");

    ServicingOffset offset = offsetter.offset(salesKg, rechargeKg, initialChargeKgUnit,
        prechargePopRaw, null, true, implicitPrechargeKg, null);

    assertBigDecimalEquals(new BigDecimal("85"), offset.getDeltaUnits(),
        "Delta units should be 85 when precharge is zero");
    assertBigDecimalEquals(new BigDecimal("5"), offset.getPrechargeKg(),
        "Precharge kg should be 5 (implicit) when precharge is zero");
  }

  /**
   * Test that null precharge falls to unit sales.
   *
   * <p>When the precharge population is null, hasPrecharge is false so it falls through to
   * offsetUnitSales.</p>
   */
  @Test
  public void testNullPrechargeFallsToUnitSales() {
    ServicingOffsetter offsetter = new ServicingOffsetter();
    BigDecimal salesKg = new BigDecimal("100");
    BigDecimal rechargeKg = new BigDecimal("10");
    BigDecimal initialChargeKgUnit = new BigDecimal("1");
    BigDecimal implicitPrechargeKg = new BigDecimal("5");

    ServicingOffset offset = offsetter.offset(salesKg, rechargeKg, initialChargeKgUnit,
        null, null, true, implicitPrechargeKg, null);

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
    ServicingOffsetter offsetter = new ServicingOffsetter();
    BigDecimal salesKg = BigDecimal.ZERO;
    BigDecimal rechargeKg = BigDecimal.ZERO;
    BigDecimal initialChargeKgUnit = new BigDecimal("1");
    BigDecimal implicitPrechargeKg = BigDecimal.ZERO;

    ServicingOffset offset = offsetter.offset(salesKg, rechargeKg, initialChargeKgUnit,
        null, null, false, implicitPrechargeKg, null);

    assertBigDecimalEquals(BigDecimal.ZERO, offset.getDeltaUnits(),
        "Delta units should be zero with no sales");
    assertBigDecimalEquals(BigDecimal.ZERO, offset.getPrechargeKg(),
        "Precharge kg should be zero with no implicit precharge");
  }
}
