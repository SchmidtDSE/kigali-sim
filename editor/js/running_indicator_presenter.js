/**
 * Manages the running indicator and progress bar display.
 */
class RunningIndicatorPresenter {
  constructor() {
    const self = this;
    self._runningIndicator = document.getElementById("running-indicator");
    self._progressBar = document.getElementById("simulation-progress");
    self._resultsSection = document.getElementById("results");
  }

  /**
   * Show the running indicator with progress at 0%.
   */
  show() {
    const self = this;
    self.reset();
    self._resultsSection.style.display = "block";
    self._runningIndicator.style.display = "block";
  }

  /**
   * Hide the running indicator.
   */
  hide() {
    const self = this;
    self._runningIndicator.style.display = "none";
  }

  /**
   * Update the progress bar.
   * @param {number} percentage - Progress percentage (0-100)
   */
  updateProgress(percentage) {
    const self = this;
    self._progressBar.value = percentage;
  }

  /**
   * Reset progress to 0%.
   */
  reset() {
    const self = this;
    self.updateProgress(0);
  }
}
