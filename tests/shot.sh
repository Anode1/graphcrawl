#!/bin/sh
# shot.sh -- screenshot a page with headless Chrome, so the rendered graph can
# be looked at instead of guessed from the JS. Never touches a real display:
# --headless=new --ozone-platform=headless. Dev-only, not built, not shipped.
#
#   sh tests/shot.sh URL [OUT.png] [WIDTHxHEIGHT] [BUDGET_MS]
#
# BUDGET_MS is virtual time the page runs before the capture (the 1 s
# simulated network of demo.html needs more than 1000).

URL=$1
OUT=${2:-shot.png}
SIZE=${3:-1000x700}
BUDGET=${4:-4000}
[ -n "$URL" ] || { echo "usage: $0 URL [OUT.png] [WxH] [BUDGET_MS]" >&2; exit 2; }

BROWSER=$(command -v google-chrome-stable || command -v google-chrome || command -v chromium || command -v chromium-browser)
[ -n "$BROWSER" ] || { echo "no chrome/chromium on PATH" >&2; exit 1; }

W=${SIZE%x*}; H=${SIZE#*x}
PROFILE=$(mktemp -d)
trap 'rm -rf "$PROFILE"' EXIT

"$BROWSER" --headless=new --ozone-platform=headless --disable-gpu --hide-scrollbars \
	--no-first-run --user-data-dir="$PROFILE" --window-size="$W,$H" \
	--virtual-time-budget="$BUDGET" --screenshot="$OUT" "$URL" >/dev/null 2>&1

if [ -s "$OUT" ]; then echo "wrote $OUT ($SIZE)"; else echo "capture failed" >&2; exit 1; fi
