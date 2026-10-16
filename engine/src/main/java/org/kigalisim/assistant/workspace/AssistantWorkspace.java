/**
 * Abstraction over a flat collection of assistant files.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.workspace;

import java.util.List;

/**
 * Persistence and storage for a flat collection of files.
 *
 * <p>A workspace holds simulation files and supporting documents. Directories and paths are not
 * supported. Missing files or sections are reported with
 * {@link java.util.NoSuchElementException} while conflicting names are reported with
 * {@link IllegalArgumentException}.</p>
 */
public interface AssistantWorkspace {

  /**
   * Creates a new empty file.
   *
   * @param filename The name of the file to create.
   * @return The canonical filename.
   * @throws IllegalArgumentException If the file already exists.
   */
  String createFile(String filename);

  /**
   * Creates a new file with string contents.
   *
   * @param filename The name of the file to create.
   * @param contents The text contents of the file.
   * @return The canonical filename.
   * @throws IllegalArgumentException If the file already exists.
   */
  String createFile(String filename, String contents);

  /**
   * Creates a new file with binary contents.
   *
   * @param filename The name of the file to create.
   * @param contents The binary contents of the file.
   * @return The canonical filename.
   * @throws IllegalArgumentException If the file already exists.
   */
  String createFile(String filename, byte[] contents);

  /**
   * Reads a file in its entirety as a string.
   *
   * @param filename The name of the file to read.
   * @return The file contents.
   * @throws java.util.NoSuchElementException If the file does not exist.
   */
  String readFile(String filename);

  /**
   * Reads a part of a file as described by section.
   *
   * @param filename The name of the file to read.
   * @param section The format-specific section to read.
   * @return The file contents limited to the section.
   * @throws java.util.NoSuchElementException If the file or section does not exist.
   */
  String readFile(String filename, String section);

  /**
   * Lists the files in the workspace.
   *
   * @return The filenames in insertion order.
   */
  List<String> listFiles();

  /**
   * Lists the sections which can be requested from a file.
   *
   * @param filename The name of the file to outline.
   * @return The format-specific section names in source order.
   * @throws java.util.NoSuchElementException If the file does not exist.
   */
  List<String> outlineFile(String filename);

  /**
   * Writes a file in its entirety, overwriting prior contents.
   *
   * @param filename The name of the file to write.
   * @param contents The replacement contents.
   * @return The canonical filename.
   * @throws java.util.NoSuchElementException If the file does not exist.
   */
  String writeFile(String filename, String contents);

  /**
   * Removes a file.
   *
   * @param filename The name of the file to remove.
   * @throws java.util.NoSuchElementException If the file does not exist.
   */
  void deleteFile(String filename);
}
