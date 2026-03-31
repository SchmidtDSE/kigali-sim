"""Validate SHA256 hashes of vendored third-party dependencies.

Reads dep_hashes.json and checks each file against its recorded hash.
Exits with code 1 if any file is missing or has a hash mismatch.
Run from the editor/ directory.
"""

import hashlib
import json
import os
import sys


def compute_sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def main():
    manifest_path = os.path.join(os.path.dirname(__file__), "dep_hashes.json")
    with open(manifest_path) as f:
        manifest = json.load(f)

    failures = []

    for local_path, entry in manifest["files"].items():
        expected = entry["sha256"]
        if not os.path.exists(local_path):
            print(f"MISSING  {local_path}")
            failures.append(local_path)
            continue
        actual = compute_sha256(local_path)
        if actual == expected:
            print(f"OK       {local_path}")
        else:
            print(f"MISMATCH {local_path}")
            print(f"         expected: {expected}")
            print(f"         actual:   {actual}")
            failures.append(local_path)

    print()
    if failures:
        print(f"FAILED: {len(failures)} file(s) did not pass hash validation.")
        sys.exit(1)
    else:
        print(f"All {len(manifest['files'])} files passed hash validation.")


if __name__ == "__main__":
    main()
