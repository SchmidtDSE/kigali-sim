/**
 * Structures to represent internal state within the engine.
 *
 * @license BSD, see LICENSE.md.
 */

import {
  GLOBAL_CONTEXT,
  STANZA_CONTEXT,
  APPLICATION_CONTEXT,
  SUBSTANCE_CONTEXT,
  STREAM_BASE_UNITS,
} from "engine_const";
import {EngineNumber} from "engine_number";
import {UnitConverter} from "engine_unit";
import {OverridingConverterStateGetter} from "engine_unit_state";

const CHECK_NAN_STATE = true;
const CHECK_POSITIVE_STREAMS = true;

/**
 * Class representing a range of years where inclusion can be tested.
 */
class YearMatcher {
  /**
   * Create a new year range.
   *
   * Create a new year range between start and end where null in either means positive or negative
   * infinity.
   *
   * @param start The starting year (inclusive) in this range or null if no min year.
   * @param end The ending year (inclusive) in this range or null if no max year.
   */
  constructor(start, end) {
    const self = this;

    const hasNull = start === null || end === null;
    const startHasSpecial = start === "beginning" || start === "onwards";
    const endHasSpecial = end === "beginning" || end === "onwards";

    if (hasNull || startHasSpecial || endHasSpecial) {
      self._start = start;
      self._end = end;
    } else {
      const startRearrange = Math.min(start, end);
      const endRearrange = Math.max(start, end);

      self._start = startRearrange;
      self._end = endRearrange;
    }
  }

  /**
   * Determine if a year is included in this range.
   *
   * @param year The year to test for inclusion.
   * @returns True if this value is between getStart and getEnd.
   */
  getInRange(year) {
    const self = this;
    const meetsMin = self._start === null || self._start <= year;
    const meetsMax = self._end === null || self._end >= year;
    return meetsMin && meetsMax;
  }

  /**
   * Get the start of the year range.
   *
   * @returns The minimum included year in this range or null if negative infinity.
   */
  getStart() {
    const self = this;
    return self._start;
  }

  /**
   * Get the end of the year range.
   *
   * @returns The maximum included year in this range or null if positive infinity.
   */
  getEnd() {
    const self = this;
    return self._end;
  }
}

/**
 * Internal object which manages user defined variables at different scopes.
 */
class VariableManager {
  /**
   * Create a new variable manager.
   *
   * @param contextLevel The context level constant at which this manager will be used.
   * @param globalContext Map from name of variable to value or undefined if no variables exist at
   *    that global level.
   * @param stanzaContext Map from name of variable to value or undefined if no variables exist at
   *    that stanza level.
   * @param applicationContext Map from name of variable to value or undefined if no variables
   *    exist at that application level.
   * @param substanceContext Map from name of variable to value or undefined if no variables exist
   *    at that substance level.
   */
  constructor(contextLevel, globalContext, stanzaContext, applicationContext, substanceContext) {
    const self = this;

    const ensureContext = (x) => (x === undefined ? new Map() : x);

    self._globalContext = ensureContext(globalContext);
    self._stanzaContext = ensureContext(stanzaContext);
    self._applicationContext = ensureContext(applicationContext);
    self._substanceContext = ensureContext(substanceContext);
    self._contextLevel = contextLevel;
  }

  /**
   * Make a new variable manager occupying this namespace but at a different context level.
   *
   * @param contextLevel Constant describing the new context level. If this matches the current
   *    context level, it is assumed adjacent to the current context.
   * @returns VariableManager at the given context level.
   */
  getWithLevel(contextLevel) {
    const self = this;
    if (contextLevel < GLOBAL_CONTEXT || contextLevel > SUBSTANCE_CONTEXT) {
      throw "Unexpected context level: " + contextLevel;
    }

    let newStanzaContext = self._stanzaContext;
    if (contextLevel <= STANZA_CONTEXT) {
      newStanzaContext = new Map();
    }

    let newApplicationContext = self._applicationContext;
    if (contextLevel <= APPLICATION_CONTEXT) {
      newApplicationContext = new Map();
    }

    let newSubstanceContext = self._substanceContext;
    if (contextLevel <= SUBSTANCE_CONTEXT) {
      newSubstanceContext = new Map();
    }

    return new VariableManager(
      contextLevel,
      self._globalContext,
      newStanzaContext,
      newApplicationContext,
      newSubstanceContext,
    );
  }

  /**
   * Define a new variable in the current context level.
   *
   * Define a new variable in the current context level where an error will be thrown if a variable
   * of the same name exists at this context level.
   *
   * @param name The name of the variable to define.
   */
  defineVariable(name) {
    const self = this;
    const context = self._getContextForLevel(self._contextLevel);

    if (context.has(name)) {
      throw "Variable already defined in this scope: " + name;
    }

    context.set(name, null);
  }

  /**
   * Set the value of a variable already defined.
   *
   * Set the value of a variable already defined where an error will be thrown if a variable of
   * this name has not been defined or is not accessible from the current scope.
   *
   * @param name The name of the variable to be set.
   * @param value The new value of the variable.
   */
  setVariable(name, value) {
    const self = this;

    let currentContext = null;
    for (let level = self._contextLevel; level >= GLOBAL_CONTEXT; level--) {
      currentContext = self._getContextForLevel(level);
      if (currentContext.has(name)) {
        currentContext.set(name, value);
        return;
      }
    }

    throw "Unable to find variable to set: " + name;
  }

  /**
   * Get the value of a variable already defined.
   *
   * Get the value of a variable already defined such that an error will be thrown if a variable of
   * this name has not been defined or is not accessible from the current scope.
   *
   * @param name The name of the variable to be set.
   * @returns Current value of this variable.
   */
  getVariable(name) {
    const self = this;

    let currentContext = null;
    for (let level = self._contextLevel; level >= GLOBAL_CONTEXT; level--) {
      currentContext = self._getContextForLevel(level);
      if (currentContext.has(name)) {
        return currentContext.get(name);
      }
    }

    throw "Unable to find variable to read: " + name;
  }

  /**
   * Get the variable map for a certain context level.
   *
   * @param level Constant corresponding to context level.
   * @returns Map from name of variable to value at the given context level.
   */
  _getContextForLevel(level) {
    const self = this;
    const levelContexts = [
      self._globalContext,
      self._stanzaContext,
      self._applicationContext,
      self._substanceContext,
    ];
    return levelContexts[level];
  }
}

/**
 * Object defining a scope within the engine including variables accessible from that scope.
 */
class Scope {
  /**
   * Create a new scope.
   *
   * @param stanza The name of stanza or null if in global scope.
   * @param application The name of the application or null if in stanza or higher scope.
   * @param substance The name of the substance or null if in application or higher scope.
   * @param variableManager The variable manager to reach variables accessible from this scope or
   *    undefined if no variables accessible.
   */
  constructor(stanza, application, substance, variableManager) {
    const self = this;
    self._stanza = stanza;
    self._application = application;
    self._substance = substance;

    if (self._substance !== null && self._application === null) {
      throw "Cannot specify substance without application.";
    }

    if (self._application !== null && self._stanza == null) {
      throw "Cannot specify application without stanza.";
    }

    if (variableManager === undefined) {
      const components = [self._stanza, self._application, self._substance];
      const contextLevel = components
        .filter((x) => x !== null)
        .map((x) => 1)
        .reduce((a, b) => a + b, 0);

      variableManager = new VariableManager(contextLevel);
    }

    self._variableManager = variableManager;
  }

  /**
   * Get the name of the stanza where this scope resides.
   *
   * @return The name of the current stanza or null if in global scope.
   */
  getStanza() {
    const self = this;
    return self._stanza;
  }

  /**
   * Get the name of the application where this scope resides.
   *
   * @return The name of the current application or null if in stanza or higher scope.
   */
  getApplication() {
    const self = this;
    return self._application;
  }

  /**
   * Get the name of the substance where this scope resides.
   *
   * @return The name of the current substance or null if in application or higher scope.
   */
  getSubstance() {
    const self = this;
    return self._substance;
  }

  /**
   * Create a new scope derived from this scope at the substance level.
   *
   * @param newSubstance The name of the substance in which the new scope resides.
   * @returns New scope at the given substance.
   */
  getWithSubstance(newSubstance) {
    const self = this;

    if (self._application === null) {
      throw "Not able to set substance without application.";
    }

    return new Scope(
      self._stanza,
      self._application,
      newSubstance,
      self._variableManager.getWithLevel(SUBSTANCE_CONTEXT),
    );
  }

  /**
   * Create a new scope derived from this scope at the application level.
   *
   * @param newSubstance The name of the application in which the new scope resides.
   * @returns New scope at the given application.
   */
  getWithApplication(newApplication) {
    const self = this;

    if (self._stanza === null) {
      throw "Not able to set substance without stanza.";
    }

    return new Scope(
      self._stanza,
      newApplication,
      null,
      self._variableManager.getWithLevel(APPLICATION_CONTEXT),
    );
  }

  /**
   * Create a new scope derived from this scope at the stanza level.
   *
   * @param newSubstance The name of the stanza in which the new scope resides.
   * @returns New scope at the given stanza.
   */
  getWithStanza(newStanza) {
    const self = this;

    return new Scope(newStanza, null, null, self._variableManager.getWithLevel(STANZA_CONTEXT));
  }

  /**
   * Define a variable in the current scope.
   *
   * Define a new variable in the current scope or throw an error if a variable of this name
   * already exists in this scope at the current context level.
   *
   * @param name The name of the variable to define.
   */
  defineVariable(name) {
    const self = this;
    self._variableManager.defineVariable(name);
  }

  /**
   * Set the value of a variable already defined.
   *
   * Set the value of a variable or throw an error if no variable of the given name is accessible
   * from the current scope.
   *
   * @param name The name of the variable to set.
   * @param value The new value of the variable.
   */
  setVariable(name, value) {
    const self = this;
    self._variableManager.setVariable(name, value);
  }

  /**
   * Get the value of a variable already defined.
   *
   * Get the value of a variable or throw an error if no variable of the given name is accessible
   * from the current scope.
   *
   * @param name The name of the variable to get.
   * @returns Value of the given variable.
   */
  getVariable(name) {
    const self = this;
    return self._variableManager.getVariable(name);
  }
}

/**
 * Class for managing stream-specific parameters and settings.
 * Handles configuration of GHG intensity, initial charge, recharge rates,
 * recovery rates, and other stream-specific values.
 */
class StreamParameterization {
  /**
   * Create a new stream parameterization instance.
   */
  constructor() {
    const self = this;
    self.resetInternals();
  }

  /**
   * Reset all internal parameters to their default values.
   */
  resetInternals() {
    const self = this;
    const createZero = (x) => new EngineNumber(0, x);
    const createNominal = (x) => new EngineNumber(1, x);
    self._ghgIntensity = createZero("tCO2e / kg");
    self._energyIntensity = createZero("kwh / kg");
    self._initialCharge = {
      manufacture: createNominal("kg / unit"),
      import: createNominal("kg / unit"),
    };
    self._rechargePopulation = createZero("%");
    self._rechargeIntensity = createZero("kg / unit");
    self._recoveryRate = createZero("%");
    self._yieldRate = createZero("%");
    self._retirementRate = createZero("%");
    self._displacementRate = new EngineNumber(100, "%");
  }

  /**
   * Set the greenhouse gas intensity.
   *
   * @param {EngineNumber} newValue - The new GHG intensity value.
   */
  setGhgIntensity(newValue) {
    const self = this;
    self._ghgIntensity = newValue;
  }

  /**
   * Get the greenhouse gas intensity.
   *
   * @returns {EngineNumber} The current GHG intensity value.
   */
  getGhgIntensity() {
    const self = this;
    return self._ghgIntensity;
  }

  /**
   * Set the energy intensity.
   *
   * @param {EngineNumber} newValue - The new energy intensity value.
   */
  setEnergyIntensity(newValue) {
    const self = this;
    self._energyIntensity = newValue;
  }

  /**
   * Get the energy intensity.
   *
   * @returns {EngineNumber} The current energy intensity value.
   */
  getEnergyIntensity() {
    const self = this;
    return self._energyIntensity;
  }

  /**
   * Set the initial charge for a stream.
   *
   * @param {string} stream - The stream identifier ('manufacture' or 'import').
   * @param {EngineNumber} newValue - The new initial charge value.
   */
  setInitialCharge(stream, newValue) {
    const self = this;
    self._ensureSalesStreamAllowed(stream);
    self._initialCharge[stream] = newValue;
  }

  /**
   * Get the initial charge for a stream.
   *
   * @param {string} stream - The stream identifier ('manufacture' or 'import').
   * @returns {EngineNumber} The initial charge value for the stream.
   */
  getInitialCharge(stream) {
    const self = this;
    self._ensureSalesStreamAllowed(stream);
    return self._initialCharge[stream];
  }

  /**
   * Set the recharge population percentage.
   *
   * @param {EngineNumber} newValue - The new recharge population value.
   */
  setRechargePopulation(newValue) {
    const self = this;
    self._rechargePopulation = newValue;
  }

  /**
   * Get the recharge population percentage.
   *
   * @returns {EngineNumber} The current recharge population value.
   */
  getRechargePopulation() {
    const self = this;
    return self._rechargePopulation;
  }

  /**
   * Set the recharge intensity.
   *
   * @param {EngineNumber} newValue - The new recharge intensity value.
   */
  setRechargeIntensity(newValue) {
    const self = this;
    self._rechargeIntensity = newValue;
  }

  /**
   * Get the recharge intensity.
   *
   * @returns {EngineNumber} The current recharge intensity value.
   */
  getRechargeIntensity() {
    const self = this;
    return self._rechargeIntensity;
  }

  /**
   * Set the recovery rate percentage.
   *
   * @param {EngineNumber} newValue - The new recovery rate value.
   */
  setRecoveryRate(newValue) {
    const self = this;
    self._recoveryRate = newValue;
  }

  /**
   * Get the recovery rate percentage.
   *
   * @returns {EngineNumber} The current recovery rate value.
   */
  getRecoveryRate() {
    const self = this;
    return self._recoveryRate;
  }

  /**
   * Set the yield rate percentage for recycling.
   *
   * @param {EngineNumber} newValue - The new yield rate value.
   */
  setYieldRate(newValue) {
    const self = this;
    self._yieldRate = newValue;
  }

  /**
   * Get the yield rate percentage for recycling.
   *
   * @returns {EngineNumber} The current yield rate value.
   */
  getYieldRate() {
    const self = this;
    return self._yieldRate;
  }

  /**
   * Set the displacement rate percentage.
   *
   * @param {EngineNumber} newValue - The new displacement rate value.
   */
  setDisplacementRate(newValue) {
    const self = this;
    self._displacementRate = newValue;
  }

  /**
   * Get the displacement rate percentage.
   *
   * @returns {EngineNumber} The current displacement rate value.
   */
  getDisplacementRate() {
    const self = this;
    return self._displacementRate;
  }

  /**
   * Set the retirement rate percentage.
   *
   * @param {EngineNumber} newValue - The new retirement rate value.
   */
  setRetirementRate(newValue) {
    const self = this;
    self._retirementRate = newValue;
  }

  /**
   * Get the retirement rate percentage
   * .
   * @returns {EngineNumber} The current retirement rate value.
   */
  getRetirementRate() {
    const self = this;
    return self._retirementRate;
  }

  /**
   * Validate that the given stream name is allowed for sales operations.
   *
   * @private
   * @param {string} name - The stream name to validate.
   * @throws {Error} If the stream name is not a sales substream.
   */
  _ensureSalesStreamAllowed(name) {
    const self = this;
    if (name !== "manufacture" && name !== "import" && name !== "recycle") {
      throw "Must address a sales substream.";
    }
  }
}

/**
 * Class representing a unique identifier for a substance within a specific application.
 */
class SubstanceInApplicationId {
  /**
   * Create a new substance-in-application identifier.
   *
   * @param {string} application - The name of the application (e.g., "domestic refrigeration").
   * @param {string} substance - The name of the substance (e.g., "HFC-134a").
   */
  constructor(application, substance) {
    const self = this;
    self._application = application;
    self._substance = substance;
  }

  /**
   * Get the name of the application.
   *
   * @returns {string} The application name associated with this identifier.
   */
  getApplication() {
    const self = this;
    return self._application;
  }

  /**
   * Get the name of the substance.
   *
   * @returns {string} The substance name associated with this identifier.
   */
  getSubstance() {
    const self = this;
    return self._substance;
  }
}

/**
 * Class responsible for managing / tracking substance streams.
 *
 * State management object for storage and retrieval of substance data, stream
 * values, and associated parameterizations.
 */
class StreamKeeper {
  /**
   * Create a new StreamKeeper instance.
   *
   * @param {ConverterStateGetter} stateGetter - Structure to retrieve state information.
   * @param {UnitConverter} unitConverter - Converter for handling unit transformations.
   */
  constructor(stateGetter, unitConverter) {
    const self = this;
    self._substances = new Map();
    self._streams = new Map();
    self._stateGetter = stateGetter;
    self._unitConverter = unitConverter;
  }

  /**
   * Get all registered substance-application pairs.
   *
   * @returns {SubstanceInApplicationId[]} Array of substance identifiers.
   */
  getRegisteredSubstances() {
    const self = this;
    const keys = Array.of(...self._substances.keys());
    const keyComponents = keys.map((x) => x.split("\t"));
    const substanceIds = keyComponents.map((x) => {
      return new SubstanceInApplicationId(x[0], x[1]);
    });
    return substanceIds;
  }

  /**
   * Check if a substance exists for an application.
   *
   * @param {string} application - The application name.
   * @param {string} substance - The substance name.
   * @returns {boolean} True if the substance exists for the application.
   */
  hasSubstance(application, substance) {
    const self = this;
    const key = self._getKey(application, substance);
    return self._substances.has(key);
  }

  /**
   * Ensure a substance exists for an application, creating it if needed.
   *
   *
   * @param {string} application - The application name.
   * @param {string} substance - The substance name to initialize with zero
   *     values.
   */
  ensureSubstance(application, substance) {
    const self = this;

    if (self.hasSubstance(application, substance)) {
      return;
    }

    const key = self._getKey(application, substance);
    self._substances.set(key, new StreamParameterization());

    const makeZero = (units) => new EngineNumber(0, units);

    // Sales: manufacture, import, recycle
    self._streams.set(self._getKey(application, substance, "manufacture"), makeZero("kg"));
    self._streams.set(self._getKey(application, substance, "import"), makeZero("kg"));
    self._streams.set(self._getKey(application, substance, "recycle"), makeZero("kg"));

    // Consumption: count, conversion
    self._streams.set(self._getKey(application, substance, "consumption"), makeZero("tCO2e"));

    // Population
    self._streams.set(self._getKey(application, substance, "equipment"), makeZero("units"));
    self._streams.set(self._getKey(application, substance, "priorEquipment"), makeZero("units"));
    self._streams.set(self._getKey(application, substance, "newEquipment"), makeZero("units"));

    // Emissions
    self._streams.set(self._getKey(application, substance, "rechargeEmissions"), makeZero("tCO2e"));
    self._streams.set(self._getKey(application, substance, "eolEmissions"), makeZero("tCO2e"));
  }

  /**
   * Set the value for a specific stream.
   *
   * @param {string} application - The application name.
   * @param {string} substance - The substance name.
   * @param {string} name - The stream name.
   * @param {EngineNumber} value - The value to set.
   */
  setStream(application, substance, name, value) {
    const self = this;
    self._ensureSubstancePresent(application, substance, "setStream");
    self._ensureStreamKnown(name);

    if (CHECK_NAN_STATE && isNaN(value.getValue())) {
      const pieces = [application, substance, name];
      const piecesStr = pieces.join(" > ");
      throw new Error("Encountered NaN to be set for: " + piecesStr);
    }

    if (name === "sales") {
      self._setStreamForSales(application, substance, name, value);
    } else if (self._getIsSettingVolumeByUnits(name, value)) {
      self._setStreamForSalesComponent(application, substance, name, value);
    } else {
      self._setSimpleStream(application, substance, name, value);
    }
  }

  /**
   * Get the value of a specific stream.
   *
   * @param {string} application - The application name.
   * @param {string} substance - The substance name.
   * @param {string} name - The stream name.
   * @returns {EngineNumber} The stream value.
   */
  getStream(application, substance, name) {
    const self = this;
    self._ensureSubstancePresent(application, substance, "getStream");
    self._ensureStreamKnown(name);

    if (name === "sales") {
      const manufactureAmountRaw = self.getStream(application, substance, "manufacture");
      const importAmountRaw = self.getStream(application, substance, "import");

      const manufactureAmount = self._unitConverter.convert(manufactureAmountRaw, "kg");
      const importAmount = self._unitConverter.convert(importAmountRaw, "kg");

      const manufactureAmountValue = manufactureAmount.getValue();
      const importAmountValue = importAmount.getValue();

      const newTotal = manufactureAmountValue + importAmountValue;

      return new EngineNumber(newTotal, "kg");
    } else {
      return self._streams.get(self._getKey(application, substance, name));
    }
  }

  /**
   * Check if a stream exists for a substance-application pair.
   *
   * @param {string} application - The application name.
   * @param {string} substance - The substance name.
   * @param {string} name - The stream name.
   * @returns {boolean} True if the stream exists.
   */
  isKnownStream(application, substance, name) {
    const self = this;
    return self._streams.has(self._getKey(application, substance, name));
  }

  /**
   * Increment the year, updating populations and resetting internal params.
   */
  incrementYear() {
    const self = this;

    // Move population
    const allKeys = Array.from(self._substances.keys());
    allKeys.forEach((key) => {
      const keyPieces = key.split("\t");
      const application = keyPieces[0];
      const substance = keyPieces[1];

      const equipment = self.getStream(application, substance, "equipment");

      self.setStream(application, substance, "priorEquipment", equipment);

      self._substances.get(key).resetInternals();
    });
  }

  /**
   * Set the greenhouse gas intensity for a substance in an application.
   *
   * @param {string} application - The application name.
   * @param {string} substance - The substance name.
   * @param {EngineNumber} newValue - The new GHG intensity value.
   */
  setGhgIntensity(application, substance, newValue) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    parameterization.setGhgIntensity(newValue);
  }

  /**
   * Set the energy intensity for a substance in an application.
   *
   * @param {string} application - The application name.
   * @param {string} substance - The substance name.
   * @param {EngineNumber} newValue - The new energy intensity value.
   */
  setEnergyIntensity(application, substance, newValue) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    parameterization.setEnergyIntensity(newValue);
  }

  /**
   * Get the greenhouse gas intensity for a substance in an application.
   *
   * @param {string} application - The application name.
   * @param {string} substance - The substance name.
   * @returns {EngineNumber} The current GHG intensity value.
   */
  getGhgIntensity(application, substance) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    return parameterization.getGhgIntensity();
  }

  /**
   * Get the energy intensity for a substance in an application.
   *
   * @param {string} application - The application name.
   * @param {string} substance - The substance name.
   * @returns {EngineNumber} The current energy intensity value.
   */
  getEnergyIntensity(application, substance) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    return parameterization.getEnergyIntensity();
  }

  /**
   * Set the initial charge for a substance in an application's stream.
   *
   * @param {string} application - The application name.
   * @param {string} substance - The substance name.
   * @param {string} substream - The stream identifier ('manufacture' or 'import').
   * @param {EngineNumber} newValue - The new initial charge value.
   */
  setInitialCharge(application, substance, substream, newValue) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    parameterization.setInitialCharge(substream, newValue);
  }

  /**
   * Get the initial charge for a substance in an application's stream.
   *
   * @param {string} application - The application name.
   * @param {string} substance - The substance name.
   * @param {string} substream - The stream identifier ('manufacture' or 'import').
   * @returns {EngineNumber} The current initial charge value.
   */
  getInitialCharge(application, substance, substream) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    return parameterization.getInitialCharge(substream);
  }

  /**
   * Set the recharge population percentage for a substance in an application.
   *
   * @param {string} application - The application name.
   * @param {string} substance - The substance name.
   * @param {EngineNumber} newValue - The new recharge population value.
   */
  setRechargePopulation(application, substance, newValue) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    parameterization.setRechargePopulation(newValue);
  }

  /**
   * Get the recharge population percentage for a substance in an application.
   *
   * @param {string} application - The application name.
   * @param {string} substance - The substance name.
   * @returns {EngineNumber} The current recharge population value.
   */
  getRechargePopulation(application, substance) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    return parameterization.getRechargePopulation();
  }

  /**
   * Set the recharge intensity for a substance in an application.
   *
   * @param {string} application - The application name.
   * @param {string} substance - The substance name.
   * @param {EngineNumber} newValue - The new recharge intensity value.
   */
  setRechargeIntensity(application, substance, newValue) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    parameterization.setRechargeIntensity(newValue);
  }

  /**
   * Get the recharge intensity for a substance in an application.
   *
   * @param {string} application - The application name.
   * @param {string} substance - The substance name.
   * @returns {EngineNumber} The current recharge intensity value.
   */
  getRechargeIntensity(application, substance) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    return parameterization.getRechargeIntensity();
  }

  /**
   * Set the recovery rate percentage for a substance in an application.
   *
   * @param {string} application - The application name.
   * @param {string} substance - The substance name.
   * @param {EngineNumber} newValue - The new recovery rate value.
   */
  setRecoveryRate(application, substance, newValue) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    parameterization.setRecoveryRate(newValue);
  }

  /**
   * Get the recovery rate percentage for a substance in an application.
   *
   * @param {string} application - The application name.
   * @param {string} substance - The substance name.
   * @returns {EngineNumber} The current recovery rate value.
   */
  getRecoveryRate(application, substance) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    return parameterization.getRecoveryRate();
  }

  /**
   * Set the displacement rate percentage for a substance in an application.
   *
   * @param {string} application - The application name.
   * @param {string} substance - The substance name.
   * @param {EngineNumber} newValue - The new displacement rate value.
   */
  setDisplacementRate(application, substance, newValue) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    parameterization.setDisplacementRate(newValue);
  }

  /**
   * Get the displacement rate percentage for a substance in an application.
   *
   * @param {string} application - The application name.
   * @param {string} substance - The substance name.
   * @returns {EngineNumber} The current displacement rate value.
   */
  getDisplacementRate(application, substance) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    return parameterization.getDisplacementRate();
  }

  /**
   * Set the yield rate percentage for recycling a substance in an application.
   *
   * @param {string} application - The application name.
   * @param {string} substance - The substance name.
   * @param {EngineNumber} newValue - The new yield rate value.
   */
  setYieldRate(application, substance, newValue) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    parameterization.setYieldRate(newValue);
  }

  /**
   * Get the yield rate percentage for recycling a substance in an application.
   *
   * @param {string} application - The application name.
   * @param {string} substance - The substance name.
   * @returns {EngineNumber} The current yield rate value.
   */
  getYieldRate(application, substance) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    return parameterization.getYieldRate();
  }

  /**
   * Set the retirement rate percentage for a substance in an application.
   *
   * @param {string} application - The application name.
   * @param {string} substance - The substance name.
   * @param {EngineNumber} newValue - The new retirement rate value.
   */
  setRetirementRate(application, substance, newValue) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    parameterization.setRetirementRate(newValue);
  }

  /**
   * Get the retirement rate percentage for a substance in an application.
   *
   * @param {string} application - The application name.
   * @param {string} substance - The substance name.
   * @returns {EngineNumber} The current retirement rate value.
   */
  getRetirementRate(application, substance) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    return parameterization.getRetirementRate();
  }

  /**
   * Retrieve parameterization for a specific application and substance.
   *
   * Verifies the existence of the substance and application combination
   * and returns the associated StreamParameterization object.
   *
   * @private
   * @param {string} application - The name of the application.
   * @param {string} substance - The name of the substance.
   * @returns {StreamParameterization} The parameterization for the given application and substance.
   */
  _getParameterization(application, substance) {
    const self = this;
    self._ensureSubstancePresent(application, substance, "getParameterization");
    const key = self._getKey(application, substance);
    return self._substances.get(key);
  }

  /**
   * Generate a key identifying a stream within a substance and application.
   *
   * @private
   * @param {string} application - The application name.
   * @param {string} substance - The substance name.
   * @param {string} [name] - The stream name.
   * @param {string} [substream] - The substream identifier.
   * @returns {string} The generated key.
   */
  _getKey(application, substance, name, substream) {
    const self = this;
    const pieces = [application, substance, name, substream];
    const getIsNotGiven = (x) => x === null || x === undefined;
    const piecesSafe = pieces.map((x) => (getIsNotGiven(x) ? "-" : x + ""));
    return piecesSafe.join("\t");
  }

  /**
   * Verify that a substance exists for an application.
   *
   * @private
   * @param {string} application - The application name.
   * @param {string} substance - The substance name.
   * @param {string} context - The context for error reporting.
   * @throws {string} If the substance does not exist for the application.
   */
  _ensureSubstancePresent(application, substance, context) {
    const self = this;

    if (!self.hasSubstance(application, substance)) {
      const pairStr = application + ", " + substance;
      throw "Not a known application substance pair in " + context + ": " + pairStr;
    }
  }

  /**
   * Verify that a stream name is valid.
   *
   * @private
   * @param {string} name - The stream name to verify.
   * @throws {string} If the stream name is not recognized.
   */
  _ensureStreamKnown(name) {
    const self = this;
    if (!STREAM_BASE_UNITS.has(name)) {
      throw "Unknown stream: " + name;
    }
  }

  /**
   * Get the base units for a stream.
   *
   * @private
   * @param {string} name - The stream name.
   * @returns {string} The base units for the stream.
   */
  _getUnits(name) {
    const self = this;
    self._ensureStreamKnown(name);
    return STREAM_BASE_UNITS.get(name);
  }

  /**
   * Handle setting the sales stream for an application and substance.
   *
   * Handle setting the sales stream which has two substreams (manufacture and import) which both
   * require modification.
   *
   * @private
   * @param {string} application - The application name.
   * @param {string} substance - The substance name.
   * @param {string} name - The stream name.
   * @param {EngineNumber} value - The value to set.
   */
  _setStreamForSales(application, substance, name, value) {
    const self = this;
    const manufactureValueRaw = self.getStream(application, substance, "manufacture");
    const importValueRaw = self.getStream(application, substance, "import");

    const manufactureValue = self._unitConverter.convert(manufactureValueRaw, "kg");
    const importValue = self._unitConverter.convert(importValueRaw, "kg");

    const manufactureAmount = manufactureValue.getValue();
    const importAmount = importValue.getValue();

    const valueConverted = self._unitConverter.convert(value, "kg");
    const amountKg = valueConverted.getValue();

    const totalAmount = manufactureAmount + importAmount;
    const isZero = totalAmount == 0;
    const manufacturePercent = isZero ? 0.5 : manufactureAmount / totalAmount;
    const importPercent = isZero ? 0.5 : importAmount / totalAmount;

    const manufactureShare = amountKg * manufacturePercent;
    const importShare = amountKg * importPercent;
    const manufactureNewValue = new EngineNumber(manufactureShare, value.getUnits());
    const importNewValue = new EngineNumber(importShare, value.getUnits());

    self.setStream(application, substance, "manufacture", manufactureNewValue);
    self.setStream(application, substance, "import", importNewValue);
  }

  /**
   * Determine if the user is setting a sales component (manufacture / import) by units.
   *
   * @private
   * @param {string} name - The stream name.
   * @param {EngineNumber} value - The value to set.
   * @returns {boolean} true if the user is setting a sales component by units and false otherwise.
   */
  _getIsSettingVolumeByUnits(name, value) {
    const self = this;
    const isSalesComponent = name === "manufacture" || name === "import";
    const isUnits = value.getUnits().startsWith("unit");
    return isSalesComponent && isUnits;
  }

  /**
   * Handle setting a stream which only requires simple unit conversion.
   *
   * @private
   * @param {string} application - The application name.
   * @param {string} substance - The substance name.
   * @param {string} name - The stream name.
   * @param {EngineNumber} value - The value to set.
   */
  _setSimpleStream(application, substance, name, value) {
    const self = this;
    const unitsNeeded = self._getUnits(name);
    const valueConverted = self._unitConverter.convert(value, unitsNeeded);

    if (CHECK_NAN_STATE && isNaN(valueConverted.getValue())) {
      const pieces = [application, substance, name];
      const piecesStr = pieces.join(" > ");
      throw new Error("Encountered NaN after conversion to be set for: " + piecesStr);
    }

    if (CHECK_POSITIVE_STREAMS && valueConverted.getValue() < 0) {
      const pieces = [application, substance, name];
      const piecesStr = pieces.join(" > ");
      throw new Error("Encountered negative stream to be set for: " + piecesStr);
    }

    self._streams.set(self._getKey(application, substance, name), valueConverted);
  }

  /**
   * Handle setting volume by units for sales components.
   *
   * Handle setting a sales component (manufacture or import) which requires conversion by way of
   * initial charge specific to that stream.
   *
   * @private
   * @param {string} application - The application name.
   * @param {string} substance - The substance name.
   * @param {string} name - The stream name.
   * @param {EngineNumber} value - The value to set.
   */
  _setStreamForSalesComponent(application, substance, name, value) {
    const self = this;
    const stateGetter = new OverridingConverterStateGetter(self._stateGetter);
    const unitConverter = new UnitConverter(stateGetter);

    const initialCharge = self.getInitialCharge(application, substance, name);
    if (initialCharge.getValue() === 0) {
      throw new Error("Cannot set " + name + " stream with a zero initial charge.");
    }
    const initialChargeConverted = unitConverter.convert(initialCharge, "kg / unit");
    stateGetter.setAmortizedUnitVolume(initialChargeConverted);

    const valueUnitsPlain = unitConverter.convert(value, "units");
    const valueConverted = unitConverter.convert(valueUnitsPlain, "kg");
    self.setStream(application, substance, name, valueConverted);
  }
}

export {YearMatcher, VariableManager, Scope, StreamParameterization, StreamKeeper};
