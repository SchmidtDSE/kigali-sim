# Tutorial 9: Global Warming Potential Refrigerant Comparison

Demonstrating GWP impact reduction through HFC-134a to R-600a substitution.

## Contents

- [Motivation](#motivation)
- [Setting Up the Business-as-Usual Scenario](#setting-up-the-business-as-usual-scenario)
- [Adding the GWP Reduction Policy](#adding-the-gwp-reduction-policy)
- [Creating the Simulation](#creating-the-simulation)
- [Results](#results)
- [Conclusion](#conclusion)
- [Next Steps](#next-steps)

## Motivation

Global Warming Potential (GWP) values represent how much a substance contributes to climate change compared to CO2. Previous tutorials included investigation of these options but this section takes a closer look at specifying tCO2e for substances and putting policies on top.

More specifically, different refrigerants have vastly different climate impacts. For this tutorial, consider HFC-134a with a GWP of 1,430 versus R-600a (isobutane), which has a GWP of only 3. This means that each kilogram of HFC-134a has the same climate impact as 477 kilograms of R-600a!

In this exercise, we'll model a refrigerant substitution policy that gradually replaces HFC-134a with R-600a in domestic refrigeration. This simulation can track this transition as it dramatically reduces overall climate impact (measured in tCO2e) even when total refrigerant consumption remains similar.

## Setting Up the Business-as-Usual Scenario

First, let's create our baseline scenario with both HFC-134a and R-600a refrigerants in domestic refrigeration. We'll start with HFC-134a as the dominant refrigerant and minimal R-600a consumption.

**Step 1: Create the Domestic Refrigeration application**
- Click **Add Application**
- Name it "Domestic Refrigeration"
- Click **Finish**

**Step 2: Add HFC-134a substance**
- In your Domestic Refrigeration application, click **Add Consumption**
- On the **General** tab:
  - Name the substance "HFC-134a"
  - Enable **domestic** manufacture
  - Set **GHG equivalency** to 1430 kgCO2e/kg
- On the **Equipment** tab:
  - Set **initial charge** to 0.15 kg/unit for domestic
  - Set **annual retirement** rate to 5% each year
- On the **Servicing** tab:
  - Set **recharge** to 10% with 0.15 kg/unit in all years
- On the **Set** tab:
  - Set **prior equipment** to 1,000,000.0 units in year 2025
  - Set **domestic** manufacture to 20 mt in year 2025
- Click **Finish**

**Step 3: Add R-600a substance**
- Again in the Domestic Refrigeration application, **Add Consumption** for R-600a with similar equipment properties except different GWP.
- On the **General** tab:
  - Set **GHG equivalency** to 3 kgCO2e/kg
  - Enable **domestic** manufacture
- On the **Equipment** tab:
  - Set **initial charge** to 0.15 kg/unit for domestic
  - Set **annual retirement** rate to 5% each year
- On the **Servicing** tab:
  - Set **recharge** to 10% with 0.15 kg/unit in all years
- On the **Set** tab:
  - Set **prior equipment** to 50,000.0 units in year 2025
  - Set **domestic** manufacture to 1 mt in year 2025
- Click **Finish**

**Step 4: Create baseline simulation**
- Click **Add Simulation**
- Name it "BAU"
- Set duration from **years 2025 to 2035**
- Leave all policies unchecked (this is our business-as-usual baseline)
- Click **Finish**

You should now see your baseline simulation running, showing HFC-134a as the dominant refrigerant with much higher consumption volumes than R-600a.

## Adding the GWP Reduction Policy

Now let's create a policy that gradually replaces HFC-134a consumption with R-600a. This will demonstrate how a substance substitution policy can reduce overall climate impact.

**Step 1: Create the substitution policy**
- Click **Add Policy**
- Name it "Replacement"
- Select **Domestic Refrigeration** as the application
- Select **HFC-134a** as the substance

**Step 2: Configure the replacement mechanism**
- Go to the **Replace** tab
- Target Domestic Refrigeration and HFC-134a
- **Add Replacement** of 10% of sales with R-600a
- Set timing to **each year during years 2028 to onwards**
- Click **Finish** to finish the policy

This policy will progressively reduce HFC-134a consumption by 10% each year starting in 2028, with that demand being met by R-600a instead. Over time, this creates a significant shift in the refrigerant mix while maintaining overall service levels.

## Creating the Simulation

Now let's create a simulation to compare the policy scenario with our business-as-usual baseline.

- Click **Add Simulation**
- Name it "Replacement"
- Check the **Replacement** policy checkbox
- Set duration from **years 2025 to 2035**
- Click **Finish**

You should now see both your **BAU** and **Replacement** scenarios displayed side by side in the results panel.

## Results

Let's examine how the substitution policy affects both substance consumption and climate emissions:

**Substance Consumption Changes**
- Select the **Consumption** radio button to see total consumption volumes
- Select **domestic** in the dropdown menu
- Select **kg / year** first to see that the amount of substance consumed is the same
- Change to **tCO2e / year** to see that, despite the same amount of substance, the tCO2e is much lower in the policy case

The dramatic emissions reduction demonstrates the power of GWP-focused policies. Each kilogram of HFC-134a replaced with R-600a eliminates approximately 1,427 kg of CO2-equivalent emissions (1,430 - 3 = 1,427). This shows how substance choice can be far more impactful for climate than overall consumption volume reductions.

## Conclusion

You've successfully modeled a Global Warming Potential-focused refrigerant substitution policy! This tutorial demonstrated:

- **GWP significance**: How refrigerant choice dramatically affects climate impact independent of consumption volumes
- **Substitution policies**: Using "replace X% of sales with substance" to model gradual market transitions
- **Climate effectiveness**: How targeting high-GWP substances can achieve large emissions reductions
- **Market realism**: Modeling progressive adoption rather than immediate switching
- **Comparative analysis**: Evaluating policy impacts against business-as-usual baselines

The GWP reduction policy shows how focusing on substance characteristics rather than just consumption volumes can maximize co-benefits.

**Download the completed tutorial**: [tutorial_09.qta](tutorial_09.qta) - this contains the complete GWP comparison model with substitution policy

## Next Steps

[Tutorial 10](/guide/tutorial_10.html) will explore energy efficiency comparisons between different equipment models. You'll learn how energy consumption trade-offs interact with refrigerant choice to provide a more comprehensive environmental impact assessment.

[Return to Guide Index](/guide) | [Next: Tutorial 10](/guide/tutorial_10.html)

[Previous: Tutorial 8](/guide/tutorial_08.html) | [Return to Guide Index](/guide) | [Next: Tutorial 10](/guide/tutorial_10.html)

---

_This tutorial is part of the Feature-Specific series demonstrating specialized aspects of Montreal Protocol policy modeling using Kigali Sim._