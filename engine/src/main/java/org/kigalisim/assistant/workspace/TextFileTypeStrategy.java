/**
 * Strategy for string-native file types.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.workspace;

import java.util.List;

/**
 * Describes how a string-native family of file extensions is interpreted.
 *
 * <p>These strategies back file types whose contents are text and can therefore be passed to the
 * language model without a binary extraction step. Binary file types are instead described by the
 * {@code BinaryFileTypeStrategy} sub-interface, which is introduced alongside the first concrete
 * binary type.</p>
 */
public interface TextFileTypeStrategy extends AssistantFileTypeStrategy {

  /**
   * Interprets the whole file for the language model.
   *
   * @param contents The string contents of the file.
   * @return The LLM-friendly contents.
   */
  String interpret(String contents);

  /**
   * Interprets a section of the file for the language model.
   *
   * @param contents The string contents of the file.
   * @param section The format-specific section to interpret.
   * @return The LLM-friendly contents limited to the section.
   * @throws java.util.NoSuchElementException If the section does not exist.
   */
  String interpret(String contents, String section);

  /**
   * Lists the sections which can be requested from the file.
   *
   * @param contents The string contents of the file.
   * @return The section names in source order.
   */
  List<String> outline(String contents);
}
