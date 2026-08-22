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

Blank lines and `#` comments are skipped. A line longer than 64 KB, a list
longer than 4096 ids, or an id out of order is a fault that `--check` names.

## The file is the index

A lookup is a binary search over the lines (`c/graph.c`, `lower_bound`):
probe the middle byte, step to the next line start, read the id, halve. For
a terabyte of 40-byte lines that is about 35 probes, each one seek and one
line. Nothing of the graph is ever in memory; the engine's footprint is its
fixed buffers (`c/common.h`), the same for ten lines and a trillion.

A click asks for the depth-N neighbourhood of one node: a breadth-first walk
over children and parents to N-1 hops, each visited node one lookup, every
node's line sent whole so the view can draw stubs for the neighbours it does
not show. Memory is the visited table, bounded at `GC_VISIT_MAX` (16384)
nodes; past it the walk stops and says so on stderr.

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

## Parents

Parents can live on the line (the 1999 server wrote them there) or in a
second file beside the graph, `FILE.parents`, one `child;parent` per line
sorted by child, built from the graph itself:

    graphcrawl --reverse FILE | sort -t';' -k1,1n -k2,2n -u > FILE.parents

`--reverse` streams the file and prints every child edge reversed; `sort`
does the external sort. A node's parents are the union of its line's field
and the side file's rows, found by the same binary search. Keeping parents
out of the main file lets the main file stay an adjacency list that is cheap
to write and cheap to regenerate the reverse of.

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
| `GC_LINE_MAX` | 65536 | one line |
| `GC_FANOUT_MAX` | 4096 | ids kept from one list |
| `GC_VISIT_MAX` | 16384 | nodes in one neighbourhood |
| `GC_DEPTH_MAX` | 8 | view depth the server expands |
| `GC_LABEL_MAX` | 1024 | label |
| `GC_URL_MAX` | 2048 | url |

A neighbourhood walk holds one parsed line (about 68 KB), the visited table
(about 270 KB) and one line buffer: under half a megabyte of stack.
