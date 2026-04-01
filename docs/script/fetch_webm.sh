#!/bin/bash
# Downloads webm tutorial files from archive.kigalisim.org into the deploy output.
# These files are too large to store in the repository and are served from a
# long-term static archive instead. See: https://github.com/SchmidtDSE/kigali-sim/issues/776
set -e

DEST=$1

if [ -z "$DEST" ]; then
  echo "Usage: $0 <destination_dir>"
  exit 1
fi

mkdir -p "$DEST"

for file in \
  tutorial_02_01 tutorial_02_02 tutorial_02_03 tutorial_02_04 tutorial_02_05 tutorial_02_06 tutorial_02_07 \
  tutorial_03_01 tutorial_03_02 tutorial_03_03 \
  tutorial_04_01 tutorial_04_02 tutorial_04_03 tutorial_04_04 \
  tutorial_05_01 tutorial_05_02 tutorial_05_03 tutorial_05_04 tutorial_05_05 \
  tutorial_06_01 tutorial_06_02 tutorial_06_03 tutorial_06_04 \
  tutorial_07_01 tutorial_07_02 tutorial_07_03 \
  tutorial_08_01 \
  tutorial_09_01 tutorial_09_02 tutorial_09_03 tutorial_09_04 \
  tutorial_10_01 tutorial_10_02 tutorial_10_03 tutorial_10_04; do
  wget -q -O "${DEST}/${file}.webm" "https://archive.kigalisim.org/webm/${file}.webm"
done
