# Tutorial 1: Introduction to Kigali Sim

Understanding HFCs and how Kigali Sim can help.

## Motivation

Welcome! In this introductory tutorial, we will explore how Kigali Sim can help in an HFC phase-down context and why models made within the tool may be valuable for countries as they analyze different policy ideas. This will set the groundwork for the rest of the tutorial series which builds up a sophisticated case study as we tour multiple features of the platform.

## Background

HFCs are potent greenhouse gases used in air conditioners, refrigerators, aerosols, foams, and other products. While they offer important improvements over the [CFCs and HCFCs](https://www.unep.org/ozonaction/who-we-are/about-montreal-protocol) which came before, these HFCs can be hundreds or thousands of times more potent than CO2 in terms of climate warming impact. For example, [HFC-134a has a global warming potential of 1,430](https://www.infraserv.com/en/services/facility-management/expertise/f-gas/gwp-calculator/), meaning that one kilogram of HFC-134a contributes 1,430 times as much to the greenhouse effect as one kilogram of CO₂ over a 100-year period.

[In 2016, 197 countries adopted the Kigali Amendment](https://www.epa.gov/ozone-layer-protection/recent-international-developments-under-montreal-protocol) to the Montreal Protocol in Rwanda to phase down HFCs. Under the agreement, countries committed to cut the production and consumption of HFCs by more than 80 percent over the next 30 years, aiming to [avoid more than 80 billion metric tons of carbon dioxide equivalent emissions by 2050](https://www.epa.gov/ozone-layer-protection/recent-international-developments-under-montreal-protocol). This would avoid up to 0.5° Celsius warming by the end of the century.

[In 2023, 24 Kigali HFC Implementation Plans (KIPs) were approved](https://www.multilateralfund.org/news/kigali-amendment), mobilizing more than 53 million USD to foster the shift towards eco-friendly alternatives. Kigali Sim enters this picture because countries developing Implementation Plans (KIPs) need to understand complex policy trade-offs and interactions. Therefore, simulation tools like Kigali Sim help policymakers explore KIP scenarios to inform decisions at each step of the way.

## Kigali Sim

Kigali Sim uses an stock-flow modeling approach that tracks HFC consumption through manufacturing, trade, servicing, and end of life. This method provides realistic projections by accounting for equipment lifecycles, retirement patterns, and the distinction between new equipment sales and maintenance based on existing equipment populations.

The tool operates by establishing business-as-usual baselines and then "stacking" policy interventions on top to compare scenarios. This allows testing individual policies or combinations against the same baseline. This approach can also evaluate different assumptions like under different climate change (SSP) scenarios.

Finally, Kigali Sim uniquely allows specification of the system either where equipment population data is provided or where substance sales (or even tCO2e estimates) seed the model. This allows for flexibility in which users can provide the data they have and the system will try to fill in the gaps as best it can until more specific data become available later. 

## Guide

This tutorial series follows a progressive learning path using ABC Country. For this hypothetical medium-sized nation, we build up an increasingly sophisticated model together. This is not necessarily meant to be entirely realistic but it will help us experiment with many of the available Kigali Sim features.

We will start with Kigali Sim's point-and-click interface which doesn't require code to build up sophisticated simulations. Though we will start with single substance modeling using basic equipment data, we will quickly expand into multi-sector, multi-substance national inventories with economic growth and business-as-usual projections. We will also consider supply chain analysis distinguishing manufacturing from imports before simulating policy interventions and impact assessment.

Finally, after you are comfortable with the basics, we will transition to QubecTalk. This bespoke programming language built just for Kigali Sim affords additional flexibility in defining models. We will use it to introduce advanced techniques for complex policies and probabilistic projection. No worries if you have limited programming experience. This is writing short commands similar in complexity to formulas in spreadsheet software.

Altogether, this progression mirrors real-world modeling we hope Kigali Sim provides: you can start simple with available data before adding sophistication as questions become more complex and additional data becomes available.

## Get Started

Kigali Sim is available as a free, open-source web application at [https://kigalisim.org](https://kigalisim.org/). No installation is required! It runs entirely in your web browser with a privacy-respecting design that does not share your simulations and data with others. Everything is saved and runs on your own machine. However, you can save and load files to send your work to colleagues and partners.

## Next Steps

**Tutorial 2** will begin hands-on modeling by creating ABC Country's first simulation focusing on domestic refrigeration. You'll learn how to set up equipment populations, configure consumption patterns, run simulations, and interpret results, building the foundation for comprehensive HFC phase-down analysis.

---

## Works Cited

1. United States Environmental Protection Agency. "Recent International Developments under the Montreal Protocol." Accessed July 21, 2025. https://www.epa.gov/ozone-layer-protection/recent-international-developments-under-montreal-protocol
    
2. United Nations Environment Programme. "About Montreal Protocol." Accessed July 21, 2025. https://www.unep.org/ozonaction/who-we-are/about-montreal-protocol
    
3. Multilateral Fund for the Implementation of the Montreal Protocol. "The Kigali Amendment." Accessed July 21, 2025. https://www.multilateralfund.org/news/kigali-amendment
    
4. International Institute of Refrigeration. "Global warming potential (GWP) of HFC refrigerants." September 15, 2023. https://iifiir.org/en/encyclopedia-of-refrigeration/global-warming-potential-gwp-of-hfc-refrigerants
    
5. Infraserv. "GWP calculator." Accessed July 21, 2025. https://www.infraserv.com/en/services/facility-management/expertise/f-gas/gwp-calculator/
    

_This tutorial is part of the ABC Country case study series demonstrating progressive HFC policy analysis using Kigali Sim._