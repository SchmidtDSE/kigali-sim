#!/usr/bin/env python3
"""
Script to fix SetExecutorTest to work with the new StreamUpdate API.
"""

import re

def fix_set_executor_test():
    """Fix the SetExecutorTest.java file."""
    file_path = '/home/ubuntu/kigali-sim/engine/src/test/java/org/kigalisim/engine/support/SetExecutorTest.java'

    with open(file_path, 'r') as f:
        content = f.read()

    original_content = content

    # Pattern 1: Remove unused ArgumentCaptor declarations and fix the assert block
    old_pattern = r'''(\s+)// Assert\s*\n\s+ArgumentCaptor<String> streamCaptor = ArgumentCaptor\.forClass\(String\.class\);\s*\n\s+ArgumentCaptor<EngineNumber> valueCaptor = ArgumentCaptor\.forClass\(EngineNumber\.class\);\s*\n\s*\n\s+ArgumentCaptor<StreamUpdate> updateCaptor = ArgumentCaptor\.forClass\(StreamUpdate\.class\);\s*\n\s+verify\(mockEngine, times\(2\)\)\.executeStreamUpdate\(updateCaptor\.capture\(\)\);\s*\n\s*\n\s+// Check domestic call \([^)]+\)\s*\n\s+assertEquals\("domestic", streamCaptor\.getAllValues\(\)\.get\(0\)\);\s*\n\s+assertEquals\(([^,]+), valueCaptor\.getAllValues\(\)\.get\(0\)\.getValue\(\)\);\s*\n\s+assertEquals\("([^"]+)", valueCaptor\.getAllValues\(\)\.get\(0\)\.getUnits\(\)\);\s*\n\s*\n\s+// Check import call \([^)]+\)\s*\n\s+assertEquals\("import", streamCaptor\.getAllValues\(\)\.get\(1\)\);\s*\n\s+assertEquals\(([^,]+), valueCaptor\.getAllValues\(\)\.get\(1\)\.getValue\(\)\);\s*\n\s+assertEquals\("([^"]+)", valueCaptor\.getAllValues\(\)\.get\(1\)\.getUnits\(\)\);'''

    def replace_assert_block(match):
        indent = match.group(1)
        domestic_value = match.group(2)
        units1 = match.group(3)
        import_value = match.group(4)
        units2 = match.group(5)

        return f'''{indent}// Assert
{indent}ArgumentCaptor<StreamUpdate> updateCaptor = ArgumentCaptor.forClass(StreamUpdate.class);
{indent}verify(mockEngine, times(2)).executeStreamUpdate(updateCaptor.capture());

{indent}List<StreamUpdate> capturedUpdates = updateCaptor.getAllValues();

{indent}// Check domestic call
{indent}assertEquals("domestic", capturedUpdates.get(0).getName());
{indent}assertEquals({domestic_value}, capturedUpdates.get(0).getValue().getValue());
{indent}assertEquals("{units1}", capturedUpdates.get(0).getValue().getUnits());

{indent}// Check import call
{indent}assertEquals("import", capturedUpdates.get(1).getName());
{indent}assertEquals({import_value}, capturedUpdates.get(1).getValue().getValue());
{indent}assertEquals("{units2}", capturedUpdates.get(1).getValue().getUnits());'''

    content = re.sub(old_pattern, replace_assert_block, content, flags=re.MULTILINE | re.DOTALL)

    # Handle test cases that don't have the full pattern - just fix individual assertions
    content = re.sub(r'assertEquals\("domestic", streamCaptor\.getAllValues\(\)\.get\(0\)\);',
                     'assertEquals("domestic", capturedUpdates.get(0).getName());', content)
    content = re.sub(r'assertEquals\("import", streamCaptor\.getAllValues\(\)\.get\(1\)\);',
                     'assertEquals("import", capturedUpdates.get(1).getName());', content)

    content = re.sub(r'assertEquals\(([^,]+), valueCaptor\.getAllValues\(\)\.get\(0\)\.getValue\(\)\);',
                     r'assertEquals(\1, capturedUpdates.get(0).getValue().getValue());', content)
    content = re.sub(r'assertEquals\(([^,]+), valueCaptor\.getAllValues\(\)\.get\(1\)\.getValue\(\)\);',
                     r'assertEquals(\1, capturedUpdates.get(1).getValue().getValue());', content)

    content = re.sub(r'assertEquals\("([^"]+)", valueCaptor\.getAllValues\(\)\.get\(0\)\.getUnits\(\)\);',
                     r'assertEquals("\1", capturedUpdates.get(0).getValue().getUnits());', content)
    content = re.sub(r'assertEquals\("([^"]+)", valueCaptor\.getAllValues\(\)\.get\(1\)\.getUnits\(\)\);',
                     r'assertEquals("\1", capturedUpdates.get(1).getValue().getUnits());', content)

    # Remove the old unused captor declarations that remain
    content = re.sub(r'\s+ArgumentCaptor<String> streamCaptor = ArgumentCaptor\.forClass\(String\.class\);\s*\n', '', content)
    content = re.sub(r'\s+ArgumentCaptor<EngineNumber> valueCaptor = ArgumentCaptor\.forClass\(EngineNumber\.class\);\s*\n', '', content)

    if content != original_content:
        with open(file_path, 'w') as f:
            f.write(content)
        print(f"Fixed {file_path}")
    else:
        print(f"No changes needed for {file_path}")

def main():
    """Main function."""
    fix_set_executor_test()

if __name__ == '__main__':
    main()