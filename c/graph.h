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

/* The line of node ID into BUF. 1 found, 0 absent, -1 read error. */
int gc_find(struct gc_graph *g, long long id, char *buf, size_t sz);

/* Parents of ID from FILE.parents into OUT (at most MAX). The count, 0 when
 * there is no parents file. */
int gc_parents(struct gc_graph *g, long long id, long long *out, int max);

/* Node ID parsed, with FILE.parents merged in. 1 found, 0 absent, -1 error. */
int gc_node(struct gc_graph *g, long long id, struct gc_line *l);

/* Every node within DEPTH-1 hops of ID over children and parents, breadth
 * first, ID first, each handed to EMIT. Returns the count, -1 if ID is
 * absent. A frontier past GC_VISIT_MAX nodes is cut, and *CUT says so. */
typedef int (*gc_emit_fn)(const struct gc_line *l, void *ctx);
int gc_neighbourhood(struct gc_graph *g, long long id, int depth,
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
