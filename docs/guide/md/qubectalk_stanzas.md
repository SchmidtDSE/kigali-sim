# QubecTalk Reference: Stanzas

Stanzas are the main structural blocks of QubecTalk programs. They define different sections of your simulation model using `start` and `end` keywords.

## Contents

- [About](#about)
- [Default](#default)
- [Policy](#policy)
- [Simulations](#simulations)
- [Variables](#variables)

## About

**Purpose:** Optional metadata block providing information about the simulation model.

**Syntax:**
```qubectalk
start about
  # Name: "Simulation Name"
  # Description: "Description"
  # Author: "Author Name"
end about
```

**Example:**
```qubectalk
start about
  # Name: "ABC Country HFC Analysis"
  # Description: "Comprehensive HFC phase-down modeling for ABC Country"
  # Author: "Policy Analysis Team"
end about
```

## Default

**Purpose:** Defines the business-as-usual baseline scenario including applications, substances, and their properties.

**Syntax:**
```qubectalk
start default
  define application "ApplicationName"
    uses substance "SubstanceName"
      # substance configuration
    end substance
  end application
end default
```

**Example:**
```qubectalk
start default
  define application "Domestic Refrigeration"
    uses substance "HFC-134a"
      enable domestic
      initial charge with 0.15 kg / unit for domestic
      set domestic to 25 mt during year 2025
      equals 1430 tCO2e / mt
    end substance
  end application
end default
```

## Policy

**Purpose:** Defines policy interventions that modify the baseline scenario defined in the default stanza.

**Syntax:**
```qubectalk
start policy "PolicyName"
  modify application "ApplicationName"
    modify substance "SubstanceName"
      # policy modifications
    end substance
  end application
end policy
```

**Example:**
```qubectalk
start policy "HFC Phase-out"
  modify application "Domestic Refrigeration"
    modify substance "HFC-134a"
      cap sales to 80% during years 2027 to 2030
      cap sales to 0 mt during years 2031 to onwards
    end substance
  end application
end policy
```

## Simulations

**Purpose:** Configures which scenarios to run, including time periods, policies, and Monte Carlo trials.

**Syntax:**
```qubectalk
start simulations
  simulate "SimulationName"
    using "PolicyName"
  from years StartYear to EndYear
  across NumberOfTrials trials
end simulations
```

**Examples:**

Basic simulation:
```qubectalk
start simulations
  simulate "Business as Usual"
  from years 2025 to 2035
end simulations
```

Policy simulation:
```qubectalk
start simulations
  simulate "Phase-out Scenario"
    using "HFC Phase-out"
  from years 2025 to 2035
end simulations
```

Monte Carlo simulation:
```qubectalk
start simulations
  simulate "Uncertainty Analysis"
    using "HFC Phase-out"
  from years 2025 to 2035
  across 1000 trials
end simulations
```

## Variables

**Purpose:** Optional stanza for defining global variables that can be used throughout the program.

**Syntax:**
```qubectalk
start variables
  define variableName as value
end variables
```

**Example:**
```qubectalk
start variables
  define baselineYear as 2025
  define policyStartYear as 2027
  define simulationEndYear as 2035
end variables
```

## See Also

- [QubecTalk Commands Reference](qubectalk_commands.md)
- [QubecTalk Language Features Reference](qubectalk_language_features.md)
- [Return to Guide Index](index.md)

---

[View HTML version](../qubectalk_stanzas.html)