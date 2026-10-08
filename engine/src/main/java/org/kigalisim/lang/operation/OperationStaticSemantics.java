/**
 * Record describing semantics of an operation that can be inferred from static analysis.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;


/**
 * Record describing common properties of an operation which can be inferred from static analysis.
 *
 * <p>Record describing common properties of an operation which can be inferred from static
 * analysis, intended for use in polymorphic program static analysis for QubecTalk scripts.</p>
 */
public class OperationStaticSemantics {

  private final OperationType operationType;
  private final Set<String> streams;
  private final Optional<Boolean> assumePriorNew;

  /**
   * Create a record indicating that only the operation type could be inferred statically.
   *
   * @param operationType The type of operation for which the record is provided.
   */
  public OperationStaticSemantics(OperationType operationType) {
    this(operationType, Set.of(), Optional.empty());
  }

  /**
   * Create a record for an operation which involves a single stream.
   *
   * @param operationType The type of operation for which the record is provided.
   * @param stream The name of the stream involved in the operation.
   */
  public OperationStaticSemantics(OperationType operationType, String stream) {
    this(operationType, Set.of(stream), Optional.empty());
  }

  /**
   * Create a new record of statically inferred operation properties.
   *
   * @param operationType The type of operation for which the record is provided.
   * @param streams The names of the streams included in the operation, copied on construction.
   * @param assumePriorNew Whether the operation treats prior equipment as a pseudo-cohort of
   *     typical age (the {@code assuming new} modifier) or empty if not applicable.
   */
  public OperationStaticSemantics(OperationType operationType, Set<String> streams,
      Optional<Boolean> assumePriorNew) {
    this.operationType = operationType;
    this.streams = Collections.unmodifiableSet(new LinkedHashSet<>(streams));
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
   * Get the streams included in the operation.
   *
   * @return Unmodifiable set of names of the streams involved, empty if none apply.
   */
  public Set<String> getStreams() {
    return streams;
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
