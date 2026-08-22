/* web.h -- the embedded web front-end: every file of web/ as bytes, built
 * into web.c by embed.sh so the binary is self-contained. */
#ifndef GC_WEB_H
#define GC_WEB_H

#include <stddef.h>

struct gc_asset {
    const char *name;
    const char *ctype;
    const unsigned char *data;
    size_t len;
};

extern const struct gc_asset gc_assets[];
extern const size_t gc_assets_n;

#endif /* GC_WEB_H */
