/**
 * Utility functions for UI-based authoring experience.
 *
 * @license BSD
 */
import {EngineNumber} from "engine_number";
import {NumberParseUtil} from "number_parse_util";
import {VALID_YEAR_KEYWORDS} from "ui_editor_const";

/**
 * Stream target selectors used throughout the application for updating dropdown states.
 * @constant {Array<string>}
 */
export const STREAM_TARGET_SELECTORS = [
  ".set-target-input",
  ".change-target-input",
  ".limit-target-input",
  ".replace-target-input",
  ".displacing-input",
];

/**
 * Invalid patterns for numeric input validation.
 * @constant {Array<RegExp>}
 */
const NUMERIC_INPUT_INVALID_PATTERNS = [
  /[a-zA-Z\s]/, // Alphabetical characters or spaces
  /[^\d\s\-+.,]/, // Non-numeric symbols except digits, spaces, signs, periods, and commas
];

/**
 * Determines if an input element is a duration field.
 *
 * @param {HTMLElement} input - The input element to check.
 * @returns {boolean} True if the input is a duration field.
 */
function getIsDurationField(input) {
  if (input.classList.contains("duration-start")) {
    return true;
  } else if (input.classList.contains("duration-end")) {
    return true;
  } else {
    return false;
  }
}

/**
 * Gets suggestion and description for numeric input validation.
 *
 * @param {string} fieldDescription - The field description from aria-label.
 * @param {string} value - The input value being validated.
 * @param {boolean} isAmbiguous - Whether the value has an ambiguous format.
 * @param {boolean} isParseError - Whether the value failed to parse.
 * @param {Object} parseResult - The parse result object (if parse error).
 * @param {NumberParseUtil} numberParser - The number parser utility.
 * @returns {Object} Object with suggestion and description fields.
 */
function getNumericInputSuggestionAndDescription(
  fieldDescription,
  value,
  isAmbiguous,
  isParseError,
  parseResult,
  numberParser,
) {
  let description = fieldDescription;
  let suggestion = "";

  if (isAmbiguous) {
    description = `${fieldDescription} (ambiguous number format)`;
    suggestion = numberParser.getDisambiguationSuggestion(value);
  } else if (isParseError) {
    const errorMessage = parseResult.getError();
    // Extract suggestion from error message if it contains "Please use:"
    if (errorMessage.includes("Please use:")) {
      const match = errorMessage.match(/Please use: '([^']+)'/);
      if (match) {
        suggestion = `Use '${match[1]}' instead (comma for thousands, period for decimal)`;
      }
    }
    description = `${fieldDescription} (unsupported number format)`;
  }

  return {suggestion, description};
}

/**
 * Updates the visibility of selector elements based on selected duration type.
 *
 * @param {HTMLElement} dateSelector - The date selector element to update.
 */
export function updateDurationSelector(dateSelector) {
  const makeVisibilityCallback = (showStart, showEnd) => {
    return () => {
      const startElement = dateSelector.querySelector(".duration-start");
      startElement.style.display = showStart ? "inline-block" : "none";

      const endElement = dateSelector.querySelector(".duration-end");
      endElement.style.display = showEnd ? "inline-block" : "none";

      const toElement = dateSelector.querySelector(".duration-to");
      const showTo = showStart && showEnd;
      toElement.style.display = showTo ? "inline-block" : "none";
    };
  };

  const strategies = {
    "in year": makeVisibilityCallback(true, false),
    "during all years": makeVisibilityCallback(false, false),
    "starting in year": makeVisibilityCallback(true, false),
    "ending in year": makeVisibilityCallback(false, true),
    "during years": makeVisibilityCallback(true, true),
  };

  const refreshVisibility = (dateSelector) => {
    const currentValue = dateSelector.querySelector(".duration-type-input").value;
    const strategy = strategies[currentValue];
    strategy();
  };

  refreshVisibility(dateSelector);
}

/**
 * Initializes a duration selector.
 *
 * @param {HTMLElement} newDiv - The new element to set up duration selector for.
 */
export function setupDurationSelector(newDiv) {
  const dateSelectors = Array.of(...newDiv.querySelectorAll(".duration-subcomponent"));
  dateSelectors.forEach((dateSelector) => {
    dateSelector.addEventListener("change", (event) => {
      updateDurationSelector(dateSelector);
    });

    updateDurationSelector(dateSelector);
  });
}

/**
 * Build a function which sets up a list button with add/delete functionality.
 *
 * @param {Function} postCallback - Function to call after each list item UI is
 *     initialized or removed. If not given, will use a no-op.
 */
export function buildSetupListButton(postCallback) {
  /**
   * Function which exeuctes on adding a new lits element.
   *
   * @param {HTMLElement} button - Button element to set up.
   * @param {HTMLElement} targetList - List element to add items to.
   * @param {string} templateId - ID of template to use for new items.
   * @param {Function} initUiCallback - Callback to invoke when item added.
   * @param {string} context - Context for stream detection ('consumption' or 'policy').
   */
  return (button, targetList, templateId, initUiCallback, context) => {
    button.addEventListener("click", (event) => {
      event.preventDefault();

      const newDiv = document.createElement("div");
      newDiv.innerHTML = document.getElementById(templateId).innerHTML;
      newDiv.classList.add("dialog-list-item");
      targetList.appendChild(newDiv);

      const deleteLink = newDiv.querySelector(".delete-command-link");
      deleteLink.addEventListener("click", (event) => {
        event.preventDefault();
        newDiv.remove();
        postCallback();
      });

      initUiCallback(null, newDiv, context);

      setupDurationSelector(newDiv);

      if (postCallback !== undefined) {
        postCallback();
      }
    });
  };
}

/**
 * Sets a form field value with fallback to default.
 *
 * @param {HTMLElement} selection - Form field element.
 * @param {Object} source - Source object to get value from.
 * @param {*} defaultValue - Default value if source is null.
 * @param {Function} strategy - Function to extract value from source.
 */
export function setFieldValue(selection, source, defaultValue, strategy) {
  const newValue = source === null ? null : strategy(source);
  const valueOrDefault = newValue === null ? defaultValue : newValue;
  selection.value = valueOrDefault;
}

/**
 * Gets raw value from a form field.
 *
 * @param {HTMLElement} selection - Form field to get value from.
 * @returns {string} The field's value.
 */
export function getFieldValue(selection) {
  return selection.value;
}

/**
 * Gets sanitized value from a form field, removing quotes and commas.
 *
 * @param {HTMLElement} selection - Form field to get sanitized value from.
 * @returns {string} Sanitized field value.
 */
export function getSanitizedFieldValue(selection) {
  const valueRaw = getFieldValue(selection);
  const clean = valueRaw.replaceAll('"', "").replaceAll(",", "");
  const trimmed = clean.trim();
  const guarded = trimmed === "" ? "Unnamed" : trimmed;
  return guarded;
}

/**
 * Sets up a list input with template-based items.
 *
 * @param {HTMLElement} listSelection - Container element for list.
 * @param {string} itemTemplate - HTML template for list items.
 * @param {Array} items - Array of items to populate list.
 * @param {Function} uiInit - Callback to initialize each item's UI.
 * @param {Function} removeCallback - Callback to invoke if item removed.
 */
export function setListInput(listSelection, itemTemplate, items, uiInit, removeCallback) {
  listSelection.innerHTML = "";
  const addItem = (item) => {
    const newDiv = document.createElement("div");
    newDiv.innerHTML = itemTemplate;
    newDiv.classList.add("dialog-list-item");
    listSelection.appendChild(newDiv);
    uiInit(item, newDiv);

    const deleteLink = newDiv.querySelector(".delete-command-link");
    deleteLink.addEventListener("click", (event) => {
      event.preventDefault();
      newDiv.remove();
      removeCallback(item);
    });
  };
  items.forEach(addItem);
}

/**
 * Read the current items in a list.
 *
 * @param {HTMLElement} selection - The HTML element containing list items.
 * @param {Function} itemReadStrategy - A function to process each list item.
 * @returns {Array} An array of processed items returned by the strategy.
 */
export function getListInput(selection, itemReadStrategy) {
  const dialogListItems = Array.of(...selection.querySelectorAll(".dialog-list-item"));
  return dialogListItems.map(itemReadStrategy);
}

/**
 * Sets a value/units pair for engine number inputs.
 *
 * @param {HTMLElement} valSelection - Value input element.
 * @param {HTMLElement} unitsSelection - Units select element.
 * @param {Object} source - Source object for values.
 * @param {EngineNumber} defaultValue - Default engine number.
 * @param {Function} strategy - Function to extract engine number from source.
 */
export function setEngineNumberValue(valSelection, unitsSelection, source, defaultValue, strategy) {
  const newValue = source === null ? null : strategy(source);
  const valueOrDefault = newValue === null ? defaultValue : newValue;

  // Use original string for value field if available, otherwise use numeric value
  if (valueOrDefault.hasOriginalString()) {
    valSelection.value = valueOrDefault.getOriginalString();
  } else {
    const numericValue = valueOrDefault.getValue();
    // Display empty string instead of NaN
    valSelection.value = isNaN(numericValue) ? "" : numericValue;
  }
  unitsSelection.value = valueOrDefault.getUnits();
}

/**
 * Gets an engine number from value/units form fields.
 *
 * @param {HTMLElement} valSelection - Value input element.
 * @param {HTMLElement} unitsSelection - Units select element.
 * @returns {EngineNumber} Combined engine number object.
 */
export function getEngineNumberValue(valSelection, unitsSelection) {
  const valueString = valSelection.value;
  const units = unitsSelection.value;

  // Parse the value to get the numeric value, but preserve original string formatting
  const numericValue = parseFloat(valueString);

  // If parsing results in NaN, default to 0 but preserve original string if not empty
  const finalValue = isNaN(numericValue) ? 0 : numericValue;
  const originalString = valueString.trim() === "" ? null : valueString.trim();

  return new EngineNumber(finalValue, units, originalString);
}

/**
 * Inverts the sign of a number string while preserving formatting.
 *
 * @param {string} numberString - The number string to invert
 * @returns {string} The number string with inverted sign
 */
export function invertNumberString(numberString) {
  const trimmed = numberString.trim();
  if (trimmed.startsWith("-")) {
    // Remove the minus sign (or replace with plus for explicit positive)
    return "+" + trimmed.substring(1);
  } else if (trimmed.startsWith("+")) {
    // Replace plus with minus
    return "-" + trimmed.substring(1);
  } else {
    // No sign present, add minus
    return "-" + trimmed;
  }
}

/**
 * Validates numeric inputs within a dialog and prompts user for potentially
 * invalid values.
 *
 * @param {HTMLElement} dialog - The dialog element containing numeric inputs
 * @param {string} dialogType - Type of dialog for error message context
 *     ("substance", "policy", "simulation")
 * @returns {boolean} True if user confirms to proceed, false if user cancels
 */
export function validateNumericInputs(dialog, dialogType) {
  const numericInputs = dialog.querySelectorAll(".numeric-input");
  const potentiallyInvalid = [];
  const numberParser = new NumberParseUtil();

  // Check each numeric input
  numericInputs.forEach((input) => {
    const value = input.value.trim();
    if (value === "") {
      return; // Skip empty values (may be optional)
    }

    // Allow valid QubecTalk year keywords for duration fields
    const isDurationField = getIsDurationField(input);
    const isValidYearKeyword = isDurationField && VALID_YEAR_KEYWORDS.includes(value.toLowerCase());

    if (isValidYearKeyword) {
      return; // Skip validation for valid year keywords
    }

    // Check against invalid patterns
    const isLikelyInvalid = NUMERIC_INPUT_INVALID_PATTERNS.some((pattern) => pattern.test(value));

    // Check for ambiguous number formats
    const isAmbiguous = numberParser.isAmbiguous(value);

    // Check if the number fails to parse (e.g., European format)
    const parseResult = numberParser.parseFlexibleNumber(value);
    const isParseError = !parseResult.isSuccess();

    if (isLikelyInvalid || isAmbiguous || isParseError) {
      // Get field description from aria-label
      const fieldDescription = input.getAttribute("aria-label") || "Unknown field";

      const {suggestion, description} = getNumericInputSuggestionAndDescription(
        fieldDescription,
        value,
        isAmbiguous,
        isParseError,
        parseResult,
        numberParser,
      );

      potentiallyInvalid.push({
        element: input,
        value: value,
        description: description,
        suggestion: suggestion,
      });
    }
  });

  // If no potentially invalid values found, proceed
  if (potentiallyInvalid.length === 0) {
    return true;
  }

  // Build user-friendly error message
  const fieldList = potentiallyInvalid.map((item) => {
    if (item.suggestion) {
      return `• ${item.description}: "${item.value}"\n  Suggestion: ${item.suggestion}`;
    } else {
      return `• ${item.description}: "${item.value}"`;
    }
  }).join("\n\n");

  const message = "The following numeric fields contain potentially " +
    `invalid values:\n\n${fieldList}\n\n` +
    "These values may cause simulation errors. You can:\n" +
    "• Click \"Continue\" if these are intentional (equations, etc.)\n" +
    "• Click \"Cancel\" to review and correct the values";

  // Prompt user for confirmation
  return confirm(message);
}

/**
 * Validates simulation duration to warn about very long simulations.
 *
 * @param {HTMLElement} dialog - Dialog element containing start and end inputs.
 * @returns {boolean} True if user confirms or duration is reasonable, false if cancelled.
 * @private
 */
export function validateSimulationDuration(dialog) {
  const startInput = dialog.querySelector(".edit-simulation-start-input");
  const endInput = dialog.querySelector(".edit-simulation-end-input");

  if (!startInput || !endInput) {
    return true; // If inputs not found, proceed
  }

  const startValue = startInput.value.trim();
  const endValue = endInput.value.trim();

  // Only check if both values are simple integers (no equations, etc.)
  const isSimpleInteger = (value) => /^\d+$/.test(value);

  if (!isSimpleInteger(startValue) || !isSimpleInteger(endValue)) {
    return true; // Skip validation for non-simple integers
  }

  const startYear = parseInt(startValue, 10);
  const endYear = parseInt(endValue, 10);
  const duration = endYear - startYear;

  if (duration > 1000) {
    const message = `This simulation spans ${duration} years (${startYear} to ${endYear}), ` +
      "which is over 1000 years.\n\n" +
      "Do you want to continue with this duration?";

    return confirm(message);
  }

  return true;
}

/**
 * Sets the state of a duration selection UI widget.
 *
 * Set the duration for shown within a duration selection UI widget to match
 * that of a given command. If the command is null, it uses the default value.
 *
 * @param {HTMLElement} selection - The selection element containing duration-related inputs.
 * @param {Object} command - The command object from which the duration is extracted.
 * @param {YearMatcher} defaultVal - The default duration value if the command is null.
 * @param {boolean} initListeners - Flag indicating if new event listeners for
 *     element visibility should be added in response to changing duration type.
 */
export function setDuring(selection, command, defaultVal, initListeners) {
  const effectiveVal = command === null ? defaultVal : command.getDuration();
  const durationTypeInput = selection.querySelector(".duration-type-input");

  const setElements = () => {
    if (effectiveVal === null) {
      durationTypeInput.value = "during all years";
      return;
    }

    const durationStartInput = selection.querySelector(".duration-start");
    const durationEndInput = selection.querySelector(".duration-end");
    const durationStart = effectiveVal.getStart();
    const noStart = durationStart === null;
    const durationEnd = effectiveVal.getEnd();
    const noEnd = durationEnd === null;

    // Helper function to safely set year values, preserving original user input
    const setYearValue = (input, yearValue) => {
      if (yearValue === null || yearValue === undefined) {
        input.value = "";
      } else {
        // Use ParsedYear's getYearStr() method for proper display
        input.value = yearValue.getYearStr();
      }
    };

    if (noStart && noEnd) {
      durationTypeInput.value = "during all years";
    } else if (noStart) {
      durationTypeInput.value = "ending in year";
      setYearValue(durationEndInput, durationEnd);
    } else if (noEnd) {
      durationTypeInput.value = "starting in year";
      setYearValue(durationStartInput, durationStart);
    } else if (durationStart && durationEnd && durationStart.equals(durationEnd)) {
      durationTypeInput.value = "in year";
      setYearValue(durationStartInput, durationStart);
    } else {
      durationTypeInput.value = "during years";
      setYearValue(durationStartInput, durationStart);
      setYearValue(durationEndInput, durationEnd);
    }
  };

  setElements();
  updateDurationSelector(selection);

  if (initListeners) {
    durationTypeInput.addEventListener("change", (event) => {
      updateDurationSelector(selection);
    });
  }
}

/**
 * Build a function which updates displays of command counts.
 *
 * @param dialog - Selection over the dialog in which the command count
 *     displays should be updated.
 * @returns Funciton which takes a string list selector and a string display
 *     selector. That function will put the count of commands found in the
 *     list selector into the display selector.
 */
export function buildUpdateCount(dialog) {
  return (listSelector, displaySelector) => {
    const listSelection = dialog.querySelector(listSelector);
    const displaySelection = dialog.querySelector(displaySelector);

    const listItems = Array.of(...listSelection.querySelectorAll(".dialog-list-item"));
    const listCount = listItems.map((x) => 1).reduce((a, b) => a + b, 0);

    displaySelection.innerHTML = listCount;
  };
}

/**
 * Sets up event listeners for internal dialog links.
 *
 * This function adds click event listeners to dialog internal links that, when
 * clicked, will toggle the corresponding tab based on the link's href.
 *
 * @param {HTMLElement} root - The root element containing dialog.
 * @param {Object} tabs - Tabby object for managing tab toggling.
 */
export function setupDialogInternalLinks(root, tabs) {
  const internalLinks = root.querySelectorAll(".dialog-internal-link");
  internalLinks.forEach((link) => {
    link.addEventListener("click", (event) => {
      event.preventDefault();
      const anchor = link.hash;
      tabs.toggle(anchor);
    });
  });
}
