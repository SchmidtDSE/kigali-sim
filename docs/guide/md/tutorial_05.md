# Tutorial 5: Simple Policies

ABC Country's first policy intervention using a sales permitting system.

## Motivation

Now that ABC Country has a solid business-as-usual baseline, let's explore policy interventions. In this tutorial, we'll add a sales permitting system that progressively reduces HFC-134a consumption in domestic refrigeration. However, that demand will go away and, instead, will shift to the lower-GWP R-600a alternative.

## Adding R-600a Imports

Before implementing our policy, let's first add import flows for R-600a to match the supply chain pattern we established for HFC-134a in Tutorial 4. This will make our policy analysis more realistic.

For **Domestic Refrigeration**, modify your **R-600a** consumption record:

- **General** tab: **Enable "Import"** (in addition to existing manufacture)
- **Equipment** tab: Add **0.07 kg/unit for import** (same as manufacture)
- **Set** tab: Split your existing 2 mt by reducing **domestic** to 1 mt in year 2025 and adding **import** of 1 mt in year 2025.
- **Change** tab: Ensure your selection uses **all sales** which, as a reminder, refers to all consumption (in our case, both domestic and imports).

This gives R-600a a similar import/domestic split as HFC-134a, making the displacement mechanism clearer when we implement the policy.

(tutorial05_01.gif, alt text: Animated gif showing updates to R-600a)

## Adding the Sales Permitting Policy

Now let's create ABC's permitting system that targets HFC-134a in domestic refrigeration, where displaced demand will shift to R-600a.

- Click **Add Policy**. Name it "Sales Permit". Then, select **Domestic Refrigeration** as the application and **HFC-134a** as the substance.
- Go to the **Limit** tab within your HFC-134a policy configuration and click **Add Limit**. Then, **Cap** all sales to 85% during **years 2029 to 2034**.
- **Displacing** means that the demand lost from HFC-134a is then sent to another stream or substance. In this case, let's have it go to **R-600a** instead.
- Click **Add Limit** again. Set **Cap** on sales to 0 kg displacing R-600a during **years 2035 to onwards**. This will conclude the phase-out.
- Click **Save** to finish the policy

(tutorial05_02.gif, alt text: Animated gif showing how to add a sales permit policy)

## Creating the Permit Simulation

Now let's create a simulation to see how the permitting policy compares to business-as-usual.

- Click **Add Simulation**.
- Name it "Permit"
- Check the **Sales Permit** policy checkbox
- Set duration from **years 2025 to 2035**
- Click **Save**

The simulation will now show both your original **BAU** and new **Permit** scenarios side by side.

(tutorial05_03.gif, alt text: Animated gif showing how to add a sales permit simulation)

## Results

Let's observe how:

- **HFC-134a consumption** starts dropping in 2027 and goes to zero in 2035
- **R-600a consumption** increases to compensate for the HFC-134a reduction
- **Emissions** see a drop or a leveling off.

Specifically, under the **Substances** view with Permit selected under the **Simulations** radio button, you can see how displacement works: as HFC-134a is restricted, R-600a scales up proportionally. Since R-600a now has both domestic and import sources, the displaced demand gets distributed across both supply chains.

(tutorial05_04.gif, alt text: Animated gif showing visualization configuration to observe HFC-134a and R-600a)

All that said, remember that without **Attribute initial charge to importer** checked, the visualizations show only the substance flows for servicing imported equipment, not their initial charge. Before finishing up, let's just quickly check that box once more and:

 - Select the **Simulations** radio button
 - Select All simulations
 - Select the **Consumption** radio button
 - Use the **configure custom** link to combine imports and domestic production.

The lines almost perfectly overlap when **Attribute initial charge to importer** is checked but are a little further off without it, especially in 2035. This is because, as the import / domestic ratios (and their growth rates) are slightly different for R-600a and HFC-134a, the trade attribution causes the numbers to shift around a bit even though the total global-level picture is essentially balanced. That said, in any case, there are very minor differences expected due to servicing differences between equipment.

(tutorial05_05.gif, alt text: Animated gif showing visualization configuration to observe alternative trade attribution.)

## Conclusion

You've successfully implemented ABC Country's first policy intervention! The sales permitting system demonstrates:

- **Targeted approach**: Here we focused on the highest-impact substance (HFC-134a) first.
- **Progressive reduction** then complete phase-out.
- **Market mechanisms**: Displacement ensures demand shifts to R-600a rather than disappearing
- **Supply chain realism**: R-600a displacement works across both domestic and import channels
- **Climate benefits**: Emissions reduction through GWP-focused targeting (1,430 → 3 tCO2e/kg)

The permitting system shows how a relatively simple policy can achieve significant impacts. This is also a very simple model but we are already incorporating socioeconomic projections and substance interactions.

## Next Steps

**Tutorial 6** will add a recovery and recycling program targeting the same HFC-134a domestic refrigeration sector. You'll learn how recycling policies complement demand-side restrictions by providing alternative supply sources, and explore how multiple policies work together to achieve comprehensive overall outcomes.

---

_This tutorial is part of the ABC Country case study series demonstrating progressive HFC policy analysis using Kigali Sim._