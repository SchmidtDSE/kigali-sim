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
date: 2026-01-05
bibliography: paper.bib
---

# Summary
With active international use, Kigali Sim offers reusable stock and flow modeling of Montreal Protocol-controlled substances. Focusing on the Kigali Amendment which reduces hydrofluorocarbons that contribute to climate change through warming potentials up to thousands of times greater than carbon dioxide, this open source platform aids both atmospheric science and policy development. Supporting a diverse community with heterogeneous programming expertise, this parallelized toolkit interoperates visual no-code rapid model development and a domain-specific language (DSL), democratizing advanced computational tools. Through WebAssembly (WASM) or Java Virtual Machine (JVM), Kigali Sim offers portable, private, and rigorous simulation in support of history's most successful international environmental treaty.

# Statement of need
Signed by all UN member states, the Montreal Protocol successfully phased out 99% of ozone-depleting substances [@ozone]. Already the most successful international environmental treaty [@montreal_protocol_success], its ambitious 2016 Kigali Amendment extends this multilateral framework to hydrofluorocarbons which contribute significantly to climate change [@kigali_amendment; @contribute]. However, related research and policy analysis involves considering complex economic, technological, and policy interactions [@complex], typically through closed source ad-hoc models. National Ozone Units (NOUs) and others who possess deep domain knowledge may work with limited resources [@noo] and the current lack of open source domain-oriented tools for holistic stock and flow simulation may inhibit transparent accessible modeling [@oses].

Serving Article 5 nations [@article5], Implementing Agencies, analysts, researchers, and related organizations, Kigali Sim therefore provides the first open reusable lifecycle modeling toolkit focused on Montreal Protocol-controlled substances. Co-created with an international community, on-the-ground scientists, analysts, and policy-makers inform its flexible design. With either code or UI-based editing, it simulates emissions, energy, substance consumption, equipment populations, trade, and policy either in browser or JVM while following treaty conventions [@mlf_guidelines].

# Research Impact Statement
Participating in official international policy-relevant treaty meetings [@oweg; @excom; @pacificnetwork; @seasianetwork] with more than a year of co-design with over a dozen nations and supporting organizations, multiple governments publicly acknowledge use of the software [@credits] which has received news media coverage [@nbc].

# Software Design
Kigali Sim's engine supports domain experts in atmospheric science and environmental policy with varied programming expertise through a dual-interface design.

## Simulation engine
Countries and supporting organizations work with varied data from trade records to industry censuses to observed emissions. Therefore, as opposed to a unidirectional structure, this engine propagates user-provided values from any location in the system through substance flows and lifecycles to estimate the unmeasured quantities. It then layers complex policy interventions such as permitting and recycling on top of the "business as usual" scenario.

![Diagram showing data flow through Kigali Sim simulation engine from input data (trade records, bank estimates, or equipment surveys) through stock and flow calculations to output metrics.\label{fig:architecture}](KigaliEngine.svg){width="100%"}

## Dual-Interface Design
We find many Kigali Sim users do not identify as programmers. Reflecting empirical findings that domain experts with "limited programming knowledge" benefit from domain-specific languages [@dsl_evaluation], our software progresses from a UI-based editor to DSL code-based authoring^[Instead of a general purpose language].

### UI-based authoring
To support those new to programming, the UI-based point-and-click editor acclimates the user to Kigali Sim's concepts through loop-based design [@loop_based_design; @core_loops] where small GUI-based changes automatically translate to code run for immediate feedback. This web interface progressively exposes functionality through sequenced disclosure [@hayashida_structure; @hayashida_video; @pyafscgap] as an on-ramp into a more open design [@open_world_zelda; @plastics] that familiarizes code.

![Screenshot of the UI-based editor modifying an example simulation.\label{fig:ui_editor}](KigaliEditor.png)

### Code-based authoring
We find some simulations' complexity cumbersome in UI-based authoring. Therefore, our QubecTalk domain-specific language facilitates expression of complex Montreal Protocol simulations in human-readable syntax inspired by but distinct to HyperTalk [@smallhypertalk]. Mirrored by the UI-editor, QubecTalk speaks in treaty terminology: directly translating terms of art like `cap import to 85% during years 2025 to 2035` into simulations. This also supports uncertainty quantification, conditional logic, and policy stacking. With optional AI assistant compatibility via the llms.txt specification [@llmstxt], users may author scripts in a web-based programming portal [@portal] or third-party editors with direct JVM invocation.

## Flexibility
As diverse methodologies measure stocks, Kigali Sim pushes information from known to unknown parts of the system, providing automated unit conversions including those which depend on equipment properties. Additionally, most simulation code can be modified either by the UI-based editor or the code-based editor where changes in one reflect in the other, attempting to bridge preferences and skill sets. Indeed, many community collaborators who identify as non-programmers report starting in the UI-editor but transition to code.

## Limitations and future work
Leaving the following for future work, Kigali Sim:

 - Can model energy consumption but only estimates direct emissions^[Users may manually calculate indirect emissions using exported and energy mix data [@energy_mix].].
 - Applies treaty trade attribution but will only attribute charge prior to equipment sale entirely to either the importer or exporter^[Local assembly can be modeled as domestic production.].
 - Complex features like formula-based variable servicing / retirement hazard rates may require code-based authoring.

It will receive updates as official guidance changes in the future [@mlf_guidelines].

# Implementation
Migrated from an original JavaScript implementation for performance and portability, Kigali Sim runs browser-based via WASM or via JVM [@teavm]. Without transmitting simulations to external servers, both enable privacy-preserving zero-installation parallel local computation with BigDecimal [@bigdecimal]. ANTLR [@antlr] parses QubecTalk code in both Java and JavaScript^[A JS-based ANTLR visitor facilitating real-time translation between the UI-editor and the code-based editor.].

![Diagram describing multi-modal execution in which simulations run across different platforms.\label{fig:execution}](KigaliExecution.svg){width="100%"}

# Acknowledgments
BSD-licensed.

## Thanks
Amanda Anderson-You, Tina Birmpili, Matt Fisher, Ava Hu, Kevin Koy, Douglas McCauley, Alejandro Ramirez-Pabon, Frederico San Martini, Annie Snyder, Suzanne Spencer, Elina Yuen, runtime dependencies [@aceeditor; @antlr; @apachecsv; @chartjs; @colorbrewer; @d3; @papaparse; @prism; @qunit; @tabby; @webpack; @teavm], drawio [@drawio], and valued community members [@credits].

## Funding
Eric and Wendy Schmidt Center for Data Science and the Environment at UC Berkeley. No conflicts.

## AI usage disclosure
AI used with constrained tasks and strict human review [@claude_ai; @copilot; @intellij_ai; @replit_ai]. No substantial LLM use in original JavaScript implementation.

# References
