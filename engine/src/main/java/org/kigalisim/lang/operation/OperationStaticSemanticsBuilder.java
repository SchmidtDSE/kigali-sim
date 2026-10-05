/**
 * Record describing semantics of an operation that can be inferred from static analysis.
 * 
 * @license BSD-3-Clause
 */

import java.util.Optional;


/**
 * Record describing common properties of an operation which can be inferred from static analysis.
 */
public class OperationStaticSemanticsBuilder {

  private final OperationType operationType;
  private Optional<String> sourceStream;
  private Optional<String> destinationStream;
  private Optional<Boolean> assumePriorNew;

  public OperationStaticSemanticsBuilder(OperationType operationType) {
    this.operationType = operationType;
    sourceStream = Optional.empty();
    destinationStream = Optional.empty();
    assumePriorNew = Optional.empty();
  }

  public OperationStaticSemanticsBuilder setStream(String stream) {
    sourceStream = Optional.of(stream);
    destinationStream = Optional.of(stream);
    return this;
  }

  public OperationStaticSemanticsBuilder setSource(String stream) {
    sourceStream = Optional.of(stream);
    return this;
  }

  public OperationStaticSemanticsBuilder setDestination(String stream) {
    destinationStream = Optional.of(stream);
    return this;
  }

  public OperationStaticSemanticsBuilder setAssumePriorNew(boolean newFlag) {
    assumePriorNew = Optional.of(newFlag);
    return this;
  }

  public OperationStaticSemantics build() {
    return new OperationStaticSemantics(
        operationType,
        sourceStream,
        destinationStream,
        assumePriorNew
    );
  }

}