# Tutorial 13: AI Assistants

If you have access to an AI assistant, you can use them to help build or modify simulations.

## Contents

- [Motivation](#motivation)
- [Giving the AI information](#giving-the-ai-information)
- [Building the basic simulation](#building-the-basic-simulation)
- [Adding a recycling program](#adding-a-recycling-program)
- [Results](#results)
- [Conclusion](#conclusion)
- [Next Steps](#next-steps)

## Motivation

If you recall [Tutorial 7](https://kigalisim.org/guide/tutorial_07.html), we revealed that you are actually writing computer code when you use the UI-based editor. Kigali Sim uses a programming language built just for Montreal Protocol models called QubecTalk. In addition to allowing you to make more sophisticated simulations, this programming option also allows you to use AI (LLM) assistants. This includes many different options like [ChatGPT](https://chatgpt.com/) or, for example, we use [Claude](https://claude.ai/).

More precisely, we use a protocol called [llms.txt](https://llmstxt.org/) which allows you to use your own assistant. This frees you and your organization to choose the right option given different needs, terms of service, and privacy options. After you have selected the AI that you want to use, you can use natural language to work on simulations. This tutorial walks through how to build up a simulation using an AI assistant.

We assume that you have access to an AI assistant and we have tested this tutorial with multiple products. If you do not have access to an LLM, we observe that [ChatGPT](https://chatgpt.com/) has a free offering. However, we caution that you should be sure to read the privacy policy and terms of service for any AI you choose as, unlike Kigali Sim, these options may involve sending your simulations and data to servers controlled by other organizations. All that in mind, we use [Claude](https://claude.ai/) due to its terms of use and strong performance.

## Giving the AI information

At minimum, you should tell your AI to read [kigalisim.org/llms.txt](https://kigalisim.org/llms.txt) but we find that providing both `llms.txt` and `llms-full.txt` is helpful. Here is how you might want to start.

```
Hello! Please review https://kigalisim.org/llms.txt?v=20250928 and https://preview.kigalisim.org/llms-full.txt?v=20250928. Then, we will build a simulation together.
```

The use of the version is optional but we recommend it to ensure you have the latest copy.

**Tip:** For LLMs which cannot access the internet, you can [download llms-full.txt](/llms-full.txt) and provide it as an attachment to your AI assistant.

## Building the basic simulation

Next, let's instruct the AI to make a simulation called Business as Usual with HFC-134a with the similar information to [Tutorial 2](https://kigalisim.org/guide/tutorial_02.html).

```
Thanks! Please make a simulation without any policies called Business as Usual which runs from 2025 to 2035. Please have this include HFC-134a with a GWP of 1430 kgCO2e / kg that is both domestically produced and consumed. Please initial charge with 0.15 kg / unit and have 5% of the equipment retire each year. Furthermore, please recharge 10% of equipment during all years with 0.15 kg / unit. Finally, please have there be sales of 25 mt in 2025 but have those sales increase 5% each year.

Please write the QubecTalk code for this simulation and then pause.
```

Once your assistant has produced the code, please paste it into the **Editor** tab. This is also a good moment to remember to double check the work of an AI by reading through the code.

## Adding a recycling program

Now that we have a simple simulation to start with, let's ask the AI assistant to add a recycling program and a new simulation.

```
Fantastic! Please next add a recycling program which happens at the time of servicing. We should capture 20% and have a yield loss of 50%. Let's have this start in 2028. Please add the recycling policy and an additional simulation alongside Business as Usual so we can compare.
```

You may need to ask the assistant to provide the entire updated code.

## Results

Select the **Consumption** radio button with **domestic** and **mt / year**. Then, click the **Simulations** radio button and ensure the **All** radio button is selected. You should see a delta between the two scenarios based on the new recycling policy and simulation added by the AI!

## Conclusion

This tutorial demonstrated the use of AI assistants to build models in Kigali Sim. You are now ready to use LLMs to explore other functionality of Kigali Sim and move faster in prototyping new simulations. Remember, you can also use [Kigali Sim on the command line](https://preview.kigalisim.org/guide/tutorial_09.html). In addition to taking advantage of version control and automation, this can also enable use of tools like [Claude Code](https://www.anthropic.com/claude-code).

## Next Steps

This tutorial completes the specialized topic series! You've learned how to leverage AI assistants to accelerate your simulation development workflow. For next steps, consider exploring the other tutorials in the series or dive deeper into the QubecTalk programming language using the command line interface from [Tutorial 9](/guide/tutorial_09.html).

[Previous: Tutorial 12](/guide/tutorial_12.html) | [Guide Index](/guide/)

---

_This tutorial is part of the Feature-Specific series demonstrating specialized aspects of Montreal Protocol policy modeling using Kigali Sim._