/* line.c -- see line.h. */
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "common.h"
#include "line.h"

static int at_end(const char *s)
{
    return *s == '\0' || *s == '\n' || *s == '\r';
}

static const char *skip_blank(const char *s)
{
    while (*s == ' ' || *s == '\t')
        s++;
    return s;
}

/* A signed decimal at S. Returns the char after it, or NULL. */
static const char *scan_id(const char *s, long long *out)
{
    char *end;
    long long v;

    errno = 0;
    v = strtoll(s, &end, 10);
    if (end == s || errno == ERANGE)
        return NULL;
    *out = v;
    return end;
}

/* A list of ids separated by ',' or blanks, up to ';' or the end. Items past
 * MAX are counted but dropped; *CUT says so. Returns the stored count, -1 on
 * a non-numeric item. */
static int scan_list(const char *s, const char **endp, long long *out,
                     int max, int *cut)
{
    int n = 0;

    for (;;) {
        long long v;
        const char *e;

        s = skip_blank(s);
        while (*s == ',')
            s = skip_blank(s + 1);
        if (at_end(s) || *s == ';')
            break;
        e = scan_id(s, &v);
        if (e == NULL)
            return -1;
        if (n < max)
            out[n++] = v;
        else
            *cut = 1;
        s = e;
    }
    *endp = s;
    return n;
}

/* A text field up to an unescaped ';'. With ESC, "\;" reads as ';' and a
 * lone '\' is dropped, as the 1999 parser did for the label. */
static const char *scan_field(const char *s, char *out, size_t sz, int esc)
{
    size_t n = 0;

    while (!at_end(s) && *s != ';') {
        char c = *s++;

        if (esc && c == '\\') {
            if (at_end(s))
                break;
            c = *s++;
        }
        if (n + 1 < sz)
            out[n++] = c;
    }
    out[n] = '\0';
    return s;
}

int gc_line_id(const char *s, long long *id)
{
    s = skip_blank(s);
    if (at_end(s) || *s == '#')
        return -1;
    return scan_id(s, id) == NULL ? -1 : 0;
}

int gc_line_parse(const char *s, struct gc_line *l)
{
    const char *p;
    int n;

    l->label[0] = l->category[0] = l->url[0] = '\0';
    l->nchildren = l->nparents = l->cut = 0;

    s = skip_blank(s);
    if (at_end(s) || *s == '#')
        return -1;
    p = scan_id(s, &l->id);
    if (p == NULL)
        return -1;
    p = skip_blank(p);
    if (at_end(p))
        return 0;                         /* "id": an isolated node */
    if (*p != ';') {                      /* "id c c c": bare adjacency */
        n = scan_list(p, &p, l->children, GC_FANOUT_MAX, &l->cut);
        if (n < 0 || !at_end(p))
            return -1;
        l->nchildren = n;
        return 0;
    }
    p = scan_field(p + 1, l->label, sizeof l->label, 1);
    if (*p != ';')
        return at_end(p) ? 0 : -1;
    p = scan_field(p + 1, l->category, sizeof l->category, 0);
    if (*p != ';')
        return at_end(p) ? 0 : -1;
    p = scan_field(p + 1, l->url, sizeof l->url, 0);
    if (*p != ';')
        return at_end(p) ? 0 : -1;
    n = scan_list(p + 1, &p, l->children, GC_FANOUT_MAX, &l->cut);
    if (n < 0)
        return -1;
    l->nchildren = n;
    if (*p != ';')
        return at_end(p) ? 0 : -1;
    n = scan_list(p + 1, &p, l->parents, GC_FANOUT_MAX, &l->cut);
    if (n < 0)
        return -1;
    l->nparents = n;
    return at_end(skip_blank(p)) ? 0 : -1;
}

int gc_line_pair(const char *s, long long *id, long long *out, int max)
{
    const char *p;
    int cut = 0, n;

    s = skip_blank(s);
    if (at_end(s) || *s == '#')
        return -1;
    p = scan_id(s, id);
    if (p == NULL)
        return -1;
    p = skip_blank(p);
    if (*p == ';')
        p++;
    n = scan_list(p, &p, out, max, &cut);
    if (n < 0 || !at_end(skip_blank(p)))
        return -1;
    return n;
}

/* Append S to BUF at *LEN, escaping ';' when ESC. 0 ok, -1 if it overflows. */
static int put_text(char *buf, size_t sz, size_t *len, const char *s, int esc)
{
    for (; *s != '\0'; s++) {
        if (esc && *s == ';') {
            if (*len + 2 >= sz)
                return -1;
            buf[(*len)++] = '\\';
        } else if (*len + 1 >= sz) {
            return -1;
        }
        buf[(*len)++] = *s;
    }
    return 0;
}

static int put_list(char *buf, size_t sz, size_t *len, const long long *v, int n)
{
    int i;

    for (i = 0; i < n; i++) {
        int k = snprintf(buf + *len, sz - *len, i ? ",%lld" : "%lld", v[i]);
        if (k < 0 || (size_t)k >= sz - *len)
            return -1;
        *len += (size_t)k;
    }
    return 0;
}

int gc_line_format(const struct gc_line *l, char *buf, size_t sz)
{
    size_t len = 0;
    int k = snprintf(buf, sz, "%lld;", l->id);

    if (k < 0 || (size_t)k >= sz)
        return -1;
    len = (size_t)k;
    if (put_text(buf, sz, &len, l->label, 1) != 0 ||
        put_text(buf, sz, &len, ";", 0) != 0 ||
        put_text(buf, sz, &len, l->category, 0) != 0 ||
        put_text(buf, sz, &len, ";", 0) != 0 ||
        put_text(buf, sz, &len, l->url, 0) != 0 ||
        put_text(buf, sz, &len, ";", 0) != 0 ||
        put_list(buf, sz, &len, l->children, l->nchildren) != 0 ||
        put_text(buf, sz, &len, ";", 0) != 0 ||
        put_list(buf, sz, &len, l->parents, l->nparents) != 0 ||
        put_text(buf, sz, &len, "\n", 0) != 0)
        return -1;
    buf[len] = '\0';
    return (int)len;
}
