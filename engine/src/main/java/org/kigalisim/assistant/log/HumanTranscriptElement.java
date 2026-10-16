/**
 * A single element of a human readable chat transcript.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.log;

/**
 * One rendered element of a human readable transcript.
 *
 * <p>The exact content is deliberately left open pending the human transcript components, but each
 * element carries a stable identity and a markdown representation.</p>
 */
public class HumanTranscriptElement {

  private final String id;
  private final String markdown;

  /**
   * Constructs an element.
   *
   * @param id A unique identifier for the element.
   * @param markdown The markdown representation of the element.
   */
  public HumanTranscriptElement(String id, String markdown) {
    this.id = id;
    this.markdown = markdown;
  }

  /**
   * Gets the unique identifier for the element.
   *
   * @return The unique identifier.
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the markdown representation of the element.
   *
   * @return The markdown representation.
   */
  public String getMarkdown() {
    return markdown;
  }
}
