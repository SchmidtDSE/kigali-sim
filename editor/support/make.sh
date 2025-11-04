#!/bin/bash

# Ensure dependencies are ready
bash support/install_deps.sh
bash support/update_wasm.sh

# Create language directory if it doesn't exist
mkdir -p language

# Copy the authoritative grammar from engine
echo "Copying QubecTalk.g4 from engine..."
GRAMMAR_SOURCE="../engine/src/main/antlr/org/kigalisim/lang/QubecTalk.g4"

if [ ! -f "$GRAMMAR_SOURCE" ]; then
  echo "ERROR: Grammar file not found at $GRAMMAR_SOURCE"
  echo "Please ensure the engine directory is present and contains the grammar file."
  exit 1
fi

cp "$GRAMMAR_SOURCE" language/QubecTalk.g4

# Remove the Java package header (JavaScript doesn't need it)
sed -i '/@header {/,/^}/d' language/QubecTalk.g4

cd language

# Download ANTLR if needed
if [ ! -f antlr-4.13.0-complete.jar ]; then
  echo "Downloading ANTLR..."
  wget https://github.com/antlr/website-antlr4/raw/gh-pages/download/antlr-4.13.0-complete.jar
fi

# Generate JavaScript parser
echo "Generating JavaScript parser from grammar..."
java -jar antlr-4.13.0-complete.jar -Dlanguage=JavaScript QubecTalk.g4 -visitor -o ../intermediate

cd ..

# Build webpack bundle
echo "Building webpack bundle..."
pnpm run build

echo "Build complete!"
