/**
 * Description of a fragment containing a parsed program.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.fragment;

import org.kigalisim.lang.program.ParsedProgram;

/**
 * A fragment containing a parsed program.
 */
public class ProgramFragment extends Fragment {

  private final ParsedProgram program;

  /**
   * Create a new fragment containing a parsed program.
   *
   * @param program The program parsed from the source of this fragment.
   */
  public ProgramFragment(ParsedProgram program) {
    this.program = program;
  }

  /**
   * Get the program parsed from the source of this fragment.
   *
   * @return The parsed program.
   */
  @Override
  public ParsedProgram getProgram() {
    return program;
  }
}
