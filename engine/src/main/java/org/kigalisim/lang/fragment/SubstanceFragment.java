/**
 * Description of a fragment containing a parsed substance.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.fragment;

import org.kigalisim.lang.program.ParsedSubstance;

/**
 * A fragment containing a parsed substance.
 */
public class SubstanceFragment extends Fragment {

  private final ParsedSubstance substance;

  /**
   * Create a new fragment containing a parsed substance.
   *
   * @param substance The substance parsed from the source of this fragment.
   */
  public SubstanceFragment(ParsedSubstance substance) {
    this.substance = substance;
  }

  /**
   * Get the substance parsed from the source of this fragment.
   *
   * @return The parsed substance.
   */
  @Override
  public ParsedSubstance getSubstance() {
    return substance;
  }
}
