/* common.h -- shared limits for graphcrawl.
 *
 * Copyright (C) 1999, 2026 Vasili Gavrilov. GNU GPL v2 or later.
 */
#ifndef GC_COMMON_H
#define GC_COMMON_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* Fixed buffers. Peak footprint is a function of these, never of the graph:
 * a trillion-line file and a ten-line file run in the same memory. */
#define GC_LINE_MAX    65536   /* one graph line                              */
#define GC_PATH_MAX     4096   /* a built path                                */
#define GC_LABEL_MAX    1024   /* label field                                 */
#define GC_CAT_MAX        64   /* category field                              */
#define GC_URL_MAX      2048   /* url field                                   */
#define GC_FANOUT_MAX   4096   /* ids kept from one list field; more are cut  */
#define GC_VISIT_MAX   16384   /* nodes one neighbourhood may hold            */
#define GC_DEPTH_MAX       8   /* view depth the server will expand to        */

#endif /* GC_COMMON_H */
