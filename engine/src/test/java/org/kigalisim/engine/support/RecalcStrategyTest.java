/**
 * Tests for individual recalculation strategy classes.
 */

package org.kigalisim.engine.support;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.Engine;
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
    PopulationChangeRecalcStrategy strategy = new PopulationChangeRecalcStrategy(testScope, true);
    // Just verify it can be instantiated and implements the interface
    assertTrue(strategy instanceof RecalcStrategy);
  }

  @Test
  public void testRechargeEmissionsRecalcStrategyAcceptsAnyEngine() {
    RechargeEmissionsRecalcStrategy strategy = new RechargeEmissionsRecalcStrategy(testScope);
    assertTrue(strategy instanceof RecalcStrategy);
  }

  @Test
  public void testEolEmissionsRecalcStrategyAcceptsAnyEngine() {
    EolEmissionsRecalcStrategy strategy = new EolEmissionsRecalcStrategy(testScope);
    assertTrue(strategy instanceof RecalcStrategy);
  }

  @Test
  public void testConsumptionRecalcStrategyAcceptsAnyEngine() {
    ConsumptionRecalcStrategy strategy = new ConsumptionRecalcStrategy(testScope);
    assertTrue(strategy instanceof RecalcStrategy);
  }

  @Test
  public void testSalesRecalcStrategyAcceptsAnyEngine() {
    SalesRecalcStrategy strategy = new SalesRecalcStrategy(testScope);
    assertTrue(strategy instanceof RecalcStrategy);
  }

  @Test
  public void testRetireRecalcStrategyAcceptsAnyEngine() {
    RetireRecalcStrategy strategy = new RetireRecalcStrategy(testScope);
    assertTrue(strategy instanceof RecalcStrategy);
  }

  @Test
  public void testAllStrategiesCanBeInstantiated() {
    // Test that all strategy classes can be instantiated without errors
    assertDoesNotThrow(() -> new PopulationChangeRecalcStrategy(testScope, true));
    assertDoesNotThrow(() -> new ConsumptionRecalcStrategy(testScope));
    assertDoesNotThrow(() -> new SalesRecalcStrategy(testScope));
    assertDoesNotThrow(() -> new RechargeEmissionsRecalcStrategy(testScope));
    assertDoesNotThrow(() -> new EolEmissionsRecalcStrategy(testScope));
    assertDoesNotThrow(() -> new RetireRecalcStrategy(testScope));
  }

  @Test
  public void testStrategiesAcceptNullScope() {
    // Test that strategies handle null scope appropriately (should use engine's scope)
    assertDoesNotThrow(() -> new PopulationChangeRecalcStrategy(null, true));
    assertDoesNotThrow(() -> new ConsumptionRecalcStrategy(null));
    assertDoesNotThrow(() -> new SalesRecalcStrategy(null));
    assertDoesNotThrow(() -> new RechargeEmissionsRecalcStrategy(null));
    assertDoesNotThrow(() -> new EolEmissionsRecalcStrategy(null));
    assertDoesNotThrow(() -> new RetireRecalcStrategy(null));
  }

  @Test
  public void testPopulationChangeAcceptsNullSubtractRecharge() {
    // Test that PopulationChangeRecalcStrategy handles null subtractRecharge
    assertDoesNotThrow(() -> new PopulationChangeRecalcStrategy(testScope, null));
  }

  @Test
  public void testStrategiesImplementRecalcInterface() {
    // Verify all strategies implement the RecalcStrategy interface
    RecalcStrategy popStrategy = new PopulationChangeRecalcStrategy(testScope, true);
    RecalcStrategy consStrategy = new ConsumptionRecalcStrategy(testScope);
    RecalcStrategy salesStrategy = new SalesRecalcStrategy(testScope);

    // Just verify they're non-null and implement the interface
    assertTrue(popStrategy instanceof RecalcStrategy);
    assertTrue(consStrategy instanceof RecalcStrategy);
    assertTrue(salesStrategy instanceof RecalcStrategy);

    final RecalcStrategy rechargeStrategy = new RechargeEmissionsRecalcStrategy(testScope);
    final RecalcStrategy eolStrategy = new EolEmissionsRecalcStrategy(testScope);
    final RecalcStrategy retireStrategy = new RetireRecalcStrategy(testScope);
    assertTrue(rechargeStrategy instanceof RecalcStrategy);
    assertTrue(eolStrategy instanceof RecalcStrategy);
    assertTrue(retireStrategy instanceof RecalcStrategy);
  }
}
