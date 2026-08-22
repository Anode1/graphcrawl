# AGENTS.md

graphcrawl is a C99 engine over a sorted plain-text graph file, with a
browser front-end that is a port of a 1999 Java applet. The code rules are
`ais`'s: read `~/ais/doc/dev/STYLE.md` before touching `c/`. Stack and
streaming, fixed buffers, no heap, one concept per file, return codes in the
modules, `die()` only in `main.c`, clean under `-std=c99 -W -Wall`.

The contract: `doc/FORMAT.md` (the file), `doc/PORT.md` (what the page must
keep doing), `c/graph.h` and `c/line.h` (the API the tests test).

    make            build ./graphcrawl (c/ + util/mkgraph)
    make codeut     c/tests.c, in process
    make cliut      tests/run.sh: the binary, then the HTTP API with curl
    make ut         both
    cd c && make ut-asan

`c/web.c` is generated from `web/` by `c/embed.sh` on every build; edit
`web/`, never `web.c`. `GRAPHCRAWL_WEB=web ./graphcrawl FILE` serves the
files from disk for editing without a rebuild.

Never open a window on the real display. Screenshots go through
`tests/shot.sh` (headless Chrome); `tests/drive.html`, served with
`GRAPHCRAWL_WEB=tests`, double-clicks and hovers by itself so a capture
shows the crawl. Start the server with `GRAPHCRAWL_NO_OPEN=1`.

Comments, the usage text and these documents are claims; when behaviour
changes they change with it.
