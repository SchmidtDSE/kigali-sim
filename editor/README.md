# Web Editor

This directory contains the web-based editor and analysis tool for the Montreal Protocol Policy Simulation Tool.

## Purpose

The editor provides an interactive web interface for creating simulations, running analyses, and visualizing results. It includes both a basic UI-based editor and an advanced code-based editor using the QubecTalk domain-specific language.

## Structure

- **`js/`**: JavaScript source code for the web application
- **`style/`**: CSS stylesheets and visual assets
- **`test/`**: JavaScript unit tests and integration tests
- **`wasm/`**: WebAssembly files compiled from the Java engine
- **`examples/`**: Sample QTA files demonstrating various features
- **`third_party/`**: External JavaScript libraries and dependencies
- **`support/`**: Build and deployment scripts
- **`intermediate/`**: Generated language parsing files
- **`language/`**: ANTLR grammar and language processing tools

## Development

### Setup

```bash
# Install Node.js dependencies
npm install

# Install frontend dependencies
bash ./support/install_deps.sh

# Build the project
bash ./support/make.sh
```

### Testing

```bash
# Run unit tests and integration tests
npx grunt

# Lint JavaScript code
npx eslint ./js/*.js
npx eslint ./test/*.js
```

### Local Development

```bash
# Start local development server
python -m http.server

# Visit http://localhost:8000 in your browser
```

### Updating Engine

When the Java engine is updated:

```bash
# Update WebAssembly files from engine build
bash ./support/update_wasm.sh
```

## Features

### Basic Editor
- Visual interface for defining applications and substances
- Point-and-click policy creation
- Guided simulation setup

### Advanced Editor
- Full QubecTalk language support with syntax highlighting
- Code completion and error checking
- Advanced modeling capabilities including probabilistic projections

### Analysis & Visualization
- Interactive charts and graphs
- Emissions, consumption, and equipment population metrics
- Data export capabilities
- Custom metric configuration

## Development Standards

- Follow Google JavaScript Style Guide conventions
- Use JSDoc for all public function documentation
- Maintain comprehensive test coverage with QUnit
- Use ESLint for code style enforcement
- Test across multiple browsers and screen sizes