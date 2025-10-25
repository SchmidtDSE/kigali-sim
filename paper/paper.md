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
    orcid: 0009-0000-1609-6244
    affiliation: 1
  - name: Ciera Martinez
    orcid: 0000-0003-4296-998X
    affiliation: 1
affiliations:
  - name: Schmidt Center for Data Science and Environment, University of California, Berkeley, California, United States of America
    index: 1
  - name: Secretariat of the Multilateral Fund for the Implementation of the Montreal Protocol, United Nations Environment Programme
    index: 2
date: 24 October 2025
bibliography: paper.bib
---

# Summary
Co-created with over a dozen countries and organizations, Kigali Sim offers reusable modeling of substances controlled under the Montreal Protocol. Specifically, this open source platform focuses on the Kigali Amendment which reduces hydrofluorocarbons that contribute to climate change through warming potentials up to thousands of times greater than carbon dioxide. This parallelized engine aids both atmospheric science research and policy development through comprehensive stock-flow modeling of substances, helping facilitate evidence-based policymaking. Furthermore, supporting a diverse community with heterogeneous programming expertise, this community-built toolkit interoperates visual no-code rapid model development and an embedded domain-specific language. Running via WebAssembly (WASM) or Java Virtual Machine (JVM), Kigali Sim offers portable privacy-preserving democratized access to rigorous simulation in support of history's most successful international environmental treaty.

# Statement of need
Signed by all UN member states and widely recognized as the most successful international environmental treaty [@montreal_protocol_success], the Montreal Protocol has successfully phased out 99% of ozone-depleting substances [@ozone]. Its ambitious 2016 Kigali Amendment extends this multi-lateral framework to hydrofluorocarbons which contribute significantly to climate change [@kigali_amendment; @contribute]. However, research and policy analysis for these controlled substances often involves simulating complex interactions [@complex] including economic, physical, and policy factors through proprietary or single-use models. Without an open source domain-oriented toolkit for holistic stock-flow modeling, National Ozone Units and others who possess deep domain knowledge may work with limited resources [@noo] which may include lack of easy access to computational or programming expertise in simulating these complex systems.

Kigali Sim provides the first open source reusable toolkit for this full lifecycle modeling of Montreal Protocol-controlled substances. It simulates emissions, power consumption, substance consumption, and in-country bank / reservoirs. Following analysis conventions, this reusable engine also accounts for complex policy effects like for permitting and recycling [@mlf_guidelines]. Furthermore, co-created through collaboration with over a dozen countries and supporting organizations, this community-built toolkit emerges from real-world use to support on-the-ground scientists. Demonstrating practical utility, multiple countries are actively using this platform.

# Design
Kigali Sim serves a diverse ecosystem spanning National Ozone Units, Implementing Agencies, analysts, researchers, and other organizations. Many practitioners are domain experts in atmospheric science or environmental policy with heterogeneous programming experience. Kigali Sim supports their work through portable simulation in a dual-interface design.

## Simulation engine
Countries and supporting organizations approach modeling with divergent data availability where some may work from trade records and industry surveys while others from bank estimates or observed emissions. The engine propagates known values provided by the user through substance flows and lifecycles to estimate the numbers not provided. It then layers complex policy interventions such as permitting and recycling on top of the "business as usual" scenario.

![Diagram showing data flow through Kigali Sim simulation engine from input data (trade records, bank estimates, or equipment surveys) through stock-flow calculations to output metrics.\label{fig:architecture}](KigaliEngine.svg){width="100%"}

## Dual-Interface Design
We find many Kigali Sim users do not identify as programmers and, reflecting empirical findings that domain experts with "limited programming knowledge" benefit from domain-specific languages (DSL) [@dsl_evaluation], our software starts with a UI-based editor with progression to DSL code-based authoring. In all cases, simulations execute locally, ensuring privacy.

### UI-based authoring
The UI-based editor offers point-and-click interaction while familiarizing the user with Kigali Sim's concepts, creating a loop-based design [@loop_based_design; @core_loops] in which users make small changes and Kigali Sim automatically translates their GUI inputs to code run for immediate feedback. Note that the web interface first progressively exposes functionality through sequenced disclosure [@hayashida_structure; @hayashida_video; @pyafscgap] as an on-ramp into a more open design [@open_world_zelda; @plastics].

![Screenshot of the UI-based editor modifying an example simulation.\label{fig:ui_editor}](KigaliEditor.png)

### Code-based authoring
Our QubecTalk domain-specific language facilitates expression of complex Montreal Protocol simulations in human-readable syntax inspired by but distinct to SmallTalk / HyperTalk [@smallhypertalk]. Mirrored by the UI-editor, QubecTalk speaks using familiar treaty terminology, allowing direct translation of terms of art like `cap import to 85% during years 3 to 5` into executable simulations. This also supports uncertainty quantification (`sample normally from mean of X std of Y`), conditional logic, and policy chains where multiple interventions apply sequentially. Users may author scripts in a web-based programming portal [@portal] or third-party editors with direct JVM invocation.

## Flexibility
As stocks may be measured using diverse methodologies, QubecTalk and the UI-based editor provide automated unit conversions including those which depend on equipment properties such as charge levels. Additionally, most simulation code can be modified either by the UI-based editor or the code-based editor where changes in one reflect in the other, attempting to bridge preferences and skill sets. Indeed, many community collaborators report starting in the UI-editor but transitioning to code over time.

## Limitations
Leaving the following for future work, Kigali Sim:

 - Can model energy consumption but only estimates direct emissions though users may combine exported data with energy mix information for indirect emissions [@energy_mix].
 - Asks users to provide retirement and servicing as amortized rates as modelers typically do not know the specific age distribution of equipment, similar to IPCC Tier 1 [@ipcc_tiers].
 - Provides automatic lookup of some values but allows for override with country-specific parameters, similar to IPCC Tier 2 [@ipcc_tiers].
 - Applies treaty trade attribution but will only attribute charge prior to equipment sale entirely to either the importer or exporter^[Local assembly can be modeled as domestic production.].

# Implementation
Kigali Sim runs in two modes: browser-based via WASM and local via JVM [@teavm]. Without transmitting simulations to external servers, this browser-based approach enables privacy-preserving zero installation local computation. The JVM-based engine offers desktop or workflow integration but with additional performance and added tools for Monte Carlo. Both modes support BigDecimal and parallelization [@bigdecimal]. The engine uses ANTLR [@antlr] for parsing QubecTalk code in both Java and JS^[A JS ANTLR visitor facilitating real-time translation between the UI-editor and the code-based editor.].

![Diagram describing multi-modal execution in which simulations run across different platforms.\label{fig:execution}](KigaliExecution.svg){width="100%"}

# Acknowledgments
BSD-licensed. Thanks Tina Birmpili, Nick Gondek, Ava Hu, Kevin Koy, Douglas McCauley, Alejandro Ramirez-Pabon, Frederico San Martini, Annie Snyder, Suzanne Spencer, and Elina Yuen as well as valued community members listed at https://kigalisim.org/credits.html. We also thank our runtime dependencies [@aceeditor; @antlr; @apachecsv; @chartjs; @d3; @papaparse; @prism; @qunit; @tabby; @webpack; @teavm]. Funding from the Eric and Wendy Schmidt Center for Data Science and the Environment at UC Berkeley. No conflicts of interest. AI assistants used with constrained tasks and strict human review [@claude_ai; @copilot; @intellij_ai; @replit_ai]. Paper uses drawio [@drawio].

# References
