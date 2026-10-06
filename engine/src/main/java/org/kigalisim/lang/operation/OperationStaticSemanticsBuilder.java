/**
 * Builder for records describing semantics of an operation inferred from static analysis.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import java.util.Optional;


/**
 * Builder for an OperationStaticSemantics record.
 *
 * <p>Builder which allows operations to specify only the statically inferrable properties which
 * apply to them, leaving the rest empty.</p>
 */
public class OperationStaticSemanticsBuilder {

  private final OperationType operationType;
  private Optional<String> sourceStream;
  private Optional<String> destinationStream;
  private Optional<Boolean> assumePriorNew;

  /**
   * Create a new builder with all optional properties empty.
   *
   * @param operationType The type of operation for which the record is being built.
   */
  public OperationStaticSemanticsBuilder(OperationType operationType) {
    this.operationType = operationType;
    sourceStream = Optional.empty();
    destinationStream = Optional.empty();
    assumePriorNew = Optional.empty();
  }

  /**
   * Indicate that the operation both reads and modifies the given stream.
   *
   * @param stream The name of the stream used as both source and destination.
   * @return This builder for chaining.
   */
  public OperationStaticSemanticsBuilder setStream(String stream) {
    sourceStream = Optional.of(stream);
    destinationStream = Optional.of(stream);
    return this;
  }

  /**
   * Indicate the stream read by the operation.
   *
   * @param stream The name of the source stream.
   * @return This builder for chaining.
   */
  public OperationStaticSemanticsBuilder setSource(String stream) {
    sourceStream = Optional.of(stream);
    return this;
  }

  /**
   * Indicate the stream modified by the operation.
   *
   * @param stream The name of the destination stream.
   * @return This builder for chaining.
   */
  public OperationStaticSemanticsBuilder setDestination(String stream) {
    destinationStream = Optional.of(stream);
    return this;
  }

  /**
   * Indicate whether the operation treats prior equipment as new.
   *
   * @param newFlag True if the {@code assuming new} modifier is present and false otherwise.
   * @return This builder for chaining.
   */
  public OperationStaticSemanticsBuilder setAssumePriorNew(boolean newFlag) {
    assumePriorNew = Optional.of(newFlag);
    return this;
  }

  /**
   * Build the record from the properties specified so far.
   *
   * @return Newly constructed static semantics record.
   */
  public OperationStaticSemantics build() {
    return new OperationStaticSemantics(
        operationType,
        sourceStream,
        destinationStream,
        assumePriorNew
    );
  }

}
