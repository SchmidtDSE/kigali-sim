---
title: 'Kigali Sim: Open source simulation toolkit for modeling substances and policies related to the Montreal Protocol'
tags:
  - JavaScript
  - Java
  - policy simulation
  - Montreal Protocol
  - hydrofluorocarbons
  - climate change
  - domain-specific language
authors:
  - name: A Samuel Pottinger
    orcid: 0000-0002-0458-4985
    affiliation: 1
    corresponding: true
  - name: Balaji Natarajan
    affiliation: 2
  - name: Magali de Bruyn
    affiliation: 1
  - name: Ciera Martinez
    affiliation: 1
affiliations:
  - name: Schmidt Center for Data Science and Environment, University of California, Berkeley, California, United States of America
    index: 1
  - name: United Nations, Ozone Secretariat
    index: 2
date: 23 October 2025
bibliography: paper.bib
---

# Summary
Co-created with over a dozen countries and supporting organizations, Kigali Sim is an open source simulation platform that offers reusable modeling of substances controlled under the Montreal Protocol with particular focus on hydrofluorocarbons regulated by the Kigali Amendment. With warming potentials up to thousands of times greater than carbon dioxide, this parallelized engine aids both atmospheric science research and policy development through comprehensive stock-flow modeling within internationally recognized conventions. Furthermore, supporting a diverse community with heterogeneous programming expertise, the community-built toolkit interoperates between a visual no-code interface for rapid model development and an embedded domain-specific language. Finally, running locally in web browsers via WebAssembly or on desktop via JVM, this portable software enables privacy-preserving democratized access to rigorous modeling techniques, facilitating science and practical evidence-based policy in support of history's most successful international environmental treaty.

# Statement of need
Widely recognized as the most successful international environmental treaty and signed by all 197 UN member countries, the Montreal Protocol has successfully phased out 99% of ozone-depleting substances [@montreal_protocol_success]. Its ambitious 2016 Kigali Amendment extends this multi-lateral framework to hydrofluorocarbons which contribute significantly to climate change [@kigali_amendment]. Research and policy analysis for these controlled substances requires modeling of complex interactions between economic, physical, and policy factors. Lacking an open source toolkit providing integrated stock-flow modeling for these substances, today's analytical workflows face substantial barriers: many rely on expensive proprietary models or labor-intensive ad-hoc approaches which cannot easily be reused, extended, or shared. These hurdles may burden national ozone units and implementing agencies who possess deep domain knowledge but may lack access to computational resources or programming expertise in simulating these complex systems.

Kigali Sim provides the first open source toolkit for this comprehensive modeling of Montreal Protocol-controlled substances across their full lifecycle. It simulates emissions, power consumption, substance consumption, and "bank" metrics (amount of equipment and substance within the country). This reusable engine also accounts for complex modeling requirements including policy effects for permitting systems and recycling while following conventions for analysis [@mlf_guidelines]. Furthermore, co-created through collaboration with over a dozen countries and implementing agencies, this community-built toolkit emerges from use in real-world data to support on-the-ground scientists. Demonstrating practical utility for evidence-based international environmental policy development, multiple countries are actively using this platform within thier Kigali Amendment activity.

# Design
Kigali Sim serves a diverse ecosystem spanning national ozone units, implementing agencies, analysts, and researchers. Most practitioners are domain experts in atmospheric chemistry and environmental policy with heterogeneous programming experience. Kigali Sim simulations transit through organizational systems as simulations and policies evolve through phases of work.

## Simulation engine
Countries and supporting organizations approach modeling with divergent data availability where some may work forwards from trade records and industry surveys while others work backwards from bank estimates or observed emissions. The engine operates in multiple modes to accommodate this diversity: the engine propagates values through substance flows and complete lifecycles. After creating baseline scenarios, it layers complex policy interventions on top of "business as usual" including caps, restrictions, replacements, and recycling systems with configurable recovery rates and economic induction effects.

![Diagram showing data flow through Kigali Sim simulation engine from input data (trade records, bank estimates, or equipment surveys) through stock-flow calculations to output metrics.\label{fig:architecture}](KigaliEngine.svg)

## Dual-Interface Design
Most users in Kigali Sim's community do not identify as programmers. Reflecting empirical studies finding domain experts benefit from specialized languages [@dsl_comprehension; @dsl_evaluation], our software therefore starts with UI-based editor but progressively transitions users to the code-based programming portal [@portal] as analytical needs evolve. However, in both cases, simulation execution remains completely local, ensuring privacy.

### UI-based authoring
The UI-based editor offers point-and-click interaction while familiarizing the user with Kigali Sim's concepts, creating a loop-based design [@loop_based_design; @core_loops] in which users make small changes and Kigali Sim automatically translates their GUI inputs to QubecTalk code run for immediate feedback. Results show emissions, consumption, equipment populations, and energy usage with CSV export functionality. Note that the web interface progressively exposes functionality through sequenced disclosure [@hayashida_structure; @hayashida_video] as an on-ramp into complexity as an open design for fluid exploration [@plastics; @open_world_zelda].

![Screenshot of the UI-based editor modifying an example simulation.\label{fig:ui_editor}](KigaliEditor.png)

### Code-based authoring
Prior research shows that domain-specific languages are particularly valuable for domain experts who do not identify as programmers [@dsl_benefits; @dsl_domain_experts]. The QubecTalk domain-specific language facilitates expression of complex policy scenarios through human-readable syntax inspired by HyperTalk [@hypertalk] and mirrored by the UI-editor. Critically, QubecTalk speaks fluently in the language of the Montreal Protocol treaty itself, using familiar terminology and allowing direct translation of terms of art into executable simulations. For example, `set domestic to 350000 kg during year 1` or `cap import to 85% during years 3 to 5`. This treaty-specific grammar also supports uncertainty quantification (`sample normally from mean of X std of Y`), variable definitions, conditional logic, and policy chains where multiple interventions apply sequentially.

## Flexibility
Kigali Sim simulations may work across many organizations and changing datasets. As the same stocks may be measured using diverse units and methodologies, QubecTalk and the UI-based editor provide automated unit conversions including those which depend on equipment properties such as charge levels. Additionally, most simulation code can be modified either by the UI-based editor or the code-based editor where changes in one reflect in the other, attempting to bridge collaborators of different preferences and skill sets. Indeed, many community collaborators reported starting in the UI-editor but transitioning to code over time.

## Limitations
We leave the following for future work:

 - Kigali Sim can model energy consumption but its estimate of CO2e only extends to direct emissions though users may export results and combining with energy mix information [@energy_mix].
 - Modelers typically do not know the specific age distribution of equipment so we ask users to provide retirement and servicing as amortized rates from which emissions are estimated, similar to IPCC Tier 1 [@ipcc_tiers].
 - Kigali Sim provides automatic lookup of some values but allows for use of country-specific parameters, similar to IPCC Tier 2 [@ipcc_tiers].

# Implementation
Released as BSD-licensed, Kigali Sim runs in two modes: browser-based via WebAssembly and local via JVM. The WebAssembly implementation comes from TeaVM [@teavm] compilation of the Java engine. This browser-based approach enables privacy-preserving local computation with zero installation and without transmitting sensitive country data to external servers. The JVM-based engine offers identical functionality for desktop or workflow integration but with additional performance features and added tools for Monte Carlo where parallelization is achieved through streaming. Both modes support parallel computation through native threads or web workers. The engine uses ANTLR4 [@antlr] for parsing the QubecTalk domain-specific language in both Java and JavaScript components^[A JS ANTLR visitor facilitating real-time translation between the UI-editor and the code-based editor.]. High-precision arithmetic relies on BigDecimal to ensure accurate calculations of emissions and consumption metrics [@bigdecimal].

![Diagram describing multi-modal execution in which simulations run across different platforms.\label{fig:execution}](KigaliExecution.png)

# Acknowledgments
We thank Annie Snyder, Frederico San Martini, Kevin Koy, Tina Birmpili, Elina Yuen, Alejandro Ramirez-Pabon, and Nick Gondek for their contributions, as well as valued community members as listed at https://kigalisim.org/humans.txt including national ozone units and workshop participants who engaged with the tool during its development. We also thank the developers of our runtime dependencies [@aceeditor; @antlr; @apachecsv; @chartjs; @d3; @papaparse; @prism; @qunit; @tabby; @webpack; @teavm]. Project of The Eric and Wendy Schmidt Center for Data Science and the Environment at UC Berkeley. No conflicts of interest to disclose. AI assistants used with constrained tasks and strict human review [@claude_ai; @copilot; @intellij_ai; @replit_ai].

# References