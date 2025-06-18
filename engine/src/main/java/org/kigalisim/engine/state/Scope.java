/**
 * Object defining a scope within the engine including variables accessible from that scope.
 *
 * <p>This class represents a scope in the engine with stanza, application, and substance
 * context, managing variable access and providing methods to create derived scopes.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import static org.kigalisim.engine.state.EngineConstants.APPLICATION_CONTEXT;
import static org.kigalisim.engine.state.EngineConstants.STANZA_CONTEXT;
import static org.kigalisim.engine.state.EngineConstants.SUBSTANCE_CONTEXT;

import java.util.Optional;
import org.kigalisim.engine.number.EngineNumber;

/**
 * Object defining a scope within the engine including variables accessible from that scope.
 *
 * <p>Manages scope hierarchy and variable access across different context levels in the engine.</p>
 */
public class Scope {

  private final Optional<String> stanza;
  private final Optional<String> application;
  private final Optional<String> substance;
  private final VariableManager variableManager;

  /**
   * Create a new scope.
   *
   * @param stanza The name of stanza or null if in global scope
   * @param application The name of the application or null if in stanza or higher scope
   * @param substance The name of the substance or null if in application or higher scope
   * @param variableManager The variable manager to reach variables accessible from this
   *     scope or null if no variables accessible
   */
  public Scope(String stanza, String application, String substance,
               VariableManager variableManager) {
    this.stanza = Optional.ofNullable(stanza);
    this.application = Optional.ofNullable(application);
    this.substance = Optional.ofNullable(substance);

    if (substance != null && application == null) {
      throw new IllegalArgumentException("Cannot specify substance without application.");
    }

    if (application != null && stanza == null) {
      throw new IllegalArgumentException("Cannot specify application without stanza.");
    }

    if (variableManager == null) {
      int contextLevel = calculateContextLevel(stanza, application, substance);
      this.variableManager = new VariableManager(contextLevel);
    } else {
      this.variableManager = variableManager;
    }
  }

  /**
   * Convenience constructor for creating a scope without a variable manager.
   *
   * @param stanza The name of stanza or null if in global scope
   * @param application The name of the application or null if in stanza or higher scope
   * @param substance The name of the substance or null if in application or higher scope
   */
  public Scope(String stanza, String application, String substance) {
    this(stanza, application, substance, null);
  }

  /**
   * Get the name of the stanza where this scope resides.
   *
   * @return The name of the current stanza or empty if in global scope
   */
  public Optional<String> getStanza() {
    return stanza;
  }

  /**
   * Get the name of the application where this scope resides.
   *
   * @return The name of the current application or empty if in stanza or higher scope
   */
  public Optional<String> getApplication() {
    return application;
  }

  /**
   * Get the name of the substance where this scope resides.
   *
   * @return The name of the current substance or empty if in application or higher scope
   */
  public Optional<String> getSubstance() {
    return substance;
  }

  /**
   * Set the stanza name for this scope.
   *
   * @param stanza The stanza name (must not be null)
   * @throws IllegalArgumentException if stanza is null
   */
  public void setStanza(String stanza) {
    if (stanza == null) {
      throw new IllegalArgumentException("Stanza cannot be null");
    }
    // This would create a new scope in practice - current implementation is immutable
    throw new UnsupportedOperationException("Scope is immutable - use getWithStanza instead");
  }

  /**
   * Set the application name for this scope.
   *
   * @param application The application name (must not be null)
   * @throws IllegalArgumentException if application is null
   */
  public void setApplication(String application) {
    if (application == null) {
      throw new IllegalArgumentException("Application cannot be null");
    }
    // This would create a new scope in practice - current implementation is immutable
    throw new UnsupportedOperationException("Scope is immutable - use getWithApplication instead");
  }

  /**
   * Set the substance name for this scope.
   *
   * @param substance The substance name (must not be null)
   * @throws IllegalArgumentException if substance is null
   */
  public void setSubstance(String substance) {
    if (substance == null) {
      throw new IllegalArgumentException("Substance cannot be null");
    }
    // This would create a new scope in practice - current implementation is immutable
    throw new UnsupportedOperationException("Scope is immutable - use getWithSubstance instead");
  }

  /**
   * Clear the stanza, setting it to empty.
   */
  public void clearStanza() {
    // This would create a new scope in practice - current implementation is immutable
    throw new UnsupportedOperationException("Scope is immutable - use getWithStanza(null) instead");
  }

  /**
   * Clear the application, setting it to empty.
   */
  public void clearApplication() {
    // This would create a new scope in practice - current implementation is immutable
    throw new UnsupportedOperationException(
        "Scope is immutable - use getWithApplication(null) instead");
  }

  /**
   * Clear the substance, setting it to empty.
   */
  public void clearSubstance() {
    // This would create a new scope in practice - current implementation is immutable
    throw new UnsupportedOperationException(
        "Scope is immutable - use getWithSubstance(null) instead");
  }

  /**
   * Create a new scope derived from this scope at the substance level.
   *
   * @param newSubstance The name of the substance in which the new scope resides
   * @return New scope at the given substance
   */
  public Scope getWithSubstance(String newSubstance) {
    if (application.isEmpty()) {
      throw new IllegalStateException("Not able to set substance without application.");
    }

    return new Scope(
        stanza.orElse(null),
        application.orElse(null),
        newSubstance,
        variableManager.getWithLevel(SUBSTANCE_CONTEXT)
    );
  }

  /**
   * Create a new scope derived from this scope at the application level.
   *
   * @param newApplication The name of the application in which the new scope resides
   * @return New scope at the given application
   */
  public Scope getWithApplication(String newApplication) {
    if (stanza.isEmpty()) {
      throw new IllegalStateException("Not able to set substance without stanza.");
    }

    return new Scope(
        stanza.orElse(null),
        newApplication,
        null,
        variableManager.getWithLevel(APPLICATION_CONTEXT)
    );
  }

  /**
   * Create a new scope derived from this scope at the stanza level.
   *
   * @param newStanza The name of the stanza in which the new scope resides
   * @return New scope at the given stanza
   */
  public Scope getWithStanza(String newStanza) {
    return new Scope(newStanza, null, null, variableManager.getWithLevel(STANZA_CONTEXT));
  }

  /**
   * Define a variable in the current scope.
   *
   * <p>Define a new variable in the current scope or throw an error if a variable
   * of this name already exists in this scope at the current context level.</p>
   *
   * @param name The name of the variable to define
   */
  public void defineVariable(String name) {
    variableManager.defineVariable(name);
  }

  /**
   * Set the value of a variable already defined.
   *
   * <p>Set the value of a variable or throw an error if no variable of the given
   * name is accessible from the current scope.</p>
   *
   * @param name The name of the variable to set
   * @param value The new value of the variable
   */
  public void setVariable(String name, EngineNumber value) {
    variableManager.setVariable(name, value);
  }

  /**
   * Get the value of a variable already defined.
   *
   * <p>Get the value of a variable or throw an error if no variable of the given
   * name is accessible from the current scope.</p>
   *
   * @param name The name of the variable to get
   * @return Value of the given variable
   */
  public EngineNumber getVariable(String name) {
    return variableManager.getVariable(name);
  }

  /**
   * Calculate the context level based on the scope components.
   *
   * @param stanza The stanza name
   * @param application The application name
   * @param substance The substance name
   * @return The context level
   */
  private int calculateContextLevel(String stanza, String application, String substance) {
    int contextLevel = 0;
    if (stanza != null) {
      contextLevel++;
    }
    if (application != null) {
      contextLevel++;
    }
    if (substance != null) {
      contextLevel++;
    }
    return contextLevel;
  }
}
