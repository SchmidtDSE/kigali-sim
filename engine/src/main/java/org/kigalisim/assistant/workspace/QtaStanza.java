/**
 * Metadata describing a top-level QubecTalk stanza.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.workspace;

/**
 * The keyword, optional policy name, and inclusive line range of a top-level stanza.
 */
final class QtaStanza {

  private final String keyword;
  private final String name;
  private final int startLine;
  private final int endLine;

  /**
   * Constructs a stanza.
   *
   * @param keyword The stanza keyword such as {@code default}.
   * @param name The policy name when applicable, otherwise null.
   * @param startLine The zero-based index of the opening line.
   * @param endLine The zero-based index of the closing line, inclusive.
   */
  QtaStanza(String keyword, String name, int startLine, int endLine) {
    this.keyword = keyword;
    this.name = name;
    this.startLine = startLine;
    this.endLine = endLine;
  }

  /**
   * Gets the stanza keyword.
   *
   * @return The keyword such as {@code default}.
   */
  String getKeyword() {
    return keyword;
  }

  /**
   * Gets the policy name when applicable.
   *
   * @return The quoted policy name, or null for non-policy stanzas.
   */
  String getName() {
    return name;
  }

  /**
   * Gets the zero-based index of the opening line.
   *
   * @return The opening line index.
   */
  int getStartLine() {
    return startLine;
  }

  /**
   * Gets the zero-based index of the closing line, inclusive.
   *
   * @return The closing line index.
   */
  int getEndLine() {
    return endLine;
  }
}
