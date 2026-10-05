/**
 * Enum describing the kind of an operation.
 *
 * <p>This enum lets code which inspects a parsed program, like validators, identify an
 * operation without checking its concrete class.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

/**
 * Enum describing the kind of an operation.
 */
public enum OperationType {
  ADDITION,
  CAP_DISPLACING,
  CAP,
  /** Retirement following a Weibull survival curve. */
  RETIRE_WEIBULL,
  /** Retirement of the cohort at an exact age. */
  RETIRE_EXACT,
  /** Set of a stream value. */
  SET,
  /** Change of a stream value. */
  CHANGE,
  /** Any other operation. */
  OTHER
}
