/**
 * Unit tests for the SystemContextProvider class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Tests for the SystemContextProvider class.
 */
public class SystemContextProviderTest {

  private static final String PREAMBLE_PATH = "/org/kigalisim/assistant/system_context_preamble.md";
  private static final String DOCS_PATH = "/org/kigalisim/assistant/llms-full.txt";

  /**
   * Reads a classpath resource as UTF-8 text for test assertions.
   *
   * @param path The absolute classpath path to read.
   * @return The resource contents.
   * @throws IOException If the resource cannot be read.
   */
  private String readResource(String path) throws IOException {
    try (InputStream stream = SystemContextProviderTest.class.getResourceAsStream(path)) {
      assertNotNull(stream, "Test resource should be present: " + path);
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /**
   * Test that the committed preamble states the conversation protocol and hard constraints.
   *
   * @throws IOException If the preamble cannot be read.
   */
  @Test
  public void testPreambleDescribesProtocolAndConstraints() throws IOException {
    String preamble = readResource(PREAMBLE_PATH);
    assertFalse(preamble.isEmpty(), "Preamble should not be empty");
    assertTrue(preamble.contains("message_user"), "Preamble should mention message_user");
    assertTrue(preamble.contains("end_turn"), "Preamble should mention end_turn");

    String lower = preamble.toLowerCase();
    assertTrue(lower.contains("network"), "Preamble should mention network access");
    assertTrue(lower.contains("internet"), "Preamble should mention internet access");
    assertTrue(
        lower.contains("cannot") || lower.contains("no network"),
        "Preamble should state that network and internet access are unavailable");
  }

  /**
   * Test that the bundled documentation is present and carries the canonical content.
   *
   * @throws IOException If the documentation cannot be read.
   */
  @Test
  public void testBundledDocumentation() throws IOException {
    String docs = readResource(DOCS_PATH);
    assertFalse(docs.isEmpty(), "Bundled llms-full.txt should not be empty");
    assertTrue(
        docs.startsWith("# Kigali Sim - Montreal Protocol Policy Simulation Tool"),
        "Bundled llms-full.txt should start with the expected Kigali Sim heading");
    assertTrue(docs.contains("QubecTalk"), "Bundled llms-full.txt should mention QubecTalk");
    assertTrue(docs.contains("start default"), "Bundled llms-full.txt should contain QubecTalk code");
  }

  /**
   * Test that the assembled context places the preamble before the documentation.
   *
   * @throws IOException If the resources cannot be read.
   */
  @Test
  public void testGetSystemContextJoinsPartsInOrder() throws IOException {
    String preamble = readResource(PREAMBLE_PATH);
    String docs = readResource(DOCS_PATH);

    String context = new SystemContextProvider().getSystemContext();
    assertFalse(context.isEmpty(), "System context should not be empty");
    assertTrue(context.contains(preamble), "System context should contain the full preamble");
    assertTrue(context.contains(docs), "System context should contain the full documentation");
    assertTrue(
        context.indexOf(preamble) < context.indexOf(docs),
        "System context should place the preamble before the documentation");
    assertTrue(
        context.contains(preamble + "\n\n---\n\n" + docs),
        "System context should join the two parts with the expected separator");
  }

  /**
   * Test that the assembled context is cached after the first call.
   */
  @Test
  public void testGetSystemContextCachesResult() {
    SystemContextProvider provider = new SystemContextProvider();
    String first = provider.getSystemContext();
    String second = provider.getSystemContext();
    assertSame(first, second, "System context should be cached between calls");
  }

  /**
   * Test that a missing resource causes the provider to fail fast.
   */
  @Test
  public void testMissingResourceFailsFast() {
    SystemContextProvider provider = new SystemContextProvider(
        "/org/kigalisim/assistant/does-not-exist.md", DOCS_PATH);
    IllegalStateException exception =
        assertThrows(IllegalStateException.class, provider::getSystemContext);
    assertTrue(
        exception.getMessage().contains("does-not-exist.md"),
        "Exception message should identify the missing resource");
  }

  /**
   * Test that a missing documentation resource also fails fast.
   */
  @Test
  public void testMissingDocumentationFailsFast() {
    SystemContextProvider provider = new SystemContextProvider(
        PREAMBLE_PATH, "/org/kigalisim/assistant/does-not-exist.txt");
    IllegalStateException exception =
        assertThrows(IllegalStateException.class, provider::getSystemContext);
    assertTrue(
        exception.getMessage().contains("does-not-exist.txt"),
        "Exception message should identify the missing documentation resource");
  }
}
