# Development Guide for KigaliSim

This guide provides comprehensive information for contributors to the Kigali Sim project, an open source web-based simulation engine for modeling substances, applications, and policies related to the Montreal Protocol.

## Documentation

Note that, in addition to this documentation and [CONTRIBUTING.md](https://github.com/SchmidtDSE/kigali-sim/blob/main/CONTRIBUTING.md) many directories within this project's repository also contain their own README files with additional information about that component or area of code.

## Architecture Overview

KigaliSim is organized into three main components and most have additional READMEs within:

The **Java Engine** (`engine` directory) provides the primary computational mechanism for running simulations. There are different entrypoints depending on how Kiglai Sim is running like via WASM or JVM. That said, developers may wish to review the [Engine JavaDoc](https://kigalisim.org/guide/javadoc/org/kigalisim/engine/Engine.html). At a glance:

- Core simulation engine built with Java 21+
- Uses ANTLR4 for parsing the QubecTalk domain specific language (DSL)
- Contains the authoritative QubecTalk.g4 grammar file
- Compiled to WebAssembly using TeaVM for browser execution
- Can also run standalone via command line

The **Web Editor** (`editor` directory) provides a bespoke IDE with built-in visualization capability running via WASM. At a glance:

- ECMAScript-based web interface
- Copies QubecTalk.g4 from engine during build process
- Generates JavaScript parser from grammar using ANTLR
- Provides both Basic (UI) and Advanced (code) editing modes
- Uses webpack for bundling and Grunt for testing
- Integrates with the WASM-compiled engine

The **Documentation** (`docs` directory) component includes the User's Guide and various reference resources. This also includes the QubecTalk formal lanugage specification.

## Development Environment Setup

Both dev containers and local setup are supported, per developer preference.

### Using Dev Container

The project includes a dev container which is compatible with IntelliJ, VS Code, and GitHub Codespaces. As described in `.devcontainer`, this provides a reproducible development environment with dependencies required for most development activities pre-installed. Indeed, this guide was written from a dev container! It uses:

- Debian 12 (bookworm-slim) with Eclipse Temurin JDK 21
- Node.js 18.x LTS (official Debian package)
- pnpm 10.20.0 (installed via npm)
- Chromium browser (official Debian package) for running tests
- Build tools: git, curl, wget, build-essential, python3
- VS Code extensions for Java, JavaScript, and Gradle development

Note that it also configures the following:

- Automatic UID/GID mapping to match host user (prevents permission issues)
- Port forwarding of 8000 and 8080 for development servers
- Automatic dependency installation on container creation
- WASM artifacts built automatically during setup

The container configuration prioritizes security by using only official package repositories (Debian and Eclipse Temurin) without third-party PPAs.

### Local Setup
You are very welcome to set up your own environment outside of a pre-prepared container. You will need the following:

- Java JDK 21 or higher (please use only OpenJDK or similar, see [Adoptium](https://adoptium.net/))
- Node.js 18.x and pnpm
- Python 3.x (for local development server)

After installing those packages based on your operating sytem:

1. Clone the repository:

   ```bash
   git clone https://github.com/your-org/kigali-sim.git
   cd kigali-sim
   ```

2. Build the engine and Java jar:

   ```bash
   cd engine
   ./gradlew build
   ```

If you would like to also work on the UI-based editor or web interface:

1. Build the UI-based editor:

   ```bash
   bash ./support/make.sh
   ```

2. Run a local development server:
   ```bash
   python -m http.server 8000
   ```

## Building

For those working on the **Java-based engine** go to the `engine` directory:

```bash
# Build the engine
./gradlew build

# Build the WAR file with WASM output
./gradlew war

# Build a standalone executable JAR
./gradlew fatJar
```

For those working on the front-end and UI-based editor, go to the `editor` directory:

```bash
# Install dependencies
pnpm install

# Build the QubecTalk parser and webpack bundle
bash ./support/make.sh

# Or build webpack directly
pnpm run build
```

## Testing

We have automated tests for both the engine and editor. From the `engine` directory, execute **Java** tests via:

```bash
# Run all unit tests
./gradlew test

# Run with detailed output
./gradlew test --info
```

From the `editor` directory, execute **front-end** tests via:

```bash
pnpm exec grunt
```

Alternatively, run a local server and go to `test/test.html`.

## Style

We have a number of automated style checks available. From the `engine/` directory:

```bash
# Lint production code using Google Java Style
./gradlew checkstyleMain

# Lint test code
./gradlew checkstyleTest

# Auto-format code using Spotless
./gradlew spotlessApply

# Check formatting without applying changes
./gradlew spotlessCheck
```

From the `editor/` directory:

```bash
# Lint production JavaScript code
pnpm exec eslint ./js/*.js

# Lint test JavaScript code
pnpm exec eslint ./test/*.js

# Auto-fix linting issues where possible
pnpm exec eslint ./js/*.js --fix
```

### Code Style Guidelines

We treat the code style guidelines as a way to help accelerate development and ensure long term maintainability. However, we trust and empower developers and maintainers to adapt or adjust guidelines on a case by case basis as needed in the code in order to achieve the most readable and sustainable otucome. Please use these as tools to start your work but not unbreakable rules.

For those working in **Java**:

- Follows [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Checkstyle configuration in `engine/config/checkstyle/google_checks.xml`
- Auto-formatting via Spotless

For those working in **ECMAScript**:

- Follows [Google JavaScript / TypeScript Style Guide](https://google.github.io/styleguide/tsguide.html)
- Configuration in `editor/.eslintrc.yml`
- Prettier configuration in `editor/.prettierrc`

## WASM

The Java engine is compiled to WebAssembly for browser execution. From the `editor/` directory:

```bash
# Builds the engine and extracts WASM files
bash support/update_wasm.sh
```

This script:

1. Builds the engine WAR file using `./gradlew war`
2. Extracts the TeaVM-generated WASM and JS files
3. Places them in the `editor/wasm/` directory

## Development Workflow

First, please create a feature branch from main:

```bash
git checkout -b feature/your-feature-name

# For bug fixes
git checkout -b fix/issue-description
```

Next, for Java Engine Development:

1. Make changes in `engine/src/`
2. Run tests: `./gradlew test`
3. Check style: `./gradlew checkstyleMain checkstyleTest`
4. Format code: `./gradlew spotlessApply`
5. Build WASM: `./gradlew war`

For front-end or UI-editor development:

1. Make changes in `editor/js/`
2. Build: `pnpm run build` / `bash support/make.sh`
3. Lint: `pnpm exec eslint ./js/*.js`
4. Test: `pnpm exec grunt`

To test your changes to the front-end interactively:

1. Start local server: `python -m http.server 8000`
2. Open browser to `http://localhost:8000`
3. Test both Basic and Advanced editor modes
4. Verify simulations run correctly

Finally, before committing, ensure:

- All tests pass (`./gradlew test` and `pnpm exec grunt`)
- Code is properly formatted
- Linting passes with no errors
- WASM artifacts are updated if engine changed and front-end tests pass
- Documentation is updated if needed

For those working with countries, please be sure to ensure no sensitive information or secrets in code.

## Environments

We have both a staging and a production environment. Start by trying your changes in **staging**:

- Push to the `main` branch
- GitHub Actions automatically deploys to https://preview.kigalisim.org

When ready to go to **production**:

- Push to the `deploy` branch
- GitHub Actions automatically deploys to https://kigalisim.org

In both cases, note that CI / CD will run via GitHub Actions.

## Troubleshooting

There are some common issues we want to mention to those developing on the Kigali Sim codebase.

### Grunt unable to load browser
The Grunt test configuration automatically falls back to `--no-sandbox` mode if needed. This is handled by the `qunit-with-fallback` task. Note that we specifically look for Chrome or Chromium due to slow installs for Firefox but the gruntfile can be modified.

### Not seeing visitor or other ANTLR classes
ANTLR grammar not generating may require a clean:

```bash
# From engine directory
./gradlew clean generateGrammarSource
```
Generated files will be in `engine/src-generated/main/java/`. If WASM files are not loading in the browser:

1. Ensure the WAR was built: `cd engine && ./gradlew war`
2. Update WASM files: `cd editor && bash support/update_wasm.sh`
3. Check browser console for loading errors

### Printf debugging suppressed
Please review `build.gradle` for information about output being suppressed during tests.

## See also

- [Google JavaScript Style Guide](https://google.github.io/styleguide/jsguide.html)
- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- [ANTLR4 Documentation](https://www.antlr.org/)
- [TeaVM Documentation](https://teavm.org/)
- [QUnit Testing Framework](https://qunitjs.com/)
- [Grunt Task Runner](https://gruntjs.com/)
- [Project JavaDoc](https://kigalisim.org/guide/javadoc)
