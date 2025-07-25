# Tutorial 4: Growth and Trade

Adding economic growth projections and import flows to create more nuanced business-as-usual scenarios.

## Motivation

Let's say ABC has economic growth forecasts and data on equipment imports. In this tutorial, we will add growth patterns and import flows to our multi-application model from Tutorial 3, creating a more nuanced business-as-usual baseline to help aid policy analysis. In addition to adding more sophistication to our model, this also demonstrates use of multiple sales streams.

## Adding Imports for HFC-134a

Before we add in growth rates, let's consider trade. Specifically, for brevity, let's have just one substance with imports.

Let's say that ABC imports some but not all of their HFC-134a. Therefore, for **Domestic Refrigeration**, modify your **HFC-134a** consumption record:

- **General** tab: **Enable "Import"** (in addition to existing manufacture)
- **Equipment** tab: Add **0.20 kg/unit for import**.
- **Set** tab: Change your existing 25 mt by reducing **domestic** to 13 mt in year 2025 and adding **import** of 11 mt in year 2025

Our tutorial later will expand this further but this gives us a good starting point.

(tutorial04_01.gif, alt text: animated gif showing the addition of imports to HFC-134a)

## First Economic Growth

In addition to trade, let's also add in economic growth. For example, these projections might come from industry surveys or from outside modeling efforts. Let's start with HFC-134a:

 - Click **edit** for HFC-134a
 - Go to **Change** tab, add a change record of +6% from 2025 - 2030 for all sales as this will apply to both imports and domestic manufacturing.
 - Add a change record of +4% from 2031 - 2035 for all sales.

(tutorial04_02.gif, alt text: animated gif showing the addition of change commands in HFC-134a)

## Expanding the Growth

Let's continue by applying these growth rates using the 
**Change** tab for the consumption records. Below is a table of everything that should be present after you are done. However, remember that you already did HFC-134a!

| Application            | Substance | Domestic Growth                  | Import Growth                    |
| ---------------------- | --------- | -------------------------------- | -------------------------------- |
| Domestic Refrigeration | HFC-134a  | +6% (2025-2030), +4% (2031-2035) | +6% (2025-2030), +4% (2031-2035) |
| Domestic Refrigeration | R-600a    | +5% (2025-2030), +3% (2031-2035) | N/A (domestic only)              |
| Domestic AC            | HFC-32    | +10% (2025-2035)                 | N/A (domestic only)              |
| Domestic AC            | R-410A    | +6% (2025-2035)                  | N/A (domestic only)              |

You can go to **Change** tab and add changes for domestic manufacture stream or all sales, both have the same effect in this case.

## Results

Note that the drop down menu under the Consumption radio button which can flip between domestic and imports. Just as we did with the custom metric under emissions before, we can click configure custom to combine imports and domestic together.

Does the imports part of HFC-134a seem small? It's important to note that, by default, initial charge for new equipment is attributed to the exporting country. We can temporarily change this behavior to get a fuller picture of our global consumption by checking **Attribute initial charge to importer**. However, to stay consistent with Montreal Protocol standards, it is best to leave this off in most cases.

Zooming out, we should see the acceleration in HFC-134a and HFC-32. With the **Emissions** radio button, things still remain quite dominated by HFC-134a. In contrast, HFC-32 is dominant when selecting the **Consumption** radio button as that 10% increase compounds over time.

## Conclusion

You now have ABC Country's realistic business-as-usual scenario incorporating economic growth, trade flows, and technology transitions. This foundation shows how consumption evolves without intervention. Later, we will try out different policies on this of this baseline.

## Next Steps

**Tutorial 5** will start to add new sophistication by modeling policies.

---

_This tutorial is part of the ABC Country case study series demonstrating progressive HFC policy analysis using Kigali Sim._