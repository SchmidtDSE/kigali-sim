# Tutorial 6: Recycling

Exploring secondary material through recycling programs.

**Note that this is the conventional version of this tutorial.** An AI-assisted alternative is available at [Tutorial 6a](/guide/md/tutorial_06a.md).

## Motivation

Building on ABC Country's sales permitting system from Tutorial 5, we'll now add a recovery and recycling program targeting the same HFC-134a domestic refrigeration sector. This demonstrates how recycling policies work and explores the introduction of secondary substance in addition to the virgin substance we've been manipulating so far.

## Adding the Recycling Policy

Let's create ABC's recovery program:

- Click **Add Policy** and name it "Domestic Recycling"
- Select **Domestic Refrigeration** as the application and **HFC-134a** as the substance
- Go to the **Recycle** tab and click **Add Recycling**
- Set **20% recovery with 90% reuse**.
- Let's have this happen at **recharge** (at the point of servicing).
- Start in **year 2027**.
- Click **Finish**

Please leave **induced demand at 100%** (the default). We will revisit this in just a minute.

## Add Simulations

Add both a Recycling simulation with just Domestic Recycling selected. Then, add a combined simulation with both Sales Permit and Domestic Recycling included. Again, use years 2025 to 2035.

Note: The domestic recycling is applied prior to the sales permit (should appear in the checkbox list of policies first). If not, please use the specify policy order link.

> **More about policy order and interaction**: Sometimes the order in which policies apply can matter. For example, consider a policy which reduces consumption by 5% and a separate policy that imposes a cap. Should the reduction count towards the cap or be applied after the cap has taken place? It depends on your policy assumptions. If the cap is a permit and the change is an educational policy, one may wish to have the percent change applied prior to cap. This is why having that policy further up in the list is important.
>
> In this case, we have recycling applied with possible changes to amount of virgin consumption. Then, the cap is applied, displacing any of the remaining virgin consumption over the limit to R-600a. Note that the combined scenario has lower consumption than permit alone because we set our permit to cap all sales which includes secondary. We can replace sales with individual caps on domestic and import if we want to target virgin only.

## Results

Piecing together what is going on with multiple policies can take a little work but let's dig in. For this, go ahead and disable **Attribute initial charge to importer** just to make it easier to reason about how volumes are shifting in the **Consumption** view. To get a closer look, select **HFC-134a** under substances.

Next, the **Emissions** radio button tells an interesting story where recycling captures some of those emissions. Also, this is where the combination of policies may be effective. Once we add in the cap as well, the two complement each other to achieve a higher impact than either on their own.

> **More on induced demand**: Note that we said 100% induced demand. This means that virgin consumption does not on its own decrease because recycling is present. This is common in recycling programs where recycling does not 1:1 offset virgin production. Determining the correct rate of displacement (100% - induced demand) can require a bit of work.
>
> For comparison, try setting the induced demand to **0%** instead. With 0% induced demand, every kg of recycled substance means one less kg of virgin substance produced, a direct 1:1 offset. First, check the **Bank** view with million units of equipment selected. Notice that BAU and recycling scenarios now have the same amount of equipment, since recycling no longer drives additional demand. Next, switch to **Consumption** and observe that virgin domestic production is now lower in the recycling scenario. Finally, return to **Emissions** to see that they remain lower with recycling in either case, though the mechanism differs between 0% and 100% induced demand. For the purposes of the later tutorials, let's leave induced demand at 100%, but remember that you can try out different values along the way.
>
> All that in mind, induced demand is likely neither 0% nor 100% in practice. However, this ambiguity largely goes away when we add the cap policy. When both are employed together, we see a stronger response in terms of emissions. In this case though, it is important to have recycling followed by permit. This is because we want recycling effects calculated first. After all, if there's enough servicing activity, demand is higher than it would be without recycling! Then, the cap policy can reduce whatever virgin sales were after recycling to the mandated levels, offsetting any induced demand over the cap into R-600a instead of leaving it in HFC-134a.

> **More about terminology / bank**: **Bank** refers to all of the equipment and substance (not yet emitted to the atmosphere) in a country. Some call this reservoir. This can be expressed in terms of number of units of equipment, substance volumes, and tCO2e. Kigali Sim uses Bank to refer to the full population of equipment and substance in the country at any given time. This is also where metrics derived from operating characteristics (like energy consumption) are reported.

## Conclusion

You've successfully implemented ABC Country's comprehensive HFC-134a strategy combining permitting and recycling policies. This tutorial demonstrated how recycling appears as an alternative supply source and complements demand-side restrictions for maximum policy effectiveness.

**Download the completed tutorial** result at [tutorial_06.qta](../tutorial_06.qta) which contains the complete model with combined permitting and recycling policies. It differs from the [prior tutorial result](../tutorial_05.qta) in that it adds recycling, the results of which are most apparent when looking at emissions.

## Next Steps

[Tutorial 7](/guide/md/tutorial_07.md) will transition from the UI-based interface to direct QubecTalk programming. You'll discover that you've been programming all along and learn to implement advanced multi-substance policies only possible through direct coding. QubecTalk also makes it easier to modify simulations faster. For example, this can help with switching from tonnes of substance to equipment unit counts. While technically possible in the UI, we will find in Tutorial 8 that those kinds of changes may be much more efficient through code.

[Previous: Tutorial 5](/guide/md/tutorial_05.md) | [Return to Guide Index](/guide/md/index.md) | [Next: Tutorial 7](/guide/md/tutorial_07.md)

---

_This tutorial is part of the ABC Country case study series demonstrating progressive HFC policy analysis using Kigali Sim._

---

[View HTML version](../tutorial_06.html)
