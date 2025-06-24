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
  private final int startYear;
  private final int endYear;
  private final int trials;

  /**
   * Create a new record of a scenario.
   *
   * @param name The name of the scenario parsed.
   * @param policies The ordered list of policies which should be applied in this scenario excluding
   *     default which is always applied first.
   * @param startYear The starting year for the simulation.
   * @param endYear The ending year for the simulation.
   * @param trials The number of trials to run for the simulation.
   */
  public ParsedScenario(String name, List<String> policies, int startYear, int endYear, int trials) {
    this.name = name;
    this.policies = policies;
    this.startYear = startYear;
    this.endYear = endYear;
    this.trials = trials;
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

  /**
   * Get the starting year for the simulation.
   *
   * @return The starting year.
   */
  public int getStartYear() {
    return startYear;
  }

  /**
   * Get the ending year for the simulation.
   *
   * @return The ending year.
   */
  public int getEndYear() {
    return endYear;
  }

  /**
   * Get the number of trials for the simulation.
   *
   * @return The number of trials.
   */
  public int getTrials() {
    return trials;
  }

  // TODO: getTrials is not used yet.

}
