# Web Editor

This directory contains the web-based editor and analysis tool for Kigali Sim.

## Purpose

The editor provides an interactive web interface for creating simulations, running analyses, and visualizing results. It includes both a basic UI-based editor and an advanced code-based editor using the QubecTalk domain-specific language. It is organized into the following directories:

- `js`: JavaScript source code for the web application
- `style`: CSS stylesheets and visual assets
- `test`: JavaScript unit tests and integration tests
- `wasm`: WebAssembly files compiled from the Java engine
- `examples`: Sample QTA files demonstrating various features
- `third_party`: External JavaScript libraries and dependencies
- `support`: Build and deployment scripts
- `intermediate`: Generated language parsing files
- `language`: ANTLR grammar and language processing tools (grammar file is copied from engine during build)

## Development

The front-end development requires some additional steps after having set up for building the Java components. Then, grunt can be used to run front-end QUnit tests.

**Note:** The QubecTalk grammar file (`QubecTalk.g4`) is automatically copied from `engine/src/main/antlr/org/kigalisim/lang/QubecTalk.g4` during the build process. To modify the grammar, edit the file in the engine directory, not in `editor/language/`.

### Setup

We use webpack minimally to support ANTLR which is used to parse QubecTalk code. It is also strongly recommended that you use pnpm.

```bash
pnpm install
```

Afterwards, we also have some front-end dependencies. We do not minify our code to support our vanilla JS workflow. That said, we also do not allow live use of CDNs for third-party libraries from production due to security and privacy concerns. Therefore, install a local copy of these libraries:

```bash
bash ./support/install_deps.sh
```

### Testing

First, build the ANTLR and WASM components:

```bash
bash ./support/make.sh
```

Then, run unit tests and front-end integration tests via grunt:

```bash
pnpm exec grunt
```

Additionally, we have support for linting ECMAScript code:

```bash
pnpm exec eslint ./js/*.js
pnpm exec eslint ./test/*.js
```

Finally, you may want to run a development server to try out the front-end locally and interactively. Any local server is acceptable and, for example, you may use Python:

```bash
python -m http.server
```

You may visit http://localhost:8000 in your browser to use the Kigali Sim IDE or go to http://localhost:8000/test/test.html to interact with front-end tets.

## Architecture

The front operates through a series of presenters (as in model-view-presenter). This starts with `MainPresenter` in `js/main.js`. These interact with the engine through `wasm_backend.js` and `WasmLayer` which, in turn, uses a pool of web workers (`wasm.worker.js`) where each runs a simulation scenario (like business as usual) in parallel. Note that we also have a service worker (`service_worker.js`) that allows for offline use and user scripts are persisted between sessions locally using `StorageKeeper` instances which, at this time, only `LocalStorageKeeper` is used in production which, as its name implies, uses [HTML5 local storage](https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage). Towards that end, users may author simulations either in the code-based editor (uses Ace Editor) or the UI-based designer which is a bespoke interface that translates to and from QubecTalk code via `ui_translator.js`. This involves a front-end invocation of [ANTLR](https://www.antlr.org/).

## Development Standards

Please see [CONTRIBUTING.md](https://github.com/SchmidtDSE/kigali-sim/blob/main/CONTRIBUTING.md) for more details but, when working on the front-end, please try to:

- Follow [Google JavaScript / TypeScript Style Guide](https://google.github.io/styleguide/tsguide.html).
- Use [JSDoc](https://jsdoc.app/) for all public function documentation.
- Maintain comprehensive test coverage with [QUnit](https://qunitjs.com/).
- Use [ESLint](https://eslint.org/) for code style enforcement.
- Test across multiple browsers and screen sizes, if possible through a service like [BrowserStack](https://www.browserstack.com/).
- Maintain [WCAG 2.0 AA](https://webaim.org/standards/wcag/checklist) or better.