# Tutorial 11: AI Coding Assistants

If you have access to an AI assistant, you can use them to help build or modify simulations through code.

## Motivation

AI assistants can help express your ideas in Kigali Sim and analyze outputs. This can benefit both those using Kigali Sim through UI-based authoring and for those taking advantage of QubecTalk code. In general, AI may offer additional speed and help in using more advanced features.

**AI for UI-based authoring**

Sometimes there is just quite a bit of typing and clicking involved to define all of the substances and policies. Of course, AI assistants can help take the natural language we use to describe simulations and convert them into structured Kigali Sim simulations that you can then read and refine using UI-based authoring. However, AI assistants can also help read data files and convert them to Kigali Sim's preferred format or walk through the defining of complex policy scenarios, helping describe those ideas in a way that Kigali Sim can understand. Just be sure that, when working with UI-based authoring, you let your assistant know like we saw in [Tutorial 3a](/guide/md/tutorial_03a.md).

**AI for programming in code**

If you recall [Tutorial 7](/guide/md/tutorial_07.md), we revealed that you are actually writing computer code when you use the UI-based authoring **Designer** tab. Kigali Sim uses a programming language built just for Montreal Protocol models called QubecTalk. In addition to allowing you to make more sophisticated simulations, this programming option also allows you to use AI (LLM) assistants. This includes many different options like [ChatGPT](https://chatgpt.com/) or, for example, we use [Claude](https://claude.ai/).

**How Kigali Sim works with AI**

We use a protocol called [llms.txt](https://llmstxt.org/) which allows you to use your own assistant. This frees you and your organization to choose the right option given different needs, terms of service, and privacy options. After you have selected the AI that you want to use, you can use natural language to work on simulations. This tutorial walks through how to build up a simulation using an AI assistant.

## Assistants

We have tried this tutorial with multiple products. If you do not yet have an assistant and are looking for one which performs well with Kigali Sim specifically, [Claude](https://claude.ai/) tends to perform the best in our testing. However, if you need an assistant with a free option, [Mistral](https://chat.mistral.ai/) also performed well.

We caution that you should be sure to read the privacy policy and terms of service for any AI you choose. Unlike Kigali Sim, these options may involve sending your simulations and data to servers controlled by other organizations. Depending your needs, you may want to choose services based on protections.

All that in mind, here are some AI assistants with which we have tested along with troubleshooting steps if needed:

### ChatGPT

[ChatGPT](https://chatgpt.com/) tends to perform well if given an example. Consider providing a tutorial qta file in your initial prompt like so:

```
Hello! Please review https://preview.kigalisim.org/llms-full.txt?v=20260128 and an example simulation at https://kigalisim.org/guide/tutorial_02.qta. Please follow the structure of that Tutorial 2 with similar keywords and layout in order to write correct QubecTalk code though you may use the other keywords and language features mentioned in llms-full.txt. Please review and then we will build a simulation together.
```

Depending on where you use ChatGPT, it may just give you back code instead of a file. This can be copied and pasted into the **Editor** tab. If you instructed ChatGPT to only use UI-editor compatible code, you can update the code and go back to the **Designer** tab to continue with UI-based authoring.

### Claude

The best performing model in our testing, typically no additional steps are needed for Claude to perform well. See [claude.ai](https://claude.ai/).

### Copilot

Different versions of copilot might perform differently. See [copilot.microsoft.com](https://copilot.microsoft.com/). In general, Copilot tends to work best when working from an example so you may want to start with a prompt like this:

```
Hello! Please review https://preview.kigalisim.org/llms-full.txt?v=20260128 and an example simulation at https://kigalisim.org/guide/tutorial_02.qta. Please follow the structure of that Tutorial 2 with similar keywords and layout in order to write correct QubecTalk code though you may use the other keywords and language features mentioned in llms-full.txt. Please review and then we will build a simulation together.
```

Depending on where you use Copilot, it may just give you back code instead of a file. This can be copied and pasted into the **Editor** tab. If you instructed Copilot to only use UI-editor compatible code, you can update the code and go back to the **Designer** tab to continue with UI-based authoring.

### DeepSeek

Visit [chat.deepseek.com](https://chat.deepseek.com/). DeepSeek tends to work fairly well but may require a reminder to add the simulations stanza like so:

```
Hello! Please review https://preview.kigalisim.org/llms-full.txt?v=20260128. Then, we will build a simulation together. Please be sure to include the simulation stanza like seen in https://kigalisim.org/guide/tutorial_02.qta.
```

### Gemini

Available at [gemini.google.com](https://gemini.google.com/), Gemini may have issues directly accessing the llms-full.txt file depending on your organization's settings. It also sometimes tries to add links in brackets which are not compatible with Kigali Sim. Therefore, you may wish to [download llms-full.txt](/llms-full.txt) and add it as an attachment in your initial prompt. To take care of both of these potential issues, here is an example initial message:

```
Hello! Please review the attached llms-full.txt for information about Kigali Sim. Then, we will build a simulation together! Also, please do not add citations in brackets in the Kigali Sim simulation itself. Instead, please mention them in your text response outside of the qta file.
```

Depending on where you use Gemini, it may just give you back code instead of a file. This can be copied and pasted into the **Editor** tab. If you instructed Gemini to only use UI-editor compatible code, you can update the code and go back to the **Designer** tab to continue with UI-based authoring.

### Mistral

One of the best performing models in our testing, typically no additional steps are needed for Mistral to perform well. See [chat.mistral.ai](https://chat.mistral.ai/). That said, if using Le Chat without login, you may need to [download llms-full.txt](/llms-full.txt) and add it as an attachment in your initial prompt. Also, depending on where you use Mistral, it may just give you back code instead of a file. This can be copied and pasted into the **Editor** tab. If you instructed Mistral to only use UI-editor compatible code, you can update the code and go back to the **Designer** tab to continue with UI-based authoring.

## Prior tutorials

This walkthrough complements prior tutorials that included AI:

- [Tutorial 3a](/guide/md/tutorial_03a.md): Modifying an existing simulation to add new substances.
- [Tutorial 4a](/guide/md/tutorial_04a.md): Using AI to incorporate information for business as usual forecasting.
- [Tutorial 6a](/guide/md/tutorial_06a.md): Using AI to walkthrough adding policy interventions.

This tutorial specifically adds additional information specific to individual AI assistants as well as guidance on using AI with more advanced features of Kigali Sim. To explore those advanced features, the rest of this tutorial will focus on code. However, the earlier tutorials can provide guidance if you want to stick to UI-based authoring.

## Giving the AI information

At minimum, you should tell your AI to read [kigalisim.org/llms.txt](https://kigalisim.org/llms.txt) but we find that providing both `llms.txt` and `llms-full.txt` is helpful. Here is how you might want to start.

```
Hello! Please review https://preview.kigalisim.org/llms-full.txt?v=20260128. Then, we will build a simulation together.
```

**Note**: You may want to review the [assistants](#assistants) section for tips on making your specific AI assistant work well. Also, the use of the version (v=20260127) is optional but we recommend it to ensure you have the latest copy.

**Tip:** For LLMs which cannot access the internet, you can [download llms-full.txt](/llms-full.txt) and provide it as an attachment to your AI assistant.

## Building the basic simulation

Next, let's instruct the AI to make a simulation called Business as Usual with HFC-134a with the similar information to [Tutorial 2](/guide/md/tutorial_02.md).

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

This tutorial demonstrated the use of AI assistants to build models in Kigali Sim. You are now ready to use LLMs to explore other functionality of Kigali Sim and move faster in prototyping new simulations. Remember, you can also use [Kigali Sim on the command line](/guide/md/tutorial_16.md). In addition to taking advantage of version control and automation, this can also enable use of tools like [Claude Code](https://www.anthropic.com/claude-code).

## Next Steps

This tutorial completes the specialized topic series! You've learned how to leverage AI assistants to accelerate your simulation development workflow. For next steps, consider exploring the other tutorials in the series or dive deeper into the QubecTalk programming language using the command line interface from [Tutorial 16](/guide/md/tutorial_16.md).

[Previous: Tutorial 10](/guide/md/tutorial_10.md) | [Return to Guide Index](/guide/md/index.md) | [Next: Tutorial 12](/guide/md/tutorial_12.md)

---

_This tutorial is part of the Feature-Specific series demonstrating specialized aspects of Montreal Protocol policy modeling using Kigali Sim._

---

[View HTML version](../tutorial_11.html)