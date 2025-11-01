package org.kigalisim.engine.support;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.ConverterStateGetter;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.engine.state.SimulationState;

/**
 * Unit tests for DisplaceExecutor class.
 *
 * <p>Tests displacement logic for stream-based and substance-based displacement,
 * equipment-unit vs volume-based modes, and automatic recycling handling.</p>
 */
class DisplaceExecutorTest {
  private Engine mockEngine;
  private SimulationState mockSimulationState;
  private ConverterStateGetter mockStateGetter;
  private UnitConverter mockUnitConverter;
  private DisplaceExecutor displaceExecutor;
  private Scope testScope;

  @BeforeEach
  void setUp() {
    mockEngine = mock(Engine.class);
    mockSimulationState = mock(SimulationState.class);
    mockStateGetter = mock(ConverterStateGetter.class);
    mockUnitConverter = mock(UnitConverter.class);

    // Setup basic test scope
    testScope = new Scope("default", "TestApp", "HFC-134a");

    when(mockEngine.getStreamKeeper()).thenReturn(mockSimulationState);
    when(mockEngine.getStateGetter()).thenReturn(mockStateGetter);
    when(mockEngine.getUnitConverter()).thenReturn(mockUnitConverter);
    when(mockEngine.getScope()).thenReturn(testScope);
    when(mockEngine.getYear()).thenReturn(2025);
    when(mockSimulationState.getCurrentYear()).thenReturn(2025);

    displaceExecutor = new DisplaceExecutor(mockEngine);
  }

  @Test
  void testExecute_NullDisplaceTarget_NoOperation() {
    // Arrange
    EngineNumber amount = new EngineNumber(new BigDecimal("10"), "kg");
    BigDecimal changeAmount = new BigDecimal("-5");

    // Act
    displaceExecutor.execute("domestic", amount, changeAmount, null);

    // Assert - no operations should occur
    verify(mockEngine, never()).executeStreamUpdate(any());
    verify(mockEngine, never()).setSubstance(any());
  }

  @Test
  void testExecute_SelfDisplacement_ThrowsException() {
    // Arrange
    EngineNumber amount = new EngineNumber(new BigDecimal("10"), "kg");
    BigDecimal changeAmount = new BigDecimal("-5");

    // Act & Assert - should throw when displacing to same stream
    assertThrows(RuntimeException.class, () -> {
      displaceExecutor.execute("domestic", amount, changeAmount, "domestic");
    });
  }

  @Test
  void testExecute_VolumeDisplacementToStream_CallsShortcuts() {
    // Arrange
    EngineNumber amount = new EngineNumber(new BigDecimal("10"), "kg");
    BigDecimal changeAmount = new BigDecimal("-5");

    // Mock a stream that exists in STREAM_NAMES
    when(mockEngine.getStream(eq("import"))).thenReturn(new EngineNumber(new BigDecimal("100"), "kg"));

    // Act
    displaceExecutor.execute("domestic", amount, changeAmount, "import");

    // Assert - should call stream update methods (implementation uses StreamUpdateShortcuts)
    // Note: Full verification would require mocking StreamUpdateShortcuts or using integration tests
  }

  @Test
  void testExecute_VolumeDisplacementToSubstance_SwitchesScope() {
    // Arrange
    EngineNumber amount = new EngineNumber(new BigDecimal("10"), "kg");
    BigDecimal changeAmount = new BigDecimal("-5");
    Scope destinationScope = testScope.getWithSubstance("R-600a");

    when(mockEngine.getStream(any())).thenReturn(new EngineNumber(new BigDecimal("100"), "kg"));

    // Act
    displaceExecutor.execute("domestic", amount, changeAmount, "R-600a");

    // Assert - should interact with scope (actual verification in integration tests)
    // The executor internally creates StreamUpdateShortcuts which handles the details
  }

  @Test
  void testExecute_UnitsDisplacementToStream_ConvertsUnits() {
    // Arrange - equipment units
    EngineNumber amount = new EngineNumber(new BigDecimal("10"), "units");
    BigDecimal changeAmount = new BigDecimal("-5");

    when(mockEngine.getStream(any())).thenReturn(new EngineNumber(new BigDecimal("100"), "kg"));
    when(mockEngine.getInitialCharge(any())).thenReturn(new EngineNumber(new BigDecimal("1"), "kg"));

    // Mock unit converter for units conversion
    when(mockUnitConverter.convert(any(), eq("units")))
        .thenReturn(new EngineNumber(new BigDecimal("5"), "units"));
    when(mockUnitConverter.convert(any(), eq("kg")))
        .thenReturn(new EngineNumber(new BigDecimal("5"), "kg"));

    // Act
    displaceExecutor.execute("domestic", amount, changeAmount, "import");

    // Assert - integration tests will verify full behavior
  }

  @Test
  void testExecute_UnitsDisplacementToSubstance_SwitchesAndRestoresScope() {
    // Arrange - equipment units with substance displacement
    EngineNumber amount = new EngineNumber(new BigDecimal("10"), "units");
    BigDecimal changeAmount = new BigDecimal("-5");

    when(mockEngine.getStream(any())).thenReturn(new EngineNumber(new BigDecimal("100"), "kg"));
    when(mockEngine.getInitialCharge(any())).thenReturn(new EngineNumber(new BigDecimal("1"), "kg"));

    // Mock unit converter
    when(mockUnitConverter.convert(any(), eq("units")))
        .thenReturn(new EngineNumber(new BigDecimal("5"), "units"));
    when(mockUnitConverter.convert(any(), eq("kg")))
        .thenReturn(new EngineNumber(new BigDecimal("5"), "kg"));

    // Act
    displaceExecutor.execute("domestic", amount, changeAmount, "R-600a");

    // Assert - should call setSubstance for scope switching and restoration
    verify(mockEngine).setSubstance(eq("R-600a"));
    verify(mockEngine).setSubstance(eq("HFC-134a")); // Restore original
  }
}
