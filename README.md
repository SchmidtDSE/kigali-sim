
# Montreal Policy Simulation

Web-based open source tool and simulation engine capable of simulations related to the Montreal Protocol.

## Purpose

This open source toolkit provides a domain-specific language (QubecTalk) and simulation engine for modeling and analyzing policies related to the Montreal Protocol. It supports business as usual and policy simulations to optionally support activities such as KIPs. It helps evaluate and compare different intervention options by simulating longitudinal projections of applications, substances, policies, and interactions between these components under different scenarios.

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
While contributing, please maintain existing styles defined in `.prettierrc` and `.eslintrc.yml`. Where ambiguous, follow Google JavaScript Style Guide conventions. Unit tests through QUnit are encouraged but a specific test coverage target is not specified. Document code using JSDoc comments (required for all public members). Note that Replit AI was used to help with documentation but, for other purposes, please disclose use of generative AI before merging pull requests.

## Open Source
We thank the following Open Source libaries:

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
