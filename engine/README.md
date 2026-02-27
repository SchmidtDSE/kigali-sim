# Simulation Engine

This directory contains the Java-based simulation engine for Kigali Sim which is also compiled to WASM via TeaVM.

## Purpose

The engine provides the core computational capabilities for modeling substances, applications, and policies related to the Montreal Protocol. It can run either as a standalone command-line tool or be compiled to WebAssembly for in-browser execution.

## Structure

The **`src/main/java`** directory contains production Java source code. It contains the following packages:

  - `org.kigalisim.engine`: Core simulation logic and state management
  - `org.kigalisim.lang`: QubecTalk language parsing and interpretation
  - `org.kigalisim.command`: Command-line interface implementations
  - `org.kigalisim.cloud`: Lambda/cloud endpoint handling and invocation parameters
  - `org.kigalisim.util`: Shared utilities

Additionally, see the following:

 - **`src/test/java/`**: Unit tests and integration tests
 - **`src/main/antlr/`**: ANTLR grammar files for QubecTalk parsing
 - **`build.gradle`**: Build configuration and dependencies

## Development

Compile Java sources:

```bash
./gradlew compileJava
```

Build standalone JAR:

```bash
./gradlew fatJar
```

Build WebAssembly for browser:

```
./gradlew war
```

Run all unit tests:

```bash
./gradlew test
```

Run code style checks:

```bash
./gradlew checkstyleMain
./gradlew checkstyleTest
```

## Usage

After building the fat jar, run simulation with a QTA (QubecTalk) file:

```bash
java -jar build/libs/kigalisim-fat.jar run example.qta -o output.csv
```

You may also validate QTA file syntax instead of running:

```bash
java -jar build/libs/kigalisim-fat.jar validate example.qta
```

## WebAssembly Integration

To update the web editor with engine changes:

1. Build the WAR file: `./gradlew war`
2. Extract to editor: `cd ../editor && bash support/update_wasm.sh`

## Development Standards

Please see DEVELOPING.md but, breifly, do not forget to:

- Follow Google Java Style Guide conventions
- Maintain comprehensive unit test coverage
- Document all public APIs with Javadoc
- Use checkstyle for code formatting consistency
- Test both standalone and WebAssembly compilation paths

## Cloud Endpoint
To support the community, a public community cloud endpoint is available. Unlike the kigalisim.org web IDE (which runs simulations locally via WebAssembly) or jar file (which runs simulations locally via JVM), this endpoint transmits your QubecTalk script to a remote server for processing. See [privacy policy](https://kigalisim.org/privacy.html) for details on data handling.

Endpoint: `https://bbaagift7g5fsza7xzxksl7uny0jjwvn.lambda-url.us-east-2.on.aws/`

Example request (URL-encoded):
```
GET https://bbaagift7g5fsza7xzxksl7uny0jjwvn.lambda-url.us-east-2.on.aws/?script=start%20default%0A...%0Aend%20default%0A...&simulation=Business%20as%20Usual
```

The `simulation` parameter accepts comma-separated scenario names (e.g., `simulation=Scenario+One,Scenario+Two`) to run multiple simulations in a single call and receive their results combined in one CSV response.

An optional `replicates` integer parameter (e.g., `replicates=5`) controls how many times each simulation is run per call, with results for all replicates combined in the response; values less than 1 return HTTP 400.
