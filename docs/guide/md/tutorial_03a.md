# Tutorial 3a: Multiple Substances with AI

Expanding to multiple sectors and refrigerants using AI.

**Note that this is the AI version of this tutorial.** You can find a version of this tutorial without AI at [Tutorial 3](https://kigalisim.org/guide/tutorial_03.html).

## Motivation
In ABC, we have more than just domestic refrigeration! In this tutorial, we'll expand our model to capture more of the broader national profile, helping us understand where policy may put its focus. Specifically, this tutorial builds directly on our Tutorial 2 Domestic Refrigeration model, adding Domestic AC. We'll also introduce multiple refrigerant substances with different Global Warming Potentials (GWPs), demonstrating how volume and climate impact interact.

All that said, all of this can take quite a bit of clicking. So, we will see if AI can help us out to speed up the process. Specifically, we will demonstrate the loop of giving AI a task and checking its output.

## Preparing for AI
In this tutorial series, we have used [Claude](https://claude.ai/). However, in practice, most AI assistants can help with Kigali Sim using a standard called [llms.txt](https://llmstxt.org/). So, to get AI ready to go, please create a new chat session. Then, give it your Tutorial 2 file by going to the Save File button at the top of the screen and adding it to the chat as an attachment. Finally, tell it to look up information about Kigali Sim through a message like this:

```
Hello! I would like help with the attached Kigali Sim simulation. Please
read https://kigalisim.org/llms-full.txt?v=20251226 to learn more. Please
stick to only features compatible with the UI editor.
```

Need a little cheat? [Download the Tutorial 2](tutorial_02.qta) file here.

For more capable assistants like Claude, this is enough. However, some assistants cannot access the full internet or won't know how to work with this kind of file out of the box. If your assistant is having issues, instead, attach the [Kigali Sim llms-full.txt](https://kigalisim.org/llms-full.txt?v=20251226) file as an attachment!

Note that we are telling the AI assistant to avoid advanced features which require programming. More details about this are available in [Tutorial 11](https://kigalisim.org/guide/tutorial_11.html).

## Add a new substance
Let's start with one new substance record. Here's a prompt:

```
Please add R-600a to the simulation. It will be in domestic refrigeration
like HFC-134a with a GWP of 3 kgCO2e / kg and initial charge of 0.07 kg /
unit. Please use retirement of 5% / year and 10% recharged per year at
0.07 kg / unit. Please have 100000 units of prior equipment and 2 mt /
year of domestic manufacture. Thanks!
```

After the AI is done, download the resulting file and click "Load File" in the UI and upload the AI output. If you are comfortable, you can also copy from the AI assistant chat into the editor tab.

## Check the work
Using what you learned in [Tutorial 2](https://kigalisim.org/guide/tutorial_02.html), go ahead and click on edit next to the new consumption record. Is it what you expected? These kinds of simple prompts are almost always successful with more capable AI assistants like Claude but be careful to double check the output of any AI.

Note that, if Kigali Sim reports that a simulation requires use of the code editor, see [Tutorial 7](https://kigalisim.org/guide/tutorial_07.html) or remind the AI assistant to only stick to UI editor compatible features.

## Adding multiple substances
This is a great start but let's try to be more brief in our next message:

```
Great! Let's add two more substances...

HFC-32:

 - Application: Domestic AC
 - GWP: 675 kgCO2e / kg
 - Initial Charge: 0.85 kg / unit
 - Retirement: 7%
 - Recharge: 15% at 0.85 kg / unit
 - Prior Equipment: 40000 units
 - Domestic Manufacture: 15 mt / yr

R-410A:

 - Application: Domestic AC
 - GWP: 2088 kgCO2e / kg
 - Initial Charge: 1.00 kg / unit
 - Retirement: 7%
 - Recharge: 15% at 1.00 kg / unit
 - Prior Equipment: 20000 units
 - Domestic Manufacture: 5 mt / yr
```

Don't get lazy! Again, please double check the AI's work after loading its resulting simulation into Kigali Sim.

## Interpreting Multi-Application Results

As you work, the simulation will update automatically.

Examine the results to understand how multiple applications and substances add together. You can do this by looking at results by selecting the **Application** or **Substances** radio buttons. To get a complete picture with the **Emissions** radio button, try clicking **"configure custom"** under emissions and combining both end-of-life and recharge emissions. This represents the total leakage throughout the equipment lifetime.

<video src="/webm/tutorial_03_02.webm" autoplay loop muted playsinline style="width: 500px; border: 2px solid #505050; border-radius: 3px;">Your browser does not support the video tag. Please upgrade to a modern browser.</video>

Before concluding, let's also pause to understand if these results make sense. First, the custom emissions which combines both end of life and recharge emissions is higher than either alone. Second, consider that the HFC-134a has higher volume and higher GWP than R-600a. Therefore, focusing on HFC-134a, we notice that these two factors intersect through a larger gap to R-600a in emissions relative to consumption when we have selected the **Substances** radio button.

<video src="/webm/tutorial_03_03.webm" autoplay loop muted playsinline style="width: 500px; border: 2px solid #505050; border-radius: 3px;">Your browser does not support the video tag. Please upgrade to a modern browser.</video>

## Conclusion

You've successfully expanded ABC Country's model to include multiple applications and substances! To get there, AI helped speed up the process quite a bit. All that said, we considered:

- **Multi-application modeling**: Different sectors with distinct equipment characteristics and service patterns.
- **Multi-substance analysis**: Comparing different refrigerants within and across applications.
- **GWP diversity**: Understanding how different substances have varying climate impacts.
- **Equipment population dynamics**: How different applications scale and behave over time.

The model now provides a foundation for understanding how substance choice and application type interact to determine overall consumption and climate impact patterns.

## Next Steps

[Tutorial 4a](/guide/tutorial_04a.html) will add economic growth projections and business-as-usual forecasting to your multi-application model. You'll learn to model how economic expansion drives consumption changes over time, creating realistic baseline scenarios for policy comparison.

[Previous: Tutorial 2](/guide/tutorial_02.html) | [Return to Guide Index](/guide) | [Next: Tutorial 4a](/guide/tutorial_04a.html)

---

_This tutorial is part of the ABC Country case study series demonstrating progressive HFC policy analysis using Kigali Sim._
