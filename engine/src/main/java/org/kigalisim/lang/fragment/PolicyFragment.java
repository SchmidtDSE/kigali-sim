/**
 * Description of a fragment containing a parsed policy.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.fragment;

import org.kigalisim.lang.program.ParsedPolicy;

/**
 * A fragment containing a parsed policy.
 */
public class PolicyFragment extends Fragment {

  private final ParsedPolicy policy;

  /**
   * Create a new fragment containing a parsed policy.
   *
   * @param policy The policy parsed from the source of this fragment.
   */
  public PolicyFragment(ParsedPolicy policy) {
    this.policy = policy;
  }

  @Override
  public ParsedPolicy getPolicy() {
    return policy;
  }

  @Override
  public boolean getIsStanzaScenarios() {
    return false;
  }
}
