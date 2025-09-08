# Tutorial 12: Equipment Energy Efficiency Comparison

Demonstrating energy consumption reduction through equipment efficiency improvements.

## Contents

- [Motivation](#motivation)
- [Setting Up the Business-as-Usual Scenario](#setting-up-the-business-as-usual-scenario)
- [Adding the Energy Efficiency Policy](#adding-the-energy-efficiency-policy)
- [Creating the Simulation](#creating-the-simulation)
- [Results](#results)
- [Conclusion](#conclusion)
- [Next Steps](#next-steps)

## Motivation

Beyond refrigerant choice, equipment energy efficiency plays a crucial role in the overall environmental impact of cooling systems. Energy-efficient equipment consumes less electricity, reducing both operational costs and indirect emissions from power generation. In this tutorial, we'll model an energy efficiency policy that gradually replaces high-energy equipment with low-energy alternatives. To narrow our focus to these effects, we'll examine how this transition can significantly reduce overall energy consumption (measured in kWh) while maintaining the same refrigerant type and cooling capacity.

## Setting Up the Business-as-Usual Scenario

Let's create our baseline scenario with both high-energy and low-energy models of the same HFC-134a refrigeration equipment. We'll start with high-energy models dominating the market and minimal low-energy model adoption.

**Step 1: Create the Domestic Refrigeration application**
- Click **Add Application**
- Name it "Domestic Refrigeration"
- Click **Save**

**Step 2: Add HFC-134a High Energy equipment model**
- Click **Add Consumption**
- Name it "HFC-134a" and use "Domestic Refrigeration"
- On the **General** tab:
  - Set **GWP** to 1430 kgCO2e/kg
  - Enable **domestic** manufacture
  - Set **annual energy consumption** to 500 kwh/unit, referring to annual amortized consumption
- On the **Equipment** tab:
  - Set **equipment type** as "high energy"
  - Set **initial charge** to 0.15 kg/unit for domestic manufacture
  - Set **annual retirement** to 5% each year
- On the **Servicing** tab:
  - Set **recharge** to 10% with 0.15 kg/unit during all years
- On the **Set** tab:
  - Set **prior equipment** to 1,000,000 units in year 2025
  - Set **domestic** manufacture to 20 mt in year 2025
- Click **Save**

**Step 3: Add HFC-134a Low Energy equipment model**
- Click **Add Consumption**
- Name it "HFC-134a" and use "Domestic Refrigeration"
- On the **General** tab:
  - Set **GWP** to 1430 kgCO2e/kg
  - Set **annual energy consumption** to 350 kwh/unit, referring to annual amortized consumption
  - Enable **domestic** manufacture
- On the **Equipment** tab:
  - Set **equipment type** as "low energy"
  - Set **initial charge** to 0.15 kg/unit for domestic manufacture
  - Set **annual retirement** to 5% each year
- On the **Servicing** tab:
  - Set **recharge** to 10% with 0.15 kg/unit during all years
- On the **Set** tab:
  - Set **prior equipment** to 1,000 units in year 2025
  - Set **domestic** manufacture to 10 mt in year 2025
- Click **Save**

**Step 4: Create baseline simulation**
- Click **Add Simulation**
- Name it "BAU"
- Set duration from **years 2025 to 2035**
- Click **Save**

You should now see your baseline simulation running, showing the high-energy model as the dominant equipment with much higher consumption volumes than the low-energy alternative. To see this, select the **Population** and **Equipment** radio buttons. With the specifics of the values we entered in given the production numbers and recharge demands, there is very slight decrease in high energy equipment over time and a gradual increase in low energy. However, overall population is increasing.

## Adding the Energy Efficiency Policy

Now let's create a policy that accelerates this replacement of high-energy equipment consumption with low-energy models. This will demonstrate how an equipment efficiency policy can reduce overall energy consumption without changing refrigerant type.

**Step 1: Create the efficiency policy**
- Click **Add Policy**
- Name it "Energy Efficiency"
- Select **Domestic Refrigeration** as the application
- Select **HFC-134a - high energy** as the substance

**Step 2: Configure the replacement mechanism**
- Go to the **Replace** tab within your HFC-134a High Energy policy configuration
- Click **Add Replace**
- Set to replace 20% of sales
- Indiciate that it is to be replaced with **HFC-134a - low energy**
- Set timing to **starting in 2028 to onwards**
- Click **Save** to finish the policy

This policy will progressively reduce high-energy equipment consumption relative to the BAU. Specifically, we change 10% of high energy sales each year starting in 2028 with that demand being met by low-energy equipment instead. This accelerates that change we saw earlier. However, before we can see the effects, we need to make an additional simulation.

## Creating the Simulation

Now let's create a simulation to compare the policy scenario with our business-as-usual baseline.

- Click **Add Simulation**
- Name it "Efficiency Policy"
- Check the **Energy Efficiency** policy checkbox
- Set duration from **years 2025 to 2035**
- Click **Save**

Kigali Sim is now simulating both options but we need to configure the visualizations to do a comparison.

## Results

Let's examine how the efficiency policy affects both equipment adoption and energy consumption:

**Equipment Comparison**
- Select the **Population** radio button
- Ensure **total** and **million units** is selected
- Select **Simulations** radio
- Select **All** under the simulations list

Compare the **BAU** and **Efficiency Policy** simulations and notice how overall number of units of equipment remains effectively the same. Next, let's consider the energy consumption

**Energy Comparison**
- Keep **Population** selected with **total**
- Select **GWh / year** instead of units of equipment
- Ensure **Simulations** is selected with **All** to compare the two scenarios
- To better see the results, change **Abslute Value** to **Relative to BAU**

Taken together, even though total equipment population and refrigerant consumption stay roughly constant, the overall kWh consumption drops substantially. This is because we're replacing equipment that consumes 500 kWh/year with units that consume only 350 kWh/year - a 30% efficiency improvement.

## Conclusion

You've successfully modeled an equipment energy efficiency policy! This tutorial demonstrated:

- **Equipment differentiation**: How the same refrigerant can have different environmental impacts based on equipment efficiency
- **Energy-focused policies**: Using "replace X% of sales with substance" to model efficiency transitions
- **Indirect impact reduction**: How energy efficiency reduces environmental impact beyond direct refrigerant emissions
- **Market transformation**: Modeling progressive adoption of efficient technologies relative to BAU

**Download the completed tutorial**: [tutorial_12.qta](tutorial_12.qta) - this contains the complete energy efficiency comparison model with equipment transition policy

## Next Steps

You've now completed both Feature-Specific tutorials! You've learned how to model GWP-focused refrigerant substitution (Tutorial 11) and energy efficiency equipment transitions (Tutorial 12). These tutorials complement each other to provide a comprehensive understanding of environmental impact assessment in cooling systems.

For more complex scenarios, consider combining both approaches: policies that simultaneously transition to lower-GWP refrigerants AND more energy-efficient equipment. Return to the [guide index](/guide/) to explore other tutorials or the QubecTalk reference for advanced modeling techniques.

[Previous: Tutorial 11](/guide/tutorial_11.html) | [Guide Index](/guide/)

---

_This tutorial is part of the Feature-Specific series demonstrating specialized aspects of Montreal Protocol policy modeling using Kigali Sim._