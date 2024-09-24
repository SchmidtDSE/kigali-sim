class ResultsPresenter {
  constructor(targetId) {
    const self = this;
    self._root = document.getElementById(targetId);
    self.hide();
  }

  hide() {
    const self = this;
    self._root.style.display = "none";
  }

  showResults() {
    const self = this;
    self._root.style.display = "block";
  }
}


export {ResultsPresenter};
