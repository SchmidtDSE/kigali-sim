import {
  GLOBAL_CONTEXT,
  STANZA_CONTEXT,
  APPLICATION_CONTEXT,
  SUBSTANCE_CONTEXT,
  STREAM_BASE_UNITS,
} from "engine_const";

import {
  EngineNumber,
} from "engine_number";


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
    self._start = start;
    self._end = end;
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
  constructor(
    contextLevel,
    globalContext,
    stanzaContext,
    applicationContext,
    substanceContext,
  ) {
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
   * @param newSubstance The name of the applicatino in which the new scope resides.
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

    return new Scope(
      newStanza,
      null,
      null,
      self._variableManager.getWithLevel(STANZA_CONTEXT),
    );
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

class StreamParameterization {
  constructor() {
    const self = this;
    self.resetInternals();
  }

  resetInternals() {
    const self = this;
    const createZero = (x) => new EngineNumber(0, x);
    self._ghgIntensity = createZero("tCO2e / kg");
    self._initialCharge = {
      "manufacture": createZero("kg / unit"),
      "import": createZero("kg / unit"),
    };
    self._rechargePopulation = createZero("%");
    self._rechargeIntensity = createZero("kg / unit");
    self._recoveryRate = createZero("%");
    self._yieldRate = createZero("%");
    self._retirementRate = createZero("%");
    self._displacementRate = new EngineNumber(100, "%");
  }

  setGhgIntensity(newValue) {
    const self = this;
    self._ghgIntensity = newValue;
  }

  getGhgIntensity() {
    const self = this;
    return self._ghgIntensity;
  }

  setInitialCharge(stream, newValue) {
    const self = this;
    self._ensureSalesStreamAllowed(stream);
    self._initialCharge[stream] = newValue;
  }

  getInitialCharge(stream) {
    const self = this;
    self._ensureSalesStreamAllowed(stream);
    return self._initialCharge[stream];
  }

  setRechargePopulation(newValue) {
    const self = this;
    self._rechargePopulation = newValue;
  }

  getRechargePopulation() {
    const self = this;
    return self._rechargePopulation;
  }

  setRechargeIntensity(newValue) {
    const self = this;
    self._rechargeIntensity = newValue;
  }

  getRechargeIntensity() {
    const self = this;
    return self._rechargeIntensity;
  }

  setRecoveryRate(newValue) {
    const self = this;
    self._recoveryRate = newValue;
  }

  getRecoveryRate() {
    const self = this;
    return self._recoveryRate;
  }

  setYieldRate(newValue) {
    const self = this;
    self._yieldRate = newValue;
  }

  getYieldRate() {
    const self = this;
    return self._yieldRate;
  }

  setDisplacementRate(newValue) {
    const self = this;
    self._displacementRate = newValue;
  }

  getDisplacementRate() {
    const self = this;
    return self._displacementRate;
  }

  setRetirementRate(newValue) {
    const self = this;
    self._retirementRate = newValue;
  }

  getRetirementRate() {
    const self = this;
    return self._retirementRate;
  }

  _ensureSalesStreamAllowed(name) {
    const self = this;
    if (name !== "manufacture" && name !== "import") {
      debugger;
      throw "Must address a sales substream.";
    }
  }
}


class SubstanceInApplicationId {
  constructor(application, substance) {
    const self = this;
    self._application = application;
    self._substance = substance;
  }

  getApplication() {
    const self = this;
    return self._application;
  }

  getSubstance() {
    const self = this;
    return self._substance;
  }
}


class StreamKeeper {
  constructor(unitConverter) {
    const self = this;
    self._substances = new Map();
    self._streams = new Map();
    self._unitConverter = unitConverter;
  }

  getRegisteredSubstances() {
    const self = this;
    const keys = Array.of(...self._substances.keys());
    const keyComponents = keys.map((x) => x.split("\t"));
    const substanceIds = keyComponents.map((x) => {
      return new SubstanceInApplicationId(x[0], x[1]);
    });
    return substanceIds;
  }

  hasSubstance(application, substance) {
    const self = this;
    const key = self._getKey(application, substance);
    return self._substances.has(key);
  }

  ensureSubstance(application, substance) {
    const self = this;

    if (self.hasSubstance(application, substance)) {
      return;
    }

    const key = self._getKey(application, substance);
    self._substances.set(key, new StreamParameterization());

    const makeZero = (units) => new EngineNumber(0, units);

    // Sales: manufacture, import
    self._streams.set(
      self._getKey(application, substance, "manufacture"),
      makeZero("kg"),
    );
    self._streams.set(
      self._getKey(application, substance, "import"),
      makeZero("kg"),
    );

    // Emissions: count, conversion
    self._streams.set(
      self._getKey(application, substance, "emissions"),
      makeZero("tCO2e"),
    );

    // Population
    self._streams.set(
      self._getKey(application, substance, "equipment"),
      makeZero("units"),
    );
    self._streams.set(
      self._getKey(application, substance, "priorEquipment"),
      makeZero("units"),
    );
  }

  setStream(application, substance, name, value) {
    const self = this;
    self._ensureSubstancePresent(application, substance, "setStream");
    self._ensureStreamKnown(name);

    if (name === "sales") {
      const manufactureValueRaw = self.getStream(
        application,
        substance,
        "manufacture",
      );
      const importValueRaw = self.getStream(application, substance, "import");
      const manufactureValue = self._unitConverter.convert(
        manufactureValueRaw,
        "kg",
      );
      const importValue = self._unitConverter.convert(importValueRaw, "kg");
      const manufactureAmount = manufactureValue.getValue();
      const importAmount = importValue.getValue();

      const totalAmount = manufactureAmount + importAmount;
      const isZero = totalAmount == 0;
      const manufacturePercent = isZero ? 0.5 : manufactureAmount / totalAmount;
      const importPercent = isZero ? 0.5 : importAmount / totalAmount;

      const manufactureShare = value.getValue() * manufacturePercent;
      const importShare = value.getValue() * importPercent;
      const manufactureNewValue = new EngineNumber(
        manufactureShare,
        value.getUnits(),
      );
      const importNewValue = new EngineNumber(importShare, value.getUnits());

      self.setStream(
        application,
        substance,
        "manufacture",
        manufactureNewValue,
      );
      self.setStream(application, substance, "import", importNewValue);
      return;
    }

    const unitsNeeded = self._getUnits(name);
    const valueConverted = self._unitConverter.convert(value, unitsNeeded);
    self._streams.set(
      self._getKey(application, substance, name),
      valueConverted,
    );
  }

  getStream(application, substance, name) {
    const self = this;
    self._ensureSubstancePresent(application, substance, "getStream");
    self._ensureStreamKnown(name);

    if (name === "sales") {
      const manufactureAmountRaw = self.getStream(
        application,
        substance,
        "manufacture",
      );
      const importAmountRaw = self.getStream(application, substance, "import");
      const manufactureAmount = self._unitConverter.convert(
        manufactureAmountRaw,
        "kg",
      );
      const importAmount = self._unitConverter.convert(importAmountRaw, "kg");
      const manufactureAmountValue = manufactureAmount.getValue();
      const importAmountValue = importAmount.getValue();
      const newTotal = manufactureAmountValue + importAmountValue;
      return new EngineNumber(newTotal, "kg");
    } else {
      return self._streams.get(self._getKey(application, substance, name));
    }
  }

  isKnownStream(application, substance, name) {
    const self = this;
    return self._streams.has(self._getKey(application, substance, name));
  }

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

  setGhgIntensity(application, substance, newValue) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    parameterization.setGhgIntensity(newValue);
  }

  getGhgIntensity(application, substance) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    return parameterization.getGhgIntensity();
  }

  setInitialCharge(application, substance, substream, newValue) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    parameterization.setInitialCharge(substream, newValue);
  }

  getInitialCharge(application, substance, substream) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    return parameterization.getInitialCharge(substream);
  }

  setRechargePopulation(application, substance, newValue) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    parameterization.setRechargePopulation(newValue);
  }

  getRechargePopulation(application, substance) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    return parameterization.getRechargePopulation();
  }

  setRechargeIntensity(application, substance, newValue) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    parameterization.setRechargeIntensity(newValue);
  }

  getRechargeIntensity(application, substance) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    return parameterization.getRechargeIntensity();
  }

  setRecoveryRate(application, substance, newValue) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    parameterization.setRecoveryRate(newValue);
  }

  getRecoveryRate(application, substance) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    return parameterization.getRecoveryRate();
  }

  setDisplacementRate(application, substance, newValue) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    parameterization.setDisplacementRate(newValue);
  }

  getDisplacementRate(application, substance) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    return parameterization.getDisplacementRate();
  }

  setYieldRate(application, substance, newValue) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    parameterization.setYieldRate(newValue);
  }

  getYieldRate(application, substance) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    return parameterization.getYieldRate();
  }

  setRetirementRate(application, substance, newValue) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    parameterization.setRetirementRate(newValue);
  }

  getRetirementRate(application, substance) {
    const self = this;
    const parameterization = self._getParameterization(application, substance);
    return parameterization.getRetirementRate();
  }

  _getParameterization(application, substance) {
    const self = this;
    self._ensureSubstancePresent(application, substance, "getParameterization");
    const key = self._getKey(application, substance);
    return self._substances.get(key);
  }

  _getKey(application, substance, name, substream) {
    const self = this;
    const pieces = [application, substance, name, substream];
    const getIsNotGiven = (x) => x === null || x === undefined;
    const piecesSafe = pieces.map((x) => (getIsNotGiven(x) ? "-" : x + ""));
    return piecesSafe.join("\t");
  }

  _ensureSubstancePresent(application, substance, context) {
    const self = this;

    if (!self.hasSubstance(application, substance)) {
      const pairStr = application + ", " + substance;
      throw "Not a known application substance pair in " + context + ": " + pairStr;
    }
  }

  _ensureStreamKnown(name) {
    const self = this;
    if (!STREAM_BASE_UNITS.has(name)) {
      throw "Unknown stream: " + name;
    }
  }

  _getUnits(name) {
    const self = this;
    self._ensureStreamKnown(name);
    return STREAM_BASE_UNITS.get(name);
  }
}

export {
  YearMatcher,
  VariableManager,
  Scope,
  StreamParameterization,
  StreamKeeper,
};
