/**
 * Tracks which build/run is the most recent, to let callers discard stale
 * results from a superseded build.
 *
 * @license BSD, see LICENSE.md.
 */

/**
 * Tracks the most recently started "generation" (build/run) so a caller can
 * tell whether a given generation is still the current one.
 *
 * Framework/DOM-independent by design so it can be unit tested in isolation.
 */
class BuildGenerationTracker {
  /**
   * Create a new tracker with no generation started yet.
   */
  constructor() {
    const self = this;

    /**
     * @private
     * @type {number}
     */
    self._latestGenerationId = 0;
  }

  /**
   * Start a new generation, superseding any previously started generation.
   *
   * @returns {number} The new generation's ID.
   */
  startNewGeneration() {
    const self = this;
    self._latestGenerationId += 1;
    return self._latestGenerationId;
  }

  /**
   * Check whether a generation ID is still the most recently started one.
   *
   * @param {number} generationId - The generation ID to check.
   * @returns {boolean} True if generationId is still the latest generation.
   */
  isCurrent(generationId) {
    const self = this;
    return generationId === self._latestGenerationId;
  }
}

export {BuildGenerationTracker};
