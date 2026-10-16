/**
 * Strategy for working with a family of file extensions.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.workspace;

import java.util.Set;

/**
 * Describes how a family of file extensions is interpreted for the assistant.
 *
 * <p>Some file types are binary but must present as text to the language model. Those types are
 * read only and are described by the {@code BinaryFileTypeStrategy} sub-interface, which is
 * introduced alongside the first concrete binary file type.</p>
 */
public interface AssistantFileTypeStrategy {

  /**
   * Gets the file extensions supported by this strategy.
   *
   * @return The supported extensions, such as {@code .pdf}.
   */
  Set<String> getFileExtensions();

  /**
   * Indicates whether the underlying file type is native string content.
   *
   * @return True when the file type is string / text based and false when binary.
   */
  boolean getIsStringNative();
}
