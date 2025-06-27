# Montreal Protocol Policy Simulation Tool
Open source web-based tool and simulation engine capable of simulations related to substances, applications, and policies relevant to the Montreal Protocol. It may potentially be useful at the discretion of relevant stakeholders as an optional step in informing proposals.

## Project Structure

This repository is organized into three main components:

- **`docs/`**: Documentation including technical specifications and user guides
- **`engine/`**: Java-based simulation engine that can run standalone or in-browser via WASM
- **`editor/`**: Web-based editor and analysis tool interface

<br>
<br>

## Purpose
This open source toolkit provides a simulation engine for modeling substances, applications, and policies related to the Montreal Protocol. It supports a foundational business as usual simulation and then supports "stacking" policy simulations on top of that baseline. These projections may optionally support activities such as Kigali Amendment Implementation Plans (KIPs). 

Though not intended to be comprehensive of all possible relevant modeling techniques, this tool provides access to both UI and code-based editing where the later can also conduct probabilistic projection. The QubecTalk domain specific language allows for high degree of customization and specificity with automated unit conversions. This includes calculation of direct emissions and energy consumption.

Unofficial and completely voluntary, this privacy-respecting simulation platform offers essential tools to optionally inform potential policy. Though informed by various perspectives from across the Montreal Protocol ecosystem of actors, this is not an official product of any agency, fund, or official international body and is, instead, a community project available to the public as an open source resource.

<br>

## Usage
To use the public hosted version of the tool:

1. Open the application in your web browser (https://mlf-policy-explorer.org).
2. Use either the Basic (UI-based) or Advanced (code-based) editor to define your simulation
3. Click Run to execute the simulation
4. View results in the visualization panel, which shows emissions metrics, consumption / sales data, and equipment population.

Data can also be downloaded through the export button shown in the results tab.

<br>

## Development

When developing on the tool, please try to ensure all automated checks pass, development standards are followed, and deployments only happen through the process described where possible (if releasing to the official release website).

### Automated checks

Various automated tests and checks are available to help those developing on the tool and engine.

#### Java Engine Development

In the engine directory (`cd engine`):

 - `./gradlew test`: Run unit tests for the engine
 - `./gradlew checkstyleMain`: Lint production Java code built into WASM engine
 - `./gradlew checkstyleTest`: Lint test Java code

#### JavaScript Editor Development

In the editor directory (`cd editor`):

 - `npx grunt`: Runs front-end unit tests and end-to-end integration tests
 - `npx eslint ./js/*.js`: Lint production JavaScript
 - `npx eslint ./test/*.js`: Lint test JavaScript

#### Building and Deploying WASM Engine

To update the web editor with changes from the Java engine:

1. Build the engine WASM output: `cd engine && ./gradlew war`
2. Update WASM files in editor: `cd editor && bash support/update_wasm.sh`

This extracts the compiled TeaVM/WASM output from the engine build and places it in the editor's `wasm/` directory.

### Development Standards
While contributing, please maintain existing styles defined in `.prettierrc` and `.eslintrc.yml`. Where ambiguous, follow [Google JavaScript / TypeScript Style Guide](https://google.github.io/styleguide/tsguide.html) conventions. Unit tests through QUnit are encouraged but a specific test coverage target is not specified. Document code using JSDoc comments (required for all public members). Note that Replit AI was used to help with documentation and small trivial code changes but, for other purposes, please disclose use of generative AI before merging pull requests. We generally require that AI not used for architecture or large operations. At this time, we are not considering a change to TypeScript.

### Deployment
This project can be deployed using Github. Simply push your changes to the repository `deploy` branch. The deployment process is automated through GitHub Actions as defined in `.github/workflows/build.yaml`.

<br>

## Development Setup

### Development Container Setup

This project includes a dev container configuration that provides a complete development environment for both the JavaScript frontend and Java engine components.

#### Using with VS Code

1. Install the [Dev Containers extension](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers)
2. Open the project in VS Code
3. When prompted, click "Reopen in Container" or press `F1` and select "Dev Containers: Reopen in Container"
4. The container will build automatically with all required dependencies

#### Using with GitHub Codespaces

1. Navigate to the repository on GitHub
2. Click the green "Code" button
3. Select the "Codespaces" tab  
4. Click "Create codespace on main"
5. The development environment will be set up automatically

#### Container Features

The dev container includes:
- Eclipse Temurin JDK 21 (as specified in requirements)
- Node.js 18.x with npm
- All system dependencies for building and testing
- VS Code extensions for Java, JavaScript, and Gradle development
- Pre-configured ports (8000, 8080) for local development servers

#### Automated Checks in Container

All the automated checks described below work in the dev container environment:

- JavaScript linting: `cd editor && npx eslint ./js/*.js` and `cd editor && npx eslint ./test/*.js`
- JavaScript testing: `cd editor && npx grunt`
- Java testing: `cd engine && ./gradlew test`
- Java linting: `cd engine && ./gradlew checkstyleMain` and `cd engine && ./gradlew checkstyleTest`

### Local Setup
To run this system locally, please complete the following steps:

1. Navigate to the editor directory and install dependencies:

```bash
cd editor
npm install
```

2. Install frontend dependencies:

```bash
bash ./support/install_deps.sh
```

3. Build the project:

```bash
bash ./support/make.sh
```

4. Run a local web server (such as Python http.server):

```bash
python -m http.server
```

5. Visit the local hosted webpage using any web browser.

<br>

## Open Source
We thank the following Open Source libraries:

- [ANTLR4](https://www.antlr.org/) for parsing the QubecTalk domain-specific language under [BSD-3](https://www.antlr.org/license.html).
- [Apache Commons CSV](https://commons.apache.org/proper/commons-csv/) for CSV file processing under [Apache-2.0](https://github.com/apache/commons-csv/blob/master/LICENSE.txt).
- [D3](https://d3js.org/) for data visualization under [ISC](https://github.com/d3/d3/blob/main/LICENSE).
- [Chart.js](https://www.chartjs.org/) for rendering some charts and graphs under [MIT](https://github.com/chartjs/Chart.js/blob/master/LICENSE.md).
- [ACE Editor](https://ace.c9.io/) for the code editing interface under [BSD-3](https://github.com/ajaxorg/ace/blob/master/LICENSE).
- [Prism.js](https://prismjs.com/) for syntax highlighting under [MIT](https://github.com/PrismJS/prism/blob/v2/LICENSE).
- [QUnit](https://qunitjs.com/) for unit testing under [MIT](https://github.com/qunitjs/qunit/blob/main/LICENSE.txt).
- [Webpack](https://webpack.js.org/) for bundling JavaScript modules under [MIT](https://github.com/webpack/webpack/blob/main/LICENSE).
- [ESLint]((https://eslint.org/)) for code style enforcement under [MIT](https://github.com/eslint/eslint/blob/main/LICENSE).
- [Public Sans](https://public-sans.digital.gov/) font under [OFL-1.1](https://github.com/uswds/public-sans/blob/master/LICENSE.md).
- [SVG Spinners](https://github.com/n3r4zzurr0/svg-spinners?tab=readme-ov-file) under [MIT](https://github.com/n3r4zzurr0/svg-spinners?tab=readme-ov-file)
- [Tabby](https://github.com/cferdinandi/tabby) for tab interface management under [MIT](https://github.com/cferdinandi/tabby/blob/master/LICENSE.md).
- [Global Plastics AI Policy Tool](https://global-plastics-tool.org/) under [BSD-3-Clause](https://github.com/SchmidtDSE/plastics-prototype/blob/main/LICENSE.md).
- [Josh](https://joshsim.org/) under [BSD-3-Clause](https://github.com/SchmidtDSE/josh/blob/main/LICENSE.md).

<br>

## License
This project's code is available under the BSD license. All documentation including QubecTalk language specification is available under the Creative Commons CC-BY 4.0 International License.
