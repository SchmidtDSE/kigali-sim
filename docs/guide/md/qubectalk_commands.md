# QubecTalk Reference: Commands

Commands are the executable statements within QubecTalk stanzas that define substance properties, set values, and configure policies.

## Contents

- [Assume](#assume)
- [Cap](#cap)
- [Change](#change)
- [Define](#define)
- [Enable](#enable)
- [Equals (GWP)](#equals-gwp)
- [Floor](#floor)
- [Get](#get)
- [Initial Charge](#initial-charge)
- [Recharge](#recharge)
- [Recover](#recover)
- [Replace](#replace)
- [Retire](#retire)
- [Set](#set)

## Assume

**Purpose:** Controls sales carryover behavior, particularly useful for bank tracking scenarios.

**Syntax:** `assume [no|only recharge|continued] streamName during years startYear to endYear`

**Available Streams:** domestic, import, export, sales, bank, equipment

**Examples:**
```qubectalk
assume no domestic during year 2030
assume only recharge sales during years 2025 to onwards
assume continued import
assume only recharge bank during years 3 to onwards
```

**Behavior:**
- `assume no` - Sets stream to 0 kg (zero all sales)
- `assume only recharge` - Sales only cover recharge needs, no new equipment
- `assume continued` - Continue from previous year (default behavior)

## Cap

**Purpose:** Limits consumption to specified levels, optionally with displacement to alternative substances.

**Syntax:**
```
cap streamName to amount during years startYear to endYear
cap streamName to amount displacing "substanceName"|stream during years startYear to endYear
cap streamName to amount displacing by volume "substanceName"|stream during years startYear to endYear
cap streamName to amount displacing by units "substanceName"|stream during years startYear to endYear
```

**Examples:**
```qubectalk
cap sales to 80% during years 2027 to 2030
cap domestic to 0 mt displacing "R-600a" during years 2031 to onwards
cap import to 50% during years 2028 to 2032
```

### Displacement Types

When using displacement, you can control how the displaced demand is calculated. Displacement always operates in the target stream's current units (units or kg) without changing its tracking mode:

- **displacing** means equivalent displacement (default). This maintains the same quantity in the units last specified. If last value was in kg, displaces by kg. If last value was in units, displaces by units.
- **displacing by volume** means volume-based displacement. This always displaces by substance mass (kg), converting from units if necessary using initial charge values. May result in different equipment counts. In other words, this assumes 1 kg of the original substance is displaced by 1 kg of the new substance.
- **displacing by units** means units-based displacement. This always displaces by equipment units, converting from kg if necessary using initial charge values. Maintains equipment population. In other words, this assumes 1 unit of equipment using the original substance is displaced by 1 unit of equipment using the new substance.

**Examples:**
```qubectalk
# Equivalent displacement (default behavior)
cap sales to 80% displacing "R-600a" during years 2027 to 2030

# Volume-based displacement (always use kg, regardless of spec)
cap sales to 50 units displacing by volume "R-600a" during years 2027 to 2030
# Result: 50 units × 1 kg/unit = 50 kg displaced to R-600a

# Units-based displacement (always use units, regardless of spec)
cap sales to 100 kg displacing by units "R-600a" during years 2027 to 2030
# Result: 100 kg ÷ 1 kg/unit = 100 units displaced to R-600a
```

**Note:** When using `displacing by volume`, total substance mass is preserved but equipment counts may change. When using `displacing by units`, equipment population is preserved but substance mass may change. Use `displacing by units` when equipment tracking is critical (e.g., equipment bank management). Use `displacing by volume` when substance consumption limits are the primary concern.

**Units Tracking Behavior:** When a percentage-based cap triggers (reduces the stream value), it preserves units tracking from the last specified value if that value was in equipment units. For example, if you "set domestic to 10 units" and then "cap domestic to 0%", the cap result will be in units (0 units), which allows recharge calculations to add equipment. If the cap does not trigger (current value is already below the cap), tracking mode remains unchanged.

## Change

**Purpose:** Applies growth or decline rates to consumption over time.

**Syntax:** `change streamName by percentage % / year during years startYear to endYear`

**Examples:**
```qubectalk
change sales by 6% / year during years 2025 to 2030
change domestic by -3% / year during years 2031 to 2035
change import by 5% / year during years 2025 to onwards
```

**Non-Negative Clamping:** Change operations prevent streams from going negative. When a change would result in a negative value (e.g., reducing a 5 kg stream by 10 kg), the stream is clamped to exactly 0. The actual amount removed (not the requested amount) is used for downstream calculations.

**Units Tracking Behavior:** Change operations preserve the current tracking mode of the stream. If the stream is currently in units (equipment-based), the change will be applied in units. If the stream is in kg or mt (volume-based), the change will be applied in volume units. Change operations do not force a conversion between tracking modes.

## Define

**Purpose:** Creates variables for use in calculations and conditional statements.

**Syntax:** `define variableName as value`

**Examples:**
```qubectalk
define baseGrowthRate as 5
define phaseOutYear as 2030
define targetReduction as 80
```

## Enable

**Purpose:** Activates specific supply streams for a substance.

**Syntax:** `enable streamName`

**Available Streams:** domestic, import, export

**Examples:**
```qubectalk
enable domestic
enable import
enable export
```

## Equals (GWP)

**Purpose:** Defines the Global Warming Potential and energy consumption for a substance.

**Syntax:** `equals gwpValue [tCO2e|kgCO2e] / [mt|kg|unit] [energyValue kwh / unit]`

**Examples:**
```qubectalk
equals 1430 tCO2e / mt
equals 1430 kgCO2e / kg
equals 675 kgCO2e / kg 100 kwh / unit
equals 3 tCO2e / mt
```

**Note:** Both tCO2e (tonnes) and kgCO2e (kilograms) CO2 equivalent units are supported for GWP values.

## Floor

**Purpose:** Sets minimum consumption levels to prevent unrealistic reductions.

**Syntax:**
```
floor streamName to amount during years startYear to endYear
floor streamName to amount displacing "substanceName"|stream during years startYear to endYear
floor streamName to amount displacing by volume "substanceName"|stream during years startYear to endYear
floor streamName to amount displacing by units "substanceName"|stream during years startYear to endYear
```

**Examples:**
```qubectalk
floor sales to 10% during years 2030 to onwards
floor domestic to 5 mt during years 2025 to 2035
floor import to 100 units displacing by units "R-600a" during years 2028 to 2032
```

Floor supports the same displacement types as Cap. See [Cap](#cap) for detailed explanation of displacement behavior.

**Units Tracking Behavior:** Floor commands follow the same units tracking behavior as cap commands. When a percentage-based floor triggers (increases the stream value), it preserves units tracking from the last specified value if that value was in equipment units. If the floor does not trigger, tracking mode remains unchanged.

## Get

**Purpose:** Retrieves values from streams for use in expressions and calculations.

**Syntax:** `get streamName [of "substanceName"] [as units]`

**Available Streams:**
- `sales`, `domestic`, `import`, `export` - Sales and trade volumes
- `equipment`, `priorEquipment`, `bank` - Equipment population
- `age` - Weighted average equipment age (read-only, Advanced Editor only)

**Indirect Access (Cross-substance):**

The `of "substanceName"` option allows you to access stream values from other substances within the same application. This is useful for creating dependencies between substances.

**Examples:**
```qubectalk
# Direct stream access
define currentSales as get sales as kg
define equipmentAge as get age as years
retire (get age as years) * 1 % each year

# Age-dependent retirement example
define retirementRate as get age as years
retire retirementRate % each year during years 5 to onwards

# Cross-substance access with indirect option
define substanceADomestic as get domestic of "substance a" as kg
set domestic to (get domestic of "substance a" as kg) * 1.5 during year 1

# Unit conversion with indirect access
define substanceAInMT as get import of "substance a" as mt
```

**Note:** The `age` stream is a computed value that tracks the weighted average age of equipment. It starts at 0 years for new equipment and increases annually. This feature is only available in the Advanced Editor.

## Initial Charge

**Purpose:** Specifies the amount of substance in new equipment per unit.

**Syntax:** `initial charge with amount unit / unit for streamName`

**Examples:**
```qubectalk
initial charge with 0.15 kg / unit for domestic
initial charge with 0.20 kg / unit for import
initial charge with 1.5 kg / unit for domestic
```

## Recharge

**Purpose:** Specifies servicing patterns for existing equipment including frequency and amount.

**Syntax:** `recharge percentage % with amount unit / unit`

**Examples:**
```qubectalk
recharge 10% with 0.15 kg / unit
recharge 15% with 0.85 kg / unit during years 2025 to 2030
recharge 5% with 1.0 kg / unit in all years
```

## Recover

**Purpose:** Implements recycling programs that recover and reuse substances from equipment.

**Syntax:** `recover percentage % with reuse_percentage % reuse [with induction_percentage % induction] [at stage] [displacing "substanceName"] during years startYear to endYear`

**Examples:**
```qubectalk
recover 30% with 90% reuse during years 2027 to onwards
recover 50% with 80% reuse with 100% induction during years 2030 to 2035
recover 25% with 100% reuse with 0% induction at eol during years 2028 to onwards
recover 20% with 80% reuse with default induction at recharge during years 2026 to onwards
recover 15% with 85% reuse displacing "HFC-134a" during years 2025 to onwards
```

### Recycling Stages

- `at recharge` - Recycling occurs during equipment servicing (default)
- `at eol` - Recycling occurs during equipment retirement (end-of-life)

### Induction Control

- `with 0% induction` - Full displacement (recycled material reduces virgin demand)
- `with 100% induction` - Full induced demand (recycled material adds to supply, recommended when uncertain)
- `with 50% induction` - Mixed behavior (partial displacement)
- `with default induction` - Uses system default based on specification type

## Replace

**Purpose:** Substitutes one substance with another in new equipment over specified time periods.

**Syntax:** `replace percentage % of streamName with "newSubstanceName" during years startYear to endYear`

**Examples:**
```qubectalk
replace 50% of domestic with "R-600a" during years 2027 to 2030
replace 100% of sales with "HFC-32" during years 2025 to onwards
```

## Retire

**Purpose:** Sets equipment retirement rates determining equipment lifespan.

**Syntax:** `retire percentage % each year`

**Examples:**
```qubectalk
retire 5% each year
retire 7% each year during years 2025 to 2030
retire 10% each year
```

## Set

**Purpose:** Sets consumption volumes or equipment units for specific streams and time periods.

**Syntax:** `set streamName to amount unit during year year`

**Examples:**
```qubectalk
set domestic to 25 mt during year 2025
set import to 15000 units during year 2025
set priorEquipment to 1000000 units during year 2025
```

## See Also

- [QubecTalk Stanzas Reference](qubectalk_stanzas.md)
- [QubecTalk Language Features Reference](qubectalk_language_features.md)
- [Return to Guide Index](index.md)