/**
 * Callback interface for reporting simulation progress.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim;

/**
 * Callback interface for reporting simulation progress during execution.
 *
 * <p>Progress represents the percent of trials across scenarios completed,
 * where each scenario (e.g., "business as usual") may have multiple trials.
 * The progress value ranges from 0.0 (0%) to 1.0 (100%).</p>
 */
public interface ProgressReportCallback {
  /**
   * Reports the current progress of the simulation.
   *
   * @param progress the progress value between 0.0 (0%) and 1.0 (100%),
   *                 representing the percent of trials across scenarios completed.
   */
  void reportProgress(double progress);
}