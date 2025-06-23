/**
 * Description of a fragment containing parsed scenarios.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.fragment;

import org.kigalisim.lang.program.ParsedScenarios;

/**
 * A fragment containing parsed scenarios.
 */
public class ScenariosFragment extends Fragment {

  private final ParsedScenarios scenarios;

  /**
   * Create a new fragment containing parsed scenarios.
   *
   * @param scenarios The scenarios parsed from the source of this fragment.
   */
  public ScenariosFragment(ParsedScenarios scenarios) {
    this.scenarios = scenarios;
  }

  @Override
  public ParsedScenarios getScenarios() {
    return scenarios;
  }

  @Override
  public boolean getIsStanzaScenarios() {
    return true;
  }
}
