/**
 * Interface for reporting simulation progress.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.pipeline;

/**
 * Reports progress of simulation execution.
 */
public interface ProgressReporter {
  /**
   * Report progress.
   *
   * @param completed Number of completed tasks
   * @param total Total number of tasks
   */
  void reportProgress(int completed, int total);
}
