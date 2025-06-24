/**
 * Tests for the ParsedSubstance class.
 *
 * @license BSD-3-Clause
 */

package org.kigalisim.lang.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kigalisim.lang.machine.PushDownMachine;
import org.kigalisim.lang.operation.Operation;

/**
 * Tests for the ParsedSubstance class.
 */
public class ParsedSubstanceTest {

  private static final String SUBSTANCE_NAME = "TestSubstance";

  private Operation operation1;
  private Operation operation2;
  private List<Operation> operations;
  private ParsedSubstance substance;

  /**
   * Set up the test environment before each test.
   */
  @BeforeEach
  public void setUp() {
    operation1 = new TestOperation();
    operation2 = new TestOperation();
    operations = new ArrayList<>();
    operations.add(operation1);
    operations.add(operation2);
    substance = new ParsedSubstance(SUBSTANCE_NAME, operations);
  }

  /**
   * Test that getName returns the correct name.
   */
  @Test
  public void testGetName() {
    assertEquals(SUBSTANCE_NAME, substance.getName(), "Substance name should match");
  }

  /**
   * Test that getOperations returns all operations.
   */
  @Test
  public void testGetOperations() {
    int count = 0;
    for (Operation operation : substance.getOperations()) {
      assertSame(operations.get(count), operation, "Operation should be the same instance");
      count++;
    }
    assertEquals(operations.size(), count, "Number of operations should match");
  }

  /**
   * Simple implementation of Operation for testing.
   */
  private static class TestOperation implements Operation {
    /**
     * Minimal implementation for testing.
     *
     * @param machine The machine in which to execute the calculation if needed.
     */
    @Override
    public void execute(PushDownMachine machine) {
      // Do nothing, this is just a stub for testing
    }
  }
}
