# End of Life Recycling

Ability to specify time of recycling. Adding to current support for recycling at time of servicing, adding support for recycling at time of end of life (scrap / retirement).

## Background

We currently have a recycling command (see `RecycleCommand` in `ui_translator.js` and `recycle` in `Engine` / `SingleThreadEngine`). This currently applies to recycling at the point of servicing. While this is currently understood to be more common, we should add support for indicating if it should be at servicing or end of life.

## Objective

This requires a multi-step approach.

### Update grammar

Add to the grammar (see both `QubecTalk.g4` in `editor/language` and in `engine/src/main/antler/org/Kigali Sim/lang`). There should be optional choice of recycling point like as follows:

```
recover 10 % with 20 % reuse at eol displacing import
```

This requires an `AT_` and new variations on `recycleStatement`.

### Update engine

We should add this additional option to `RecoverOperation` which should pass an enum value (defined in `RecoverOperation` called `RecoverStage` for either eol indicating end of life or recharge indicating at time of recharge (service). This should influence how much volume is used for recycling calculations but may require doing this calculation twice: once for EOL and once at time of servicing. Please note that this requires updates to the Java visitor.

### Update editor

Adding support for this will be required in `RecycleCommand` though a string instead of an enum is acceptable. Additionally, updates are required in `#recycle-command-template`. Please note that this will involve updates to the JS visitor.

## Implementation

This may require disambiguating recovery and yield streams, splitting them into two with one for eol and one for recharge. To support this operation, private methods should be considered to reduce repetition.

## Detailed Implementation Plan

### Phase 1: Grammar Updates

#### 1.1 Update Lexer Tokens
**Files to modify:**
- `editor/language/QubecTalk.g4:1-430`
- `engine/src/main/antlr/org/kigalisim/lang/QubecTalk.g4:1-435`

**Changes:**
- Add `AT_: 'at'` token around line 200 in the Commands section
- Add `EOL_: 'eol'` token around line 200 in the Commands section

#### 1.2 Update Parser Rules
**Files to modify:**
- Both grammar files mentioned above

**Changes:**
Update `recycleStatement` rule (currently lines 387-391 in editor, 391-395 in engine) to include new variations:
```antlr
recycleStatement: RECOVER_ volume=unitValue WITH_ yieldVal=unitValue REUSE_  # recoverAllYears
  | RECOVER_ volume=unitValue WITH_ yieldVal=unitValue REUSE_ duration=during  # recoverDuration
  | RECOVER_ volume=unitValue WITH_ yieldVal=unitValue REUSE_ DISPLACING_ (string | stream)  # recoverDisplacementAllYears
  | RECOVER_ volume=unitValue WITH_ yieldVal=unitValue REUSE_ DISPLACING_ (string | stream) duration=during  # recoverDisplacementDuration
  | RECOVER_ volume=unitValue WITH_ yieldVal=unitValue REUSE_ AT_ stage=(RECHARGE_ | EOL_)  # recoverStageAllYears
  | RECOVER_ volume=unitValue WITH_ yieldVal=unitValue REUSE_ AT_ stage=(RECHARGE_ | EOL_) duration=during  # recoverStageDuration
  | RECOVER_ volume=unitValue WITH_ yieldVal=unitValue REUSE_ AT_ stage=(RECHARGE_ | EOL_) DISPLACING_ (string | stream)  # recoverStageDisplacementAllYears
  | RECOVER_ volume=unitValue WITH_ yieldVal=unitValue REUSE_ AT_ stage=(RECHARGE_ | EOL_) DISPLACING_ (string | stream) duration=during  # recoverStageDisplacementDuration
  ;
```

### Phase 2: Engine Implementation

#### 2.1 Add RecoverStage Enum
**File to modify:**
- `engine/src/main/java/org/kigalisim/lang/operation/RecoverOperation.java:27-115`

**Changes:**
- Add enum `RecoverStage` with values `RECHARGE`, `EOL`
- Add private field `Optional<RecoverStage> recoverStage`
- Add new constructors accepting `RecoverStage` parameter
- Modify `execute()` method to pass stage information to engine

#### 2.2 Update Engine Interface
**Files to modify:**
- `engine/src/main/java/org/kigalisim/engine/Engine.java` (interface)
- `engine/src/main/java/org/kigalisim/engine/SingleThreadEngine.java:107-113`

**Changes:**
- Add new overloaded `recycle()` methods accepting `RecoverStage` parameter
- Implement stage-specific recycling logic in `SingleThreadEngine`

#### 2.3 Update Java Visitor
**File to modify:**
- `engine/src/main/java/org/kigalisim/lang/QubecTalkEngineVisitor.java:70+`

**Changes:**
- Add visitor methods for new grammar rules:
  - `visitRecoverStageAllYears()`
  - `visitRecoverStageDuration()`
  - `visitRecoverStageDisplacementAllYears()`
  - `visitRecoverStageDisplacementDuration()`
- Extract stage from context and create appropriate `RecoverOperation` instances

### Phase 3: Editor Grammar Update and Build Verification

#### 3.1 Update Editor Grammar
**File to modify:**
- `editor/language/QubecTalk.g4:1-430`

**Changes:**
- Add `AT_: 'at'` token around line 200 in the Commands section
- Add `EOL_: 'eol'` token around line 200 in the Commands section
- Update `recycleStatement` rule (currently lines 387-391) to include new variations as shown in Phase 1

#### 3.2 Run Build Scripts
**Commands to execute:**
```bash
cd /home/ubuntu/montreal-protocol/editor
./make.sh
support/update_wasm.sh
```

**Verification:**
- Confirm both commands complete successfully without errors
- Verify that grammar changes are properly processed
- Check that WASM build incorporates the new grammar rules
- **CRITICAL**: Do not proceed to Phase 4 until both commands work correctly

### Phase 4: Editor Implementation

#### 4.1 Update RecycleCommand
**File to modify:**
- `editor/js/ui_translator.js:34+` (RecycleCommand class)

**Changes:**
- Add `_stage` field to `RecycleCommand` constructor
- Add `getStage()` method
- Update constructor signature to accept stage parameter

#### 4.2 Update JavaScript Visitor  
**File to modify:**
- `editor/js/ui_translator.js` (visitor methods section)

**Changes:**
- Add visitor methods for new grammar rules:
  - `visitRecoverStageAllYears()`
  - `visitRecoverStageDuration()`
  - `visitRecoverStageDisplacementAllYears()`
  - `visitRecoverStageDisplacementDuration()`
- Extract stage information from parse context

#### 4.3 Update Command Template
**File to check:**
- Search for `#recycle-command-template` in editor files
- Update template to include stage selection options

### Phase 5: Syntax Highlighting Updates

#### 5.1 Update Ace Mode
**File to modify:**
- `editor/js/ace-mode-qubectalk.js:16-18`

**Changes:**
- Add `at` and `eol` to `commandKeywords` string

#### 5.2 Update Prism Configuration
**Files to check and potentially modify:**
- Look for prism configuration files for syntax highlighting
- Add new keywords to relevant highlighting rules

### Phase 6: Testing

#### 6.1 Add Unit Tests
**Files to create/modify:**
- `engine/src/test/java/org/kigalisim/lang/operation/RecoverOperationTest.java`
- New test methods in `engine/src/test/java/org/kigalisim/KigaliSimFacadeTest.java`

**Test cases:**
- Test parsing of new `at eol` and `at recharge` syntax
- Test engine behavior with different stages
- Test displacement with stages
- Test duration with stages
- Regression tests for existing functionality

#### 6.2 Integration Tests
- Test complete end-to-end workflow from grammar to engine execution
- Verify that EOL recycling calculations occur at appropriate times
- Verify that recharge recycling maintains existing behavior

### Phase 7: Validation Commands

Run these commands to ensure implementation correctness:

```bash
# In engine directory
cd /home/ubuntu/montreal-protocol/engine
./gradlew test checkstyleMain checkstyleTest
./gradlew clean build

# In editor directory (if engine changes made)
cd /home/ubuntu/montreal-protocol/editor
support/update_wasm.sh
npx grunt
```

### Implementation Priority

1. **High Priority**: Grammar updates and core engine functionality
2. **High Priority**: Java visitor implementation  
3. **High Priority**: JavaScript visitor and RecycleCommand updates
4. **Medium Priority**: Syntax highlighting
5. **High Priority**: Testing and validation

### Key Considerations

- Maintain backward compatibility with existing recycling syntax
- Ensure proper error handling for invalid stage specifications
- Consider performance implications of dual recycling calculations
- Document the new syntax and behavior changes
- Follow existing code patterns and naming conventions

## Notes

Need to determine if there are location where it is assumed that this came from recharge and not EOL. Please also update the QubecTalkHighlightRules and the prism configuration for the guide.

Please review `background.md` in `tasks` to learn more about how to interact with the repository. Please ensure validation commands pass at the end of the operation.

---

## Appendix: Stage-Specific Implementation Plan

### Current Status Analysis

The EOL recycling feature has been **substantially implemented** but contains critical TODOs that prevent proper stage differentiation:

**✅ Completed Components:**
- Grammar support for `at eol` and `at recharge` syntax (QubecTalk.g4)
- RecoverStage enum with EOL and RECHARGE values (RecoverOperation.java)
- Engine interface methods accepting RecoverStage parameters (Engine.java)
- Visitor implementations for parsing stage-specific syntax
- Basic testing infrastructure with test files (recycle_eol.qta, recycle_recharge.qta, recycle_eol_units.qta)

**❌ Missing Implementation:**
- Stage-specific logic in SingleThreadEngine.java (lines 694, 715)
- Proper timing differentiation between EOL and RECHARGE recycling
- Integration with existing equipment lifecycle operations

### Core Problem

The current implementation in `SingleThreadEngine.java` has placeholder TODOs:

```java
// Line 694 & 715:
// For now, delegate to existing method regardless of stage
// TODO: Implement stage-specific logic for EOL vs RECHARGE timing
```

### Proposed Implementation Strategy

#### 1. **Timing Differentiation Approach**

**RECHARGE Recycling (existing behavior):**
- Occurs during equipment servicing operations
- Integrates with `RechargeEmissionsRecalcStrategy`
- Applied to `priorEquipment` population
- Recovery volume affects recharge substance volume calculations

**EOL Recycling (new behavior):**
- Occurs during equipment retirement operations
- Integrates with `EolEmissionsRecalcStrategy` 
- Applied to retiring equipment population
- Recovery affects end-of-life emission calculations

#### 2. **Implementation Plan**

**Phase 1: Create Stage-Specific RecalcStrategies**

Create new recalc strategies to handle stage-specific recycling:

**File: `EolRecyclingRecalcStrategy.java`**
```java
public class EolRecyclingRecalcStrategy implements RecalcStrategy {
  // Applies recovery rates specifically during EOL operations
  // Integrates with equipment retirement and EOL emissions
  // Calculates recycled material from retiring equipment
}
```

**File: `RechargeRecyclingRecalcStrategy.java`**  
```java
public class RechargeRecyclingRecalcStrategy implements RecalcStrategy {
  // Applies recovery rates specifically during recharge operations
  // Integrates with equipment servicing and recharge emissions
  // Calculates recycled material from serviced equipment
}
```

**Phase 2: Modify SingleThreadEngine Stage-Specific Methods**

Update `SingleThreadEngine.java` methods (lines 688-735) to use appropriate strategies:

```java
@Override
public void recycle(EngineNumber recoveryWithUnits, EngineNumber yieldWithUnits,
    YearMatcher yearMatcher, RecoverStage recoverStage) {
  if (!getIsInRange(yearMatcher)) {
    return;
  }

  // Set recovery and yield rates
  streamKeeper.setRecoveryRate(scope, recoveryWithUnits);
  streamKeeper.setYieldRate(scope, yieldWithUnits);

  // Build stage-specific recalc operation
  RecalcOperationBuilder builder = new RecalcOperationBuilder()
      .setRecalcKit(createRecalcKit())
      .recalcSales()
      .thenPropagateToPopulationChange();

  if (recoverStage == RecoverStage.EOL) {
    builder.thenRecalc(new EolRecyclingRecalcStrategy(Optional.of(scope)))
           .thenPropagateToConsumption()
           .thenRecalc(new EolEmissionsRecalcStrategy(Optional.of(scope)));
  } else { // RECHARGE
    builder.thenRecalc(new RechargeRecyclingRecalcStrategy(Optional.of(scope)))
           .thenPropagateToConsumption()
           .thenRecalc(new RechargeEmissionsRecalcStrategy(Optional.of(scope)));
  }

  RecalcOperation operation = builder.build();
  operation.execute(this);
}
```

**Phase 3: Integration Points**

**Equipment Population Access:**
- `EOL`: Access retiring equipment through existing retire operations
- `RECHARGE`: Access serviceable equipment through existing recharge operations

**Volume Calculations:**
- Create `EolRecyclingVolumeCalculator` similar to `RechargeVolumeCalculator`
- Calculate available material based on equipment stage and recovery rates

**Emission Integration:**
- EOL recycling affects `eolEmissions` calculations
- Recharge recycling affects `rechargeEmissions` calculations

#### 3. **Key Technical Considerations**

**Volume Source Differentiation:**
- **EOL**: Recovery from equipment reaching end-of-life (retirement volume)
- **RECHARGE**: Recovery from equipment being serviced (recharge volume)

**Timing Logic:**
- **EOL**: Applied when equipment retires (integrate with `RetireOperation`)
- **RECHARGE**: Applied when equipment is serviced (integrate with `RechargeOperation`)

**Calculation Sequencing:**
- EOL recycling must occur BEFORE EOL emissions calculation
- Recharge recycling must occur BEFORE recharge emissions calculation

#### 4. **Testing Strategy**

**Update Existing Tests:**
- `testRecycleEol()`: Verify EOL-specific timing and volume calculations
- `testRecycleRecharge()`: Verify recharge-specific behavior
- `testRecycleEolUnits()`: Verify units-based EOL recycling

**Add New Tests:**
- Test interaction between EOL recycling and retirement operations
- Test interaction between recharge recycling and servicing operations
- Test proper emission calculation integration
- Test timing differentiation (recycling only occurs at specified stage)

#### 5. **Implementation Files**

**New Files to Create:**
- `engine/src/main/java/org/kigalisim/engine/recalc/EolRecyclingRecalcStrategy.java`
- `engine/src/main/java/org/kigalisim/engine/recalc/RechargeRecyclingRecalcStrategy.java`
- `engine/src/main/java/org/kigalisim/engine/support/EolRecyclingVolumeCalculator.java`

**Existing Files to Modify:**
- `engine/src/main/java/org/kigalisim/engine/SingleThreadEngine.java` (lines 688-735)
- Test files to verify proper stage-specific behavior

#### 6. **Validation Approach**

**Behavioral Verification:**
- EOL recycling only occurs when equipment retires
- Recharge recycling only occurs when equipment is serviced
- Different recovery rates based on equipment lifecycle stage
- Proper integration with emission calculations

**CSV Output Verification:**
- `recycleConsumption` values reflect stage-appropriate calculations
- `eolEmissions` and `rechargeEmissions` properly account for recycling
- Equipment population changes reflect recycling impact

### Implementation Priority

1. **Phase 1**: Create stage-specific RecalcStrategy classes
2. **Phase 2**: Update SingleThreadEngine methods to remove TODOs
3. **Phase 3**: Create volume calculators and integration logic
4. **Phase 4**: Update and expand test suite
5. **Phase 5**: Run comprehensive validation

This plan addresses the core missing functionality while building on the substantial infrastructure already in place.

---

## Updated Implementation Strategy: Template Method with Stage-Specific Streams

### **Comprehensive Analysis Results**

After examining the existing codebase, here are the key findings that inform our implementation:

#### **Current Stream Usage Patterns:**
1. **Recovery/Yield Rate Access:** 
   - `SalesRecalcStrategy.java:73,79` - Main recycling logic during sales recalculation
   - `SingleThreadEngine.java:1255,1256` - General calculations
   - `StreamKeeper.java:935,948` - Equipment retirement calculations

2. **Rate Reset Behavior:** 
   - `StreamParameterization.resetStateAtTimestep()` resets `recoveryRate` to 0% between years
   - Need to preserve stage-specific rates independently

3. **Perfect Integration Points:**
   - `RetireRecalcStrategy.java:85` - EOL recycling should occur here, before EOL emissions
   - `PopulationChangeRecalcStrategy.java:117` - Recharge recycling should occur here, before recharge emissions

### **Final Implementation Plan**

#### **Phase 1: Extend StreamParameterization for Stage-Specific Rates**

**Key Changes to `StreamParameterization.java`:**
- Add separate fields for EOL and recharge recovery/yield rates
- Maintain backward compatibility with existing `recoveryRate`/`yieldRate` fields
- Update `resetStateAtTimestep()` to handle stage-specific rates

```java
// New fields to add:
private EngineNumber eolRecoveryRate;
private EngineNumber eolYieldRate; 
private EngineNumber rechargeRecoveryRate;
private EngineNumber rechargeYieldRate;

// Update resetStateAtTimestep() to reset all stage-specific rates
public void resetStateAtTimestep() {
  recoveryRate = new EngineNumber(BigDecimal.ZERO, "%");
  eolRecoveryRate = new EngineNumber(BigDecimal.ZERO, "%");
  rechargeRecoveryRate = new EngineNumber(BigDecimal.ZERO, "%");
  // Note: yield rates represent efficiency, not program activation
}
```

#### **Phase 2: Extend StreamKeeper for Stage-Specific Management**

**Key Changes to `StreamKeeper.java`:**
- Add overloaded methods: `setRecoveryRate(useKey, value, stage)` and `setYieldRate(useKey, value, stage)`
- Add overloaded getters: `getRecoveryRate(useKey, stage)` and `getYieldRate(useKey, stage)`
- Maintain existing additive recycling logic (like current `setRecoveryRate` implementation)
- Handle weighted averages for yield rates (like current `setYieldRate` implementation)

#### **Phase 3: Template Method Pattern Implementation**

**Create `AbstractRecyclingRecalcStrategy.java`:**
```java
public abstract class AbstractRecyclingRecalcStrategy implements RecalcStrategy {
  protected final Optional<UseKey> scope;
  protected final RecoverStage stage;
  
  // Template method implementation
  @Override
  public final void execute(Engine target, RecalcKit kit) {
    UseKey scopeEffective = scope.orElse(target.getScope());
    StreamKeeper streamKeeper = kit.getStreamKeeper();
    
    // Check if recycling is configured for this stage
    EngineNumber recoveryRate = streamKeeper.getRecoveryRate(scopeEffective, stage);
    if (recoveryRate.getValue().compareTo(BigDecimal.ZERO) == 0) {
      return; // No recycling for this stage
    }
    
    // Template method pattern
    EngineNumber availableVolume = calculateAvailableVolume(target, kit, scopeEffective);
    EngineNumber yieldRate = streamKeeper.getYieldRate(scopeEffective, stage);
    
    applyRecycling(target, kit, scopeEffective, availableVolume, recoveryRate, yieldRate);
  }
  
  // Abstract method for subclasses
  protected abstract EngineNumber calculateAvailableVolume(Engine target, RecalcKit kit, UseKey scope);
  
  // Shared recycling application logic
  protected void applyRecycling(Engine target, RecalcKit kit, UseKey scope, 
                               EngineNumber availableVolume, EngineNumber recoveryRate, EngineNumber yieldRate) {
    // Implement recycling calculation similar to SalesRecalcStrategy.java:73-81
  }
}
```

**Concrete Implementations:**
- `EolRecyclingRecalcStrategy` - Calculate volume from retiring equipment
- `RechargeRecyclingRecalcStrategy` - Use `RechargeVolumeCalculator` for servicing volume

#### **Phase 4: Integration with Existing Recalc Strategies**

**Update `RetireRecalcStrategy.java:85`:**
```java
// Add before existing EolEmissionsRecalcStrategy
EolRecyclingRecalcStrategy eolRecycling = new EolRecyclingRecalcStrategy(Optional.of(scopeEffective));
eolRecycling.execute(target, kit);

EolEmissionsRecalcStrategy eolStrategy = new EolEmissionsRecalcStrategy(Optional.of(scopeEffective));
eolStrategy.execute(target, kit);
```

**Update `PopulationChangeRecalcStrategy.java:117`:**
```java
// Add before existing RechargeEmissionsRecalcStrategy
RechargeRecyclingRecalcStrategy rechargeRecycling = new RechargeRecyclingRecalcStrategy(Optional.of(scopeEffective));
rechargeRecycling.execute(target, kit);

RechargeEmissionsRecalcStrategy rechargeStrategy = new RechargeEmissionsRecalcStrategy(Optional.of(scopeEffective));
rechargeStrategy.execute(target, kit);
```

#### **Phase 5: Update SingleThreadEngine Methods**

**Remove TODOs in `SingleThreadEngine.java` (lines 694, 715):**
```java
@Override
public void recycle(EngineNumber recoveryWithUnits, EngineNumber yieldWithUnits,
    YearMatcher yearMatcher, RecoverStage recoverStage) {
  if (!getIsInRange(yearMatcher)) {
    return;
  }

  // Set stage-specific recovery and yield rates  
  streamKeeper.setRecoveryRate(scope, recoveryWithUnits, recoverStage);
  streamKeeper.setYieldRate(scope, yieldWithUnits, recoverStage);

  // No immediate recalc operation needed - recycling will be applied by 
  // stage-specific strategies during RetireRecalcStrategy or PopulationChangeRecalcStrategy
}
```

### **Key Benefits of This Approach:**

1. **Proper Timing:** Recycling occurs exactly when equipment retires (EOL) or is serviced (recharge)
2. **Backward Compatibility:** Existing methods continue to work
3. **Template Method:** Shared recycling logic with stage-specific volume calculations  
4. **Stream-Based:** Leverages existing stream parameterization architecture
5. **Integration:** Seamlessly integrates with existing recalc strategy pattern

### **Implementation Files:**

**New Files:**
- `AbstractRecyclingRecalcStrategy.java`
- `EolRecyclingRecalcStrategy.java` 
- `RechargeRecyclingRecalcStrategy.java`

**Modified Files:**
- `StreamParameterization.java` - Add stage-specific rate fields
- `StreamKeeper.java` - Add stage-specific rate methods
- `SingleThreadEngine.java` - Remove TODOs, implement stage-specific logic
- `RetireRecalcStrategy.java` - Add EOL recycling integration
- `PopulationChangeRecalcStrategy.java` - Add recharge recycling integration

This approach properly addresses both the timing/triggering question and implements the template method pattern while leveraging the existing stream-based architecture.

---

## Final Implementation Plan Update (Based on Diff Analysis)

### **Current Implementation Status (from diff analysis)**

**✅ Already Completed:**
1. **Grammar Support**: Both engine and editor grammar files updated with `AT_` and `EOL_` tokens
2. **Parser Rules**: New `recoverStage*` rules added to support `at eol` and `at recharge` syntax
3. **RecoverOperation**: Complete stage-specific implementation with `RecoverStage` enum
4. **Engine Interface**: Methods accepting `RecoverStage` parameter added
5. **JavaScript Visitor**: Editor support for stage-specific syntax parsing
6. **Syntax Highlighting**: Ace editor updated with new keywords (`at`, `eol`)
7. **Test Infrastructure**: Three test files and corresponding test methods created:
   - `recycle_eol.qta` + `testRecycleEol()`
   - `recycle_recharge.qta` + `testRecycleRecharge()`  
   - `recycle_eol_units.qta` + `testRecycleEolUnits()`

**❌ Still Missing (TODOs in SingleThreadEngine.java):**
- Lines 694, 715: Stage-specific logic not implemented - methods currently delegate to existing implementation

### **Remaining Implementation Work**

#### **1. Test Logic Updates Required**

**Current Test Behavior:** The existing tests currently pass because:
- `testRecycleEol()`: Uses behavioral assertions (positive recycling, reduced consumption) rather than exact values
- `testRecycleRecharge()`: Tests import displacement but no actual stage differentiation
- `testRecycleEolUnits()`: Verifies units-based recycling works with EOL syntax

**Test Updates Needed After Implementation:**
1. **Timing Verification**: Tests should verify that:
   - EOL recycling only occurs when equipment retires (not during recharge)
   - Recharge recycling only occurs during equipment servicing (not at EOL)
   
2. **Stage Isolation**: Add tests that verify:
   - `recover 30% with 80% reuse at eol` does NOT affect recharge emissions
   - `recover 25% with 90% reuse at recharge` does NOT affect EOL emissions
   
3. **Volume Source Verification**: Tests should verify:
   - EOL recycling uses retiring equipment volume as basis
   - Recharge recycling uses serviceable equipment volume as basis

#### **2. Implementation Tasks**

**Phase 1: StreamParameterization Extension**
```java
// Add to StreamParameterization.java
private EngineNumber eolRecoveryRate;
private EngineNumber eolYieldRate; 
private EngineNumber rechargeRecoveryRate;
private EngineNumber rechargeYieldRate;

public void resetStateAtTimestep() {
  recoveryRate = new EngineNumber(BigDecimal.ZERO, "%");
  eolRecoveryRate = new EngineNumber(BigDecimal.ZERO, "%");
  rechargeRecoveryRate = new EngineNumber(BigDecimal.ZERO, "%");
  // yield rates not reset (represent efficiency)
}
```

**Phase 2: StreamKeeper Stage-Aware Methods**
```java
// Add to StreamKeeper.java
public void setRecoveryRate(UseKey useKey, EngineNumber newValue, RecoverStage stage);
public void setYieldRate(UseKey useKey, EngineNumber newValue, RecoverStage stage);
public EngineNumber getRecoveryRate(UseKey useKey, RecoverStage stage);
public EngineNumber getYieldRate(UseKey useKey, RecoverStage stage);
```

**Phase 3: Template Method Implementation**
- `AbstractRecyclingRecalcStrategy` with shared recycling logic
- `EolRecyclingRecalcStrategy` - calculates available volume from retiring equipment
- `RechargeRecyclingRecalcStrategy` - uses `RechargeVolumeCalculator`

**Phase 4: Integration Points**
- `RetireRecalcStrategy.java:85` - Add EOL recycling before EOL emissions
- `PopulationChangeRecalcStrategy.java:117` - Add recharge recycling before recharge emissions

**Phase 5: SingleThreadEngine TODO Resolution**
```java
// Replace TODOs with:
streamKeeper.setRecoveryRate(scope, recoveryWithUnits, recoverStage);
streamKeeper.setYieldRate(scope, yieldWithUnits, recoverStage);
// Recycling will be applied automatically during lifecycle events
```

#### **3. Test Update Strategy**

**Enhanced Test Scenarios:**
1. **Stage Isolation Tests**: Create tests that use both EOL and recharge recycling in same scenario to verify they operate independently
2. **Timing Tests**: Tests that verify recycling only occurs during appropriate lifecycle events
3. **Volume Source Tests**: Tests that verify different volume calculations for EOL vs recharge
4. **Integration Tests**: Verify proper interaction with emissions calculations

**Test File Updates:**
- `testRecycleEol()`: Add verification that recharge emissions are unaffected
- `testRecycleRecharge()`: Add verification that EOL emissions are unaffected  
- `testRecycleEolUnits()`: Add timing verification (only year 2, not year 3)

**New Test Requirements:**
- Test mixing EOL and recharge recycling in same substance
- Test proper emission integration timing
- Test equipment volume calculation differences

### **Implementation Priority**

1. **High**: StreamParameterization and StreamKeeper stage-specific methods
2. **High**: Template method pattern implementation
3. **High**: Integration with existing recalc strategies
4. **High**: SingleThreadEngine TODO resolution
5. **Medium**: Enhanced test scenarios and verification
6. **Low**: Performance optimization and cleanup

This plan builds on the substantial infrastructure already completed and focuses on the core missing functionality: proper timing differentiation between EOL and recharge recycling operations.

---

## Appendix B: Detailed EOL Recycling Completion Plan (Recharge Flow Analysis)

### **Analysis: How Recharge Recycling Currently Works**

To properly implement EOL recycling timing, we must understand how recharge recycling flows through the system. Here's the detailed trace:

#### **1. Recharge Recycling Entry Points**

**Primary Entry:** `PopulationChangeRecalcStrategy.java:117`
```java
// Apply recharge recycling before emissions calculation  
RechargeRecyclingRecalcStrategy rechargeRecycling = new RechargeRecyclingRecalcStrategy(Optional.of(scopeEffective));
rechargeRecycling.execute(target, kit);
```

**Context:** This executes during equipment population change calculations, when equipment is being serviced.

#### **2. Recharge Volume Calculation Flow**

**Volume Source:** `RechargeRecyclingRecalcStrategy.calculateAvailableVolume()` 
- **Step 1:** Get base population from `priorEquipment` stream
- **Step 2:** Get recharge population via `streamKeeper.getRechargePopulation(scope)`  
- **Step 3:** Set population context: `stateGetter.setPopulation(rechargePop)`
- **Step 4:** Get recharge intensity: `streamKeeper.getRechargeIntensity(scope)`
- **Step 5:** Convert to volume: `unitConverter.convert(rechargeIntensityRaw, "kg")`

**Key Insight:** Recharge recycling volume = `rechargePopulation × rechargeIntensity`

#### **3. Rate Application Flow**

**Rate Retrieval:** `AbstractRecyclingRecalcStrategy.execute()`
```java
// Get stage-specific rates
EngineNumber recoveryRate = streamKeeper.getRecoveryRate(scopeEffective, stage);  
EngineNumber yieldRate = streamKeeper.getYieldRate(scopeEffective, stage);
```

**Recycling Calculation:** `AbstractRecyclingRecalcStrategy.applyRecycling()`
- **Step 1:** Set volume context: `stateGetter.setVolume(availableVolume)`
- **Step 2:** Calculate recovered: `unitConverter.convert(recoveryRate, "kg")`
- **Step 3:** Calculate recycled: Use recovered volume as context, apply `yieldRate`
- **Step 4:** Apply displacement: `recycledVolume × displacementRate`
- **Step 5:** Set recycle stream: `target.setStreamFor("recycle", recycledAmount, ...)`

#### **4. Integration with Sales Flow**

**Critical Connection:** `SalesRecalcStrategy.java:137-140`
```java
// Recycle stream affects sales calculations
EngineNumber newRecycleValue = new EngineNumber(recycledDisplacedKg, "kg");
streamKeeper.setStream(scopeEffective, "recycle", newRecycleValue);
```

**Sales Impact:** The recycled material displaces import/manufacture according to distribution percentages.

### **EOL Recycling Implementation Requirements (Paralleling Recharge)**

#### **1. EOL Volume Calculation (Required Implementation)**

**EOL Equivalent to Recharge Flow:**
```java
// EolRecyclingRecalcStrategy.calculateAvailableVolume() must implement:
// Step 1: Get equipment being retired (similar to recharge population)
// Step 2: Calculate volume based on retirement context
// Step 3: Use retirement rate and prior population for volume calculation
```

**Key Difference:** Instead of `rechargePopulation × rechargeIntensity`, EOL uses:
`retiringEquipment × equipmentChargeIntensity`

**Required Implementation:**
```java
protected EngineNumber calculateAvailableVolume(Engine target, RecalcKit kit, UseKey scope) {
  // Get prior equipment population
  EngineNumber priorPopulation = target.getStream("priorEquipment", Optional.of(scope), Optional.empty());
  
  // Get retirement rate to calculate retiring equipment
  stateGetter.setPopulation(priorPopulation);
  EngineNumber retirementRateRaw = streamKeeper.getRetirementRate(scope);
  EngineNumber retiringUnits = unitConverter.convert(retirementRateRaw, "units");
  stateGetter.clearPopulation();
  
  // Calculate substance volume in retiring equipment
  stateGetter.setPopulation(retiringUnits);
  EngineNumber initialChargeRaw = target.getInitialCharge("sales");
  EngineNumber substanceVolume = unitConverter.convert(initialChargeRaw, "kg");
  stateGetter.clearPopulation();
  
  return substanceVolume;
}
```

#### **2. Timing Integration Points**

**EOL Recycling Trigger:** `RetireRecalcStrategy.java:85`
```java
// This is WHERE EOL recycling must occur - during equipment retirement
EolRecyclingRecalcStrategy eolRecycling = new EolRecyclingRecalcStrategy(Optional.of(scopeEffective));
eolRecycling.execute(target, kit);
```

**Critical Timing:** EOL recycling MUST happen:
- **AFTER** retirement calculations (lines 74-82) determine how much equipment is retiring
- **BEFORE** EOL emissions calculations (line 89) to affect emission calculations
- **DURING** the same timestep as equipment retirement

#### **3. Stream Coordination Requirements**

**Stage-Specific Rate Storage:** `StreamKeeper` extensions must handle:
```java
// EOL rates stored separately from recharge rates
public void setRecoveryRate(UseKey useKey, EngineNumber newValue, RecoverStage stage) {
  if (stage == RecoverStage.EOL) {
    parameterization.setEolRecoveryRate(newValue);
  } else if (stage == RecoverStage.RECHARGE) {
    parameterization.setRechargeRecoveryRate(newValue);
  }
  // Maintain additive behavior for multiple recycling programs
}
```

**Rate Persistence:** `StreamParameterization.resetStateAtTimestep()` must:
- Reset EOL rates to 0% (indicating no active recycling program)  
- Reset recharge rates to 0%
- Preserve yield rates (efficiency doesn't reset between years)

#### **4. Integration with Existing Recalc Strategies**

**EOL Integration Pattern (Following Recharge Model):**
```java
// In RetireRecalcStrategy.java (parallel to PopulationChangeRecalcStrategy.java:117)
EolRecyclingRecalcStrategy eolRecycling = new EolRecyclingRecalcStrategy(Optional.of(scopeEffective));
eolRecycling.execute(target, kit);

EolEmissionsRecalcStrategy eolStrategy = new EolEmissionsRecalcStrategy(Optional.of(scopeEffective));
eolStrategy.execute(target, kit);
```

**Recycle Stream Management:**
- EOL recycling sets `"recycle"` stream with displaced material
- Sales recalculation uses this to adjust import/manufacture
- Multiple recycling stages contribute additively to total recycling stream

### **Implementation Sequence (Based on Recharge Analysis)**

#### **Phase 1: Volume Calculation Implementation**
1. Complete `EolRecyclingRecalcStrategy.calculateAvailableVolume()`
2. Use retirement rate and equipment population (parallel to recharge calculation)
3. Calculate substance volume from retiring equipment

#### **Phase 2: Rate Management System**
1. Implement stage-specific methods in `StreamKeeper`
2. Extend `StreamParameterization` with EOL rate fields
3. Ensure proper rate persistence and reset behavior

#### **Phase 3: SingleThreadEngine Integration**
1. Replace TODOs in lines 694, 715
2. Set stage-specific rates: `streamKeeper.setRecoveryRate(scope, rate, stage)`
3. Remove immediate recalc - let strategies handle timing

#### **Phase 4: Test Verification**
1. Verify EOL recycling only occurs during retirement (not recharge)
2. Verify recharge recycling only occurs during servicing (not retirement)
3. Test additive behavior when both stages have recycling programs
4. Verify proper integration with emission calculations

### **Critical Implementation Details**

#### **Equipment Lifecycle Context**
- **Recharge:** Equipment being serviced (active, in-use population)
- **EOL:** Equipment being retired (exiting population, becoming waste)

#### **Volume Calculation Differences**
- **Recharge:** `RechargeVolumeCalculator` uses servicing patterns
- **EOL:** Must calculate from retirement rate and equipment charge

#### **Emission Integration**
- **Recharge:** Affects `rechargeEmissions` calculation timing
- **EOL:** Affects `eolEmissions` calculation timing
- **Both:** Contribute to overall `recycle` stream for sales displacement

#### **Stream Flow Impact**
- **Input:** Stage-specific recovery/yield rates from QTA parsing
- **Processing:** Stage-appropriate volume calculation and recycling application  
- **Output:** Unified `recycle` stream affecting sales and emissions

### **Success Criteria**

1. **Timing Isolation:** EOL recycling only triggers during `RetireRecalcStrategy`
2. **Volume Accuracy:** EOL recycling uses retiring equipment volume, not servicing volume
3. **Rate Independence:** EOL and recharge rates stored/applied separately
4. **Integration Consistency:** Both stages contribute to total recycling displacement
5. **Test Validation:** All existing tests pass with proper stage differentiation

This detailed plan ensures EOL recycling follows the same architectural patterns as recharge recycling while operating on the correct equipment lifecycle events and volume sources.
