#!/usr/bin/env bash
# Captures README screenshots of the sample app on a running Android TV emulator.
# D-pad presses can't be timed reliably through adb on a fresh emulator, so the sample focuses
# the right card for each scene from the `scene` extra.
set -euo pipefail
source "$(dirname "$0")/screenshot-lib.sh"

install_sample
for scene in home focused-rail continue; do
  fresh_launch --es scene "$scene"
  capture "$scene"
done
