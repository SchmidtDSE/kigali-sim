/**
 * Tests for individual recalculation strategy classes.
 */

package org.kigalisim.engine.recalc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.ConverterStateGetter;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.engine.state.StreamKeeper;

/**
 * Test class for individual recalculation strategy classes.
 *
 * <p>These tests focus on verifying the strategy pattern implementation
 * with RecalcKit dependency injection. Full integration testing is covered
 * by existing engine tests.</p>
 */
public class RecalcStrategyTest {

  private Scope testScope;
  private RecalcKit testKit;

  /**
   * Set up test data.
   */
  @BeforeEach
  public void setUp() {
    testScope = new Scope("test", "testApp", "testSubstance");

    // Create a mock RecalcKit for testing
    StreamKeeper streamKeeper = mock(StreamKeeper.class);
    UnitConverter unitConverter = mock(UnitConverter.class);
    ConverterStateGetter stateGetter = mock(ConverterStateGetter.class);

    testKit = new RecalcKit(streamKeeper, unitConverter, stateGetter);
  }

  @Test
  public void testPopulationChangeRecalcStrategyAcceptsAnyEngine() {
    PopulationChangeRecalcStrategy strategy = new PopulationChangeRecalcStrategy(Optional.of(testScope), Optional.of(true));
    // Just verify it can be instantiated and implements the interface
    assertTrue(strategy instanceof RecalcStrategy);
  }

  @Test
  public void testRechargeEmissionsRecalcStrategyAcceptsAnyEngine() {
    RechargeEmissionsRecalcStrategy strategy = new RechargeEmissionsRecalcStrategy(Optional.of(testScope));
    assertTrue(strategy instanceof RecalcStrategy);
  }

  @Test
  public void testEolEmissionsRecalcStrategyAcceptsAnyEngine() {
    EolEmissionsRecalcStrategy strategy = new EolEmissionsRecalcStrategy(Optional.of(testScope));
    assertTrue(strategy instanceof RecalcStrategy);
  }

  @Test
  public void testConsumptionRecalcStrategyAcceptsAnyEngine() {
    ConsumptionRecalcStrategy strategy = new ConsumptionRecalcStrategy(Optional.of(testScope));
    assertTrue(strategy instanceof RecalcStrategy);
  }

  @Test
  public void testSalesRecalcStrategyAcceptsAnyEngine() {
    SalesRecalcStrategy strategy = new SalesRecalcStrategy(Optional.of(testScope));
    assertTrue(strategy instanceof RecalcStrategy);
  }

  @Test
  public void testRetireRecalcStrategyAcceptsAnyEngine() {
    RetireRecalcStrategy strategy = new RetireRecalcStrategy(Optional.of(testScope));
    assertTrue(strategy instanceof RecalcStrategy);
  }

  @Test
  public void testAllStrategiesCanBeInstantiated() {
    // Test that all strategy classes can be instantiated without errors
    assertDoesNotThrow(() -> new PopulationChangeRecalcStrategy(Optional.of(testScope), Optional.of(true)));
    assertDoesNotThrow(() -> new ConsumptionRecalcStrategy(Optional.of(testScope)));
    assertDoesNotThrow(() -> new SalesRecalcStrategy(Optional.of(testScope)));
    assertDoesNotThrow(() -> new RechargeEmissionsRecalcStrategy(Optional.of(testScope)));
    assertDoesNotThrow(() -> new EolEmissionsRecalcStrategy(Optional.of(testScope)));
    assertDoesNotThrow(() -> new RetireRecalcStrategy(Optional.of(testScope)));
  }

  @Test
  public void testStrategiesAcceptNullScope() {
    // Test that strategies handle null scope appropriately (should use engine's scope)
    assertDoesNotThrow(() -> new PopulationChangeRecalcStrategy(Optional.empty(), Optional.of(true)));
    assertDoesNotThrow(() -> new ConsumptionRecalcStrategy(Optional.empty()));
    assertDoesNotThrow(() -> new SalesRecalcStrategy(Optional.empty()));
    assertDoesNotThrow(() -> new RechargeEmissionsRecalcStrategy(Optional.empty()));
    assertDoesNotThrow(() -> new EolEmissionsRecalcStrategy(Optional.empty()));
    assertDoesNotThrow(() -> new RetireRecalcStrategy(Optional.empty()));
  }

  @Test
  public void testPopulationChangeAcceptsNullSubtractRecharge() {
    // Test that PopulationChangeRecalcStrategy handles null subtractRecharge
    assertDoesNotThrow(() -> new PopulationChangeRecalcStrategy(Optional.of(testScope), Optional.empty()));
  }

  @Test
  public void testStrategiesImplementRecalcInterface() {
    // Verify all strategies implement the RecalcStrategy interface
    RecalcStrategy popStrategy = new PopulationChangeRecalcStrategy(Optional.of(testScope), Optional.of(true));
    RecalcStrategy consStrategy = new ConsumptionRecalcStrategy(Optional.of(testScope));
    RecalcStrategy salesStrategy = new SalesRecalcStrategy(Optional.of(testScope));

    // Just verify they're non-null and implement the interface
    assertTrue(popStrategy instanceof RecalcStrategy);
    assertTrue(consStrategy instanceof RecalcStrategy);
    assertTrue(salesStrategy instanceof RecalcStrategy);

    final RecalcStrategy rechargeStrategy = new RechargeEmissionsRecalcStrategy(Optional.of(testScope));
    final RecalcStrategy eolStrategy = new EolEmissionsRecalcStrategy(Optional.of(testScope));
    final RecalcStrategy retireStrategy = new RetireRecalcStrategy(Optional.of(testScope));
    assertTrue(rechargeStrategy instanceof RecalcStrategy);
    assertTrue(eolStrategy instanceof RecalcStrategy);
    assertTrue(retireStrategy instanceof RecalcStrategy);
  }
}
