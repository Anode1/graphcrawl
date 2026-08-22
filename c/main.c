/* main.c -- the graphcrawl command line.
 *
 * Copyright (C) 1999, 2026 Vasili Gavrilov. GNU GPL v2 or later.
 *
 *   graphcrawl FILE                      serve FILE on 127.0.0.1 and open it
 *   graphcrawl FILE --node ID [-d N]     the depth-N neighbourhood of ID, as
 *                                        the server would send it
 *   graphcrawl FILE --info | --check | --reverse
 */
#ifndef UNIT_TEST
#define _POSIX_C_SOURCE 200809L
#include <getopt.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "common.h"
#include "graph.h"
#include "line.h"
#include "log.h"
#include "serve.h"

#ifndef GC_VERSION
#define GC_VERSION "0.0.0-dev"
#endif

static const char usage[] =
"usage: graphcrawl [options] FILE\n"
"\n"
"FILE is a graph, one node per line, sorted by id (doc/FORMAT.md):\n"
"    id;label;category;url;children;parents\n"
"    id child child child\n"
"\n"
"  (no command)      serve FILE at http://127.0.0.1:PORT/ and open it\n"
"  -n, --node ID     print the neighbourhood of ID and exit\n"
"  -d, --depth N     view depth for --node (2)\n"
"  -p, --port PORT   listen port (8090)\n"
"      --no-open     do not launch a browser (also: GRAPHCRAWL_NO_OPEN=1)\n"
"      --info        first id, last id, size\n"
"      --check       verify the file: parseable, ids ascending\n"
"      --reverse     print child;parent for every edge, to sort into\n"
"                    FILE.parents: graphcrawl --reverse FILE |\n"
"                    sort -t';' -k1,1n -k2,2n -u > FILE.parents\n"
"  -D                debug trace on stderr\n"
"  -h, --help        this text\n"
"      --version\n";

static int emit_stdout(const struct gc_line *l, void *ctx)
{
    char buf[GC_LINE_MAX];
    int n = gc_line_format(l, buf, sizeof buf);

    (void)ctx;
    if (n > 0)
        fputs(buf, stdout);
    return 0;
}

int main(int argc, char **argv)
{
    enum { CMD_SERVE, CMD_NODE, CMD_INFO, CMD_CHECK, CMD_REVERSE } cmd = CMD_SERVE;
    static const struct option longopts[] = {
        { "node",    required_argument, 0, 'n' },
        { "depth",   required_argument, 0, 'd' },
        { "port",    required_argument, 0, 'p' },
        { "no-open", no_argument,       0, 'N' },
        { "info",    no_argument,       0, 'I' },
        { "check",   no_argument,       0, 'C' },
        { "reverse", no_argument,       0, 'R' },
        { "help",    no_argument,       0, 'h' },
        { "version", no_argument,       0, 'V' },
        { 0, 0, 0, 0 }
    };
    struct gc_graph g;
    long long id = 0;
    int depth = 2, port = 8090, open_browser = 1, c;

    while ((c = getopt_long(argc, argv, "n:d:p:Dh", longopts, NULL)) != -1) {
        switch (c) {
        case 'n':
            if (gc_line_id(optarg, &id) != 0)
                die("--node: not an id: %s", optarg);
            cmd = CMD_NODE;
            break;
        case 'd': depth = atoi(optarg); break;
        case 'p': port = atoi(optarg); break;
        case 'N': open_browser = 0; break;
        case 'I': cmd = CMD_INFO; break;
        case 'C': cmd = CMD_CHECK; break;
        case 'R': cmd = CMD_REVERSE; break;
        case 'D': debug_enable(1); break;
        case 'V': puts("graphcrawl " GC_VERSION); return 0;
        case 'h': fputs(usage, stdout); return 0;
        default:  fputs(usage, stderr); return 2;
        }
    }
    if (optind != argc - 1) {
        fputs(usage, stderr);
        return 2;
    }
    if (depth < 1 || depth > GC_DEPTH_MAX)
        die("--depth: 1..%d", GC_DEPTH_MAX);
    if (gc_open(&g, argv[optind]) != 0)
        die("cannot open %s", argv[optind]);
    if (getenv("GRAPHCRAWL_NO_OPEN") != NULL)
        open_browser = 0;

    switch (cmd) {
    case CMD_NODE: {
        int cut, n = gc_neighbourhood(&g, id, depth, emit_stdout, NULL, &cut);
        if (n < 0)
            die("no node %lld in %s", id, g.path);
        if (cut)
            fprintf(stderr, "graphcrawl: neighbourhood cut at %d nodes\n", GC_VISIT_MAX);
        break;
    }
    case CMD_INFO: {
        long long first, last;
        if (gc_bounds(&g, &first, &last) != 0)
            die("%s holds no node line", g.path);
        printf("file=%s\nsize=%lld\nfirst=%lld\nlast=%lld\nparents=%d\n",
               g.path, (long long)g.size, first, last, g.pfp != NULL);
        break;
    }
    case CMD_CHECK: {
        long long lines;
        if (gc_check(&g, stderr, &lines) != 0) {
            gc_close(&g);
            return 1;
        }
        printf("%s: %lld lines, sorted\n", g.path, lines);
        break;
    }
    case CMD_REVERSE:
        if (gc_reverse(&g, stdout) != 0)
            die("read error on %s", g.path);
        break;
    case CMD_SERVE:
        if (gc_serve(&g, port, open_browser) != 0)
            die("cannot listen on 127.0.0.1:%d", port);
        break;
    }
    gc_close(&g);
    return 0;
}
#endif /* UNIT_TEST */
