# The graph file

One node per line, the file sorted by id. The line is the 1999 applet's wire
line, so storage and wire are one grammar:

    id;label;category;url;children;parents

| field | meaning |
| --- | --- |
| `id` | signed 64-bit decimal; the sort key |
| `label` | shown on the node, cut to 15 characters until hovered; `\;` is a literal `;` |
| `category` | an integer: `category_icon_N` in the page picks the icon; 0 or empty draws a shape |
| `url` | opened in the content frame on double-click; empty shows the record |
| `children` | comma-separated ids |
| `parents` | comma-separated ids; may be empty if a `.parents` file supplies them |

Trailing fields may be absent. A bare adjacency line is the same node with no
label:

    id child child child

Blank lines and `#` comments are skipped. A line longer than 256 KB, a list
longer than 16384 ids, or an id out of order is a fault that `--check` names.

## The file is the index

A lookup is a binary search over the lines (`c/graph.c`, `lower_bound`):
probe the middle byte, step to the next line start, read the id, halve. For
a terabyte of 40-byte lines that is about 35 probes, each one seek and one
line; `--node -D` prints the count. Nothing of the graph is ever in memory;
the engine's footprint is its fixed buffers (`c/common.h`), the same for ten
lines and a trillion.

A click asks for the nodes nearest one node: a breadth-first walk over
children and parents visiting at most N nodes (`limit=N`), within D hops
when a cap is given (`depth=D`; the 1999 request was depth alone), each
visited node one lookup (two with a parents file), every node's line sent
whole so the view can draw stubs for the neighbours it does not show. The
budget counts what the walk visits, so an id that turns out to have no line
spends budget and sends none: the lines returned can be fewer than N. A
graph smaller than the budget comes out whole. Memory is the visited table,
bounded at `GC_VISIT_MAX` (16384) nodes, the budget's ceiling.

The response ends with a `#cut N` line, which the page reports, when the
walk left something out: a neighbour the budget had no room for, or a list
too long for its line. A walk that closed on itself is not cut, however
exactly it filled the budget, so a view whose nodes are all green never
carries the trailer.

Locality is what makes this fast rather than merely possible. Ids near each
other in value are near each other in the file, and a node's neighbours in
a graph with locality have nearby ids, so the page cache holds the pages a
click touches after the first probe or two.

## Sorting

The file must be sorted numerically by id; an unsorted file is refused by
`--check` and misread by lookups. Sort it once, externally:

    sort -t';' -k1,1n FILE > FILE.sorted

`sort` spills to disk and handles a file larger than memory. A file built in
id order (as `util/mkgraph` does, and as any id-assigning writer can) needs
no sort.

## From an edge list

Most graphs arrive as one edge per line, `src dst` (SNAP, Common Crawl, a
citation dump). Two external sorts make the file and its parents:

    sort -k1,1n -k2,2n -u EDGES | graphcrawl --group > FILE
    graphcrawl --reverse FILE | sort -t';' -k1,1n -k2,2n -u > FILE.parents

`--group` streams the sorted edges and writes one `src;;;;dst,dst;` line per
source, dropping repeated targets; the separator may be blank, tab, `,`,
`;` or `|`. A node that is only ever a target has no line of its own; it is
still a node, found through the parents file and returned with parents
only, so the crawl can enter it. Labels are not in an edge list; a label
file is a future addition (the paper's notes).

## Parents

Parents can live on the line (the 1999 server wrote them there) or in
`FILE.parents` beside the graph, one `child;parent` per line sorted by
child, built by the `--reverse` line above. A node's parents are the union
of its line's field and the side file's rows, found by the same binary
search. A hub with more rows than `GC_FANOUT_MAX` (16384) keeps the first
16384 and reports the cut, rather than coming back looking complete: on a
Debian package graph `libc6` has 24744 dependents and shows 16384 of them. Keeping parents out of the main file lets the main file stay an
adjacency list that is cheap to write and cheap to regenerate the reverse
of.

## Label search

`--find TEXT` and `/api/find?q=TEXT` stream the file and return the first
50 nodes whose label holds TEXT, case-insensitively. A full scan: on the
ten-million-node example it is a second or two; on a terabyte it is a
terabyte. A sorted label file with its own binary search is the scalable
form, not yet built.

## Sharding, discussed

`ais` shards its index into one file per key, under a prefix directory,
because its keys are words: a shard is navigable by name, rsync moves only
the keys that changed, and damage stays in one key. Here the key is an
integer with locality, and one sorted file already gives O(log n) lookup
with no directory walk, so sharding buys nothing for reading. It would buy
two things for writing: appending to a shard instead of re-sorting the
whole file, and syncing by shard. The shape, when needed, is `split -l` of
the sorted file into id ranges, each shard sorted and named by its first id,
with `gc_open` choosing the shard by range before the binary search. Not
implemented; the one-file form covers reading a graph of any size, which is
what the viewer needs.

## Bounds

| limit | value | holds |
| --- | --- | --- |
| `GC_LINE_MAX` | 262144 | one line |
| `GC_FANOUT_MAX` | 16384 | ids kept from one list |
| `GC_VISIT_MAX` | 16384 | nodes in one neighbourhood |
| `GC_DEPTH_MAX` | 8 | view depth the server expands |
| `GC_FIND_MAX` | 50 | matches a label search returns |
| `GC_LABEL_MAX` | 1024 | label |
| `GC_URL_MAX` | 2048 | url |

A neighbourhood walk holds its visited table and one parsed line (531 KB),
the lookup inside it a line buffer and the merged parents (384 KB), and the
parents lookup inside that another line buffer (256 KB): 1.14 MB of stack
for a click, 1.40 MB through the server, which adds its request buffer.
Measured with `cc -fstack-usage`.
