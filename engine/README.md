# Simulation Engine

This directory contains the Java-based simulation engine for Kigali Sim.

## Purpose

The engine provides the core computational capabilities for modeling substances, applications, and policies related to the Montreal Protocol. It can run either as a standalone command-line tool or be compiled to WebAssembly for in-browser execution.

## Structure

- **`src/main/java/`**: Production Java source code
  - `org.kigalisim.engine`: Core simulation logic and state management
  - `org.kigalisim.lang`: QubecTalk language parsing and interpretation
  - `org.kigalisim.command`: Command-line interface implementations
- **`src/test/java/`**: Unit tests and integration tests
- **`src/main/antlr/`**: ANTLR grammar files for QubecTalk parsing
- **`build.gradle`**: Build configuration and dependencies

## Development

### Building

```bash
# Compile Java sources
./gradlew compileJava

# Build standalone JAR
./gradlew fatJar

# Build WebAssembly for browser
./gradlew war
```

### Testing

```bash
# Run all unit tests
./gradlew test

# Run code style checks
./gradlew checkstyleMain
./gradlew checkstyleTest
```

### Usage

#### Standalone Command-Line

```bash
# Build the fat JAR
./gradlew fatJar

# Run simulation with QTA file
java -jar build/libs/kigalisim-fat.jar run example.qta -o output.csv

# Validate QTA file syntax
java -jar build/libs/kigalisim-fat.jar validate example.qta
```

#### WebAssembly Integration

To update the web editor with engine changes:

1. Build the WAR file: `./gradlew war`
2. Extract to editor: `cd ../editor && bash support/update_wasm.sh`

## Development Standards

- Follow Google Java Style Guide conventions
- Maintain comprehensive unit test coverage
- Document all public APIs with Javadoc
- Use checkstyle for code formatting consistency
- Test both standalone and WebAssembly compilation paths