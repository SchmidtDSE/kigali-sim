---
title: 'Kigali Sim: Open source simulation toolkit for modeling substances and policies related to the Montreal Protocol'
tags:
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
  - name: Secretariat of the Multilateral Fund for the Implementation of the Montreal Protocol, United Nations Environment Programme
    index: 2
date: 23 October 2025
bibliography: paper.bib
---

# Summary
Co-created with over a dozen countries and organizations, Kigali Sim offers reusable modeling of substances controlled under the Montreal Protocol. This open source platform focuses on the Kigali Amendment regulating hydrofluorocarbons which contribute to climate change through warming potentials up to thousands of times greater than carbon dioxide. This parallelized engine aids both atmospheric science research and policy development through comprehensive stock-flow modeling, helping facilitate evidence-based policymaking. Furthermore, supporting a diverse community with heterogeneous programming expertise, this community-built toolkit interoperates between a visual no-code interface for rapid model development and an embedded domain-specific language. Finally, running locally in web browsers via WebAssembly or on desktop via the Java Virtual Machine, this portable software enables privacy-preserving democratized access to rigorous simulation, facilitating scientific analysis in support of history's most successful international environmental treaty.

# Statement of need
Widely recognized as the most successful international environmental treaty [@montreal_protocol_success] and signed by all 197 UN member countries, the Montreal Protocol has successfully phased out 99% of ozone-depleting substances [@ozone]. Its ambitious 2016 Kigali Amendment extends this multi-lateral framework to hydrofluorocarbons which contribute significantly to climate change [@kigali_amendment; @contribute]. However, research and policy analysis for these controlled substances requires modeling of complex interactions between economic, physical, and policy factors. Lacking an open source toolkit or holistic stock-flow modeling, today's analytical workflows face substantial barriers: many rely on proprietary models or labor-intensive ad-hoc approaches which cannot easily be reused, extended, or shared. These hurdles may burden National Ozone Units and Implementing Agencies who possess deep domain knowledge but may lack easy access to computational resources or programming expertise in simulating these complex systems.

Kigali Sim provides the first open source toolkit for this comprehensive modeling of Montreal Protocol-controlled substances across their full lifecycle. It simulates emissions, power consumption, substance consumption, and "bank" metrics (equipment and substance within the country). Following conventions for analysis, this reusable engine also accounts for complex policy effects like for permitting systems and recycling [@mlf_guidelines]. Furthermore, co-created through collaboration with over a dozen countries and implementing agencies, this community-built toolkit emerges from real-world se to support on-the-ground scientists. Demonstrating practical utility for evidence-based international environmental policy development, multiple countries are actively using this platform.

# Design
Kigali Sim serves a diverse ecosystem spanning National Ozone Units, Implementing Agencies, analysts, researchers, and other organizations. Most practitioners are domain experts in atmospheric science or environmental policy with heterogeneous programming experience. Kigali Sim supports their work through portable simulation in a dual-interface design.

## Simulation engine
Countries and supporting organizations approach modeling with divergent data availability where some may work from trade records and industry surveys while others from bank estimates or observed emissions. The engine propagates known values provided by the user through substance flows and lifecycles to estimate the numbers not provided. After creating this baseline scenario, it layers complex policy interventions on top of "business as usual" such as permitting and recycling with configurable economic and physical assumptions.

![Diagram showing data flow through Kigali Sim simulation engine from input data (trade records, bank estimates, or equipment surveys) through stock-flow calculations to output metrics.\label{fig:architecture}](KigaliEngine.svg){width="100%"}

## Dual-Interface Design
Most users in Kigali Sim's community do not identify as programmers. Reflecting empirical studies finding domain experts with "limited programming knowledge" benefit from specialized languages [@dsl_evaluation], our software therefore starts with a UI-based editor but progressively transitions users to the code-based authoring as analytical needs evolve. However, in all cases, simulation execution remains local, ensuring privacy.

### UI-based authoring
The UI-based editor offers point-and-click interaction while familiarizing the user with Kigali Sim's concepts, creating a loop-based design [@loop_based_design; @core_loops] in which users make small changes and Kigali Sim automatically translates their GUI inputs to QubecTalk code run for immediate feedback. Results show emissions, consumption, equipment populations, and energy usage with CSV export functionality. Note that the web interface first progressively exposes functionality through sequenced disclosure [@hayashida_structure; @hayashida_video; @pyafscgap] as an on-ramp into an more open design for fluid exploration [@open_world_zelda; @plastics].

![Screenshot of the UI-based editor modifying an example simulation.\label{fig:ui_editor}](KigaliEditor.png)

### Code-based authoring
The QubecTalk domain-specific language facilitates expression of Montreal Protocol simulations, tackling complex policy scenarios through human-readable syntax inspired by but distinct to SmallTalk / HyperTalk [@smallhypertalk] mirrored by the UI-editor. Critically, QubecTalk speaks fluently in the language of the Montreal Protocol, using familiar terminology and allowing direct translation of terms of art into executable simulations. For example, `set domestic to 350000 kg during year 1` or `cap import to 85% during years 3 to 5`. This treaty-specific grammar also supports uncertainty quantification (`sample normally from mean of X std of Y`), variable definitions, conditional logic, and policy chains where multiple interventions apply sequentially. Users may author scripts in a web-based programming portal [@portal] or in a third-party editor with direct invocation of the Java Virtual Machine (JVM).

## Flexibility
Kigali Sim simulations may work across many organizations and changing datasets. As the same stocks may be measured using diverse methodologies, QubecTalk and the UI-based editor provide automated unit conversions including those which depend on equipment properties such as charge levels. Additionally, most simulation code can be modified either by the UI-based editor or the code-based editor where changes in one reflect in the other, attempting to bridge different preferences and skill sets. Indeed, many community collaborators reported starting in the UI-editor but transitioning to code over time.

## Limitations
Leaving the following for future work, Kigali Sim:

 - Can model energy consumption but its estimate of CO2e only extends to direct emissions though users may calculate indirect emissions by combining exported data with energy mix information [@energy_mix].
 - Asks users to provide retirement and servicing as amortized rates as modelers typically do not know the specific age distribution of equipment, similar to IPCC Tier 1 [@ipcc_tiers].
 - Provides automatic lookup of some values but allows for override with country-specific parameters, similar to IPCC Tier 2 [@ipcc_tiers].
 - Applies treaty trade attribution but will only attribute charge prior to equipment sale entirely to either the importer or exporter^[Local assembly can be modeled as domestic production.].

# Implementation
Released as BSD-licensed, Kigali Sim runs in two modes: browser-based via WebAssembly (WASM) and local via JVM [@teavm]. This browser-based approach enables privacy-preserving local computation with zero installation and without transmitting simulations to external servers. The JVM-based engine offers identical functionality for desktop or workflow integration but with additional performance and added tools for Monte Carlo. Both modes support parallel computation through native threads or web workers. The engine uses ANTLR [@antlr] for parsing the QubecTalk domain-specific language in both Java and JS^[A JS ANTLR visitor facilitating real-time translation between the UI-editor and the code-based editor.]. High-precision arithmetic relies on BigDecimal [@bigdecimal].

![Diagram describing multi-modal execution in which simulations run across different platforms.\label{fig:execution}](KigaliExecution.svg){width="100%"}

# Acknowledgments
We thank Tina Birmpili, Nick Gondek, Ava Hu, Kevin Koy, Douglas McCauley, Alejandro Ramirez-Pabon, Frederico San Martini, Annie Snyder, Suzanne Spencer, and Elina Yuen. Additionally, we thank valued community members as listed at https://kigalisim.org/credits.html as well as National Ozone Units and workshop participants who engaged with the tool during its development. We also thank the developers of our runtime dependencies [@aceeditor; @antlr; @apachecsv; @chartjs; @d3; @papaparse; @prism; @qunit; @tabby; @webpack; @teavm]. Funding from The Eric and Wendy Schmidt Center for Data Science and the Environment at UC Berkeley. No conflicts of interest to disclose. AI assistants used with constrained tasks and strict human review [@claude_ai; @copilot; @intellij_ai; @replit_ai]. Paper prepared with drawio [@drawio].

# References
