# KigaliSim Development Guide

## Project Structure

```
kigali-sim/
├── editor/              # Web-based editor and interface
│   ├── js/             # JavaScript source files
│   │   ├── main.js     # Main application entry point
│   │   ├── code_editor.js # Code editor functionality
│   │   └── engine_*.js # Engine interface modules
│   ├── intermediate/   # Build outputs
│   │   ├── static/     # Bundled JavaScript
│   │   └── *.js        # Generated parser files
│   ├── test/           # JavaScript tests
│   ├── style/          # CSS stylesheets
│   ├── support/        # Build and setup scripts
│   └── index.html      # Main application HTML
├── engine/             # Java simulation engine (separate directory)
├── docs/               # Documentation
└── README.md           # Project overview
```

## Quick Start

1. **Install dependencies**:
   ```bash
   cd editor
   pnpm install
   bash ./support/install_deps.sh
   ```

2. **Build the project**:
   ```bash
   bash ./support/make.sh
   ```

3. **Start local server**:
   ```bash
   python -m http.server
   ```

## Validation Commands

### JavaScript Editor Validation

Run all validation commands from the `editor/` directory:

#### Linting
```bash
# Lint production code
pnpm exec eslint ./js/*.js

# Lint test code
pnpm exec eslint ./test/*.js
```

#### Testing
```bash
# Run all tests (unit + integration)
pnpm exec grunt

# Run specific test suites
pnpm exec grunt qunit  # Unit tests only
```

### Java Engine Validation

Run from the `engine/` directory:

```bash
# Run unit tests
./gradlew test

# Lint production Java code
./gradlew checkstyleMain

# Lint test Java code
./gradlew checkstyleTest

# Build WASM output
./gradlew war
```

### Full Validation Suite

To run all checks before committing:

```bash
# From editor directory
cd editor
pnpm exec eslint ./js/*.js
pnpm exec eslint ./test/*.js
pnpm exec grunt

# From engine directory (if working on Java)
cd ../engine
./gradlew test
./gradlew checkstyleMain
./gradlew checkstyleTest
```

## Common Development Tasks

### Adding a New JavaScript Module

1. Create the module in `js/`
2. Follow existing naming conventions (e.g., `module_name.js`)
3. Add tests in `test/`
4. Run linting and tests to verify

### Updating the QubecTalk Grammar

1. Modify the grammar file in `language/`
2. Rebuild with `bash ./support/make.sh`
3. Generated parser files will be in `intermediate/`
4. Test changes with example QubecTalk programs

### Updating WASM Engine

1. Make changes in `engine/` directory
2. Build: `cd engine && ./gradlew war`
3. Update editor: `cd editor && bash support/update_wasm.sh`
4. Test the integration in the web interface

## Code Style Guidelines

- Follow `.eslintrc.yml` configuration
- Use `.prettierrc` for formatting
- Follow Google JavaScript Style Guide for ambiguous cases
- Use JSDoc comments for all public functions
- Maintain existing patterns and conventions

## Testing Strategy

### Unit Tests
- Located in `test/` directory
- Run with QUnit framework
- Test individual functions and modules

### Integration Tests
- Test full simulation workflows
- Verify QubecTalk parsing and execution
- Check UI interactions

### Manual Testing
- Test in multiple browsers
- Verify WASM engine integration
- Check responsive design

## Troubleshooting

### Build Failures

If `make.sh` fails:
1. Check Java is installed (for ANTLR)
2. Verify pnpm dependencies are installed
3. Check for syntax errors in grammar files

### Test Failures

If tests fail:
1. Run tests individually to isolate issues
2. Check browser console for errors
3. Verify test data files are present

### Linting Issues

Common fixes:
- Missing semicolons
- Undefined variables
- Unused variables
- Incorrect indentation

## Performance Considerations

- Bundle size target: < 500KB
- Keep WASM engine calls efficient
- Minimize DOM manipulation
- Use requestAnimationFrame for animations

## Deployment

### Preview/Staging
Push to `main` branch → deploys to https://preview.kigalisim.org

### Production
Push to `deploy` branch → deploys to https://kigalisim.org

Deployment is automated via GitHub Actions (`.github/workflows/build.yaml`)

## Getting Help

- Check existing issues on GitHub
- Review documentation in `docs/`
- Consult `llms.txt` for AI assistant context
- Follow commit message conventions from recent history