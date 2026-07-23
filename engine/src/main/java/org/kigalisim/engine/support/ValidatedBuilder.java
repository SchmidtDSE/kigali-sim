/**
 * Base class for builders that validate required fields before constructing their result.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import java.util.Optional;

/**
 * Base class for builders that validate required fields before constructing their result.
 *
 * <p>Subclasses implement {@link #validate()} to check fields required by every build path, and
 * {@link #buildInternal()} to construct the result. Fields required only by a specific branch of
 * construction logic can call {@link #requireField(Object, String)} directly from within that
 * branch instead of (or in addition to) {@link #validate()}, so validation can be deferred until
 * it is known that branch will actually run.</p>
 *
 * @param <T> The type of object this builder builds
 */
public abstract class ValidatedBuilder<T> {

  private final String builtTypeName;

  /**
   * Create a new ValidatedBuilder.
   *
   * @param builtTypeName The name of the type this builder builds, used in validation error
   *     messages
   */
  protected ValidatedBuilder(String builtTypeName) {
    this.builtTypeName = builtTypeName;
  }

  /**
   * Validate required fields and build the result.
   *
   * @return The built result
   * @throws IllegalStateException if a required field is missing
   */
  public final T build() {
    validate();
    return buildInternal();
  }

  /**
   * Validate the fields required by every build path.
   *
   * @throws IllegalStateException if a required field is missing
   */
  protected abstract void validate();

  /**
   * Construct the result after {@link #validate()} has passed.
   *
   * @return The built result
   */
  protected abstract T buildInternal();

  /**
   * Require that a builder field was set before building.
   *
   * @param value The field value to check
   * @param fieldName The name of the field, used in the error message
   * @throws IllegalStateException if value is empty
   */
  protected void requireField(Object value, String fieldName) {
    if (Optional.ofNullable(value).isEmpty()) {
      throw new IllegalStateException(fieldName + " is required to build a " + builtTypeName);
    }
  }

  /**
   * Require that a builder field backed by an {@link Optional} was set before building.
   *
   * @param value The optional field value to check
   * @param fieldName The name of the field, used in the error message
   * @throws IllegalStateException if value is empty
   */
  protected void requireField(Optional<?> value, String fieldName) {
    if (value.isEmpty()) {
      throw new IllegalStateException(fieldName + " is required to build a " + builtTypeName);
    }
  }
}
