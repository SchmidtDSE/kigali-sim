# ABC Case Study
To demonstrate the capabilities of Kigali Sim, we will explore ABC. This hypothetical country is a medium-sized nation in the Asia Pacific region that consumes four substances in manufacturing and servicing: HFC-134a, R-410A, HFC-32, and R-404A.

**Currently under construction / in progress.**

## Starting conditions
Before exploring different potential futures, we start by detailing the current state of ABC by substance. These are based on averages of historic information.

### HFC-134a
HFC-134a is used across multiple applications with the following equipment populations and average consumption characteristics (MAC stands for mobile air conditioner):

|                                    | **Prior Equipment Population (Units)** | **Initial Charge (kg/unit)** | **Units Annually Serviced** | **Annual Retirement** | **Annual Recharge (kg/unit)** |
| ---------------------------------- | -------------------------------------- | ---------------------------- | --------------------------- | --------------------- | ----------------------------- |
| **Domestic refrigeration**         | 1,000,000                              | 0.15                         | 10%                         | 2%                    | 0.15                          |
| **Commercial refrigeration**       | 200,000                                | 0.60                         | 30%                         | 3%                    | 0.60                          |
| **MAC**                            | 500,000                                | 1.00                         | 20%                         | 2%                    | 1.00                          |
| **Chiller**                        | 50,000                                 | 5.00                         | 25%                         | 2%                    | 5.00                          |

Local manufacturing for domestic refrigeration is made up of a single manufacturer accounts which for 30,000 units per year are produced. There is no import for domestic refrigeration with HFC-134a. However, 10,000 units per year are imported spread proportionally across all other applications. This has a GWP of 1,430.0 tCO2e.

```
start default

	define application "Domestic Refrigeration"
	
	  uses substance "HFC-134a"
		  set priorEquipment to 1000000 units during beginning
		  initial charge with 0.15 kg / unit for sales
		  retire 2 % each year
		  recharge 10% each year with 0.15 kg / unit
		  set manufacture to 30000 units / year during beginning
		  equals 1430 tCO2e / kg
	  end substance
	
	end application


	define application "Commercial Refrigeration"
	
	  uses substance "HFC-134a"
		  set priorEquipment to 200000 units during beginning
		  initial charge with 0.60 kg / unit for sales
		  retire 3 % each year
		  recharge 30% each year with 0.60 kg / unit
		  equals 1430 tCO2e / kg
		  set import to (10000 * (200000 / 750000)) units / year during year 1
	  end substance
	
	end application


	define application "MAC"
	
	  uses substance "HFC-134a"
		  set priorEquipment to 500000 units during beginning
		  initial charge with 1.00 kg / unit for sales
		  retire 2 % each year
		  recharge 20% each year with 1.00 kg / unit
		  equals 1430 tCO2e / kg
		  set import to (10000 * (500000 / 750000)) units / year during beginning
	  end substance
	
	end application



	define application "Chiller"
	
	  uses substance "HFC-134a"
		  set priorEquipment to 50000 units during beginning
		  initial charge with 5.00 kg / unit for sales
		  retire 2 % each year
		  recharge 25% each year with 5.00 kg / unit
		  equals 1430 tCO2e / kg
		  set import to (10000 * (50000 / 750000)) units / year during beginning
	  end substance
	
	end application

end default
```

### R-404A Applications
R-404A is used in commercial refrigeration applications:

|                              | **Prior Equipment Population (Units)** | **Initial Charge (kg/unit)** | **Units Annually Serviced** | **Annual Retirement** | **Recharge (kg/unit)** |
| ---------------------------- | -------------------------------------- | ---------------------------- | --------------------------- | --------------------- | ---------------------- |
| **Commercial refrigeration** | 90,000                                 | 2.00                         | 10%                         | 2%                    | 2.00                   |
| **Transport refrigeration**  | 20,000                                 | 10.00                        | 30%                         | 3%                    | 10.00                  |

This substance is fully imported and there is no domestic manufacturing. 1,100 units are being imported currently per year. This has a GWP of 3,921.6 tCO2e. While not currently used in domestic refrigeration, MAC and chiller, this could be a replacement in those applications to HFC-134a with the same annual servicing frequency and retirement but 50% of the initial charge and recharge levels.

```
	define application "Transport Refrigeration"

	  uses substance "R-404A"
		  set priorEquipment to 90000 units during beginning
		  initial charge with 2.00 kg / unit for sales
		  retire 2 % each year
		  recharge 10% each year with 2.00 kg / unit
		  set import to 900 units / year during beginning
		  equals 3921.6 tCO2e / kg
	  end substance
	
	end application


	define application "Domestic Refrigeration"

		# ... HFC-134a ...

		uses substance "R-404A"
		  initial charge with 0.08 kg / unit for sales
		  retire 2 % each year
		  recharge 10% each year with 0.08 kg / unit
		  equals 3921.6 tCO2e / kg
	  end substance

	end application


	define application "Commercial Refrigeration"

		# ... HFC-134a ...
	
	  uses substance "R-404A"
		  set priorEquipment to 20000 units during beginning
		  initial charge with 10.00 kg / unit for sales
		  retire 3 % each year
		  recharge 30% each year with 10.00 kg / unit
		  equals 3921.6 tCO2e / kg
		  set import to 200 units / year during beginning
	  end substance
	
	end application


	define application "MAC"

		# ... HFC-134a ...
	
	  uses substance "R-404A"
		  initial charge with 0.50 kg / unit for sales
		  retire 2 % each year
		  recharge 20% each year with 0.50 kg / unit
		  equals 3921.6 tCO2e / kg
	  end substance
	
	end application


	define application "Chiller"

		# ... HFC-134a ...
	
	  uses substance "R-404A"
		  initial charge with 2.50 kg / unit for sales
		  retire 2 % each year
		  recharge 25% each year with 2.50 kg / unit
		  equals 3921.6 tCO2e / kg
	  end substance
	
	end application
```

### HFC-32
HFC-32 is used in domestic and commercial air conditioning:

|                     | **Prior Equipment Population (Units)** | **Initial Charge (kg/unit)** | **Units Annually Serviced** | **Annual Retirement** | **Recharge (kg/unit)** |
| ------------------- | -------------------------------------- | ---------------------------- | --------------------------- | --------------------- | ---------------------- |
| **Domestic AC**     | 20,000                                 | 0.85                         | 10%                         | 1%                    | 0.85                   |
| **Commercial AC**   | 5,000                                  | 4.00                         | 10%                         | 1%                    | 4.00                   |

There is currently no domestic manufacturing for HFC-32 and 1,000 units are imported per year. This has a GWP of 675.0 tCO2e.

```
	define application "Domestic AC"

	  uses substance "HFC-32"
		  set priorEquipment to 20000 units during beginning
		  initial charge with 0.85 kg / unit for sales
		  retire 1 % each year
		  recharge 10% each year with 0.85 kg / unit
		  set import to (1000 * (20000 / 25000)) units / year during beginning
		  equals 675.0 tCO2e / kg
	  end substance

	end application

	define application "Commercial AC"

		uses substance "HFC-32"
		  set priorEquipment to 5000 units during beginning
		  initial charge with 4.00 kg / unit for sales
		  retire 1 % each year
		  recharge 10% each year with 4.00 kg / unit
		  set import to (1000 * (5000 / 25000)) units / year during beginning
		  equals 675.0 tCO2e / kg
	  end substance

	end application
```

### R-410A
Both substances are used in domestic and commercial air conditioning:

|                     | **Prior Equipment Population (Units)** | **Initial Charge (kg/unit)** | **Units Annually Serviced** | **Annual Retirement** | **Recharge (kg/unit)** |
| ------------------- | -------------------------------------- | ---------------------------- | --------------------------- | --------------------- | ---------------------- |
| **Domestic AC**     | 400,000                                | 1.00                         | 10%                         | 1%                    | 1.00                   |
| **Commercial AC**   | 30,000                                 | 5.00                         | 10%                         | 1%                    | 5.00                   |

There is one Domestic AC manufacturer using R-410A with production volume of 40,000 units per annum. Domestic AC are imported at 100 units per year and Commercial AC are imported at 3,000 units per year. This has a GWP of 2,087.5 tCO2e.

```
	define application "Domestic AC"

		# ... HFC-32 ...

	  uses substance "R-410A"
		  set priorEquipment to 400000 units during beginning
		  initial charge with 1.00 kg / unit for sales
		  retire 1 % each year
		  recharge 10% each year with 1.00 kg / unit
		  set import to 100 units / year during beginning
		  set manufacture to 40000 units / year during beginning
		  equals 2087.5 tCO2e / kg
	  end substance

	end application

	define application "Commercial AC"

		# ... HFC-32 ...

		uses substance "R-410A"
		  set priorEquipment to 30000 units during beginning
		  initial charge with 5.00 kg / unit for sales
		  retire 1 % each year
		  recharge 10% each year with 5.00 kg / unit
		  set import to 3000 units / year during beginning
		  equals 2087.5 tCO2e / kg
	  end substance

	end application
```

## Business as Usual
Before considering policy interventions, we start by examining a business as usual case which does not involve any policy intervention.

### General
We first can use national economic growth projections as well as industry growth projections to set overall trends.

| **Application**          | **2025 - 2033**    | **2034 - 2035**    | **Source**                                     |
| ------------------------ | ------------------ | ------------------ | ---------------------------------------------- |
| Domestic refrigeration   | + 5% annual growth | + 3% annual growth | National economic growth                       |
| Commercial refrigeration | + 8% annual growth | + 5% annual growth | Industry growth in food processing and tourism |
| Commercial AC            | + 8% annual growth | + 5% annual growth | Industry growth in food processing and tourism |

In addition to these high level trends, there are some assumptions specific to individual substances. First, we anticipate 50% by weight of HFC initial charge for refrigeration equipment for alternatives. This leads to R-404A initial charges of 0.03 kg / unit for commercial refrigeration and 0.08 for domestic refrigeration.

```
	define application "Domestic Refrigeration"

		uses substance "HFC-134a"
			# ... existing code ...
			change equipment by +5 % each year from year 2025 to 2033
			change equipment by +3 % each year from year 2034 to 2035
		end substance

		# ... R-404A ...

	end application


	define application "Commercial Refrigeration"

		uses substance "HFC-134a"
			# ... existing code ...
			change equipment by +8 % each year from year 2025 to 2033
			change equipment by +5 % each year from year 2034 to 2035
		end substance

		uses substance "R-404A"
			# ... existing code ...
			change equipment by +8 % each year from year 2025 to 2033
			change equipment by +5 % each year from year 2034 to 2035
		end substance

	end application


	define application "Commercial AC"

		uses substance "HFC-32"
			# ... existing code ...
			change equipment by +8 % each year from year 2025 to 2033
			change equipment by +5 % each year from year 2034 to 2035
		end substance

		uses substance "R-410A"
			# ... existing code ...
			change equipment by +8 % each year from year 2025 to 2033
			change equipment by +5 % each year from year 2034 to 2035
		end substance

	end application
```

That said, we will further refine this based off of additional substance-specific information.

### Substance-specific
All that said, let’s assume we have some more specific information available about some substances. We will refine our earlier code to reflect.

First, for HFC-32, we expect growth in imports to grow by 100% linearly from 2025 to 2035 without impacting R-410A across all applications. We will assume that this corresponds to an annual average percent increase of 7.18% per the CAGR formula. Also, unlike historic data, the simulation should expect 90% of the R-410A initial charge size. This means 0.90 kg / unit for residential AC and 4.50 kg / unit for commercial AC for imports only. 

```
	define application "Domestic AC"

		uses substance "HFC-32"
		  initial charge with 0.90 kg / unit for sales
			# ... other code ...
			change equipment by +7.18 % each year from year 2025 to 2035
		end substance

		# ... R-410A ...

	end application


  define application "Commercial AC"

		uses substance "HFC-32"
		  initial charge with 4.50 kg / unit for sales
			# ... other code ...
			change equipment by +7.18 % each year from year 2025 to 2035
		end substance

		# ... R-410A ...

	end application
```

Next, moving forward, we will assume that HFC consumption will be a bit higher than historic values. Specifically, we will assume a 20% increase over the historic averages in HFC consumption. This should be reflected in starting conditions used in the first year of simulation.  This is used to represent HCFC component of baseline.

```
  define application "Domestic Refrigeration"
	
	  uses substance "HFC-134a"
		  set priorEquipment to 1000000 * 120 % units during beginning
		  # ... other code ...
	  end substance
	
	end application


	define application "Commercial Refrigeration"
	
	  uses substance "HFC-134a"
		  set priorEquipment to 200000 * 120 % units during beginning
		  # ... other code ...
	  end substance
	
	end application


	define application "Mobile AC"
	
	  uses substance "HFC-134a"
		  set priorEquipment to 500000 * 120 % units during beginning
		  # ... other code ...
	  end substance
	
	end application


	define application "Chillers"
	
	  uses substance "HFC-134a"
		  set priorEquipment to 50000 * 120 % units during beginning
			# ... other code ...
	  end substance
	
	end application


	define application "Domestic AC"

	  uses substance "HFC-32"
		  set priorEquipment to 20000 * 120 % units during beginning
		  initial charge with 0.85 kg / unit for sales
		  retire 1 % each year
		  recharge 10% each year with 0.85 kg / unit
		  set import to (1000 * (20000 / 25000)) units / year during beginning
		  equals 675.0 tCO2e / kg
	  end substance

	end application

	define application "Commercial AC"

		uses substance "HFC-32"
		  set priorEquipment to 5000 * 120 % units during beginning
		  initial charge with 4.00 kg / unit for sales
		  retire 1 % each year
		  recharge 10% each year with 4.00 kg / unit
		  set import to (1000 * (5000 / 25000)) units / year during beginning
		  equals 675.0 tCO2e / kg
	  end substance

	end application
```

### Fallback
For variables in application / substance combinations not listed we assume an unchanged continuation of starting of starting conditions based on historic averages. This does not require modification to the code.

## Policies
Policy options can be modeled individually or in combination to demonstrate how different intervention strategies would result in compliance or non-compliance with Kigali Amendment targets. Each option addresses different aspects of the HFC consumption cycle. Specifically, we will consider manufacturing prohibition targeting new equipment production, import bans controlling market supply of HFC-based equipment, and enhanced recovery/recycling reducing service sector consumption from existing equipment stock through recharge.

### New Equipment Prohibition
Coming into effect in 2028, a full prohibition of domestic manufacturing of new HFC-134a and R-410A equipment. This leads manufacturing consumption of controlled substances to falls but servicing (recharge) continues for existing equipment. In this process, HFC-32 replaces the new units lost that would have otherwise been manufactured by R-410A. However, nothing replaces the new units which are lost from HFC-134a.

```
start policy "Manufacturing Prohibition"

	modify application "Domestic Refrigeration"
	
		modify substance "HFC-134a"
			cap manufacture to 0% during years 2028 to onwards
		end substance
	
	end application

	modify application "Domestic AC"

	  modify substance "R-410A"
		  cap manufacture to 0% displacing "HFC-32" during years 2028 to onwards
	  end substance

	end application

	modify application "Commercial AC"

		modify substance "R-410A"
		  cap manufacture to 0% displacing "HFC-32" during years 2028 to onwards
	  end substance

	end application

end policy
```

### Equipment Import Ban
Coming into effect in 2029, this policy would place a full ban on imports for HFC equipment. Instead, imports would shift to alternatives in R-410A and R-404A. These would be based on the application compatibilities of each substance. That said, recharge would continue for existing equipment.

```
start policy "Equipment Import Ban"

	modify application "Commercial Refrigeration"
	
		modify substance "HFC-134a"
			cap import to 0% displacing "R-404A" during years 2029 to onwards
		end substance
	
	end application

	modify application "MAC"
	
		modify substance "HFC-134a"
			cap import to 0% displacing "R-404A" during years 2029 to onwards
		end substance
	
	end application

	modify application "Chiller"
	
		modify substance "HFC-134a"
			cap import to 0% displacing "R-404A" during years 2029 to onwards
		end substance
	
	end application

	modify application "Domestic AC"

	  modify substance "HFC-32"
		  cap import to 0% displacing "R-410A" during years 2029 to onwards
	  end substance

	end application

	modify application "Commercial AC"

		modify substance "HFC-32"
		  cap import to 0% displacing "R-410A" during years 2029 to onwards
	  end substance

	end application

end policy
```

### Enhanced Recovery and Recycling
Continuous starting now (2025) and increasing to 2030. Over these 5 years, recycling at recharge would go from 0% to 70% with 14% added each year. By adding code for this, we reduce recharge effectively to 30% of the original value once the policy is in full force.

```
start policy "Recovery and Recycling"

	modify application "Domestic Refrigeration"
	
		modify substance "HFC-134a"
			recover 14 % with 100 % reuse during year 2026
			recover 28 % with 100 % reuse during year 2027
			recover 42 % with 100 % reuse during year 2028
			recover 56 % with 100 % reuse during year 2029
			recover 70 % with 100 % reuse during years 2030 to onwards
		end substance
		
		modify substance "R-404A"
			recover 14 % with 100 % reuse during year 2026
			recover 28 % with 100 % reuse during year 2027
			recover 42 % with 100 % reuse during year 2028
			recover 56 % with 100 % reuse during year 2029
			recover 70 % with 100 % reuse during years 2030 to onwards
		end substance
	
	end application


	modify application "Commercial Refrigeration"
	
		modify substance "HFC-134a"
			recover 14 % with 100 % reuse during year 2026
			recover 28 % with 100 % reuse during year 2027
			recover 42 % with 100 % reuse during year 2028
			recover 56 % with 100 % reuse during year 2029
			recover 70 % with 100 % reuse during years 2030 to onwards
		end substance
		
		modify substance "R-404A"
			recover 14 % with 100 % reuse during year 2026
			recover 28 % with 100 % reuse during year 2027
			recover 42 % with 100 % reuse during year 2028
			recover 56 % with 100 % reuse during year 2029
			recover 70 % with 100 % reuse during years 2030 to onwards
		end substance
	
	end application


	modify application "Mobile AC"
	
		modify substance "HFC-134a"
			recover 14 % with 100 % reuse during year 2026
			recover 28 % with 100 % reuse during year 2027
			recover 42 % with 100 % reuse during year 2028
			recover 56 % with 100 % reuse during year 2029
			recover 70 % with 100 % reuse during years 2030 to onwards
		end substance
		
		modify substance "R-404A"
			recover 14 % with 100 % reuse during year 2026
			recover 28 % with 100 % reuse during year 2027
			recover 42 % with 100 % reuse during year 2028
			recover 56 % with 100 % reuse during year 2029
			recover 70 % with 100 % reuse during years 2030 to onwards
		end substance
	
	end application


	modify application "Chillers"
	
		modify substance "HFC-134a"
			recover 14 % with 100 % reuse during year 2026
			recover 28 % with 100 % reuse during year 2027
			recover 42 % with 100 % reuse during year 2028
			recover 56 % with 100 % reuse during year 2029
			recover 70 % with 100 % reuse during years 2030 to onwards
		end substance
		
		modify substance "R-404A"
			recover 14 % with 100 % reuse during year 2026
			recover 28 % with 100 % reuse during year 2027
			recover 42 % with 100 % reuse during year 2028
			recover 56 % with 100 % reuse during year 2029
			recover 70 % with 100 % reuse during years 2030 to onwards
		end substance
	
	end application

	modify application "Residential AC"

	  modify substance "HFC-32"
			recover 14 % with 100 % reuse during year 2026
			recover 28 % with 100 % reuse during year 2027
			recover 42 % with 100 % reuse during year 2028
			recover 56 % with 100 % reuse during year 2029
			recover 70 % with 100 % reuse during years 2030 to onwards
	  end substance

	  modify substance "R-410A"
			recover 14 % with 100 % reuse during year 2026
			recover 28 % with 100 % reuse during year 2027
			recover 42 % with 100 % reuse during year 2028
			recover 56 % with 100 % reuse during year 2029
			recover 70 % with 100 % reuse during years 2030 to onwards
	  end substance

	end application

	modify application "Commercial AC"

		modify substance "HFC-32"
			recover 14 % with 100 % reuse during year 2026
			recover 28 % with 100 % reuse during year 2027
			recover 42 % with 100 % reuse during year 2028
			recover 56 % with 100 % reuse during year 2029
			recover 70 % with 100 % reuse during years 2030 to onwards
	  end substance

		modify substance "R-410A"
			recover 14 % with 100 % reuse during year 2026
			recover 28 % with 100 % reuse during year 2027
			recover 42 % with 100 % reuse during year 2028
			recover 56 % with 100 % reuse during year 2029
			recover 70 % with 100 % reuse during years 2030 to onwards
	  end substance

	end application

end policy
```

## Simulations
Policies can be combined or “stacked” together in different scenarios. We will have the following simulations:

 - **Business as Usual** without any policies.
 - **Manufacturing Prohibition** with just that intervention alone.
 - **Equipment Import Ban** which includes just that policy.
 - **Recovery and Recycling** that only includes that program.
 - **Combined** which uses all three policies considered at the same time.

Other policy combinations like manufacturing prohibition and equipment import ban may also be added later.

```
start simulations

  simulate "Business as Usual" from years 2025 to 2035

  simulate "Manufacturing Prohibition" using "Manufacturing Prohibition" from years 2025 to 2035

  simulate "Equipment Import Ban" using "Equipment Import Ban" from years 2025 to 2035

  simulate "Recovery and Recycling" using "Recovery and Recycling" from years 2025 to 2035

  simulate "Combined" 
    using "Manufacturing Prohibition" 
    then "Equipment Import Ban" 
    then "Recovery and Recycling" 
  from years 2025 to 2035

end simulations
```

## Notes
Don’t forget that, when setting starting conditions, use `during beginning` so that the `set` command is not repeated across all years.

## Complete implementation
The full implementation is as follows:

```
start default

  define application "Domestic Refrigeration"

    uses substance "HFC-134a"
      set priorEquipment to 1200000 units during year 2025
      initial charge with 0.15 kg / unit for sales
      retire 2 % each year
      recharge 10 % each year with 0.15 kg / unit
      set manufacture to 30000 units / year during year 2025
      change equipment by +5 % each year during years 2026 to 2033
      change equipment by +3 % each year during years 2034 to 2035
      equals 1430 tCO2e / kg
    end substance

    uses substance "R-404A"
      initial charge with 0.08 kg / unit for sales
      retire 2 % each year
      recharge 10 % each year with 0.08 kg / unit
      equals 3921.6 tCO2e / kg
    end substance

  end application

  define application "Commercial Refrigeration"

    uses substance "HFC-134a"
      set priorEquipment to 240000 units during year 2025
      initial charge with 0.60 kg / unit for sales
      retire 3 % each year
      recharge 30 % each year with 0.60 kg / unit
      set import to 2667 units / year during year 2025
      change equipment by +8 % each year during years 2026 to 2033
      change equipment by +5 % each year during years 2034 to 2035
      equals 1430 tCO2e / kg
    end substance

    uses substance "R-404A"
      set priorEquipment to 20000 units during year 2025
      initial charge with 10.00 kg / unit for sales
      retire 3 % each year
      recharge 30 % each year with 10.00 kg / unit
      set import to 200 units / year during year 2025
      change equipment by +8 % each year during years 2026 to 2033
      change equipment by +5 % each year during years 2034 to 2035
      equals 3921.6 tCO2e / kg
    end substance

  end application

  define application "MAC"

    uses substance "HFC-134a"
      set priorEquipment to 600000 units during year 2025
      initial charge with 1.00 kg / unit for sales
      retire 2 % each year
      recharge 20 % each year with 1.00 kg / unit
      set import to 6667 units / year during year 2025
      equals 1430 tCO2e / kg
    end substance

    uses substance "R-404A"
      initial charge with 0.50 kg / unit for sales
      retire 2 % each year
      recharge 20 % each year with 0.50 kg / unit
      equals 3921.6 tCO2e / kg
    end substance

  end application

  define application "Chiller"

    uses substance "HFC-134a"
      set priorEquipment to 60000 units during year 2025
      initial charge with 5.00 kg / unit for sales
      retire 2 % each year
      recharge 25 % each year with 5.00 kg / unit
      set import to 667 units / year during year 2025
      equals 1430 tCO2e / kg
    end substance

    uses substance "R-404A"
      initial charge with 2.50 kg / unit for sales
      retire 2 % each year
      recharge 25 % each year with 2.50 kg / unit
      equals 3921.6 tCO2e / kg
    end substance

  end application

  define application "Transport Refrigeration"

    uses substance "R-404A"
      set priorEquipment to 90000 units during year 2025
      initial charge with 2.00 kg / unit for sales
      retire 2 % each year
      recharge 10 % each year with 2.00 kg / unit
      set import to 900 units / year during year 2025
      equals 3921.6 tCO2e / kg
    end substance

  end application

  define application "Domestic AC"

    uses substance "HFC-32"
      set priorEquipment to 24000 units during year 2025
      initial charge with 0.90 kg / unit for sales
      retire 1 % each year
      recharge 10 % each year with 0.85 kg / unit
      set import to 800 units / year during year 2025
      change equipment by +7 % each year during years 2026 to 2035
      equals 675 tCO2e / kg
    end substance

    uses substance "R-410A"
      set priorEquipment to 400000 units during year 2025
      initial charge with 1.00 kg / unit for sales
      retire 1 % each year
      recharge 10 % each year with 1.00 kg / unit
      set import to 100 units / year during year 2025
      set manufacture to 40000 units / year during year 2025
      equals 2087.5 tCO2e / kg
    end substance

  end application

  define application "Commercial AC"

    uses substance "HFC-32"
      set priorEquipment to 6000 units during year 2025
      initial charge with 4.50 kg / unit for sales
      retire 1 % each year
      recharge 10 % each year with 4.00 kg / unit
      set import to 200 units / year during year 2025
      change equipment by +7 % each year during years 2026 to 2035
      equals 675 tCO2e / kg
    end substance

    uses substance "R-410A"
      set priorEquipment to 30000 units during year 2025
      initial charge with 5.00 kg / unit for sales
      retire 1 % each year
      recharge 10 % each year with 5.00 kg / unit
      set import to 3000 units / year during year 2025
      change equipment by +8 % each year during years 2026 to 2033
      change equipment by +5 % each year during years 2034 to 2035
      equals 2087.5 tCO2e / kg
    end substance

  end application

end default


start policy "Manufacturing Prohibition"

  modify application "Domestic Refrigeration"
    modify substance "HFC-134a"
      cap manufacture to 0% during years 2028 to onwards
    end substance
  end application

  modify application "Domestic AC"
    modify substance "R-410A"
      cap manufacture to 0% displacing "HFC-32" during years 2028 to onwards
    end substance
  end application

  modify application "Commercial AC"
    modify substance "R-410A"
      cap manufacture to 0% displacing "HFC-32" during years 2028 to onwards
    end substance
  end application

end policy


start policy "Equipment Import Ban"

  modify application "Commercial Refrigeration"
    modify substance "HFC-134a"
      cap import to 0% displacing "R-404A" during years 2029 to onwards
    end substance
  end application

  modify application "MAC"
    modify substance "HFC-134a"
      cap import to 0% displacing "R-404A" during years 2029 to onwards
    end substance
  end application

  modify application "Chiller"
    modify substance "HFC-134a"
      cap import to 0% displacing "R-404A" during years 2029 to onwards
    end substance
  end application

  modify application "Domestic AC"
    modify substance "HFC-32"
      cap import to 0% displacing "R-410A" during years 2029 to onwards
    end substance
  end application

  modify application "Commercial AC"
    modify substance "HFC-32"
      cap import to 0% displacing "R-410A" during years 2029 to onwards
    end substance
  end application

end policy


start policy "Recovery and Recycling"

  modify application "Domestic Refrigeration"
    modify substance "HFC-134a"
      recover 14 % with 100 % reuse during year 2026
      recover 28 % with 100 % reuse during year 2027
      recover 42 % with 100 % reuse during year 2028
      recover 56 % with 100 % reuse during year 2029
      recover 70 % with 100 % reuse during years 2030 to onwards
    end substance
    modify substance "R-404A"
      recover 14 % with 100 % reuse during year 2026
      recover 28 % with 100 % reuse during year 2027
      recover 42 % with 100 % reuse during year 2028
      recover 56 % with 100 % reuse during year 2029
      recover 70 % with 100 % reuse during years 2030 to onwards
    end substance
  end application

  modify application "Commercial Refrigeration"
    modify substance "HFC-134a"
      recover 14 % with 100 % reuse during year 2026
      recover 28 % with 100 % reuse during year 2027
      recover 42 % with 100 % reuse during year 2028
      recover 56 % with 100 % reuse during year 2029
      recover 70 % with 100 % reuse during years 2030 to onwards
    end substance
    modify substance "R-404A"
      recover 14 % with 100 % reuse during year 2026
      recover 28 % with 100 % reuse during year 2027
      recover 42 % with 100 % reuse during year 2028
      recover 56 % with 100 % reuse during year 2029
      recover 70 % with 100 % reuse during years 2030 to onwards
    end substance
  end application

  modify application "MAC"
    modify substance "HFC-134a"
      recover 14 % with 100 % reuse during year 2026
      recover 28 % with 100 % reuse during year 2027
      recover 42 % with 100 % reuse during year 2028
      recover 56 % with 100 % reuse during year 2029
      recover 70 % with 100 % reuse during years 2030 to onwards
    end substance
    modify substance "R-404A"
      recover 14 % with 100 % reuse during year 2026
      recover 28 % with 100 % reuse during year 2027
      recover 42 % with 100 % reuse during year 2028
      recover 56 % with 100 % reuse during year 2029
      recover 70 % with 100 % reuse during years 2030 to onwards
    end substance
  end application

  modify application "Chiller"
    modify substance "HFC-134a"
      recover 14 % with 100 % reuse during year 2026
      recover 28 % with 100 % reuse during year 2027
      recover 42 % with 100 % reuse during year 2028
      recover 56 % with 100 % reuse during year 2029
      recover 70 % with 100 % reuse during years 2030 to onwards
    end substance
    modify substance "R-404A"
      recover 14 % with 100 % reuse during year 2026
      recover 28 % with 100 % reuse during year 2027
      recover 42 % with 100 % reuse during year 2028
      recover 56 % with 100 % reuse during year 2029
      recover 70 % with 100 % reuse during years 2030 to onwards
    end substance
  end application

  modify application "Domestic AC"
    modify substance "HFC-32"
      recover 14 % with 100 % reuse during year 2026
      recover 28 % with 100 % reuse during year 2027
      recover 42 % with 100 % reuse during year 2028
      recover 56 % with 100 % reuse during year 2029
      recover 70 % with 100 % reuse during years 2030 to onwards
    end substance
    modify substance "R-410A"
      recover 14 % with 100 % reuse during year 2026
      recover 28 % with 100 % reuse during year 2027
      recover 42 % with 100 % reuse during year 2028
      recover 56 % with 100 % reuse during year 2029
      recover 70 % with 100 % reuse during years 2030 to onwards
    end substance
  end application

  modify application "Commercial AC"
    modify substance "HFC-32"
      recover 14 % with 100 % reuse during year 2026
      recover 28 % with 100 % reuse during year 2027
      recover 42 % with 100 % reuse during year 2028
      recover 56 % with 100 % reuse during year 2029
      recover 70 % with 100 % reuse during years 2030 to onwards
    end substance
    modify substance "R-410A"
      recover 14 % with 100 % reuse during year 2026
      recover 28 % with 100 % reuse during year 2027
      recover 42 % with 100 % reuse during year 2028
      recover 56 % with 100 % reuse during year 2029
      recover 70 % with 100 % reuse during years 2030 to onwards
    end substance
  end application

end policy


start simulations

  simulate "Business as Usual" from years 2025 to 2035

  simulate "Manufacturing Prohibition" using "Manufacturing Prohibition" from years 2025 to 2035

  simulate "Equipment Import Ban" using "Equipment Import Ban" from years 2025 to 2035

  simulate "Recovery and Recycling" using "Recovery and Recycling" from years 2025 to 2035

  simulate "Combined Policies" 
    using "Manufacturing Prohibition" 
    then "Equipment Import Ban" 
    then "Recovery and Recycling" 
  from years 2025 to 2035

end simulations
```
