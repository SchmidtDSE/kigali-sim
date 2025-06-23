/**
 * Description of a fragment containing an about stanza.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.fragment;

/**
 * A fragment representing an about stanza.
 */
public class AboutStanzaFragment extends Fragment {

  /**
   * Create a new fragment for an about stanza.
   */
  public AboutStanzaFragment() {
    // No arguments needed for constructor
  }

  @Override
  public boolean getIsStanzaScenarios() {
    return false;
  }

  @Override
  public boolean getIsStanzaPolicyOrDefault() {
    return false;
  }
}