**The following is an example of a task given to an AI assistant which we assume to be sufficiently specific from a recent PR**

Hello! I want to make some changes to the editor (JS) code.

# File-level feedback
Please call feedback-implementer once per file below with the comments provided. Please go one file at a time. Please tell each feedback-implementer in its invocation to review `tasks/editor_readability.md` before starting work.

Please go one file at a time (no parallel subagents). However, subagents may edit other files when necessary. Please commit after each file.

## duplicate_util.js
Please move `const duplicateLink = document.querySelector(".duplicate-entity-link");` to an instance variable `_duplicateLink` resolved in the constructor.

Please decompose `_setupDialog` into private methods:

 - `_setupEntityType`
 - `_setupSourceEntity`
 - `_setupEquipmentModel`
 - `_setupSaveButton`
 - `_setupCancelButton`

Please remove the extra newline before save button handler if still present after refactor.

Please remove comments as no longer needed:

```
// Generate compound name suggestion
// Run validation checks that might show confirmation dialogs.
// Keep same application name (policy context)
// Clear fields when not substance
```

Please move copy policy array into a private method.

## duration.js
Please move the determination of `self._year` from the constructor into a private method.

Let's remove comment `// If both have finite numeric years, compare numeric values` and, instead, make a local `const` called `bothFinite` equal to `self.hasFiniteNumericYear() && otherParsedYear.hasFiniteNumericYear()`. Structure if statement in `equals` to be `if (bothFinite) { ... } else { ... }` so we can remove `// Otherwise, compare string representations`.

Remove comment `// Create new ParsedYear instances for the rearranged values`.

Let's simplify ternary in `getInRange` by making local `const` for `startIsNumeric` equal to `self._start && self._start.hasFiniteNumericYear()` and similar for `endValue`.

## engine_number.js
Let's re-arrange JSDoc for `makeNumberUnambiguousString` like so:

```
/**
 * Converts a numeric value to an unambiguous US-format string.
 *
 * Ensures numbers are formatted to avoid ambiguity (e.g., 1.234 becomes
 * "1.2340", 1234 becomes "1,234.0"). Used when we need to generate 
 * formatted strings but don't have access to the user's original
 * formatting.
 *
 * @param {number} numericValue - The numeric value to format
 * @returns {string} The unambiguous US-format number string
 */
```

Remove comment `// Allow up to 20 decimal places to preserve precision`.

## engine_struct.js
The constructor for `EngineResult` is far too long. Let's have it take in an `EngineResultBuilder` instance instead and save values to `self._*`. Please update those calling `EngineResult` constructor in JS accordingly and add getters as needed to `EngineResultBuilder`. Similarly, let's have `AggregatedResult` take in two objects which are compatible with `EngineResult` and have `AggregatedResult` take on the logic for combining results (`self._combineUnitValue`) in the constructor instead of having a separate combine method. Please update callers to `combine` to instead mae a new `AggregatedResult`. Ensure files which make `new AggregatedResult` are also updated such as but not limited to `report_data.js`.

## main.js
Please remove check for existence at `// Check if elements exist before setting up listeners`. We should assume they are there.

For `TooltipPresenter`, let's make `_setupWelcomeScreen` and `_setupFooterToggle` and use it to break up `_setupPreferenceControls`. Please move the inline comments into JSDoc.

Move `HELP_TEXT`, `WHITESPACE_REGEX`, and `NEW_FILE_MSG` to top of file.

For `MainPresenter`, let's break up the constructor. Let's make private methods for the following and move inline comments into JSDoc:

 - `_initStorageKeeper`
 - `_initWasmBackend`
 - `_initCodeEditor`
 - `_runInitialSource(source)` for what is in `if (source)`

After doing everything else, this file is too large. Let's create a new file called `editor_actions.js` which contains the following:

 - RunningIndicatorPresenter
 - ButtonPanelPresenter
 - TooltipPresenter

Then, let's make a new file called `informational.js` with the following:

 - IntroductionPresenter
 - PrivacyConfirmationPresenter
 - AIAssistantPresenter
 - AIDesignerPresenter

## meta_serialization.js
Please use line continuation format:

```
result.addError(new SubstanceMetadataError(
	rowNumber,
	"input",
    "Row data must be a Map object",
    "SYSTEM"
));
```

Please check other `addError` calls.

Please add a comment before `if (rowMap)` saying that a valid `rowMap` will be returned or, if not, the error will have been added to result for reporting and should be ignored.

I don't love how we are using exception-based flow in things like `_parseBoolean`. Let's make a new class in `meta_serialization.js` called FieldParseResult that has a `getResult` and `getError` method. In the case of success, let's have the later return null. In the case of the failure, let's have the former return null. Let's also add a `hasError` method.

Move `status success` and `static failure` to to right below constructor.

Let's save `oldName && oldName.trim()` to a local `const` called `oldNameNotFound`

Let's improve readibility in `_ensureApplicationExists` by making and using the following constants:

```
const substances = [];
const isModification = false;
const isCompatible = true;
```

Please remove comments next to parameters in call to `new Application`.

Please move `// Examples: "+1,500.25% kwh / unit", "-42.5 kgCO2e / kg", "10% / year"` and `/^([+-]?[\d,]+(?:\.\d+)?%?)\s+(\S.*)$/` to the top of a file as a constant.

I don't love how we structured `_parseUnitValue`. For each of the branches in the `if (!match)` and `if (throwOnError)` branch, let's move them to a private function such that we replace the inner if / else if structure with:

```
self._checkInfinity(trimmed);
self._checkNumberTrailingSpaces(trimmed);
self._checkMalformedDecimals(trimmed);
self._checkMissingSpace(trimmed);
self._checkNonNumeric(trimmed);
self._throwInvalidGeneric(valueString);
```

Please absorb any inline comments into JSDoc for those new private methods.

Remove comment `// Validate units string is not empty` and instead, make a local `const` called `isEmpty = !unitsStr || !unitsStr.trim()`;

Add `_isInfinityLiteral` to absorb the logic at `// Special check for Infinity before parsing` and please move inline comment to JSDoc.

In `_parseUnitValue`, please move the `isNaN` and `isFinite` check to `_checkJsUnparsable`.

In `_parseUnitValue`, let's move the code at `// Handle percentage units - if original value had %, include it in units` and `// Validate final units string format` into `_parseUnitString` and move inline comment to JSDoc for that new private method.

Let's move `/^([+-]?[\d,]+(?:\.\d+)?%?)\s+(.+)$/` to a constant at the top of file along with comment `// Match number (with optional commas, decimals, and %) followed by units`.

Let's make private method `_handlePercentSignInUnitValue` that absorbs the logic near `// If the original value had a %, include it in the units` and moves the inline comment into JSDoc for the newly created method.

## number_parse_util.js
Let's improve readability of `_parseWithSeparators`. Instead of having these comments, let's remove them and instead have the following local variables:

 - hasCommas
 - hasPeriods
 - bothSeparatorsPresent

Use the above in if statements instead. May keep ` // Shouldn't reach here based on calling conditions`.

Please remove the following comments from `_parseMixedSeparators`:

```
// Period comes after comma - US format (period as decimal)
// Check if there are any periods before the last comma (invalid in US format)
// Validate US format: comma(s) for thousands, period for decimal
// Format: 123,456.78 - comma is thousands, period is decimal
// Comma comes after period - European format → ERRO
```

Instead, let's make the following local `const` variables and use them in if statements:

 - usesUsFormat
 - multiplePeriods
 - periodsBeforeComma

In `_parseSingleSeparatorType`, please decompose this method by making:

 - `_parseSingleSeparatorMultipleOccurrences`
 - `_parseSingleSeparatorSingleOccurrence`
 - `_parseStartsWithSeparator`
 - `_parseUnambiguousPriorDigits` (use for `digitsBefore >= 4`)
 - `_parseWithLeadingZero`
 - `_interpretAmbiguous`

Please organize in an if / else to call those new private methods which should absorb the current logic. Please do not use the arrow character (remove it). Please move inline comments to JSDoc where possible.

Please move this into JSDoc:

```
    // Handle different European patterns:
    // "1.234,56" → "1,234.56"
    // "123,45" → "123.45"
    // "1.234.567,89" → "1,234,567.89"
```

Please remove these comments:

```
// European mixed format: periods for thousands, comma for decimal
// Single comma as European decimal
```

Instead, please have:

 - `const europeanMixed = lastCommaIndex > lastPeriodIndex && lastCommaIndex !== -1`
 - `const signleComma = lastCommaIndex !== -1 && lastPeriodIndex === -1`

Please remove `// Fallback`

## results.js
Please update the line continuation style as follows:

```
self._updateCard(
	equipmentScorecard,
	millionEqipment,
	currentYear,
    equipmentSelected,
    hideVal
);
```

## ui_editor.js
There are a lot of loose functions in here not part of classes. Please move those to a new file `ui_editor_util.js`. 

## ui_translator.js
Let's break `ui_translator.js` up into a few files. Let's leave `UiTranslatorCompiler` and `TranslatorVisitor` here. However, let's put the following in `ui_translator_components.js`:

 - AboutStanza
 - Application
 - Command
 - DefinitionalStanza
 - LimitCommand
 - Program
 - RechargeCommand
 - RecycleCommand
 - ReplaceCommand
 - RetireCommand
 - SimulationScenario
 - SimulationStanza
 - Substance
 - SubstanceBuilder
 - SubstanceMetadata
 - SubstanceMetadataBuilder

Let's put loose functions into `ui_translator_util.js`.

## updates.js
Except for `versionInput` and checks for browser compatibility (like `"serviceWorker" in navigator`), please remove checks for if the elements exist such as for `upToDateMessage`. Just assume they are there. This includes `reloadButton` and `continueButton`.

## wasm_backend.js
Please make `ReportDataParser` non-static. Let's have actual instantiations of the class. Please see other files where it is used to update.

Please move `parseEngineNumber` to a private method `_parseEngineNumber`. Please have `const hasUnitsStr = parts.length >= 2` and use in if statement.

Let's move the contents of `if (allComplete)` into a private method `_resolveCompleteRequests`.

Let's break up `execute`. First, lets save `const noScenarios = !scenarioNames || scenarioNames.length === 0`. Then, let's have an if / else structure. In the true side for `if (noScenarios)` let's call `_repsondToNoScenarios`. For the else, let's call `_runScenarios`. Let's absorb the inline comments into the JSDoc for these newly created methods.

# Broader feedbaack
For the following files, please make a markdown "task file" in the tasks directory. Then, call component-planner (reminding it to only update the task markdown file) followed by component-implementer (reminding it to ignore lint issues) followed by component-validator. Please give them the full path to the task file and tell them that the task is a single component. Please also ask all subagents to read `tasks/editor_readability.md`. Please commit after each task.

## report_data refactor
In `report_data.js`, let's see if we can move methods like `addBankStrategies` to private methods on the class with separate JSDoc. To keep it expedient, we can have a `const addEmissionsConversionCurry = (x) => addEmissionsConversion(x, ...)` and similar. Please do this in a way that does not change external behavior.

## results constrain filter set refactor
In `results.js`, let's break up the `self._results !== null` branch

First, let's have a private method on the class to `_constrainFilterSet`. Let's have that call a series of private methods as follows which return the filter set to use:

 - `_checkScenarioExists`
 - `_checkApplicationExists`
 - `_checkSubstanceExists`
 - `_validateCustomMetrics`

Move inline comments to JSDoc where possible for these new methods.

Remove comment `// Original constraint logic to avoid difficult to interpret charts` and make a `const `

Please do this in a way that does not change external behavior.

## results show unsafe refactor
In `results.js`, let's break up `showUnsafe` by making private methods on `ScorecardPresenter`:

 - `_getDropdownMetricStrategy`
 - `_updateDropdownMenus`
 - `_safeGetValue`