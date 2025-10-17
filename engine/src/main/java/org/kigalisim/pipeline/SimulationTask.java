/**
 * Represents a single simulation task (scenario + replicate).
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.pipeline;

/**
 * A single simulation task consisting of a scenario name and replicate number.
 */
public class SimulationTask {
  private final String scenarioName;
  private final int replicateNumber;
  private final int totalReplicates;

  /**
   * Create a new simulation task.
   *
   * @param scenarioName The name of the scenario to run
   * @param replicateNumber The replicate number (1-indexed)
   * @param totalReplicates The total number of replicates for this scenario
   */
  public SimulationTask(String scenarioName, int replicateNumber, int totalReplicates) {
    this.scenarioName = scenarioName;
    this.replicateNumber = replicateNumber;
    this.totalReplicates = totalReplicates;
  }

  /**
   * Get the scenario name.
   *
   * @return The scenario name
   */
  public String getScenarioName() {
    return scenarioName;
  }

  /**
   * Get the replicate number.
   *
   * @return The replicate number (1-indexed)
   */
  public int getReplicateNumber() {
    return replicateNumber;
  }

  /**
   * Get the total number of replicates.
   *
   * @return The total number of replicates
   */
  public int getTotalReplicates() {
    return totalReplicates;
  }

  @Override
  public String toString() {
    return "SimulationTask{scenario='" + scenarioName + "', replicate=" + replicateNumber
        + "/" + totalReplicates + "}";
  }
}
