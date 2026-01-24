/**
 * Presenter for managing the applications list in the UI editor.
 *
 * @license BSD, see LICENSE.md.
 */
import {MetaSerializer, MetaChangeApplier} from "meta_serialization";
import {Application} from "ui_translator_components";
import {
  NameConflictResolution,
  resolveNameConflict,
  DuplicateEntityPresenter,
} from "duplicate_util";
import {
  buildSetupListButton,
  getSanitizedFieldValue,
  setupDialogInternalLinks,
} from "ui_editor_util";

/**
 * Manages the UI for listing and editing applications.
 *
 * Manages the UI for listing and editing applications where these refer to
 * collections of substances based on use like commercial refrigeration.
 */
class ApplicationsListPresenter {
  /**
   * Creates a new ApplicationsListPresenter.
   *
   * @param {HTMLElement} root - Root DOM element for the applications list.
   * @param {Function} getCodeObj - Callback to get the current code object.
   * @param {Function} onCodeObjUpdate - Callback when code object is updated.
   */
  constructor(root, getCodeObj, onCodeObjUpdate) {
    const self = this;
    self._root = root;
    self._dialog = self._root.querySelector(".dialog");
    self._getCodeObj = getCodeObj;
    self._onCodeObjUpdate = onCodeObjUpdate;
    self._editingName = null;
    self._setupDialog();
    self.refresh();
  }

  /**
   * Refreshes the applications list display.
   *
   * @param {Object} codeObj - Current code object.
   */
  refresh(codeObj) {
    const self = this;
    self._refreshList(codeObj);
  }

  /**
   * Updates the applications list UI with current data.
   *
   * @param {Object} codeObj - Current code object from which to extract
   *     applications.
   * @private
   */
  _refreshList(codeObj) {
    const self = this;
    const appNames = self._getAppNames();
    const itemList = d3.select(self._root).select(".item-list");

    itemList.html("");
    const newItems = itemList.selectAll("li")
      .data(appNames)
      .enter()
      .append("li");

    newItems.attr("aria-label", (x) => x);

    const buttonsPane = newItems.append("div").classed("list-buttons", true);

    newItems
      .append("div")
      .classed("list-label", true)
      .text((x) => x);

    buttonsPane
      .append("a")
      .attr("href", "#")
      .on("click", (event, x) => {
        event.preventDefault();
        self._showDialogFor(x);
      })
      .text("edit")
      .attr("aria-label", (x) => "edit " + x);

    buttonsPane.append("span").text(" | ");

    buttonsPane
      .append("a")
      .attr("href", "#")
      .on("click", (event, x) => {
        event.preventDefault();
        const message = "Are you sure you want to delete " + x + "?";
        const isConfirmed = confirm(message);
        if (isConfirmed) {
          const codeObj = self._getCodeObj();
          codeObj.deleteApplication(x);
          self._onCodeObjUpdate(codeObj);
        }
      })
      .text("delete")
      .attr("aria-label", (x) => "delete " + x);
  }

  /**
   * Sets up the dialog window for adding/editing applications.
   *
   * @private
   */
  _setupDialog() {
    const self = this;
    const addLink = self._root.querySelector(".add-link");
    addLink.addEventListener("click", (event) => {
      self._showDialogFor(null);
      event.preventDefault();
    });

    const closeButton = self._root.querySelector(".cancel-button");
    closeButton.addEventListener("click", (event) => {
      self._dialog.close();
      event.preventDefault();
    });

    const saveButton = self._root.querySelector(".save-button");
    saveButton.addEventListener("click", (event) => {
      event.preventDefault();

      const cleanName = (x) => x.replaceAll('"', "").replaceAll(",", "").trim();

      const nameInput = self._dialog.querySelector(".edit-application-name-input");
      const newNameUnguarded = cleanName(nameInput.value);

      const subnameInput = self._dialog.querySelector(".edit-application-subname-input");
      const newSubnameUnguarded = cleanName(subnameInput.value);
      const subnameEmpty = newSubnameUnguarded === "";

      const getEffectiveName = () => {
        if (subnameEmpty) {
          return newNameUnguarded;
        } else {
          return newNameUnguarded + " - " + newSubnameUnguarded;
        }
      };

      const effectiveName = getEffectiveName();
      const baseName = effectiveName === "" ? "Unnamed" : effectiveName;

      const priorNames = new Set(self._getAppNames());
      const resolution = resolveNameConflict(baseName, priorNames);
      const newName = resolution.getNewName();

      // Update the input field to show the resolved name if it was changed
      if (resolution.getNameChanged()) {
        // If the resolved name differs from the effective name, update the main name input
        // We need to handle the case where there's a subname
        if (subnameEmpty) {
          nameInput.value = newName;
        } else {
          // For compound names with subnames, we need to update the main part
          const nameParts = newName.split(" - ");
          if (nameParts.length > 1) {
            nameInput.value = nameParts[0];
            // The subname part should remain as-is since conflict resolution affects the whole name
          } else {
            nameInput.value = newName;
          }
        }
      }

      if (self._editingName === null) {
        const application = new Application(newName, [], false, true);
        const codeObj = self._getCodeObj();
        codeObj.addApplication(application);
        self._onCodeObjUpdate(codeObj);
      } else {
        const codeObj = self._getCodeObj();
        codeObj.renameApplication(self._editingName, newName);
        self._onCodeObjUpdate(codeObj);
      }

      self._dialog.close();
    });
  }

  /**
   * Shows the dialog for adding or editing an application.
   *
   * @param {string|null} name - Name of application to edit. Pass null if this
   *     is for a new application.
   * @private
   */
  _showDialogFor(name) {
    const self = this;
    self._editingName = name;

    if (name === null) {
      self._dialog.querySelector(".edit-application-name-input").value = "";
      self._dialog.querySelector(".edit-application-subname-input").value = "";
      self._dialog.querySelector(".action-title").innerHTML = "Add";
    } else {
      const nameComponents = name.split(" - ");
      const displayName = nameComponents[0];
      const subname = nameComponents.slice(1).join(" - ");
      self._dialog.querySelector(".edit-application-name-input").value = displayName;
      self._dialog.querySelector(".edit-application-subname-input").value = subname;
      self._dialog.querySelector(".action-title").innerHTML = "Edit";
    }

    self._dialog.showModal();
  }

  /**
   * Gets list of all application names.
   *
   * @returns {string[]} Array of application names.
   * @private
   */
  _getAppNames() {
    const self = this;
    const codeObj = self._getCodeObj();
    const applications = codeObj.getApplications();
    const appNames = applications.map((x) => x.getName());
    return appNames.sort();
  }
}

export {ApplicationsListPresenter};
