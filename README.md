# Montreal Protocol Policy Simulation Tool

Open source unofficial web-based tool and simulation engine capable of simulations related to substances, applications, and policies relevant to the Montreal Protocol. It may potentially be useful at the discretion of relevant stakeholders as an optional step in informing proposals.

## Purpose

This open source toolkit provides a simulation engine for modeling substances, applications, and policies related to the Montreal Protocol. It supports a foundational business as usual simulation and then supports "stacking" policy simulations on top of that baseline. These projections may optionally support activities such as KIPs. Though not intended to be comprehensive of all possible relevant modeling techniques, this tool provides access to both UI and code-based editing where the later can also conduct probabilistic projection. The QubecTalk domain specific language allows for high degree of customization and specificity with automated unit conversions. Unofficial and completely voluntary, this privacy-respecting simulation platform offers essential tools to optionally inform potential policy.

## Usage

1. Open the application in your web browser (https://mlf-policy-explorer.org).
2. Use either the Basic (UI-based) or Advanced (code-based) editor to define your simulation
3. Click Run to execute the simulation
4. View results in the visualization panel, which shows:
   - Consumption metrics
   - Sales data
   - Equipment population statistics

## Local Development Setup

1. Install dependencies:

```bash
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

4. Execute (use any web browser):

```bash
python -m http.server
```

## Deployment

This project can be deployed using Github. Simply push your changes to the repository at `main`. The deployment process is automated through GitHub Actions as defined in `.github/workflows/build.yaml`.

## Development Standards
While contributing, please maintain existing styles defined in `.prettierrc` and `.eslintrc.yml`. Where ambiguous, follow Google JavaScript Style Guide conventions. Unit tests through QUnit are encouraged but a specific test coverage target is not specified. Document code using JSDoc comments (required for all public members). Note that Replit AI was used to help with documentation and small trivial code changes but, for other purposes, please disclose use of generative AI before merging pull requests. We generally require that AI not used for architecture or large operations.

## Open Source
We thank the following Open Source libraries:

- [ANTLR4](https://www.antlr.org/) for parsing the QubecTalk domain-specific language under [BSD-3](https://www.antlr.org/license.html).
- [D3](https://d3js.org/) for data visualization under [ISC](https://github.com/d3/d3/blob/main/LICENSE).
- [Chart.js](https://www.chartjs.org/) for rendering some charts and graphs under [MIT](https://github.com/chartjs/Chart.js/blob/master/LICENSE.md).
- [ACE Editor](https://ace.c9.io/) for the code editing interface under [BSD-3](https://github.com/ajaxorg/ace/blob/master/LICENSE).
- [QUnit](https://qunitjs.com/) for unit testing under [MIT](https://github.com/qunitjs/qunit/blob/main/LICENSE.txt).
- [Webpack](https://webpack.js.org/) for bundling JavaScript modules under [MIT](https://github.com/webpack/webpack/blob/main/LICENSE).
- [ESLint]((https://eslint.org/)) for code style enforcement under [MIT](https://github.com/eslint/eslint/blob/main/LICENSE).
- [Public Sans](https://public-sans.digital.gov/) font under [OFL-1.1](https://github.com/uswds/public-sans/blob/master/LICENSE.md).
- [Tabby](https://github.com/cferdinandi/tabby) for tab interface management under [MIT](https://github.com/cferdinandi/tabby/blob/master/LICENSE.md).

## License

This project's code is available under the BSD license. The QubecTalk language documentation is available under the Creative Commons CC-BY 4.0 International License.
