/**
 * Record of a substance parsed from the source of a QubecTalk program.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.program;

import java.util.List;
import org.kigalisim.lang.operation.Operation;

/**
 * Record of a substance parsed from the source of a QubecTalk program.
 *
 * <p>Contains the operations defined for this substance.</p>
 */
public class ParsedSubstance {

  private final String name;
  private final List<Operation> operations;

  /**
   * Create a new record of a substance.
   *
   * @param name The name of the substance parsed.
   * @param operations The operations defined for this substance.
   */
  public ParsedSubstance(String name, List<Operation> operations) {
    this.name = name;
    this.operations = operations;
  }

  /**
   * Get the name of this substance.
   *
   * @return The name of this substance.
   */
  public String getName() {
    return name;
  }

  /**
   * Get the operations defined for this substance.
   *
   * @return Iterable over the operations defined for this substance.
   */
  public Iterable<Operation> getOperations() {
    return operations;
  }

}
