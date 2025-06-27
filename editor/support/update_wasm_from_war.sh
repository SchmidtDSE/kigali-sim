#!/bin/bash

# Script to extract TeaVM outputs from an existing WAR file and update the wasm directory
# Usage: ./update_wasm_from_war.sh [war_file_path]

# Get WAR file path from argument or use default
WAR_FILE="${1:-KigaliSim.war}"

# Check if the WAR file exists
if [ ! -f "$WAR_FILE" ]; then
    echo "Error: WAR file not found at $WAR_FILE"
    exit 1
fi

# Create wasm directory if it doesn't exist
mkdir -p wasm

# Extract WASM and JS files from the WAR
echo "Extracting WASM files from $WAR_FILE..."
unzip -j -o "$WAR_FILE" "wasm-gc/*" -d wasm/ || echo "No WASM files found in war"
unzip -j -o "$WAR_FILE" "js/*" -d wasm/ || echo "No JS files found in war"

echo "WASM files updated successfully in the wasm directory"