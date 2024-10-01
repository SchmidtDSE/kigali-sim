class ApplicationsListPresenter {

  constructor(root) {
    const self = this;
    self._root = root;
    self._dialog = self._root.querySelector(".dialog");
    self._setupAddLink();
  }

  _setupAddLink() {
    const self = this;
    const addLink = self._root.querySelector(".add-link");
    addLink.addEventListener("click", (event) => {
      self._dialog.showModal();
      event.preventDefault();
    });
  }

}


class UiEditorPresenter {

  constructor(tabRoot, contentsRoot) {
    const self = this;
    self._tabs = new Tabby("#" + tabRoot.id);
    self._contentsSelection = contentsRoot;
    const appEditor = self._contentsSelection.querySelector(".applications");
    self._applicationsList = new ApplicationsListPresenter(appEditor);
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
