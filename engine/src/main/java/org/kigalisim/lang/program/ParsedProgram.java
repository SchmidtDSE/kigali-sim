/**
 * The result of parsing a QubecTalk program.
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
 * Result of parsing (interpreting / compiling) a QubecTalk program.
 *
 * <p>Result of parsing (interpreting / compiling) a QubecTalk program which can be used to evaluate
 * all of the policies and scenarios found.</p>
 */
public class ParsedProgram {

  private final Map<String, ParsedPolicy> policies;  // Includes "default" for the base.
  private final Map<String, ParsedScenario> scenarios;

  /**
   * Create a new record of a parsed program.
   *
   * @param policies The policies parsed from the source of this program.
   * @param scenarios The scenarios parsed from the source of this program.
   */
  public ParsedProgram(Iterable<ParsedPolicy> policies, Iterable<ParsedScenario> scenarios) {
    this.policies = StreamSupport.stream(policies.spliterator(), false)
        .collect(Collectors.toMap(ParsedPolicy::getName, Function.identity()));
    this.scenarios = StreamSupport.stream(scenarios.spliterator(), false)
        .collect(Collectors.toMap(ParsedScenario::getName, Function.identity()));
  }

  /**
   * Get the names of all policies defined in this program.
   *
   * @return Set of policy names.
   */
  public Set<String> getPolicies() {
    return policies.keySet();
  }

  /**
   * Get a specific policy by name.
   *
   * @param name The name of the policy to retrieve.
   * @return The policy with the specified name.
   * @throws IllegalArgumentException if no policy with the given name exists.
   */
  public ParsedPolicy getPolicy(String name) {
    if (!policies.containsKey(name)) {
      throw new IllegalArgumentException("No policy named " + name);
    }
    return policies.get(name);
  }

  /**
   * Get the names of all scenarios defined in this program.
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
