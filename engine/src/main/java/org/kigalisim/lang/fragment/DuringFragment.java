/**
 * Description of a fragment containing a parsed during expression.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.fragment;

import org.kigalisim.lang.time.ParsedDuring;


/**
 * A fragment containing a parsed during expression.
 *
 * <p>A fragment containing a parsed during expression which can be used to manipulate when
 * operations are applied.</p>
 */
public class DuringFragment extends Fragment {

  private final ParsedDuring during;

  /**
   * Create a new DuringFragment.
   *
   * @param during The parsed during expression parsed from the source of this fragment.
   */
  public DuringFragment(ParsedDuring during) {
    this.during = during;
  }

  @Override
  public ParsedDuring getDuring() {
    return during;
  }

}
