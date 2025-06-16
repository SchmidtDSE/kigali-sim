/**
 * Internal object which manages user defined variables at different scopes.
 *
 * <p>This class manages variables at different context levels (global, stanza,
 * application, substance) and provides scoped access to variable definitions
 * and values.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.state;

import static org.kigalisim.engine.state.EngineConstants.APPLICATION_CONTEXT;
import static org.kigalisim.engine.state.EngineConstants.GLOBAL_CONTEXT;
import static org.kigalisim.engine.state.EngineConstants.STANZA_CONTEXT;
import static org.kigalisim.engine.state.EngineConstants.SUBSTANCE_CONTEXT;

import java.util.HashMap;
import java.util.Map;

/**
 * Internal object which manages user defined variables at different scopes.
 *
 * <p>Manages variable storage and retrieval across different context levels,
 * supporting variable shadowing and scope traversal.</p>
 */
public class VariableManager {

  private final Map<String, Object> globalContext;
  private final Map<String, Object> stanzaContext;
  private final Map<String, Object> applicationContext;
  private final Map<String, Object> substanceContext;
  private final int contextLevel;

  /**
   * Create a new variable manager.
   *
   * @param contextLevel The context level constant at which this manager will be used
   * @param globalContext Map from name of variable to value or null if no variables
   *     exist at that global level
   * @param stanzaContext Map from name of variable to value or null if no variables
   *     exist at that stanza level
   * @param applicationContext Map from name of variable to value or null if no variables
   *     exist at that application level
   * @param substanceContext Map from name of variable to value or null if no variables
   *     exist at that substance level
   */
  public VariableManager(int contextLevel, Map<String, Object> globalContext,
                        Map<String, Object> stanzaContext,
                        Map<String, Object> applicationContext,
                        Map<String, Object> substanceContext) {
    this.globalContext = ensureContext(globalContext);
    this.stanzaContext = ensureContext(stanzaContext);
    this.applicationContext = ensureContext(applicationContext);
    this.substanceContext = ensureContext(substanceContext);
    this.contextLevel = contextLevel;
  }

  /**
   * Convenience constructor for creating a new variable manager at a specific context level.
   *
   * @param contextLevel The context level constant at which this manager will be used
   */
  public VariableManager(int contextLevel) {
    this(contextLevel, null, null, null, null);
  }

  /**
   * Make a new variable manager occupying this namespace but at a different context level.
   *
   * @param contextLevel Constant describing the new context level. If this matches the
   *     current context level, it is assumed adjacent to the current context.
   * @return VariableManager at the given context level
   */
  public VariableManager getWithLevel(int contextLevel) {
    if (contextLevel < GLOBAL_CONTEXT || contextLevel > SUBSTANCE_CONTEXT) {
      throw new IllegalArgumentException("Unexpected context level: " + contextLevel);
    }

    Map<String, Object> newStanzaContext = this.stanzaContext;
    if (contextLevel <= STANZA_CONTEXT) {
      newStanzaContext = new HashMap<>();
    }

    Map<String, Object> newApplicationContext = this.applicationContext;
    if (contextLevel <= APPLICATION_CONTEXT) {
      newApplicationContext = new HashMap<>();
    }

    Map<String, Object> newSubstanceContext = this.substanceContext;
    if (contextLevel <= SUBSTANCE_CONTEXT) {
      newSubstanceContext = new HashMap<>();
    }

    return new VariableManager(
        contextLevel,
        this.globalContext,
        newStanzaContext,
        newApplicationContext,
        newSubstanceContext
    );
  }

  /**
   * Define a new variable in the current context level.
   *
   * <p>Define a new variable in the current context level where an error will be thrown
   * if a variable of the same name exists at this context level.</p>
   *
   * @param name The name of the variable to define
   */
  public void defineVariable(String name) {
    Map<String, Object> context = getContextForLevel(contextLevel);

    if (context.containsKey(name)) {
      throw new IllegalStateException("Variable already defined in this scope: " + name);
    }

    context.put(name, null);
  }

  /**
   * Set the value of a variable already defined.
   *
   * <p>Set the value of a variable already defined where an error will be thrown if
   * a variable of this name has not been defined or is not accessible from the
   * current scope.</p>
   *
   * @param name The name of the variable to be set
   * @param value The new value of the variable
   */
  public void setVariable(String name, Object value) {
    for (int level = contextLevel; level >= GLOBAL_CONTEXT; level--) {
      Map<String, Object> currentContext = getContextForLevel(level);
      if (currentContext.containsKey(name)) {
        currentContext.put(name, value);
        return;
      }
    }

    throw new IllegalStateException("Unable to find variable to set: " + name);
  }

  /**
   * Get the value of a variable already defined.
   *
   * <p>Get the value of a variable already defined such that an error will be thrown
   * if a variable of this name has not been defined or is not accessible from the
   * current scope.</p>
   *
   * @param name The name of the variable to be retrieved
   * @return Current value of this variable
   */
  public Object getVariable(String name) {
    for (int level = contextLevel; level >= GLOBAL_CONTEXT; level--) {
      Map<String, Object> currentContext = getContextForLevel(level);
      if (currentContext.containsKey(name)) {
        return currentContext.get(name);
      }
    }

    throw new IllegalStateException("Unable to find variable to read: " + name);
  }

  /**
   * Get the variable map for a certain context level.
   *
   * @param level Constant corresponding to context level
   * @return Map from name of variable to value at the given context level
   */
  private Map<String, Object> getContextForLevel(int level) {
    switch (level) {
      case GLOBAL_CONTEXT:
        return globalContext;
      case STANZA_CONTEXT:
        return stanzaContext;
      case APPLICATION_CONTEXT:
        return applicationContext;
      case SUBSTANCE_CONTEXT:
        return substanceContext;
      default:
        throw new IllegalArgumentException("Invalid context level: " + level);
    }
  }

  /**
   * Ensure a context map is not null.
   *
   * @param context The context map or null
   * @return The context map or a new empty map if null
   */
  private Map<String, Object> ensureContext(Map<String, Object> context) {
    return context == null ? new HashMap<>() : context;
  }
}
