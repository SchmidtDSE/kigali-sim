/**
 * Command classes for QubecTalk UI translator.
 *
 * Provides command class definitions for substance operations
 * including basic commands, lifecycle commands (retire, recharge),
 * policy commands (recycle, replace, assume, limit), and
 * compatibility markers.
 *
 * @license BSD, see LICENSE.md
 */

import {formatEngineNumber} from "ui_translator_util";

/**
 * Command with type, target, value and duration.
 *
 * Command such as a set command with a specified type, target, value and
 * duration.
 */
class Command {
  /**
   * Create a new Command.
   *
   * @param {string} typeName - Type of the command.
   * @param {string} target - Target of the command (e.g., "domestic", "import", "export",
   *    "equipment", "priorEquipment", "bank", "priorBank", "sales").
   *    Note: "bank" and "priorBank" are synonyms for "equipment" and
   *    "priorEquipment" respectively.
   * @param {EngineNumber} value - Value for the command.
   * @param {YearMatcher} duration - Duration for the command.
   */
  constructor(typeName, target, value, duration) {
    const self = this;
    self._typeName = typeName;
    self._target = target;
    self._value = value;
    self._duration = duration;
  }

  /**
   * Get the type name of this command.
   *
   * @returns {string} The command type name (e.g. "change", "retire", "setVal", etc).
   */
  getTypeName() {
    const self = this;
    return self._typeName;
  }

  /**
   * Get the target of this command.
   *
   * @returns {string} The target name (e.g., "domestic", "import", "equipment", "bank", etc).
   *    Note: "bank" and "priorBank" are accepted as synonyms for "equipment"
   *    and "priorEquipment".
   */
  getTarget() {
    const self = this;
    return self._target;
  }

  /**
   * Get the value associated with this command.
   *
   * @returns {EngineNumber} The command's value with units.
   */
  getValue() {
    const self = this;
    return self._value;
  }

  /**
   * Get the duration for which this command applies.
   *
   * @returns {YearMatcher} The duration specification, or null for all years.
   */
  getDuration() {
    const self = this;
    return self._duration;
  }

  /**
   * Check if this command is compatible with UI editing.
   *
   * @returns {boolean} Always returns true as basic commands are UI-compatible.
   */
  getIsCompatible() {
    const self = this;
    return true;
  }
}

/**
 * Retire command with optional replacement capability.
 *
 * Retire command with value, duration, and withReplacement flag indicating
 * whether retired equipment should be replaced to maintain population.
 */
class RetireCommand {
  /**
   * Create a new RetireCommand.
   *
   * @param {EngineNumber} value - Retirement rate/amount.
   * @param {YearMatcher|null} duration - Duration specification or null for all years.
   * @param {boolean} withReplacement - Whether to maintain equipment via replacement.
   */
  constructor(value, duration, withReplacement) {
    const self = this;
    self._value = value;
    self._duration = duration;
    self._withReplacement = withReplacement;
  }

  /**
   * Get the type name of this command.
   *
   * @returns {string} Always returns "retire".
   */
  getTypeName() {
    const self = this;
    return "retire";
  }

  /**
   * Get the value associated with this command.
   *
   * @returns {EngineNumber} The retirement rate/amount with units.
   */
  getValue() {
    const self = this;
    return self._value;
  }

  /**
   * Get the duration for which this command applies.
   *
   * @returns {YearMatcher|null} The duration specification, or null for all years.
   */
  getDuration() {
    const self = this;
    return self._duration;
  }

  /**
   * Get whether this retire command uses replacement.
   *
   * @returns {boolean} True if retire should maintain equipment via replacement.
   */
  getWithReplacement() {
    const self = this;
    return self._withReplacement;
  }

  /**
   * Check if this command is compatible with UI editing.
   *
   * @returns {boolean} Always returns true as retire commands are UI-compatible.
   */
  getIsCompatible() {
    const self = this;
    return true;
  }
}

/**
 * Assume command with mode for sales assumptions.
 *
 * Assume command with target stream, mode, and duration for specifying
 * sales behavior assumptions.
 */
class AssumeCommand {
  /**
   * Create a new AssumeCommand.
   *
   * @param {string} mode - The assumption mode: "no", "only recharge", or "continued".
   * @param {string} stream - The target stream (e.g., "sales").
   * @param {YearMatcher|null} duration - Duration specification or null for all years.
   */
  constructor(mode, stream, duration) {
    const self = this;
    self._mode = mode;
    self._stream = stream;
    self._duration = duration;
  }

  /**
   * Get the type name of this command.
   *
   * @returns {string} Always returns "assume".
   */
  getTypeName() {
    const self = this;
    return "assume";
  }

  /**
   * Get the assumption mode.
   *
   * @returns {string} The mode: "no", "only recharge", or "continued".
   */
  getMode() {
    const self = this;
    return self._mode;
  }

  /**
   * Get the target stream.
   *
   * @returns {string} The target stream name (e.g., "sales").
   */
  getStream() {
    const self = this;
    return self._stream;
  }

  /**
   * Get the duration for which this command applies.
   *
   * @returns {YearMatcher|null} The duration specification, or null for all years.
   */
  getDuration() {
    const self = this;
    return self._duration;
  }

  /**
   * Check if this command is compatible with UI editing.
   *
   * @returns {boolean} Always returns true as assume commands are UI-compatible.
   */
  getIsCompatible() {
    const self = this;
    return true;
  }
}

/**
 * Limit command with displacement capability.
 */
class LimitCommand {
  /**
   * Create a new LimitCommand.
   *
   * @param {string} typeName - Type of limit (cap/floor).
   * @param {string} target - Target of the limit.
   * @param {EngineNumber} value - Limit value.
   * @param {YearMatcher} duration - Duration of limit.
   * @param {string} displacing - Substance/stream being displaced.
   * @param {string} displacingType - Type of displacement ("", "by volume", "by units").
   */
  constructor(typeName, target, value, duration, displacing, displacingType) {
    const self = this;
    self._typeName = typeName;
    self._target = target;
    self._value = value;
    self._duration = duration;
    self._displacing = displacing;
    self._displacingType = displacingType;
  }

  /**
   * Get the type name of this limit command.
   *
   * @returns {string} The command type ("cap" or "floor").
   */
  getTypeName() {
    const self = this;
    return self._typeName;
  }

  /**
   * Get the target of this limit command.
   *
   * @returns {string} The target name (e.g. "domestic", "import", etc).
   */
  getTarget() {
    const self = this;
    return self._target;
  }

  /**
   * Get the value associated with this limit.
   *
   * @returns {EngineNumber} The limit value with units.
   */
  getValue() {
    const self = this;
    return self._value;
  }

  /**
   * Get the duration for which this limit applies.
   *
   * @returns {YearMatcher} The duration specification, or null for all years.
   */
  getDuration() {
    const self = this;
    return self._duration;
  }

  /**
   * Get the substance being displaced by this limit.
   *
   * @returns {string|null} Name of substance being displaced, or null if none.
   */
  getDisplacing() {
    const self = this;
    return self._displacing;
  }

  /**
   * Get the type of displacement.
   *
   * @returns {string} The displacement type ("", "by volume", "by units").
   */
  getDisplacingType() {
    const self = this;
    return self._displacingType;
  }

  /**
   * Check if this limit command is compatible with UI editing.
   *
   * @returns {boolean} Always returns true as limit commands are UI-compatible.
   */
  getIsCompatible() {
    const self = this;
    return true;
  }
}

/**
 * Recharge command with duration capability.
 */
class RechargeCommand {
  /**
   * Create a new RechargeCommand.
   *
   * @param {EngineNumber} populationEngineNumber - Population amount and units to recharge.
   * @param {EngineNumber} volumeEngineNumber - Volume per unit amount and units.
   * @param {YearMatcher|null} duration - Duration specification or null for all years.
   */
  constructor(populationEngineNumber, volumeEngineNumber, duration) {
    const self = this;
    self._populationEngineNumber = populationEngineNumber;
    self._volumeEngineNumber = volumeEngineNumber;
    self._duration = duration;
  }

  /**
   * Get the population EngineNumber.
   *
   * @returns {EngineNumber} The population amount and units.
   */
  getPopulationEngineNumber() {
    const self = this;
    return self._populationEngineNumber;
  }

  /**
   * Get the volume EngineNumber.
   *
   * @returns {EngineNumber} The volume per unit amount and units.
   */
  getVolumeEngineNumber() {
    const self = this;
    return self._volumeEngineNumber;
  }

  /**
   * Get the target for this recharge command (population - Command interface compatibility).
   *
   * @returns {EngineNumber} The population EngineNumber.
   */
  getTarget() {
    const self = this;
    return self._populationEngineNumber;
  }

  /**
   * Get the value for this recharge command (volume - Command interface compatibility).
   *
   * @returns {EngineNumber} The volume EngineNumber.
   */
  getValue() {
    const self = this;
    return self._volumeEngineNumber;
  }

  /**
   * Get the duration for which this recharge command applies.
   *
   * @returns {YearMatcher|null} The duration specification, or null for all years.
   */
  getDuration() {
    const self = this;
    return self._duration;
  }

  /**
   * Build the command string for this recharge with original number formatting preserved.
   *
   * @param {string} substance - The substance name (unused but kept for consistency).
   * @returns {string} The generated recharge command.
   */
  buildCommand(substance) {
    const self = this;
    const populationStr = formatEngineNumber(self._populationEngineNumber);
    const volumeStr = formatEngineNumber(self._volumeEngineNumber);
    const baseCommand = `recharge ${populationStr} with ${volumeStr}`;
    let command = baseCommand;

    if (self._duration) {
      const start = self._duration.getStart();
      const end = self._duration.getEnd();

      if (start !== null && end !== null) {
        if (start.equals(end)) {
          command += ` during year ${start.getYearStr()}`;
        } else {
          command += ` during years ${start.getYearStr()} to ${end.getYearStr()}`;
        }
      } else if (start !== null) {
        command += ` during years ${start.getYearStr()} to onwards`;
      } else if (end !== null) {
        command += ` during years beginning to ${end.getYearStr()}`;
      }
    }

    return command;
  }

  /**
   * Get the type name of this command.
   *
   * @returns {string} The command type name.
   */
  getTypeName() {
    const self = this;
    return "recharge";
  }

  /**
   * Check if this recharge command is compatible with UI editing.
   *
   * @returns {boolean} Always returns true as recharge commands are UI-compatible.
   */
  getIsCompatible() {
    const self = this;
    return true;
  }
}

/**
 * Recycle command for substance recovery.
 */
class RecycleCommand {
  /**
   * Create a new RecycleCommand.
   *
   * @param {EngineNumber} target - Recovery amount and units.
   * @param {EngineNumber} value - Reuse amount and units.
   * @param {YearMatcher} duration - Duration of recovery.
   * @param {string} stage - Recycling stage ("eol" or "recharge").
   * @param {EngineNumber|string|null} induction - Induction amount, "default", or null for
   *   backward compatibility.
   */
  constructor(target, value, duration, stage, induction = null) {
    const self = this;
    self._target = target;
    self._value = value;
    self._duration = duration;
    self._stage = stage;
    self._induction = induction;
  }

  /**
   * Get the type name of this recycle command.
   *
   * @returns {string} Always returns "recycle".
   */
  getTypeName() {
    const self = this;
    return "recycle";
  }

  /**
   * Get the target (recovery amount) of this recycle command.
   *
   * @returns {EngineNumber} The recovery amount with units.
   */
  getTarget() {
    const self = this;
    return self._target;
  }

  /**
   * Get the value (reuse amount) associated with this recycle.
   *
   * @returns {EngineNumber} The reuse amount with units.
   */
  getValue() {
    const self = this;
    return self._value;
  }

  /**
   * Get the duration for which this recycle applies.
   *
   * @returns {YearMatcher} The duration specification, or null for all years.
   */
  getDuration() {
    const self = this;
    return self._duration;
  }

  /**
   * Get the recycling stage for this recycle command.
   *
   * @returns {string} The recycling stage ("eol" or "recharge").
   */
  getStage() {
    const self = this;
    return self._stage;
  }

  /**
   * Get the induction rate for this recycle command.
   *
   * @returns {EngineNumber|string|null} The induction amount, "default", or null if not specified.
   */
  getInduction() {
    const self = this;
    return self._induction;
  }

  /**
   * Check if this recycle command is compatible with UI editing.
   *
   * @returns {boolean} Always returns true as recycle commands are UI-compatible.
   */
  getIsCompatible() {
    const self = this;
    return true;
  }
}

/**
 * Represent a command to replace one substance with another.
 */
class ReplaceCommand {
  /**
   * Create a new ReplaceCommand.
   *
   * @param {EngineNumber} volume - Volume to replace.
   * @param {string} source - Source substance.
   * @param {string} destination - Destination substance.
   * @param {YearMatcher} duration - Duration of replacement.
   */
  constructor(volume, source, destination, duration) {
    const self = this;
    self._volume = volume;
    self._source = source;
    self._destination = destination;
    self._duration = duration;
  }

  /**
   * Get the type name of this replace command.
   *
   * @returns {string} Always returns "replace".
   */
  getTypeName() {
    const self = this;
    return "replace";
  }

  /**
   * Get the volume to be replaced.
   *
   * @returns {EngineNumber} The volume with units.
   */
  getVolume() {
    const self = this;
    return self._volume;
  }

  /**
   * Get the source substance to replace from.
   *
   * @returns {string} Name of source substance.
   */
  getSource() {
    const self = this;
    return self._source;
  }

  /**
   * Get the destination substance to replace with.
   *
   * @returns {string} Name of destination substance.
   */
  getDestination() {
    const self = this;
    return self._destination;
  }

  /**
   * Get the duration for which this replacement applies.
   *
   * @returns {YearMatcher} The duration specification, or null for all years.
   */
  getDuration() {
    const self = this;
    return self._duration;
  }

  /**
   * Check if this replace command is compatible with UI editing.
   *
   * @returns {boolean} Always returns true as replace commands are UI-compatible.
   */
  getIsCompatible() {
    const self = this;
    return true;
  }
}

/**
 * Command that is not compatible with the UI editor.
 */
class IncompatibleCommand {
  /**
   * Create a new incompatible command marker.
   *
   * @param {string} typeName - Type of incompatible command for reporting.
   */
  constructor(typeName) {
    const self = this;
    self._typeName = typeName;
  }

  /**
   * Get the type name of this incompatible command.
   *
   * @returns {string} Type name of the incompatible command.
   */
  getTypeName() {
    const self = this;
    return self._typeName;
  }

  /**
   * Check if this command is compatible with UI editing.
   *
   * @returns {boolean} Always returns false as this marks incompatibility.
   */
  getIsCompatible() {
    const self = this;
    return false;
  }
}

export {
  AssumeCommand,
  Command,
  IncompatibleCommand,
  LimitCommand,
  RechargeCommand,
  RecycleCommand,
  ReplaceCommand,
  RetireCommand,
};
