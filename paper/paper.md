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
Kigali Sim offers stock and flow modeling of Montreal Protocol-controlled substances. This includes hydrofluorocarbons that contribute to climate change through warming potentials much greater than carbon dioxide. Supporting a diverse community with heterogeneous programming expertise, this parallelized toolkit democratizes advanced computational tools by interoperating visual no-code rapid model development and a domain-specific language (DSL). Either through WebAssembly (WASM) or Java Virtual Machine (JVM), Kigali Sim affords portable, private, and rigorous simulation in support of history's most successful international environmental treaty, aiding both atmospheric science and policy development.

# Statement of Need
Signed by all UN member states, the Montreal Protocol phased out 99% of ozone-depleting substances [@ozone]. Already the most successful international environmental treaty [@montreal_protocol_success], its ambitious 2016 Kigali Amendment extends this multilateral framework to hydrofluorocarbons which contribute significantly to climate change [@kigali_amendment; @contribute]. However, research and policy analysis for these substances involves modeling complex economic, technological, energy, and policy interactions [@complex].

## State of field
On the current public market, only the proprietary HFC Outlook offers a reusable full lifecycle model for the Montreal Protocol [@hfcoutlook]. Otherwise, organizations typically build private ad-hoc models [@complex].

## Contribution
Kigali Sim provides the first open source reusable lifecycle modeling toolkit focused on Montreal Protocol-controlled substances. The Multilateral Fund for the Implementation of the Montreal Protocol (MLF) met the University of California Schmidt Center for Data Science and Environment (DSE) during a 2023 workshop for their global plastics pollution treaty interactive simulation tool [@plasticssci]. From that initial conversation, co-design sessions in 2024 sought to consider game design-inspired techniques for participatory data science similar to that prior project [@plasticsgame] but within the Montreal Protocol's science and policy context. Kigali Sim's design ultimately focuses on modeling emissions, energy, consumption, equipment populations, trade, and policy while following treaty conventions [@mlf_guidelines]. Finally, Kigali Sim was implemented by DSE in collaboration with MLF as well as users from various governments and supporting organizations [@credits].

## Research impact statement
Kigali Sim serves Article 5 nations [@article5], Implementing Agencies, analysts, researchers, and related organizations. This includes National Ozone Units (NOUs) with limited resources [@noo] for whom developing ad-hoc models may be burdensome. Though usage remains anonymized, more than a dozen nations and supporting organizations co-designed Kigali Sim over more than a year. This effort spanned scientists, analysts, and policy-makers. Participating in many international meetings [@oweg; @excom; @pacificnetwork; @seasianetwork], multiple governments publicly acknowledge contributing to this community project [@credits]. This open source effort also received media coverage [@nbc].

# Implementation
Migrated from an original JavaScript implementation for performance and portability, Kigali Sim runs via JVM or browser-based WASM through TeaVM [@teavm]. Without transmitting simulations to external servers, both modalities enable privacy-preserving parallel local computation with BigDecimal [@bigdecimal] after ANTLR [@antlr] interpretation.

![Diagram describing multi-modal execution in which simulations run across different platforms with simulation results displayed via a web browser.\label{fig:execution}](KigaliExecution.svg){width="100%"}

# Software Design
Kigali Sim's engine supports domain experts in atmospheric science and environmental policy with varied programming expertise through a dual-interface design.

## Flexible engine
Countries and supporting organizations work with varied information from trade records to industry census data. Kigali Sim pushes information from known to unknown stocks, providing automated unit conversions dependent on equipment properties. As opposed to a unidirectional structure with a single entry-point, this propagates limited user-provided values through substance flows and lifecycles to estimate unmeasured quantities. It then layers complex policy interventions such as permitting and recycling on top of the triangulated "business as usual" scenario.

![Bi-directional graph diagram where Kigali Sim can output an unknown value by traversing this graph using equipment properties to reach known values. All values can be outputs except exports which cannot be inferred by the other values from the same country under treaty trade attribution.\label{fig:architecture}](KigaliEngine.svg){width="100%"}

## Dual-interface design
Most simulations can be modified either by the UI-based editor or the code-based editor where changes in one reflect in the other, attempting to bridge preferences and skill sets.

### UI-based authoring
To support beginning programmers, the UI-based point-and-click editor acclimates users to Kigali Sim. This web interface exposes functionality through a 4-step Hayashida tutorial [@hayashida_structure; @hayashida_video], sequenced disclosure building up the tool UI elements over time [@pyafscgap]:

 - **Introduction**: User sees vocabulary in the app starting state in which many controls are disabled or hidden but where the application specification button introduces the primary loop [@core_loops].
 - **Development**: Specification of consumption offers first modeling decisions.
 - **Twist**: The interface reveals that multiple scenarios can run with different policies, introducing the secondary loop.
 - **Conclusion**: Specification of policies continues using the mechanics first introduced in development.

This sequence runs if the user does not have a simulation loaded in the tool when opening the app. This includes a user visiting for the first time. GUI-based changes automatically translate to code run for immediate feedback and eventual transition to code-based authoring.

![Screenshot of the UI-based editor modifying an example simulation.\label{fig:ui_editor}](KigaliEditor.png)

### Code-based authoring
Many Kigali Sim users do not identify as programmers and, reflecting empirical findings that domain experts with "limited programming knowledge" benefit from DSLs [@dsl_evaluation], we created the QubecTalk domain-specific language to facilitate expression of complex models in human-readable syntax inspired by but distinct from HyperTalk [@smallhypertalk]. Mirrored by the UI-editor, QubecTalk speaks in treaty terminology, translating terms of art into simulations. This also supports uncertainty quantification, conditional logic, and policy stacking. With optional AI assistant compatibility via the llms.txt specification [@llmstxt], users may author scripts in a web-based programming portal [@portal] or outside with direct JVM invocation.

## Limitations and future work
Kigali Sim can model energy consumption but only estimates direct emissions. Users may calculate indirect emissions using outside energy mix data [@energy_mix]. Additionally, it embodies Montreal Protocol definitions such as trade attribution rules and it will receive updates as official guidance changes in the future [@mlf_guidelines]. Therefore, substances not subject to Montreal Protocol accounting norms are out of scope.

# Acknowledgments
BSD-licensed.

## Thanks
Thank you to Amanda Anderson-You, Tina Birmpili, Matt Fisher, Ava Hu, Kevin Koy, Douglas McCauley, Alejandro Ramirez-Pabon, Frederico San Martini, Annie Snyder, Suzanne Spencer, and Elina Yuen. Thank you to runtime dependencies:

 - Ace Editor [@aceeditor]
 - ANTLR [@antlr]
 - Apache CSV [@apachecsv]
 - Chart.js [@chartjs]
 - ColorBrewer [@colorbrewer]
 - D3 [@d3]
 - Papa Parse [@papaparse]
 - Prism.js [@prism]
 - QUnit [@qunit]
 - Tabby [@tabby]
 - Webpack [@webpack]
 - TeaVM [@teavm].

Also, thank you to drawio [@drawio] and valued community members [@credits].

## Funding
Ongoing funding and maintenance provided by the Eric and Wendy Schmidt Center for Data Science and the Environment at UC Berkeley. We have no conflicts to disclose.

## AI usage disclosure
As described in-repo, AI providers used with constrained tasks and strict human review:

 - Claude [@claude_ai]
 - Copilot [@copilot]
 - IntelliJ [@intellij_ai]
 - Replit [@replit_ai].

No substantial LLM use in original JavaScript implementation.

# References
