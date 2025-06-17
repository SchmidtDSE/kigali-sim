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

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;

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
    EngineNumber testValue = new EngineNumber(BigDecimal.valueOf(123), "units");
    manager.setVariable("testVar", testValue);

    EngineNumber result = manager.getVariable("testVar");
    assertEquals(testValue.getValue(), result.getValue(), "Should retrieve set variable value");
    assertEquals(testValue.getUnits(), result.getUnits(), "Should retrieve set variable units");
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
      EngineNumber testValue = new EngineNumber(BigDecimal.valueOf(123), "units");
      manager.setVariable("undefinedVar", testValue);
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
    EngineNumber globalValue = new EngineNumber(BigDecimal.valueOf(100), "global_units");
    globalManager.setVariable("globalVar", globalValue);

    VariableManager stanzaManager = globalManager.getWithLevel(STANZA_CONTEXT);

    // Should still be able to access global variable
    EngineNumber result = stanzaManager.getVariable("globalVar");
    assertEquals(globalValue.getValue(), result.getValue(),
                 "Should access global variable from stanza level");
    assertEquals(globalValue.getUnits(), result.getUnits(),
                 "Should access global variable units from stanza level");

    // Should be able to define stanza-level variable
    stanzaManager.defineVariable("stanzaVar");
    EngineNumber stanzaValue = new EngineNumber(BigDecimal.valueOf(200), "stanza_units");
    stanzaManager.setVariable("stanzaVar", stanzaValue);

    EngineNumber stanzaResult = stanzaManager.getVariable("stanzaVar");
    assertEquals(stanzaValue.getValue(), stanzaResult.getValue(),
                 "Should access stanza variable");
    assertEquals(stanzaValue.getUnits(), stanzaResult.getUnits(),
                 "Should access stanza variable units");
  }

  /**
   * Test variable shadowing between context levels.
   */
  @Test
  public void testVariableShadowing() {
    VariableManager globalManager = new VariableManager(GLOBAL_CONTEXT);
    globalManager.defineVariable("testVar");
    EngineNumber globalValue = new EngineNumber(BigDecimal.valueOf(100), "global_units");
    globalManager.setVariable("testVar", globalValue);

    VariableManager stanzaManager = globalManager.getWithLevel(STANZA_CONTEXT);
    EngineNumber result1 = stanzaManager.getVariable("testVar");
    assertEquals(globalValue.getValue(), result1.getValue(),
                 "Should read global variable from stanza level");
    assertEquals(globalValue.getUnits(), result1.getUnits(),
                 "Should read global variable units from stanza level");

    // Define variable with same name at stanza level
    stanzaManager.defineVariable("testVar");
    EngineNumber stanzaValue = new EngineNumber(BigDecimal.valueOf(200), "stanza_units");
    stanzaManager.setVariable("testVar", stanzaValue);

    EngineNumber result2 = stanzaManager.getVariable("testVar");
    assertEquals(stanzaValue.getValue(), result2.getValue(),
                 "Should read stanza variable when shadowing global");
    assertEquals(stanzaValue.getUnits(), result2.getUnits(),
                 "Should read stanza variable units when shadowing global");

    // Setting the variable should affect stanza level, not global
    EngineNumber modifiedValue = new EngineNumber(BigDecimal.valueOf(250), "modified_units");
    stanzaManager.setVariable("testVar", modifiedValue);

    EngineNumber result3 = stanzaManager.getVariable("testVar");
    assertEquals(modifiedValue.getValue(), result3.getValue(),
                 "Should read modified stanza variable");
    assertEquals(modifiedValue.getUnits(), result3.getUnits(),
                 "Should read modified stanza variable units");

    // Global should remain unchanged
    EngineNumber globalResult = globalManager.getVariable("testVar");
    assertEquals(globalValue.getValue(), globalResult.getValue(),
                 "Global variable should remain unchanged");
    assertEquals(globalValue.getUnits(), globalResult.getUnits(),
                 "Global variable units should remain unchanged");
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
    EngineNumber globalValue = new EngineNumber(BigDecimal.valueOf(100), "global_units");
    globalManager.setVariable("globalVar", globalValue);

    VariableManager stanzaManager = globalManager.getWithLevel(STANZA_CONTEXT);
    stanzaManager.defineVariable("stanzaVar");
    EngineNumber stanzaValue = new EngineNumber(BigDecimal.valueOf(200), "stanza_units");
    stanzaManager.setVariable("stanzaVar", stanzaValue);

    VariableManager appManager = stanzaManager.getWithLevel(APPLICATION_CONTEXT);

    // Should access both global and stanza variables
    EngineNumber globalResult = appManager.getVariable("globalVar");
    assertEquals(globalValue.getValue(), globalResult.getValue(),
                 "Should access global variable from application level");
    assertEquals(globalValue.getUnits(), globalResult.getUnits(),
                 "Should access global variable units from application level");

    EngineNumber stanzaResult = appManager.getVariable("stanzaVar");
    assertEquals(stanzaValue.getValue(), stanzaResult.getValue(),
                 "Should access stanza variable from application level");
    assertEquals(stanzaValue.getUnits(), stanzaResult.getUnits(),
                 "Should access stanza variable units from application level");

    // Should be able to define app-level variable
    appManager.defineVariable("appVar");
    EngineNumber appValue = new EngineNumber(BigDecimal.valueOf(300), "app_units");
    appManager.setVariable("appVar", appValue);

    EngineNumber appResult = appManager.getVariable("appVar");
    assertEquals(appValue.getValue(), appResult.getValue(),
                 "Should access application variable");
    assertEquals(appValue.getUnits(), appResult.getUnits(),
                 "Should access application variable units");
  }
}
