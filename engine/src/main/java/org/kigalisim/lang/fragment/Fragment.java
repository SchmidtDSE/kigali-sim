/**
 * Interface for a piece of interpreted QubecTalk code.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.fragment;

import org.kigalisim.lang.operation.Operation;
import org.kigalisim.lang.time.ParsedDuring;


/**
 * Description of a part of a parsed QubecTalk program.
 *
 * <p>Description of a part of a parsed QubecTalk program that provides a type which can be used to
 * facilitate the ANTLR visitor.</p>
 */
public abstract class Fragment {

  /**
   * Get the calculation parsed from the source of this fragment.
   *
   * @return Operation which can be resolved to an EngineNumber at runtime.
   */
  public Operation getOperation() {
    throw new RuntimeException("This fragment does not have a calculation.");
  }

  /**
   * Get the units that should be applied to the result of a calculation or to an EngineNumber.
   *
   * @return Parsed units as a string.
   */
  public String getUnit() {
    throw new RuntimeException("This fragment does not have units.");
  }

  /**
   * Get the during that should be applied in filtering if an operation should be applied.
   *
   * @return Parsed during.
   */
  public ParsedDuring getDuring() {
    throw new RuntimeException("This fragment does not have a during.");
  }

}
