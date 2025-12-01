# Contributing
Thank you for your time! This guide will help you get started contributing to the Kigali Sim project. Looking for a place to start? See the [issues tracker](https://github.com/SchmidtDSE/kigali-sim/issues) to report an issue, ask a question, or look for something that needs help. Issues tagged with "good first issue" are great places to get started.

<br>
<br>

## Welcome
Thank you for your contribution! We appreciate the community's help in any capacity from filing an issue to opening a pull request. No matter how your contribution shows up, we are happy you are here. Kigali Sim is used by multiple Article 5 nations and your work helps support climate action worldwide.

<br>
<br>

## Coding Guidelines
To ensure the conceptual integrity and readability of our code, we have a few guidelines:

For the **Java code (Engine)**:

 - Please try to follow the conventions laid out by the project in existing code. When in doubt, refer to the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).
 - Tests are encouraged and should be included with new functionality.
 - JavaDoc comments are encouraged. We generate JavaDoc at https://kigalisim.org/guide/javadoc/.
 - Use 2-space indentation (no tabs).
 - Proper import ordering is expected.

For **ECMAScript (Editor)**:

 - Please follow the [Google JavaScript Style Guide](https://google.github.io/styleguide/jsguide.html).
 - Tests are encouraged using QUnit.
 - Use JSDoc comments for documentation.
 - Use 2-space indentation.
 - Use double quotes for strings.
 - Maximum line length: 100 characters.
 - Include trailing commas in multi-line structures.

**Do not worry if you aren't sure that you met all of the guidelines!** We encourage pull requests and are happy to work through any necessary outstanding tasks with you. We're here to help if you get stuck or are unsure about anything.

<br>
<br>

## Terminology
Note that we use the name ECMAScript instead of JS or JavaScript as we specifically use the ECMA standards. Additionally, we use `main` not `master` for our primary branch and we refer to, in a concurrency context, `leader` and `worker` nodes. We use the term "pull request" for a request to change code in the repository though merge request is also acceptable.

<br>
<br>

## Automated Tests and Checks
When you submit a pull request, several automated tests and checks will run. Don't worry if some fail at first - we're happy to help you get them passing! See below for what gets checked and we encourage you to try to get these to pass prior to opening a pull request:

**Java / engine** tests are run from the `engine/` directory:

```bash
# Run unit tests
./gradlew test

# Check code style (Google Java Style)
./gradlew checkstyleMain
./gradlew checkstyleTest

# Auto-format code
./gradlew spotlessApply
```

**ECMAScript / editor** tests are run from the `editor/` directory:

```bash
# Run QUnit tests
pnpm exec grunt

# Lint production code
pnpm exec eslint ./js/*.js

# Lint test code
pnpm exec eslint ./test/*.js

# Auto-fix linting issues where possible
pnpm exec eslint ./js/*.js --fix
```

All of these checks are described in detail in [DEVELOPING.md](DEVELOPING.md). If you run into issues with any of these checks, please don't hesitate to ask for help in your pull request. We understand that some of these tools can be tricky to set up, especially if you're new to the project. We're happy to guide you through the process.

<br>
<br>

## Design Choices
There are reasonable differences of opinion in the community about ideal implementations. That in mind, there are a few opinionated choices we've made in the design of this project that we will maintain moving forward. We encourage folks in our community to open issues if they wish to discuss these design choices further but, at this time, we may not merge pull requests that do not conform to these choices.

 1. QubecTalk is intentionally designed to be accessible to non-programmers while remaining expressive for technical users and is intended as the code-based entry point to Kigali Sim: we have previously transitioned from an ECMAScript to Java engine and want to retain the choice to change languages again in the future.
 2. We use ANTLR4 for parsing QubecTalk and alternative parsers are not under consideration at this time.
 3. We compile the Java engine to WebAssembly using TeaVM for browser execution while maintaining standalone CLI capability.
 4. All simulations must run locally on the user's machine for privacy.
 5. We do not use CDNs for privacy reasons and CDN migration PRs are discouraged.
 6. We use vanilla ECMAScript (not TypeScript / other transpiled languages or UI frameworks like React) for the editor interface as, due to our limited resrources, we must limit dependencies whose major versions may require substantial rewrite and have found in our experience that vanilla ages the best in a resource constrained environment.
 7. The engine is designed to support both UI-based (Designer) and code-based (Editor) authoring but, even as all scripts which are authored using the UI-based editor must be compatible with the code-based editor, not all scripts which are possible to write in code need to be compatible with the UI-based editor.
 8. We support Monte Carlo probabilistic simulation with parallelization capabilities but assume Monte Carlo users primarily operate on the command line with the JVM. At this time, analysis of probabilistic outputs in the UI-based editor is not a priority.
 9. Parallelization in the form of parallel scenarios (BAU, with permit, etc) must be supported by all runtimes but not parallelization within a scenario.
 10. Due to recurring security issues, we ask that you please use pnpm and not npm.
 11. Immutability is preferred where possible but not a hard requirement if there is profiler-backed evidence against immutability. Builder patterns are considered compliant.
 12. Prior versions saw over and under-flow in practical usage. Please use BigDecimal where possible when working with user-provided numbers and calculations.

Please reach out if you have further questions about these guidelines or want to discuss alternative approaches.

<br>
<br>

## Scope
There are some limitations of scope for this project:

 - The Java engine must remain compatible with Java 21+ to support TeaVM compilation to WebAssembly but earlier versions are not guaranteed to be supported.
 - Breaking changes to the QubecTalk language syntax should be discussed in an issue before implementation and require community input.
 - We must maintain both the browser-based application and standalone JAR/Docker execution paths but other execution modalities are currently out of scope.
 - Energy consumption can be modeled but energy mix for secondary emissions is currently considered out of scope.
 - Direct use of the engine outside of QubecTalk or the UI-based editor are not currently priorities for the project given our limited resources.
 - Currently the web editor must work without transpilation or compilation steps for ECMAScript except for the WASM and ANTLR components (we use webpack only for bundling). Features requiring new compilation steps may be declined.

<br>
<br>

## Development Environment
We support both dev container and local development setups. See [DEVELOPING.md](https://github.com/SchmidtDSE/kigali-sim/blob/main/DEVELOPING.md) for setup instructions.

<br>
<br>

## Procedure
If you would like to contribute code but don't have a specific issue to address, thank you! Please look for issues tagged "good first issue" or reach out to hello@kigalisim.org. To make a contribution, please:

 - If one is not already open, [open an issue](https://github.com/SchmidtDSE/kigali-sim/issues).
 - [Open a pull request](https://github.com/SchmidtDSE/kigali-sim/pulls).
 - Mark the pull request as draft until you pass checks and are ready for review.
 - Indicate that your pull request closes your issue by saying "closes #" followed by your issue number in the PR description.
 - Request a review when ready.

We apologize for the formality but there's just a few logistical pieces to keep in mind. First, by contributing, you attest that you are legally permitted to provide code to the project and that you are both able and agree to release that code under the [project's BSD-3-Clause license](https://github.com/SchmidtDSE/kigali-sim/blob/main/LICENSE.md). Second, you agree to follow [CODE_OF_CONDUCT.md](https://github.com/SchmidtDSE/kigali-sim/blob/main/CODE_OF_CONDUCT.md).

<br>
<br>

## Use of Generative AI
We welcome use of AI coding assistants. Indeed, the maintainers of this package are using them but ask them to follow the pattern of existing code within the repository. However, we have some guidelines:

 - Unless edits are trivial, please disclose use of AI assistants by mentioning the model you used as a co-author in your commit. If uncertain, please add as a co-author.
 - Prompts should be narrow and all generated work should be manually evaluated before submitting to the repository either in a main / deploy branch or in a PR branch.
 - All structural choices must be made manually. AI assistants are only expected to be used for "mechanical" edits.
 - We view these resources as tools but you remain responsible for all actions taken. Any code you contribute with assistance of an AI is held to the same standard as code contributed without an AI assistant.

As with any contribution, the maintainers reserve the right to politely decline contributions and, in particular, be sure that any AI-assisted edits follow the norms, conventions, and architecture of the existing code. For an example of a specific prompt, please see `tasks/example.md`. We make an exception for the co-author disclosure only for mechanical edits strictly replicating existing patterns in the codebase closely similar to classic refactoring tools in IDEs.

<br>
<br>

## Documentation
When adding new features or changing existing functionality:

 - Update relevant documentation in the tutorial and user's guide if appropriate.
 - Add JavaDoc comments for Java classes and methods
 - Add JSDoc comments for JavaScript functions
 - Update [DEVELOPING.md](https://github.com/SchmidtDSE/kigali-sim/blob/main/DEVELOPING.md) if you change development workflows

<br>
<br>

## Accessibility
Please ensure contributions meet WCAG 2.0 AA guidelines or better.

## Parting Thoughts
Open source is an act of love. Please be kind and respectful of all contributors. Your work on Kigali Sim helps support countries and other organizations charged with safeguarding our environment like through the Kigali Amendment. Thank you for being part of this community.

For more information about community standards, please see [CODE_OF_CONDUCT.md](https://github.com/SchmidtDSE/kigali-sim/blob/main/CODE_OF_CONDUCT.md) for our Code of Conduct.
