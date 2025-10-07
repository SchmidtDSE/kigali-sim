# Equipment Stream Handling Implementation Tasks

## Overview
Fix equipment stream handling in QubecTalk to properly account for recharge needs when equipment levels change. Currently, direct equipment modifications bypass sales/recharge logic, causing incorrect calculations.

## Problem Statement
When users execute `set equipment to X units` in QubecTalk, the system:
- ❌ Bypasses sales stream logic
- ❌ Doesn't generate imports/domestic for recharge needs
- ❌ Results in 0 imports when population decreases (failing test)

**Same issue affects:** `change equipment`, `cap equipment`, `floor equipment`, and their `priorEquipment` variants.

## Solution Architecture
Create `EquipmentChangeUtil` in `org.kigalisim.engine.support` to centralize all equipment modification logic:
- Converts equipment operations → sales/retire operations
- Handles recharge automatically through sales mechanism
- Preserves displacement functionality for cap/floor
- Enables isolated unit testing

---

## Phase 1: Create EquipmentChangeUtil Class

### Task 1.1: Create EquipmentChangeUtil.java
**File:** `/home/ubuntu/kigali-sim/engine/src/main/java/org/kigalisim/engine/support/EquipmentChangeUtil.java`

**Class structure:**
```java
package org.kigalisim.engine.support;

import java.math.BigDecimal;
import java.util.Optional;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.number.UnitConverter;
import org.kigalisim.engine.state.YearMatcher;

/**
 * Utility class for handling equipment stream modifications.
 *
 * <p>This class provides logic for set, change, cap, and floor operations on
 * equipment streams. It converts equipment operations into appropriate sales
 * and retirement operations to ensure recharge needs are properly handled.</p>
 *
 * @license BSD-3-Clause
 */
public class EquipmentChangeUtil {
  private final Engine engine;

  /**
   * Creates a new EquipmentChangeUtil for the given engine.
   *
   * @param engine The Engine instance to operate on
   */
  public EquipmentChangeUtil(Engine engine) {
    this.engine = engine;
  }

  // Public handler methods to be implemented in subsequent tasks
  // ... (see tasks 1.2-1.5)
}
```

**Acceptance criteria:**
- [ ] File created in correct package
- [ ] Constructor takes Engine parameter
- [ ] Follows Google Java Style Guide
- [ ] Includes proper license header and Javadoc

---

### Task 1.2: Add handleSet method
**Location:** `EquipmentChangeUtil.java`

**Method signature:**
```java
/**
 * Handle setting equipment to a target value.
 *
 * <p>Compares target equipment level with current level:
 * - If higher: converts delta to sales operation (handles recharge automatically)
 * - If lower: sets sales to 0 and retires excess from priorEquipment
 * - If equal: no action needed</p>
 *
 * @param targetEquipment The target equipment level in units
 * @param yearMatcher Optional year matcher for conditional setting
 */
public void handleSet(EngineNumber targetEquipment, Optional<YearMatcher> yearMatcher) {
  // Implementation
}
```

**Implementation logic:**
1. Check year range with `EngineSupportUtils.isInRange()`
2. Get current equipment level using `engine.getStream("equipment")`
3. Convert both to units using `UnitConverter`
4. Calculate delta: `targetUnits - currentUnits`
5. If delta > 0: call `convertToSalesIncrease()` (see Task 1.6)
6. If delta < 0: call `retireEquipment()` (see Task 1.6)
7. If delta == 0: return (no action)

**Acceptance criteria:**
- [ ] Method added with correct signature
- [ ] Handles increase, decrease, and equal cases
- [ ] Uses helper methods for sales/retire operations
- [ ] Includes comprehensive Javadoc
- [ ] Updates `lastSpecifiedValue` for unit tracking

---

### Task 1.3: Add handleChange method
**Location:** `EquipmentChangeUtil.java`

**Method signature:**
```java
/**
 * Handle changing equipment by a delta amount.
 *
 * <p>Supports both percentage and absolute changes:
 * - Percentage: change equipment by +8%
 * - Absolute: change equipment by +100 units</p>
 *
 * @param changeAmount The change amount (percentage or units)
 * @param yearMatcher Matcher to determine if change applies to current year
 */
public void handleChange(EngineNumber changeAmount, YearMatcher yearMatcher) {
  // Implementation
}
```

**Implementation logic:**
1. Check year range
2. Get current equipment level
3. If changeAmount has "%" units:
   - Calculate percentage delta using `calculatePercentageChange()` (see Task 1.6)
4. Else if changeAmount has "units":
   - Use absolute delta directly
5. Calculate new equipment level: `current + delta`
6. If delta > 0: call `convertToSalesIncrease()`
7. If delta < 0: call `retireEquipment()`

**Acceptance criteria:**
- [ ] Handles percentage changes (e.g., +8%, -10%)
- [ ] Handles absolute unit changes (e.g., +100 units, -50 units)
- [ ] Delegates to appropriate helper methods
- [ ] Includes comprehensive Javadoc

---

### Task 1.4: Add handleCap method
**Location:** `EquipmentChangeUtil.java`

**Method signature:**
```java
/**
 * Handle capping equipment to a maximum value.
 *
 * <p>If current equipment exceeds cap, retires excess. Supports displacement
 * to move retired equipment to another substance.</p>
 *
 * @param capValue The maximum equipment level
 * @param yearMatcher Matcher to determine if cap applies to current year
 * @param displaceTarget Optional substance/stream to displace to (null for no displacement)
 */
public void handleCap(EngineNumber capValue, YearMatcher yearMatcher, String displaceTarget) {
  // Implementation
}
```

**Implementation logic:**
1. Check year range
2. Get current equipment level
3. Convert capValue to units
4. If current > cap:
   - Calculate excess: `current - cap`
   - Set sales to 0 units
   - Call `retireEquipment()` for excess
   - If displaceTarget != null: handle displacement (complex - see SingleThreadEngine:933-1002)
5. Else: no action (already below cap)

**Acceptance criteria:**
- [ ] Enforces maximum equipment level
- [ ] Retires excess equipment
- [ ] Preserves displacement functionality
- [ ] Works with existing `cap equipment to 0 units displacing "SubX"` examples
- [ ] Includes comprehensive Javadoc

---

### Task 1.5: Add handleFloor method
**Location:** `EquipmentChangeUtil.java`

**Method signature:**
```java
/**
 * Handle flooring equipment to a minimum value.
 *
 * <p>If current equipment is below floor, increases sales to meet minimum.
 * Supports displacement to offset the increase.</p>
 *
 * @param floorValue The minimum equipment level
 * @param yearMatcher Matcher to determine if floor applies to current year
 * @param displaceTarget Optional substance/stream to displace from (null for no displacement)
 */
public void handleFloor(EngineNumber floorValue, YearMatcher yearMatcher, String displaceTarget) {
  // Implementation
}
```

**Implementation logic:**
1. Check year range
2. Get current equipment level
3. Convert floorValue to units
4. If current < floor:
   - Calculate deficit: `floor - current`
   - Call `convertToSalesIncrease()` for deficit
   - If displaceTarget != null: handle displacement
5. Else: no action (already above floor)

**Acceptance criteria:**
- [ ] Enforces minimum equipment level
- [ ] Increases sales to meet floor
- [ ] Preserves displacement functionality
- [ ] Includes comprehensive Javadoc

---

### Task 1.6: Add shared helper methods
**Location:** `EquipmentChangeUtil.java`

**Helper method 1: convertToSalesIncrease**
```java
/**
 * Convert equipment increase to sales operation.
 *
 * <p>Sets sales to the delta in units, which automatically handles recharge
 * through existing sales distribution logic.</p>
 *
 * @param deltaUnits The number of units to add
 * @param yearMatcher Optional year matcher
 */
private void convertToSalesIncrease(EngineNumber deltaUnits, Optional<YearMatcher> yearMatcher) {
  // Get scope from engine
  UseKey scope = engine.getScope();

  // Update lastSpecifiedValue for sales to maintain unit-based tracking
  SimulationState simulationState = engine.getStreamKeeper();
  simulationState.setLastSpecifiedValue(scope, "sales", deltaUnits);

  // Use SetExecutor to distribute to domestic/import
  SetExecutor setExecutor = new SetExecutor(engine);
  setExecutor.handleSalesSet(scope, "sales", deltaUnits, yearMatcher);
}
```

**Helper method 2: retireEquipment**
```java
/**
 * Retire equipment from priorEquipment.
 *
 * <p>Sets sales to 0 units and retires the specified amount from priorEquipment.
 * If insufficient priorEquipment, retires only what's available.</p>
 *
 * @param unitsToRetire The number of units to retire
 * @param yearMatcher Optional year matcher
 */
private void retireEquipment(EngineNumber unitsToRetire, Optional<YearMatcher> yearMatcher) {
  UseKey scope = engine.getScope();
  UnitConverter unitConverter = EngineSupportUtils.createUnitConverterWithTotal(engine, "equipment");

  // Set sales to 0 units (maintains unit-based tracking)
  EngineNumber zeroSales = new EngineNumber(BigDecimal.ZERO, "units");
  SimulationState simulationState = engine.getStreamKeeper();
  simulationState.setLastSpecifiedValue(scope, "sales", zeroSales);

  SetExecutor setExecutor = new SetExecutor(engine);
  setExecutor.handleSalesSet(scope, "sales", zeroSales, yearMatcher);

  // Calculate retirement from priorEquipment
  EngineNumber priorEquipmentRaw = engine.getStream("priorEquipment");
  EngineNumber priorEquipment = unitConverter.convert(priorEquipmentRaw, "units");
  BigDecimal actualRetirement = priorEquipment.getValue().min(unitsToRetire.getValue());

  // Convert to percentage of priorEquipment for retire command
  BigDecimal retirementPercentage;
  if (priorEquipment.getValue().compareTo(BigDecimal.ZERO) == 0) {
    retirementPercentage = BigDecimal.ZERO;
  } else {
    retirementPercentage = actualRetirement
      .divide(priorEquipment.getValue(), java.math.MathContext.DECIMAL128)
      .multiply(BigDecimal.valueOf(100));
  }

  // Execute retirement
  EngineNumber retirementRate = new EngineNumber(retirementPercentage, "%");
  simulationState.setRetirementRate(scope, retirementRate);

  // Trigger retirement recalc
  RecalcOperation operation = new RecalcOperationBuilder()
    .setRecalcKit(engine.createRecalcKit()) // Note: need to expose this or create locally
    .recalcRetire()
    .thenPropagateToSales()
    .build();
  operation.execute(engine);
}
```

**Helper method 3: calculatePercentageChange**
```java
/**
 * Calculate absolute change from percentage.
 *
 * @param currentValue The current value
 * @param percentChange The percentage change (e.g., 8 for +8%)
 * @return The absolute change in same units as currentValue
 */
private BigDecimal calculatePercentageChange(EngineNumber currentValue, EngineNumber percentChange) {
  return currentValue.getValue()
    .multiply(percentChange.getValue())
    .divide(BigDecimal.valueOf(100), java.math.MathContext.DECIMAL128);
}
```

**Acceptance criteria:**
- [ ] All three helper methods implemented
- [ ] Proper handling of edge cases (zero priorEquipment, etc.)
- [ ] Includes Javadoc for each method
- [ ] Uses existing utilities (SetExecutor, UnitConverter, etc.)

**Note:** May need to expose `createRecalcKit()` from Engine interface or create RecalcKit directly in EquipmentChangeUtil.

---

## Phase 2: Integrate EquipmentChangeUtil into SingleThreadEngine

### Task 2.1: Add EquipmentChangeUtil instance
**File:** `/home/ubuntu/kigali-sim/engine/src/main/java/org/kigalisim/engine/SingleThreadEngine.java`

**Changes:**

1. **Add field (after line 81):**
```java
private final EquipmentChangeUtil equipmentChangeUtil;
```

2. **Initialize in constructor (after line 104):**
```java
this.equipmentChangeUtil = new EquipmentChangeUtil(this);
```

**Acceptance criteria:**
- [ ] Field added with correct type
- [ ] Initialized in constructor
- [ ] Follows existing pattern of ChangeExecutor initialization

---

### Task 2.2: Update fulfillSetCommand to use util.handleSet
**File:** `SingleThreadEngine.java`
**Location:** Lines 379-401

**Current code:**
```java
@Override
public void fulfillSetCommand(String name, EngineNumber value, Optional<YearMatcher> yearMatcher) {
  // Check year range before proceeding
  if (!getIsInRange(yearMatcher.orElse(null))) {
    return;
  }

  // Delegate sales streams to SetExecutor for proper component distribution
  if ("sales".equals(name)) {
    SetExecutor setExecutor = new SetExecutor(this);
    setExecutor.handleSalesSet(scope, name, value, yearMatcher);
    return;
  }

  // For non-sales streams, use executeStreamUpdate with builder
  StreamUpdate update = new StreamUpdateBuilder()
      .setName(name)
      .setValue(value)
      .setYearMatcher(yearMatcher)
      .inferSubtractRecycling()
      .build();
  executeStreamUpdate(update);
}
```

**New code (add before sales check):**
```java
@Override
public void fulfillSetCommand(String name, EngineNumber value, Optional<YearMatcher> yearMatcher) {
  // Check year range before proceeding
  if (!getIsInRange(yearMatcher.orElse(null))) {
    return;
  }

  // NEW: Handle equipment stream with special logic
  if ("equipment".equals(name)) {
    equipmentChangeUtil.handleSet(value, yearMatcher);
    return;
  }

  // Delegate sales streams to SetExecutor for proper component distribution
  if ("sales".equals(name)) {
    SetExecutor setExecutor = new SetExecutor(this);
    setExecutor.handleSalesSet(scope, name, value, yearMatcher);
    return;
  }

  // For non-sales streams, use executeStreamUpdate with builder
  StreamUpdate update = new StreamUpdateBuilder()
      .setName(name)
      .setValue(value)
      .setYearMatcher(yearMatcher)
      .inferSubtractRecycling()
      .build();
  executeStreamUpdate(update);
}
```

**Acceptance criteria:**
- [ ] Equipment check added before sales check
- [ ] Delegates to `equipmentChangeUtil.handleSet()`
- [ ] Preserves existing behavior for other streams

---

### Task 2.3: Update changeStream to use util.handleChange
**File:** `SingleThreadEngine.java`
**Location:** Lines 772-781

**Current code:**
```java
@Override
public void changeStream(String stream, EngineNumber amount, YearMatcher yearMatcher) {
  changeStream(stream, amount, yearMatcher, null);
}

@Override
public void changeStream(String stream, EngineNumber amount, YearMatcher yearMatcher,
    UseKey useKey) {
  UseKey useKeyEffective = useKey == null ? scope : useKey;
  changeExecutor.executeChange(stream, amount, yearMatcher, useKeyEffective);
}
```

**New code:**
```java
@Override
public void changeStream(String stream, EngineNumber amount, YearMatcher yearMatcher) {
  changeStream(stream, amount, yearMatcher, null);
}

@Override
public void changeStream(String stream, EngineNumber amount, YearMatcher yearMatcher,
    UseKey useKey) {
  // NEW: Handle equipment stream with special logic
  if ("equipment".equals(stream)) {
    equipmentChangeUtil.handleChange(amount, yearMatcher);
    return;
  }

  UseKey useKeyEffective = useKey == null ? scope : useKey;
  changeExecutor.executeChange(stream, amount, yearMatcher, useKeyEffective);
}
```

**Acceptance criteria:**
- [ ] Equipment check added at start of method
- [ ] Delegates to `equipmentChangeUtil.handleChange()`
- [ ] Preserves existing behavior for other streams

---

### Task 2.4: Update cap to use util.handleCap
**File:** `SingleThreadEngine.java`
**Location:** Lines 784-795

**Current code:**
```java
@Override
public void cap(String stream, EngineNumber amount, YearMatcher yearMatcher,
    String displaceTarget) {
  if (!getIsInRange(yearMatcher)) {
    return;
  }

  if ("%".equals(amount.getUnits())) {
    capWithPercent(stream, amount, displaceTarget);
  } else {
    capWithValue(stream, amount, displaceTarget);
  }
}
```

**New code:**
```java
@Override
public void cap(String stream, EngineNumber amount, YearMatcher yearMatcher,
    String displaceTarget) {
  if (!getIsInRange(yearMatcher)) {
    return;
  }

  // NEW: Handle equipment stream with special logic
  if ("equipment".equals(stream)) {
    equipmentChangeUtil.handleCap(amount, yearMatcher, displaceTarget);
    return;
  }

  if ("%".equals(amount.getUnits())) {
    capWithPercent(stream, amount, displaceTarget);
  } else {
    capWithValue(stream, amount, displaceTarget);
  }
}
```

**Acceptance criteria:**
- [ ] Equipment check added after year range check
- [ ] Delegates to `equipmentChangeUtil.handleCap()`
- [ ] Passes displaceTarget parameter
- [ ] Preserves existing behavior for other streams

---

### Task 2.5: Update floor to use util.handleFloor
**File:** `SingleThreadEngine.java`
**Location:** Lines 798-809

**Current code:**
```java
@Override
public void floor(String stream, EngineNumber amount, YearMatcher yearMatcher,
    String displaceTarget) {
  if (!getIsInRange(yearMatcher)) {
    return;
  }

  if ("%".equals(amount.getUnits())) {
    floorWithPercent(stream, amount, displaceTarget);
  } else {
    floorWithValue(stream, amount, displaceTarget);
  }
}
```

**New code:**
```java
@Override
public void floor(String stream, EngineNumber amount, YearMatcher yearMatcher,
    String displaceTarget) {
  if (!getIsInRange(yearMatcher)) {
    return;
  }

  // NEW: Handle equipment stream with special logic
  if ("equipment".equals(stream)) {
    equipmentChangeUtil.handleFloor(amount, yearMatcher, displaceTarget);
    return;
  }

  if ("%".equals(amount.getUnits())) {
    floorWithPercent(stream, amount, displaceTarget);
  } else {
    floorWithValue(stream, amount, displaceTarget);
  }
}
```

**Acceptance criteria:**
- [ ] Equipment check added after year range check
- [ ] Delegates to `equipmentChangeUtil.handleFloor()`
- [ ] Passes displaceTarget parameter
- [ ] Preserves existing behavior for other streams

---

## Phase 3: Unit Tests for EquipmentChangeUtil

### Task 3.1: Create EquipmentChangeUtilTest.java
**File:** `/home/ubuntu/kigali-sim/engine/src/test/java/org/kigalisim/engine/support/EquipmentChangeUtilTest.java`

**Test class structure:**
```java
package org.kigalisim.engine.support;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kigalisim.engine.Engine;
import org.kigalisim.engine.number.EngineNumber;
import org.kigalisim.engine.state.YearMatcher;

/**
 * Unit tests for EquipmentChangeUtil.
 */
public class EquipmentChangeUtilTest {
  private Engine mockEngine;
  private EquipmentChangeUtil util;

  @BeforeEach
  public void setUp() {
    mockEngine = mock(Engine.class);
    util = new EquipmentChangeUtil(mockEngine);
  }

  // Tests to be added in tasks 3.2-3.7
}
```

**Acceptance criteria:**
- [ ] File created in correct package
- [ ] Uses Mockito for mocking Engine
- [ ] Includes setUp method
- [ ] Follows testing conventions

---

### Task 3.2: Test handleSet with equipment increase
**Location:** `EquipmentChangeUtilTest.java`

**Test method:**
```java
@Test
public void testHandleSetWithIncrease() {
  // Setup: current equipment = 1000 units, target = 1200 units
  EngineNumber currentEquipment = new EngineNumber(new BigDecimal("1000"), "units");
  EngineNumber targetEquipment = new EngineNumber(new BigDecimal("1200"), "units");

  when(mockEngine.getStream("equipment")).thenReturn(currentEquipment);
  // Mock other required methods...

  // Execute
  util.handleSet(targetEquipment, Optional.empty());

  // Verify: should set sales to 200 units (delta)
  // Verify SetExecutor was called with correct parameters
  // Verify lastSpecifiedValue was updated
}
```

**Acceptance criteria:**
- [ ] Test verifies sales is set to delta (200 units)
- [ ] Test verifies SetExecutor is called
- [ ] Test verifies lastSpecifiedValue is updated

---

### Task 3.3: Test handleSet with equipment decrease
**Location:** `EquipmentChangeUtilTest.java`

**Test method:**
```java
@Test
public void testHandleSetWithDecrease() {
  // Setup: current equipment = 1200 units, target = 100 units
  EngineNumber currentEquipment = new EngineNumber(new BigDecimal("1200"), "units");
  EngineNumber targetEquipment = new EngineNumber(new BigDecimal("100"), "units");
  EngineNumber priorEquipment = new EngineNumber(new BigDecimal("1140"), "units");

  when(mockEngine.getStream("equipment")).thenReturn(currentEquipment);
  when(mockEngine.getStream("priorEquipment")).thenReturn(priorEquipment);
  // Mock other required methods...

  // Execute
  util.handleSet(targetEquipment, Optional.empty());

  // Verify: should set sales to 0 units
  // Verify retirement was triggered
  // Verify retirement percentage calculated correctly
}
```

**Additional test:**
```java
@Test
public void testHandleSetWithDecreaseInsufficientPriorEquipment() {
  // Test case where priorEquipment < unitsToRetire
  // Should retire only what's available
}
```

**Acceptance criteria:**
- [ ] Test verifies sales is set to 0 units
- [ ] Test verifies retirement is triggered
- [ ] Test handles insufficient priorEquipment case

---

### Task 3.4: Test handleChange with percentage changes
**Location:** `EquipmentChangeUtilTest.java`

**Test methods:**
```java
@Test
public void testHandleChangeWithPositivePercentage() {
  // Setup: current = 1000 units, change = +8%
  // Expected: increase by 80 units
}

@Test
public void testHandleChangeWithNegativePercentage() {
  // Setup: current = 1000 units, change = -10%
  // Expected: decrease by 100 units (retire)
}
```

**Acceptance criteria:**
- [ ] Test verifies percentage calculation is correct
- [ ] Test verifies positive changes convert to sales increase
- [ ] Test verifies negative changes trigger retirement

---

### Task 3.5: Test handleChange with absolute unit changes
**Location:** `EquipmentChangeUtilTest.java`

**Test methods:**
```java
@Test
public void testHandleChangeWithPositiveUnits() {
  // Setup: current = 1000 units, change = +100 units
}

@Test
public void testHandleChangeWithNegativeUnits() {
  // Setup: current = 1000 units, change = -50 units
}
```

**Acceptance criteria:**
- [ ] Test verifies absolute unit changes are handled correctly
- [ ] Test verifies both increase and decrease scenarios

---

### Task 3.6: Test handleCap with displacement
**Location:** `EquipmentChangeUtilTest.java`

**Test methods:**
```java
@Test
public void testHandleCapBelowCurrent() {
  // Setup: current = 1000 units, cap = 500 units
  // Expected: retire 500 units
}

@Test
public void testHandleCapAboveCurrent() {
  // Setup: current = 1000 units, cap = 1500 units
  // Expected: no action
}

@Test
public void testHandleCapWithDisplacement() {
  // Setup: current = 1000 units, cap = 0 units, displacing "SubstanceX"
  // Expected: retire all, displace to SubstanceX
}
```

**Acceptance criteria:**
- [ ] Test verifies cap enforcement
- [ ] Test verifies displacement is triggered correctly
- [ ] Test verifies no action when already below cap

---

### Task 3.7: Test handleFloor with displacement
**Location:** `EquipmentChangeUtilTest.java`

**Test methods:**
```java
@Test
public void testHandleFloorAboveCurrent() {
  // Setup: current = 500 units, floor = 1000 units
  // Expected: increase sales by 500 units
}

@Test
public void testHandleFloorBelowCurrent() {
  // Setup: current = 1000 units, floor = 500 units
  // Expected: no action
}

@Test
public void testHandleFloorWithDisplacement() {
  // Setup: current = 500 units, floor = 1000 units, displacing "SubstanceX"
  // Expected: increase sales by 500, displace from SubstanceX
}
```

**Acceptance criteria:**
- [ ] Test verifies floor enforcement
- [ ] Test verifies displacement is triggered correctly
- [ ] Test verifies no action when already above floor

---

## Phase 4: Handle priorEquipment Operations

### Task 4.1: Analyze current priorEquipment behavior
**Research tasks:**

1. **Review existing tests:**
   - File: `/home/ubuntu/kigali-sim/engine/src/test/java/org/kigalisim/validate/CapLiveTests.java`
   - Look for tests with `cap priorEquipment to 0 units displacing "X"`

2. **Review example files:**
   - File: `/home/ubuntu/kigali-sim/examples/cap_displace_prior_equipment_test.qta`
   - Lines 75-76, 90-91 show `cap equipment` AND `cap priorEquipment` used together

3. **Understand the relationship:**
   - equipment = priorEquipment + newEquipment
   - Setting priorEquipment directly affects equipment total
   - May need different logic than equipment operations

**Questions to answer:**
- [ ] Does `set priorEquipment` need same treatment as `set equipment`?
- [ ] Do existing `cap priorEquipment` tests pass or fail?
- [ ] Should priorEquipment operations be separate methods or integrated?

**Acceptance criteria:**
- [ ] Document current priorEquipment behavior
- [ ] Identify which operations (set/change/cap/floor) are used on priorEquipment
- [ ] Determine if separate handling is needed

---

### Task 4.2: Add priorEquipment handling to EquipmentChangeUtil
**Location:** `EquipmentChangeUtil.java` (if needed based on Task 4.1)

**Potential approach 1 - Add stream parameter:**
```java
// Modify existing methods to accept stream parameter
public void handleSet(String stream, EngineNumber value, Optional<YearMatcher> yearMatcher) {
  if ("equipment".equals(stream)) {
    // Existing equipment logic
  } else if ("priorEquipment".equals(stream)) {
    // priorEquipment-specific logic
  }
}
```

**Potential approach 2 - Separate methods:**
```java
public void handlePriorEquipmentSet(EngineNumber value, Optional<YearMatcher> yearMatcher) {
  // priorEquipment-specific logic
}

public void handlePriorEquipmentCap(EngineNumber capValue, YearMatcher yearMatcher, String displaceTarget) {
  // priorEquipment-specific logic
}
```

**Acceptance criteria:**
- [ ] Implementation based on findings from Task 4.1
- [ ] Preserves existing priorEquipment behavior
- [ ] Includes Javadoc
- [ ] Updates SingleThreadEngine integration if needed

---

### Task 4.3: Test priorEquipment operations
**Location:** `EquipmentChangeUtilTest.java` or new test file

**Test coverage:**
- [ ] Test `set priorEquipment` if implemented
- [ ] Test `cap priorEquipment to 0 units displacing "X"`
- [ ] Test interaction between equipment and priorEquipment operations
- [ ] Verify existing priorEquipment tests still pass

**Acceptance criteria:**
- [ ] All priorEquipment operations tested
- [ ] Edge cases covered
- [ ] Integration with equipment operations verified

---

## Phase 5: Integration Testing and Validation

### Task 5.1: Verify testPopulationDecreaseRecharge passes
**File:** `/home/ubuntu/kigali-sim/engine/src/test/java/org/kigalisim/validate/BasicLiveTests.java`
**Test:** `testPopulationDecreaseRecharge()` (lines 807-828)

**QTA File:** `/home/ubuntu/kigali-sim/examples/population_decrease_recharge_issue.qta`

**Expected behavior:**
- Year 1: set equipment to 1000 units → sales = 1000 units
- Year 2: set equipment to 1200 units → sales = 200 units (delta)
- Year 3: set equipment to 100 units → sales = 0 units, retire ~1100 units
  - Recharge needs: 5% of remaining equipment after retirement
  - **Import consumption should be > 0 kg in year 3**

**Command to run:**
```bash
cd /home/ubuntu/kigali-sim/engine
./gradlew test --tests "BasicLiveTests.testPopulationDecreaseRecharge"
```

**Acceptance criteria:**
- [ ] Test passes (import consumption > 0 in year 3)
- [ ] No exceptions thrown
- [ ] Behavior matches expected calculation

---

### Task 5.2: Run all BasicLiveTests
**File:** `/home/ubuntu/kigali-sim/engine/src/test/java/org/kigalisim/validate/BasicLiveTests.java`

**Command:**
```bash
./gradlew test --tests "BasicLiveTests"
```

**Current test count:** 27 tests (including new testPopulationDecreaseRecharge)

**Tests to watch:**
- `testSetPriorEquipmentAffectsCurrentEquipment` - priorEquipment behavior
- `testBasicCarryOver` - unit-based carry-over
- `testSetImportToZeroWithRecharge` - recharge with zero sales
- `testSetUnitsWithRecharge` - recharge distribution

**Acceptance criteria:**
- [ ] All 27 tests pass
- [ ] No new failures introduced
- [ ] Any failures analyzed and documented

---

### Task 5.3: Run all CapLiveTests
**File:** `/home/ubuntu/kigali-sim/engine/src/test/java/org/kigalisim/validate/CapLiveTests.java`

**Command:**
```bash
./gradlew test --tests "CapLiveTests"
```

**Critical tests for equipment handling:**
- `testCapDisplaceUnits` - cap with units and displacement
- `testCapDisplaceWithRechargeUnits` - cap with recharge needs
- `testCapDisplacePriorEquipment` - priorEquipment capping
- `testCapDisplacePriorEquipmentPreservation` - priorEquipment preservation
- `testCapPriorEquipmentOnly` - priorEquipment-only capping

**Acceptance criteria:**
- [ ] All CapLiveTests pass
- [ ] Displacement logic preserved
- [ ] priorEquipment behavior correct
- [ ] Any failures analyzed and documented

---

### Task 5.4: Run full test suite
**Command:**
```bash
cd /home/ubuntu/kigali-sim/engine
./gradlew test
```

**Current test count:** 879 tests

**Test categories to monitor:**
- BasicLiveTests (27 tests)
- CapLiveTests (displacement logic)
- RechargeLiveTests (recharge calculations)
- RecycleRecoverLiveTests (recycling logic)
- ReplaceLiveTests (replacement operations)
- Unit tests for engine components

**Acceptance criteria:**
- [ ] All 879+ tests pass
- [ ] No regressions in existing functionality
- [ ] Build succeeds without errors

---

### Task 5.5: Review and update any failing tests
**Process:**

1. **For each failing test:**
   - [ ] Analyze the failure reason
   - [ ] Determine if it's due to:
     - Bug in new implementation → fix implementation
     - Incorrect test expectations → update test
     - Previously hidden bug now exposed → update test to expect correct behavior

2. **Document changes:**
   - [ ] Create list of tests that were updated
   - [ ] Explain why expectations changed
   - [ ] Verify new expectations are correct per QubecTalk semantics

3. **Example QTA files:**
   - Files that use `change equipment by +8%`:
     - `cap_displace_bug_kg.qta`
     - `cap_displace_bug_units.qta`
     - `cap_displace_with_recharge_units.qta`
     - `cold_start_equipment.qta`
     - `ordering_sensitive_emissions.qta`
     - `test_change_monte_carlo.qta`
   - Files that use `cap equipment to 0 units`:
     - `cap_displace_prior_equipment_test.qta`
     - `cap_displace_prior_equipment.qta`
     - `cap_prior_equipment_only_test.qta`

**Acceptance criteria:**
- [ ] All failing tests analyzed
- [ ] Implementation bugs fixed OR test expectations corrected
- [ ] Documentation of any test changes
- [ ] Final test run passes 100%

---

## Success Criteria

### Phase 1 Complete:
- [x] EquipmentChangeUtil.java created
- [x] All handler methods implemented (handleSet, handleChange, handleCap, handleFloor)
- [x] All helper methods implemented
- [x] Code follows Google Java Style Guide
- [x] Comprehensive Javadoc for all methods

### Phase 2 Complete:
- [x] EquipmentChangeUtil integrated into SingleThreadEngine
- [x] All four operations (set/change/cap/floor) route to util
- [x] No breaking changes to existing functionality

### Phase 3 Complete:
- [x] EquipmentChangeUtilTest.java created
- [x] Unit tests for all methods (handleSet, handleChange, handleCap, handleFloor)
- [x] Edge cases covered (zero values, insufficient priorEquipment, etc.)
- [x] All unit tests pass

### Phase 4 Complete:
- [x] priorEquipment behavior analyzed and documented
- [x] priorEquipment handling implemented if needed
- [x] priorEquipment tests pass

### Phase 5 Complete:
- [x] testPopulationDecreaseRecharge passes (import consumption > 0 in year 3)
- [x] All BasicLiveTests pass (27 tests)
- [x] All CapLiveTests pass
- [x] Full test suite passes (879+ tests)
- [x] Any test expectation changes documented

## Final Deliverables

1. **New Files:**
   - `/home/ubuntu/kigali-sim/engine/src/main/java/org/kigalisim/engine/support/EquipmentChangeUtil.java`
   - `/home/ubuntu/kigali-sim/engine/src/test/java/org/kigalisim/engine/support/EquipmentChangeUtilTest.java`

2. **Modified Files:**
   - `/home/ubuntu/kigali-sim/engine/src/main/java/org/kigalisim/engine/SingleThreadEngine.java`
   - `/home/ubuntu/kigali-sim/engine/src/test/java/org/kigalisim/validate/BasicLiveTests.java` (test already added)

3. **Test Files:**
   - `/home/ubuntu/kigali-sim/examples/population_decrease_recharge_issue.qta` (already created)

4. **Documentation:**
   - Updated Javadoc in EquipmentChangeUtil
   - List of any test expectation changes with rationale

---

## Notes and Considerations

### Design Decisions:
1. **Why EquipmentChangeUtil?**
   - Follows pattern of SetExecutor and ChangeExecutor
   - Provides isolated unit testing
   - Centralizes complex equipment logic
   - Easier to maintain and debug

2. **Why convert to sales operations?**
   - Sales mechanism already handles recharge automatically
   - Leverages existing distribution logic (domestic/import)
   - Maintains unit-based tracking through lastSpecifiedValue
   - Ensures consistency with other QubecTalk operations

3. **Why retire from priorEquipment?**
   - priorEquipment is the pool available for retirement
   - Matches behavior of retire command
   - Properly handles recharge needs before retirement

### Potential Issues:
1. **RecalcKit access:**
   - `retireEquipment()` helper needs RecalcKit
   - May need to expose `createRecalcKit()` in Engine interface
   - Or create RecalcKit directly in EquipmentChangeUtil

2. **Displacement complexity:**
   - Cap/floor with displacement is complex (see SingleThreadEngine:933-1002)
   - May need to extract displacement logic to separate helper
   - Need to preserve GWP calculation context for destination substance

3. **priorEquipment operations:**
   - Need to analyze if behavior should differ from equipment
   - Existing tests use both `cap equipment` AND `cap priorEquipment` together
   - May reveal edge cases or unexpected interactions

### Testing Strategy:
1. **Unit tests first:** Test EquipmentChangeUtil in isolation
2. **Integration test:** Verify testPopulationDecreaseRecharge
3. **Regression tests:** Ensure no existing tests break
4. **Review failures:** Some tests may have incorrect expectations

### References:
- Grammar: `/home/ubuntu/kigali-sim/engine/src/main/antlr/org/kigalisim/lang/QubecTalk.g4` (lines 335, 379-427)
- Similar utilities: `SetExecutor.java`, `ChangeExecutor.java`
- Recalc strategies: `PopulationChangeRecalcStrategy.java`, `RetireRecalcStrategy.java`, `SalesRecalcStrategy.java`
- Existing tests: `BasicLiveTests.java`, `CapLiveTests.java`
