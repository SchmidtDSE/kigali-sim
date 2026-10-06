/**
 * Record describing semantics of an operation that can be inferred from static analysis.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import java.util.Optional;


/**
 * Record describing common properties of an operation which can be inferred from static analysis.
 *
 * <p>Record describing common properties of an operation which can be inferred from static
 * analysis, intended for use in polymorphic program static analysis for QubecTalk scripts.</p>
 */
public class OperationStaticSemantics {

  private final OperationType operationType;
  private final Optional<String> sourceStream;
  private final Optional<String> destinationStream;
  private final Optional<Boolean> assumePriorNew;

  /**
   * Create a record indicating that only the operation type could be inferred statically.
   *
   * @param operationType The type of operation for which the record is provided.
   */
  public OperationStaticSemantics(OperationType operationType) {
    this(operationType, Optional.empty(), Optional.empty(), Optional.empty());
  }

  /**
   * Create a new record of statically inferred operation properties.
   *
   * @param operationType The type of operation for which the record is provided.
   * @param sourceStream The name of the stream read by the operation or empty if not applicable.
   * @param destinationStream The name of the stream modified by the operation or empty if not
   *     applicable.
   * @param assumePriorNew Whether the operation treats prior equipment as a pseudo-cohort of
   *     typical age (the {@code assuming new} modifier) or empty if not applicable.
   */
  public OperationStaticSemantics(OperationType operationType, Optional<String> sourceStream,
      Optional<String> destinationStream, Optional<Boolean> assumePriorNew) {
    this.operationType = operationType;
    this.sourceStream = sourceStream;
    this.destinationStream = destinationStream;
    this.assumePriorNew = assumePriorNew;
  }

  /**
   * Get the kind of operation described.
   *
   * @return The type of the operation.
   */
  public OperationType getOperationType() {
    return operationType;
  }

  /**
   * Get the stream read by the operation.
   *
   * @return The name of the stream read or empty if not applicable.
   */
  public Optional<String> getSourceStream() {
    return sourceStream;
  }

  /**
   * Get the stream modified by the operation.
   *
   * @return The name of the stream modified or empty if not applicable.
   */
  public Optional<String> getDestinationStream() {
    return destinationStream;
  }

  /**
   * Determine if the operation treats prior equipment as new (the {@code assuming new} modifier).
   *
   * @return True if assuming new, false if not, or empty if not applicable to the operation.
   */
  public Optional<Boolean> getAssumePriorNew() {
    return assumePriorNew;
  }

}
