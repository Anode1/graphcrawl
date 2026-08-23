#!/bin/sh
# film.sh -- the README's moving figure: tests/film.html captured frame by
# frame with headless Chrome at successive virtual-time budgets (each frame
# a fresh, deterministic page run), assembled with ImageMagick the way
# cjitter's erd_settle.gif is: 10 frames a second, twelve and a half seconds
# of motion and a short hold on the last frame, 700 px wide. The film opens on
# motion and loops forever, so a README shows it moving. Not part of the
# tests; the output is a committed fixture.
#
#   sh tests/film.sh [GRAPH] [OUT.gif]       default example/packages.txt, screenshots/crawl.gif
#
# Needs ./graphcrawl, google-chrome or chromium, convert (ImageMagick).

GRAPH=${1:-example/packages.txt}
OUT=${2:-screenshots/crawl.gif}
FRAMES=115          # 11.5 s at 100 ms of virtual time per frame
STEP=100
PORT=18570
W=1000; H=520       # resized to 700 below: 110 nodes need the pixels

[ -s "$GRAPH" ] || { echo "no $GRAPH: apt-cache dumpavail | util/aptgraph.py > $GRAPH" >&2; exit 1; }
command -v convert >/dev/null 2>&1 || { echo "convert (ImageMagick) not found" >&2; exit 1; }

D=$(mktemp -d)
trap 'kill $SRV 2>/dev/null; rm -rf "$D"' EXIT
GRAPHCRAWL_NO_OPEN=1 GRAPHCRAWL_WEB=tests ./graphcrawl -p $PORT "$GRAPH" >/dev/null 2>&1 &
SRV=$!
sleep 0.5

i=1
while [ $i -le $FRAMES ]; do
	n=$(printf '%04d' $i)
	sh tests/shot.sh "http://127.0.0.1:$PORT/film.html" "$D/f$n.png" "${W}x${H}" $((i * STEP)) >/dev/null || exit 1
	[ $((i % 10)) -eq 0 ] && echo "frame $i/$FRAMES"
	i=$((i + 1))
done

convert -delay 10 -loop 0 "$D"/f*.png -resize 700x \
        \( +clone -set delay 150 \) +swap +delete -layers Optimize "$OUT"
ls -l "$OUT"
