/**
 * Preprocessor that expands pipe characters to spaces in QubecTalk scripts sent via URL.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.cloud;

/**
 * Expands {@code |} pipe characters back to spaces in a QubecTalk script string.
 *
 * <p>When QubecTalk scripts are transmitted as URL query parameters, each space is
 * URL-encoded as {@code %20} (three bytes instead of one), which can significantly
 * reduce the size of scripts that fit within practical URL length constraints. Because
 * the pipe character ({@code |}) does not appear in the QubecTalk grammar, it is safe
 * to use as a compact stand-in for a space. This preprocessor transparently converts
 * every {@code |} back to a space before the script reaches the parser.</p>
 */
public class CloudQubectalkPreprocessor {

  /**
   * Constructs a new CloudQubectalkPreprocessor.
   */
  public CloudQubectalkPreprocessor() {
  }

  /**
   * Replaces all pipe ({@code |}) characters in the given script with space characters.
   *
   * <p>The substitution is safe because {@code |} has no role in the QubecTalk syntax,
   * so replacing it with a space does not alter the meaning of any valid script. The
   * operation is idempotent for scripts that contain no pipe characters: they are
   * returned unchanged. This transformation must be applied before the script is passed
   * to the parser so that the parser receives well-formed QubecTalk input.</p>
   *
   * @param script The raw script string as received from the URL query parameter.
   * @return A new string with every {@code |} character replaced by a space.
   */
  public String preprocess(String script) {
    return script.replace('|', ' ');
  }

}
