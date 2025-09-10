/**
 * Utility class for executing set operations on sales streams.
 *
 * <p>This class provides set operation logic for Engine implementations
 * to provide better separation of concerns and testability. It handles 
 * distribution of sales values to component streams (domestic/import) while
 * preserving lastSpecifiedValue for subsequent change operations.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import java.math.BigDecimal;
import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.recalc.SalesStreamDistribution;
import org.kigalisim.engine.state.StreamKeeper;
import org.kigalisim.engine.state.UseKey;
import org.kigalisim.engine.state.YearMatcher;

/**
 * Handles set operations for sales streams with proper component distribution.
 *
 * <p>This class provides methods to handle setting sales values that need to be
 * distributed to component streams (domestic, import) while preserving the
 * lastSpecifiedValue that enables subsequent change operations to work correctly.</p>
 */
public class SetExecutor {

  private final Engine engine;

  /**
   * Creates a new SetExecutor for the given engine.
   *
   * @param engine The Engine instance to operate on
   */
  public SetExecutor(Engine engine) {
    this.engine = engine;
  }

  /**
   * Handle sales stream setting by distributing to component streams.
   *
   * @param useKey The use key for the operation scope
   * @param stream The stream identifier (should be "sales")  
   * @param value The value to set
   * @param yearMatcher Optional year matcher for conditional setting
   */
  public void handleSalesSet(UseKey useKey, String stream, EngineNumber value, 
                             Optional<YearMatcher> yearMatcher) {
    // Check if this operation should apply to current year
    if (yearMatcher.isPresent()
        && !EngineSupportUtils.isInRange(yearMatcher.get(), engine.getYear())) {
      return;
    }

    // Get distribution ratios like SalesRecalcStrategy does
    StreamKeeper streamKeeper = engine.getStreamKeeper();
    SalesStreamDistribution distribution = streamKeeper.getDistribution(useKey);
    
    // Calculate component amounts based on distribution percentages
    BigDecimal domesticAmount = value.getValue().multiply(distribution.getPercentDomestic());
    BigDecimal importAmount = value.getValue().multiply(distribution.getPercentImport());
    
    // Set component streams, letting SingleThreadEngine handle lastSpecifiedValue
    // Only set streams that have non-zero allocations (are enabled)
    if (distribution.getPercentDomestic().compareTo(BigDecimal.ZERO) > 0) {
      EngineNumber domesticValue = new EngineNumber(domesticAmount, value.getUnits());
      engine.setStream("domestic", domesticValue, yearMatcher);
    }
    
    if (distribution.getPercentImport().compareTo(BigDecimal.ZERO) > 0) {
      EngineNumber importValue = new EngineNumber(importAmount, value.getUnits());
      engine.setStream("import", importValue, yearMatcher);
    }
  }
}