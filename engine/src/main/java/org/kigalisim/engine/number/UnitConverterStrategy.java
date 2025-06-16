/**
 * Strategy interface for unit conversion operations.
 *
 * <p>This interface defines the contract for converting EngineNumber instances
 * to different units. Implementations provide specific conversion logic for
 * different unit types.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.number;

/**
 * Strategy for converting EngineNumber to specific unit types.
 *
 * <p>This interface follows the Strategy pattern to encapsulate unit conversion
 * algorithms. Each implementation handles conversion to a specific unit type.</p>
 */
public interface UnitConverterStrategy {
  
  /**
   * Convert an EngineNumber to the target unit type.
   *
   * @param input The EngineNumber to convert
   * @return The converted EngineNumber with the target unit type
   */
  EngineNumber convert(EngineNumber input);
}