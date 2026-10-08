/**
 * Builder for records describing semantics of an operation inferred from static analysis.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;


/**
 * Builder for an OperationStaticSemantics record.
 *
 * <p>Builder which allows operations to specify only the statically inferrable properties which
 * apply to them, leaving the rest empty.</p>
 */
public class OperationStaticSemanticsBuilder {

  private final OperationType operationType;
  private final Set<String> streams;
  private Optional<Boolean> assumePriorNew;

  /**
   * Create a new builder with all optional properties empty.
   *
   * @param operationType The type of operation for which the record is being built.
   */
  public OperationStaticSemanticsBuilder(OperationType operationType) {
    this.operationType = operationType;
    streams = new LinkedHashSet<>();
    assumePriorNew = Optional.empty();
  }

  /**
   * Indicate that a stream is included in the operation.
   *
   * @param stream The name of the stream involved in the operation.
   * @return This builder for chaining.
   */
  public OperationStaticSemanticsBuilder addStream(String stream) {
    streams.add(stream);
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
        streams,
        assumePriorNew
    );
  }

}
