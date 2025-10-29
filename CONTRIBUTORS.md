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

### Java Code (Engine)
 - Please try to follow the conventions laid out by the project in existing code. When in doubt, refer to the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).
 - Tests are encouraged and should be included with new functionality.
 - JavaDoc comments are encouraged. We generate JavaDoc at https://kigalisim.org/guide/javadoc/.
 - Use 2-space indentation (no tabs).
 - Proper import ordering is expected.

### JavaScript Code (Editor)
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

## Automated Tests and Checks
When you submit a pull request, several automated tests and checks will run. Don't worry if some fail at first - we're happy to help you get them passing! Here's what gets checked:

### Java Engine Tests
From the `engine/` directory:
```bash
# Run unit tests
./gradlew test

# Check code style (Google Java Style)
./gradlew checkstyleMain
./gradlew checkstyleTest

# Auto-format code
./gradlew spotlessApply
```

### JavaScript Editor Tests
From the `editor/` directory:
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

All of these checks are described in detail in [DEVELOPING.md](DEVELOPING.md). If you run into issues with any of these checks, please don't hesitate to ask for help in your pull request. We understand that some of these tools can be tricky to set up, especially if you're new to the project, and we're happy to guide you through the process.

<br>
<br>

## Design Choices
There are reasonable differences of opinion in the community about ideal implementations. That in mind, there are a few opinionated choices we've made in the design of this project that we will maintain moving forward. We encourage folks in our community to open issues if they wish to discuss these design choices further but, at this time, we may not merge pull requests that do not conform to these choices.

### QubecTalk Domain Specific Language
 - QubecTalk is intentionally designed to be accessible to non-programmers while remaining expressive for technical users.
 - The language specification is maintained at https://kigalisim.org/guide/qubectalk.pdf.
 - We use ANTLR4 for parsing QubecTalk.

### Browser-Based Application
 - We compile the Java engine to WebAssembly using TeaVM for browser execution while maintaining standalone CLI capability.
 - All simulations run locally on the user's machine for privacy.
 - We do not use CDNs for privacy reasons and CDN migration PRs are discouraged.
 - We use vanilla JavaScript (not TypeScript or other transpiled languages) for the editor interface.

### Architecture
 - The engine is designed to support both UI-based (Designer) and code-based (Editor) authoring.
 - We support Monte Carlo probabilistic simulation with parallelization capabilities but assume Monte Carlo users primarily operate on the command line with the JVM.
 - We accept that some advanced simulations will not be compatible with the UI-based editor.

Please reach out if you have further questions about these guidelines or want to discuss alternative approaches.

<br>
<br>

## Scope
There are some limitations of scope for this project that we will enforce:

 - The Java engine must remain compatible with Java 19+ to support TeaVM compilation to WebAssembly.
 - The web editor must work without transpilation or compilation steps for JavaScript (we use webpack only for bundling).
 - Changes to QubecTalk language syntax should be discussed in an issue before implementation.
 - We maintain both the browser-based application and standalone JAR/Docker execution paths.

Please open issues with other ideas!

<br>
<br>

## Development Environment
We support both dev container and local development setups. See [DEVELOPING.md](DEVELOPING.md) for complete setup instructions.

### Quick Start with Dev Container
The easiest way to get started is using the dev container:

 - **VS Code**: Install the Dev Containers extension and reopen in container
 - **GitHub Codespaces**: Click "Code" → "Codespaces" → "Create codespace on main"

The dev container includes all dependencies and tools pre-configured.

### Local Setup
If you prefer local development:

 1. Install Java 19+ (recommend [Adoptium](https://adoptium.net))
 2. Install Node.js 18.x and pnpm
 3. Follow the detailed steps in [DEVELOPING.md](DEVELOPING.md)

<br>
<br>

## Procedure
By contributing, you attest that you are legally permitted to provide code to the project and agree to release that code under the [project's BSD-3-Clause license](LICENSE.md). To make a contribution, please:

 - If one is not already open, [open an issue](https://github.com/SchmidtDSE/kigali-sim/issues).
 - [Open a pull request](https://github.com/SchmidtDSE/kigali-sim/pulls).
 - Mark the pull request as draft until you pass checks and are ready for review.
 - Indicate that your pull request closes your issue by saying "closes #" followed by your issue number in the PR description.
 - Request a review when ready.

If you would like to contribute code but don't have a specific issue to address, thank you! Please look for issues tagged "good first issue" or reach out to dse@berkeley.edu.

<br>
<br>

## Use of Generative AI
We welcome use of AI coding assistants under some guidelines. First, please disclosue their use for any non-mechanical / non-trivial edits and, if you are uncertain, error on the side of caution by disclosing anyway. We suggest that you mention the model you used as a co-author in your commit. Prompts should be narrow and all generated work should be manually evaluated before submitting to the repository either in a main / deploy branch or in a PR branch. We view these resources as tools but you remain responsible for all actions taken. As with any edit, the maintainers reserve the right to politely decline contributions and, in particular, be sure that any AI-assisted edits follow the norms, conventions, and architecture of the existing code.

<br>
<br>

## Documentation
When adding new features or changing existing functionality:

 - Update relevant documentation in the tutorial and users's guide if appropriate.
 - Add JavaDoc comments for Java classes and methods
 - Add JSDoc comments for JavaScript functions
 - Update [DEVELOPING.md](DEVELOPING.md) if you change development workflows

<br>
<br>

## Getting Help
If you're stuck or unsure about anything:

 - Ask questions in your issue or pull request
 - Review [DEVELOPING.md](DEVELOPING.md) for detailed development instructions
 - Check existing issues and pull requests for similar questions
 - Reach out to dse@berkeley.edu

We want contributing to be a positive experience, so please don't hesitate to ask for help at any stage of the process.

<br>
<br>

## Parting Thoughts
Open source is an act of love. Please be kind and respectful of all contributors. Your work on Kigali Sim helps support countries implementing the Kigali Amendment and contributes to global climate action. Thank you for being part of this community.

For more information about community standards, please see [CONDUCT.md](CONDUCT.md) for our Code of Conduct.
