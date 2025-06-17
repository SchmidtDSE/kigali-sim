/**
 * Tests for the SingleThreadEngine class.
 */

package org.kigalisim.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.Scope;
import org.kigalisim.engine.state.YearMatcher;

/**
 * Tests for the SingleThreadEngine class.
 */
public class SingleThreadEngineTest {

  /**
   * Test that SingleThreadEngine can be initialized.
   */
  @Test
  public void testInitializes() {
    SingleThreadEngine engine = new SingleThreadEngine(1, 3);
    assertNotNull(engine, "Engine should be constructable");
  }

  /**
   * Test basic getter methods.
   */
  @Test
  public void testGetters() {
    SingleThreadEngine engine = new SingleThreadEngine(1, 3);
    assertEquals(1, engine.getStartYear(), "Should return correct start year");
    assertEquals(3, engine.getEndYear(), "Should return correct end year");
    assertEquals(1, engine.getYear(), "Should return correct current year");
    assertFalse(engine.getIsDone(), "Should not be done initially");
  }

  /**
   * Test year range handling when arguments are reversed.
   * Note: This test mirrors the JavaScript "gets years reverse" test.
   */
  @Test
  public void testGettersReverse() {
    SingleThreadEngine engine = new SingleThreadEngine(30, 1);
    assertEquals(1, engine.getStartYear(), "Should return correct start year when args reversed");
    assertEquals(30, engine.getEndYear(), "Should return correct end year when args reversed");
  }

  /**
   * Test year management.
   */
  @Test
  public void testYearManagement() {
    SingleThreadEngine engine = new SingleThreadEngine(1, 3);

    assertEquals(1, engine.getYear(), "Should start at year 1");
    assertFalse(engine.getIsDone(), "Should not be done initially");

    engine.incrementYear();
    assertEquals(2, engine.getYear(), "Should be at year 2 after increment");
    assertFalse(engine.getIsDone(), "Should not be done at year 2");

    engine.incrementYear();
    assertEquals(3, engine.getYear(), "Should be at year 3 after second increment");
    assertFalse(engine.getIsDone(), "Should not be done at year 3");

    engine.incrementYear();
    assertEquals(4, engine.getYear(), "Should be at year 4 after third increment");
    assertTrue(engine.getIsDone(), "Should be done at year 4");

    assertThrows(RuntimeException.class, () -> engine.incrementYear(),
        "Should throw error when trying to increment past end");
  }

  /**
   * Test scope management.
   * Note: This test is similar to JavaScript "changes scope" test but doesn't verify scope changes.
   * JavaScript test also verifies that setSubstance changes scope correctly.
   */
  @Test
  public void testScopeManagement() {
    SingleThreadEngine engine = new SingleThreadEngine(1, 3);

    engine.setStanza("default");
    engine.setApplication("test app");
    engine.setSubstance("test substance");

    Scope scope = engine.getScope();
    assertEquals("default", scope.getStanza(), "Should have correct stanza");
    assertEquals("test app", scope.getApplication(), "Should have correct application");
    assertEquals("test substance", scope.getSubstance(), "Should have correct substance");

    // Test scope changes like JavaScript test does
    engine.setSubstance("test substance 2");

    Scope newScope = engine.getScope();
    assertEquals("default", newScope.getStanza(), "Should maintain stanza after substance change");
    assertEquals(
        "test app",
        newScope.getApplication(),
        "Should maintain application after substance change"
    );
    assertEquals("test substance 2", newScope.getSubstance(), "Should have updated substance");
  }

  /**
   * Test variable management for protected variables.
   */
  @Test
  public void testProtectedVariables() {
    SingleThreadEngine engine = new SingleThreadEngine(1, 3);

    // Test getting protected variables
    EngineNumber yearsElapsed = engine.getVariable("yearsElapsed");
    assertEquals(BigDecimal.ZERO, yearsElapsed.getValue(), "Should have 0 years elapsed initially");

    EngineNumber yearAbsolute = engine.getVariable("yearAbsolute");
    assertEquals(BigDecimal.valueOf(1), yearAbsolute.getValue(), "Should have year 1 absolute");

    // Test that protected variables cannot be defined or set
    assertThrows(RuntimeException.class, () -> engine.defineVariable("yearsElapsed"),
        "Should not allow defining yearsElapsed");
    assertThrows(RuntimeException.class, () -> engine.defineVariable("yearAbsolute"),
        "Should not allow defining yearAbsolute");
    assertThrows(RuntimeException.class,
        () -> engine.setVariable("yearsElapsed", new EngineNumber(BigDecimal.ONE, "years")),
        "Should not allow setting yearsElapsed");
    assertThrows(RuntimeException.class,
        () -> engine.setVariable("yearAbsolute", new EngineNumber(BigDecimal.ONE, "year")),
        "Should not allow setting yearAbsolute");
  }

  /**
   * Test protected variables update correctly after year increment.
   */
  @Test
  public void testProtectedVariablesUpdate() {
    SingleThreadEngine engine = new SingleThreadEngine(1, 3);

    engine.incrementYear();

    EngineNumber yearsElapsed = engine.getVariable("yearsElapsed");
    assertEquals(BigDecimal.valueOf(1), yearsElapsed.getValue(), "Should have 1 year elapsed");

    EngineNumber yearAbsolute = engine.getVariable("yearAbsolute");
    assertEquals(BigDecimal.valueOf(2), yearAbsolute.getValue(), "Should have year 2 absolute");
  }

  /**
   * Test basic stream operations.
   */
  @Test
  public void testBasicStreamOperations() {
    SingleThreadEngine engine = new SingleThreadEngine(1, 3);

    engine.setStanza("default");
    engine.setApplication("test app");
    engine.setSubstance("test substance");

    // Test getting a stream (should return zero initially)
    EngineNumber manufacture = engine.getStream("manufacture");
    assertNotNull(manufacture, "Should return a value for manufacture stream");
    assertEquals(BigDecimal.ZERO, manufacture.getValue(), "Should be zero initially");
    assertEquals("kg", manufacture.getUnits(), "Should have kg units");

    // Test setting a stream
    EngineNumber newValue = new EngineNumber(BigDecimal.valueOf(10), "kg");
    engine.setStream("manufacture", newValue, null);

    EngineNumber updated = engine.getStream("manufacture");
    assertEquals(BigDecimal.valueOf(10), updated.getValue(), "Should have updated value");
    assertEquals("kg", updated.getUnits(), "Should maintain units");
  }

  /**
   * Test that setting stream without application/substance throws error.
   */
  @Test
  public void testStreamWithoutScope() {
    SingleThreadEngine engine = new SingleThreadEngine(1, 3);

    EngineNumber value = new EngineNumber(BigDecimal.valueOf(10), "kg");
    assertThrows(RuntimeException.class, () -> engine.setStream("manufacture", value, null),
        "Should throw error when setting stream without application and substance");
  }

  /**
   * Test year matcher functionality.
   */
  @Test
  public void testYearMatcher() {
    SingleThreadEngine engine = new SingleThreadEngine(1, 3);

    engine.setStanza("default");
    engine.setApplication("test app");
    engine.setSubstance("test substance");

    // Set a stream with year matcher that should apply
    EngineNumber value = new EngineNumber(BigDecimal.valueOf(10), "kg");
    YearMatcher matcher = new YearMatcher(1, null);
    engine.setStream("manufacture", value, matcher);

    EngineNumber result = engine.getStream("manufacture");
    assertEquals(BigDecimal.valueOf(10), result.getValue(), "Should set value when year matches");

    // Set a stream with year matcher that should not apply
    EngineNumber value2 = new EngineNumber(BigDecimal.valueOf(20), "kg");
    YearMatcher matcher2 = new YearMatcher(2, null);
    engine.setStream("manufacture", value2, matcher2);

    EngineNumber result2 = engine.getStream("manufacture");
    assertEquals(BigDecimal.valueOf(10), result2.getValue(),
        "Should not change value when year doesn't match");
  }

  /**
   * Test changeStream functionality.
   */
  @Test
  public void testChangeStream() {
    SingleThreadEngine engine = new SingleThreadEngine(1, 3);

    engine.setStanza("default");
    engine.setApplication("test app");
    engine.setSubstance("test substance");

    // Set initial value
    EngineNumber initialValue = new EngineNumber(BigDecimal.valueOf(10), "kg");
    engine.setStream("manufacture", initialValue, null);

    // Change stream by a delta
    EngineNumber delta = new EngineNumber(BigDecimal.valueOf(5), "kg");
    engine.changeStream("manufacture", delta, null);

    EngineNumber result = engine.getStream("manufacture");
    assertEquals(BigDecimal.valueOf(15), result.getValue(), "Should add delta to original value");
    assertEquals("kg", result.getUnits(), "Should maintain original units");
  }

  /**
   * Test changeStream with year matcher.
   */
  @Test
  public void testChangeStreamWithYearMatcher() {
    SingleThreadEngine engine = new SingleThreadEngine(1, 3);

    engine.setStanza("default");
    engine.setApplication("test app");
    engine.setSubstance("test substance");

    // Set initial value
    EngineNumber initialValue = new EngineNumber(BigDecimal.valueOf(10), "kg");
    engine.setStream("manufacture", initialValue, null);

    // Change stream with year matcher that should apply
    EngineNumber delta = new EngineNumber(BigDecimal.valueOf(5), "kg");
    YearMatcher matcher = new YearMatcher(1, null);
    engine.changeStream("manufacture", delta, matcher);

    EngineNumber result = engine.getStream("manufacture");
    assertEquals(
        BigDecimal.valueOf(15),
        result.getValue(),
        "Should apply change when year matches"
    );

    // Try to change stream with year matcher that should not apply
    EngineNumber delta2 = new EngineNumber(BigDecimal.valueOf(10), "kg");
    YearMatcher matcher2 = new YearMatcher(2, null);
    engine.changeStream("manufacture", delta2, matcher2);

    EngineNumber result2 = engine.getStream("manufacture");
    assertEquals(BigDecimal.valueOf(15), result2.getValue(),
        "Should not apply change when year doesn't match");
  }

  /**
   * Test that mirrors the JavaScript "change stream alternative notation" test.
   */
  @Test
  public void testChangeStreamAlternativeNotation() {
    SingleThreadEngine engine = new SingleThreadEngine(1, 3);

    engine.setStanza("default");
    engine.setApplication("test app");
    engine.setSubstance("test substance");

    // Set initial value
    EngineNumber initialValue = new EngineNumber(BigDecimal.valueOf(10), "kg");
    engine.setStream("manufacture", initialValue, new YearMatcher(null, null));

    // Change by 10% when year doesn't match (should not apply)
    EngineNumber percentChange = new EngineNumber(BigDecimal.valueOf(10), "%");
    engine.changeStream("manufacture", percentChange, new YearMatcher(2, null));

    EngineNumber result1 = engine.getStream("manufacture");
    assertEquals(
        BigDecimal.valueOf(10),
        result1.getValue(),
        "Should remain 10 when year doesn't match"
    );
    assertEquals("kg", result1.getUnits(), "Should maintain kg units");

    // Increment year to 2
    engine.incrementYear();

    // Change by 10% when year matches (should apply)
    engine.changeStream("manufacture", percentChange, new YearMatcher(null, null));

    EngineNumber result2 = engine.getStream("manufacture");
    assertEquals(0, BigDecimal.valueOf(11).compareTo(result2.getValue()),
        "Should be 11 after 10% increase");
    assertEquals("kg", result2.getUnits(), "Should maintain kg units");
  }
}
