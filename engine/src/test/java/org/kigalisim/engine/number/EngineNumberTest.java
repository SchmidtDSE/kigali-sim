/**
 * Unit tests for the EngineNumber class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.number;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Tests for the EngineNumber class.
 */
public class EngineNumberTest {

  /**
   * Helper method to create an example EngineNumber.
   *
   * @return A new EngineNumber with value 1.23 and units "kg"
   */
  private EngineNumber makeExample() {
    return new EngineNumber(1.23, "kg");
  }

  /**
   * Test that EngineNumber can be initialized.
   */
  @Test
  public void testInitializes() {
    EngineNumber number = makeExample();
    assertNotNull(number, "EngineNumber should be constructable");
  }

  /**
   * Test the getValue method.
   */
  @Test
  public void testGetValue() {
    EngineNumber number = makeExample();
    BigDecimal expected = BigDecimal.valueOf(1.23);
    assertEquals(expected, number.getValue(), "getValue should return the correct value");
  }

  /**
   * Test the getUnits method.
   */
  @Test
  public void testGetUnits() {
    EngineNumber number = makeExample();
    assertEquals("kg", number.getUnits(), "getUnits should return the correct units");
  }

  /**
   * Test the hasEquipmentUnits method with equipment units.
   */
  @Test
  public void testHasEquipmentUnitsTrue() {
    EngineNumber number = new EngineNumber(10.0, "units");
    assertTrue(number.hasEquipmentUnits(), "hasEquipmentUnits should return true for 'units'");

    EngineNumber number2 = new EngineNumber(5.0, "unit");
    assertTrue(number2.hasEquipmentUnits(), "hasEquipmentUnits should return true for 'unit'");
  }

  /**
   * Test the hasEquipmentUnits method with non-equipment units.
   */
  @Test
  public void testHasEquipmentUnitsFalse() {
    EngineNumber number = makeExample();
    assertFalse(number.hasEquipmentUnits(), "hasEquipmentUnits should return false for 'kg'");
  }

  /**
   * Test constructor with BigDecimal value.
   */
  @Test
  public void testConstructorWithBigDecimal() {
    BigDecimal value = new BigDecimal("123.456789");
    String units = "mt";
    EngineNumber number = new EngineNumber(value, units);
    
    assertEquals(value, number.getValue(), "getValue should return the exact BigDecimal value");
    assertEquals(units, number.getUnits(), "getUnits should return the correct units");
  }

  /**
   * Test constructor with double value.
   */
  @Test
  public void testConstructorWithDouble() {
    double value = 42.5;
    String units = "liters";
    EngineNumber number = new EngineNumber(value, units);
    
    assertEquals(
        BigDecimal.valueOf(value),
        number.getValue(), 
        "getValue should return the converted BigDecimal value"
    );
    assertEquals(units, number.getUnits(), "getUnits should return the correct units");
  }
}
