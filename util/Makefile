CC     ?= cc
CFLAGS ?= -O2
.PHONY: all clean
all: mkgraph evict
mkgraph: mkgraph.c
	$(CC) -std=c99 -W -Wall $(CFLAGS) -o mkgraph mkgraph.c
evict: evict.c
	$(CC) -std=c99 -W -Wall $(CFLAGS) -o evict evict.c
clean:
	-rm -f mkgraph evict
