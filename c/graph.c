/* graph.c -- see graph.h. */
#define _POSIX_C_SOURCE 200809L
#define _FILE_OFFSET_BITS 64
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>

#include "common.h"
#include "graph.h"
#include "line.h"
#include "log.h"

static long probes;

long gc_probes(void)
{
    return probes;
}

static off_t file_size(FILE *fp)
{
    struct stat st;

    if (fstat(fileno(fp), &st) != 0)
        return -1;
    return st.st_size;
}

int gc_open(struct gc_graph *g, const char *path)
{
    char ppath[GC_PATH_MAX];

    memset(g, 0, sizeof *g);
    if (snprintf(g->path, sizeof g->path, "%s", path) >= (int)sizeof g->path)
        return -1;
    g->fp = fopen(path, "rb");
    if (g->fp == NULL)
        return -1;
    g->size = file_size(g->fp);
    if (g->size < 0) {
        fclose(g->fp);
        g->fp = NULL;
        return -1;
    }
    if (snprintf(ppath, sizeof ppath, "%s.parents", path) < (int)sizeof ppath) {
        g->pfp = fopen(ppath, "rb");
        if (g->pfp != NULL) {
            g->psize = file_size(g->pfp);
            if (g->psize < 0) {
                fclose(g->pfp);
                g->pfp = NULL;
            }
        }
    }
    debug("open %s (%lld bytes)%s", path, (long long)g->size,
          g->pfp ? ", with .parents" : "");
    return 0;
}

void gc_close(struct gc_graph *g)
{
    if (g->fp != NULL)
        fclose(g->fp);
    if (g->pfp != NULL)
        fclose(g->pfp);
    g->fp = g->pfp = NULL;
}

/* The line starting at POS into BUF. Returns the bytes it occupies in the
 * file including its newline, 0 at end of file, -1 on error. A line longer
 * than BUF is consumed whole but arrives cut, so it fails to parse rather
 * than shifting the lines after it. */
static long read_at(FILE *fp, off_t pos, char *buf, size_t sz)
{
    size_t n;
    long used;

    if (fseeko(fp, pos, SEEK_SET) != 0)
        return -1;
    if (fgets(buf, (int)sz, fp) == NULL)
        return 0;
    n = strlen(buf);
    used = (long)n;
    if (n > 0 && buf[n - 1] != '\n') {
        int c;
        while ((c = getc(fp)) != EOF) {
            used++;
            if (c == '\n')
                break;
        }
    }
    return used;
}

/* The first line start at or after POS: POS itself when the byte before it
 * is a newline, else the byte after the next newline. Returns SIZE when
 * there is none, -1 on error. */
static off_t line_start(FILE *fp, off_t pos, off_t size)
{
    int c;

    if (pos <= 0)
        return 0;
    if (pos >= size)
        return size;
    if (fseeko(fp, pos - 1, SEEK_SET) != 0)
        return -1;
    while ((c = getc(fp)) != EOF) {
        pos++;
        if (c == '\n')
            return pos - 1;
    }
    return size;
}

/* From line start POS, the first node line at or before LIMIT into BUF:
 * blank and comment lines are stepped over. Sets *AT to its start and
 * returns its length, 0 when none is found before LIMIT, -1 on error. */
static long node_line_from(FILE *fp, off_t pos, off_t limit, char *buf,
                           size_t sz, off_t *at, long long *id)
{
    while (pos < limit) {
        long n = read_at(fp, pos, buf, sz);
        if (n < 0)
            return -1;
        if (n == 0)
            return 0;
        if (gc_line_id(buf, id) == 0) {
            *at = pos;
            return n;
        }
        pos += n;
    }
    return 0;
}

/* Binary search for the first node line whose id is >= ID. Sets *AT to its
 * start and returns its length with the line in BUF; 0 when every id is
 * smaller, -1 on error. LO is always a line start, so the line containing
 * the probe point is never lost to either half. */
static long lower_bound(FILE *fp, off_t size, long long id, char *buf,
                        size_t sz, off_t *at, long long *found)
{
    off_t lo = 0, hi = size;

    while (lo < hi) {
        off_t mid = lo + (hi - lo) / 2;
        off_t st = line_start(fp, mid, size);
        off_t here;
        long long lid;
        long n;

        if (st < 0)
            return -1;
        if (st >= hi) {
            hi = mid;
            continue;
        }
        n = node_line_from(fp, st, hi, buf, sz, &here, &lid);
        probes++;
        if (n < 0)
            return -1;
        if (n == 0) {
            hi = mid;
            continue;
        }
        if (lid < id)
            lo = here + n;
        else
            hi = here;
    }
    if (lo >= size)
        return 0;
    return node_line_from(fp, lo, size, buf, sz, at, found);
}

int gc_find(struct gc_graph *g, long long id, char *buf, size_t sz)
{
    off_t at;
    long long found;
    long n = lower_bound(g->fp, g->size, id, buf, sz, &at, &found);

    if (n < 0)
        return -1;
    return n > 0 && found == id;
}

int gc_parents(struct gc_graph *g, long long id, long long *out, int max,
               int *cut)
{
    char buf[GC_LINE_MAX];
    off_t at;
    long long found;
    long n;
    int count = 0;

    if (g->pfp == NULL)
        return 0;
    n = lower_bound(g->pfp, g->psize, id, buf, sizeof buf, &at, &found);
    while (n > 0 && found == id) {
        int k;
        if (count >= max) {
            *cut = 1;                  /* rows for ID left unread past MAX */
            break;
        }
        k = gc_line_pair(buf, &found, out + count, max - count);
        if (k > 0)
            count += k;
        at += n;
        n = node_line_from(g->pfp, at, g->psize, buf, sizeof buf, &at, &found);
    }
    return count;
}

static int has_id(const long long *v, int n, long long id)
{
    int i;

    for (i = 0; i < n; i++)
        if (v[i] == id)
            return 1;
    return 0;
}

int gc_node(struct gc_graph *g, long long id, struct gc_line *l)
{
    char buf[GC_LINE_MAX];
    long long extra[GC_FANOUT_MAX];
    int r = gc_find(g, id, buf, sizeof buf);
    int n, i;

    if (r < 0)
        return r;
    if (r == 1) {
        if (gc_line_parse(buf, l) != 0)
            return 0;
    } else {
        /* no line of its own: a node only ever named as an edge's target */
        l->id = id;
        l->label[0] = l->category[0] = l->url[0] = '\0';
        l->nchildren = l->nparents = l->cut = 0;
        if (g->pfp == NULL)
            return 0;
    }
    n = gc_parents(g, id, extra, GC_FANOUT_MAX, &l->cut);
    if (r == 0 && n == 0)
        return 0;
    for (i = 0; i < n; i++) {
        if (has_id(l->parents, l->nparents, extra[i]))
            continue;
        if (l->nparents >= GC_FANOUT_MAX) {
            l->cut = 1;
            break;
        }
        l->parents[l->nparents++] = extra[i];
    }
    return 1;
}

/* The visited set of one neighbourhood walk: open addressing over a table
 * twice GC_VISIT_MAX, so it never fills. */
#define SEEN_SLOTS (GC_VISIT_MAX * 2)

struct walk {
    long long seen[GC_VISIT_MAX];       /* also the BFS queue, in order   */
    unsigned char level[GC_VISIT_MAX];
    int slot[SEEN_SLOTS];                /* index into seen + 1, 0 = empty */
    int n;
};

static unsigned slot_of(long long id)
{
    unsigned long long h = (unsigned long long)id * 11400714819323198485ull;
    return (unsigned)(h >> 40) % SEEN_SLOTS;
}

/* Add ID at LEVEL unless seen. 1 added, 0 seen already, -1 ID is new and the
 * budget is spent: only then did the walk leave a node out. LIMIT is at most
 * GC_VISIT_MAX (its caller clamps it), so the table never fills. */
static int walk_add(struct walk *w, long long id, int level, int limit)
{
    unsigned s = slot_of(id);

    for (;;) {
        int k = w->slot[s];
        if (k == 0)
            break;
        if (w->seen[k - 1] == id)
            return 0;
        s = (s + 1) % SEEN_SLOTS;
    }
    if (w->n >= limit)
        return -1;
    w->seen[w->n] = id;
    w->level[w->n] = (unsigned char)level;
    w->slot[s] = ++w->n;
    return 1;
}

int gc_neighbourhood(struct gc_graph *g, long long id, int depth, int limit,
                     gc_emit_fn emit, void *ctx, int *cut)
{
    struct walk w;
    struct gc_line l;
    int head = 0, emitted = 0;

    *cut = 0;
    if (depth < 0)
        depth = 0;
    if (depth > GC_DEPTH_MAX)
        depth = GC_DEPTH_MAX;
    if (limit < 1 || limit > GC_VISIT_MAX)
        limit = GC_VISIT_MAX;
    memset(w.slot, 0, sizeof w.slot);
    w.n = 0;
    walk_add(&w, id, 1, limit);
    while (head < w.n) {
        long long cur = w.seen[head];
        int level = w.level[head], i;

        head++;
        if (gc_node(g, cur, &l) != 1) {
            if (head == 1)
                return -1;             /* the centre itself is absent */
            continue;                  /* a dangling id: the view draws a dummy */
        }
        if (emit(&l, ctx) != 0)
            break;
        emitted++;
        if (l.cut)
            *cut = 1;                  /* its own lists did not fit the line */
        if (depth > 0 && level >= depth)
            continue;
        /* Every neighbour is offered even once the budget is spent: only an id
         * the view does not already hold counts as a node left out, so a walk
         * that closed on itself is not reported as cut. */
        for (i = 0; i < l.nchildren; i++)
            if (walk_add(&w, l.children[i], level + 1, limit) < 0)
                *cut = 1;
        for (i = 0; i < l.nparents; i++)
            if (walk_add(&w, l.parents[i], level + 1, limit) < 0)
                *cut = 1;
    }
    return emitted;
}

int gc_bounds(struct gc_graph *g, long long *first, long long *last)
{
    char buf[GC_LINE_MAX];
    off_t at, pos;
    long n;
    int have = 0;

    n = node_line_from(g->fp, 0, g->size, buf, sizeof buf, &at, first);
    if (n <= 0)
        return -1;
    /* the last node line: read forward from the last GC_LINE_MAX bytes */
    pos = g->size > (off_t)sizeof buf ? g->size - (off_t)sizeof buf : 0;
    pos = line_start(g->fp, pos, g->size);
    while (pos < g->size) {
        long long id;
        n = read_at(g->fp, pos, buf, sizeof buf);
        if (n <= 0)
            break;
        if (gc_line_id(buf, &id) == 0) {
            *last = id;
            have = 1;
        }
        pos += n;
    }
    if (!have)
        *last = *first;
    return 0;
}

int gc_check(struct gc_graph *g, FILE *report, long long *lines)
{
    char buf[GC_LINE_MAX];
    struct gc_line l;
    long long prev = 0, lineno = 0;
    int have_prev = 0;

    rewind(g->fp);
    while (fgets(buf, sizeof buf, g->fp) != NULL) {
        size_t n = strlen(buf);
        long long id;

        lineno++;
        if (n > 0 && buf[n - 1] != '\n' && !feof(g->fp)) {
            fprintf(report, "line %lld: longer than %d bytes\n", lineno, GC_LINE_MAX);
            return -1;
        }
        if (gc_line_id(buf, &id) != 0) {
            const char *p = buf;
            while (*p == ' ' || *p == '\t')
                p++;
            if (*p == '\0' || *p == '\n' || *p == '\r' || *p == '#')
                continue;
            fprintf(report, "line %lld: no id\n", lineno);
            return -1;
        }
        if (gc_line_parse(buf, &l) != 0) {
            fprintf(report, "line %lld: malformed\n", lineno);
            return -1;
        }
        if (l.cut) {
            fprintf(report, "line %lld: a list exceeds %d ids\n", lineno, GC_FANOUT_MAX);
            return -1;
        }
        if (have_prev && id <= prev) {
            fprintf(report, "line %lld: id %lld after %lld, not ascending\n",
                    lineno, id, prev);
            return -1;
        }
        prev = id;
        have_prev = 1;
    }
    *lines = lineno;
    return ferror(g->fp) ? -1 : 0;
}

int gc_reverse(struct gc_graph *g, FILE *out)
{
    char buf[GC_LINE_MAX];
    struct gc_line l;

    rewind(g->fp);
    while (fgets(buf, sizeof buf, g->fp) != NULL) {
        int i;
        if (gc_line_parse(buf, &l) != 0)
            continue;
        for (i = 0; i < l.nchildren; i++)
            fprintf(out, "%lld;%lld\n", l.children[i], l.id);
    }
    return ferror(g->fp) || ferror(out) ? -1 : 0;
}

/* lowercase ASCII substring test */
static int has_text(const char *hay, const char *needle)
{
    size_t nl = strlen(needle);
    const char *p;

    if (nl == 0)
        return 1;
    for (p = hay; *p != '\0'; p++) {
        size_t i;
        for (i = 0; i < nl; i++) {
            unsigned char a = (unsigned char)p[i], b = (unsigned char)needle[i];
            if (a >= 'A' && a <= 'Z') a = (unsigned char)(a + 32);
            if (b >= 'A' && b <= 'Z') b = (unsigned char)(b + 32);
            if (a != b)
                break;
        }
        if (i == nl)
            return 1;
        if (p[i] == '\0')
            return 0;
    }
    return 0;
}

int gc_search(struct gc_graph *g, const char *text, int max,
              gc_emit_fn emit, void *ctx)
{
    char buf[GC_LINE_MAX];
    struct gc_line l;
    int count = 0;

    rewind(g->fp);
    while (count < max && fgets(buf, sizeof buf, g->fp) != NULL) {
        if (gc_line_parse(buf, &l) != 0)
            continue;
        if (!has_text(l.label, text))
            continue;
        if (emit(&l, ctx) != 0)
            break;
        count++;
    }
    return count;
}

int gc_group(FILE *in, FILE *out, FILE *report)
{
    char buf[GC_LINE_MAX];
    long long cur = 0, last_dst = 0, lineno = 0;
    int open = 0, first = 1;

    while (fgets(buf, sizeof buf, in) != NULL) {
        const char *p = buf;
        char *end;
        long long src, dst;

        lineno++;
        while (*p == ' ' || *p == '\t')
            p++;
        if (*p == '\0' || *p == '\n' || *p == '\r' || *p == '#')
            continue;
        src = strtoll(p, &end, 10);
        if (end == p) {
            fprintf(report, "line %lld: no source id\n", lineno);
            return -1;
        }
        p = end;
        while (*p == ' ' || *p == '\t' || *p == ',' || *p == ';' || *p == '|')
            p++;
        dst = strtoll(p, &end, 10);
        if (end == p) {
            fprintf(report, "line %lld: no target id\n", lineno);
            return -1;
        }
        if (!open || src != cur) {
            if (open && src < cur) {
                fputs(";\n", out);
                fprintf(report, "line %lld: source %lld after %lld, not sorted\n",
                        lineno, src, cur);
                return -1;
            }
            if (open)
                fputs(";\n", out);
            fprintf(out, "%lld;;;;", src);
            cur = src;
            open = 1;
            first = 1;
        } else if (dst == last_dst) {
            continue;
        }
        fprintf(out, first ? "%lld" : ",%lld", dst);
        first = 0;
        last_dst = dst;
    }
    if (open)
        fputs(";\n", out);
    return ferror(in) || ferror(out) ? -1 : 0;
}
