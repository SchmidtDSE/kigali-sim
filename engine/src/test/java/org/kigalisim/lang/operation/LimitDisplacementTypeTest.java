/**
 * Unit tests for LimitDisplacementType parsing and functionality.
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
 * Tests for the LimitDisplacementType enum in CapOperation and FloorOperation.
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

    assertEquals(CapOperation.LimitDisplacementType.EQUIVALENT, operation.getDisplacementType(),
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

    assertEquals(FloorOperation.LimitDisplacementType.EQUIVALENT, operation.getDisplacementType(),
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
        CapOperation.LimitDisplacementType.BY_VOLUME);

    assertNotNull(operation, "CapOperation should be constructable with BY_VOLUME type");
    assertEquals(CapOperation.LimitDisplacementType.BY_VOLUME, operation.getDisplacementType(),
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
        CapOperation.LimitDisplacementType.BY_UNITS);

    assertNotNull(operation, "CapOperation should be constructable with BY_UNITS type");
    assertEquals(CapOperation.LimitDisplacementType.BY_UNITS, operation.getDisplacementType(),
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
        FloorOperation.LimitDisplacementType.BY_VOLUME);

    assertNotNull(operation, "FloorOperation should be constructable with BY_VOLUME type");
    assertEquals(FloorOperation.LimitDisplacementType.BY_VOLUME, operation.getDisplacementType(),
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
        FloorOperation.LimitDisplacementType.BY_UNITS);

    assertNotNull(operation, "FloorOperation should be constructable with BY_UNITS type");
    assertEquals(FloorOperation.LimitDisplacementType.BY_UNITS, operation.getDisplacementType(),
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
        CapOperation.LimitDisplacementType.BY_VOLUME);

    assertNotNull(operation, "CapOperation should be constructable with duration and BY_VOLUME type");
    assertEquals(CapOperation.LimitDisplacementType.BY_VOLUME, operation.getDisplacementType(),
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
        FloorOperation.LimitDisplacementType.BY_UNITS);

    assertNotNull(operation, "FloorOperation should be constructable with duration and BY_UNITS type");
    assertEquals(FloorOperation.LimitDisplacementType.BY_UNITS, operation.getDisplacementType(),
        "FloorOperation with duration should store and return BY_UNITS displacement type");
  }

  /**
   * Test that all three displacement type enum values exist for CapOperation.
   */
  @Test
  public void testCapOperationEnumValues() {
    assertEquals(3, CapOperation.LimitDisplacementType.values().length,
        "CapOperation.LimitDisplacementType should have exactly 3 enum values");

    // Verify all expected values exist
    assertNotNull(CapOperation.LimitDisplacementType.EQUIVALENT,
        "EQUIVALENT should be a valid enum value");
    assertNotNull(CapOperation.LimitDisplacementType.BY_VOLUME,
        "BY_VOLUME should be a valid enum value");
    assertNotNull(CapOperation.LimitDisplacementType.BY_UNITS,
        "BY_UNITS should be a valid enum value");
  }

  /**
   * Test that all three displacement type enum values exist for FloorOperation.
   */
  @Test
  public void testFloorOperationEnumValues() {
    assertEquals(3, FloorOperation.LimitDisplacementType.values().length,
        "FloorOperation.LimitDisplacementType should have exactly 3 enum values");

    // Verify all expected values exist
    assertNotNull(FloorOperation.LimitDisplacementType.EQUIVALENT,
        "EQUIVALENT should be a valid enum value");
    assertNotNull(FloorOperation.LimitDisplacementType.BY_VOLUME,
        "BY_VOLUME should be a valid enum value");
    assertNotNull(FloorOperation.LimitDisplacementType.BY_UNITS,
        "BY_UNITS should be a valid enum value");
  }
}
