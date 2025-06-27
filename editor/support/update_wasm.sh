#!/bin/bash

# Script to build and extract TeaVM outputs from the engine WAR file and update the wasm directory
# Usage: ./update_wasm.sh (when run from support directory)

# Get the script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Change to the project root
cd "$PROJECT_ROOT"

# Change to the engine directory
cd engine

# Build the WAR file
echo "Building WAR file..."
./gradlew war

# Check if the WAR file was created
if [ ! -f "build/libs/KigaliSim.war" ]; then
    echo "Error: WAR file not found at build/libs/KigaliSim.war"
    exit 1
fi

# Change back to the project root
cd "$PROJECT_ROOT"

# Use the extraction script
echo "Extracting WASM files..."
bash support/update_wasm_from_war.sh engine/build/libs/KigaliSim.war

echo "WASM files updated successfully"