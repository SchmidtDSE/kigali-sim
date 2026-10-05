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
  /** Addition of two values. */
  ADDITION,
  /** Cap of a stream value with equivalent displacement of the excess. */
  CAP_DISPLACING,
  /** Cap of a stream value with optional displacement of the excess. */
  CAP,
  /** Change of a stream value. */
  CHANGE,
  /** Conversion of a value to different units. */
  CHANGE_UNITS,
  /** Ordering comparison of two values. */
  COMPARISON,
  /** Conditional (if-else) selection between two values. */
  CONDITIONAL,
  /** Definition of a variable. */
  DEFINE_VARIABLE,
  /** Division of two values. */
  DIVISION,
  /** Random draw from a normal distribution. */
  DRAW_NORMAL,
  /** Random draw from a uniform distribution. */
  DRAW_UNIFORM,
  /** Enabling of a stream without setting a value. */
  ENABLE,
  /** Equality comparison of two values. */
  EQUALITY,
  /** Set of a consumption (equals) value. */
  EQUALS,
  /** Floor of a stream value with equivalent displacement of the shortfall. */
  FLOOR_DISPLACING,
  /** Floor of a stream value with optional displacement of the shortfall. */
  FLOOR,
  /** Retrieval of a stream value. */
  GET_STREAM,
  /** Retrieval of a variable value. */
  GET_VARIABLE,
  /** Set of an initial charge. */
  INITIAL_CHARGE,
  /** Sequential combination of two operations. */
  JOINT,
  /** Limiting of a value to a range. */
  LIMIT,
  /** Logical combination of two values. */
  LOGICAL,
  /** Multiplication of two values. */
  MULTIPLICATION,
  /** Exponentiation of one value by another. */
  POWER,
  /** Value resolved prior to runtime. */
  PRE_CALCULATED,
  /** Servicing (recharge or precharge) of equipment. */
  RECHARGE,
  /** Recovery of substance. */
  RECOVER,
  /** Removal of units from a value. */
  REMOVE_UNITS,
  /** Replacement of one substance with another. */
  REPLACE,
  /** Retirement at a constant rate. */
  RETIRE,
  /** Retirement following a Weibull survival curve. */
  RETIRE_WEIBULL,
  /** Retirement of the cohort at an exact age. */
  RETIRE_EXACT,
  /** Retirement at a constant rate with replacement. */
  RETIRE_WITH_REPLACEMENT,
  /** Set of a stream value. */
  SET,
  /** Subtraction of two values. */
  SUBTRACTION,
  /** Any other operation, like those defined outside the built-in set. */
  OTHER
}
