# Tutorial 4a: Growth and Trade with AI

Adding economic growth projections and import flows to create more nuanced business-as-usual scenarios, aided by AI.

**Note that this is the AI version of this tutorial.** You can find a version of this tutorial without AI at [Tutorial 4](/guide/md/tutorial_04.md).

## Motivation
Let's say ABC has economic growth forecasts and data on equipment imports. In this tutorial, we will add growth patterns and import flows to our multi-application model from Tutorial 3, creating a more nuanced business-as-usual baseline to help aid policy analysis. In addition to adding more sophistication to our model, this also demonstrates use of multiple sales streams.

As before, all of this can take quite a bit of clicking. So, we will see if AI can help us out to speed up the process! This can show how AI can help us not just add new things to our simulations but modify our existing work as well.

## Preparing for AI
If you did the AI version of Tutorial 3, you probably are already familiar with this process. However, just to cover everything, let's step through this again. Most AI assistants can help with Kigali Sim using a standard called [llms.txt](https://llmstxt.org/).

**If you did Tutorial 3 with AI** and the output from your assistant looked good, just continue your same chat session! Continue with [Adding imports for HFC-134a](#adding-imports-for-hfc-134a) section.

**If you are new to the AI** or just want to start fresh, create a new chat session. Then, tell it to look up information about Kigali Sim through a message like this:

```
Hello! I would like help with building a Kigali Sim simulation. Please read https://kigalisim.org/llms-full.txt?v=20260128 to learn more. Please stick to only features compatible with the UI editor.
```

Need a little cheat? [Download the Tutorial 3](../tutorial_03.qta) file here. Also, for more capable assistants like Claude, this is enough. However, some assistants cannot access the full internet or won't know how to work with this kind of file out of the box. If your assistant is having issues, instead, attach the [Kigali Sim llms-full.txt](https://kigalisim.org/llms-full.txt?v=20260128) file as an attachment!

When your assistant is ready, next give it your Tutorial 3 file. This can be done by going to the Save File button at the top of the Kigali Sim application and adding it to the chat as an attachment with a message like this:

```
Great! The attached is the simulation I started working on.
```

**Note**: More details and troubleshooting steps specific to individual AI assistants are available in [Tutorial 11](/guide/md/tutorial_11.md).

## Adding imports for HFC-134a
Before we add in growth rates, let's consider trade. Specifically, for brevity, let's have just one substance with imports.

Let's say that ABC imports some but not all of their HFC-134a. Therefore, for **Domestic Refrigeration**, modify your **HFC-134a** consumption record with a prompt like this:

```
Please modify HFC-134a to enable imports in addition to the domestic manufacture already present. For charge, let's use 0.2 kg / unit for import. Then, let's reduce domestic to 13 mt / year for this substance in 2025. However, also add 11 mt / year in year 2025. Thanks!
```

When the AI is done, check its work by downloading the file it creates and using the Load File button in Kigali Sim. You should see changes when you click "edit" next to HFC-134a (domestic refrigeration).

## First Economic Growth
In addition to trade, let's also add in economic growth. For example, these projections might come from industry surveys or from outside modeling efforts. Let's continue with HFC-134a:

```
Great! Let's continue with HFC-134a. Next, please expect growth +6% of current for all sales (domestic manufacture and import) from 2025 to 2030. Then, have this decrease to +4% of current for all sales from 2031 to 2035.
```

Be sure to open the resulting simulation again and see how the AI's edits translate to changes in the configuration seen in the editor. You'll want to open up the "change" tab after clicking "edit" next to HFC-134a again.

**More about percentages:** You can use `% current` instead of `%` for growth rates to make it explicit that the percentage applies to the current year's value. The system supports three percentage formats: `%` (equivalent to `% current`) applies the percentage to the current year's value, `% current` explicitly applies to the current year's value, and `% prior year` applies the percentage to the previous year's value. This flexibility allows you to match your data source's reference year convention. This could come up in simulations with multiple change directives due to complex economic modeling.

**More about the sales stream:** This refers to all consumption in the country. So, having Kigali Sim set / change sales impacts overall consumption. More specifically, it includes domestic and import but excludes export. When applying changes through the sales keyword, Kigali Sim will try to keep the ratio between domestic and import the same for the substance.

Note that this may also include "secondary" substance if recycling is active. That said, indicated by the recover command, recycling capacity is assumed to be limited. So, domestic and import will be modified to satisfy a set or change command after taking the unchanged recycling stream into account. However, using sales with cap/floor (like for permitting) places lower or upper limits on all consumption including recycling. For virgin only, replace sales with individual commands on domestic and import. This will exclude secondary production.

## Expanding the Growth
Let's continue by applying these growth rates to other substances.

```
Great! Let's continue with change statements. Using HFC-134a as an example, please ensure the following:

HFC-134a (domestic refrigeration):

 - +6% (2025-2030) domestic
 - +4% (2031-2035) domestic
 - +6% (2025-2030) import
 - +4% (2031-2035) import

R-600a (domestic refrigeration):

 - +5% (2025-2030) domestic
 - +3% (2031-2035) domestic

HFC-32 (domestic AC): +10% (2025-2035)

R-410A (domestic AC): +6% (2025-2035)
```

You could choose to say just `%` instead of `% of current` and the result should be the same. Once more, be sure to load the resulting simulation into Kigali Sim to double check the work.

Note: If Kigali Sim reports that a simulation requires use of the code editor, see [Tutorial 7](/guide/md/tutorial_07.md) or remind the AI assistant to only stick to UI editor compatible features.

## Results

Note that the drop down menu under the Consumption radio button which can flip between domestic and imports. Just as we did with the custom metric under emissions before, we can click configure custom to combine imports and domestic together.

Does the imports part of HFC-134a seem small? It's important to note that, by default, initial charge for new equipment is attributed to the exporting country. We can temporarily change this behavior to get a fuller picture of our global consumption by checking **Attribute initial charge to importer**. However, to stay consistent with Montreal Protocol standards, uncheck it to review treaty-aligned numbers. When authoring simulations, often it helps to consider both perspectives.

Zooming out, we should see the acceleration in HFC-134a and HFC-32. With the **Emissions** radio button, things still remain quite dominated by HFC-134a. In contrast, the two are closer when selecting the **Consumption** radio button as that 10% increase compounds over time for HFC-32.

## Conclusion

You now have ABC Country's realistic business-as-usual scenario incorporating economic growth, trade flows, and technology transitions. You even sped this up through AI! Anyway, this foundation shows how consumption evolves without intervention. Later, we will try out different policies on this of this baseline.

Note that, for those with **local assembly**, domestic can be used to model in-country initial charge. In other words, for substance which is equipment and then initial charged within the country, you can move the "sales" of that substance from import to domestic and the correct trade attribution will apply.

**Download the completed tutorial**: [tutorial_04.qta](../tutorial_04.qta) - this contains the complete model with economic growth and trade flows

## Next Steps

[Tutorial 5](/guide/md/tutorial_05.md) will start to add new sophistication by modeling policies. You'll learn to create sales permitting systems that progressively reduce HFC consumption while managing market displacement to lower-GWP alternatives.

[Previous: Tutorial 3a](/guide/md/tutorial_03a.md) | [Return to Guide Index](/guide/md/index.md) | [Next: Tutorial 5](/guide/md/tutorial_05.md)

---

_This tutorial is part of the ABC Country case study series demonstrating progressive HFC policy analysis using Kigali Sim._

---

[View HTML version](../tutorial_04a.html)
