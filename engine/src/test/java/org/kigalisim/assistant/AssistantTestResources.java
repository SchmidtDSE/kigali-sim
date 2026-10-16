/**
 * Reads fixtures bundled with the assistant tests.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Helper for reading test fixtures from the classpath.
 */
public final class AssistantTestResources {

  private static final String QTA_DIR = "/org/kigalisim/assistant/qta/";

  private AssistantTestResources() {
    // Utility class - prevent instantiation
  }

  /**
   * Reads a classpath resource as UTF-8 text.
   *
   * @param path The absolute classpath path to read.
   * @return The resource contents.
   */
  public static String read(String path) {
    try (InputStream stream = AssistantTestResources.class.getResourceAsStream(path)) {
      if (stream == null) {
        throw new AssertionError("Test resource should be present: " + path);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new AssertionError("Failed to read test resource: " + path, e);
    }
  }

  /**
   * Reads a QubecTalk fixture from the shared assistant fixture directory.
   *
   * <p>Trailing whitespace is stripped so that fixtures can carry a conventional final newline
   * without changing the exact strings tests assert against.</p>
   *
   * @param name The fixture file name such as {@code basic.qta}.
   * @return The fixture contents without trailing whitespace.
   */
  public static String readQta(String name) {
    return read(QTA_DIR + name).stripTrailing();
  }
}
