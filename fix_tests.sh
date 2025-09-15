#!/bin/bash

# Find all Java test files that contain setStream calls
find /home/ubuntu/kigali-sim/engine/src/test -name "*.java" -exec grep -l "setStream(" {} \; | while read file; do
    echo "Processing $file"

    # Add imports if not already present
    if ! grep -q "import org.kigalisim.engine.support.StreamUpdate;" "$file"; then
        # Find the last import line and add our imports after it
        sed -i '/^import org\.kigalisim\.engine/a import org.kigalisim.engine.support.StreamUpdate;\nimport org.kigalisim.engine.support.StreamUpdateBuilder;' "$file"
    fi

    # Replace setStream patterns with executeStreamUpdate patterns
    # This is a simplified replacement - may need manual adjustment for complex cases
    sed -i 's/engine\.setStream(\([^,]*\), \([^,]*\), \([^)]*\))/StreamUpdate update = new StreamUpdateBuilder()\n        .setName(\1)\n        .setValue(\2)\n        .setYearMatcher(\3)\n        .inferSubtractRecycling()\n        .build();\n    engine.executeStreamUpdate(update)/g' "$file"
done