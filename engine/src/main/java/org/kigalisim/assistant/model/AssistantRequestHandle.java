/**
 * Handle for an in-flight model request.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.assistant.model;

/**
 * Allows an in-flight request to be cancelled.
 *
 * <p>Cancellation may be a platform-specific operation. This handle deliberately limits its scope
 * to cancellation only.</p>
 */
public interface AssistantRequestHandle {

  /**
   * Requests cancellation of the associated request.
   */
  void cancel();
}
