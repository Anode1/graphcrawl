#!/bin/sh
# scale.sh -- the cost of a click on a big synthetic graph, cold and warm.
# Generates N nodes with hubs and long edges (util/mkgraph), checks the file,
# builds the parents side-file by external sort, then times clicks with the
# file's pages evicted from the cache before each (util/evict, no root) and
# again warm. Writes nothing but the files under DIR. POSIX sh.
#
#   sh tests/scale.sh [N] [DIR]        default N=100000000 (about 6 GB), DIR=/tmp
#
# Needs: ./graphcrawl ./util/mkgraph ./util/evict, sort, /usr/bin/time.

N=${1:-100000000}
DIR=${2:-/tmp}
GC=./graphcrawl; MK=./util/mkgraph; EV=./util/evict
F="$DIR/graphcrawl-scale-$N.txt"
# seconds,KB of one command (GNU time; on BSD/macOS substitute gtime)
t() { /usr/bin/time -f %e,%M -o "$DIR/t.$$" "$@" >/dev/null 2>&1; cat "$DIR/t.$$"; }

echo "N=$N file=$F"
if [ ! -s "$F" ]; then
	START=$(date +%s); "$MK" -n "$N" -H 1000 -l 100 -s 11 > "$F"
	echo "generated in $(( $(date +%s) - START )) s"
fi
ls -l "$F" | awk '{print "bytes", $5}'
echo "check (streams the whole file): s,KB = $(t "$GC" --check "$F")"
if [ ! -s "$F.parents" ]; then
	echo "reverse + sort (external sort of every edge)..."
	START=$(date +%s); "$GC" --reverse "$F" | sort -t';' -k1,1n -k2,2n -u -S 2G -T "$DIR" > "$F.parents"
	echo "  $(( $(date +%s) - START )) s, $(ls -l "$F.parents" | awk '{print $5}') bytes"
fi
LAST=$("$GC" --info "$F" | sed -n 's/^last=//p')
echo
echo "click    depth  cold s,KB     warm s,KB     nodes  lines read"
i=1
while [ $i -le 5 ]; do
	ID=$(( (LAST / 7) * i + 12345 ))
	for D in 2 3; do
		"$EV" "$F" "$F.parents"
		COLD=$(t "$GC" --node "$ID" -d "$D" "$F")
		WARM=$(t "$GC" --node "$ID" -d "$D" "$F")
		NODES=$("$GC" --node "$ID" -d "$D" "$F" | wc -l | tr -d ' ')
		PROBES=$("$GC" -D --node "$ID" -d "$D" "$F" 2>&1 >/dev/null | sed -n 's/.*nodes, \([0-9]*\) lines.*/\1/p')
		printf '%-8s %-6s %-13s %-13s %-6s %s\n' "$ID" "$D" "$COLD" "$WARM" "$NODES" "$PROBES"
	done
	i=$((i+1))
done
rm -f "$DIR/t.$$"
