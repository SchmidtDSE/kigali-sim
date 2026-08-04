package org.kigalisim.engine.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.SingleThreadEngine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.recalc.StreamUpdate;
import org.kigalisim.engine.recalc.StreamUpdateBuilder;

/**
 * Unit tests for NewEquipmentChangeUtil class.
 *
 * <p>Tests changing the newEquipment stream by absolute amounts (units, kg, mt) and
 * by percent, including the zero-clamp behavior for large negative changes.</p>
 */
class NewEquipmentChangeUtilTest {
  private SingleThreadEngine engine;
  private NewEquipmentChangeUtil newEquipmentChangeUtil;

  @BeforeEach
  void setUp() {
    engine = new SingleThreadEngine(2020, 2030);
    engine.setStanza("default");
    engine.setApplication("TestApp");
    engine.setSubstance("HFC-134a");
    engine.enable("domestic", Optional.empty());
    engine.equals(new EngineNumber(new BigDecimal("1430"), "kgCO2e / kg"), null);
    engine.setInitialCharge(new EngineNumber(BigDecimal.ONE, "kg / unit"), "domestic", null);

    newEquipmentChangeUtil = new NewEquipmentChangeUtil(engine);
  }

  private void setStreamValue(String stream, BigDecimal value, String units) {
    StreamUpdate update = new StreamUpdateBuilder()
        .setName(stream)
        .setValue(new EngineNumber(value, units))
        .build();
    engine.executeStreamUpdate(update);
  }

  @Test
  void testHandleChangeAbsoluteUnits() {
    // Arrange
    setStreamValue("domestic", new BigDecimal("100"), "kg");
    setStreamValue("newEquipment", new BigDecimal("100"), "units");
    EngineNumber changeAmount = new EngineNumber(new BigDecimal("10"), "units");

    // Act
    newEquipmentChangeUtil.handleChange(changeAmount);

    // Assert - sales should increase by 10 units worth of kg (1 kg/unit)
    EngineNumber salesResult = engine.getStream("sales");
    assertEquals(0, new BigDecimal("110").compareTo(salesResult.getValue()),
        "Expected sales to be 110 (100 + 10) but got " + salesResult.getValue());
  }

  @Test
  void testHandleChangeAbsoluteKg() {
    // Arrange
    setStreamValue("domestic", new BigDecimal("100"), "kg");
    setStreamValue("newEquipment", new BigDecimal("100"), "units");
    EngineNumber changeAmount = new EngineNumber(new BigDecimal("20"), "kg");

    // Act - 20kg at 1 kg/unit initial charge is a 20-unit delta in sales
    newEquipmentChangeUtil.handleChange(changeAmount);

    // Assert
    EngineNumber salesResult = engine.getStream("sales");
    assertEquals(0, new BigDecimal("120").compareTo(salesResult.getValue()),
        "Expected sales to be 120 (100 + 20) but got " + salesResult.getValue());
  }

  @Test
  void testHandleChangeAbsoluteMt() {
    // Arrange
    setStreamValue("domestic", new BigDecimal("1000"), "kg");
    setStreamValue("newEquipment", new BigDecimal("1000"), "units");
    EngineNumber changeAmount = new EngineNumber(new BigDecimal("1"), "mt");

    // Act - 1mt = 1000kg = 1000 units delta at 1 kg/unit
    newEquipmentChangeUtil.handleChange(changeAmount);

    // Assert
    EngineNumber salesResult = engine.getStream("sales");
    assertEquals(0, new BigDecimal("2000").compareTo(salesResult.getValue()),
        "Expected sales to be 2000 (1000 + 1000) but got " + salesResult.getValue());
  }

  @Test
  void testHandleChangePercent() {
    // Arrange
    setStreamValue("domestic", new BigDecimal("100"), "kg");
    setStreamValue("newEquipment", new BigDecimal("100"), "units");
    EngineNumber changeAmount = new EngineNumber(new BigDecimal("10"), "%");

    // Act - 10% of 100 units newEquipment is a 10-unit delta in sales
    newEquipmentChangeUtil.handleChange(changeAmount);

    // Assert
    EngineNumber salesResult = engine.getStream("sales");
    assertEquals(0, new BigDecimal("110").compareTo(salesResult.getValue()),
        "Expected sales to be 110 (100 + 10) but got " + salesResult.getValue());
  }

  @Test
  void testHandleChangeLargeNegativePercentClampsAtZero() {
    // Arrange
    setStreamValue("domestic", new BigDecimal("100"), "kg");
    setStreamValue("newEquipment", new BigDecimal("100"), "units");
    EngineNumber changeAmount = new EngineNumber(new BigDecimal("-200"), "%");

    // Act - a -200% change would drive newEquipment to -100, so it should clamp to a
    // delta of exactly -100 (landing newEquipment at 0), not -200.
    newEquipmentChangeUtil.handleChange(changeAmount);

    // Assert - sales should decrease by only 100 (clamped), not 200
    EngineNumber salesResult = engine.getStream("sales");
    assertEquals(0, BigDecimal.ZERO.compareTo(salesResult.getValue()),
        "Expected sales to be 0 (100 - 100 clamped) but got " + salesResult.getValue());
  }

  @Test
  void testHandleChangeLargeNegativeAbsoluteClampsAtZero() {
    // Arrange
    setStreamValue("domestic", new BigDecimal("100"), "kg");
    setStreamValue("newEquipment", new BigDecimal("100"), "units");
    EngineNumber changeAmount = new EngineNumber(new BigDecimal("-500"), "units");

    // Act - a -500 unit change would drive newEquipment to -400, so it should clamp to a
    // delta of exactly -100 (landing newEquipment at 0), not -500.
    newEquipmentChangeUtil.handleChange(changeAmount);

    // Assert
    EngineNumber salesResult = engine.getStream("sales");
    assertEquals(0, BigDecimal.ZERO.compareTo(salesResult.getValue()),
        "Expected sales to be 0 (100 - 100 clamped) but got " + salesResult.getValue());
  }

  @Test
  void testHandleSetAbsoluteUnits() {
    // Act - set newEquipment to an absolute unit target; no prior state needed since set is
    // absolute, not delta.
    newEquipmentChangeUtil.handleSet(new EngineNumber(new BigDecimal("150"), "units"));

    // Assert - unit-path sets sales directly to 150 units, i.e. 150 kg at 1 kg/unit with zero
    // recharge/precharge configured in this fixture.
    EngineNumber salesResult = engine.getStream("sales");
    assertEquals(0, new BigDecimal("150").compareTo(salesResult.getValue()),
        "Expected sales to be 150 but got " + salesResult.getValue());
  }

  @Test
  void testHandleSetAbsoluteKg() {
    // Act - mass-path target with zero recharge/precharge to add on top.
    newEquipmentChangeUtil.handleSet(new EngineNumber(new BigDecimal("300"), "kg"));

    // Assert
    EngineNumber salesResult = engine.getStream("sales");
    assertEquals(0, new BigDecimal("300").compareTo(salesResult.getValue()),
        "Expected sales to be 300 but got " + salesResult.getValue());
  }

  @Test
  void testHandleSetAbsoluteMt() {
    // Act - 1 mt = 1000 kg, mass-path, zero recharge/precharge to add.
    newEquipmentChangeUtil.handleSet(new EngineNumber(BigDecimal.ONE, "mt"));

    // Assert
    EngineNumber salesResult = engine.getStream("sales");
    assertEquals(0, new BigDecimal("1000").compareTo(salesResult.getValue()),
        "Expected sales to be 1000 but got " + salesResult.getValue());
  }

  @Test
  void testHandleSetPercent() {
    // Arrange - newEquipment's native units are "units", so a bare percent resolves against
    // this value and lands in the unit-path.
    setStreamValue("newEquipment", new BigDecimal("200"), "units");

    // Act
    newEquipmentChangeUtil.handleSet(new EngineNumber(new BigDecimal("50"), "%"));

    // Assert - target resolves to 100 units (50% of 200), set directly as sales.
    EngineNumber salesResult = engine.getStream("sales");
    assertEquals(0, new BigDecimal("100").compareTo(salesResult.getValue()),
        "Expected sales to be 100 but got " + salesResult.getValue());
  }

  @Test
  void testHandleSetNegativeUnitsClampsAtZero() {
    // Act - a negative unit target should clamp to zero before the unit-path set, not be
    // left negative.
    newEquipmentChangeUtil.handleSet(new EngineNumber(new BigDecimal("-50"), "units"));

    // Assert
    EngineNumber salesResult = engine.getStream("sales");
    assertEquals(0, BigDecimal.ZERO.compareTo(salesResult.getValue()),
        "Expected sales to be 0 (clamped) but got " + salesResult.getValue());
  }

  @Test
  void testHandleSetNegativeMassClampsAtZero() {
    // Act - a negative mass target should clamp to zero before the mass-path set.
    newEquipmentChangeUtil.handleSet(new EngineNumber(new BigDecimal("-10"), "kg"));

    // Assert
    EngineNumber salesResult = engine.getStream("sales");
    assertEquals(0, BigDecimal.ZERO.compareTo(salesResult.getValue()),
        "Expected sales to be 0 (clamped) but got " + salesResult.getValue());
  }

  @Test
  void testHandleCapAbsoluteUnitsBelowCurrentReducesSales() {
    // Arrange
    setStreamValue("domestic", new BigDecimal("100"), "kg");
    setStreamValue("newEquipment", new BigDecimal("100"), "units");

    // Act - cap below the current 100-unit value should apply the excess as a sales reduction.
    newEquipmentChangeUtil.handleCap(new EngineNumber(new BigDecimal("60"), "units"), null, null);

    // Assert - excess is 40 units, so sales drops from 100 to 60.
    EngineNumber salesResult = engine.getStream("sales");
    assertEquals(0, new BigDecimal("60").compareTo(salesResult.getValue()),
        "Expected sales to be 60 (100 - 40 excess) but got " + salesResult.getValue());
  }

  @Test
  void testHandleCapAbsoluteUnitsAboveCurrentIsNoOp() {
    // Arrange
    setStreamValue("domestic", new BigDecimal("100"), "kg");
    setStreamValue("newEquipment", new BigDecimal("100"), "units");

    // Act - cap above the current 100-unit value is already satisfied, so it should no-op.
    newEquipmentChangeUtil.handleCap(new EngineNumber(new BigDecimal("150"), "units"), null, null);

    // Assert - sales is untouched.
    EngineNumber salesResult = engine.getStream("sales");
    assertEquals(0, new BigDecimal("100").compareTo(salesResult.getValue()),
        "Expected sales to remain 100 (cap already satisfied) but got " + salesResult.getValue());
  }

  @Test
  void testHandleCapAbsoluteUnitsEqualToCurrentIsNoOp() {
    // Arrange
    setStreamValue("domestic", new BigDecimal("100"), "kg");
    setStreamValue("newEquipment", new BigDecimal("100"), "units");

    // Act - cap exactly equal to the current value should also no-op (not raise excess to 0).
    newEquipmentChangeUtil.handleCap(new EngineNumber(new BigDecimal("100"), "units"), null, null);

    // Assert
    EngineNumber salesResult = engine.getStream("sales");
    assertEquals(0, new BigDecimal("100").compareTo(salesResult.getValue()),
        "Expected sales to remain 100 (cap already satisfied) but got " + salesResult.getValue());
  }

  @Test
  void testHandleCapAbsoluteKgBelowCurrentReducesSales() {
    // Arrange - 1 kg/unit initial charge (fixture default), so a 50 kg cap is a 50-unit target.
    setStreamValue("domestic", new BigDecimal("100"), "kg");
    setStreamValue("newEquipment", new BigDecimal("100"), "units");

    // Act
    newEquipmentChangeUtil.handleCap(new EngineNumber(new BigDecimal("50"), "kg"), null, null);

    // Assert - excess is 50 units (100 - 50), so sales drops from 100 to 50.
    EngineNumber salesResult = engine.getStream("sales");
    assertEquals(0, new BigDecimal("50").compareTo(salesResult.getValue()),
        "Expected sales to be 50 (100 - 50 excess) but got " + salesResult.getValue());
  }

  @Test
  void testHandleCapBarePercentUsesPriorYearBasis() {
    // Arrange - year 1: newEquipment is 1000 units.
    setStreamValue("domestic", new BigDecimal("1000"), "kg");
    setStreamValue("newEquipment", new BigDecimal("1000"), "units");

    engine.incrementYear();

    // Year 2: current newEquipment is set to a different value (1200) than year 1's raw
    // snapshot (1000), so a prior-year-basis cap and a current-year-basis cap produce
    // distinguishable results.
    setStreamValue("domestic", new BigDecimal("1200"), "kg");
    setStreamValue("newEquipment", new BigDecimal("1200"), "units");

    // Act - bare "%" cap should resolve against year 1's raw 1000, not year 2's current 1200.
    newEquipmentChangeUtil.handleCap(new EngineNumber(new BigDecimal("90"), "%"), null, null);

    // Assert - target is 900 (90% of prior year's 1000); current is 1200, so excess is 300 and
    // sales drops from 1200 to 900.
    EngineNumber salesResult = engine.getStream("sales");
    assertEquals(0, new BigDecimal("900").compareTo(salesResult.getValue()),
        "Expected sales to be 900 (90% of prior year's 1000) but got " + salesResult.getValue());
  }

  @Test
  void testHandleCapPercentPriorYearMatchesBarePercent() {
    // Arrange - identical setup to testHandleCapBarePercentUsesPriorYearBasis.
    setStreamValue("domestic", new BigDecimal("1000"), "kg");
    setStreamValue("newEquipment", new BigDecimal("1000"), "units");

    engine.incrementYear();

    setStreamValue("domestic", new BigDecimal("1200"), "kg");
    setStreamValue("newEquipment", new BigDecimal("1200"), "units");

    // Act - explicit "% prior year" should be identical to bare "%" for cap.
    newEquipmentChangeUtil.handleCap(new EngineNumber(new BigDecimal("90"), "%prioryear"), null, null);

    // Assert - same result as the bare-percent test: 900.
    EngineNumber salesResult = engine.getStream("sales");
    assertEquals(0, new BigDecimal("900").compareTo(salesResult.getValue()),
        "Expected sales to be 900 (identical to bare %) but got " + salesResult.getValue());
  }

  @Test
  void testHandleCapPercentCurrentYearDiffersFromBarePercent() {
    // Arrange - identical setup to the prior-year tests above.
    setStreamValue("domestic", new BigDecimal("1000"), "kg");
    setStreamValue("newEquipment", new BigDecimal("1000"), "units");

    engine.incrementYear();

    setStreamValue("domestic", new BigDecimal("1200"), "kg");
    setStreamValue("newEquipment", new BigDecimal("1200"), "units");

    // Act - "% current year" should resolve against year 2's current 1200, not year 1's 1000.
    newEquipmentChangeUtil.handleCap(new EngineNumber(new BigDecimal("90"), "%currentyear"), null, null);

    // Assert - target is 1080 (90% of current year's 1200); current is 1200, so excess is 120
    // and sales drops from 1200 to 1080 -- measurably different from the prior-year basis's 900.
    EngineNumber salesResult = engine.getStream("sales");
    assertEquals(0, new BigDecimal("1080").compareTo(salesResult.getValue()),
        "Expected sales to be 1080 (90% of current year's 1200) but got " + salesResult.getValue());
  }

  @Test
  void testHandleCapPercentCurrentMatchesPercentCurrentYear() {
    // Arrange - identical setup to testHandleCapPercentCurrentYearDiffersFromBarePercent.
    setStreamValue("domestic", new BigDecimal("1000"), "kg");
    setStreamValue("newEquipment", new BigDecimal("1000"), "units");

    engine.incrementYear();

    setStreamValue("domestic", new BigDecimal("1200"), "kg");
    setStreamValue("newEquipment", new BigDecimal("1200"), "units");

    // Act - "% current" should be identical to "% current year" for cap.
    newEquipmentChangeUtil.handleCap(new EngineNumber(new BigDecimal("90"), "%current"), null, null);

    // Assert - same result as the current-year test: 1080.
    EngineNumber salesResult = engine.getStream("sales");
    assertEquals(0, new BigDecimal("1080").compareTo(salesResult.getValue()),
        "Expected sales to be 1080 (identical to % current year) but got " + salesResult.getValue());
  }

  @Test
  void testHandleCapNegativeAbsoluteTargetClampsAtZero() {
    // Arrange
    setStreamValue("domestic", new BigDecimal("100"), "kg");
    setStreamValue("newEquipment", new BigDecimal("100"), "units");

    // Act - a negative absolute cap target should clamp to a zero target (decision 1), not go
    // negative; this drives newEquipment all the way down to 0, not below.
    newEquipmentChangeUtil.handleCap(new EngineNumber(new BigDecimal("-50"), "units"), null, null);

    // Assert - all 100 units of excess are removed from sales, landing at 0, not -50.
    EngineNumber salesResult = engine.getStream("sales");
    assertEquals(0, BigDecimal.ZERO.compareTo(salesResult.getValue()),
        "Expected sales to be 0 (clamped target) but got " + salesResult.getValue());
  }
}
