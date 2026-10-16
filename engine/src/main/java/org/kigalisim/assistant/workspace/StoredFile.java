/**
 * A file held in memory as bytes.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.workspace;

import java.util.Arrays;

/**
 * Immutable byte storage for a single workspace file.
 *
 * <p>Contents are copied on the way in and on the way out so callers cannot mutate the stored
 * bytes.</p>
 */
final class StoredFile {

  private final byte[] contents;

  /**
   * Constructs a stored file.
   *
   * @param contents The file contents.
   */
  StoredFile(byte[] contents) {
    this.contents = Arrays.copyOf(contents, contents.length);
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
