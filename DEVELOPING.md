# Development Guide for KigaliSim

This guide provides comprehensive information for contributors to the KigaliSim project, an open source web-based simulation engine for modeling substances, applications, and policies related to the Montreal Protocol.

## Table of Contents
- [Architecture Overview](#architecture-overview)
- [Development Environment Setup](#development-environment-setup)
- [Building the Project](#building-the-project)
- [Running Tests](#running-tests)
- [Code Quality & Validation](#code-quality--validation)
- [WASM Artifacts Management](#wasm-artifacts-management)
- [Development Workflow](#development-workflow)
- [Deployment Process](#deployment-process)
- [Troubleshooting](#troubleshooting)

## Architecture Overview

KigaliSim is organized into three main components:

### 1. **Java Engine** (`engine/`)
- Core simulation engine built with Java 19+
- Uses ANTLR4 for parsing the QubecTalk DSL
- Compiled to WebAssembly using TeaVM for browser execution
- Can also run standalone via command line

### 2. **Web Editor** (`editor/`)
- JavaScript-based web interface
- Provides both Basic (UI) and Advanced (code) editing modes
- Uses webpack for bundling and Grunt for testing
- Integrates with the WASM-compiled engine

### 3. **Documentation** (`docs/`)
- Technical specifications and user guides
- QubecTalk language documentation

## Development Environment Setup

Both dev containers and local setup are supported, per developer preference.

### Option 1: Using Dev Container

The project includes a complete dev container configuration for VS Code and GitHub Codespaces.

#### VS Code Setup
1. Install the [Dev Containers extension](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers)
2. Open the project in VS Code
3. Press `F1` and select "Dev Containers: Reopen in Container"
4. The container will build with all required dependencies

#### GitHub Codespaces
1. Navigate to the repository on GitHub
2. Click the green "Code" button
3. Select the "Codespaces" tab
4. Click "Create codespace on main"

#### Dev Container Configuration

The dev container (`.devcontainer/`) provides a reproducible development environment with all dependencies pre-installed. It uses:

**Base System:**
- Debian 12 (bookworm-slim) with Eclipse Temurin JDK 21
- All packages from official Debian and Eclipse Temurin repositories

**Development Tools:**
- Node.js 18.x LTS (official Debian package)
- pnpm 10.20.0 (installed via npm)
- Chromium browser (official Debian package) for running tests
- Build tools: git, curl, wget, build-essential, python3
- VS Code extensions for Java, JavaScript, and Gradle development

**Configuration Features:**
- Automatic UID/GID mapping to match host user (prevents permission issues)
- Pre-configured ports: 8000 and 8080 for development servers
- Automatic dependency installation on container creation
- WASM artifacts built automatically during setup

The container configuration prioritizes security by using only official package repositories (Debian and Eclipse Temurin) without third-party PPAs.

### Option 2: Local Setup

#### Prerequisites
- Java JDK 19 or higher (TeaVM requirement)
- Node.js 18.x and pnpm
- Python 3.x (for local development server)

#### Installation Steps
1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-org/kigali-sim.git
   cd kigali-sim
   ```

2. **Set up the JavaScript editor:**
   ```bash
   cd editor
   pnpm install
   bash ./support/install_deps.sh
   ```

3. **Build the project:**
   ```bash
   bash ./support/make.sh
   ```

4. **Run a local development server:**
   ```bash
   python -m http.server 8000
   ```

5. **Access the application:**
   Open your browser to `http://localhost:8000`

## Building the Project

### Building the Java Engine

From the `engine/` directory:

```bash
# Build the engine
./gradlew build

# Build the WAR file with WASM output
./gradlew war

# Build a standalone executable JAR
./gradlew fatJar
```

### Building the JavaScript Editor

From the `editor/` directory:

```bash
# Install dependencies
pnpm install

# Build the QubecTalk parser and webpack bundle
bash ./support/update_wasm.sh
bash ./support/make.sh

# Or build webpack directly
pnpm run build
```

## Running Tests

### Java Engine Tests

From the `engine/` directory:

```bash
# Run all unit tests
./gradlew test

# Run with detailed output
./gradlew test --info
```

### JavaScript Editor Tests

From the `editor/` directory:

```bash
# Run QUnit tests with Grunt
pnpm exec grunt

# The Grunt task will:
# 1. Copy examples for testing
# 2. Start a local server on port 8000
# 3. Run tests in Chrome (with fallback to --no-sandbox if needed)
# 4. Clean up test artifacts
```

## Code Quality & Validation

### Java Code Validation

From the `engine/` directory:

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

### JavaScript Code Validation

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

#### JavaScript
- Follows Google JavaScript Style Guide
- Configuration in `editor/.eslintrc.yml`
- Prettier configuration in `editor/.prettierrc`
- Key rules:
  - 2-space indentation
  - Double quotes for strings
  - Maximum line length: 100 characters
  - Trailing commas in multi-line structures

#### Java
- Follows Google Java Style Guide
- Checkstyle configuration in `engine/config/checkstyle/google_checks.xml`
- Auto-formatting via Spotless
- Key rules:
  - 2-space indentation
  - No tabs (converted to spaces)
  - Proper import ordering

## WASM Artifacts Management

The Java engine is compiled to WebAssembly for browser execution. Here's how to update the WASM artifacts:

### Method 1: Full Build and Update (Recommended)

From the `editor/` directory:

```bash
# Builds the engine and extracts WASM files
bash support/update_wasm.sh
```

This script:
1. Builds the engine WAR file using `./gradlew war`
2. Extracts the TeaVM-generated WASM and JS files
3. Places them in the `editor/wasm/` directory

### Method 2: Extract from Existing WAR

If you have a pre-built WAR file:

```bash
# From the editor directory
bash support/update_wasm_from_war.sh /path/to/KigaliSim.war
```

### WASM Build Details

The WASM compilation is configured in `engine/build.gradle`:
- Uses TeaVM version 0.11.0
- Main class: `org.kigalisim.KigaliWasmSimFacade`
- Generates both JavaScript and WASM-GC outputs
- Output is included in the WAR file under `wasm-gc/` and `js/` directories

## Development Workflow

### 1. Branch Strategy
```bash
# Create a feature branch from main
git checkout -b feature/your-feature-name

# For bug fixes
git checkout -b fix/issue-description
```

### 2. Making Changes

#### For Java Engine Development:
1. Make changes in `engine/src/`
2. Run tests: `./gradlew test`
3. Check style: `./gradlew checkstyleMain`
4. Format code: `./gradlew spotlessApply`
5. Build WASM: `./gradlew war`
6. Update editor: `cd ../editor && bash support/update_wasm.sh`

#### For JavaScript Editor Development:
1. Make changes in `editor/js/`
2. Build: `pnpm run build` or `bash support/make.sh`
3. Lint: `pnpm exec eslint ./js/*.js`
4. Test: `pnpm exec grunt`

### 3. Testing Your Changes
1. Start local server: `python -m http.server 8000`
2. Open browser to `http://localhost:8000`
3. Test both Basic and Advanced editor modes
4. Verify simulations run correctly

### 4. Pre-Commit Checklist

Before committing, ensure:

- [ ] All tests pass (`./gradlew test` and `pnpm exec grunt`)
- [ ] Code is properly formatted
- [ ] Linting passes with no errors
- [ ] WASM artifacts are updated if engine changed
- [ ] Documentation is updated if needed
- [ ] No sensitive information or secrets in code

### 5. Commit Message Format
```
type(scope): brief description

- Detailed change 1
- Detailed change 2

Fixes #issue-number
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

## Deployment Process

### Preview/Staging Deployment
- Push to the `main` branch
- GitHub Actions automatically deploys to https://preview.kigalisim.org

### Production Deployment
- Push to the `deploy` branch
- GitHub Actions automatically deploys to https://kigalisim.org

### GitHub Actions Workflow
The deployment process is defined in `.github/workflows/build.yaml` and includes:
1. Building the Java engine
2. Running all tests
3. Building the JavaScript editor
4. Deploying to the appropriate environment

## Troubleshooting

### Common Issues and Solutions

#### 1. Chrome Sandbox Errors During Testing
The Grunt test configuration automatically falls back to `--no-sandbox` mode if needed. This is handled by the `qunit-with-fallback` task.

#### 2. ANTLR Grammar Not Generating
```bash
# From engine directory
./gradlew clean generateGrammarSource
```
Generated files will be in `engine/src-generated/main/java/`

#### 3. WASM Files Not Loading
1. Ensure the WAR was built: `cd engine && ./gradlew war`
2. Update WASM files: `cd editor && bash support/update_wasm.sh`
3. Check browser console for loading errors

#### 4. Port 8000 Already in Use
```bash
# Use a different port
python -m http.server 8080

# Or kill the process using port 8000
lsof -i :8000
kill -9 <PID>
```

#### 5. JavaScript Build Failures
```bash
# Clean and reinstall dependencies
cd editor
rm -rf node_modules pnpm-lock.yaml
pnpm install
bash support/install_deps.sh
```

#### 6. Java Version Mismatch
Ensure Java 19+ is installed and active:
```bash
java -version
# Should show version 19 or higher
```

#### 7. No printf debugging
Please review `build.gradle` for information about output being suppressed during tests.

### Getting Help

- Check existing issues on GitHub
- Consult the QubecTalk language documentation in `llms.txt`
- Review the main README.md for usage instructions
- For TeaVM-specific issues, check the [TeaVM documentation](https://teavm.org/)

## Additional Resources

- [Google JavaScript Style Guide](https://google.github.io/styleguide/jsguide.html)
- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- [ANTLR4 Documentation](https://www.antlr.org/)
- [TeaVM Documentation](https://teavm.org/)
- [QUnit Testing Framework](https://qunitjs.com/)
- [Grunt Task Runner](https://gruntjs.com/)

## Contributing

1. Fork the repository
2. Create your feature branch
3. Make your changes following the guidelines above
4. Ensure all tests pass and code is properly formatted
5. Submit a pull request with a clear description of changes

Remember to:
- Document any new features or API changes
- Add tests for new functionality
- Update this guide if you change the development process
- Disclose any use of generative AI (per project policy)
