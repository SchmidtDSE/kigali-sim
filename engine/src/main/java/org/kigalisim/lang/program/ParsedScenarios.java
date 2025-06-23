/**
 * Record of scenarios parsed from the source of a QubecTalk program.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.program;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Record of scenarios parsed from the source of a QubecTalk program.
 *
 * <p>Contains the scenarios defined in the simulations stanza with their associated policies.</p>
 */
public class ParsedScenarios {

  private final Map<String, ParsedScenario> scenarios;

  /**
   * Create a new record of scenarios.
   *
   * @param scenarios The scenarios defined in the simulations stanza.
   */
  public ParsedScenarios(Iterable<ParsedScenario> scenarios) {
    this.scenarios = StreamSupport.stream(scenarios.spliterator(), false)
        .collect(Collectors.toMap(ParsedScenario::getName, Function.identity()));
  }

  /**
   * Get the names of all scenarios defined in this simulations stanza.
   *
   * @return Set of scenario names.
   */
  public Set<String> getScenarios() {
    return scenarios.keySet();
  }

  /**
   * Get a specific scenario by name.
   *
   * @param name The name of the scenario to retrieve.
   * @return The scenario with the specified name.
   * @throws IllegalArgumentException if no scenario with the given name exists.
   */
  public ParsedScenario getScenario(String name) {
    if (!scenarios.containsKey(name)) {
      throw new IllegalArgumentException("No scenario named " + name);
    }
    return scenarios.get(name);
  }
}
