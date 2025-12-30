/**
 * Unit tests for DisplacementType enum functionality.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.lang.time.ParsedDuring;

/**
 * Tests for the DisplacementType enum used by CapOperation and FloorOperation.
 */
public class LimitDisplacementTypeTest {

  /**
   * Test that CapOperation defaults to EQUIVALENT displacement type.
   */
  @Test
  public void testCapOperationDefaultsToEquivalent() {
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);
    CapOperation operation = new CapOperation("domestic", valueOperation);

    assertEquals(DisplacementType.EQUIVALENT, operation.getDisplacementType(),
        "CapOperation should default to EQUIVALENT displacement type");
  }

  /**
   * Test that FloorOperation defaults to EQUIVALENT displacement type.
   */
  @Test
  public void testFloorOperationDefaultsToEquivalent() {
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);
    FloorOperation operation = new FloorOperation("domestic", valueOperation);

    assertEquals(DisplacementType.EQUIVALENT, operation.getDisplacementType(),
        "FloorOperation should default to EQUIVALENT displacement type");
  }

  /**
   * Test that CapOperation with BY_VOLUME type stores and returns correct enum value.
   */
  @Test
  public void testCapOperationWithByVolume() {
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);
    CapOperation operation = new CapOperation("domestic", valueOperation, "other_stream",
        DisplacementType.BY_VOLUME);

    assertNotNull(operation, "CapOperation should be constructable with BY_VOLUME type");
    assertEquals(DisplacementType.BY_VOLUME, operation.getDisplacementType(),
        "CapOperation should store and return BY_VOLUME displacement type");
  }

  /**
   * Test that CapOperation with BY_UNITS type stores and returns correct enum value.
   */
  @Test
  public void testCapOperationWithByUnits() {
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);
    CapOperation operation = new CapOperation("domestic", valueOperation, "other_stream",
        DisplacementType.BY_UNITS);

    assertNotNull(operation, "CapOperation should be constructable with BY_UNITS type");
    assertEquals(DisplacementType.BY_UNITS, operation.getDisplacementType(),
        "CapOperation should store and return BY_UNITS displacement type");
  }

  /**
   * Test that FloorOperation with BY_VOLUME type stores and returns correct enum value.
   */
  @Test
  public void testFloorOperationWithByVolume() {
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);
    FloorOperation operation = new FloorOperation("domestic", valueOperation, "other_stream",
        DisplacementType.BY_VOLUME);

    assertNotNull(operation, "FloorOperation should be constructable with BY_VOLUME type");
    assertEquals(DisplacementType.BY_VOLUME, operation.getDisplacementType(),
        "FloorOperation should store and return BY_VOLUME displacement type");
  }

  /**
   * Test that FloorOperation with BY_UNITS type stores and returns correct enum value.
   */
  @Test
  public void testFloorOperationWithByUnits() {
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);
    FloorOperation operation = new FloorOperation("domestic", valueOperation, "other_stream",
        DisplacementType.BY_UNITS);

    assertNotNull(operation, "FloorOperation should be constructable with BY_UNITS type");
    assertEquals(DisplacementType.BY_UNITS, operation.getDisplacementType(),
        "FloorOperation should store and return BY_UNITS displacement type");
  }

  /**
   * Test that CapOperation with duration and BY_VOLUME type works correctly.
   */
  @Test
  public void testCapOperationWithDurationAndByVolume() {
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);
    ParsedDuring during = new ParsedDuring(Optional.empty(), Optional.empty());
    CapOperation operation = new CapOperation("domestic", valueOperation, "other_stream", during,
        DisplacementType.BY_VOLUME);

    assertNotNull(operation, "CapOperation should be constructable with duration and BY_VOLUME type");
    assertEquals(DisplacementType.BY_VOLUME, operation.getDisplacementType(),
        "CapOperation with duration should store and return BY_VOLUME displacement type");
  }

  /**
   * Test that FloorOperation with duration and BY_UNITS type works correctly.
   */
  @Test
  public void testFloorOperationWithDurationAndByUnits() {
    EngineNumber number = new EngineNumber(BigDecimal.valueOf(42), "kg");
    Operation valueOperation = new PreCalculatedOperation(number);
    ParsedDuring during = new ParsedDuring(Optional.empty(), Optional.empty());
    FloorOperation operation = new FloorOperation("domestic", valueOperation, "other_stream", during,
        DisplacementType.BY_UNITS);

    assertNotNull(operation, "FloorOperation should be constructable with duration and BY_UNITS type");
    assertEquals(DisplacementType.BY_UNITS, operation.getDisplacementType(),
        "FloorOperation with duration should store and return BY_UNITS displacement type");
  }

  /**
   * Test that all three displacement type enum values exist.
   */
  @Test
  public void testDisplacementTypeEnumValues() {
    assertEquals(3, DisplacementType.values().length,
        "DisplacementType should have exactly 3 enum values");

    // Verify all expected values exist
    assertNotNull(DisplacementType.EQUIVALENT,
        "EQUIVALENT should be a valid enum value");
    assertNotNull(DisplacementType.BY_VOLUME,
        "BY_VOLUME should be a valid enum value");
    assertNotNull(DisplacementType.BY_UNITS,
        "BY_UNITS should be a valid enum value");
  }
}
