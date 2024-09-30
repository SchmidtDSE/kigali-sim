class ApplicationsListPresenter {

  constructor(targetId, dialogId) {
    const self = this;
    self._root = document.getElementById(targetId);
    self._dialog = document.getElementById(dialogId);
    self._setupAddLink();
  }

  _setupAddLink() {
    const self = this;
    const addLink = self._root.querySelector(".add-link");
    addLink.addEventListener("click", (event) => {
      self._dialog.show();
      event.preventDefault();
    });
  }

}


class UiEditorPresenter {

  constructor(tabsId, contentsId) {
    const self = this;
    self._tabs = new Tabby("#" + tabsId);
    self._contentsSelection = document.getElementById(contentsId);
    self._applicationsList = new ApplicationsListPresenter("ui-application-editor", "application-dialog");
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
