/**
 * Integration test demonstrating the new RecalcOperationBuilder pattern.
 * This test shows how the builder pattern replaces the old direct method calls.
 */

package org.kigalisim.engine.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.SingleThreadEngine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.engine.state.YearMatcher;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test demonstrating the RecalcOperationBuilder pattern usage.
 */
public class RecalcOperationBuilderIntegrationTest {

  private SingleThreadEngine engine;

  @BeforeEach
  public void setUp() {
    engine = new SingleThreadEngine(1, 10);
    
    // Set up basic application/substance context
    engine.setStanza("test");
    engine.setApplication("testApp");
    engine.setSubstance("testSubstance");
    
    // Initialize some basic streams
    engine.setStream("manufacture", new EngineNumber(100, "kg"), new YearMatcher(null, null));
    engine.setStream("import", new EngineNumber(50, "kg"), new YearMatcher(null, null));
  }

  @Test
  public void testBuilderPatternReplacesDirectCalls() {
    // This demonstrates the new pattern as specified in the issue
    Scope scopeEffective = engine.getScope();
    
    // Old way (still works but should be replaced):
    // engine.recalcPopulationChange(scopeEffective, true);
    // engine.recalcConsumption(scopeEffective);
    // engine.recalcSales(scopeEffective);
    
    // New way using builder pattern:
    RecalcOperationBuilder recalcBuilder = new RecalcOperationBuilder();
    recalcBuilder.setScopeEffective(scopeEffective)
        .setSubtractRecharge(true)
        .recalcPopulationChange()
        .thenPropagateToConsumption()
        .thenPropagateToSales();

    RecalcOperation operation = recalcBuilder.build();
    operation.execute(engine);  // Assuming inside of Engine
    
    // This should complete without error
    assertNotNull(operation);
  }

  @Test
  public void testDifferentRecalcSequences() {
    Scope scopeEffective = engine.getScope();
    
    // Test various recalc sequences as would be used in different scenarios
    
    // Sequence 1: Start with consumption, propagate to sales and population
    RecalcOperation operation1 = new RecalcOperationBuilder()
        .setScopeEffective(scopeEffective)
        .recalcConsumption()
        .thenPropagateToSales()
        .thenPropagateToPopulationChange()
        .build();
    
    assertDoesNotThrow(() -> operation1.execute(engine));
    
    // Sequence 2: Start with sales, propagate to consumption
    RecalcOperation operation2 = new RecalcOperationBuilder()
        .setScopeEffective(scopeEffective)
        .recalcSales()
        .thenPropagateToConsumption()
        .build();
    
    assertDoesNotThrow(() -> operation2.execute(engine));
    
    // Sequence 3: Start with retire (no propagation needed)
    RecalcOperation operation3 = new RecalcOperationBuilder()
        .setScopeEffective(scopeEffective)
        .recalcRetire()
        .build();
    
    assertDoesNotThrow(() -> operation3.execute(engine));
  }

  @Test
  public void testBuilderValidatesOrder() {
    // Should enforce that recalc comes before propagate
    assertThrows(IllegalStateException.class, () -> {
      new RecalcOperationBuilder()
          .thenPropagateToConsumption(); // This should fail
    });
    
    // Should enforce only one initial recalc
    assertThrows(IllegalStateException.class, () -> {
      new RecalcOperationBuilder()
          .recalcPopulationChange()
          .recalcConsumption(); // This should fail
    });
  }

  @Test
  public void testBuilderRequiresAtLeastOneOperation() {
    // Should require at least one operation
    assertThrows(IllegalStateException.class, () -> {
      new RecalcOperationBuilder()
          .setScopeEffective(engine.getScope())
          .build(); // This should fail - no operations
    });
  }
}