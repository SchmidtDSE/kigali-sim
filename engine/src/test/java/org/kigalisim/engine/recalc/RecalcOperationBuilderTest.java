/**
 * Tests for recalculation strategy classes.
 */

package org.kigalisim.engine.recalc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.SingleThreadEngine;
import org.kigalisim.engine.state.Scope;

/**
 * Test class for RecalcOperationBuilder and related classes.
 */
public class RecalcOperationBuilderTest {

  private SingleThreadEngine engine;
  private Scope testScope;

  /**
   * Set up test engine with application and substance context.
   */
  @BeforeEach
  public void setUp() {
    engine = new SingleThreadEngine(1, 10);
    testScope = new Scope("test", "testApp", "testSubstance");
  }

  @Test
  public void testBuilderRequiresInitialRecalc() {
    RecalcOperationBuilder builder = new RecalcOperationBuilder();

    Exception exception = assertThrows(IllegalStateException.class, () -> {
      builder.thenPropagateToConsumption();
    });

    assertTrue(exception.getMessage().contains("Must call a recalc method"));
  }

  @Test
  public void testBuilderPreventsMultipleRecalcs() {
    RecalcOperationBuilder builder = new RecalcOperationBuilder();
    builder.recalcPopulationChange();

    Exception exception = assertThrows(IllegalStateException.class, () -> {
      builder.recalcConsumption();
    });

    assertTrue(exception.getMessage().contains("Only one recalc method can be called"));
  }

  @Test
  public void testBuilderRequiresAtLeastOneStrategy() {
    RecalcOperationBuilder builder = new RecalcOperationBuilder();

    Exception exception = assertThrows(IllegalStateException.class, () -> {
      builder.build();
    });

    assertTrue(exception.getMessage().contains(
        "Must configure at least one recalculation strategy"));
  }

  @Test
  public void testBuilderRequiresRecalcKit() {
    RecalcOperationBuilder builder = new RecalcOperationBuilder();

    Exception exception = assertThrows(IllegalStateException.class, () -> {
      builder.recalcPopulationChange().build();
    });

    assertTrue(exception.getMessage().contains("RecalcKit is required"));
  }

  @Test
  public void testBuilderFluentInterface() {
    RecalcKit recalcKit = new RecalcKitBuilder()
        .setStreamKeeper(engine.getStreamKeeper())
        .setUnitConverter(engine.getUnitConverter())
        .setStateGetter(engine.getStateGetter())
        .build();

    RecalcOperationBuilder builder = new RecalcOperationBuilder();

    RecalcOperation operation = builder
        .setScopeEffective(testScope)
        .setUseExplicitRecharge(true)
        .setRecalcKit(recalcKit)
        .recalcPopulationChange()
        .thenPropagateToConsumption()
        .thenPropagateToSales()
        .build();

    assertNotNull(operation);
  }

  @Test
  public void testRecalcOperationExecution() {
    // Set up the engine with proper application/substance context
    engine.setStanza("test");
    engine.setApplication("testApp");
    engine.setSubstance("testSubstance");

    // Create RecalcKit for the operation
    RecalcKit recalcKit = new RecalcKitBuilder()
        .setStreamKeeper(engine.getStreamKeeper())
        .setUnitConverter(engine.getUnitConverter())
        .setStateGetter(engine.getStateGetter())
        .build();

    RecalcOperationBuilder builder = new RecalcOperationBuilder();

    RecalcOperation operation = builder
        .setRecalcKit(recalcKit)
        .recalcConsumption()
        .build();

    // This should not throw an exception now that we have proper context
    assertDoesNotThrow(() -> operation.execute(engine));
  }

  @Test
  public void testIndvidualStrategies() {
    // Set up the engine with proper application/substance context
    engine.setStanza("test");
    engine.setApplication("testApp");
    engine.setSubstance("testSubstance");

    // Enable streams to enable sales distribution calculation
    engine.enable("import", Optional.empty());

    // Create a RecalcKit for the strategies
    RecalcKit recalcKit = new RecalcKitBuilder()
        .setStreamKeeper(engine.getStreamKeeper())
        .setUnitConverter(engine.getUnitConverter())
        .setStateGetter(engine.getStateGetter())
        .build();

    // Test individual strategy creation and execution
    PopulationChangeRecalcStrategy popStrategy =
        new PopulationChangeRecalcStrategy(Optional.of(testScope), Optional.of(true));
    assertDoesNotThrow(() -> popStrategy.execute(engine, recalcKit));

    ConsumptionRecalcStrategy consumptionStrategy =
        new ConsumptionRecalcStrategy(Optional.of(testScope));
    assertDoesNotThrow(() -> consumptionStrategy.execute(engine, recalcKit));

    SalesRecalcStrategy salesStrategy =
        new SalesRecalcStrategy(Optional.of(testScope));
    assertDoesNotThrow(() -> salesStrategy.execute(engine, recalcKit));

    RechargeEmissionsRecalcStrategy rechargeStrategy =
        new RechargeEmissionsRecalcStrategy(Optional.of(testScope));
    assertDoesNotThrow(() -> rechargeStrategy.execute(engine, recalcKit));

    EolEmissionsRecalcStrategy eolStrategy =
        new EolEmissionsRecalcStrategy(Optional.of(testScope));
    assertDoesNotThrow(() -> eolStrategy.execute(engine, recalcKit));

    RetireRecalcStrategy retireStrategy =
        new RetireRecalcStrategy(Optional.of(testScope));
    assertDoesNotThrow(() -> retireStrategy.execute(engine, recalcKit));
  }

  @Test
  public void testAllRecalcMethods() {
    // Create a RecalcKit for the tests
    RecalcKit recalcKit = new RecalcKitBuilder()
        .setStreamKeeper(engine.getStreamKeeper())
        .setUnitConverter(engine.getUnitConverter())
        .setStateGetter(engine.getStateGetter())
        .build();

    // Test each recalc method can be used as the initial method
    assertDoesNotThrow(() -> {
      new RecalcOperationBuilder()
          .setRecalcKit(recalcKit)
          .recalcPopulationChange()
          .build();
    });

    assertDoesNotThrow(() -> {
      new RecalcOperationBuilder()
          .setRecalcKit(recalcKit)
          .recalcConsumption()
          .build();
    });

    assertDoesNotThrow(() -> {
      new RecalcOperationBuilder()
          .setRecalcKit(recalcKit)
          .recalcSales()
          .build();
    });

    assertDoesNotThrow(() -> {
      new RecalcOperationBuilder()
          .setRecalcKit(recalcKit)
          .recalcRechargeEmissions()
          .build();
    });

    assertDoesNotThrow(() -> {
      new RecalcOperationBuilder()
          .setRecalcKit(recalcKit)
          .recalcEolEmissions()
          .build();
    });

    assertDoesNotThrow(() -> {
      new RecalcOperationBuilder()
          .setRecalcKit(recalcKit)
          .recalcRetire()
          .build();
    });
  }

  @Test
  public void testAllPropagateMethods() {
    // Set up the engine with proper application/substance context
    engine.setStanza("test");
    engine.setApplication("testApp");
    engine.setSubstance("testSubstance");

    // Enable streams to enable sales distribution calculation
    engine.enable("import", Optional.empty());

    // Create RecalcKit for the operation
    RecalcKit recalcKit = new RecalcKitBuilder()
        .setStreamKeeper(engine.getStreamKeeper())
        .setUnitConverter(engine.getUnitConverter())
        .setStateGetter(engine.getStateGetter())
        .build();

    RecalcOperationBuilder builder = new RecalcOperationBuilder();

    RecalcOperation operation = builder
        .setRecalcKit(recalcKit)
        .recalcPopulationChange()
        .thenPropagateToConsumption()
        .thenPropagateToSales()
        .thenPropagateToRechargeEmissions()
        .thenPropagateToEolEmissions()
        .thenPropagateToRetire()
        .thenPropagateToPopulationChange()
        .build();

    assertNotNull(operation);
    assertDoesNotThrow(() -> operation.execute(engine));
  }
}
