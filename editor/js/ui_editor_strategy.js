/**
 * Strategies for initing and setting UI elements.
 *
 * @license BSD, see LICENSE.md.
 */
import {ParsedYear, YearMatcher} from "duration";
import {EngineNumber} from "engine_number";
import {NumberParseUtil} from "number_parse_util";
import {
  Command,
  LimitCommand,
  RechargeCommand,
  RecycleCommand,
  ReplaceCommand,
} from "ui_translator_components";
import {
  setFieldValue,
  getFieldValue,
  setEngineNumberValue,
  getEngineNumberValue,
  invertNumberString,
  setDuring,
} from "ui_editor_util";

/**
 * Initializes a set command UI element.
 *
 * @param {Object} itemObj - The command object to initialize from.
 * @param {HTMLElement} root - The root element containing the UI.
 * @param {Object} codeObj - Optional code object containing substances data.
 * @param {string} context - Context for stream detection ('consumption' or 'policy').
 * @param {Object} streamUpdater - StreamSelectionAvailabilityUpdater instance.
 */
function initSetCommandUi(itemObj, root, codeObj, context, streamUpdater) {
  // Update stream options based on enabled streams - use context-aware detection
  const enabledStreams = streamUpdater.getEnabledStreamsForCurrentContext(codeObj, null, context);
  const targetSelect = root.querySelector(".set-target-input");

  streamUpdater.updateStreamOptionStates(targetSelect, enabledStreams);

  setFieldValue(root.querySelector(".set-target-input"), itemObj, "sales", (x) =>
    x.getTarget(),
  );
  setEngineNumberValue(
    root.querySelector(".set-amount-input"),
    root.querySelector(".set-units-input"),
    itemObj,
    new EngineNumber(1, "mt"),
    (x) => x.getValue(),
  );
  setDuring(
    root.querySelector(".duration-subcomponent"),
    itemObj,
    new YearMatcher(new ParsedYear(1), new ParsedYear(1)),
    true,
  );
}

/**
 * Reads values from a set command UI element.
 *
 * @param {HTMLElement} root - The root element containing the UI.
 * @returns {Command} A new Command object with the UI values.
 */
function readSetCommandUi(root) {
  const target = getFieldValue(root.querySelector(".set-target-input"));
  const amount = getEngineNumberValue(
    root.querySelector(".set-amount-input"),
    root.querySelector(".set-units-input"),
  );
  const duration = readDurationUi(root.querySelector(".duration-subcomponent"));
  return new Command("setVal", target, amount, duration);
}

/**
 * Initializes a change command UI element.
 *
 * @param {Object} itemObj - The command object to initialize from.
 * @param {HTMLElement} root - The root element containing the UI.
 * @param {Object} codeObj - Optional code object containing substances data.
 * @param {string} context - Context for stream detection ('consumption' or 'policy').
 * @param {Object} streamUpdater - StreamSelectionAvailabilityUpdater instance.
 */
function initChangeCommandUi(itemObj, root, codeObj, context, streamUpdater) {
  // Update stream options based on enabled streams - use context-aware detection
  const enabledStreams = streamUpdater.getEnabledStreamsForCurrentContext(codeObj, null, context);
  const targetSelect = root.querySelector(".change-target-input");

  streamUpdater.updateStreamOptionStates(targetSelect, enabledStreams);
  setFieldValue(root.querySelector(".change-target-input"), itemObj, "sales", (x) =>
    x.getTarget(),
  );
  setFieldValue(root.querySelector(".change-sign-input"), itemObj, "+", (x) =>
    x.getValue() < 0 ? "-" : "+",
  );
  setFieldValue(root.querySelector(".change-amount-input"), itemObj, 5, (x) => {
    if (x.getValue() === null || x.getValue().getValue() === null) {
      return 5;
    }
    const valueSigned = x.getValue().getValue();
    const valueUnsigned = Math.abs(valueSigned);
    return valueUnsigned;
  });
  setFieldValue(root.querySelector(".change-units-input"), itemObj, "% / year", (x) => {
    if (x.getValue() === null) {
      return "% / year";
    }
    return x.getValue().getUnits();
  });
  setDuring(
    root.querySelector(".duration-subcomponent"),
    itemObj,
    new YearMatcher(new ParsedYear(2), new ParsedYear(10)),
    true,
  );
}

/**
 * Reads values from a change command UI element.
 *
 * @param {HTMLElement} root - The root element containing the UI.
 * @returns {Command} A new Command object with the UI values.
 */
function readChangeCommandUi(root) {
  const target = getFieldValue(root.querySelector(".change-target-input"));
  const invert = getFieldValue(root.querySelector(".change-sign-input")) === "-";
  const numberParser = new NumberParseUtil();
  const amountInput = getFieldValue(root.querySelector(".change-amount-input"));
  const result = numberParser.parseFlexibleNumber(amountInput);
  if (!result.isSuccess()) {
    throw new Error(`Invalid amount format: ${result.getError()}`);
  }
  const amount = result.getNumber() * (invert ? -1 : 1);
  const units = getFieldValue(root.querySelector(".change-units-input"));
  // Preserve original string format, applying sign inversion if needed
  const originalString = invert ? invertNumberString(amountInput) : amountInput.trim();
  const amountWithUnits = new EngineNumber(amount, units, originalString);
  const duration = readDurationUi(root.querySelector(".duration-subcomponent"));
  return new Command("change", target, amountWithUnits, duration);
}

/**
 * Initializes a limit command UI widget.
 *
 * @param {Object} itemObj - The command object to initialize from.
 * @param {HTMLElement} root - The root element containing the UI.
 * @param {Object} codeObj - The code object containing available substances.
 * @param {string} context - Context for stream detection ('consumption' or 'policy').
 * @param {Object} streamUpdater - StreamSelectionAvailabilityUpdater instance.
 */
function initLimitCommandUi(itemObj, root, codeObj, context, streamUpdater) {
  const substances = codeObj.getSubstances();
  const substanceNamesDup = substances.map((x) => x.getName());
  const substanceNames = Array.of(...new Set(substanceNamesDup));
  const substanceSelect = d3.select(root.querySelector(".substances-select"));
  substanceSelect.html("");
  substanceSelect
    .selectAll("option")
    .data(substanceNames)
    .enter()
    .append("option")
    .attr("value", (x) => x)
    .text((x) => x);

  setFieldValue(root.querySelector(".limit-type-input"), itemObj, "cap", (x) => x.getTypeName());
  setFieldValue(root.querySelector(".limit-target-input"), itemObj, "sales", (x) => x.getTarget());
  setEngineNumberValue(
    root.querySelector(".limit-amount-input"),
    root.querySelector(".limit-units-input"),
    itemObj,
    new EngineNumber(1, "mt"),
    (x) => x.getValue(),
  );
  setFieldValue(root.querySelector(".displacing-type-input"), itemObj, "", (x) =>
    x && x.getDisplacingType ? x.getDisplacingType() : "",
  );
  setFieldValue(root.querySelector(".displacing-target-input"), itemObj, "", (x) =>
    x && x.getDisplacing ? (x.getDisplacing() === null ? "" : x.getDisplacing()) : "",
  );
  setDuring(
    root.querySelector(".duration-subcomponent"),
    itemObj,
    new YearMatcher(new ParsedYear(2), new ParsedYear(10)),
    true,
  );

  // Add event listener to update options when substance changes
  const substanceSelectElement = root.querySelector(".substances-select");

  const updateLimitTargetOptions = () => {
    const limitTargetSelect = root.querySelector(".limit-target-input");
    const enabledStreams = streamUpdater.getEnabledStreamsForCurrentContext(codeObj, null, context);
    streamUpdater.updateStreamOptionStates(limitTargetSelect, enabledStreams);
  };

  const updateDisplacingOptions = () => {
    const displacingTargetSelect = root.querySelector(".displacing-target-input");
    if (displacingTargetSelect) {
      const enabledStreams = streamUpdater.getEnabledStreamsForCurrentContext(
        codeObj, null, context,
      );
      streamUpdater.updateStreamOptionStates(displacingTargetSelect, enabledStreams);
    }
  };

  substanceSelectElement.addEventListener("change", updateLimitTargetOptions);
  substanceSelectElement.addEventListener("change", updateDisplacingOptions);

  // Initial update of stream options
  updateLimitTargetOptions();
  updateDisplacingOptions();
}

/**
 * Reads values from a limit command UI widget.
 *
 * @param {HTMLElement} root - The root element containing the UI.
 * @returns {LimitCommand} A new LimitCommand object with the UI values.
 */
function readLimitCommandUi(root) {
  const limitType = getFieldValue(root.querySelector(".limit-type-input"));
  const target = getFieldValue(root.querySelector(".limit-target-input"));
  const amount = getEngineNumberValue(
    root.querySelector(".limit-amount-input"),
    root.querySelector(".limit-units-input"),
  );
  const displacingTypeRaw = getFieldValue(root.querySelector(".displacing-type-input"));
  const displacingType = displacingTypeRaw || "";
  const displacingTargetRaw = getFieldValue(root.querySelector(".displacing-target-input"));
  const displacingTarget = displacingTargetRaw === "" ? null : displacingTargetRaw;
  const duration = readDurationUi(root.querySelector(".duration-subcomponent"));
  return new LimitCommand(limitType, target, amount, duration, displacingTarget, displacingType);
}

/**
 * Initializes a recharge command UI widget.
 *
 * @param {Object} itemObj - The recharge command object or null for new commands.
 * @param {HTMLElement} root - The root element containing the UI.
 * @param {Object} codeObj - The code object for context.
 */
function initRechargeCommandUi(itemObj, root, codeObj) {
  // All recharge objects are now RechargeCommand instances
  const populationGetter = (x) => {
    const engineNumber = x.getPopulationEngineNumber();
    return engineNumber.getOriginalString() || String(engineNumber.getValue());
  };
  const populationUnitsGetter = (x) => {
    return x.getPopulationEngineNumber().getUnits();
  };
  const volumeGetter = (x) => {
    const engineNumber = x.getVolumeEngineNumber();
    return engineNumber.getOriginalString() || String(engineNumber.getValue());
  };
  const volumeUnitsGetter = (x) => {
    return x.getVolumeEngineNumber().getUnits();
  };

  setFieldValue(
    root.querySelector(".recharge-population-input"),
    itemObj,
    "5",
    populationGetter,
  );
  setFieldValue(
    root.querySelector(".recharge-population-units-input"),
    itemObj,
    "%",
    populationUnitsGetter,
  );
  setFieldValue(
    root.querySelector(".recharge-volume-input"),
    itemObj,
    "0.85",
    volumeGetter,
  );
  setFieldValue(
    root.querySelector(".recharge-volume-units-input"),
    itemObj,
    "kg / unit",
    volumeUnitsGetter,
  );

  // Set up duration using standard pattern
  setDuring(
    root.querySelector(".duration-subcomponent"),
    itemObj,
    new YearMatcher(new ParsedYear(1), new ParsedYear(1)),
    true,
  );
}

/**
 * Reads values from a recharge command UI widget.
 *
 * @param {HTMLElement} root - The root element containing the UI.
 * @returns {RechargeCommand} A new RechargeCommand object with the UI values.
 */
function readRechargeCommandUi(root) {
  const population = getFieldValue(root.querySelector(".recharge-population-input"));
  const populationUnits = getFieldValue(
    root.querySelector(".recharge-population-units-input"),
  );
  const volume = getFieldValue(root.querySelector(".recharge-volume-input"));
  const volumeUnits = getFieldValue(
    root.querySelector(".recharge-volume-units-input"),
  );

  // Read duration using standard pattern
  const duration = readDurationUi(root.querySelector(".duration-subcomponent"));

  // Create EngineNumber objects with original string preservation
  const populationEngineNumber = new EngineNumber(population, populationUnits, population);
  const volumeEngineNumber = new EngineNumber(volume, volumeUnits, volume);

  return new RechargeCommand(
    populationEngineNumber,
    volumeEngineNumber,
    duration,
  );
}

/**
 * Initializes a recycle command UI widget.
 *
 * @param {Object} itemObj - The command object to initialize from.
 * @param {HTMLElement} root - The root element containing the UI.
 * @param {Object} codeObj - Optional code object containing substances data.
 * @param {string} context - Context for stream detection ('consumption' or 'policy').
 * @param {Object} streamUpdater - StreamSelectionAvailabilityUpdater instance.
 */
function initRecycleCommandUi(itemObj, root, codeObj, context, streamUpdater) {
  // Update stream options based on enabled streams - use context-aware detection
  const enabledStreams = streamUpdater.getEnabledStreamsForCurrentContext(codeObj, null, context);
  const displacingSelect = root.querySelector(".displacing-input");

  streamUpdater.updateStreamOptionStates(displacingSelect, enabledStreams);
  setEngineNumberValue(
    root.querySelector(".recycle-amount-input"),
    root.querySelector(".recycle-units-input"),
    itemObj,
    new EngineNumber(10, "%"),
    (x) => x.getTarget(),
  );
  setEngineNumberValue(
    root.querySelector(".recycle-reuse-amount-input"),
    root.querySelector(".recycle-reuse-units-input"),
    itemObj,
    new EngineNumber(10, "%"),
    (x) => x.getValue(),
  );
  setFieldValue(root.querySelector(".displacing-input"), itemObj, "", (x) =>
    x && x.getDisplacing ? (x.getDisplacing() === null ? "" : x.getDisplacing()) : "",
  );
  setFieldValue(
    root.querySelector(".recycle-induction-amount-input"),
    itemObj,
    "100",
    (x) => {
      if (x && x.getInduction) {
        const induction = x.getInduction();
        if (induction === null) {
          return "100";
        } else if (induction === "default") {
          return "100";
        } else if (induction instanceof EngineNumber) {
          return induction.getValue().toString();
        } else {
          return induction.toString();
        }
      } else {
        return "100";
      }
    },
  );
  setFieldValue(root.querySelector(".recycle-stage-input"), itemObj, "recharge", (x) =>
    x && x.getStage ? x.getStage() : "recharge",
  );
  setDuring(
    root.querySelector(".duration-subcomponent"),
    itemObj,
    new YearMatcher(new ParsedYear(2), new ParsedYear(10)),
    true,
  );
}

/**
 * Validates and normalizes induction input values.
 *
 * @param {string} rawValue - The raw input value
 * @returns {string|EngineNumber} Normalized induction value
 */
function validateInductionInput(rawValue) {
  if (rawValue === "") {
    throw new Error("Induction rate is required. Please enter a value between 0-100.");
  }

  if (rawValue === "default") {
    return "default";
  }

  const numValue = parseFloat(rawValue);
  if (isNaN(numValue)) {
    throw new Error(`Invalid induction rate: "${rawValue}". Must be a number between 0-100.`);
  }

  if (numValue < 0 || numValue > 100) {
    throw new Error(`Induction rate ${numValue}% is out of range. Must be between 0-100%.`);
  }

  return new EngineNumber(numValue, "%", rawValue.trim());
}

/**
 * Reads values from a recycle command UI widget.
 *
 * @param {HTMLElement} root - The root element containing the UI.
 * @returns {Command} A new Command object with the UI values.
 */
function readRecycleCommandUi(root) {
  const collection = getEngineNumberValue(
    root.querySelector(".recycle-amount-input"),
    root.querySelector(".recycle-units-input"),
  );
  const reuse = getEngineNumberValue(
    root.querySelector(".recycle-reuse-amount-input"),
    root.querySelector(".recycle-reuse-units-input"),
  );

  // Add induction handling with validation
  const inductionRaw = getFieldValue(
    root.querySelector(".recycle-induction-amount-input"),
  );
  const induction = validateInductionInput(inductionRaw);

  const stage = getFieldValue(root.querySelector(".recycle-stage-input"));
  const duration = readDurationUi(root.querySelector(".duration-subcomponent"));

  // RecycleCommand constructor: (target, value, duration, stage, induction)
  return new RecycleCommand(collection, reuse, duration, stage, induction);
}

/**
 * Initializes a replace command UI widget.
 *
 * @param {Object} itemObj - The command object to initialize from.
 * @param {HTMLElement} root - The root element containing the UI.
 * @param {Object} codeObj - The code object containing available substances.
 * @param {string} context - Context for stream detection ('consumption' or 'policy').
 * @param {Object} streamUpdater - StreamSelectionAvailabilityUpdater instance.
 */
function initReplaceCommandUi(itemObj, root, codeObj, context, streamUpdater) {
  const substances = codeObj.getSubstances();
  const substanceNamesDup = substances.map((x) => x.getName());
  const substanceNames = Array.of(...new Set(substanceNamesDup));
  const substanceSelect = d3.select(root.querySelector(".substances-select"));
  substanceSelect.html("");
  substanceSelect
    .selectAll("option")
    .data(substanceNames)
    .enter()
    .append("option")
    .attr("value", (x) => x)
    .text((x) => x);

  setEngineNumberValue(
    root.querySelector(".replace-amount-input"),
    root.querySelector(".replace-units-input"),
    itemObj,
    new EngineNumber(10, "%"),
    (x) => x.getVolume(),
  );

  // Add event listener to update options when substance changes
  const substanceSelectElement = root.querySelector(".substances-select");
  const updateReplaceTargetOptions = () => {
    const replaceTargetSelect = root.querySelector(".replace-target-input");
    const enabledStreams = streamUpdater.getEnabledStreamsForCurrentContext(codeObj, null, context);
    streamUpdater.updateStreamOptionStates(replaceTargetSelect, enabledStreams);
  };
  const updateDisplacingOptions = () => {
    const displacingSelect = root.querySelector(".displacing-input");
    if (displacingSelect) {
      const enabledStreams = streamUpdater.getEnabledStreamsForCurrentContext(
        codeObj, null, context,
      );
      streamUpdater.updateStreamOptionStates(displacingSelect, enabledStreams);
    }
  };
  substanceSelectElement.addEventListener("change", updateReplaceTargetOptions);
  substanceSelectElement.addEventListener("change", updateDisplacingOptions);

  setFieldValue(root.querySelector(".replace-target-input"), itemObj, "sales", (x) =>
    x.getSource(),
  );

  setFieldValue(root.querySelector(".replace-replacement-input"), itemObj, substanceNames[0], (x) =>
    x.getDestination(),
  );

  setDuring(
    root.querySelector(".duration-subcomponent"),
    itemObj,
    new YearMatcher(new ParsedYear(2), new ParsedYear(10)),
    true,
  );

  // Initial update of stream options
  updateReplaceTargetOptions();
  updateDisplacingOptions();
}

/**
 * Reads values from a replace command UI widget.
 *
 * @param {HTMLElement} root - The root element containing the UI.
 * @returns {ReplaceCommand} A new ReplaceCommand object with the UI values.
 */
function readReplaceCommandUi(root) {
  const target = getFieldValue(root.querySelector(".replace-target-input"));
  const amount = getEngineNumberValue(
    root.querySelector(".replace-amount-input"),
    root.querySelector(".replace-units-input"),
  );
  const replacement = getFieldValue(root.querySelector(".replace-replacement-input"));
  const duration = readDurationUi(root.querySelector(".duration-subcomponent"));

  return new ReplaceCommand(amount, target, replacement, duration);
}

/**
 * Reads duration values from a duration UI widget.
 *
 * @param {HTMLElement} root - The root element containing the UI.
 * @returns {YearMatcher} A new YearMatcher object with the UI values.
 */
function readDurationUi(root) {
  const durationType = getFieldValue(root.querySelector(".duration-type-input"));
  const targets = {
    "in year": {min: "duration-start", max: "duration-start"},
    "during all years": {min: null, max: null},
    "starting in year": {min: "duration-start", max: null},
    "ending in year": {min: null, max: "duration-end"},
    "during years": {min: "duration-start", max: "duration-end"},
  }[durationType];
  const getYearValue = (x) => (x === null ? null : root.querySelector("." + x).value);
  const minYear = getYearValue(targets["min"]);
  const maxYear = getYearValue(targets["max"]);
  return new YearMatcher(
    minYear ? new ParsedYear(minYear) : null,
    maxYear ? new ParsedYear(maxYear) : null,
  );
}


export {
  initSetCommandUi,
  readSetCommandUi,
  initChangeCommandUi,
  readChangeCommandUi,
  initLimitCommandUi,
  readLimitCommandUi,
  initRechargeCommandUi,
  readRechargeCommandUi,
  initRecycleCommandUi,
  validateInductionInput,
  readRecycleCommandUi,
  initReplaceCommandUi,
  readReplaceCommandUi,
  readDurationUi,
};
