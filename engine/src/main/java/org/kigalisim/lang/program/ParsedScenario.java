/**
 * Record of a scenario parsed from the source of a QubecTalk program.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.program;

import java.util.List;

/**
 * Record of a scenario parsed from the source of a QubecTalk program.
 *
 * <p>Contains the ordered list of policies which should be applied in this scenario.</p>
 */
public class ParsedScenario {

  private final String name;
  private final List<String> policies;

  /**
   * Create a new record of a scenario.
   *
   * @param name The name of the scenario parsed.
   * @param policies The ordered list of policies which should be applied in this scenario excluding
   *     default which is always applied first.
   */
  public ParsedScenario(String name, List<String> policies) {
    this.name = name;
    this.policies = policies;
  }

  public String getName() {
    return name;
  }

  /**
   * An ordered list of policies which does not include the "default" policy offering the base.
   *
   * @return Iterable over the names of the policies which should be applied in this scenario in
   *     order.
   */
  public Iterable<String> getPolicies() {
    return policies;
  }

}
