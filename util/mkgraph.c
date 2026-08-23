/* mkgraph.c -- a synthetic graph with locality, sorted by construction.
 *
 *   mkgraph [-n NODES] [-w WINDOW] [-s SEED] [-c CATEGORIES]
 *           [-H HUBS] [-l LONG] > FILE
 *
 * Node i has 0..4 children drawn from i+1 .. i+WINDOW, so ids stay close to
 * their neighbours, the property graphcrawl's page cache relies on. Parents
 * of those edges are computed on the way with a ring of WINDOW lines, so
 * every line carries both lists. Two options break the regularity, as real
 * graphs do:
 *
 *   -H HUBS   one node in HUBS is a hub with 50..4000 children spread over
 *             the next 100*WINDOW ids (a power-law tail of degrees)
 *   -l LONG   one node in LONG gets one child anywhere in the graph
 *
 * Hub and long edges are listed as children only; their parents come from
 * `graphcrawl --reverse | sort`, the way an edge list's do. Labels carry a
 * word for `--find`. Deterministic from SEED. Memory is the ring, never the
 * graph: a billion nodes stream out the same way.
 */
#define _POSIX_C_SOURCE 200809L
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#define WMAX   64
#define KIDS    4
#define PARMAX (WMAX * KIDS)
#define HUBMAX 4000

static const char *words[] = {
    "alder", "basalt", "cobalt", "delta", "ember", "fjord", "granite", "harbour",
    "iris", "juniper", "kestrel", "lantern", "marble", "nettle", "orchid", "pewter",
    "quartz", "raven", "saffron", "thistle", "umber", "velvet", "willow", "xenon",
    "yarrow", "zephyr", "amber", "birch", "cedar", "dune", "elm", "fern"
};

static unsigned long long rng;

static unsigned next(unsigned bound)
{
    rng = rng * 6364136223846793005ull + 1442695040888963407ull;
    return (unsigned)((rng >> 33) % bound);
}

static int cmp_ll(const void *a, const void *b)
{
    long long x = *(const long long *)a, y = *(const long long *)b;
    return x < y ? -1 : x > y;
}

int main(int argc, char **argv)
{
    long long nodes = 1000, i, hubs = 0, longs = 0;
    int window = 8, cats = 5, c;
    /* ring[i % window]: the window children chosen for node i; parents of
     * node j are every i in j-window..j-1 whose list holds j */
    long long ring[WMAX][KIDS];
    int nring[WMAX];
    long long hub[HUBMAX];

    rng = 1;
    while ((c = getopt(argc, argv, "n:w:s:c:H:l:")) != -1) {
        switch (c) {
        case 'n': nodes = atoll(optarg); break;
        case 'w': window = atoi(optarg); break;
        case 's': rng = (unsigned long long)atoll(optarg); break;
        case 'c': cats = atoi(optarg); break;
        case 'H': hubs = atoll(optarg); break;
        case 'l': longs = atoll(optarg); break;
        default:
            fputs("usage: mkgraph [-n NODES] [-w WINDOW] [-s SEED] [-c CATEGORIES]"
                  " [-H HUBS] [-l LONG]\n", stderr);
            return 2;
        }
    }
    if (window < 1 || window > WMAX) {
        fprintf(stderr, "mkgraph: -w 1..%d\n", WMAX);
        return 2;
    }
    memset(nring, 0, sizeof nring);
    for (i = 0; i < nodes; i++) {
        int slot = (int)(i % window), n = (int)next(KIDS + 1), k, j, nhub = 0;
        long long parents[PARMAX], extra = -1;
        int np = 0, first = 1;

        /* parents: the window of earlier nodes that named i */
        for (j = 1; j <= window && j <= i; j++) {
            int s = (int)((i - j) % window);
            for (k = 0; k < nring[s]; k++)
                if (ring[s][k] == i && np < PARMAX)
                    parents[np++] = i - j;
        }
        /* window children: distinct, ascending, inside the graph */
        nring[slot] = 0;
        for (k = 0; k < n; k++) {
            long long child = i + 1 + next((unsigned)window);
            int dup = 0;
            if (child >= nodes)
                continue;
            for (j = 0; j < nring[slot]; j++)
                if (ring[slot][j] == child)
                    dup = 1;
            if (!dup)
                ring[slot][nring[slot]++] = child;
        }
        for (j = 1; j < nring[slot]; j++) {       /* insertion sort, <= 4 items */
            long long v = ring[slot][j];
            for (k = j - 1; k >= 0 && ring[slot][k] > v; k--)
                ring[slot][k + 1] = ring[slot][k];
            ring[slot][k + 1] = v;
        }
        /* a hub: degree from a power law between 50 and HUBMAX */
        if (hubs > 0 && next((unsigned)hubs) == 0) {
            unsigned span = (unsigned)window * 100u;
            double u = (next(1000000u) + 1) / 1000001.0;
            nhub = (int)(50.0 / (u * u * 0.9 + 0.1));     /* u^-2, capped */
            if (nhub > HUBMAX)
                nhub = HUBMAX;
            for (k = 0; k < nhub; k++)
                hub[k] = i + 1 + next(span);
            qsort(hub, (size_t)nhub, sizeof hub[0], cmp_ll);
        }
        /* a long edge: one child anywhere */
        if (longs > 0 && nodes > 1 && next((unsigned)longs) == 0) {
            extra = (long long)next(1000000u) * (nodes / 1000000 + 1) % nodes;
            if (extra == i)
                extra = (i + 1) % nodes;
        }
        printf("%lld;node %lld %s;%d;;", i, i, words[(i * 7919) % 32],
               cats > 0 ? (int)(i % (cats + 1)) : 0);
        for (k = 0; k < nring[slot]; k++, first = 0)
            printf(first ? "%lld" : ",%lld", ring[slot][k]);
        for (k = 0; k < nhub; k++) {
            if (hub[k] >= nodes || (k > 0 && hub[k] == hub[k - 1]))
                continue;
            printf(first ? "%lld" : ",%lld", hub[k]);
            first = 0;
        }
        if (extra >= 0) {
            printf(first ? "%lld" : ",%lld", extra);
            first = 0;
        }
        putchar(';');
        for (k = 0; k < np; k++)
            printf(k ? ",%lld" : "%lld", parents[k]);
        putchar('\n');
    }
    return 0;
}
