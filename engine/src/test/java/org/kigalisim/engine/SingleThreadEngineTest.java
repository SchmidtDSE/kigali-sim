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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.recalc.StreamUpdate;
import org.kigalisim.engine.recalc.StreamUpdateBuilder;
import org.kigalisim.engine.serializer.EngineResult;
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
    EngineNumber manufacture = engine.getStream("domestic");
    assertNotNull(manufacture, "Should return a value for manufacture stream");
    assertEquals(BigDecimal.ZERO, manufacture.getValue(), "Should be zero initially");
    assertEquals("kg", manufacture.getUnits(), "Should have kg units");

    // Test setting a stream
    engine.enable("domestic", Optional.empty());
    EngineNumber newValue = new EngineNumber(BigDecimal.valueOf(10), "kg");
    StreamUpdate update = new StreamUpdateBuilder()
        .setName("domestic")
        .setValue(newValue)
        .setYearMatcher(Optional.empty())
        .inferSubtractRecycling()
        .build();
    engine.executeStreamUpdate(update);

    EngineNumber updated = engine.getStream("domestic");
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
    assertThrows(RuntimeException.class, () -> {
      StreamUpdate update = new StreamUpdateBuilder()
          .setName("domestic")
          .setValue(value)
          .setYearMatcher(Optional.empty())
          .inferSubtractRecycling()
          .build();
      engine.executeStreamUpdate(update);
    }, "Should throw error when setting stream without application and substance");
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
    engine.enable("domestic", Optional.empty());
    EngineNumber value = new EngineNumber(BigDecimal.valueOf(10), "kg");
    YearMatcher matcher = new YearMatcher(Optional.of(1), Optional.empty());
    StreamUpdate update2 = new StreamUpdateBuilder()
        .setName("domestic")
        .setValue(value)
        .setYearMatcher(Optional.ofNullable(matcher))
        .inferSubtractRecycling()
        .build();
    engine.executeStreamUpdate(update2);

    EngineNumber result = engine.getStream("domestic");
    assertEquals(BigDecimal.valueOf(10), result.getValue(), "Should set value when year matches");

    // Set a stream with year matcher that should not apply
    EngineNumber value2 = new EngineNumber(BigDecimal.valueOf(20), "kg");
    YearMatcher matcher2 = new YearMatcher(Optional.of(2), Optional.empty());
    StreamUpdate update3 = new StreamUpdateBuilder()
        .setName("domestic")
        .setValue(value2)
        .setYearMatcher(Optional.ofNullable(matcher2))
        .inferSubtractRecycling()
        .build();
    engine.executeStreamUpdate(update3);

    EngineNumber result2 = engine.getStream("domestic");
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
    engine.enable("domestic", Optional.empty());
    EngineNumber initialValue = new EngineNumber(BigDecimal.valueOf(10), "kg");
    StreamUpdate update = new StreamUpdateBuilder()
        .setName("domestic")
        .setValue(initialValue)
        .setYearMatcher(Optional.empty())
        .inferSubtractRecycling()
        .build();
    engine.executeStreamUpdate(update);

    // Change stream by a delta
    EngineNumber delta = new EngineNumber(BigDecimal.valueOf(5), "kg");
    engine.changeStream("domestic", delta, null);

    EngineNumber result = engine.getStream("domestic");
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
    engine.enable("domestic", Optional.empty());
    EngineNumber initialValue = new EngineNumber(BigDecimal.valueOf(10), "kg");
    StreamUpdate update = new StreamUpdateBuilder()
        .setName("domestic")
        .setValue(initialValue)
        .setYearMatcher(Optional.empty())
        .inferSubtractRecycling()
        .build();
    engine.executeStreamUpdate(update);

    // Change stream with year matcher that should apply
    EngineNumber delta = new EngineNumber(BigDecimal.valueOf(5), "kg");
    YearMatcher matcher = new YearMatcher(Optional.of(1), Optional.empty());
    engine.changeStream("domestic", delta, matcher);

    EngineNumber result = engine.getStream("domestic");
    assertEquals(
        BigDecimal.valueOf(15),
        result.getValue(),
        "Should apply change when year matches"
    );

    // Try to change stream with year matcher that should not apply
    EngineNumber delta2 = new EngineNumber(BigDecimal.valueOf(10), "kg");
    YearMatcher matcher2 = new YearMatcher(Optional.of(2), Optional.empty());
    engine.changeStream("domestic", delta2, matcher2);

    EngineNumber result2 = engine.getStream("domestic");
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
    engine.enable("domestic", Optional.empty());
    EngineNumber initialValue = new EngineNumber(BigDecimal.valueOf(10), "kg");
    StreamUpdate updateUnbounded = new StreamUpdateBuilder()
        .setName("domestic")
        .setValue(initialValue)
        .setYearMatcher(Optional.ofNullable(YearMatcher.unbounded()))
        .inferSubtractRecycling()
        .build();
    engine.executeStreamUpdate(updateUnbounded);

    // Change by 10% when year doesn't match (should not apply)
    EngineNumber percentChange = new EngineNumber(BigDecimal.valueOf(10), "%");
    engine.changeStream("domestic", percentChange, new YearMatcher(2, null));

    EngineNumber result1 = engine.getStream("domestic");
    assertEquals(
        BigDecimal.valueOf(10),
        result1.getValue(),
        "Should remain 10 when year doesn't match"
    );
    assertEquals("kg", result1.getUnits(), "Should maintain kg units");

    // Increment year to 2
    engine.incrementYear();

    // Change by 10% when year matches (should apply)
    engine.changeStream("domestic", percentChange, YearMatcher.unbounded());

    EngineNumber result2 = engine.getStream("domestic");
    assertEquals(0, BigDecimal.valueOf(11).compareTo(result2.getValue()),
        "Should be 11 after 10% increase");
    assertEquals("kg", result2.getUnits(), "Should maintain kg units");
  }

  /**
   * Test getResults returns empty list when no substances are registered.
   */
  @Test
  public void testGetResultsEmpty() {
    SingleThreadEngine engine = new SingleThreadEngine(1, 3);

    List<EngineResult> results = engine.getResults();

    assertNotNull(results, "Should return a non-null list");
    assertTrue(results.isEmpty(), "Should return empty list when no substances registered");
  }

  /**
   * Test getResults returns results for registered substances.
   */
  @Test
  public void testGetResultsWithSubstances() {
    SingleThreadEngine engine = new SingleThreadEngine(2020, 2025);

    // Set up first substance
    engine.setStanza("default");
    engine.setApplication("test app");
    engine.setSubstance("test substance");

    // Set some stream values to ensure substance is registered
    engine.enable("domestic", Optional.empty());
    EngineNumber manufactureValue = new EngineNumber(BigDecimal.valueOf(100), "kg");
    StreamUpdate manufactureUpdate = new StreamUpdateBuilder()
        .setName("domestic")
        .setValue(manufactureValue)
        .setYearMatcher(Optional.empty())
        .inferSubtractRecycling()
        .build();
    engine.executeStreamUpdate(manufactureUpdate);

    // Set up second substance
    engine.setApplication("test app 2");
    engine.setSubstance("test substance 2");

    engine.enable("import", Optional.empty());
    EngineNumber importValue = new EngineNumber(BigDecimal.valueOf(50), "kg");
    StreamUpdate importUpdate = new StreamUpdateBuilder()
        .setName("import")
        .setValue(importValue)
        .setYearMatcher(Optional.empty())
        .inferSubtractRecycling()
        .build();
    engine.executeStreamUpdate(importUpdate);

    // Get results
    List<EngineResult> results = engine.getResults();

    // Verify results
    assertNotNull(results, "Should return a non-null list");
    assertEquals(2, results.size(), "Should return results for both substances");

    // Check that results contain expected data
    for (EngineResult result : results) {
      assertNotNull(result.getApplication(), "Result should have application");
      assertNotNull(result.getSubstance(), "Result should have substance");
      assertEquals(2020, result.getYear(), "Result should have current year");

      // Verify we have expected applications and substances
      boolean isFirstExpected = "test app".equals(result.getApplication())
          && "test substance".equals(result.getSubstance());
      boolean isSecondExpected = "test app 2".equals(result.getApplication())
          && "test substance 2".equals(result.getSubstance());
      boolean isExpectedCombination = isFirstExpected || isSecondExpected;
      assertTrue(isExpectedCombination,
          "Result should contain expected app/substance combinations");
    }
  }

  /**
   * Test getResults reflects current year.
   */
  @Test
  public void testGetResultsCurrentYear() {
    SingleThreadEngine engine = new SingleThreadEngine(2020, 2025);

    // Set up substance
    engine.setStanza("default");
    engine.setApplication("test app");
    engine.setSubstance("test substance");
    engine.enable("domestic", Optional.empty());
    StreamUpdate update100 = new StreamUpdateBuilder()
        .setName("domestic")
        .setValue(new EngineNumber(BigDecimal.valueOf(100), "kg"))
        .setYearMatcher(Optional.empty())
        .inferSubtractRecycling()
        .build();
    engine.executeStreamUpdate(update100);

    // Get results for initial year
    List<EngineResult> results1 = engine.getResults();
    assertEquals(1, results1.size(), "Should have one result");
    assertEquals(2020, results1.get(0).getYear(), "Should have year 2020");

    // Increment year and get results again
    engine.incrementYear();
    List<EngineResult> results2 = engine.getResults();
    assertEquals(1, results2.size(), "Should still have one result");
    assertEquals(2021, results2.get(0).getYear(), "Should now have year 2021");
  }

  /**
   * Test cap with displacement across different substances using equipment units.
   * This tests the unit-based displacement logic when substances have different
   * initial charges per unit.
   */
  @Test
  public void testCapDisplacementWithUnits() {
    SingleThreadEngine engine = new SingleThreadEngine(1, 3);

    engine.setStanza("default");
    engine.setApplication("test app");

    // Set up sub1 with 10 kg/unit initial charge
    engine.setSubstance("sub1");
    engine.enable("domestic", Optional.empty());
    engine.setInitialCharge(
        new EngineNumber(BigDecimal.valueOf(10), "kg / unit"),
        "domestic",
        null
    );
    StreamUpdate update100 = new StreamUpdateBuilder()
        .setName("domestic")
        .setValue(new EngineNumber(BigDecimal.valueOf(100), "kg"))
        .setYearMatcher(Optional.empty())
        .inferSubtractRecycling()
        .build();
    engine.executeStreamUpdate(update100);
    StreamUpdate update4 = new StreamUpdateBuilder()
        .setName("priorEquipment")
        .setValue(new EngineNumber(BigDecimal.valueOf(20), "units"))
        .setYearMatcher(Optional.empty())
        .inferSubtractRecycling()
        .build();
    engine.executeStreamUpdate(update4);
    engine.recharge(
        new EngineNumber(BigDecimal.valueOf(10), "%"),
        new EngineNumber(BigDecimal.valueOf(10), "kg / unit"),
        null
    );

    // Set up sub2 with 20 kg/unit initial charge
    engine.setSubstance("sub2");
    engine.enable("domestic", Optional.empty());
    engine.setInitialCharge(
        new EngineNumber(BigDecimal.valueOf(20), "kg / unit"),
        "domestic",
        null
    );
    StreamUpdate update200 = new StreamUpdateBuilder()
        .setName("domestic")
        .setValue(new EngineNumber(BigDecimal.valueOf(200), "kg"))
        .setYearMatcher(Optional.empty())
        .inferSubtractRecycling()
        .build();
    engine.executeStreamUpdate(update200);

    // Apply cap with displacement
    engine.setSubstance("sub1");
    engine.cap(
        "domestic",
        new EngineNumber(BigDecimal.valueOf(5), "units"),
        null,
        "sub2"
    );

    // Check sub1 was capped: 5 units * 10 kg/unit + recharge
    // (20 units * 10% * 10 kg/unit) = 50 + 20 = 70 kg
    EngineNumber capVal = engine.getStream("domestic");
    assertEquals(0, BigDecimal.valueOf(70).compareTo(capVal.getValue()),
        "Sub1 should be capped to 70 kg (50 kg + 20 kg recharge)");
    assertEquals("kg", capVal.getUnits(), "Sub1 should have kg units");

    // Check sub2 received displacement: original 200 kg + displaced units
    // converted to sub2's charge
    // Original sub1 manufacture: 100 kg (no recharge added when specified in kg), after cap: 70 kg, displaced: 30 kg
    // 30 kg displaced from sub1 = 30 kg / 10 kg/unit = 3 units
    // 3 units in sub2 = 3 units * 20 kg/unit = 60 kg
    // Final sub2: 200 kg + 60 kg = 260 kg
    engine.setSubstance("sub2");
    EngineNumber displaceVal = engine.getStream("domestic");
    assertEquals(0, BigDecimal.valueOf(260).compareTo(displaceVal.getValue()),
        "Sub2 should receive displaced units: 200 kg + 60 kg = 260 kg");
    assertEquals("kg", displaceVal.getUnits(), "Sub2 should have kg units");
  }

  /**
   * Test floor with displacement across different substances using equipment units.
   * This tests the unit-based displacement logic when substances have different
   * initial charges per unit.
   */
  @Test
  public void testFloorDisplacementWithUnits() {
    SingleThreadEngine engine = new SingleThreadEngine(1, 3);

    engine.setStanza("default");
    engine.setApplication("test app");

    // Set up sub1 with 10 kg/unit initial charge
    engine.setSubstance("sub1");
    engine.enable("domestic", Optional.empty());
    engine.setInitialCharge(
        new EngineNumber(BigDecimal.valueOf(10), "kg / unit"),
        "domestic",
        null
    );
    StreamUpdate update50 = new StreamUpdateBuilder()
        .setName("domestic")
        .setValue(new EngineNumber(BigDecimal.valueOf(50), "kg"))
        .setYearMatcher(Optional.empty())
        .inferSubtractRecycling()
        .build();
    engine.executeStreamUpdate(update50);
    StreamUpdate update4 = new StreamUpdateBuilder()
        .setName("priorEquipment")
        .setValue(new EngineNumber(BigDecimal.valueOf(20), "units"))
        .setYearMatcher(Optional.empty())
        .inferSubtractRecycling()
        .build();
    engine.executeStreamUpdate(update4);
    engine.recharge(
        new EngineNumber(BigDecimal.valueOf(10), "%"),
        new EngineNumber(BigDecimal.valueOf(10), "kg / unit"),
        null
    );

    // Set up sub2 with 20 kg/unit initial charge
    engine.setSubstance("sub2");
    engine.enable("domestic", Optional.empty());
    engine.setInitialCharge(
        new EngineNumber(BigDecimal.valueOf(20), "kg / unit"),
        "domestic",
        null
    );
    StreamUpdate update200 = new StreamUpdateBuilder()
        .setName("domestic")
        .setValue(new EngineNumber(BigDecimal.valueOf(200), "kg"))
        .setYearMatcher(Optional.empty())
        .inferSubtractRecycling()
        .build();
    engine.executeStreamUpdate(update200);

    // Apply floor with displacement
    engine.setSubstance("sub1");
    engine.floor(
        "domestic",
        new EngineNumber(BigDecimal.valueOf(10), "units"),
        null,
        "sub2"
    );

    // Check sub1 was floored: 10 units * 10 kg/unit + recharge
    // (20 units * 10% * 10 kg/unit) = 100 + 20 = 120 kg
    EngineNumber floorVal = engine.getStream("domestic");
    assertEquals(0, BigDecimal.valueOf(120).compareTo(floorVal.getValue()),
        "Sub1 should be floored to 120 kg (100 kg + 20 kg recharge)");
    assertEquals("kg", floorVal.getUnits(), "Sub1 should have kg units");

    // Check sub2 received displacement: original 200 kg - displaced units
    // converted to sub2's charge
    // Original sub1 manufacture: 50 kg (no recharge added when specified in kg), after floor: 120 kg (10 units * 10 kg/unit + 20 kg recharge), displaced: +70 kg
    // 70 kg added to sub1 = 70 kg / 10 kg/unit = 7 units
    // 7 units removed from sub2 = 7 units * 20 kg/unit = 140 kg
    // Final sub2: 200 kg - 140 kg = 60 kg
    engine.setSubstance("sub2");
    EngineNumber displaceVal = engine.getStream("domestic");
    assertEquals(0, BigDecimal.valueOf(60).compareTo(displaceVal.getValue()),
        "Sub2 should receive displaced units: 200 kg - 140 kg = 60 kg");
    assertEquals("kg", displaceVal.getUnits(), "Sub2 should have kg units");
  }

  /**
   * Test replace with units converts correctly between substances.
   * This mirrors the JavaScript test to ensure the Java implementation
   * handles equipment units properly when replacing between substances
   * with different initial charges per unit.
   */
  @Test
  public void testReplaceWithUnitsConvertsCorrectlyBetweenSubstances() {
    SingleThreadEngine engine = new SingleThreadEngine(1, 3);

    engine.setStanza("default");
    engine.setApplication("test app");

    // Set up substance A with 10 kg/unit initial charge
    engine.setSubstance("sub A");
    engine.enable("domestic", Optional.empty());
    engine.setInitialCharge(
        new EngineNumber(BigDecimal.valueOf(10), "kg / unit"),
        "domestic",
        YearMatcher.unbounded()
    );
    StreamUpdate updateMultiA = new StreamUpdateBuilder()
        .setName("domestic")
        .setValue(new EngineNumber(BigDecimal.valueOf(50), "kg"))
        .setYearMatcher(Optional.of(YearMatcher.unbounded()))
        .inferSubtractRecycling()
        .build();
    engine.executeStreamUpdate(updateMultiA);

    // Set up substance B with 20 kg/unit initial charge
    engine.setSubstance("sub B");
    engine.enable("domestic", Optional.empty());
    engine.setInitialCharge(
        new EngineNumber(BigDecimal.valueOf(20), "kg / unit"),
        "domestic",
        YearMatcher.unbounded()
    );
    StreamUpdate updateMultiB = new StreamUpdateBuilder()
        .setName("domestic")
        .setValue(new EngineNumber(BigDecimal.valueOf(0), "kg"))
        .setYearMatcher(Optional.of(YearMatcher.unbounded()))
        .inferSubtractRecycling()
        .build();
    engine.executeStreamUpdate(updateMultiB);

    // Go back to substance A and replace 2 units with substance B
    engine.setSubstance("sub A");
    engine.replace(
        new EngineNumber(BigDecimal.valueOf(2), "units"),
        "domestic",
        "sub B",
        YearMatcher.unbounded()
    );

    // Check substance A: should lose 2 units * 10 kg/unit = 20 kg
    // Original: 50 kg, after replace: 30 kg
    EngineNumber substanceOneResult = engine.getStream("domestic");
    assertEquals(0, BigDecimal.valueOf(30).compareTo(substanceOneResult.getValue()),
        "Sub A should lose 20 kg (2 units * 10 kg/unit): 50 - 20 = 30");
    assertEquals("kg", substanceOneResult.getUnits(), "Sub A should have kg units");

    // Check substance B: should gain 2 units * 20 kg/unit = 40 kg
    // Original: 0 kg, after replace: 40 kg
    engine.setSubstance("sub B");
    EngineNumber substanceTwoResult = engine.getStream("domestic");
    assertEquals(0, BigDecimal.valueOf(40).compareTo(substanceTwoResult.getValue()),
        "Sub B should gain 40 kg (2 units * 20 kg/unit): 0 + 40 = 40");
    assertEquals("kg", substanceTwoResult.getUnits(), "Sub B should have kg units");
  }

  /**
   * Test enable method for manufacture stream.
   */
  @Test
  public void testEnableManufactureStream() {
    SingleThreadEngine engine = new SingleThreadEngine(2020, 2025);

    // Set up the engine with a scope
    engine.setStanza("default");
    engine.setApplication("test app");
    engine.setSubstance("test substance");

    // Enable manufacture stream
    engine.enable("domestic", Optional.empty());

    // Test passes if no exception is thrown
    assertTrue(true, "Enable method should execute without error for manufacture stream");
  }

  /**
   * Test enable method for import stream.
   */
  @Test
  public void testEnableImportStream() {
    SingleThreadEngine engine = new SingleThreadEngine(2020, 2025);

    // Set up the engine with a scope
    engine.setStanza("default");
    engine.setApplication("test app");
    engine.setSubstance("test substance");

    // Enable import stream
    engine.enable("import", Optional.empty());

    // Test passes if no exception is thrown
    assertTrue(true, "Enable method should execute without error for import stream");
  }

  /**
   * Test enable method for export stream.
   */
  @Test
  public void testEnableExportStream() {
    SingleThreadEngine engine = new SingleThreadEngine(2020, 2025);

    // Set up the engine with a scope
    engine.setStanza("default");
    engine.setApplication("test app");
    engine.setSubstance("test substance");

    // Enable export stream
    engine.enable("export", Optional.empty());

    // Test passes if no exception is thrown
    assertTrue(true, "Enable method should execute without error for export stream");
  }

  /**
   * Test enable method with year matcher.
   */
  @Test
  public void testEnableWithYearMatcher() {
    SingleThreadEngine engine = new SingleThreadEngine(2020, 2025);

    // Set up the engine with a scope
    engine.setStanza("default");
    engine.setApplication("test app");
    engine.setSubstance("test substance");

    // Enable manufacture stream with year matcher
    YearMatcher yearMatcher = YearMatcher.unbounded();
    engine.enable("domestic", Optional.of(yearMatcher));

    // Test passes if no exception is thrown
    assertTrue(true, "Enable method should execute without error with year matcher");
  }

  /**
   * Test enable method throws error without scope.
   */
  @Test
  public void testEnableThrowsWithoutScope() {
    SingleThreadEngine engine = new SingleThreadEngine(2020, 2025);

    // Don't set up scope - should throw error
    assertThrows(RuntimeException.class, () -> engine.enable("domestic", Optional.empty()),
        "Enable method should throw error when called without proper scope");
  }

  /**
   * Test enable method ignores invalid stream names.
   */
  @Test
  public void testEnableIgnoresInvalidStreams() {
    SingleThreadEngine engine = new SingleThreadEngine(2020, 2025);

    // Set up the engine with a scope
    engine.setStanza("default");
    engine.setApplication("test app");
    engine.setSubstance("test substance");

    // Try to enable invalid stream - should throw error
    assertThrows(RuntimeException.class, () -> engine.enable("invalidStream", Optional.empty()),
        "Enable method should throw RuntimeException for unknown stream names");
  }
}
