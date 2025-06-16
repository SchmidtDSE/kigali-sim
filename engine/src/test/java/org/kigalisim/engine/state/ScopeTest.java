/**
 * Unit tests for the Scope class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;

/**
 * Tests for the Scope class.
 */
public class ScopeTest {

  /**
   * Test that Scope can be initialized.
   */
  @Test
  public void testInitializes() {
    Scope scope = new Scope("default", "test app", "test substance");
    assertNotNull(scope, "Scope should be constructable");
  }

  /**
   * Test getter methods.
   */
  @Test
  public void testGetters() {
    Scope scope = new Scope("default", "test app", "test substance");
    assertEquals("default", scope.getStanza(), "Should return correct stanza");
    assertEquals("test app", scope.getApplication(), "Should return correct application");
    assertEquals("test substance", scope.getSubstance(), "Should return correct substance");
  }

  /**
   * Test changing substance creates new scope with correct values.
   */
  @Test
  public void testChangesSubstance() {
    Scope scopeOld = new Scope("default", "test app", "test substance");
    Scope scopeNew = scopeOld.getWithSubstance("test substance 2");

    assertEquals("default", scopeNew.getStanza(), "Should preserve stanza");
    assertEquals("test app", scopeNew.getApplication(), "Should preserve application");
    assertEquals("test substance 2", scopeNew.getSubstance(), "Should update substance");
  }

  /**
   * Test changing application creates new scope with correct values.
   */
  @Test
  public void testChangesApplication() {
    Scope scopeOld = new Scope("default", "test app", "test substance");
    Scope scopeNew = scopeOld.getWithApplication("test app 2");

    assertEquals("default", scopeNew.getStanza(), "Should preserve stanza");
    assertEquals("test app 2", scopeNew.getApplication(), "Should update application");
    assertNull(scopeNew.getSubstance(), "Should reset substance to null");
  }

  /**
   * Test changing stanza creates new scope with correct values.
   */
  @Test
  public void testChangesStanza() {
    Scope scopeOld = new Scope("default", "test app", "test substance");
    Scope scopeNew = scopeOld.getWithStanza("policy \"test policy\"");

    assertEquals("policy \"test policy\"", scopeNew.getStanza(), "Should update stanza");
    assertNull(scopeNew.getApplication(), "Should reset application to null");
    assertNull(scopeNew.getSubstance(), "Should reset substance to null");
  }

  /**
   * Test writing and reading variables.
   */
  @Test
  public void testWritesAndReadsVar() {
    Scope scope = new Scope("default", "test app", "test substance");
    scope.defineVariable("testVar");
    EngineNumber testValue = new EngineNumber(BigDecimal.valueOf(123), "test_units");
    scope.setVariable("testVar", testValue);
    
    EngineNumber result = scope.getVariable("testVar");
    assertEquals(testValue.getValue(), result.getValue(), "Should retrieve set variable value");
    assertEquals(testValue.getUnits(), result.getUnits(), "Should retrieve set variable units");
  }

  /**
   * Test reading variables upwards in scope.
   */
  @Test
  public void testReadsUpwardsInScope() {
    Scope oldScope = new Scope("default", "test app", null);
    oldScope.defineVariable("testVar");
    EngineNumber testValue = new EngineNumber(BigDecimal.valueOf(123), "test_units");
    oldScope.setVariable("testVar", testValue);
    
    EngineNumber result1 = oldScope.getVariable("testVar");
    assertEquals(testValue.getValue(), result1.getValue(), 
                 "Should read variable in original scope");
    assertEquals(testValue.getUnits(), result1.getUnits(), 
                 "Should read variable units in original scope");

    Scope newScope = oldScope.getWithSubstance("test substance 2");
    EngineNumber result2 = newScope.getVariable("testVar");
    assertEquals(testValue.getValue(), result2.getValue(), 
                 "Should read variable from parent scope");
    assertEquals(testValue.getUnits(), result2.getUnits(), 
                 "Should read variable units from parent scope");

    EngineNumber updatedValue = new EngineNumber(BigDecimal.valueOf(124), "updated_units");
    newScope.setVariable("testVar", updatedValue);
    
    EngineNumber result3 = newScope.getVariable("testVar");
    assertEquals(updatedValue.getValue(), result3.getValue(), "Should read updated variable value");
    assertEquals(updatedValue.getUnits(), result3.getUnits(), "Should read updated variable units");
  }

  /**
   * Test variable shadowing.
   */
  @Test
  public void testVariableShadowing() {
    Scope oldScope = new Scope("default", "test app", null);
    oldScope.defineVariable("testVar");
    EngineNumber originalValue = new EngineNumber(BigDecimal.valueOf(123), "original_units");
    oldScope.setVariable("testVar", originalValue);
    
    EngineNumber result1 = oldScope.getVariable("testVar");
    assertEquals(originalValue.getValue(), result1.getValue(), 
                 "Should read variable in original scope");
    assertEquals(originalValue.getUnits(), result1.getUnits(), 
                 "Should read variable units in original scope");

    Scope newScope = oldScope.getWithSubstance("test substance 2");
    newScope.defineVariable("testVar");
    EngineNumber shadowValue = new EngineNumber(BigDecimal.valueOf(124), "shadow_units");
    newScope.setVariable("testVar", shadowValue);
    
    EngineNumber result2 = newScope.getVariable("testVar");
    assertEquals(shadowValue.getValue(), result2.getValue(), "Should read shadowed variable");
    assertEquals(shadowValue.getUnits(), result2.getUnits(), "Should read shadowed variable units");

    Scope restoredScope = newScope.getWithSubstance("test substance 3");
    EngineNumber result3 = restoredScope.getVariable("testVar");
    assertEquals(originalValue.getValue(), result3.getValue(),
                 "Should read original variable when shadow is removed");
    assertEquals(originalValue.getUnits(), result3.getUnits(),
                 "Should read original variable units when shadow is removed");
  }

  /**
   * Test editing variables in parent scopes.
   */
  @Test
  public void testEditsScopesAbove() {
    Scope oldScope = new Scope("default", "test app", null);
    oldScope.defineVariable("testVar");
    EngineNumber originalValue = new EngineNumber(BigDecimal.valueOf(123), "original_units");
    oldScope.setVariable("testVar", originalValue);
    
    EngineNumber result1 = oldScope.getVariable("testVar");
    assertEquals(originalValue.getValue(), result1.getValue(), 
                 "Should read variable in original scope");
    assertEquals(originalValue.getUnits(), result1.getUnits(), 
                 "Should read variable units in original scope");

    Scope tempScope = oldScope.getWithSubstance("test substance 2");
    EngineNumber modifiedValue = new EngineNumber(BigDecimal.valueOf(124), "modified_units");
    tempScope.setVariable("testVar", modifiedValue);

    Scope newScope = tempScope.getWithSubstance("test substance 3");
    EngineNumber result2 = newScope.getVariable("testVar");
    assertEquals(modifiedValue.getValue(), result2.getValue(),
                 "Should read modified variable from parent scope");
    assertEquals(modifiedValue.getUnits(), result2.getUnits(),
                 "Should read modified variable units from parent scope");
  }

  /**
   * Test that substance cannot be specified without application.
   */
  @Test
  public void testSubstanceWithoutApplicationThrows() {
    assertThrows(IllegalArgumentException.class, () -> {
      new Scope("default", null, "test substance");
    }, "Should throw when substance specified without application");
  }

  /**
   * Test that application cannot be specified without stanza.
   */
  @Test
  public void testApplicationWithoutStanzaThrows() {
    assertThrows(IllegalArgumentException.class, () -> {
      new Scope(null, "test app", null);
    }, "Should throw when application specified without stanza");
  }

  /**
   * Test that setting substance without application in existing scope throws.
   */
  @Test
  public void testSetSubstanceWithoutApplicationThrows() {
    Scope scope = new Scope("default", null, null);

    assertThrows(IllegalStateException.class, () -> {
      scope.getWithSubstance("test substance");
    }, "Should throw when trying to set substance without application");
  }

  /**
   * Test that setting application without stanza in existing scope throws.
   */
  @Test
  public void testSetApplicationWithoutStanzaThrows() {
    Scope scope = new Scope(null, null, null);

    assertThrows(IllegalStateException.class, () -> {
      scope.getWithApplication("test app");
    }, "Should throw when trying to set application without stanza");
  }
}
