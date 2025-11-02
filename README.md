# Kigali Sim
Open source engine for simulating substances, applications, and policies relevant to the Montreal Protocol.

[![Project Status: Active – The project has reached a stable, usable state and is being actively developed.](https://www.repostatus.org/badges/latest/active.svg)](https://www.repostatus.org/#active) [![BSD Licensed](https://img.shields.io/badge/License-BSD_3--Clause-blue.svg)](https://opensource.org/licenses/BSD-3-Clause) [![Build](https://github.com/SchmidtDSE/kigali-sim/actions/workflows/build.yaml/badge.svg?branch=deploy)](https://github.com/SchmidtDSE/kigali-sim/actions/workflows/build.yaml) ![Temurin 21 or higher](https://img.shields.io/badge/Java-Temurin%2021%2B-blue)

<br>

## Purpose
This reusable toolkit models substances and equipment related to Montreal Protocol and the Kigali Amendment including those with a high global warming potential. 

**What it does**: This open source toolkit provides a simulation engine and related software for modeling substances, applications, and policies related to the Montreal Protocol. It focuses on hydrofluorocarbons and their alternatives within the Kigali Amendment. It supports a foundational business as usual simulation as well as "stacking" policy simulations on top of that baseline. It simulates emissions, energy, substance consumption, equipment populations, and trade longitudinally in service of research and policy activities like Kigali Amendment Implementation Plans (KIPs).

**How it does it**: This platform provides practical flexibility in stock and flow modeling. It supports user-defined structures in terms of applications like commercial refrigeration and substances like HFC-134a. It supports both UI-based and code-based editing within the QubecTalk domain specific language (DSL) where these two options interoperate between each other. For more information about the design of Kigali Sim including why we made QubecTalk, see [our paper draft](https://github.com/SchmidtDSE/kigali-sim/blob/main/paper/paper.md).

**Where it does it**: This portable execution can happen through a web-based bespoke IDE or outside the browser in the command line or within larger workflows. It is available running in WebAssembly (WASM) or on the Java Virtual Machine (JVM). It can also run with parallelization and conduct probabilistic simulation via Monte Carlo.

**Who it serves**: This scientific package supports researchers working on stock and flow modeling of these important substances as well as policy makers. With ongoing [Article 5](https://ozone.unep.org/treaties/montreal-protocol/articles/article-5-special-situation-developing-countries) nation usage, Kigali Sim was co-created in consultation with over a dozen countries and supporting organizations. See [our credits page](https://kigalisim.org/credits.html). We recognize that our community includes those of varied backgrounds in programming and [AI assistance is available](https://github.com/SchmidtDSE/kigali-sim?tab=readme-ov-file#llm-assistants).

<br>

## Quickstart
Make a simulation in 3 minutes! View our [quickstart video](https://vimeo.com/1116227339?share=copy&fl=sv&fe=ci). Prefer a text version? See our [initial written tutorial with animated gifs](https://kigalisim.org/guide/tutorial_02.html). Alternatively, go to https://kigalisim.org and then in the Designer tab:

 - Click "Add Application" to create a "Commerical Refrigeration" application
 - Click "Add Consumption" to create HFC-134a (click lookup for GHG equivalency, leave energy consumption at default, check domestic)
 - In your HFC-134a record, add a set record for 1 metric tonne (mt) sales in year 1.
 - Click "Add Simulation" and make "Business as Usual" from year 1 to 10.
 - Click the "Bank" radio button for total million units of equipment and see population increase over time.

Finally, go do the Editor tab and see the QubecTalk code you wrote:

```
start default

  define application "Commercial Refrigeration"
  
    uses substance "HFC-134a"
      enable domestic
      initial charge with 1 kg / unit for domestic
      initial charge with 0 kg / unit for import
      initial charge with 0 kg / unit for export
      equals 1430 kgCO2e / kg
      equals 1 kwh / unit
      set sales to 1 mt during year 1
      retire 5 % / year
    end substance
  
  end application

end default


start simulations

  simulate "Business as Usual"
  from years 1 to 10

end simulations
```

<br>

## Setup
All executions of simulations are private and local.

### Public hosted browser-based app
Requiring no new local software installation, use the public hosted version of the tool at https://kigalisim.org. Simulations are run locally on your own machine through WebAssembly and Kigali Sim will not send your simulation code / data to external machines without your permission. We simply provide public static file hosting (see [privacy](https://kigalisim.org/privacy.html)).

### Local jar file
For those who prefer the command-line interface, install Java (like through [Adoptium](https://adoptium.net/installation)) and download the [latest Kigali Sim jar](https://kigalisim.org/kigalisim-fat.jar). Then, execute simulations with:

```
java -jar kigalisim-fat.jar run -o output.csv script.qta
```

Here, you may replace `script.qta` with the path to your QubecTalk script and `output.csv` with the path to where you would like to write results.

Note that this jar is also posted to GitHub Maven. 

### Docker
For those who prefer Docker, see `Dockerfile` which installs Java ([Temurin](https://adoptium.net/temurin/releases)) and can be used to run simulations. For example, the following builds the image and runs `script.qta` with output to `output.csv` through the mounted `working` directory:

```
docker build -t kigalisim .
docker run -v $(pwd):/working kigalisim run -o output.csv script.qta
```

Note: Windows users should replace `$(pwd)` with `%cd%` for Command Prompt or `${PWD}` for PowerShell.

We also provide a Dev Container with additional tools for those looking to modify Kigali Sim or run a local version of the web-based editor. see `DEVELOPING.md`.

<br>

## Usage
Kigali Sim authors can choose to use a UI-based editor which does not require writing code or the QubecTalk domain-specific programming language.

### UI-based authoring
For users preferring a no-code point-and-click approach to authoring simulations, open the [UI-based editor](https://kigalisim.org/) and then review our [no-code hello world example](https://kigalisim.org/guide/tutorial_02.html). Alternatively, review an [example simulation](https://kigalisim.org/guide/tutorial_02.qta) made using the UI-based editor in order to explore the tools' capabilities.

### Code-based authoring
For those preferring to write code, review the [setup instructions](https://github.com/SchmidtDSE/kigali-sim?tab=readme-ov-file#setup) and then consider this example of a permitting system to replace a high global warming potential substance with one exhibiting a lower GWP. This demonstrates all of the basic building blocks of Kigali Sim model (applications, substances, policies, and simulations):

```
start default

  define application "Commercial Refrigeration"
  
    uses substance "HFC-134a"
      enable domestic
      initial charge with 1 kg / unit for domestic
      initial charge with 0 kg / unit for import
      initial charge with 0 kg / unit for export
      equals 1430 kgCO2e / kg
      equals 1 kwh / unit
      set sales to 1 mt during year 1
      retire 5 % / year
      recharge 5 % with 0.85 kg / unit
    end substance


    uses substance "R-600a"
      enable domestic
      initial charge with 1 kg / unit for domestic
      initial charge with 0 kg / unit for import
      initial charge with 0 kg / unit for export
      equals 3 kgCO2e / kg
      equals 1 kwh / unit
      set sales to 1 kg during year 1
      retire 5 % / year
      recharge 5 % with 0.85 kg / unit
    end substance
  
  end application

end default


start policy "Permit"

  modify application "Commercial Refrigeration"
  
    modify substance "HFC-134a"
      cap sales to 80 % displacing "R-600a" during years 3 to 10
    end substance
  
  end application

end policy


start simulations

  simulate "Business as Usual"
  from years 1 to 10


  simulate "With Permit"
    using "Permit"
  from years 1 to 10

end simulations
```

This can be run using the editor tab at https://kigalisim.org/ or locally via `java -jar kigalisim-fat.jar run -o output.csv script.qta` or similar.

### Additional resources
Developers can continue their work by going to the [User Guide](https://kigalisim.org/guide/) which had information on other features. See also the [formal QubecTalk language specification](https://kigalisim.org/guide/qubectalk.pdf).

### LLM assistants
If desired, AI coding assistants or chatbots can help in using Kigali Sim. We implement the [llms.txt specification](https://llmstxt.org), a standard that allows users to bring their own LLM assistants to the tool. Direct your AI to read `https://kigalisim.org/llms-full.txt?v=20250928` and / or `https://kigalisim.org/llms.txt?v=20250928`.

<br>

## Project Structure

This repository is organized into three main components:

- **`docs/`**: Documentation including technical specifications and user guides
- **`engine/`**: Java-based simulation engine that can run standalone or in-browser via WASM
- **`editor/`**: Web-based editor and analysis tool interface

These directories and their subdirectories also often have their own `README.md` files with additional details.

<br>

## Help and Contributions
For those with a bug report or a suggestion to improve Kigali Sim, please [open an issue](https://github.com/SchmidtDSE/kigali-sim/issues/new). It's OK if you do not have all of the details sorted out and we are happy to help you refine your report. That said, some basic information is requested and please complete the issue template to the best of your ability. We will help you take it from there!

For those seeking help with using Kigali Sim, we recommend that you consider starting with the "Get Help" button within the application if you are comfortable sharing your work with us. This free and private support channel offered by the project maintainers (currently the University of California DSE). If you do not want to share your simluation with us or are instructed to file a public help request, please [open an issue](https://github.com/SchmidtDSE/kigali-sim/issues/new) and try your best to fill in the issue template. Please note that this issue will be public.

For those interested in contributing to the Kigali Sim open source project as a developer, thank you and please review `CONTRIBUTORS.md`. Also, please see [our credits page](https://kigalisim.org/credits.html) and our [humans.txt](https://kigalisim.org/humans.txt) for more information about the community behind this open source project. If you are unsure which issue might be best for getting started, please look for the "good first issue" tag. For information about community standards and expectations, please see `CONDUCT.md`.

<br>

## Development
If you want to change the code of Kigali Sim itself, we have some basic getting started steps. However, for additional information about development, see [DEVELOPING.md](https://github.com/SchmidtDSE/kigali-sim/blob/main/DEVELOPING.md).

### Using a Dev Container
For those interested in a dev container, please see `.devcontainer`. **IntelliJ** should automatically detect the dev container (see [JetBrains documentation](https://www.jetbrains.com/help/idea/connect-to-devcontainer.html)). **VS Code** users can use an extension:

1. Install the [Dev Containers extension](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers)
2. Open the project in VS Code
3. When prompted, click "Reopen in Container" or press `F1` and select "Dev Containers: Reopen in Container"
4. The container will build automatically with all required dependencies

One can also use **GitHub Codespaces**:

1. Navigate to the repository on GitHub
2. Click the green "Code" button
3. Select the "Codespaces" tab
4. Click "Create codespace on main"
5. The development environment will be set up automatically

See `DEVELOPING.md` for detailed information about the dev container configuration.

All the automated checks described below work in the dev container environment:

- Java testing: `cd engine && ./gradlew test`
- Java linting: `cd engine && ./gradlew checkstyleMain` and `cd engine && ./gradlew checkstyleTest`
- JavaScript linting: `cd editor && pnpm exec eslint ./js/*.js` and `cd editor && pnpm exec eslint ./test/*.js`
- JavaScript testing: `cd editor && pnpm exec grunt`

### Other Local Setup
To run this system locally outside a dev container, please:

1. Install Java like through [Adoptium](https://adoptium.net).

2. Go to the editor directory:

```bash
cd engine
```

3. Run unit tests:

```bash
./gradlew test
```

4. Check style:

```bash
./gradlew checkstyleMain checkstyleTest
```

5. Build the Jar (Gradle will be setup automatically)

```bash
./gradlew build
```

If working on the front-end, please set up the editor:

1. [Setup Node](https://docs.npmjs.com/downloading-and-installing-node-js-and-npm) and [pnpm](https://pnpm.io).

2. Navigate to the editor directory:

```bash
cd editor
```

3. Install build dependencies:

```bash
pnpm install
```

4. Install frontend dependencies:

```bash
bash ./support/install_deps.sh
```

5. Build the project (TeaVM build step for WASM):

```bash
bash ./support/update_wasm.sh
bash ./support/make.sh
```

6. Run the front-end tests (may install browser via Puppeteer):

```bash
pnpm exec grunt
```

7. Run style checks:

```bash
pnpm exec eslint ./js/*.js
pnpm exec eslint ./test/*.js
```

8. Run a local web server (such as Python http.server):

```bash
python -m http.server
```

9. Visit the local hosted webpage using any web browser at the given address.

### Additional resources
We have continuous integration and deployment through GitHub actions. See [DEVELOPING.md](https://github.com/SchmidtDSE/kigali-sim/blob/main/DEVELOPING.md) for more details.

<br>

## Open Source
We thank the following Open Source libraries and resources:

- [ACE Editor](https://ace.c9.io/) under [BSD-3](https://github.com/ajaxorg/ace/blob/master/LICENSE).
- [ANTLR4](https://www.antlr.org/) under [BSD-3](https://www.antlr.org/license.html).
- [Apache Commons CSV](https://commons.apache.org/proper/commons-csv/) under [Apache-2.0](https://github.com/apache/commons-csv/blob/master/LICENSE.txt).
- [Chart.js](https://www.chartjs.org/) under [MIT](https://github.com/chartjs/Chart.js/blob/master/LICENSE.md).
- [D3](https://d3js.org/) under [ISC](https://github.com/d3/d3/blob/main/LICENSE).
- [ESLint]((https://eslint.org/)) under [MIT](https://github.com/eslint/eslint/blob/main/LICENSE).
- [Papa Parse](https://www.papaparse.com/) under [MIT](https://github.com/mholt/PapaParse/blob/master/LICENSE).
- [Popper](https://popper.js.org/) under [MIT](https://github.com/floating-ui/floating-ui/blob/master/LICENSE).
- [Prism.js](https://prismjs.com/) under [MIT](https://github.com/PrismJS/prism/blob/v2/LICENSE).
- [Public Sans](https://public-sans.digital.gov/) under [OFL-1.1](https://github.com/uswds/public-sans/blob/master/LICENSE.md).
- [QUnit](https://qunitjs.com/) under [MIT](https://github.com/qunitjs/qunit/blob/main/LICENSE.txt).
- [SVG Spinners](https://github.com/n3r4zzurr0/svg-spinners?tab=readme-ov-file) under [MIT](https://github.com/n3r4zzurr0/svg-spinners?tab=readme-ov-file)
- [Tabby](https://github.com/cferdinandi/tabby) under [MIT](https://github.com/cferdinandi/tabby/blob/master/LICENSE.md).
- [Tippy](https://atomiks.github.io/tippyjs/) under [MIT](https://github.com/atomiks/tippyjs/blob/master/LICENSE).
- [Webpack](https://webpack.js.org/) under [MIT](https://github.com/webpack/webpack/blob/main/LICENSE).
- [Global Plastics AI Policy Tool](https://global-plastics-tool.org/) under [BSD-3-Clause](https://github.com/SchmidtDSE/plastics-prototype/blob/main/LICENSE.md).
- [Josh](https://joshsim.org/) under [BSD-3-Clause](https://github.com/SchmidtDSE/josh/blob/main/LICENSE.md).

We also use data from [UNEP Ozone Secretariat](https://ozone.unep.org/lists-substances-and-blends) for Global Warming Potential (GWP) values of substances and refrigerant blends. See `editor/json/known_gwp.json`. Additionally, [Pandoc](https://pandoc.org/) under [GPL-2.0](https://github.com/jgm/pandoc/blob/master/COPYRIGHT) is invoked from CI / CD but exclusively for optional documentation generation (invoked as an executable). Not a compile-time or run-time dependency of Kigali Sim itself or any of its components and it is used in a completely isolated CI / CD pipeline from the rest of the project.

<br>

## License
This project's code is available under the [BSD license](https://github.com/SchmidtDSE/kigali-sim/blob/main/LICENSE.md). All documentation is available under the Creative Commons [CC-BY 4.0 International License](https://creativecommons.org/licenses/by/4.0/deed.en).

This privacy-respecting simulation platform offers essential tools to assist policy and research efforts. Informed by various perspectives from across the Montreal Protocol ecosystem of actors, we believe that this project is owned by its community. It is available to the public as an optional open source resource.
