# Tutorial 9: Global Warming Potential Refrigerant Comparison

Demonstrating GWP impact reduction through HFC-134a to R-600a substitution.

## Motivation

Global Warming Potential (GWP) values represent how much a substance contributes to climate change compared to CO2. For example, consider HFC-134a with a GWP of 1,430 kgCO2e / kg versus R-600a which has only 3. This means that each kilogram of HFC-134a has the same climate impact as 477 kilograms of R-600a.

Previous tutorials included investigation of modeling these values but this section takes a closer look at specifying tCO2e for substances, modeling both with and without different policies. More specifically, we'll look at a refrigerant substitution policy that gradually replaces HFC-134a with R-600a in domestic refrigeration. This simulation can track this transition as it dramatically reduces overall climate impact (measured in tCO2e) even when total refrigerant consumption remains similar.

Note that instructions are a bit more brief under the assumption that the earlier tutorials have been completed.

## Setting Up the Business-as-Usual Scenario

In this tutorial, we will move away from our ABC Country case study. If you are continuing from [Tutorial 8](/guide/md/tutorial_08.md), go ahead and click **New File** at the top of the web editor. This will give you a fresh simulation.

First, let's create our baseline scenario with both HFC-134a and R-600a refrigerants in domestic refrigeration. We'll start with HFC-134a as the dominant refrigerant and minimal R-600a consumption.

**Note:** In this feature specific tutorial, we will start from scratch each time. Click **Save File** to save your current work and then click **New File** to start a simulation.

**Step 1: Create the Domestic Refrigeration application**
- Click **Add Application**
- Name it "Domestic Refrigeration"
- Click **Finish**

**Step 2: Add HFC-134a substance**
- In your Domestic Refrigeration application, click **Add Consumption**
- On the **General** tab:
  - Name the substance "HFC-134a" without quotes.
  - Use Domestic Refrigeration as the **Application**.
  - Set **GHG equivalency** to 1430 kgCO2e/kg.
  - Leave **Annual energy consumption** alone for now.
  - Enable **domestic** manufacture.
- On the **Equipment** tab:
  - We will leave **Equipment type** alone for now.
  - Set **initial charge** to 0.15 kg/unit for domestic.
  - Set **annual retirement** rate to 5% each year.
- On the **Servicing** tab:
  - Set **recharge** to 10% with 0.15 kg/unit in all years
- On the **Set** tab:
  - Set **prior equipment** to 1,000,000.0 units in year 2025.
  - Set **domestic** manufacture to 20 mt in year 2025.
- Click **Finish**

**Step 3: Add R-600a substance**
- Again in the Domestic Refrigeration application, **Add Consumption** for R-600a with similar equipment properties except different GWP.
- On the **General** tab:
  - Name the substance "R-600a" without quotes.
  - Use Domestic Refrigeration as the **Application**.
  - Set **GHG equivalency** to 3 kgCO2e/kg.
  - Leave **Annual energy consumption** alone for now.
  - Enable **domestic** manufacture.
- On the **Equipment** tab:
  - We will leave **Equipment type** alone for now.
  - Set **initial charge** to 0.15 kg/unit for domestic.
  - Set **annual retirement** rate to 5% each year.
- On the **Servicing** tab:
  - Set **recharge** to 10% with 0.15 kg/unit in all years.
- On the **Set** tab:
  - Set **prior equipment** to 50,000.0 units in year 2025.
  - Set **domestic** manufacture to 1 mt in year 2025.
- Click **Finish**

**Step 4: Create baseline simulation**
- Click **Add Simulation**.
- Name it "BAU" without quotes.
- Set duration from **years 2025 to 2035**.
- Click **Finish**

You should now see your baseline simulation running.

**Step 5: review business as usual**
- Select **Bank**.
- Ensure **total** and **million units** (as in units of equipment) are selected.
- Select **Equipment**.
- Ensure **All** is selected.

This view shows HFC-134a as the dominant refrigerant.

## Adding the GWP Reduction Policy

Now let's create a policy that gradually replaces HFC-134a consumption with R-600a. This will demonstrate how a substance substitution policy can reduce overall climate impact.

**Step 1: Create the substitution policy**
- Click **Add Policy**.
- Name it "Replacement" without quotes.
- Select **Domestic Refrigeration** as the application.
- Select **HFC-134a** as the substance.

**Step 2: Configure the replacement mechanism**
- Go to the **Replace** tab.
- **Add Replacement** of 10% of sales.
- Indicate that it should be **replaced with** R-600a.
- Set timing to **2028 to onwards.**
- Click **Finish** to finish the policy.

This policy will progressively reduce HFC-134a consumption by 10% each year starting in 2028, with that demand being met by R-600a instead. Over time, this creates a significant shift in the refrigerant mix while maintaining overall service levels.

## Creating the Simulation

Now let's create a simulation to compare the policy scenario with our business-as-usual baseline.

- Click **Add Simulation**.
- Name it "Replacement" without quotes.
- Check the **Replacement** policy checkbox.
- Set duration from **years 2025 to 2035**.
- Click **Finish**.

You should now see both your **BAU** and **Replacement** scenarios displayed side by side in the results panel.

## Results

Let's examine how the substitution policy affects both substance consumption and climate emissions:

**Emissions Changes**
- Compare simulations by selecting **Simulations** and then selecting **All** in the simulations panel.
- Select the **Emissions** radio button.
- Click **configure custom** and combine **recharge** and **end of life**.

The dramatic emissions reduction demonstrates the power of GWP-focused policies. Each kilogram of HFC-134a replaced with R-600a eliminates approximately 1,427 kg of CO2-equivalent emissions. This shows how substance choice can be impactful for climate. However, before concluding, let's also confirm that, despite this drop in GWP, the overall amount of substance consumed is the same.

**Consumption Changes**

- Ensure you are comparing simulations by selecting **Simulations** and then selecting **All** in the simulations panel.
- Select the **Consumption** radio button.
- Select **domestic** (both substances are domestic only).
- Ensure **mt/ year** is selected.

Indeed, despite the reduction in GWP, the overall consumption remains unchanged.

## Conclusion

You've successfully modeled a Global Warming Potential-focused refrigerant substitution policy! This tutorial demonstrated:

- **GWP significance**: How refrigerant choice dramatically affects climate impact independent of consumption volumes.
- **Substitution policies**: Using "replace X% of sales with substance" to model market transitions.
- **Climate effectiveness**: How targeting high-GWP substances can achieve large emissions reductions.
- **Comparative analysis**: Evaluating policy impacts against business-as-usual baselines.

The GWP reduction policy shows how focusing on substance characteristics rather than just consumption volumes can maximize co-benefits.

**Download the completed tutorial**: [tutorial_09.qta](../tutorial_09.qta) - this contains the complete GWP comparison model with substitution policy

## Next Steps

[Tutorial 10](/guide/md/tutorial_10.md) will explore energy efficiency comparisons between different equipment models. You'll learn how energy consumption trade-offs interact with refrigerant choice to provide a more comprehensive environmental impact assessment.

[Previous: Tutorial 8](/guide/md/tutorial_08.md) |
[Return to Guide Index](/guide/md/index.md) |
[Next: Tutorial 10](/guide/md/tutorial_10.md)

---

*This tutorial is part of the Feature-Specific series demonstrating specialized aspects of Montreal Protocol policy modeling using Kigali Sim.*
