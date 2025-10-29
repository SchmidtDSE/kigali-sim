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
Co-created with over a dozen countries and organizations, Kigali Sim offers reusable modeling of substances controlled under the Montreal Protocol. Specifically, this open source platform focuses on the Kigali Amendment which reduces hydrofluorocarbons that contribute to climate change through warming potentials up to thousands of times greater than carbon dioxide, aiding both atmospheric science and policy development through comprehensive stock-flow modeling. Furthermore, supporting a diverse community with heterogeneous programming expertise, this parallelized toolkit interoperates visual no-code rapid model development and an embedded domain-specific language. Running via WebAssembly (WASM) or Java Virtual Machine (JVM), Kigali Sim offers portable private democratized access to rigorous simulation in support of history's most successful international environmental treaty.

# Statement of need
Signed by all UN member states, the Montreal Protocol has successfully phased out 99% of ozone-depleting substances [@ozone]. Already the most successful international environmental treaty [@montreal_protocol_success], its ambitious 2016 Kigali Amendment extends this multilateral framework to hydrofluorocarbons which contribute significantly to climate change [@kigali_amendment; @contribute]. However, related research and policy analysis involves complex economic, technological, and policy interactions [@complex]. As National Ozone Units (NOUs) and others who possess deep domain knowledge may work with limited resources [@noo], the current lack of open source domain-oriented toolkit for holistic stock-flow modeling may inhibit transparent accessible modeling [@oses].

Informed by consulting over a dozen countries and supporting organizations in real-world use by on-the-ground scientists, analysts, and policy-makers, Kigali Sim provides the first open source reusable full lifecycle toolkit addressing Montreal Protocol-controlled substances. It simulates emissions, energy, substance consumption, equipment populations, trade, and policy following conventions [@mlf_guidelines].

# Design
Multiple Article 5 countries [@article5] use Kigali Sim. Spanning NOUs, Implementing Agencies, analysts, researchers, project implementers, and other organizations, it supports domain experts in atmospheric science or environmental policy with heterogeneous programming experience.

## Simulation engine
Some countries and supporting organizations work from trade records and industry surveys while others from bank estimates or observed emissions. The engine propagates user-provided values through substance flows and lifecycles to estimate the unmeasured quantities. It then layers complex policy interventions such as permitting and recycling on top of the "business as usual" scenario.

![Diagram showing data flow through Kigali Sim simulation engine from input data (trade records, bank estimates, or equipment surveys) through stock-flow calculations to output metrics.\label{fig:architecture}](KigaliEngine.svg){width="100%"}

## Dual-Interface Design
We find many Kigali Sim users do not identify as programmers and, reflecting empirical findings that domain experts with "limited programming knowledge" benefit from domain-specific languages (DSL) [@dsl_evaluation], our software progresses from a UI-based editor to DSL code-based authoring. In all cases, simulations execute locally, ensuring privacy.

### UI-based authoring
The UI-based point-and-click editor familiarizes the user with Kigali Sim's concepts, creating a loop-based design [@loop_based_design; @core_loops] in which users make small changes and Kigali Sim automatically translates their GUI inputs to code run for immediate feedback. Note that the web interface first progressively exposes functionality through sequenced disclosure [@hayashida_structure; @hayashida_video; @pyafscgap] as an on-ramp into a more open design [@open_world_zelda; @plastics].

![Screenshot of the UI-based editor modifying an example simulation.\label{fig:ui_editor}](KigaliEditor.png)

### Code-based authoring
We find some simulations' complexity too cumbersome to manage in the UI-based editor. Therefore, our QubecTalk domain-specific language facilitates expression of complex Montreal Protocol simulations in human-readable syntax inspired by but distinct to SmallTalk / HyperTalk [@smallhypertalk]. Mirrored by the UI-editor, QubecTalk speaks in treaty terminology: directly translating terms of art like `cap import to 85% during years 3 to 5` into simulations. This also supports uncertainty quantification, conditional logic, and policy chains where multiple interventions apply sequentially. Users may author scripts in a web-based programming portal [@portal] or third-party editors with direct JVM invocation.

## Flexibility
As diverse methodologies measure stocks, Kigali Sim provides automated unit conversions including those which depend on equipment properties. Additionally, most simulation code can be modified either by the UI-based editor or the code-based editor where changes in one reflect in the other, attempting to bridge preferences and skill sets. Indeed, many community collaborators who identify as non-programmers report starting in the UI-editor but transition to code.

## Limitations and future work
Leaving the following for future work, Kigali Sim:

 - Can model energy consumption but only estimates direct emissions^[Users may combine exported data with energy mix information for indirect emissions [@energy_mix].].
 - Applies treaty trade attribution but will only attribute charge prior to equipment sale entirely to either the importer or exporter^[Local assembly can be modeled as domestic production.].
 - Complex features like formula-based variable servicing / retirement hazard rates may require code-based authoring.

Kigali Sim will receive updates as official guidance changes in the future [@mlf_guidelines].

# Implementation
Kigali Sim runs browser-based via WASM or via JVM [@teavm]. Without transmitting simulations to external servers, both enable privacy-preserving zero installation local computation, BigDecimal [@bigdecimal], and parallelization. The engine uses ANTLR [@antlr] for parsing QubecTalk code in both Java and JavaScript^[A Javascript-based ANTLR visitor facilitating real-time translation between the UI-editor and the code-based editor.].

![Diagram describing multi-modal execution in which simulations run across different platforms.\label{fig:execution}](KigaliExecution.svg){width="100%"}

# Acknowledgments
BSD-licensed. Thanks Tina Birmpili, Matt Fisher, Nick Gondek, Ava Hu, Kevin Koy, Douglas McCauley, Alejandro Ramirez-Pabon, Frederico San Martini, Annie Snyder, Suzanne Spencer, Elina Yuen, runtime dependencies [@aceeditor; @antlr; @apachecsv; @chartjs; @d3; @papaparse; @prism; @qunit; @tabby; @webpack; @teavm], and valued community members listed at https://kigalisim.org/credits.html. Funding from the Eric and Wendy Schmidt Center for Data Science and the Environment at UC Berkeley. No conflicts. AI assistants used with constrained tasks and strict human review [@claude_ai; @copilot]. Early efforts also tried other AIs for mechanical edits [@intellij_ai; @replit_ai]. Paper uses drawio [@drawio].

# References
