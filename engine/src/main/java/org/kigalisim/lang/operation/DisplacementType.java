/**
 * Enum representing the type of displacement for cap and floor operations.
 *
 * <p>This enum determines how displacement is calculated when a cap or floor operation
 * displaces excess or deficit to another stream or substance. It supports three modes:
 * equivalent (context-based), by volume (always in kg), and by units (always in equipment count).</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.operation;

/**
 * Enum representing the type of displacement for cap and floor operations.
 */
public enum DisplacementType {
  /** Equivalent displacement - displaces based on context (volume if last specified as volume, units if units). */
  EQUIVALENT,
  /** By volume displacement - always convert to kg and displace by volume. */
  BY_VOLUME,
  /** By units displacement - always convert to units and displace by equipment count. */
  BY_UNITS
}
