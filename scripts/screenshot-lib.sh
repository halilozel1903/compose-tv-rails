#!/usr/bin/env bash
# Shared helpers for scripts/screenshots.sh. Expects PKG to be set by the caller's environment
# or falls back to the sample's application id from sample/build.gradle.kts.
PKG="${PKG:-$(grep -oE 'applicationId = "[^"]+"' sample/build.gradle.kts | cut -d'"' -f2)}"
OUT="docs/screenshots"
mkdir -p "$OUT"

install_sample() {
  adb install -r sample/build/outputs/apk/debug/sample-debug.apk
  # A freshly booted TV emulator often shows "System UI isn't responding" or a launcher ANR.
  # Hide error/ANR dialogs and give the system time to settle.
  adb shell settings put global hide_error_dialogs 1
  adb shell settings put global anr_show_background 0
  sleep 20
  dismiss_system_dialogs
}

fresh_launch() {
  adb shell pm clear "$PKG" > /dev/null
  adb shell am start -W -n "$PKG/.MainActivity" "$@" > /dev/null
  sleep 8
}

dismiss_system_dialogs() {
  for _ in 1 2 3; do
    if adb shell dumpsys window | grep -qiE "Application Not Responding|isn't responding"; then
      adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS > /dev/null
      adb shell input keyevent KEYCODE_ENTER
      sleep 2
    else
      return 0
    fi
  done
  if adb shell dumpsys window | grep -qiE "Application Not Responding|isn't responding"; then
    echo "A system dialog is still on screen; refusing to capture a broken screenshot." >&2
    exit 1
  fi
}

# Fails unless the sample is the resumed activity (not the launcher, a dialog or a crash screen).
require_sample_in_front() {
  if ! adb shell dumpsys activity activities | grep -E "mResumedActivity|topResumedActivity" | grep -q "$PKG"; then
    echo "The sample is not in front; refusing to capture a broken screenshot." >&2
    adb shell dumpsys activity activities | grep -E "mResumedActivity|topResumedActivity" >&2 || true
    exit 1
  fi
}

# Fails on blank, tiny or nearly single colored captures (a black screen while the app starts,
# a crashed surface). Decodes the PNG with the Python standard library only.
verify_png() {
  python3 - "$1" <<'PY'
import struct, sys, zlib

path = sys.argv[1]
data = open(path, "rb").read()
if len(data) < 30_000 or data[:8] != b"\x89PNG\r\n\x1a\n":
    sys.exit(f"{path}: not a real screenshot ({len(data)} bytes)")
pos, idat, width, height, color_type = 8, b"", 0, 0, 0
while pos < len(data):
    length, kind = struct.unpack(">I4s", data[pos:pos + 8])
    body = data[pos + 8:pos + 8 + length]
    if kind == b"IHDR":
        width, height, depth, color_type = struct.unpack(">IIBB", body[:10])
        if depth != 8 or color_type not in (2, 6):
            sys.exit(f"{path}: unexpected PNG format (depth {depth}, color type {color_type})")
    elif kind == b"IDAT":
        idat += body
    pos += 12 + length
if width < 1280 or height < 720:
    sys.exit(f"{path}: {width}x{height} is not a TV sized screenshot")
bpp = 4 if color_type == 6 else 3
raw = zlib.decompress(idat)
stride = width * bpp
prev = bytearray(stride)
colors = set()
dark = 0
samples = 0
offset = 0
for y in range(height):
    filter_type = raw[offset]
    line = bytearray(raw[offset + 1:offset + 1 + stride])
    offset += 1 + stride
    if filter_type == 1:
        for i in range(bpp, stride):
            line[i] = (line[i] + line[i - bpp]) & 0xFF
    elif filter_type == 2:
        for i in range(stride):
            line[i] = (line[i] + prev[i]) & 0xFF
    elif filter_type == 3:
        for i in range(stride):
            left = line[i - bpp] if i >= bpp else 0
            line[i] = (line[i] + ((left + prev[i]) >> 1)) & 0xFF
    elif filter_type == 4:
        for i in range(stride):
            a = line[i - bpp] if i >= bpp else 0
            b = prev[i]
            c = prev[i - bpp] if i >= bpp else 0
            p = a + b - c
            pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
            predictor = a if pa <= pb and pa <= pc else (b if pb <= pc else c)
            line[i] = (line[i] + predictor) & 0xFF
    if y % 8 == 0:
        for x in range(0, width, 8):
            r, g, b = line[x * bpp], line[x * bpp + 1], line[x * bpp + 2]
            colors.add((r >> 3, g >> 3, b >> 3))
            samples += 1
            if r + g + b < 30:
                dark += 1
    prev = line
if len(colors) < 64:
    sys.exit(f"{path}: only {len(colors)} distinct colors, the screen looks blank")
if dark / samples > 0.9:
    sys.exit(f"{path}: {dark * 100 // samples}% of the screen is black")
print(f"{path}: {width}x{height}, {len(colors)} colors, looks fine")
PY
}

capture() {
  dismiss_system_dialogs
  require_sample_in_front
  adb exec-out screencap -p > "$OUT/$1.png"
  if ! verify_png "$OUT/$1.png"; then
    rm -f "$OUT/$1.png"
    exit 1
  fi
  echo "Captured $1"
}
