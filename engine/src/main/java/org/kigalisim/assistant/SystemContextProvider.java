/**
 * Provider of the assembled system context for the Kigali Sim AI assistant.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Loads and caches the system context supplied to the language model.
 *
 * <p>The context combines the committed harness preamble with the canonical Kigali Sim
 * documentation ({@code llms-full.txt}). The documentation is bundled into the jar at build
 * time from the repository root so that the root file remains the single source of truth and
 * no duplicate copy of it is committed.</p>
 */
public class SystemContextProvider {

  private static final String DEFAULT_PREAMBLE_PATH =
      "/org/kigalisim/assistant/system_context_preamble.md";
  private static final String DEFAULT_LLMS_FULL_PATH = "/org/kigalisim/assistant/llms-full.txt";
  private static final String SEPARATOR = "\n\n---\n\n";

  private final String preamblePath;
  private final String llmsFullPath;
  private String systemContext;

  /**
   * Constructs a provider that reads the packaged preamble and documentation resources.
   */
  public SystemContextProvider() {
    this(DEFAULT_PREAMBLE_PATH, DEFAULT_LLMS_FULL_PATH);
  }

  /**
   * Constructs a provider that reads the given classpath resources.
   *
   * <p>This constructor is intended for testing the missing-resource behavior.</p>
   *
   * @param preamblePath The classpath path to the preamble resource.
   * @param llmsFullPath The classpath path to the {@code llms-full.txt} resource.
   */
  SystemContextProvider(String preamblePath, String llmsFullPath) {
    this.preamblePath = Objects.requireNonNull(preamblePath, "preamblePath");
    this.llmsFullPath = Objects.requireNonNull(llmsFullPath, "llmsFullPath");
  }

  /**
   * Gets the assembled system context, reading and caching it on first use.
   *
   * <p>The returned value places the harness preamble before the Kigali Sim documentation,
   * separated by a horizontal rule.</p>
   *
   * @return The preamble and the {@code llms-full.txt} documentation joined together.
   * @throws IllegalStateException If either required resource is missing or cannot be read.
   */
  public String getSystemContext() {
    if (systemContext == null) {
      String preamble = readResource(preamblePath);
      String llmsFull = readResource(llmsFullPath);
      systemContext = preamble + SEPARATOR + llmsFull;
      return systemContext;
    } else {
      return systemContext;
    }
  }

  /**
   * Reads a classpath resource as UTF-8 text.
   *
   * @param path The absolute classpath path to the resource.
   * @return The resource contents decoded as UTF-8.
   * @throws IllegalStateException If the resource is absent or cannot be read.
   */
  private String readResource(String path) {
    try (InputStream stream = SystemContextProvider.class.getResourceAsStream(path)) {
      if (stream == null) {
        throw new IllegalStateException(
            "Required system context resource not found on classpath: " + path);
      } else {
        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read system context resource: " + path, e);
    }
  }
}
