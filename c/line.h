/* line.h -- one graph line: parse and format.
 *
 * The grammar is the 1999 applet's wire line, kept as the storage line too
 * (doc/FORMAT.md):
 *
 *     id;label;category;url;children;parents
 *
 * id is a signed 64-bit decimal. children and parents are comma-separated
 * ids, either may be empty, trailing fields may be absent. A ';' inside the
 * label is written '\;'. A bare adjacency line is also read:
 *
 *     id child child child
 *
 * and is the same as "id;;;;child,child,child;".
 */
#ifndef GC_LINE_H
#define GC_LINE_H

#include <stddef.h>

#include "common.h"

struct gc_line {
    long long id;
    char label[GC_LABEL_MAX];
    char category[GC_CAT_MAX];
    char url[GC_URL_MAX];
    long long children[GC_FANOUT_MAX];
    long long parents[GC_FANOUT_MAX];
    int nchildren;
    int nparents;
    int cut;                 /* 1 if a list held more than GC_FANOUT_MAX ids */
};

/* Parse S into L. 0 on success, -1 if S is not a graph line (blank, a
 * '#' comment, a non-numeric id or list item). */
int gc_line_parse(const char *s, struct gc_line *l);

/* Read only the leading id. 0 on success, -1 if S carries none. */
int gc_line_id(const char *s, long long *id);

/* Parse "id;a,b,c" or "id a b c": the shape of a FILE.parents line. Returns
 * the list count (capped at MAX), -1 if S is not such a line. */
int gc_line_pair(const char *s, long long *id, long long *out, int max);

/* Format L in the canonical ';' form with a trailing newline. Returns the
 * length, or -1 if it does not fit SZ. */
int gc_line_format(const struct gc_line *l, char *buf, size_t sz);

#endif /* GC_LINE_H */
