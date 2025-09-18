# Tutorial 6: Recycling

Exploring secondary material through recycling programs.

## Motivation

Building on ABC Country's sales permitting system from Tutorial 5, we'll now add a recovery and recycling program targeting the same HFC-134a domestic refrigeration sector. This demonstrates how recycling policies work and explores the introduction of secondary substance in addition to the virgin substance we've been manipulating so far.

## Adding the Recycling Policy

Let's create ABC's recovery program:

- Click **Add Policy** and name it "Domestic Recycling"
- Select **Domestic Refrigeration** as the application and **HFC-134a** as the substance
- Go to the **Recycle** tab and click **Add Recycling**
- Set **20% recovery with 90% reuse**.
- Let's have this start at **recharge** (at the point of servicing).
- Leave **induced demand at 100%** (the default).
- Start in **year 2027**.
- Click **Finish**

(tutorial06_01.gif, alt text: animated gif showing how to add the recycling policy)

## Add Simulations

Add both a Recycling simulation with just Domestic Recycling selected. Then, add a combined simulation with both Sales Permit and Domestic Recycling included. Again, use years 2025 to 2035.

(tutorial06_02.gif, alt text: animated gif showing how to add recycling simulations)

## Results

Piecing together what is going on with multiple policies can take a little work but let's dig in. For this, go ahead and enable **Attribute initial charge to importer** just to make it easier to reason about how volumes are shifting.

First, using the **Consumption** radio button, recycling certainly cuts down on overall virgin substance, either imported or domestic. This is unlike the caps we introduced previously which simply move that consumption between substances. Even so, the combined policies encounter a small snag: moving more substance to R-600a means there's less HFC-134a to recycle late in the simulation. In a later tutorial, we will address this by adding a R-600a recycling program!

Next, the **Emissions** radio button tells a more interesting story where recycling from servicing fails to keep up with demand so, eventually, BAU and the recycling scenario trend together. This is where the combination of policies may be effective. Once we add in the cap as well, the two complement each other to achieve a higher impact than either on their own.

## Conclusion

You've successfully implemented ABC Country's comprehensive HFC-134a strategy combining permitting and recycling policies. This tutorial demonstrated how recycling appears as an alternative supply source and complements demand-side restrictions for maximum policy effectiveness.

## Next Steps

**Tutorial 7** will transition from the UI-based interface to direct QubecTalk programming. You'll discover that you've been programming all along and learn to implement advanced multi-substance policies only possible through direct coding. QubecTalk also makes it easier to modify simulations faster. For example, this can help with switching from tonnes of substance to equipment unit counts. While technically possible in the UI, we will find in Tutorial 8 that those kinds of changes may be much more efficient through code.

---

_This tutorial is part of the ABC Country case study series demonstrating progressive HFC policy analysis using Kigali Sim._