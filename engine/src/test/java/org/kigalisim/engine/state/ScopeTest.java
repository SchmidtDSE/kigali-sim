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

import org.junit.jupiter.api.Test;

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
    scope.setVariable("testVar", 123);
    assertEquals(123, scope.getVariable("testVar"), "Should retrieve set variable value");
  }

  /**
   * Test reading variables upwards in scope.
   */
  @Test
  public void testReadsUpwardsInScope() {
    Scope oldScope = new Scope("default", "test app", null);
    oldScope.defineVariable("testVar");
    oldScope.setVariable("testVar", 123);
    assertEquals(123, oldScope.getVariable("testVar"), "Should read variable in original scope");

    Scope newScope = oldScope.getWithSubstance("test substance 2");
    assertEquals(123, newScope.getVariable("testVar"), "Should read variable from parent scope");

    newScope.setVariable("testVar", 124);
    assertEquals(124, newScope.getVariable("testVar"), "Should read updated variable value");
  }

  /**
   * Test variable shadowing.
   */
  @Test
  public void testVariableShadowing() {
    Scope oldScope = new Scope("default", "test app", null);
    oldScope.defineVariable("testVar");
    oldScope.setVariable("testVar", 123);
    assertEquals(123, oldScope.getVariable("testVar"), "Should read variable in original scope");

    Scope newScope = oldScope.getWithSubstance("test substance 2");
    newScope.defineVariable("testVar");
    newScope.setVariable("testVar", 124);
    assertEquals(124, newScope.getVariable("testVar"), "Should read shadowed variable");

    Scope restoredScope = newScope.getWithSubstance("test substance 3");
    assertEquals(123, restoredScope.getVariable("testVar"),
                 "Should read original variable when shadow is removed");
  }

  /**
   * Test editing variables in parent scopes.
   */
  @Test
  public void testEditsScopesAbove() {
    Scope oldScope = new Scope("default", "test app", null);
    oldScope.defineVariable("testVar");
    oldScope.setVariable("testVar", 123);
    assertEquals(123, oldScope.getVariable("testVar"), "Should read variable in original scope");

    Scope tempScope = oldScope.getWithSubstance("test substance 2");
    tempScope.setVariable("testVar", 124);

    Scope newScope = tempScope.getWithSubstance("test substance 3");
    assertEquals(124, newScope.getVariable("testVar"),
                 "Should read modified variable from parent scope");
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
