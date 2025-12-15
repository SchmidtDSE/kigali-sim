/**
 * Executor for displacement operations in cap, floor, and recover commands.
 *
 * <p>This class handles displacing changes in one stream to another stream or substance.
 * Displacement allows policy operations to offset reductions in one area with increases in another,
 * enabling complex policy scenarios like substance replacement, import/export balancing, and
 * recycling credit systems.</p>
 *
 * <p>Displacement supports two modes:
 * <ul>
 *   <li><strong>Stream-based displacement</strong>: Displacing to another stream within
 *       the same substance (e.g., domestic to import)</li>
 *   <li><strong>Substance-based displacement</strong>: Displacing to a different substance
 *       (e.g., HFC-134a to R-600a)</li>
 * </ul>
 * </p>
 *
 * <p>For equipment-unit-based operations, displacement applies the same number of units
 * to the destination but converts using destination-specific properties (initial charge, GWP). For
 * volume-based operations, displacement uses the same substance volume.</p>
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.engine.support;

import java.math.BigDecimal;
import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.recalc.SalesStreamDistribution;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.engine.state.SimulationState;

/**
 * Executor for displacement operations in cap, floor, and recover commands.
 */
public class DisplaceExecutor {
  private final Engine engine;
  private final StreamUpdateShortcuts shortcuts;

  /**
   * Creates a new DisplaceExecutor for the given engine.
   *
   * @param engine The Engine instance to operate on
   */
  public DisplaceExecutor(Engine engine) {
    this.engine = engine;
    this.shortcuts = new StreamUpdateShortcuts(engine);
  }

  /**
   * Executes a displacement operation to offset a change in one stream with a change in another.
   *
   * <p>Displacement allows changes in one stream to be balanced by inverse changes in another
   * stream or substance. This is used by cap, floor, and recover operations to enable complex policy
   * scenarios like:</p>
   * <ul>
   *   <li>Capping domestic manufacturing while increasing imports (stream displacement)</li>
   *   <li>Phasing down a high-GWP substance while ramping up a low-GWP alternative
   *       (substance displacement)</li>
   *   <li>Crediting recycled material recovery against virgin material consumption</li>
   * </ul>
   *
   * <p>The method handles three distinct cases:</p>
   * <ol>
   *   <li><strong>Automatic recycling displacement</strong>: When recovering material to the
   * sales stream, automatically adds the recycled volume to sales before applying targeted
   * displacement. This maintains material balance in the system.</li> <li><strong>Equipment-unit
   * displacement</strong>: When the operation uses equipment units, converts the volume change back
   * to units in the source substance, then applies the same number of units to the destination (with
   * destination-specific initial charge for substance displacement).</li> <li><strong>Volume
   * displacement</strong>: When the operation uses volume units (kg/mt), applies the same substance
   * volume to the destination.</li>
   * </ol>
   *
   * @param stream The stream identifier being modified (e.g., "domestic", "import", "sales")
   * @param amount The amount used for the original operation (determines units vs volume mode)
   * @param changeAmount The actual change amount in kg (negative for reductions)
   * @param displaceTarget The target for displacement (stream name or substance name), or null
   * @throws IllegalArgumentException if attempting to displace stream to itself
   */
  public void execute(String stream, EngineNumber amount, BigDecimal changeAmount,
      String displaceTarget) {
    // Early return if no displacement requested
    if (displaceTarget == null) {
      return;
    }

    // Validate not displacing to self
    if (stream.equals(displaceTarget)) {
      ExceptionsGenerator.raiseSelfDisplacement(stream);
    }

    // Determine if this is stream-based or substance-based displacement
    boolean isStreamDisplacement = EngineSupportUtils.STREAM_NAMES.contains(displaceTarget);

    // Handle automatic recycling addition before targeted displacement
    applyRecyclingBeforeDisplacement(stream, changeAmount, isStreamDisplacement);

    // Apply displacement based on unit type
    if (amount.hasEquipmentUnits()) {
      applyUnitsDisplacement(stream, changeAmount, displaceTarget, isStreamDisplacement);
    } else {
      applyVolumeDisplacement(stream, changeAmount, displaceTarget, isStreamDisplacement);
    }
  }

  /**
   * Applies automatic recycling addition when recovering material to the sales stream.
   *
   * <p>When a recovery operation creates recycled material from the sales stream and
   * specifies stream-based displacement, the recycled material is automatically added back to the
   * sales stream before applying the targeted displacement. This ensures that the total material
   * balance is maintained in the system.</p>
   *
   * <p>This automatic recycling addition only occurs when:</p>
   * <ul>
   *   <li>The displacement target is a stream (not a substance)</li>
   *   <li>The source stream is the recycle recovery stream (typically "sales")</li>
   * </ul>
   *
   * <p>Without this step, recovered material would be double-counted as both a reduction
   * in virgin material consumption and as recycled material, leading to incorrect totals.</p>
   *
   * @param stream The stream identifier being modified
   * @param changeAmount The change amount in kg
   * @param isStreamDisplacement True if displacement target is a stream, false if substance
   */
  private void applyRecyclingBeforeDisplacement(String stream, BigDecimal changeAmount,
      boolean isStreamDisplacement) {
    boolean displacementAutomatic = isStreamDisplacement
        && EngineSupportUtils.RECYCLE_RECOVER_STREAM.equals(stream);

    if (displacementAutomatic) {
      EngineNumber recycledAddition = new EngineNumber(changeAmount, "kg");
      shortcuts.changeStreamWithoutReportingUnits(
          EngineSupportUtils.RECYCLE_RECOVER_STREAM,
          recycledAddition,
          Optional.empty(),
          Optional.empty()
      );
    }
  }

  /**
   * Applies equipment-unit-based displacement logic.
   *
   * <p>For equipment unit operations, displacement transfers the same number of equipment
   * units from source to destination, but the actual substance volumes may differ due to different
   * initial charge requirements for each substance.</p>
   *
   * <p>The process:</p>
   * <ol>
   *   <li>Convert the volume change (in kg) back to equipment units using the source
   *       substance's initial charge</li>
   *   <li>For stream-based displacement: Apply the negated volume change to the target
   *       stream (same substance, so volumes match)</li>
   *   <li>For substance-based displacement: Apply the same number of units to the
   * destination substance, converting using destination's initial charge to get destination
   * volume</li>
   * </ol>
   *
   * <p>This method delegates to specialized helpers for same-substance and different-substance
   * displacement to keep the logic clear and maintainable.</p>
   *
   * @param stream The stream identifier being modified
   * @param changeAmount The change amount in kg (negative for reductions)
   * @param displaceTarget The target for displacement (stream name or substance name)
   * @param isStreamDisplacement True if displacement target is a stream, false if substance
   */
  private void applyUnitsDisplacement(String stream, BigDecimal changeAmount,
      String displaceTarget, boolean isStreamDisplacement) {
    UnitConverter currentUnitConverter = EngineSupportUtils.createUnitConverterWithTotal(
        engine,
        stream
    );

    // Convert the volume change back to units in the original substance
    EngineNumber volumeChangeFlip = new EngineNumber(changeAmount.negate(), "kg");
    EngineNumber unitsChanged = currentUnitConverter.convert(
        volumeChangeFlip,
        "units"
    );

    if (isStreamDisplacement) {
      applyUnitsDisplacementSameSubstance(stream, changeAmount, displaceTarget);
    } else {
      applyUnitsDisplacementDifferentSubstance(stream, unitsChanged, displaceTarget);
    }
  }

  /**
   * Applies equipment-unit displacement within the same substance (stream-based).
   *
   * <p>When displacing to another stream within the same substance (e.g., domestic to
   * import), the equipment units have the same initial charge, so the substance volumes are
   * identical. This method simply applies the negated volume change to the target stream.</p>
   *
   * <p>Example: Capping domestic to 80% displacing "import" means reducing domestic by
   * 20% and increasing import by the same substance volume.</p>
   *
   * @param stream The source stream identifier
   * @param changeAmount The change amount in kg (negative for reductions)
   * @param displaceTarget The target stream name (within same substance)
   */
  private void applyUnitsDisplacementSameSubstance(String stream, BigDecimal changeAmount,
      String displaceTarget) {
    EngineNumber displaceChange = new EngineNumber(changeAmount.negate(), "kg");

    shortcuts.changeStreamWithoutReportingUnits(
        displaceTarget,
        displaceChange,
        Optional.empty(),
        Optional.empty()
    );
  }

  /**
   * Applies equipment-unit displacement to a different substance.
   *
   * <p>When displacing to a different substance, the same number of equipment units is
   * transferred, but the actual substance volumes differ based on each substance's initial charge
   * requirements. This method:</p>
   * <ol>
   *   <li>Switches to the destination substance scope</li>
   *   <li>Converts the number of units to destination substance volume using the
   *       destination's initial charge</li>
   *   <li>Applies the destination volume change using displacement context for correct
   *       GWP calculations</li>
   *   <li>Restores the original scope</li>
   * </ol>
   *
   * <p>Example: Capping HFC-134a to 80% displacing "R-600a" transfers units to R-600a,
   * but R-600a may have a different initial charge (kg/unit), resulting in different total substance
   * volume but same equipment count.</p>
   *
   * @param stream The source stream identifier
   * @param unitsChanged The number of equipment units to transfer
   * @param displaceTarget The destination substance name
   */
  private void applyUnitsDisplacementDifferentSubstance(String stream,
      EngineNumber unitsChanged, String displaceTarget) {
    Scope currentScope = engine.getScope();
    Scope destinationScope = currentScope.getWithSubstance(displaceTarget);

    engine.setSubstance(displaceTarget);
    UnitConverter destinationUnitConverter = EngineSupportUtils.createUnitConverterWithTotal(
        engine,
        stream
    );

    EngineNumber destinationVolumeChange = destinationUnitConverter.convert(unitsChanged, "kg");
    EngineNumber displaceChange = new EngineNumber(destinationVolumeChange.getValue(), "kg");

    shortcuts.changeStreamWithDisplacementContext(stream, displaceChange, destinationScope);
    updateLastSpecifiedForDisplacement(stream, displaceChange, destinationScope);

    String originalSubstance = currentScope.getSubstance();
    engine.setSubstance(originalSubstance);
  }

  /**
   * Applies volume-based displacement using the same substance volume.
   *
   * <p>For volume-based operations (kg, mt), displacement uses the same substance volume
   * for both source and destination. Unlike equipment-unit displacement, the actual substance volume
   * is identical regardless of whether displacing to a stream or substance.</p>
   *
   * <p>The process:</p>
   * <ol>
   *   <li>For stream-based displacement: Apply the negated volume change to the target
   *       stream using changeStreamWithoutReportingUnits</li>
   *   <li>For substance-based displacement: Apply the negated volume change to the
   * destination substance using changeStreamWithDisplacementContext for correct GWP calculations</li>
   * </ol>
   *
   * @param stream The stream identifier being modified
   * @param changeAmount The change amount in kg (negative for reductions)
   * @param displaceTarget The target for displacement (stream name or substance name)
   * @param isStreamDisplacement True if displacement target is a stream, false if substance
   */
  private void applyVolumeDisplacement(String stream, BigDecimal changeAmount,
      String displaceTarget, boolean isStreamDisplacement) {
    EngineNumber displaceChange = new EngineNumber(changeAmount.negate(), "kg");

    if (isStreamDisplacement) {
      shortcuts.changeStreamWithoutReportingUnits(
          displaceTarget,
          displaceChange,
          Optional.empty(),
          Optional.empty()
      );
    } else {
      Scope currentScope = engine.getScope();
      Scope destinationScope = currentScope.getWithSubstance(displaceTarget);

      shortcuts.changeStreamWithDisplacementContext(stream, displaceChange, destinationScope);
      updateLastSpecifiedForDisplacement(stream, displaceChange, destinationScope);
    }
  }

  /**
   * Updates lastSpecified values for domestic and import in the destination substance.
   *
   * <p>When displacement occurs to a different substance, this method updates the lastSpecified
   * values for domestic and import streams so that future "change sales by X%" commands build
   * on the displaced values rather than the original values. This ensures that displacement
   * effects compound properly with growth rates.</p>
   *
   * <p>The displacement amount is distributed proportionally between domestic and import
   * based on their current distribution ratio.</p>
   *
   * @param stream The stream identifier being modified (typically "sales")
   * @param displaceChange The displacement amount in kg
   * @param destinationScope The scope of the destination substance
   */
  private void updateLastSpecifiedForDisplacement(String stream, EngineNumber displaceChange,
      Scope destinationScope) {
    boolean isSalesStream = "sales".equals(stream);
    if (!isSalesStream) {
      return;
    }

    engine.setSubstance(destinationScope.getSubstance());

    SimulationState simulationState = engine.getStreamKeeper();

    EngineNumber domesticLast = simulationState.getLastSpecifiedValue(destinationScope, "domestic");
    EngineNumber importLast = simulationState.getLastSpecifiedValue(destinationScope, "import");

    // Get the current distribution to proportionally allocate the displacement
    SalesStreamDistribution distribution = simulationState.getDistribution(destinationScope);
    BigDecimal percentDomestic = distribution.getPercentDomestic();
    BigDecimal percentImport = distribution.getPercentImport();

    BigDecimal domesticDisplacement = displaceChange.getValue().multiply(percentDomestic);
    BigDecimal importDisplacement = displaceChange.getValue().multiply(percentImport);

    UnitConverter converter = EngineSupportUtils.createUnitConverterWithTotal(engine, stream);

    boolean hasDomesticLast = domesticLast != null;
    if (hasDomesticLast) {
      EngineNumber domesticDisplacementInUnits = converter.convert(
          new EngineNumber(domesticDisplacement, "kg"),
          domesticLast.getUnits()
      );
      BigDecimal newDomestic = domesticLast.getValue().add(domesticDisplacementInUnits.getValue());
      EngineNumber updatedDomestic = new EngineNumber(newDomestic, domesticLast.getUnits());
      simulationState.setLastSpecifiedValue(destinationScope, "domestic", updatedDomestic);
    }

    boolean hasImportLast = importLast != null;
    if (hasImportLast) {
      EngineNumber importDisplacementInUnits = converter.convert(
          new EngineNumber(importDisplacement, "kg"),
          importLast.getUnits()
      );
      BigDecimal newImport = importLast.getValue().add(importDisplacementInUnits.getValue());
      EngineNumber updatedImport = new EngineNumber(newImport, importLast.getUnits());
      simulationState.setLastSpecifiedValue(destinationScope, "import", updatedImport);
    }

    Scope currentScope = engine.getScope();
    String originalSubstance = currentScope.getSubstance();
    engine.setSubstance(originalSubstance);
  }
}
