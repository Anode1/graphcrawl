/* log.h -- die() for the CLI, debug() gated on -d. Modules return codes;
 * only main.c dies.
 */
#ifndef GC_LOG_H
#define GC_LOG_H

void die(const char *fmt, ...);
void debug(const char *fmt, ...);
void debug_enable(int on);

#endif /* GC_LOG_H */
