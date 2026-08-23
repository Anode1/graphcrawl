/* tests.c -- the engine tests, built with -DUNIT_TEST (make ut). Linear,
 * inline, one comment per test. A fixture graph is written to a temp dir. */
#ifdef UNIT_TEST
#define _POSIX_C_SOURCE 200809L
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "common.h"
#include "graph.h"
#include "line.h"

static int fails;

#define CHECK(cond) do { if (!(cond)) { fails++; \
    printf("  FAIL %s:%d %s\n", __FILE__, __LINE__, #cond); } } while (0)

static void write_file(const char *path, const char *text)
{
    FILE *f = fopen(path, "w");
    if (f == NULL) { perror(path); exit(1); }
    fputs(text, f);
    fclose(f);
}

struct collect { long long ids[64]; int n; };

static int collect_cb(const struct gc_line *l, void *ctx)
{
    struct collect *c = ctx;
    if (c->n < 64)
        c->ids[c->n++] = l->id;
    return 0;
}

int main(void)
{
    char dir[] = "/tmp/graphcrawl-ut-XXXXXX";
    char gpath[256], ppath[256], buf[GC_LINE_MAX];
    struct gc_line l;
    struct gc_graph g;
    long long ids[8], id;
    int n;

    if (mkdtemp(dir) == NULL) { perror("mkdtemp"); return 1; }
    snprintf(gpath, sizeof gpath, "%s/g.txt", dir);
    snprintf(ppath, sizeof ppath, "%s/g.txt.parents", dir);

    /* line: the full 1999 form parses every field */
    CHECK(gc_line_parse("5;Five;2;http://x/5;6,7;4", &l) == 0);
    CHECK(l.id == 5 && strcmp(l.label, "Five") == 0 && strcmp(l.category, "2") == 0);
    CHECK(strcmp(l.url, "http://x/5") == 0);
    CHECK(l.nchildren == 2 && l.children[0] == 6 && l.children[1] == 7);
    CHECK(l.nparents == 1 && l.parents[0] == 4);

    /* line: '\;' in a label is a literal ';', a lone '\' is dropped */
    CHECK(gc_line_parse("1;a\\;b\\c;;;;", &l) == 0 && strcmp(l.label, "a;bc") == 0);

    /* line: the bare adjacency form "id c c c" */
    CHECK(gc_line_parse("10 11 12  13", &l) == 0 && l.id == 10 && l.nchildren == 3 && l.children[2] == 13);

    /* line: an isolated node, trailing fields absent, blanks in lists */
    CHECK(gc_line_parse("42", &l) == 0 && l.id == 42 && l.nchildren == 0);
    CHECK(gc_line_parse("42;x", &l) == 0 && strcmp(l.label, "x") == 0);
    CHECK(gc_line_parse("42;x;;; 1 , 2 ,,3;", &l) == 0 && l.nchildren == 3);

    /* line: negative ids (the 1999 oval range) */
    CHECK(gc_line_parse("-300;GOAL;1;;59,60;0", &l) == 0 && l.id == -300 && l.parents[0] == 0);

    /* line: not a line: blank, comment, non-numeric id or item */
    CHECK(gc_line_parse("", &l) != 0 && gc_line_parse("# c", &l) != 0);
    CHECK(gc_line_parse("abc;x", &l) != 0 && gc_line_parse("1;x;;;a,b;", &l) != 0);

    /* line: format round-trips, escaping ';' in the label */
    CHECK(gc_line_parse("7;a\\;b;3;u;8,9;6", &l) == 0);
    CHECK(gc_line_format(&l, buf, sizeof buf) > 0 && strcmp(buf, "7;a\\;b;3;u;8,9;6\n") == 0);

    /* line: the parents-file pair in both separators */
    CHECK(gc_line_pair("3;1,2", &id, ids, 8) == 2 && id == 3 && ids[1] == 2);
    CHECK(gc_line_pair("3 9", &id, ids, 8) == 1 && ids[0] == 9);

    /* graph: a sorted fixture with a comment, a blank and a dangling child id */
    write_file(gpath,
        "# family\n"
        "0;Anode;0;;;1,2\n"
        "1;father;0;;;3,4\n"
        "\n"
        "2;mother;0;;;5,6\n"
        "3;grandmother(father);0;;;7,8\n"
        "4;grandfather(father);0;;;9,10\n"
        "5;grandmother(mother);0;;;11,12\n"
        "6;grandfather(mother);4;;;13,14\n"
        "7;ggf1;1;;;15,16\n"
        "8;ggm2;2;;;\n"
        "9;ggf3;3;;;\n"
        "10;ggm4;3;;;\n"
        "11;ggf5;4;;;\n"
        "12;ggm5;4;;;\n"
        "13;ggf6;4;;;\n"
        "14;ggm6;4;;;\n"
        "15;ggf7;4;;;\n");
    CHECK(gc_open(&g, gpath) == 0 && g.pfp == NULL);

    /* find: first, last, middle, absent before, between and after */
    CHECK(gc_find(&g, 0, buf, sizeof buf) == 1 && strncmp(buf, "0;Anode", 7) == 0);
    CHECK(gc_find(&g, 15, buf, sizeof buf) == 1 && strncmp(buf, "15;ggf7", 7) == 0);
    CHECK(gc_find(&g, 6, buf, sizeof buf) == 1 && strncmp(buf, "6;grand", 7) == 0);
    CHECK(gc_find(&g, -1, buf, sizeof buf) == 0);
    CHECK(gc_find(&g, 16, buf, sizeof buf) == 0);
    CHECK(gc_find(&g, 99, buf, sizeof buf) == 0);

    /* find: every id, against the id the line carries */
    for (id = 0; id <= 15; id++) {
        long long got;
        CHECK(gc_find(&g, id, buf, sizeof buf) == 1);
        CHECK(gc_line_id(buf, &got) == 0 && got == id);
    }

    /* bounds */
    {
        long long first, last;
        CHECK(gc_bounds(&g, &first, &last) == 0 && first == 0 && last == 15);
    }

    /* neighbourhood: depth 1 is the node alone; depth 2 adds its parents */
    {
        struct collect c = { {0}, 0 };
        int cut;
        CHECK(gc_neighbourhood(&g, 0, 1, 0, collect_cb, &c, &cut) == 1 && c.ids[0] == 0);
        c.n = 0;
        CHECK(gc_neighbourhood(&g, 0, 2, 0, collect_cb, &c, &cut) == 3);
        CHECK(c.ids[0] == 0 && c.ids[1] == 1 && c.ids[2] == 2 && cut == 0);
        /* depth 3 reaches the grandparents; id 16 is dangling and skipped */
        c.n = 0;
        CHECK(gc_neighbourhood(&g, 0, 3, 0, collect_cb, &c, &cut) == 7);
        c.n = 0;
        /* 7 lists 15 and 16; 16 has no line and is skipped */
        CHECK(gc_neighbourhood(&g, 7, 2, 0, collect_cb, &c, &cut) == 2);
        /* an absent centre is -1 */
        CHECK(gc_neighbourhood(&g, 77, 2, 0, collect_cb, &c, &cut) == -1);
    }

    /* check: the fixture passes */
    {
        long long lines;
        CHECK(gc_check(&g, stderr, &lines) == 0 && lines == 18);
    }
    gc_close(&g);

    /* parents file: merged into a node, deduplicated, found for several rows */
    write_file(ppath, "1;0\n2;0\n3 1\n3;1\n3;2\n4;1\n");
    CHECK(gc_open(&g, gpath) == 0 && g.pfp != NULL);
    CHECK(gc_parents(&g, 3, ids, 8) == 3);
    CHECK(gc_parents(&g, 0, ids, 8) == 0);
    CHECK(gc_node(&g, 3, &l) == 1 && l.nparents == 4 && l.nchildren == 0);   /* 7,8 + 1,2; the repeat dropped */
    CHECK(gc_node(&g, 1, &l) == 1 && l.nparents == 3 && l.parents[2] == 0 && l.nchildren == 0);
    /* neighbourhood now reaches 1 and 2 through the parents file as well as 7 and 8 */
    {
        struct collect c = { {0}, 0 };
        int cut;
        CHECK(gc_neighbourhood(&g, 3, 2, 0, collect_cb, &c, &cut) == 5);
    }
    gc_close(&g);

    /* check: an unsorted file is refused */
    write_file(gpath, "1;a\n3;b\n2;c\n");
    CHECK(gc_open(&g, gpath) == 0);
    {
        long long lines;
        FILE *null = fopen("/dev/null", "w");
        CHECK(gc_check(&g, null, &lines) == -1);
        fclose(null);
    }
    gc_close(&g);

    /* reverse: one child;parent line per child edge */
    write_file(gpath, "1;;;;2,3;\n2;;;;3;\n3\n");
    CHECK(gc_open(&g, gpath) == 0);
    {
        char rpath[256];
        FILE *out;
        snprintf(rpath, sizeof rpath, "%s/r.txt", dir);
        out = fopen(rpath, "w");
        CHECK(gc_reverse(&g, out) == 0);
        fclose(out);
        out = fopen(rpath, "r");
        n = 0;
        while (fgets(buf, sizeof buf, out) != NULL) {
            if (n == 0) CHECK(strcmp(buf, "2;1\n") == 0);
            if (n == 2) CHECK(strcmp(buf, "3;2\n") == 0);
            n++;
        }
        fclose(out);
        CHECK(n == 3);
        unlink(rpath);
    }
    gc_close(&g);

    /* find: a bigger file with gaps between ids */
    {
        FILE *f = fopen(gpath, "w");
        long long i;
        for (i = 0; i < 20000; i++)
            fprintf(f, "%lld;n%lld;;;%lld,%lld;\n", i * 3, i * 3, i * 3 + 3, i * 3 + 6);
        fclose(f);
        CHECK(gc_open(&g, gpath) == 0);
        for (i = 0; i < 20000; i += 97)
            CHECK(gc_find(&g, i * 3, buf, sizeof buf) == 1);
        CHECK(gc_find(&g, 1, buf, sizeof buf) == 0);
        CHECK(gc_find(&g, 59998, buf, sizeof buf) == 0);
        {
            struct collect c = { {0}, 0 };
            int cut;
            CHECK(gc_neighbourhood(&g, 30000, 3, 0, collect_cb, &c, &cut) == 5);
        }
        gc_close(&g);
    }

    /* a node that exists only as an edge target comes back with its parents */
    write_file(gpath, "1;;;;2,3;\n2;;;;3;\n");
    write_file(ppath, "2;1\n3;1\n3;2\n");
    CHECK(gc_open(&g, gpath) == 0);
    CHECK(gc_node(&g, 3, &l) == 1 && l.nparents == 2 && l.nchildren == 0 && l.label[0] == '\0');
    CHECK(gc_node(&g, 4, &l) == 0);
    {
        struct collect c = { {0}, 0 };
        int cut;
        CHECK(gc_neighbourhood(&g, 3, 2, 0, collect_cb, &c, &cut) == 3);
    }
    gc_close(&g);

    /* search: case-insensitive substring on the label, capped */
    write_file(gpath, "1;Alder tree;;;;\n2;basalt;;;;\n3;alder again;;;;\n4;Alder;;;;\n");
    unlink(ppath);
    CHECK(gc_open(&g, gpath) == 0);
    {
        struct collect c = { {0}, 0 };
        CHECK(gc_search(&g, "ALDER", 50, collect_cb, &c) == 3 && c.ids[2] == 4);
        c.n = 0;
        CHECK(gc_search(&g, "alder", 2, collect_cb, &c) == 2);
        c.n = 0;
        CHECK(gc_search(&g, "zzz", 50, collect_cb, &c) == 0);
    }
    gc_close(&g);

    /* group: a sorted edge list becomes node lines, repeats dropped */
    {
        char epath[256], opath[256];
        FILE *in, *out;
        snprintf(epath, sizeof epath, "%s/e.txt", dir);
        snprintf(opath, sizeof opath, "%s/o.txt", dir);
        write_file(epath, "# edges\n1 2\n1\t3\n1,3\n2;3\n5|1\n");
        in = fopen(epath, "r"); out = fopen(opath, "w");
        CHECK(gc_group(in, out, stderr) == 0);
        fclose(in); fclose(out);
        out = fopen(opath, "r");
        CHECK(fgets(buf, sizeof buf, out) != NULL && strcmp(buf, "1;;;;2,3;\n") == 0);
        CHECK(fgets(buf, sizeof buf, out) != NULL && strcmp(buf, "2;;;;3;\n") == 0);
        CHECK(fgets(buf, sizeof buf, out) != NULL && strcmp(buf, "5;;;;1;\n") == 0);
        CHECK(fgets(buf, sizeof buf, out) == NULL);
        fclose(out);
        /* an unsorted edge list is refused */
        write_file(epath, "2 1\n1 2\n");
        in = fopen(epath, "r"); out = fopen(opath, "w");
        {
            FILE *null = fopen("/dev/null", "w");
            CHECK(gc_group(in, out, null) == -1);
            fclose(null);
        }
        fclose(in); fclose(out);
        unlink(epath); unlink(opath);
    }

    /* probes: a lookup on 20000 lines reads about log2 of them */
    {
        FILE *f = fopen(gpath, "w");
        long long i, before;
        for (i = 0; i < 20000; i++)
            fprintf(f, "%lld;n;;;;\n", i);
        fclose(f);
        CHECK(gc_open(&g, gpath) == 0);
        before = gc_probes();
        CHECK(gc_find(&g, 12345, buf, sizeof buf) == 1);
        CHECK(gc_probes() - before >= 10 && gc_probes() - before <= 20);
        gc_close(&g);
    }

    unlink(gpath);
    unlink(ppath);
    rmdir(dir);
    if (fails) {
        printf("graphcrawl tests: %d FAILED\n", fails);
        return 1;
    }
    puts("graphcrawl tests: all passed");
    return 0;
}
#endif /* UNIT_TEST */
