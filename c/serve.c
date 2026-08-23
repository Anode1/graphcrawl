/* serve.c -- see serve.h. The shape is ais's serve.c: accept, read, route,
 * write, close. No mutation endpoint exists, so there is nothing a cross-site
 * page could do here beyond reading a graph the user already serves. */
#define _POSIX_C_SOURCE 200809L
#include <netinet/in.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <unistd.h>

#include "common.h"
#include "graph.h"
#include "line.h"
#include "log.h"
#include "serve.h"
#include "web.h"

static void write_all(int fd, const char *p, size_t n)
{
    while (n > 0) {
        ssize_t w = write(fd, p, n);
        if (w <= 0)
            return;
        p += w;
        n -= (size_t)w;
    }
}

static void send_head(int fd, const char *ctype)
{
    char h[160];
    int n = snprintf(h, sizeof h,
                     "HTTP/1.0 200 OK\r\nContent-Type: %s\r\n"
                     "Cache-Control: no-store\r\nConnection: close\r\n\r\n", ctype);
    if (n > 0)
        write_all(fd, h, (size_t)n);
}

static void not_found(int fd)
{
    static const char nf[] =
        "HTTP/1.0 404 Not Found\r\nConnection: close\r\n\r\nnot found\n";
    write_all(fd, nf, sizeof nf - 1);
}

static int hexval(int c)
{
    if (c >= '0' && c <= '9') return c - '0';
    if (c >= 'a' && c <= 'f') return c - 'a' + 10;
    if (c >= 'A' && c <= 'F') return c - 'A' + 10;
    return -1;
}

/* in-place URL-decode (%xx and '+') */
static void url_decode(char *s)
{
    char *o = s;

    while (*s != '\0') {
        int hi, lo;
        if (*s == '+') {
            *o++ = ' ';
            s++;
        } else if (*s == '%' && (hi = hexval((unsigned char)s[1])) >= 0
                             && (lo = hexval((unsigned char)s[2])) >= 0) {
            *o++ = (char)(hi * 16 + lo);
            s += 3;
        } else {
            *o++ = *s++;
        }
    }
    *o = '\0';
}

/* The value of KEY in the query string Q ("a=1&b=2"), decoded into OUT.
 * 1 if present. */
static int query_get(const char *q, const char *key, char *out, size_t sz)
{
    size_t klen = strlen(key);

    while (q != NULL && *q != '\0') {
        const char *amp = strchr(q, '&');
        size_t plen = amp ? (size_t)(amp - q) : strlen(q);

        if (plen > klen && strncmp(q, key, klen) == 0 && q[klen] == '=') {
            size_t vlen = plen - klen - 1;
            if (vlen >= sz)
                vlen = sz - 1;
            memcpy(out, q + klen + 1, vlen);
            out[vlen] = '\0';
            url_decode(out);
            return 1;
        }
        q = amp ? amp + 1 : NULL;
    }
    return 0;
}

static const char *ctype_of(const char *name)
{
    const char *dot = strrchr(name, '.');

    if (dot != NULL) {
        if (strcmp(dot, ".html") == 0) return "text/html";
        if (strcmp(dot, ".css")  == 0) return "text/css";
        if (strcmp(dot, ".js")   == 0) return "text/javascript";
        if (strcmp(dot, ".gif")  == 0) return "image/gif";
        if (strcmp(dot, ".png")  == 0) return "image/png";
        if (strcmp(dot, ".svg")  == 0) return "image/svg+xml";
    }
    return "application/octet-stream";
}

/* NAME is one safe file name: letters, digits, '.', '_', '-'. */
static int safe_name(const char *name)
{
    const char *p;

    if (name[0] == '\0' || name[0] == '.')
        return 0;
    for (p = name; *p != '\0'; p++) {
        unsigned char c = (unsigned char)*p;
        int ok = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') ||
                 (c >= 'A' && c <= 'Z') || c == '.' || c == '_' || c == '-';
        if (!ok)
            return 0;
    }
    return 1;
}

/* <$GRAPHCRAWL_WEB>/NAME when that directory is set and holds it, else the
 * embedded copy. 1 if served. */
static int serve_asset(int fd, const char *name)
{
    const char *webdir = getenv("GRAPHCRAWL_WEB");
    size_t i;

    if (!safe_name(name))
        return 0;
    if (webdir != NULL && webdir[0] != '\0') {
        char path[GC_PATH_MAX], buf[8192];
        FILE *fp;
        size_t n;

        if (snprintf(path, sizeof path, "%s/%s", webdir, name) < (int)sizeof path
            && (fp = fopen(path, "rb")) != NULL) {
            send_head(fd, ctype_of(name));
            while ((n = fread(buf, 1, sizeof buf, fp)) > 0)
                write_all(fd, buf, n);
            fclose(fp);
            return 1;
        }
    }
    for (i = 0; i < gc_assets_n; i++) {
        if (strcmp(gc_assets[i].name, name) == 0) {
            send_head(fd, gc_assets[i].ctype);
            write_all(fd, (const char *)gc_assets[i].data, gc_assets[i].len);
            return 1;
        }
    }
    return 0;
}

static int emit_line(const struct gc_line *l, void *ctx)
{
    char buf[GC_LINE_MAX];
    int n = gc_line_format(l, buf, sizeof buf);

    if (n > 0)
        write_all(*(int *)ctx, buf, (size_t)n);
    return 0;
}

static void api_node(struct gc_graph *g, int fd, const char *query)
{
    char v[64];
    long long id;
    int depth = 2, cut, n;

    if (!query_get(query, "keywordid", v, sizeof v) &&
        !query_get(query, "id", v, sizeof v)) {
        not_found(fd);
        return;
    }
    if (gc_line_id(v, &id) != 0) {
        not_found(fd);
        return;
    }
    if (query_get(query, "depth", v, sizeof v))
        depth = atoi(v);
    send_head(fd, "text/plain; charset=utf-8");
    n = gc_neighbourhood(g, id, depth, emit_line, &fd, &cut);
    if (cut) {
        char t[64];
        int k = snprintf(t, sizeof t, "#cut %d\n", GC_VISIT_MAX);
        write_all(fd, t, (size_t)k);        /* a trailer the page reports */
    }
    debug("node %lld depth %d: %d lines%s", id, depth, n, cut ? " (cut)" : "");
}

static void api_find(struct gc_graph *g, int fd, const char *query)
{
    char q[256];
    int n;

    if (!query_get(query, "q", q, sizeof q) || q[0] == '\0') {
        not_found(fd);
        return;
    }
    send_head(fd, "text/plain; charset=utf-8");
    n = gc_search(g, q, GC_FIND_MAX, emit_line, &fd);
    debug("find '%s': %d lines", q, n);
}

static void api_info(struct gc_graph *g, int fd)
{
    char buf[GC_PATH_MAX + 256];
    long long first = 0, last = 0;
    int n;

    gc_bounds(g, &first, &last);
    n = snprintf(buf, sizeof buf,
                 "file=%s\nsize=%lld\nfirst=%lld\nlast=%lld\nparents=%d\n",
                 g->path, (long long)g->size, first, last, g->pfp != NULL);
    send_head(fd, "text/plain; charset=utf-8");
    if (n > 0 && (size_t)n < sizeof buf)
        write_all(fd, buf, (size_t)n);
}

static void handle(struct gc_graph *g, int fd)
{
    char buf[GC_LINE_MAX];
    char *method, *path, *query, *sp;
    ssize_t n = read(fd, buf, sizeof buf - 1);

    if (n <= 0)
        return;
    buf[n] = '\0';
    method = buf;
    sp = strchr(buf, ' ');
    if (sp == NULL)
        return;
    *sp = '\0';
    path = sp + 1;
    sp = strpbrk(path, " \r\n");
    if (sp != NULL)
        *sp = '\0';
    query = strchr(path, '?');
    if (query != NULL)
        *query++ = '\0';
    if (strcmp(method, "GET") != 0 && strcmp(method, "HEAD") != 0) {
        not_found(fd);
        return;
    }
    if (strcmp(path, "/api/node") == 0)
        api_node(g, fd, query);
    else if (strcmp(path, "/api/find") == 0)
        api_find(g, fd, query);
    else if (strcmp(path, "/api/info") == 0)
        api_info(g, fd);
    else if (strcmp(path, "/") == 0)
        serve_asset(fd, "index.html");
    else if (!serve_asset(fd, path + 1))
        not_found(fd);
}

/* Launch the user's browser on the page; the desktop's opener on Linux, the
 * system one on macOS. Ignored when neither exists. */
static void launch_browser(int port)
{
    char cmd[224];
    int rc;

    snprintf(cmd, sizeof cmd,
             "{ xdg-open 'http://127.0.0.1:%d/' || open 'http://127.0.0.1:%d/'; }"
             " >/dev/null 2>&1 &", port, port);
    rc = system(cmd);
    (void)rc;
}

int gc_serve(struct gc_graph *g, int port, int open_browser)
{
    int sfd, cfd, yes = 1;
    struct sockaddr_in addr;

    signal(SIGPIPE, SIG_IGN);           /* a client hangup must not kill us */
    sfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sfd < 0)
        return -1;
    setsockopt(sfd, SOL_SOCKET, SO_REUSEADDR, &yes, sizeof yes);
    memset(&addr, 0, sizeof addr);
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = htonl(INADDR_LOOPBACK);   /* 127.0.0.1 only */
    addr.sin_port = htons((unsigned short)port);
    if (bind(sfd, (struct sockaddr *)&addr, sizeof addr) != 0 ||
        listen(sfd, 16) != 0) {
        close(sfd);
        return -1;
    }
    fprintf(stderr, "graphcrawl: http://127.0.0.1:%d/  (Ctrl-C to stop)\n", port);
    if (open_browser)
        launch_browser(port);
    for (;;) {
        struct timeval tv;

        cfd = accept(sfd, NULL, NULL);
        if (cfd < 0)
            continue;
        tv.tv_sec = 5;                 /* a silent client must not park the loop */
        tv.tv_usec = 0;
        setsockopt(cfd, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof tv);
        handle(g, cfd);
        close(cfd);
    }
}
