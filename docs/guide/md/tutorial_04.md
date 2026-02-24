# Tutorial 4: Growth and Trade

Adding economic growth projections and import flows to create more nuanced business-as-usual scenarios.

**Note that this is the conventional version of this tutorial.** An AI-assisted alternative is available at [Tutorial 4a](/guide/tutorial_04a.html).

## Motivation

Let's say ABC has economic growth forecasts and data on equipment imports. In this tutorial, we will add growth patterns and import flows to our multi-application model from Tutorial 3, creating a more nuanced business-as-usual baseline to help aid policy analysis. In addition to adding more sophistication to our model, this also demonstrates use of multiple sales streams.

## Adding Imports for HFC-134a

Before we add in growth rates, let's consider trade. Specifically, for brevity, let's have just one substance with imports.

Let's say that ABC imports some but not all of their HFC-134a. Therefore, for **Domestic Refrigeration**, modify your **HFC-134a** consumption record:

- **General** tab: **Enable "Import"** (in addition to existing manufacture)
- **Equipment** tab: Add **0.20 kg/unit for import**.
- **Set** tab: Change your existing 25 mt by reducing **domestic** to 13 mt in year 2025 and adding **import** of 11 mt in year 2025

Our tutorial later will expand this further but this gives us a good starting point.

<video src="/webm/tutorial_04_01.webm" autoplay loop muted playsinline style="width: 500px; border: 2px solid #505050; border-radius: 3px;">Your browser does not support the video tag. Please upgrade to a modern browser.</video>

## First Economic Growth

In addition to trade, let's also add in economic growth. For example, these projections might come from industry surveys or from outside modeling efforts. Let's start with HFC-134a:

- Click **edit** for HFC-134a if you already clicked finish.
- Go to **Change** tab, add a change record of **+6%** (or equivalently **+6% current**) from 2025 to 2030 for all sales as this will apply to all consumption in the country. In our case, both imports and domestic manufacturing.
- Add a change record of **+4%** from 2031 to 2035 for all sales.

> **More about percentages**: Note: You can use `% current` instead of `%` for growth rates to make it explicit that the percentage applies to the current year's value. The system supports three percentage formats: `%` (equivalent to `% current`) applies the percentage to the current year's value, `% current` explicitly applies to the current year's value, and `% prior year` applies the percentage to the previous year's value. This flexibility allows you to match your data source's reference year convention. This could come up in simulations with multiple change directives due to complex economic modeling.

> **More about the sales stream**: This refers to all consumption in the country. So, having Kigali Sim set / change sales impacts overall consumption. More specifically, it includes domestic and import but excludes export. When applying changes through the sales keyword, Kigali Sim will try to keep the ratio between domestic and import the same for the substance.
>
> Note that this may also include "secondary" substance if recycling is active. That said, indicated by the recover command, recycling capacity is assumed to be limited. So, domestic and import will be modified to satisfy a set or change command after taking the unchanged recycling stream into account. However, using sales with cap/floor (like for permitting) places lower or upper limits on all consumption including recycling. For virgin only, replace sales with individual commands on domestic and import. This will exclude secondary production.

<video src="/webm/tutorial_04_02.webm" autoplay loop muted playsinline style="width: 500px; border: 2px solid #505050; border-radius: 3px;">Your browser does not support the video tag. Please upgrade to a modern browser.</video>

## Expanding the Growth

Let's continue by applying these growth rates using the **Change** tab for the consumption records. Below is a table of everything that should be present after you are done. However, remember that you already did HFC-134a!

| Application | Substance | Domestic Growth | Import Growth |
|-------------|-----------|-----------------|---------------|
| Domestic Refrigeration | HFC-134a | +6% (2025-2030), +4% (2031-2035) | +6% (2025-2030), +4% (2031-2035) |
| Domestic Refrigeration | R-600a | +5% (2025-2030), +3% (2031-2035) | N/A (domestic only) |
| Domestic AC | HFC-32 | +10% (2025-2035) | N/A (domestic only) |
| Domestic AC | R-410A | +6% (2025-2035) | N/A (domestic only) |

You can go to **Change** tab and add changes for domestic manufacture stream or all sales, both have the same effect in this case.

<video src="/webm/tutorial_04_03.webm" autoplay loop muted playsinline style="width: 500px; border: 2px solid #505050; border-radius: 3px;">Your browser does not support the video tag. Please upgrade to a modern browser.</video>

## Results

Note that the drop down menu under the Consumption radio button which can flip between domestic and imports. Just as we did with the custom metric under emissions before, we can click configure custom to combine imports and domestic together.

Does the imports part of HFC-134a seem small? It's important to note that, by default, initial charge for new equipment is attributed to the exporting country. We can temporarily change this behavior to get a fuller picture of our global consumption by checking **Attribute initial charge to importer**. However, to stay consistent with Montreal Protocol standards, uncheck it to review treaty-aligned numbers. When authoring simulations, often it helps to consider both perspectives.

Zooming out, we should see the acceleration in HFC-134a and HFC-32. With the **Emissions** radio button, things still remain quite dominated by HFC-134a. In contrast, the two are closer when selecting the **Consumption** radio button as that 10% increase compounds over time for HFC-32.

<video src="/webm/tutorial_04_04.webm" autoplay loop muted playsinline style="width: 500px; border: 2px solid #505050; border-radius: 3px;">Your browser does not support the video tag. Please upgrade to a modern browser.</video>

## Conclusion

You now have ABC Country's realistic business-as-usual scenario incorporating economic growth, trade flows, and technology transitions. This foundation shows how consumption evolves without intervention. Later, we will try out different policies on this of this baseline.

Finally, for those with **local assembly**, domestic can be used to model in-country initial charge. In other words, for substance which is equipment and then initial charged within the country, you can move the "sales" of that substance from import to domestic and the correct trade attribution will apply.

**Download the completed tutorial** result at [tutorial_04.qta](tutorial_04.qta) which contains the complete model with economic growth and trade flows. This differs from the [prior tutorial result](tutorial_03.qta) in that it now has the consumption change over time.

## Next Steps

[Tutorial 5](/guide/tutorial_05.html) will start to add new sophistication by modeling policies. You'll learn to create sales permitting systems that progressively reduce HFC consumption while managing market displacement to lower-GWP alternatives.

[Previous: Tutorial 3](/guide/tutorial_03.html) | [Return to Guide Index](/guide) | [Next: Tutorial 5](/guide/tutorial_05.html)

---

_This tutorial is part of the ABC Country case study series demonstrating progressive HFC policy analysis using Kigali Sim._
