CC     ?= cc
CFLAGS ?= -O2
.PHONY: all clean
all: mkgraph
mkgraph: mkgraph.c
	$(CC) -std=c99 -W -Wall $(CFLAGS) -o mkgraph mkgraph.c
clean:
	-rm -f mkgraph
