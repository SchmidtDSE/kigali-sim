/**
 * In-memory assistant workspace.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.workspace;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A flat workspace which keeps files in memory.
 *
 * <p>Files are stored as byte arrays in insertion order and dispatched to a registered
 * {@link AssistantFileTypeStrategy} by extension. This is the default persistence mechanism and is
 * safe under TeaVM / WASM as well as the JVM.</p>
 */
public class InMemoryWorkspace implements AssistantWorkspace {

  private final Map<String, StoredFile> files = new LinkedHashMap<>();
  private final Map<String, AssistantFileTypeStrategy> strategies = new LinkedHashMap<>();

  /**
   * Constructs an empty workspace over the given file type strategies.
   *
   * @param fileTypeStrategies The strategies which describe supported file extensions.
   * @throws IllegalArgumentException If two strategies claim the same extension.
   */
  public InMemoryWorkspace(Iterable<AssistantFileTypeStrategy> fileTypeStrategies) {
    Objects.requireNonNull(fileTypeStrategies, "fileTypeStrategies");
    for (AssistantFileTypeStrategy strategy : fileTypeStrategies) {
      Objects.requireNonNull(strategy, "strategy");
      for (String extension : strategy.getFileExtensions()) {
        String key = normalizeExtension(extension);
        if (strategies.containsKey(key)) {
          throw new IllegalArgumentException(
              "Duplicate file type strategy for extension '" + extension + "'");
        }
        strategies.put(key, strategy);
      }
    }
  }

  @Override
  public List<String> listFiles() {
    return new ArrayList<>(files.keySet());
  }

  @Override
  public String createFile(String filename) {
    return createFile(filename, new byte[0]);
  }

  @Override
  public String createFile(String filename, String contents) {
    Objects.requireNonNull(contents, "contents");
    return createFile(filename, contents.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public String createFile(String filename, byte[] contents) {
    Objects.requireNonNull(filename, "filename");
    Objects.requireNonNull(contents, "contents");
    if (files.containsKey(filename)) {
      throw new IllegalArgumentException("A file named '" + filename + "' already exists");
    }
    requireStrategy(filename);
    files.put(filename, new StoredFile(Arrays.copyOf(contents, contents.length)));
    return filename;
  }

  @Override
  public String readFile(String filename) {
    StoredFile file = requireFile(filename);
    return requireTextStrategy(filename).interpret(decode(file));
  }

  @Override
  public String readFile(String filename, String section) {
    StoredFile file = requireFile(filename);
    TextFileTypeStrategy strategy = requireTextStrategy(filename);
    try {
      return strategy.interpret(decode(file), section);
    } catch (NoSuchElementException e) {
      throw new NoSuchElementException(
          "No section '" + section + "' in file '" + filename + "'");
    }
  }

  @Override
  public List<String> outlineFile(String filename) {
    StoredFile file = requireFile(filename);
    return requireTextStrategy(filename).outline(decode(file));
  }

  @Override
  public String writeFile(String filename, String contents) {
    Objects.requireNonNull(contents, "contents");
    requireFile(filename);
    files.put(filename, new StoredFile(contents.getBytes(StandardCharsets.UTF_8)));
    return filename;
  }

  @Override
  public void deleteFile(String filename) {
    requireFile(filename);
    files.remove(filename);
  }

  /**
   * Resolves a stored file or throws.
   *
   * @param filename The name of the file.
   * @return The stored file.
   * @throws NoSuchElementException If the file does not exist.
   */
  private StoredFile requireFile(String filename) {
    Objects.requireNonNull(filename, "filename");
    StoredFile file = files.get(filename);
    if (file == null) {
      throw new NoSuchElementException("No file named '" + filename + "'");
    }
    return file;
  }

  /**
   * Resolves the strategy for a filename or throws.
   *
   * @param filename The name of the file.
   * @return The strategy registered for the filename's extension.
   * @throws IllegalArgumentException If no strategy supports the extension.
   */
  private AssistantFileTypeStrategy requireStrategy(String filename) {
    AssistantFileTypeStrategy strategy = strategies.get(getExtension(filename));
    if (strategy == null) {
      throw new IllegalArgumentException(
          "Unsupported file extension for '" + filename + "'");
    }
    return strategy;
  }

  /**
   * Resolves the text strategy for a filename or throws.
   *
   * @param filename The name of the file.
   * @return The text strategy registered for the filename's extension.
   * @throws IllegalArgumentException If no strategy supports the extension.
   * @throws UnsupportedOperationException If the extension is backed by a binary strategy.
   */
  private TextFileTypeStrategy requireTextStrategy(String filename) {
    AssistantFileTypeStrategy strategy = requireStrategy(filename);
    if (strategy instanceof TextFileTypeStrategy textStrategy) {
      return textStrategy;
    }
    throw new UnsupportedOperationException(
        "File '" + filename + "' is not a string-native file type");
  }

  /**
   * Decodes stored bytes as UTF-8 text.
   *
   * @param file The stored file.
   * @return The decoded contents.
   */
  private String decode(StoredFile file) {
    return new String(file.getContents(), StandardCharsets.UTF_8);
  }

  /**
   * Gets the lower-case extension of a filename.
   *
   * @param filename The name of the file.
   * @return The extension including the leading dot, or an empty string when there is none.
   */
  private String getExtension(String filename) {
    int dot = filename.lastIndexOf('.');
    if (dot < 0) {
      return "";
    }
    return normalizeExtension(filename.substring(dot));
  }

  /**
   * Normalizes a file extension for use as a map key.
   *
   * @param extension The extension such as {@code .qta}.
   * @return The lower-case extension.
   */
  private String normalizeExtension(String extension) {
    return extension.toLowerCase(Locale.ROOT);
  }

  /**
   * A file held in memory as bytes.
   */
  private static final class StoredFile {

    private final byte[] contents;

    /**
     * Constructs a stored file.
     *
     * @param contents The file contents.
     */
    StoredFile(byte[] contents) {
      this.contents = contents;
    }

    /**
     * Gets a defensive copy of the contents.
     *
     * @return The file contents.
     */
    byte[] getContents() {
      return Arrays.copyOf(contents, contents.length);
    }
  }
}
