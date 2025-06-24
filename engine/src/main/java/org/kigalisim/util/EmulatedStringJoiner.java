/**
 * StringJoiner for WASM.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.util;

/**
 * A WebAssembly-compatible implementation of StringJoiner.
 *
 * <p>This implementation provides string joining functionality for WebAssembly environments where
 * Java's StringJoiner is not available.</p>
 */
public class EmulatedStringJoiner {

  private final String delimiter;
  private boolean isFirst;
  private final StringBuilder value;

  /**
   * Constructs a new EmulatedStringJoiner with the specified delimiter.
   *
   * @param delimiter The delimiter to be used between joined strings
   */
  public EmulatedStringJoiner(CharSequence delimiter) {
    this.delimiter = delimiter.toString();
    this.value = new StringBuilder();
    this.isFirst = true;
  }

  /**
   * Adds a new element to the string sequence.
   *
   * @param newElement The element to add
   * @return This EmulatedStringJoiner instance
   */
  public EmulatedStringJoiner add(CharSequence newElement) {
    String elementStr = newElement == null ? "null" : newElement.toString();
    if (isFirst) {
      value.append(elementStr);
      isFirst = false;
    } else {
      value.append(delimiter).append(elementStr);
    }

    return this;
  }

  /**
   * Returns the string representation of this joiner.
   *
   * @return The joined string
   */
  @Override
  public String toString() {
    return value.toString();
  }
}
