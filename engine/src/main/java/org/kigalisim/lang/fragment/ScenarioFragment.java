/**
 * Description of a fragment containing a parsed scenario.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.fragment;

import org.kigalisim.lang.program.ParsedScenario;

/**
 * A fragment containing a parsed scenario.
 */
public class ScenarioFragment extends Fragment {

  private final ParsedScenario scenario;

  /**
   * Create a new fragment containing a parsed scenario.
   *
   * @param scenario The scenario parsed from the source of this fragment.
   */
  public ScenarioFragment(ParsedScenario scenario) {
    this.scenario = scenario;
  }

  /**
   * Get the scenario parsed from the source of this fragment.
   *
   * @return The parsed scenario.
   */
  @Override
  public ParsedScenario getScenario() {
    return scenario;
  }
}
