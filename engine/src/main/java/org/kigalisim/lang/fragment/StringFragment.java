/**
 * Structure describing a parsed string.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.fragment;


/**
 * A fragment containing only a string value.
 */
public class StringFragment extends Fragment {

  private final String value;

  /**
   * Create a new StringFragment.
   *
   * @param value The string value parsed from the source of this fragment.
   */
  public StringFragment(String value) {
    this.value = value;
  }

  @Override
  public String getString() {
    return value;
  }

}
