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
Actively used internationally, Kigali Sim offers stock and flow modeling of Montreal Protocol-controlled substances. Focusing on the Kigali Amendment which reduces hydrofluorocarbons that contribute to climate change through warming potentials up to thousands of times greater than carbon dioxide, this open source platform aids both atmospheric science and policy development. Supporting a diverse community with heterogeneous programming expertise, this parallelized toolkit interoperates visual no-code rapid model development and a domain-specific language (DSL), democratizing advanced computational tools. Through WebAssembly (WASM) or Java Virtual Machine (JVM), Kigali Sim offers portable, private, and rigorous simulation in support of history's most successful international environmental treaty.

# Statement of Need
Signed by all UN member states, the Montreal Protocol phased out 99% of ozone-depleting substances [@ozone]. Already the most successful international environmental treaty [@montreal_protocol_success], its ambitious 2016 Kigali Amendment extends this multilateral framework to hydrofluorocarbons which contribute significantly to climate change [@kigali_amendment; @contribute]. However, research and policy analysis for these HFCs involves modeling complex economic, technological, energy, and policy interactions [@complex].

## State of field
National Ozone Units (NOUs) and others who possess deep domain knowledge may work with limited resources [@noo]. Currently requiring powerful but closed source [@hfcoutlook] or ad-hoc models [@complex] that serve a valued but distinct role, we contribute new access through open domain-oriented tools for holistic stock and flow simulation [@oses].

## Research impact statement
Serving Article 5 nations [@article5], Implementing Agencies, analysts, researchers, and related organizations, Kigali Sim provides the first open source reusable lifecycle modeling toolkit focused on Montreal Protocol-controlled substances. Simulating emissions, energy, consumption, equipment populations, trade, and policy while following treaty conventions [@mlf_guidelines], more than a dozen nations and supporting organizations co-designed Kigali Sim over more than a year, spanning scientists, analysts, and policy-makers. Participating in many international meetings [@oweg; @excom; @pacificnetwork; @seasianetwork], multiple governments publicly acknowledge contributing to this community project [@credits] which also received media coverage [@nbc].

# Software Design
Kigali Sim's engine supports domain experts in atmospheric science and environmental policy with varied programming expertise through a dual-interface design.

## Flexible engine
Countries and supporting organizations work with varied information from trade records to industry census data to observed emissions. Given diverse methodologies, Kigali Sim pushes information from known to unknown stocks, providing automated unit conversions dependent on equipment properties. As opposed to a unidirectional structure with a single entry-point, this propagates limited user-provided values through substance flows and lifecycles to estimate unmeasured quantities. It then layers complex policy interventions such as permitting and recycling on top of the triangulated "business as usual" scenario.

![Diagram showing data flow through Kigali Sim simulation engine from input data (trade records, bank estimates, or equipment surveys) through stock and flow calculations to output metrics.\label{fig:architecture}](KigaliEngine.svg){width="100%"}

## Dual-interface design
Many Kigali Sim users do not identify as programmers and, reflecting empirical findings that domain experts with "limited programming knowledge" benefit from DSLs [@dsl_evaluation], it forgoes general purpose languages to instead progress from a UI-based editor to DSL code-based authoring. Most simulations can be modified either by the UI-based editor or the code-based editor where changes in one reflect in the other, attempting to bridge preferences and skill sets. Indeed, many community collaborators who identify as non-programmers report starting in the UI-editor but transition to code.

### UI-based authoring
To support beginning programmers, the UI-based point-and-click editor acclimates the user to Kigali Sim's concepts through loop-based design [@loop_based_design; @core_loops] where small GUI-based changes automatically translate to code run for immediate feedback. This web interface progressively exposes functionality through sequenced disclosure [@hayashida_structure; @hayashida_video; @pyafscgap] as an on-ramp into a more open design [@open_world_zelda; @plastics] that familiarizes code.

![Screenshot of the UI-based editor modifying an example simulation.\label{fig:ui_editor}](KigaliEditor.png)

### Code-based authoring
We find some simulations' complexity cumbersome in UI-based authoring. Therefore, our QubecTalk domain-specific language facilitates expression of complex models in human-readable syntax inspired by but distinct to HyperTalk [@smallhypertalk]. Mirrored by the UI-editor, QubecTalk speaks in treaty terminology, translating terms of art into simulations. This also supports uncertainty quantification, conditional logic, and policy stacking. With optional AI assistant compatibility via the llms.txt specification [@llmstxt], users may author scripts in a web-based programming portal [@portal] or outside with direct JVM invocation.

## Limitations and future work
Kigali Sim can model energy consumption but only estimates direct emissions^[Users may calculate indirect emissions using outside energy mix data [@energy_mix].]. Additionally, it applies treaty trade attribution but will only attribute charge prior to equipment sale entirely to either the importer or exporter^[Local assembly can be modeled as domestic production.]. However, it will receive updates as official guidance changes in the future [@mlf_guidelines].

# Implementation
Migrated from an original JavaScript implementation for performance and portability, Kigali Sim runs via WASM^[Zero-install browser-based.] or JVM [@teavm]. Without transmitting simulations to external servers, both enable privacy-preserving parallel local computation with BigDecimal [@bigdecimal] after ANTLR [@antlr] interpretation.

![Diagram describing multi-modal execution in which simulations run across different platforms.\label{fig:execution}](KigaliExecution.svg){width="100%"}

# Acknowledgments
BSD-licensed.

## Thanks
Amanda Anderson-You, Tina Birmpili, Matt Fisher, Ava Hu, Kevin Koy, Douglas McCauley, Alejandro Ramirez-Pabon, Frederico San Martini, Annie Snyder, Suzanne Spencer, Elina Yuen, runtime dependencies [@aceeditor; @antlr; @apachecsv; @chartjs; @colorbrewer; @d3; @papaparse; @prism; @qunit; @tabby; @webpack; @teavm], drawio [@drawio], and valued community members [@credits].

## Funding
Eric and Wendy Schmidt Center for Data Science and the Environment at UC Berkeley. No conflicts.

## AI usage disclosure
As described in-repo, AI used with constrained tasks and strict human review [@claude_ai; @copilot; @intellij_ai; @replit_ai]. No substantial LLM use in original JavaScript implementation.

# References
