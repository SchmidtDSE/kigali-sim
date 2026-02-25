# Tutorial 6a: Recycling with AI
Exploring secondary material through recycling programs, aided by AI to help design the intervention.

**Note that this is the AI version of this tutorial.** You can find a version of this tutorial without AI at [Tutorial 6](tutorial_06.md).

## Motivation
Building on ABC Country's sales permitting system from Tutorial 5, we'll now add a recovery and recycling program targeting the same HFC-134a domestic refrigeration sector. This demonstrates how recycling policies work and explores the introduction of secondary substance in addition to the virgin substance we've been manipulating so far.

Recycling can be complex! Let's use AI to help us walk through the design of the intervention.

## Preparing for AI
If you did the AI version of Tutorial 3 or 4, you probably are already familiar with this process. However, again, most AI assistants can help with Kigali Sim using a standard called [llms.txt](https://llmstxt.org/).

In this case, let's create a new chat session. Then, tell it to look up information about Kigali Sim through a message like this:

```
Hello! I would like help with building a Kigali Sim simulation. Please read https://kigalisim.org/llms-full.txt?v=20260128 to learn more. Please stick to only features compatible with the UI editor.
```

Need a little cheat? [Download the Tutorial 5](../tutorial_05.qta) file here. Also, for more capable assistants like Claude, this is enough. However, some assistants cannot access the full internet or won't know how to work with this kind of file out of the box. If your assistant is having issues, instead, attach the [Kigali Sim llms-full.txt](https://kigalisim.org/llms-full.txt?v=20260128) file as an attachment!

When your assistant is ready, next give it your Tutorial 5 file. This can be done by going to the Save File button at the top of the Kigali Sim application and adding it to the chat as an attachment with a message like this:

```
Great! The attached is the simulation I started working on.
```

**Note**: More details and troubleshooting steps specific to individual AI assistants are available in [Tutorial 11](tutorial_11.md).

## Asking about Recycling
There's quite a few parameters to specify for recycling. So, let's actually start this time by asking the AI assistant for help. Here's an example prompt:

```
I want to add a recycling intervention for HFC-134a. Can you please tell me what each of the parameters are that are required for a recycling policy?
```

Let's follow up to its response in a moment.

## Adding the Recycling Policy
With that in mind, let's go ahead and provide the details:

```
Great! Here's the recycling intervention I would like:

 - Recover 20%.
 - With 90% reuse.
 - With 100% induction.

Let's have this start in year 2027 and continue to the end of the simulation.
We can have this recycling happen at point of servicing (recharge).

Let's call this policy Domestic Recycling. Please also add a new simulation
alongside BAU and Permit called Recycling with this new Domestic Recycling
policy included.
```

Let's take a look at what the AI produced by opening up the resulting file in Kigali Sim. Check its work but, in particular, note that it both added a new policy and a new simulation scenario.

## Add a Combined Simulation
Let's finish this off by asking for a combined simulation which both recycles and applies the sales permit.

```
Fantastic! Please next add a Combined simulation scenario which applies
domestic recycling then also sales permit.
```

Pay close attention to how this is reflected in the UI-based editor after opening the new file. Note that we asked for domestic recycling to be applied prior to the sales permit. This means that induced demand is applied but then the sales permit ensures that the limits are still respected. This is further explored at the end of the tutorial.

> **More about policy order and interaction**
>
> Sometimes the order in which policies apply can matter. For example, consider a policy which reduces consumption by 5% and a separate policy that imposes a cap. Should the reduction count towards the cap or be applied after the cap has taken place? It depends on your policy assumptions. If the cap is a permit and the change is an educational policy, one may wish to have the percent change applied prior to cap. This is why having that policy further up in the list is important.
>
> In this case, we have recycling applied with possible changes to amount of virgin consumption. Then, the cap is applied, displacing any of the remaining virgin consumption over the limit to R-600a. Note that the combined scenario has lower consumption than permit alone because we set our permit to cap all sales which includes secondary. We can replace sales with individual caps on domestic and import if we want to target virgin only.

## Results

Piecing together what is going on with multiple policies can take a little work but let's dig in. For this, go ahead and disable **Attribute initial charge to importer** just to make it easier to reason about how volumes are shifting in the **Consumption** view. To get a closer look, select **HFC-134a** under substances.

Next, the **Emissions** radio button tells an interesting story where recycling captures some of those emissions. Also, this is where the combination of policies may be effective. Once we add in the cap as well, the two complement each other to achieve a higher impact than either on their own.

[Video: tutorial_06_03.webm - demonstration of results across scenarios](/webm/tutorial_06_03.webm)

> **More on induced demand**
>
> Note that we said 100% induced demand. This means that virgin consumption does not on its own decrease because recycling is present. This is common in recycling programs where recycling does not 1:1 offset virgin production. Determining the correct rate of displacement (100% - induced demand) can require a bit of work.
>
> For comparison, try setting the induced demand to **0%** instead. With 0% induced demand, every kg of recycled substance means one less kg of virgin substance produced, a direct 1:1 offset. First, check the **Bank** view with million units of equipment selected. Notice that BAU and recycling scenarios now have the same amount of equipment, since recycling no longer drives additional demand. Next, switch to **Consumption** and observe that virgin domestic production is now lower in the recycling scenario. Finally, return to **Emissions** to see that they remain lower with recycling in either case, though the mechanism differs between 0% and 100% induced demand. For the purposes of the later tutorials, let's leave induced demand at 100%, but remember that you can try out different values along the way.
>
> All that in mind, induced demand is likely neither 0% nor 100% in practice. However, this ambiguity largely goes away when we add the cap policy. When both are employed together, we see a stronger response in terms of emissions. In this case though, it is important to have recycling followed by permit. This is because we want recycling effects calculated first. After all, if there's enough servicing activity, demand is higher than it would be without recycling! Then, the cap policy can reduce whatever virgin sales were after recycling to the mandated levels, offsetting any induced demand over the cap into R-600a instead of leaving it in HFC-134a.

> **More about terminology / bank**
>
> **Bank** refers to all of the equipment and substance (not yet emitted to the atmosphere) in a country. Some call this reservoir. This can be expressed in terms of number of units of equipment, substance volumes, and tCO2e. Kigali Sim uses Bank to refer to the full population of equipment and substance in the country at any given time. This is also where metrics derived from operating characteristics (like energy consumption) are reported.

[Video: tutorial_06_04.webm - demonstration of bank and emissions views](/webm/tutorial_06_04.webm)

## Conclusion

You've successfully implemented ABC Country's comprehensive HFC-134a strategy combining permitting and recycling policies. This tutorial demonstrated how recycling appears as an alternative supply source and complements demand-side restrictions for maximum policy effectiveness. Along the way, used AI assistance to help understand what each of the recycling parameters meant.

**Download the completed tutorial** result at [tutorial_06.qta](../tutorial_06.qta) which contains the complete model with combined permitting and recycling policies. It differs from the [prior tutorial result](../tutorial_05.qta) in that it adds recycling, the results of which are most apparent when looking at emissions.

## Next Steps

[Tutorial 7](tutorial_07.md) will transition from the UI-based interface to direct QubecTalk programming. You'll discover that you've been programming all along and learn to implement advanced multi-substance policies only possible through direct coding. QubecTalk also makes it easier to modify simulations faster. For example, this can help with switching from tonnes of substance to equipment unit counts. While technically possible in the UI, we will find in [Tutorial 8](tutorial_08.md) that those kinds of changes may be much more efficient through code.

[Previous: Tutorial 5](tutorial_05.md) | [Return to Guide Index](index.md) | [Next: Tutorial 7](tutorial_07.md)

---

_This tutorial is part of the ABC Country case study series demonstrating progressive HFC policy analysis using Kigali Sim._
