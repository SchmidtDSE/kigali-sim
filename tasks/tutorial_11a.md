# Task: Add Tutorial 11a — Agentic AI (JVM)

Closes #770.

This task adds a new tutorial **Tutorial 11a: Agentic AI (JVM)** to the Kigali Sim user guide. The tutorial teaches users how to set up an agentic AI workflow in which an AI assistant (with code execution enabled) autonomously runs the Kigali Sim JVM, iterates on a model, and validates its own outputs — while the user reviews decisions at each stage.

The tutorial follows the same "a-variant" pattern used by tutorials 3a, 4a, 6a: it is a companion to an existing tutorial (Tutorial 11: AI Coding Assistants) that extends its topic into a more specific use-case.

---

## Component 1 — Create `docs/guide/tutorial_11a.html`

Create a new HTML tutorial file at `docs/guide/tutorial_11a.html`. It must follow the structural conventions of existing tutorials exactly (see `tutorial_11.html`, `tutorial_03a.html`, and `tutorial_16.html` as primary references).

### Required HTML conventions

- `<html lang="en-GB">` root element.
- `<head>` must include:
  - `<title>Tutorial 11a: Agentic AI (JVM)</title>`
  - `<link href="/guide/guide.css?v=EPOCH" rel="stylesheet" type="text/css" />`
  - Prism links are **not** needed (no inline code blocks requiring syntax highlighting).
- Skip link: `<a href="#main" class="skip-link">Skip to main content</a>`
- Header: `This tutorial is part of the <a href="/guide">guide</a> for the <a href="/">Kigali Sim</a>.`
- `<main id="main">` wraps all content.
- `<h1>Tutorial 11a: Agentic AI (JVM)</h1>` followed immediately by a one-line description.
- Note that this is the agentic variant (mirrors how 3a notes it is the AI variant of tutorial 3).
- A `<details><summary>Contents</summary>` TOC block with anchor links to all sections.
- All top-level sections use `<section class="major" id="..."><h2>...</h2>`.
- Sub-sections (if any) use `<section id="..."><h3>...</h3>` inside the major section.
- Footer:
  ```html
  <footer>
    <hr />
    <p><em>This tutorial is part of the Artificial Intelligence series demonstrating AI-assisted workflows using Kigali Sim. <a href="md/tutorial_11a.md">View Markdown version</a></em></p>
  </footer>
  ```

### Sections and content

**Subtitle** (directly under `<h1>`, before TOC):
> Using AI agents to autonomously run and iterate on simulations through the JVM.

**Note** (immediately after subtitle, before TOC):
> `<strong>Note that this is the agentic / JVM variant of the AI tutorial series.</strong>` You can find the browser-based AI coding assistant tutorial at `<a href="/guide/tutorial_11.html">Tutorial 11</a>`.

**Contents TOC** — anchor links to: `#motivation`, `#prerequisites`, `#project-setup`, `#historic-data`, `#forward-projection`, `#policy-modeling`, `#reviewing-outputs`, `#conclusion`, `#next-steps`.

---

#### Section: `motivation` — Motivation

AI assistants can not only help write code but, when equipped with code-execution capabilities, can act as **agents**: reading data, running simulations, evaluating outputs, and proposing iterative refinements — all autonomously. This tutorial shows how to set up such a workflow using the Kigali Sim JVM (command-line) interface.

Key principle to communicate clearly: *AI assistants can help with execution, but the decision-making remains in your hands.* The user must review outputs at each stage and must not let the agent run ahead unchecked.

Reference `<a href="/guide/tutorial_16.html">Tutorial 16</a>` as the prerequisite that introduced the command-line interface.

---

#### Section: `prerequisites` — Prerequisites

Users need:

1. An AI assistant with **code execution** capabilities enabled (e.g. Claude with the "Run code" tool, Mistral with code execution, or a similar agent capable of running shell commands and creating files). Mention that some plans / tiers of these products may not include code execution.
2. The Kigali Sim **fat JAR** (`kigalisim-fat.jar`) downloaded locally — link to the download at the bottom of `https://kigalisim.org` or via `<a href="/kigalisim-fat.jar" download>direct download</a>`.
3. Java 21+ installed (reference `<a href="https://adoptium.net">Adoptium</a>`).
4. The assistant must be able to access `https://kigalisim.org` (not blocked by organisation firewall or assistant settings). Tell users to allowlist `kigalisim.org` if needed.

Include a note about privacy: the simulation data and any project documents will be sent to the AI provider. Users should review the AI provider's privacy policy.

---

#### Section: `project-setup` — Project Setup

Agentic workflows work best when the AI has structured context about the problem. The user should create a short **project document** (plain text or Markdown) that the agent can read. Describe what the document should contain:

- Country name and sector (e.g. "ABC Country, Domestic Refrigeration").
- Data sources available (e.g. spreadsheet columns: Socioeconomics sheet with GDP/population, Metadata sheet with substance GWPs, Census sheet with equipment population estimates, Import/Export sheet).
- Modelling constraints (e.g. year range, substances in scope such as HFC-134a and R-600a, known policy targets).
- Where the JAR file is located and what output CSV to write.

Example prompt to start the agent:

```
Hello! Please read https://kigalisim.org/llms-full.txt?v=20260302 to understand Kigali Sim. Then read my project document [attach file]. I will be giving you tasks that involve running the Kigali Sim JAR at [path/to/kigalisim-fat.jar]. Please confirm you understand the setup before we begin.
```

Remind users that they should check that the agent has correctly read and understood the project document before proceeding.

---

#### Section: `historic-data` — Step 1: Historic Data Visualisation

Ask the agent to:
1. Read the relevant data sheet (CSV or attached file).
2. Write and run a QubecTalk simulation that reproduces historic consumption as closely as possible.
3. Export results to a CSV.
4. Summarise what it found and flag any anomalies.

Example prompt:

```
Please read the attached data file. Using the Kigali Sim JAR, build an initial simulation for [Substance] in [Application] that reproduces the historic consumption data from [year] to [year]. Run the simulation and show me the CSV output. Flag any data points that look unusual.
```

Include a warning: *"Agentic workflows can move fast — you have to be careful to ensure the agent doesn't move too fast!"* The user must review the CSV output before moving on.

---

#### Section: `forward-projection` — Step 2: Forward Projection

Ask the agent to:
1. Use GDP/population growth data to project future consumption for HFC-134a.
2. For R-600a (or other alternatives), use curve-fitting based on historical trends.
3. Run both projections and compare.

Example prompt:

```
Now please extend the simulation to [future year] using the GDP and population projections in the Socioeconomics sheet. For HFC-134a, tie growth to GDP. For R-600a, fit a curve to the historical trend. Run the simulation and present the results. Pause and wait for my review.
```

Emphasise the **pause and review** step. The user should examine the projection logic before accepting it.

---

#### Section: `policy-modeling` — Step 3: Policy Modeling

Ask the agent to add policy scenarios:
1. A cap on HFC-134a sales displacing R-600a.
2. A recycling program.
3. Run the policy simulation alongside Business as Usual.

Example prompt:

```
Please now add two policy scenarios on top of the Business as Usual:
1. A cap on HFC-134a sales of 80% starting in [year], displacing to R-600a.
2. A recycling program starting in [year] capturing 20% at servicing with 50% yield loss.
Run both and compare with BAU. Pause after running.
```

---

#### Section: `reviewing-outputs` — Reviewing and Validating Outputs

Before concluding, the user must validate that:
- Substance totals and units are correct (check `mt / year` vs `kg / year`).
- Policy deltas between scenarios make intuitive sense.
- The simulation years and stanza structure are correct.

Suggest asking the agent to summarise its own reasoning:

```
Please explain the key assumptions you made in the model and flag anything you are uncertain about.
```

---

#### Section: `conclusion` — Conclusion

Summarise what was demonstrated:
- Agentic AI workflow running the JVM autonomously.
- Iterative simulation refinement.
- The importance of human review at every stage.

Reference `<a href="/guide/tutorial_11.html">Tutorial 11</a>` for browser-based AI assistance and `<a href="/guide/tutorial_16.html">Tutorial 16</a>` for the command-line interface more broadly.

---

#### Section: `next-steps` — Next Steps

Navigation footer links:
```
<a href="/guide/tutorial_11.html">Previous: Tutorial 11</a> |
<a href="/guide">Return to Guide Index</a> |
<a href="/guide/tutorial_12.html">Next: Tutorial 12</a>
```

---

## Component 2 — Update `docs/guide/index.html`

In the existing `<section id="artificial-intelligence" class="no-border">` block, add a new entry for tutorial 11a **after** the existing tutorial 11 entry:

```html
<div><a href="/guide/tutorial_11a.html"><strong>Tutorial 11a:</strong> Agentic AI (JVM)</a> - Using AI agents to autonomously run and iterate on simulations through the JVM</div>
```

No other changes to index.html are needed.

---

## Component 3 — Update `docs/guide/tutorial_11.html`

Tutorial 11a requires an AI assistant with code-execution capabilities, which not all users will have. The next-steps section of tutorial_11.html must give users a clear path whether or not they have access to an agentic AI.

In the existing `<section class="major" id="next-steps">` of `tutorial_11.html`, update the navigation line at the bottom. Currently it reads:

```html
<a href="/guide/tutorial_10.html">Previous: Tutorial 10</a> |
<a href="/guide">Return to Guide Index</a> |
<a href="/guide/tutorial_12.html">Next: Tutorial 12</a>
```

Change to include both the 11a path and a skip-to-12 path:

```html
<a href="/guide/tutorial_10.html">Previous: Tutorial 10</a> |
<a href="/guide">Return to Guide Index</a> |
<a href="/guide/tutorial_11a.html">Next: Tutorial 11a</a> |
<a href="/guide/tutorial_12.html">Skip to Tutorial 12</a>
```

Also, in the body of the `next-steps` section, replace the final paragraph (currently ending with the Tutorial 16 reference) with text that presents both paths explicitly — one for users who have an agentic AI and one for users who do not:

> You've learned how to leverage AI assistants to accelerate your simulation development workflow. If your AI assistant supports code execution (the ability to run shell commands and create files), <a href="/guide/tutorial_11a.html">Tutorial 11a</a> explores agentic workflows where the AI runs the Kigali Sim JVM autonomously. If you do not have access to an AI with code execution, you can <a href="/guide/tutorial_12.html">skip ahead to Tutorial 12</a> and continue with advanced modeling topics.

---

## Component 4 — Update `.github/workflows/build.yaml`

Tutorial 11a is an instructional tutorial that does not require a standalone example QTA file (similar to tutorials 11, 16, and the "a" variant tutorials 3a, 4a, 6a — none of which have QTA files in the deploy step). The Markdown version is generated automatically by the `generateGuideMd` CI job (which processes all `docs/guide/*.html` files). Therefore, **no changes to `build.yaml` are needed**.

If a future decision is made to add a companion `tutorial_11a.qta` file, it should be:
1. Created at `examples/tutorial_11a.qta`
2. Added to the `deployPrep` job's "Copy tutorial QTA files" step alongside the other tutorials

---

## Validation checklist

Before considering this task complete:

- [ ] `docs/guide/tutorial_11a.html` exists and validates as well-formed HTML.
- [ ] All internal links in tutorial_11a.html resolve (guide, tutorial_11, tutorial_12, tutorial_16, kigalisim-fat.jar download).
- [ ] `docs/guide/index.html` lists tutorial_11a in the Artificial Intelligence section.
- [ ] `docs/guide/tutorial_11.html` next-steps includes both a link to tutorial_11a ("Next") and a "Skip to Tutorial 12" link for users without agentic AI capabilities.
- [ ] No changes to `build.yaml` are needed (markdown is auto-generated).
- [ ] The tutorial follows WCAG 2.0 AA conventions consistent with the rest of the guide (skip link, semantic headings, no colour-only indicators).
- [ ] The tutorial text does not use the word "JavaScript" (project uses "ECMAScript") — not relevant here but included for completeness.
