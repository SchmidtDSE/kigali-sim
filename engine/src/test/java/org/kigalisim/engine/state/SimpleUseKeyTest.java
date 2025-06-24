/**
 * Unit tests for the SimpleUseKey class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests for the SimpleUseKey class.
 */
public class SimpleUseKeyTest {

  /**
   * Test that SimpleUseKey can be initialized.
   */
  @Test
  public void testInitializes() {
    SimpleUseKey useKey = new SimpleUseKey("test app", "test substance");
    assertNotNull(useKey, "SimpleUseKey should be constructable");
  }

  /**
   * Test that SimpleUseKey can be initialized with null values.
   */
  @Test
  public void testInitializesWithNulls() {
    SimpleUseKey useKey = new SimpleUseKey(null, null);
    assertNotNull(useKey, "SimpleUseKey should be constructable with nulls");
  }

  /**
   * Test key generation with both application and substance.
   */
  @Test
  public void testGetKeyWithBothValues() {
    SimpleUseKey useKey = new SimpleUseKey("test app", "test substance");
    String key = useKey.getKey();
    assertEquals("test app\ttest substance", key, "Key should combine app and substance with tab");
  }

  /**
   * Test key generation with null application.
   */
  @Test
  public void testGetKeyWithNullApplication() {
    SimpleUseKey useKey = new SimpleUseKey(null, "test substance");
    String key = useKey.getKey();
    assertEquals("-\ttest substance", key, "Key should use '-' for null application");
  }

  /**
   * Test key generation with null substance.
   */
  @Test
  public void testGetKeyWithNullSubstance() {
    SimpleUseKey useKey = new SimpleUseKey("test app", null);
    String key = useKey.getKey();
    assertEquals("test app\t-", key, "Key should use '-' for null substance");
  }

  /**
   * Test key generation with both null values.
   */
  @Test
  public void testGetKeyWithBothNull() {
    SimpleUseKey useKey = new SimpleUseKey(null, null);
    String key = useKey.getKey();
    assertEquals("-\t-", key, "Key should use '-' for both null values");
  }

  /**
   * Test that key is cached (lazy initialization).
   */
  @Test
  public void testKeyCaching() {
    SimpleUseKey useKey = new SimpleUseKey("test app", "test substance");
    String key1 = useKey.getKey();
    String key2 = useKey.getKey();
    assertSame(key1, key2, "Key should be cached and return same instance");
  }

  /**
   * Test that SimpleUseKey generates same key as equivalent Scope.
   */
  @Test
  public void testKeyCompatibilityWithScope() {
    String application = "test app";
    String substance = "test substance";

    SimpleUseKey useKey = new SimpleUseKey(application, substance);
    Scope scope = new Scope("stanza", application, substance);

    assertEquals(scope.getKey(), useKey.getKey(),
        "SimpleUseKey should generate same key as Scope for same app/substance");
  }

  /**
   * Test that SimpleUseKey generates same key as Scope with null values.
   */
  @Test
  public void testKeyCompatibilityWithScopeNulls() {
    SimpleUseKey useKey = new SimpleUseKey(null, null);
    Scope scope = new Scope("stanza", null, null);

    assertEquals(scope.getKey(), useKey.getKey(),
        "SimpleUseKey should generate same key as Scope for null values");
  }
}
