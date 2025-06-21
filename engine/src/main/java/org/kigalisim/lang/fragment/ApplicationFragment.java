/**
 * Description of a fragment containing a parsed application.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.fragment;

import org.kigalisim.lang.program.ParsedApplication;

/**
 * A fragment containing a parsed application.
 */
public class ApplicationFragment extends Fragment {

  private final ParsedApplication application;

  /**
   * Create a new fragment containing a parsed application.
   *
   * @param application The application parsed from the source of this fragment.
   */
  public ApplicationFragment(ParsedApplication application) {
    this.application = application;
  }

  /**
   * Get the application parsed from the source of this fragment.
   *
   * @return The parsed application.
   */
  @Override
  public ParsedApplication getApplication() {
    return application;
  }
}
