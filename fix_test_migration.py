#!/usr/bin/env python3
"""
Script to migrate test files from deprecated setStream API to new executeStreamUpdate API.
"""

import os
import re
import sys

def fix_test_file(file_path):
    """Fix a single test file by migrating setStream calls to executeStreamUpdate."""
    with open(file_path, 'r') as f:
        content = f.read()

    original_content = content

    # Add imports if not already present
    if 'StreamUpdate' not in content:
        import_section = re.search(r'(import org\.kigalisim\.engine\.state\.YearMatcher;)', content)
        if import_section:
            new_imports = import_section.group(1) + '\nimport org.kigalisim.engine.support.StreamUpdate;\nimport org.kigalisim.engine.support.StreamUpdateBuilder;'
            content = content.replace(import_section.group(1), new_imports)

    # Pattern to match setStream calls
    setstream_pattern = r'engine\.setStream\(([^,]+),\s*([^,]+),\s*([^)]+)\);'

    # Counter for unique variable names
    counter = 1

    def replace_setstream(match):
        nonlocal counter
        name = match.group(1)
        value = match.group(2)
        year_matcher = match.group(3)

        replacement = f"""StreamUpdate update{counter} = new StreamUpdateBuilder()
        .setName({name})
        .setValue({value})
        .setYearMatcher({year_matcher})
        .inferSubtractRecycling()
        .build();
    engine.executeStreamUpdate(update{counter});"""

        counter += 1
        return replacement

    # Replace all setStream calls
    content = re.sub(setstream_pattern, replace_setstream, content)

    # Only write if content changed
    if content != original_content:
        with open(file_path, 'w') as f:
            f.write(content)
        print(f"Fixed {file_path}")
        return True
    else:
        print(f"No changes needed for {file_path}")
        return False

def main():
    """Main function to process all test files."""
    test_files = [
        '/home/ubuntu/kigali-sim/engine/src/test/java/org/kigalisim/lang/operation/ChangeOperationTest.java',
        '/home/ubuntu/kigali-sim/engine/src/test/java/org/kigalisim/lang/operation/FloorOperationTest.java',
        '/home/ubuntu/kigali-sim/engine/src/test/java/org/kigalisim/lang/operation/GetStreamOperationTest.java',
        '/home/ubuntu/kigali-sim/engine/src/test/java/org/kigalisim/lang/operation/RechargeOperationTest.java',
        '/home/ubuntu/kigali-sim/engine/src/test/java/org/kigalisim/lang/operation/ReplaceOperationTest.java',
        '/home/ubuntu/kigali-sim/engine/src/test/java/org/kigalisim/lang/operation/RetireOperationTest.java',
        '/home/ubuntu/kigali-sim/engine/src/test/java/org/kigalisim/engine/SingleThreadEngineTest.java',
        '/home/ubuntu/kigali-sim/engine/src/test/java/org/kigalisim/engine/recalc/RecalcOperationBuilderIntegrationTest.java',
        '/home/ubuntu/kigali-sim/engine/src/test/java/org/kigalisim/engine/support/ChangeExecutorTest.java'
    ]

    total_fixed = 0
    for file_path in test_files:
        if os.path.exists(file_path):
            if fix_test_file(file_path):
                total_fixed += 1
        else:
            print(f"File not found: {file_path}")

    print(f"\nTotal files fixed: {total_fixed}")

if __name__ == '__main__':
    main()