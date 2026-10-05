/**
 * Record describing semantics of an operation that can be inferred from static analysis.
 * 
 * @license BSD-3-Clause
 */

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
    this.operationType = operationType;
  }

  /**
   * Create a new record of statically inferred operation properties.
   * 
   * @param operationType The type of operation for which the record is provided.
   * @param sourceStream
   * @param destinationStream
   * @param assumePriorNew
   */
  public OperationStaticSemantics(OperationType operationType, Optional<String> sourceStream,
      Optional<String> destinationStream, Optional<Boolean> assumePriorNew) {
    this.operationType = operationType;
    this.sourceStream = sourceStream;
    this.destinationStream = destinationStream;
    this.assumePriorNew = assumePriorNew;
  }

  public OperationType getOperationType() {
    return operationType;
  }

  public Optional<String> getSourceStream() {
    return sourceStream;
  }

  public Optional<String> getDestinationStream() {
    return destinationStream;
  }

  public Optional<Boolean> getAssumePriorNew() {
    return assumePriorNew;
  }

}