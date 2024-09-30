class UiEditorPresenter {

  constructor(tabsId, contentsId) {
    const self = this;
    self._tabs = new Tabby("#" + tabsId);
    self._contentsSelection = document.getElementById(contentsId);
    self._setupAdvancedLinks();
  }

  _setupAdvancedLinks() {
    const self = this;
    const links = Array.of(...self._contentsSelection.querySelectorAll(".advanced-editor-link"));
    links.forEach((link) => link.addEventListener("click", (event) => {
      self._tabs.toggle("#code-editor-pane");
      event.preventDefault();
    }));
  }

}


export { UiEditorPresenter };
