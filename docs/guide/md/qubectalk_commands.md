# QubecTalk Reference: Commands

Commands are the executable statements within QubecTalk stanzas that define substance properties, set values, and configure policies.

## Contents

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

## Cap

**Purpose:** Limits consumption to specified levels, optionally with displacement to alternative substances.

**Syntax:** `cap streamName to amount [displacing "substanceName"] during years startYear to endYear`

**Examples:**
```qubectalk
cap sales to 80% during years 2027 to 2030
cap domestic to 0 mt displacing "R-600a" during years 2031 to onwards
cap import to 50% during years 2028 to 2032
```

## Change

**Purpose:** Applies growth or decline rates to consumption over time.

**Syntax:** `change streamName by percentage % / year during years startYear to endYear`

**Examples:**
```qubectalk
change sales by 6% / year during years 2025 to 2030
change domestic by -3% / year during years 2031 to 2035
change import by 5% / year during years 2025 to onwards
```

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

**Syntax:** `equals gwpValue tCO2e / unit [energyValue kwh / unit]`

**Examples:**
```qubectalk
equals 1430 tCO2e / mt
equals 675 tCO2e / mt 100 kwh / unit
equals 3 tCO2e / mt
```

## Floor

**Purpose:** Sets minimum consumption levels to prevent unrealistic reductions.

**Syntax:** `floor streamName to amount during years startYear to endYear`

**Examples:**
```qubectalk
floor sales to 10% during years 2030 to onwards
floor domestic to 5 mt during years 2025 to 2035
```

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

**Purpose:** Implements recycling programs that recover and reuse substances from end-of-life equipment.

**Syntax:** `recover percentage % with reuse_percentage % reuse during years startYear to endYear`

**Examples:**
```qubectalk
recover 30% with 90% reuse during years 2027 to onwards
recover 50% with 80% reuse during years 2030 to 2035
recover 25% with 100% reuse at eol during years 2028 to onwards
```

## Replace

**Purpose:** Substitutes one substance with another in new equipment over specified time periods.

**Syntax:** `replace with "newSubstanceName" during years startYear to endYear`

**Examples:**
```qubectalk
replace with "R-600a" during years 2027 to 2030
replace with "HFC-32" during years 2025 to onwards
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