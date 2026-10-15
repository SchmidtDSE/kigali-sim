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
 *
 * @param id A unique identifier for the element.
 * @param markdown The markdown representation of the element.
 */
public record HumanTranscriptElement(String id, String markdown) {
}
