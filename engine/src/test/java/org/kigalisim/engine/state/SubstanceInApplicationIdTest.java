/**
 * Unit tests for the SubstanceInApplicationId class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Tests for the SubstanceInApplicationId class.
 */
public class SubstanceInApplicationIdTest {

  /**
   * Test that SubstanceInApplicationId can be initialized.
   */
  @Test
  public void testInitializes() {
    SubstanceInApplicationId id = new SubstanceInApplicationId("test app", "test substance");
    assertNotNull(id, "SubstanceInApplicationId should be constructable");
  }

  /**
   * Test that getApplication returns correct application name.
   */
  @Test
  public void testGetApplication() {
    SubstanceInApplicationId id = new SubstanceInApplicationId("test app", "test substance");
    assertEquals("test app", id.getApplication(), "Should return correct application name");
  }

  /**
   * Test that getSubstance returns correct substance name.
   */
  @Test
  public void testGetSubstance() {
    SubstanceInApplicationId id = new SubstanceInApplicationId("test app", "test substance");
    assertEquals("test substance", id.getSubstance(), "Should return correct substance name");
  }

  /**
   * Test with realistic application and substance names.
   */
  @Test
  public void testWithRealisticNames() {
    SubstanceInApplicationId id = new SubstanceInApplicationId(
        "domestic refrigeration", "HFC-134a");
    assertEquals("domestic refrigeration", id.getApplication(),
                 "Should return correct realistic application name");
    assertEquals("HFC-134a", id.getSubstance(),
                 "Should return correct realistic substance name");
  }
}
