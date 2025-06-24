/**
 * Structure describing a parsed unit string.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.fragment;


/**
 * A fragment containing only a unit string.
 */
public class UnitFragment extends Fragment {

  private final String unit;

  /**
   * Create a new UnitFragment.
   *
   * @param unit The unit string parsed from the source of this fragment.
   */
  public UnitFragment(String unit) {
    this.unit = unit;
  }

  @Override
  public String getUnit() {
    return unit;
  }

}
