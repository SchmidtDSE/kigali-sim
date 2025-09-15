#!/usr/bin/env python3
"""
Script to fix remaining engine.setStream calls in test files.
"""

import re

def fix_single_thread_engine_test():
    """Fix the SingleThreadEngineTest.java file."""
    file_path = '/home/ubuntu/kigali-sim/engine/src/test/java/org/kigalisim/engine/SingleThreadEngineTest.java'

    with open(file_path, 'r') as f:
        content = f.read()

    original_content = content

    # Pattern to match engine.setStream calls
    pattern = r'(\s+)engine\.setStream\("([^"]+)",\s*([^,]+),\s*([^)]+)\);'

    counter = 10  # Start from 10 to avoid conflicts with previous updates

    def replace_setstream(match):
        nonlocal counter
        indent = match.group(1)
        name = match.group(2)
        value = match.group(3)
        year_matcher = match.group(4)

        replacement = f"""{indent}StreamUpdate update{counter} = new StreamUpdateBuilder()
{indent}    .setName("{name}")
{indent}    .setValue({value})
{indent}    .setYearMatcher({year_matcher})
{indent}    .inferSubtractRecycling()
{indent}    .build();
{indent}engine.executeStreamUpdate(update{counter});"""

        counter += 1
        return replacement

    # Replace all setStream calls
    content = re.sub(pattern, replace_setstream, content, flags=re.MULTILINE)

    # Handle the multi-line setStream call case
    multiline_pattern = r'(\s+)engine\.setStream\("([^"]+)",\s*([^,]+),\s*\n\s*([^)]+)\);'

    def replace_multiline_setstream(match):
        nonlocal counter
        indent = match.group(1)
        name = match.group(2)
        value = match.group(3)
        year_matcher = match.group(4)

        replacement = f"""{indent}StreamUpdate update{counter} = new StreamUpdateBuilder()
{indent}    .setName("{name}")
{indent}    .setValue({value})
{indent}    .setYearMatcher({year_matcher})
{indent}    .inferSubtractRecycling()
{indent}    .build();
{indent}engine.executeStreamUpdate(update{counter});"""

        counter += 1
        return replacement

    content = re.sub(multiline_pattern, replace_multiline_setstream, content, flags=re.MULTILINE)

    if content != original_content:
        with open(file_path, 'w') as f:
            f.write(content)
        print(f"Fixed {file_path}")
    else:
        print(f"No changes needed for {file_path}")

def fix_floor_operation_test():
    """Fix the FloorOperationTest.java file."""
    file_path = '/home/ubuntu/kigali-sim/engine/src/test/java/org/kigalisim/lang/operation/FloorOperationTest.java'

    with open(file_path, 'r') as f:
        content = f.read()

    original_content = content

    # Replace the specific setStream call
    old_call = '    engine.setStream("import", new EngineNumber(BigDecimal.valueOf(50), "kg"), Optional.empty());'
    new_call = '''    StreamUpdate importUpdate = new StreamUpdateBuilder()
        .setName("import")
        .setValue(new EngineNumber(BigDecimal.valueOf(50), "kg"))
        .setYearMatcher(Optional.empty())
        .inferSubtractRecycling()
        .build();
    engine.executeStreamUpdate(importUpdate);'''

    content = content.replace(old_call, new_call)

    if content != original_content:
        with open(file_path, 'w') as f:
            f.write(content)
        print(f"Fixed {file_path}")
    else:
        print(f"No changes needed for {file_path}")

def main():
    """Main function."""
    fix_single_thread_engine_test()
    fix_floor_operation_test()

if __name__ == '__main__':
    main()