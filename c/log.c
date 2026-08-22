/* log.c -- see log.h. */
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>

#include "log.h"

static int debug_on;

void debug_enable(int on)
{
    debug_on = on;
}

void die(const char *fmt, ...)
{
    va_list ap;

    fputs("graphcrawl: ", stderr);
    va_start(ap, fmt);
    vfprintf(stderr, fmt, ap);
    va_end(ap);
    fputc('\n', stderr);
    exit(1);
}

void debug(const char *fmt, ...)
{
    va_list ap;

    if (!debug_on)
        return;
    fputs("debug: ", stderr);
    va_start(ap, fmt);
    vfprintf(stderr, fmt, ap);
    va_end(ap);
    fputc('\n', stderr);
}
