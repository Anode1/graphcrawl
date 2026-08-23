/* graph.h -- a graph kept as one text file sorted by id.
 *
 * The file is the index: every lookup is a binary search over the sorted
 * lines (about 40 seeks for a terabyte), so a depth-N neighbourhood costs
 * O(N * degree * log n) seeks whatever the graph's size, and nothing of the
 * graph is held in memory. Parents come from a line's own parents field, or
 * from FILE.parents beside it: "child;parent" lines sorted by child, built
 * by `graphcrawl --reverse FILE | sort -t';' -k1,1n -k2,2n -u` (doc/FORMAT.md).
 */
#ifndef GC_GRAPH_H
#define GC_GRAPH_H

#include <stdio.h>
#include <sys/types.h>

#include "common.h"
#include "line.h"

struct gc_graph {
    FILE *fp;               /* the graph file                    */
    off_t size;
    FILE *pfp;              /* FILE.parents, or NULL             */
    off_t psize;
    char path[GC_PATH_MAX];
};

/* Open PATH (and PATH.parents when present). 0 ok, -1 cannot open. */
int gc_open(struct gc_graph *g, const char *path);
void gc_close(struct gc_graph *g);

/* A node handed to a caller's sink; a non-zero return stops the walk. */
typedef int (*gc_emit_fn)(const struct gc_line *l, void *ctx);

/* The line of node ID into BUF. 1 found, 0 absent, -1 read error. */
int gc_find(struct gc_graph *g, long long id, char *buf, size_t sz);

/* Parents of ID from FILE.parents into OUT (at most MAX). The count, 0 when
 * there is no parents file. *CUT is set when the file holds rows past MAX, so
 * a hub whose parents do not fit says so instead of looking complete. */
int gc_parents(struct gc_graph *g, long long id, long long *out, int max,
               int *cut);

/* Node ID parsed, with FILE.parents merged in. An id that has no line but
 * has rows in FILE.parents is a node too (an edge-list target): it comes
 * back with parents only. 1 found, 0 absent, -1 error. */
int gc_node(struct gc_graph *g, long long id, struct gc_line *l);

/* Lines read by the binary searches so far, for measuring the cost of a
 * click: about log2(lines) per lookup. */
long gc_probes(void);

/* Stream the file and hand EMIT every node whose label holds TEXT, case
 * insensitively, up to MAX of them. Returns the count. A full scan: the
 * cost is the file's size. */
int gc_search(struct gc_graph *g, const char *text, int max,
              gc_emit_fn emit, void *ctx);

/* Group a sorted edge list into node lines: IN holds "src dst" lines (the
 * separator any of blank, ',', ';', '|'), sorted by src then dst; OUT gets
 * one "src;;;;dst,dst,...;" per source, repeats of a dst dropped. Streams
 * with no memory of the graph. 0 ok; -1 and a message on REPORT for a
 * non-numeric or out-of-order line. */
int gc_group(FILE *in, FILE *out, FILE *report);

/* The nodes nearest ID, breadth first over children and parents, ID first,
 * each handed to EMIT with its full lists: at most LIMIT of them (capped at
 * GC_VISIT_MAX), and within DEPTH-1 hops when DEPTH is not 0. The budget
 * counts nodes sent, so an id that turns out to have no line costs a lookup
 * and no budget. *CUT is set only when something was left out: a node still
 * on the frontier when the budget ran out, or a list too long for its line.
 * A small graph comes out whole and uncut however exactly it fills the
 * budget. Returns the count, -1 if ID is absent. */
int gc_neighbourhood(struct gc_graph *g, long long id, int depth, int limit,
                     gc_emit_fn emit, void *ctx, int *cut);

/* First and last ids in the file. 0 ok, -1 if the file has no node line. */
int gc_bounds(struct gc_graph *g, long long *first, long long *last);

/* Stream the whole file: every line a node line, blank or a comment, ids
 * strictly ascending, no list cut. Reports the first fault to REPORT and
 * returns -1; returns 0 and the line count otherwise. */
int gc_check(struct gc_graph *g, FILE *report, long long *lines);

/* Stream the whole file writing "child;parent" for every child edge, the
 * input of an external sort that builds FILE.parents. 0 ok, -1 error. */
int gc_reverse(struct gc_graph *g, FILE *out);

#endif /* GC_GRAPH_H */
