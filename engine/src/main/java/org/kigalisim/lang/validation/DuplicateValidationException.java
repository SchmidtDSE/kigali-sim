/**
 * Exception thrown when duplicate names are detected during program validation.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.validation;

/**
 * Exception thrown when duplicate names are detected during program validation.
 *
 * <p>This exception provides structured information about the type of duplicate
 * detected, the duplicate name, and the context where the duplication occurred.</p>
 */
public class DuplicateValidationException extends RuntimeException {

  private final String duplicateType;
  private final String duplicateName;
  private final String context;

  /**
   * Create a new duplicate validation exception.
   *
   * @param duplicateType The type of item that is duplicated (e.g., "scenario", "application")
   * @param duplicateName The name that is duplicated
   * @param context The context where the duplication occurred (e.g., "program", "policy 'default'")
   */
  public DuplicateValidationException(String duplicateType, String duplicateName, String context) {
    super(buildMessage(duplicateType, duplicateName, context));
    this.duplicateType = duplicateType;
    this.duplicateName = duplicateName;
    this.context = context;
  }

  /**
   * Create a new duplicate validation exception with a custom message.
   *
   * @param duplicateType The type of item that is duplicated
   * @param duplicateName The name that is duplicated
   * @param context The context where the duplication occurred
   * @param message Custom error message
   */
  public DuplicateValidationException(String duplicateType, String duplicateName,
      String context, String message) {
    super(message);
    this.duplicateType = duplicateType;
    this.duplicateName = duplicateName;
    this.context = context;
  }

  /**
   * Get the type of item that is duplicated.
   *
   * @return The duplicate type (e.g., "scenario", "application")
   */
  public String getDuplicateType() {
    return duplicateType;
  }

  /**
   * Get the name that is duplicated.
   *
   * @return The duplicate name
   */
  public String getDuplicateName() {
    return duplicateName;
  }

  /**
   * Get the context where the duplication occurred.
   *
   * @return The context (e.g., "program", "policy 'default'")
   */
  public String getContext() {
    return context;
  }

  /**
   * Build a standardized error message for duplicate validation.
   *
   * @param duplicateType The type of item that is duplicated
   * @param duplicateName The name that is duplicated
   * @param context The context where the duplication occurred
   * @return Formatted error message
   */
  private static String buildMessage(String duplicateType, String duplicateName, String context) {
    String contextQualifier = getContextQualifier(context);
    return String.format("Duplicate %s name '%s' found in %s. Each %s must have a unique name%s.",
        duplicateType, duplicateName, context, duplicateType, contextQualifier);
  }

  /**
   * Get the context qualifier for the error message.
   *
   * @param context The context where the duplication occurred
   * @return Context qualifier string (e.g., " within its policy", "")
   */
  private static String getContextQualifier(String context) {
    if (context.startsWith("policy")) {
      return " within its policy";
    } else if (context.startsWith("application")) {
      return " within its application";
    } else {
      return "";
    }
  }
}
