# Kigali Sim Guide

Hello! This website provides a series of tutorials intended to help you get started with the [Kigali Sim](https://kigalisim.org). If you are new, consider starting with the [first tutorial](tutorial_01.md).

This tutorial series follows a progressive learning path using ABC Country, a hypothetical medium-sized nation where we build up an increasingly sophisticated model together. You'll learn both point-and-click interface techniques and QubecTalk programming to master comprehensive HFC phase-down analysis.

## Tutorials

All numbers are intentionally fake and used only for demonstrative purposes. Please keep in mind that this software is still under development. We encourage your ideas as our living software continues to grow based on your feedback. However, please note that things may also change and this guide may be refined.

### Introduction

- [**Tutorial 1:** Introduction to Kigali Sim](tutorial_01.md) - Understanding HFCs and how Kigali Sim can help

### UI-based Authoring

- [**Tutorial 2:** Single Application and Substance](tutorial_02.md) - Start with the basic building blocks of a simulation
- [**Tutorial 3:** Multiple Applications and Substances](tutorial_03.md) - Expanding to multiple sectors and refrigerants
- [**Tutorial 4:** Growth and Trade](tutorial_04.md) - Adding economic growth projections and import flows
- [**Tutorial 5:** Simple Policies](tutorial_05.md) - ABC Country's first policy intervention using a sales permitting system
- [**Tutorial 6:** Recycling](tutorial_06.md) - Exploring secondary material through recycling programs

### Code-based Authoring

- [**Tutorial 7:** First Code](tutorial_07.md) - Discovering QubecTalk and implementing multi-substance recycling policies
- [**Tutorial 8:** Equipment Units-Based Modeling](tutorial_08.md) - Alternative consumption specification using equipment sales data
- [**Tutorial 9:** Command Line and Monte Carlo Analysis](tutorial_09.md) - Advanced uncertainty analysis using probabilistic programming
- [**Tutorial 10:** Custom Policies with Code](tutorial_10.md) - Advanced policy modeling with induced demand (under construction)

### Feature-Specific

- [**Tutorial 11:** Global Warming Potential](tutorial_11.md) - Demonstrating GWP impact reduction with a sample substitution policy
- [**Tutorial 12:** Equipment Energy Efficiency](tutorial_12.md) - Exploring energy consumption with equipment model

## QubecTalk Reference

Complete reference documentation for QubecTalk, the domain-specific language used by Kigali Sim for advanced modeling and policy analysis.

- [**Stanzas Reference**](qubectalk_stanzas.md) - Program structure including default, policy, and simulations blocks
- [**Commands Reference**](qubectalk_commands.md) - Executable statements for substance properties, consumption, and policy interventions
- [**Language Features Reference**](qubectalk_language_features.md) - Advanced features including conditional logic, probabilistic sampling, and mathematical operations

## Get Help

Need assistance? Email us at [hello@kigalisim.org](mailto:hello@kigalisim.org) or [learn about all of the support options](https://vimeo.com/1061085671?share=copy) offered by our University of California Berkeley team.

## Formal Documentation

In addition to the tutorials, [formal documentation on the engine](qubectalk.pdf) is also available. This living specification document provides a robust technical description of this work.

## Coming Soon

Some parts of our documentation are still under construction. Please see below:

- Custom policy logic
- Examples / gallery

Some of these features may be available within the tool but tutorials are not yet available. Please watch this page for additional updates.

## License and Dedication

Code is released under the [BSD 3-Clause License](https://opensource.org/license/bsd-3-clause). Everything else in this guide is released under the [CC-BY License](https://creativecommons.org/licenses/by/4.0/). This guide is dedicated to Barbara Berke and her dedication to making computation available to everyone.