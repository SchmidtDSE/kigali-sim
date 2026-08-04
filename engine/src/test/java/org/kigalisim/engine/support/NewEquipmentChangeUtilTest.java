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
}
