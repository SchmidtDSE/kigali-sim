/**
 * Tests for ExceptionsGenerator.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for ExceptionsGenerator.
 */
public class ExceptionsGeneratorTest {

  @Test
  public void testRaiseNoAppOrSubstance() {
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      ExceptionsGenerator.raiseNoAppOrSubstance("testing operation", " specified");
    });

    assertEquals("Error testing operation because application and / or substance not specified",
        exception.getMessage());
  }

  @Test
  public void testRaiseNoAppOrSubstanceWithEmptySuffix() {
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      ExceptionsGenerator.raiseNoAppOrSubstance("testing operation", "");
    });

    assertEquals("Error testing operation because application and / or substance not",
        exception.getMessage());
  }

  @Test
  public void testRaiseNoAppOrSubstanceWithDifferentOperation() {
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      ExceptionsGenerator.raiseNoAppOrSubstance("recalculating consumption", " specified");
    });

    assertEquals(
        "Error recalculating consumption because application and / or substance not specified",
        exception.getMessage());
  }

  @Test
  public void testRaiseNewEquipmentInitialCharge() {
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      ExceptionsGenerator.raiseNewEquipmentInitialCharge();
    });

    assertTrue(exception.getMessage().contains("initial charge"));
    assertTrue(exception.getMessage().contains("newEquipment"));
    assertTrue(exception.getMessage().contains("set, change, cap, or floor"));
  }

  @Test
  public void testRaiseNewEquipmentReplace() {
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      ExceptionsGenerator.raiseNewEquipmentReplace();
    });

    assertTrue(exception.getMessage().contains("replace"));
    assertTrue(exception.getMessage().contains("newEquipment"));
    assertTrue(exception.getMessage().contains("set, change, cap, or floor"));
  }
}
