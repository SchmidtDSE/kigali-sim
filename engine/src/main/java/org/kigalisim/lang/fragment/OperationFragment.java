/**
 * Description of a fragment containing only a calculation.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.fragment;

import org.kigalisim.lang.operation.Operation;


/**
 * A fragment containing only a calculation.
 */
public class OperationFragment extends Fragment {

  private final Operation operation;

  /**
   * Create a new fragment containing an operation.
   *
   * @param operation The calculation parsed from the source of this fragment.
   */
  public OperationFragment(Operation operation) {
    this.operation = operation;
  }

  /**
   * Get the operation parsed from the source of this fragment.
   *
   * @return Operation which can be resolved to an EngineNumber at runtime.
   */
  @Override
  public Operation getOperation() {
    return operation;
  }

}
