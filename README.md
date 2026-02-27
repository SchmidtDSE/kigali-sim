# Kigali Sim
Open source engine for simulating substances, applications, and policies relevant to the Montreal Protocol and the Kigali Amendment.

[![Project Status: Active – The project has reached a stable, usable state and is being actively developed.](https://www.repostatus.org/badges/latest/active.svg)](https://www.repostatus.org/#active) [![BSD Licensed](https://img.shields.io/badge/License-BSD_3--Clause-blue.svg)](https://opensource.org/licenses/BSD-3-Clause) [![Build](https://github.com/SchmidtDSE/kigali-sim/actions/workflows/build.yaml/badge.svg?branch=deploy)](https://github.com/SchmidtDSE/kigali-sim/actions/workflows/build.yaml) ![Temurin 21 or higher](https://img.shields.io/badge/Java-Temurin%2021%2B-blue)

<br>

## Quickstart
While it can certainly can also run locally on the JVM, the fastest way to try Kigali Sim is from your browser. Make a simulation in 3 minutes without any installation required!

**No code?** Go to https://kigalisim.org and then in the Designer tab:

 - Click "Add Application" to create a "Commerical Refrigeration" application
 - Click "Add Consumption" to create HFC-134a (click lookup for GHG equivalency, leave energy consumption at default, check domestic)
 - In your HFC-134a record, add a set record for 1 metric tonne (mt) sales in year 1.
 - Click "Add Simulation" and make "Business as Usual" from year 1 to 10.
 - Click the "Bank" radio button for total million units of equipment and see population increase over time.

**With code?** Go to https://kigalisim.org and, in the Editor tab, try the following:

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

**Can't decide?** The UI-based authoring steps we provided generate the code seen above so you can move between UI-based and code-based authoring. Just go between the designer and editor tabs.

Note: We use [WebAssembly](https://webassembly.org/) so simluations run privately on your machine. Continue your journey with a [tutorial](https://kigalisim.org/guide/tutorial_02.html).

<br>

## Purpose
This open source toolkit models substances and equipment related to the Montreal Protocol and Kigali Amendment, including high global warming potential substances.

**What it does**: Models substances, applications, and policies related to the Montreal Protocol, focusing on hydrofluorocarbons and their alternatives within the Kigali Amendment. Supports business-as-usual simulation and "stacking" policy simulations on top. Simulates emissions, energy, substance consumption, equipment populations, and trade longitudinally.

**How it does it**: Stock and flow modeling with user-defined structures for applications and substances. Features both UI-based authoring and code-based editing using the QubecTalk domain specific language (DSL). Design details available in [our paper draft](https://github.com/SchmidtDSE/kigali-sim/blob/main/paper/paper.md).

**Where it does it**: Portable execution through web-based bespoke IDE, command line, or integration into larger workflows. Available on WebAssembly (WASM) or Java Virtual Machine (JVM). Supports parallelization and probabilistic simulation via Monte Carlo.

**Who it serves**: Intended for researchers working on stock and flow modeling of these substances and policy makers like those working on Kigali Amendment Implementation Plans (KIPs). With ongoing [Article 5](https://ozone.unep.org/treaties/montreal-protocol/articles/article-5-special-situation-developing-countries) use, it was co-created with over a dozen countries and supporting organizations. Serves community members with varied programming backgrounds and offers [optional AI assistance](https://github.com/SchmidtDSE/kigali-sim?tab=readme-ov-file#llm-assistants).

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

Note that this jar is also posted to GitHub Maven as `org.kigalisim.engine`. See [JavaDoc](https://kigalisim.org/guide/javadoc).

### Docker (CLI)
For those who prefer Docker, see `Dockerfile` which installs Java ([Temurin](https://adoptium.net/temurin/releases)) and can be used to run simulations from the command line. For example, the following builds the image and runs `script.qta` with output to `output.csv` through the mounted `working` directory:

```
docker build -t kigalisim .
docker run -v $(pwd):/working kigalisim run -o output.csv script.qta
```

Note: Windows users should replace `$(pwd)` with `%cd%` for Command Prompt or `${PWD}` for PowerShell.

### AI / Programmatic Access
For programmatic or AI-assisted use, some tools are available. See [engine readme](https://github.com/SchmidtDSE/kigali-sim/blob/main/engine/README.md).

### Local UI-based editor
For information about running the Kigali Sim IDE locally on your machine, see the [development section of this README](https://github.com/SchmidtDSE/kigali-sim?tab=readme-ov-file#development) for various options including Docker, GitHub Codespaces, or a manual local setup.

<br>

## Usage
Kigali Sim authors can choose to use a UI-based editor which does not require writing code or the QubecTalk domain-specific programming language. The easiest way to get started is through the [User's Guide](https://kigalisim.org/guide/) which includes tutorials and reference materials such as a glossary.

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
If desired, AI coding assistants or chatbots can help in using Kigali Sim. We implement the [llms.txt specification](https://llmstxt.org), a standard that allows users to bring their own LLM assistants to the tool. Direct your AI to read `https://kigalisim.org/llms-full.txt?v=20260128` and / or `https://kigalisim.org/llms.txt?v=20260128`.

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

For those seeking help with using Kigali Sim, we recommend that you consider starting with the "Get Help" button within the application if you are comfortable sharing your work with us. This free and private support channel offered by the project maintainers (currently the University of California DSE). If you do not want to share your simulation with us or are instructed to file a public help request, please [open an issue](https://github.com/SchmidtDSE/kigali-sim/issues/new) and try your best to fill in the issue template. Please note that this issue will be public.

For those interested in contributing to the Kigali Sim open source project as a developer, thank you and please review [CONTRIBUTING.md](https://github.com/SchmidtDSE/kigali-sim/blob/main/CONTRIBUTING.md). Also, please see [our credits page](https://kigalisim.org/guide/credits.html) and our [humans.txt](https://kigalisim.org/humans.txt) for more information about the community behind this open source project. If you are unsure which issue might be best for getting started, please look for the "good first issue" tag. For information about community standards and expectations, please see [CODE_OF_CONDUCT.md](https://github.com/SchmidtDSE/kigali-sim/blob/main/CODE_OF_CONDDUCT.md).

<br>

## Development
We have some basic getting started steps in this developer quickstart for those using Docker / Dev Container, GitHub Codespaces or compatible, or a manual local setup.

### Using Docker / Dev Container
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

All the automated checks and development operations described below work in the dev container environment:

- Java testing: `cd engine && ./gradlew test`
- Java linting: `cd engine && ./gradlew checkstyleMain` and `cd engine && ./gradlew checkstyleTest`
- ECMAScript / JavaScript linting: `cd editor && pnpm exec eslint ./js/*.js` and `cd editor && pnpm exec eslint ./test/*.js`
- ECMAScript / JavaScript testing: `cd editor && pnpm exec grunt`
- Build and run the UI-based editor locally: `cd editor && bash ./support/make.sh && python3 -m http.server`

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

4. Build the project (TeaVM build step for WASM):

```bash
bash ./support/make.sh
```

5. Run the front-end tests (may install browser via Puppeteer):

```bash
pnpm exec grunt
```

6. Run style checks:

```bash
pnpm exec eslint ./js/*.js
pnpm exec eslint ./test/*.js
```

7. Run a local web server (such as Python http.server):

```bash
python -m http.server
```

8. Visit the local hosted webpage using any web browser at the given address.

### Additional resources
We have continuous integration and deployment through GitHub actions. For additional information about development, see [DEVELOPING.md](https://github.com/SchmidtDSE/kigali-sim/blob/main/DEVELOPING.md). If you are contributing, see [CONTRIBUTING.md](https://github.com/SchmidtDSE/kigali-sim/blob/main/CONTRIBUTING.md). Finally, if you are using an AI coding assistant to write code you want to donate to the project, please review the [AI contribution guidelines](https://github.com/SchmidtDSE/kigali-sim/blob/main/CONTRIBUTING.md#use-of-generative-ai).

<br>

## Open Source
We thank the following Open Source libraries and resources:

- [ACE Editor](https://ace.c9.io/) under [BSD-3](https://github.com/ajaxorg/ace/blob/master/LICENSE).
- [ANTLR4](https://www.antlr.org/) under [BSD-3](https://www.antlr.org/license.html).
- [Apache Commons CSV](https://commons.apache.org/proper/commons-csv/) under [Apache-2.0](https://github.com/apache/commons-csv/blob/master/LICENSE.txt).
- [AWS Lambda Java Core](https://github.com/aws/aws-lambda-java-libs) under [Apache-2.0](https://github.com/aws/aws-lambda-java-libs/blob/main/LICENSE).
- [AWS Lambda Java Events](https://github.com/aws/aws-lambda-java-libs) under [Apache-2.0](https://github.com/aws/aws-lambda-java-libs/blob/main/LICENSE).
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

We also use colors from [ColorBrewer 2.0](https://colorbrewer2.org/) by Cynthia Brewer, Mark Harrower, Ben Sheesley, Andy Woodruff, and David Heyman at The Pennsylvania State University. Additionally, we use data from [UNEP Ozone Secretariat](https://ozone.unep.org/lists-substances-and-blends) for Global Warming Potential (GWP) values of substances and refrigerant blends. See `editor/json/known_gwp.json`. Additionally, [Pandoc](https://pandoc.org/) under [GPL-2.0](https://github.com/jgm/pandoc/blob/master/COPYRIGHT) is invoked from CI / CD but exclusively for optional documentation generation (invoked as an executable). Not a compile-time or run-time dependency of Kigali Sim itself or any of its components and it is used in a completely isolated CI / CD pipeline from the rest of the project.

<br>

## License
This project's code is available under the [BSD license](https://github.com/SchmidtDSE/kigali-sim/blob/main/LICENSE.md). All documentation is available under the Creative Commons [CC-BY 4.0 International License](https://creativecommons.org/licenses/by/4.0/deed.en).

This privacy-respecting simulation platform offers essential tools to assist policy and research efforts. Informed by various perspectives from across the Montreal Protocol ecosystem of actors, we believe that this project is owned by its community. It is available to the public as an optional open source resource.
