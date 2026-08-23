#!/bin/sh
# run.sh -- the CLI and HTTP tests: the real binary through the shell, then
# the server on a high port queried with curl. POSIX sh. Exit 0 passed,
# 1 a failure, 77 SKIP (curl absent for the HTTP half).
#
#   sh tests/run.sh [path-to-graphcrawl]      (default ./c/graphcrawl)

GC=${1:-./c/graphcrawl}
case $GC in /*) ;; *) GC=$(cd "$(dirname "$GC")" && pwd)/$(basename "$GC") ;; esac
MK=$(dirname "$GC")/../util/mkgraph

T=$(mktemp -d)
PORT=$(( 18090 + ($$ % 2000) ))
SRV=
cleanup() { [ -n "$SRV" ] && kill "$SRV" 2>/dev/null; rm -rf "$T"; }
trap cleanup EXIT
trap 'cleanup; exit 130' INT

pass=0; fail=0
ok()  { case "$3" in *"$2"*) pass=$((pass+1)); echo "  ok   $1";;
                     *) fail=$((fail+1)); echo "  FAIL $1 (want '$2', got '$3')";; esac; }
eq()  { if [ "$2" = "$3" ]; then pass=$((pass+1)); echo "  ok   $1";
        else fail=$((fail+1)); echo "  FAIL $1 (want '$2', got '$3')"; fi; }

# --- a generated graph: sorted by construction, 5000 nodes, both lists on every line
"$MK" -n 5000 -s 7 > "$T/g.txt"
eq "mkgraph emits one line per node" "5000" "$(wc -l < "$T/g.txt" | tr -d ' ')"
ok "check accepts it"                "5000 lines, sorted" "$("$GC" --check "$T/g.txt")"
ok "info: first and last ids"        "first=0" "$("$GC" --info "$T/g.txt")"
ok "info: last"                      "last=4999" "$("$GC" --info "$T/g.txt")"

# --- the neighbourhood: depth 1 is the node, depth 2 its children and parents
eq "node depth 1 is one line"        "1" "$("$GC" --node 100 -d 1 "$T/g.txt" | wc -l | tr -d ' ')"
N2=$("$GC" --node 100 -d 2 "$T/g.txt")
ok "node depth 2 leads with the node" "100;node 100 " "$(echo "$N2" | head -1)"
L=$(grep '^100;' "$T/g.txt"); LISTS=$(echo "$L" | cut -d';' -f5,6 | tr ';,' '  ')
want=1; for id in $LISTS; do want=$((want+1)); done
eq "node depth 2 has the node plus every neighbour" "$want" "$(echo "$N2" | wc -l | tr -d ' ')"
eq "node: an absent id fails"        "1" "$("$GC" --node 999999 "$T/g.txt" >/dev/null 2>&1; echo $?)"
eq "node: the first id works"        "0;node 0 " "$("$GC" --node 0 -d 1 "$T/g.txt" | cut -c1-9)"
eq "node: the last id works"         "4999;" "$("$GC" --node 4999 -d 1 "$T/g.txt" | cut -c1-5)"

# --- a node budget: the nearest N, breadth first; a small graph comes out whole
eq "limit 1 is the node"             "1" "$("$GC" --node 100 -l 1 "$T/g.txt" | wc -l | tr -d ' ')"
eq "limit 20 gives 20 lines"         "20" "$("$GC" --node 100 -l 20 "$T/g.txt" | wc -l | tr -d ' ')"
ok "limit reports the cut"           "more nodes beyond the 20" "$("$GC" --node 100 -l 20 "$T/g.txt" 2>&1 >/dev/null)"
printf '1;;;;2,3;\n2;;;;4;\n3\n4\n' > "$T/small.txt"
eq "a small graph comes out whole"   "4" "$("$GC" --node 1 -l 100 "$T/small.txt" | wc -l | tr -d ' ')"
eq "... with no cut"                 "" "$("$GC" --node 1 -l 100 "$T/small.txt" 2>&1 >/dev/null)"
eq "limit and depth together"        "3" "$("$GC" --node 1 -l 100 -d 2 "$T/small.txt" | wc -l | tr -d ' ')"

# --- a walk that closed on itself is not cut, however exactly it fills the budget
printf '1;;;;2;\n2;;;;3;\n3;;;;1;\n' > "$T/cycle.txt"
eq "a cycle at its exact budget is whole" "3" "$("$GC" --node 1 -l 3 "$T/cycle.txt" | wc -l | tr -d ' ')"
eq "... and reports no cut"          "" "$("$GC" --node 1 -l 3 "$T/cycle.txt" 2>&1 >/dev/null)"
ok "one short of it does report one" "more nodes beyond the 2" "$("$GC" --node 1 -l 2 "$T/cycle.txt" 2>&1 >/dev/null)"

# --- parents past GC_FANOUT_MAX are cut, not silently dropped
awk 'BEGIN { print "1;hub;;;;"; for (i = 2; i <= 16500; i++) print i ";;;;1;" }' > "$T/hub.txt"
"$GC" --reverse "$T/hub.txt" | sort -t';' -k1,1n -k2,2n -u > "$T/hub.txt.parents"
eq "a hub keeps GC_FANOUT_MAX parents" "16384" "$("$GC" --node 1 -d 1 "$T/hub.txt" 2>/dev/null | cut -d';' -f6 | tr ',' '\n' | grep -c .)"
ok "... and says the rest were cut"  "more nodes beyond" "$("$GC" --node 1 -d 1 "$T/hub.txt" 2>&1 >/dev/null)"

# --- the parents side-file built by --reverse + sort, read back into the lines
awk -F';' '{print $1";"$2";"$3";"$4";"$5";"}' "$T/g.txt" > "$T/np.txt"   # strip parents
eq "stripped file has no parents"    "" "$("$GC" --node 100 -d 1 "$T/np.txt" | cut -d';' -f6)"
"$GC" --reverse "$T/np.txt" | sort -t';' -k1,1n -k2,2n -u > "$T/np.txt.parents"
ok "parents file is child;parent"    ";" "$(head -1 "$T/np.txt.parents")"
# a node that has parents (not every one does), compared as sorted sets
PID=$(awk -F';' '$6 != "" { print $1; exit }' "$T/g.txt")
WANTP=$(grep "^$PID;" "$T/g.txt" | cut -d';' -f6 | tr ',' '\n' | sort -n | tr '\n' ',')
GOTP=$("$GC" --node "$PID" -d 1 "$T/np.txt" | cut -d';' -f6 | tr -d '\n' | tr ',' '\n' | sort -n | tr '\n' ',')
eq "parents come back from the side file" "$WANTP" "$GOTP"
eq "a node without parents stays without" "" "$("$GC" --node 1 -d 1 "$T/np.txt" | cut -d';' -f6)"
ok "info reports the parents file"   "parents=1" "$("$GC" --info "$T/np.txt")"

# --- the bare adjacency form and an unsorted file
printf '1 2 3\n2 3\n3\n' > "$T/bare.txt"
eq "bare form: children parsed"      "1;;;;2,3;" "$("$GC" --node 1 -d 1 "$T/bare.txt")"
eq "bare form: depth 2"              "3" "$("$GC" --node 1 -d 2 "$T/bare.txt" | wc -l | tr -d ' ')"
printf '1;a\n3;b\n2;c\n' > "$T/bad.txt"
eq "check refuses an unsorted file"  "1" "$("$GC" --check "$T/bad.txt" >/dev/null 2>&1; echo $?)"
ok "check names the line"            "line 3" "$("$GC" --check "$T/bad.txt" 2>&1)"

# --- an edge list becomes a graph: group, reverse, and a target-only node crawls
printf '1 2\n1 3\n2 3\n7 1\n' | sort -k1,1n -k2,2n | "$GC" --group > "$T/eg.txt"
eq "group: node lines"               "1;;;;2,3;" "$(head -1 "$T/eg.txt")"
"$GC" --reverse "$T/eg.txt" | sort -t';' -k1,1n -k2,2n -u > "$T/eg.txt.parents"
eq "target-only node has its parents" "3;;;;;1,2" "$("$GC" --node 3 -d 1 "$T/eg.txt")"
eq "group refuses unsorted input"    "1" "$(printf '2 1\n1 2\n' | "$GC" --group >/dev/null 2>&1; echo $?)"

# --- label search
ok "find is a substring, any case"   ";node 16 quartz;" "$("$GC" --find QUARTZ "$T/g.txt" | head -1)"
eq "find caps at 50"                 "50" "$("$GC" --find node "$T/g.txt" | wc -l | tr -d ' ')"
eq "find of nothing is empty"        "" "$("$GC" --find zzzz "$T/g.txt")"

# --- a hub graph: the probe count per click stays near log2(n)
"$MK" -n 50000 -H 200 -l 20 -s 3 > "$T/h.txt"
ok "hub graph checks"                "sorted" "$("$GC" --check "$T/h.txt")"
P=$("$GC" -D --node 25000 -d 3 "$T/h.txt" 2>&1 >/dev/null | sed -n 's/.*nodes, \([0-9]*\) lines.*/\1/p')
N=$("$GC" --node 25000 -d 3 "$T/h.txt" | wc -l | tr -d ' ')
if [ "$P" -gt 0 ] && [ "$P" -le $((N * 40)) ]; then pass=$((pass+1)); echo "  ok   probes per node under 40 ($P for $N)";
else fail=$((fail+1)); echo "  FAIL probes $P for $N nodes"; fi

# --- HTTP
command -v curl >/dev/null 2>&1 || { echo "  SKIP http (no curl)"; echo "passed $pass, failed $fail"; [ $fail -eq 0 ] && exit 77 || exit 1; }
GRAPHCRAWL_NO_OPEN=1 "$GC" -p "$PORT" "$T/g.txt" >/dev/null 2>&1 &
SRV=$!
B="http://127.0.0.1:$PORT"
i=0; while [ $i -lt 50 ]; do curl -s -o /dev/null "$B/" && break; i=$((i+1)); sleep 0.1; done
ok "GET / is the page"               "<canvas id=\"graph\">" "$(curl -s "$B/")"
ok "GET /graphcrawl.js"              "class NavigatorApplet" "$(curl -s "$B/graphcrawl.js")"
ok "GET /graphcrawl.css"             "#graph" "$(curl -s "$B/graphcrawl.css")"
ok "GET /demo.html"                  "line_15" "$(curl -s "$B/demo.html")"
eq "GET /minus.gif is a GIF"         "GIF89a" "$(curl -s "$B/minus.gif" | head -c 6)"
ok "GET /api/info"                   "last=4999" "$(curl -s "$B/api/info")"
eq "GET /api/node matches the CLI"   "$N2" "$(curl -s "$B/api/node?keywordid=100&depth=2")"
ok "GET /api/node accepts id="       "100;node 100 " "$(curl -s "$B/api/node?id=100")"
eq "GET /api/node with a limit"      "12" "$(curl -s "$B/api/node?keywordid=100&limit=12" | grep -vc '^#')"
ok "... ends with the cut trailer"   "#cut 12" "$(curl -s "$B/api/node?keywordid=100&limit=12" | tail -1)"
ok "GET /api/node of an absent id is empty" "" "$(curl -s "$B/api/node?keywordid=999999")"
ok "GET /api/find"                   "quartz" "$(curl -s "$B/api/find?q=quartz" | head -1)"
eq "GET /api/find without q is 404"  "404" "$(curl -s -o /dev/null -w '%{http_code}' "$B/api/find")"
ok "GET / has the go-to box"         'id="goto"' "$(curl -s "$B/")"
eq "GET /api/node without an id is 404" "404" "$(curl -s -o /dev/null -w '%{http_code}' "$B/api/node")"
eq "GET of a missing asset is 404"   "404" "$(curl -s -o /dev/null -w '%{http_code}' "$B/nothing.js")"
eq "GET ../ is refused"              "404" "$(curl -s -o /dev/null -w '%{http_code}' --path-as-is "$B/../etc/passwd")"
eq "POST is refused"                 "404" "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$B/api/info")"
# HEAD answers with the headers a GET would send and no body (RFC 1945 8.2)
eq "HEAD / is 200"                   "200" "$(curl -s -o /dev/null -w '%{http_code}' -I "$B/")"
eq "HEAD / has no body"              "0" "$(curl -s -o - -I "$B/" | sed -n '/^.$/,$p' | tail -n +2 | wc -c | tr -d ' ')"
eq "HEAD /api/node has no body"      "0" "$(curl -s -I "$B/api/node?keywordid=100" | sed -n '/^.$/,$p' | tail -n +2 | wc -c | tr -d ' ')"
eq "HEAD of a missing asset is 404"  "404" "$(curl -s -o /dev/null -w '%{http_code}' -I "$B/nothing.js")"
# the content frame may not open a url the graph file made up
ok "the page sandboxes the content frame" 'sandbox="allow-scripts' "$(curl -s "$B/")"
ok "... and refuses a non-http url"  "safeUrl" "$(curl -s "$B/graphcrawl.js")"

echo "passed $pass, failed $fail"
[ $fail -eq 0 ]
