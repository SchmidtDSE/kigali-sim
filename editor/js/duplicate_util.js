/**
 * Utilities for handling entity duplication and name conflict resolution.
 *
 * @license BSD, see LICENSE.md.
 */

/**
 * Result of name conflict resolution.
 */
class NameConflictResolution {
  /**
   * Create a new NameConflictResolution.
   *
   * @param {string} originalName - The original name that was requested.
   * @param {string} resolvedName - The final name after conflict resolution.
   */
  constructor(originalName, resolvedName) {
    const self = this;
    self._originalName = originalName;
    self._resolvedName = resolvedName;
  }

  /**
   * Check if the name was changed during conflict resolution.
   *
   * @returns {boolean} True if the name was changed, false otherwise.
   */
  getNameChanged() {
    const self = this;
    return self._originalName !== self._resolvedName;
  }

  /**
   * Get the final resolved name.
   *
   * @returns {string} The resolved name.
   */
  getNewName() {
    const self = this;
    return self._resolvedName;
  }
}

/**
 * Resolves name conflicts by appending incrementing numbers until finding a unique name.
 *
 * @param {string} baseName - The initial desired name.
 * @param {Set<string>} existingNames - Set of existing names to avoid conflicts with.
 * @returns {NameConflictResolution} A resolution object with the result.
 */
function resolveNameConflict(baseName, existingNames) {
  if (!existingNames.has(baseName)) {
    return new NameConflictResolution(baseName, baseName);
  }

  let counter = 0;
  let candidate = `${baseName} (${counter})`;

  while (existingNames.has(candidate)) {
    counter++;
    candidate = `${baseName} (${counter})`;
  }

  return new NameConflictResolution(baseName, candidate);
}

/**
 * Resolves substance name conflicts with special handling for effective substance names.
 * This function handles the combination of substance and equipment model names.
 *
 * @param {string} baseName - The initial desired substance name.
 * @param {Set<string>} existingNames - Set of existing substance names to avoid conflicts with.
 * @returns {NameConflictResolution} A resolution object with the result.
 */
function resolveSubstanceNameConflict(baseName, existingNames) {
  return resolveNameConflict(baseName, existingNames);
}

export {NameConflictResolution, resolveNameConflict, resolveSubstanceNameConflict};