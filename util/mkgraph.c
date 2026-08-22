/* mkgraph.c -- a synthetic graph with locality, sorted by construction.
 *
 *   mkgraph [-n NODES] [-w WINDOW] [-s SEED] [-c CATEGORIES] > FILE
 *
 * Node i has 0..4 children drawn from i+1 .. i+WINDOW, so ids stay close to
 * their neighbours, the property graphcrawl's page cache relies on. Parents
 * are computed on the way with a ring of WINDOW lines, so every line carries
 * both lists and no FILE.parents is needed. Deterministic from SEED. Memory
 * is the ring, never the graph: a billion nodes stream out the same way.
 */
#define _POSIX_C_SOURCE 200809L
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#define WMAX   64
#define KIDS    4
#define PARMAX (WMAX * KIDS)

static unsigned long long rng;

static unsigned next(unsigned bound)
{
    rng = rng * 6364136223846793005ull + 1442695040888963407ull;
    return (unsigned)((rng >> 33) % bound);
}

int main(int argc, char **argv)
{
    long long nodes = 1000, i;
    int window = 8, cats = 5, c;
    /* ring[i % window]: the children chosen for node i; parents of node j are
     * every i in j-window..j-1 whose list holds j */
    long long ring[WMAX][KIDS];
    int nring[WMAX];

    rng = 1;
    while ((c = getopt(argc, argv, "n:w:s:c:")) != -1) {
        switch (c) {
        case 'n': nodes = atoll(optarg); break;
        case 'w': window = atoi(optarg); break;
        case 's': rng = (unsigned long long)atoll(optarg); break;
        case 'c': cats = atoi(optarg); break;
        default:
            fputs("usage: mkgraph [-n NODES] [-w WINDOW] [-s SEED] [-c CATEGORIES]\n", stderr);
            return 2;
        }
    }
    if (window < 1 || window > WMAX) {
        fprintf(stderr, "mkgraph: -w 1..%d\n", WMAX);
        return 2;
    }
    memset(nring, 0, sizeof nring);
    for (i = 0; i < nodes; i++) {
        int slot = (int)(i % window), n = (int)next(KIDS + 1), k, j;
        long long parents[PARMAX];
        int np = 0;

        /* parents: the window of earlier nodes that named i */
        for (j = 1; j <= window && j <= i; j++) {
            int s = (int)((i - j) % window);
            for (k = 0; k < nring[s]; k++)
                if (ring[s][k] == i && np < PARMAX)
                    parents[np++] = i - j;
        }
        /* children: distinct, ascending, within the window, inside the graph */
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
        printf("%lld;node %lld;%d;;", i, i, cats > 0 ? (int)(i % (cats + 1)) : 0);
        for (k = 0; k < nring[slot]; k++)
            printf(k ? ",%lld" : "%lld", ring[slot][k]);
        putchar(';');
        for (k = 0; k < np; k++)
            printf(k ? ",%lld" : "%lld", parents[k]);
        putchar('\n');
    }
    return 0;
}
