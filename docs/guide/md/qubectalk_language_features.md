# QubecTalk Reference: Language Features

QubecTalk includes advanced language features for conditional logic, probabilistic modeling, and mathematical operations.

## Contents

- [Comments](#comments)
- [Comparison Operators](#comparison-operators)
- [Conditional Statements](#conditional-statements)
- [Logical Operators](#logical-operators)
- [Mathematical Operations](#mathematical-operations)
- [Probabilistic Sampling](#probabilistic-sampling)
- [Units and Measurements](#units-and-measurements)

## Comments

**Purpose:** Add explanatory text that is ignored by the interpreter.

**Syntax:** `# Comment text`

**Examples:**
```qubectalk
# This is a full line comment
set domestic to 25 mt during year 2025  # This is an end-of-line comment

# Multi-line comments require # on each line
# This explains the following complex calculation
# that implements economic growth projections
change sales by 6% / year during years 2025 to 2030
```

## Comparison Operators

**Purpose:** Compare values to create conditions for logical operations.

**Available Operators:**
- `==` - Equal to
- `!=` - Not equal to
- `>` - Greater than
- `>=` - Greater than or equal to
- `<` - Less than
- `<=` - Less than or equal to

**Examples:**
```qubectalk
define currentYear as 2025
define targetYear as 2030

set import to 100 if currentYear == targetYear else 75 endif mt
set export to 50 if currentYear >= targetYear else 25 endif mt
set domestic to 200 if currentYear < targetYear else 150 endif mt

# Mixed comparison and logical operators
define testA as 1
define testB as 0
set domestic to 90 if testA > 0 and testB == 0 else 55 endif mt during year 6
```

## Conditional Statements

**Purpose:** Execute different actions based on conditions using if-else logic.

**Syntax:** `value if condition else alternative endif`

**Examples:**
```qubectalk
set domestic to 100 if testVar > 10 else 50 endif mt

# Complex nested conditions
set import to 75 if economicGrowth > 5 else 
              50 if economicGrowth > 2 else 
              25 endif endif mt

# With variable definitions
define testVar as 5
set domestic to 100 if testVar > 10 else 50 endif mt during year 2025
```

## Logical Operators

**Purpose:** Combine multiple conditions using logical operations.

**Available Operators:**
- `and` - Both conditions must be true
- `or` - At least one condition must be true
- `xor` - Exactly one condition must be true (exclusive or)

**Examples:**
```qubectalk
define testA as 1
define testB as 0
define testC as 2

# AND operation: 1 and 0 = false, uses else branch (30)
set domestic to 100 if testA and testB else 30 endif mt during year 1

# OR operation: 1 or 0 = true, uses if branch (50)
set domestic to 50 if testA or testB else 20 endif mt during year 2

# XOR operation: 1 xor 2 = false (both truthy), uses else branch (40)
set domestic to 60 if testA xor testC else 40 endif mt during year 3

# Complex with parentheses
set domestic to 70 if (testA or testB) and testC else 35 endif mt during year 4
```

## Mathematical Operations

**Purpose:** Perform arithmetic calculations within expressions.

**Available Operators:**
- `+` - Addition
- `-` - Subtraction
- `*` - Multiplication
- `/` - Division
- `^` - Exponentiation
- `()` - Parentheses for precedence

**Examples:**
```qubectalk
# Basic arithmetic
set domestic to (100 + 50) mt during year 2025
set import to (200 * 0.15) mt during year 2025

# Standard deviation calculation
set priorEquipment to sample normally from mean of 100000 std of (100000 * 0.1) units

# Complex calculations
define baselineConsumption as 100
define growthRate as 5
set domestic to (baselineConsumption * (1 + growthRate / 100)) mt during year 2026
```

## Probabilistic Sampling

**Purpose:** Introduce uncertainty into models using probability distributions for Monte Carlo analysis.

**Available Distributions:**
- `sample normally from mean of X std of Y` - Normal distribution
- `sample uniformly from X to Y` - Uniform distribution

**Examples:**
```qubectalk
# Normal distribution for growth rates
change sales by sample normally from mean of 5% std of 1% / year during years 2025 to 2030

# Normal distribution for equipment populations
set priorEquipment to sample normally from mean of 1000000 std of 100000 units during year 2025

# Uniform distribution for policy effectiveness
recover sample uniformly from 20% to 40% with 90% reuse during years 2027 to onwards
```

## Units and Measurements

**Purpose:** Specify quantities with proper units for consumption, time, and other measurements.

**Volume Units:**
- `kg` - Kilograms
- `mt` - Metric tons
- `unit` / `units` - Equipment units
- `tCO2e` - Tons of CO₂ equivalent
- `kwh` - Kilowatt hours

**Time Units:**
- `year` / `years` / `yr` / `yrs` - Years
- `month` / `months` - Months
- `day` / `days` - Days

**Other Units:**
- `%` - Percentage (context-dependent: same as `% prior year` for caps/floors, `% current` for set/change)
- `% prior year` - Percentage of prior year's value (explicit form)
- `% current` or `% current year` - Percentage of current year's value (explicit form)
- `each` - Per unit (e.g., "% each year")

**Percentage Unit Variants:**

The `%` unit is context-dependent and behaves differently based on the operation:
- In `cap` and `floor` operations: `%` means percentage of prior year's value (same as `% prior year`)
- In `set` and `change` operations: `%` means percentage of current year's value (same as `% current`)

The explicit forms `% prior year` and `% current` (or `% current year`) allow you to override this default behavior when needed and make your intent clearer. For example:
- Use `% prior year` in limits to be explicit about prior year basis
- Use `% current` in growth rates to be explicit about current value basis
- Use explicit forms when you want behavior different from the context default

**Examples:**
```qubectalk
# Volume specifications
set domestic to 25 mt during year 2025
set import to 15000 units during year 2025
initial charge with 0.15 kg / unit for domestic

# GWP and energy
equals 1430 tCO2e / mt 100 kwh / unit

# Time specifications
change sales by 5% / year during years 2025 to 2030
retire 5% each year
recharge 10% with 0.15 kg / unit in all years
```

## See Also

- [QubecTalk Stanzas Reference](qubectalk_stanzas.md)
- [QubecTalk Commands Reference](qubectalk_commands.md)
- [Return to Guide Index](index.md)