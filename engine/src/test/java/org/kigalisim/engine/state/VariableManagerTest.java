/**
 * Unit tests for the VariableManager class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.kigalisim.engine.state.EngineConstants.APPLICATION_CONTEXT;
import static org.kigalisim.engine.state.EngineConstants.GLOBAL_CONTEXT;
import static org.kigalisim.engine.state.EngineConstants.STANZA_CONTEXT;

import org.junit.jupiter.api.Test;

/**
 * Tests for the VariableManager class.
 */
public class VariableManagerTest {

  /**
   * Test that VariableManager can be initialized.
   */
  @Test
  public void testInitializes() {
    VariableManager manager = new VariableManager(GLOBAL_CONTEXT);
    assertNotNull(manager, "VariableManager should be constructable");
  }

  /**
   * Test defining and accessing variables.
   */
  @Test
  public void testDefineAndAccessVariable() {
    VariableManager manager = new VariableManager(GLOBAL_CONTEXT);

    manager.defineVariable("testVar");
    manager.setVariable("testVar", 123);

    assertEquals(123, manager.getVariable("testVar"), "Should retrieve set variable value");
  }

  /**
   * Test that defining a variable twice throws an error.
   */
  @Test
  public void testDefineVariableTwiceThrows() {
    VariableManager manager = new VariableManager(GLOBAL_CONTEXT);

    manager.defineVariable("testVar");

    assertThrows(IllegalStateException.class, () -> {
      manager.defineVariable("testVar");
    }, "Should throw when defining variable twice");
  }

  /**
   * Test that setting an undefined variable throws an error.
   */
  @Test
  public void testSetUndefinedVariableThrows() {
    VariableManager manager = new VariableManager(GLOBAL_CONTEXT);

    assertThrows(IllegalStateException.class, () -> {
      manager.setVariable("undefinedVar", 123);
    }, "Should throw when setting undefined variable");
  }

  /**
   * Test that getting an undefined variable throws an error.
   */
  @Test
  public void testGetUndefinedVariableThrows() {
    VariableManager manager = new VariableManager(GLOBAL_CONTEXT);

    assertThrows(IllegalStateException.class, () -> {
      manager.getVariable("undefinedVar");
    }, "Should throw when getting undefined variable");
  }

  /**
   * Test creating manager with different context levels.
   */
  @Test
  public void testGetWithLevel() {
    VariableManager globalManager = new VariableManager(GLOBAL_CONTEXT);
    globalManager.defineVariable("globalVar");
    globalManager.setVariable("globalVar", "global");

    VariableManager stanzaManager = globalManager.getWithLevel(STANZA_CONTEXT);

    // Should still be able to access global variable
    assertEquals("global", stanzaManager.getVariable("globalVar"),
                 "Should access global variable from stanza level");

    // Should be able to define stanza-level variable
    stanzaManager.defineVariable("stanzaVar");
    stanzaManager.setVariable("stanzaVar", "stanza");
    assertEquals("stanza", stanzaManager.getVariable("stanzaVar"),
                 "Should access stanza variable");
  }

  /**
   * Test variable shadowing between context levels.
   */
  @Test
  public void testVariableShadowing() {
    VariableManager globalManager = new VariableManager(GLOBAL_CONTEXT);
    globalManager.defineVariable("testVar");
    globalManager.setVariable("testVar", "global");

    VariableManager stanzaManager = globalManager.getWithLevel(STANZA_CONTEXT);
    assertEquals("global", stanzaManager.getVariable("testVar"),
                 "Should read global variable from stanza level");

    // Define variable with same name at stanza level
    stanzaManager.defineVariable("testVar");
    stanzaManager.setVariable("testVar", "stanza");
    assertEquals("stanza", stanzaManager.getVariable("testVar"),
                 "Should read stanza variable when shadowing global");

    // Setting the variable should affect stanza level, not global
    stanzaManager.setVariable("testVar", "stanza_modified");
    assertEquals("stanza_modified", stanzaManager.getVariable("testVar"),
                 "Should read modified stanza variable");

    // Global should remain unchanged
    assertEquals("global", globalManager.getVariable("testVar"),
                 "Global variable should remain unchanged");
  }

  /**
   * Test invalid context level.
   */
  @Test
  public void testInvalidContextLevel() {
    VariableManager manager = new VariableManager(GLOBAL_CONTEXT);

    assertThrows(IllegalArgumentException.class, () -> {
      manager.getWithLevel(-1);
    }, "Should throw for invalid negative context level");

    assertThrows(IllegalArgumentException.class, () -> {
      manager.getWithLevel(4);
    }, "Should throw for invalid high context level");
  }

  /**
   * Test variable access across multiple context levels.
   */
  @Test
  public void testMultiLevelVariableAccess() {
    VariableManager globalManager = new VariableManager(GLOBAL_CONTEXT);
    globalManager.defineVariable("globalVar");
    globalManager.setVariable("globalVar", "global");

    VariableManager stanzaManager = globalManager.getWithLevel(STANZA_CONTEXT);
    stanzaManager.defineVariable("stanzaVar");
    stanzaManager.setVariable("stanzaVar", "stanza");

    VariableManager appManager = stanzaManager.getWithLevel(APPLICATION_CONTEXT);

    // Should access both global and stanza variables
    assertEquals("global", appManager.getVariable("globalVar"),
                 "Should access global variable from application level");
    assertEquals("stanza", appManager.getVariable("stanzaVar"),
                 "Should access stanza variable from application level");

    // Should be able to define app-level variable
    appManager.defineVariable("appVar");
    appManager.setVariable("appVar", "application");
    assertEquals("application", appManager.getVariable("appVar"),
                 "Should access application variable");
  }
}
