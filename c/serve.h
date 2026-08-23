/* serve.h -- the localhost web front-end: a single-threaded HTTP/1.0 loop on
 * 127.0.0.1 serving the embedded page (web.c) and two read-only endpoints:
 *
 *   GET /api/node?keywordid=ID&limit=N   the N nodes nearest ID, breadth
 *                                        first, one line per node; &depth=D
 *                                        caps the hops (the 1999 child.cgi
 *                                        contract was depth alone; `id=` is
 *                                        accepted for keywordid); a walk
 *                                        that hit its budget ends with a
 *                                        "#cut N" line
 *   GET /api/find?q=TEXT                 nodes whose label holds TEXT, up to
 *                                        GC_FIND_MAX lines (a full scan)
 *   GET /api/info                        key=value lines: file, size, first,
 *                                        last, parents
 *
 * $GRAPHCRAWL_WEB names a directory served in place of the embedded files,
 * for editing the page without a rebuild. One client at a time, the request
 * must fit one read. Returns -1 on setup failure, else runs until killed.
 */
#ifndef GC_SERVE_H
#define GC_SERVE_H

#include "graph.h"

int gc_serve(struct gc_graph *g, int port, int open_browser);

#endif /* GC_SERVE_H */
