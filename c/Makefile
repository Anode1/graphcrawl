# c/Makefile -- the graphcrawl engine. Honors CC CFLAGS CPPFLAGS LDFLAGS LDLIBS;
# project flags are appended. make | make debug | make ut | make clean.
SHELL = /bin/sh

BIN     = graphcrawl
TESTBIN = graphcrawl_ut

CC       ?= cc
CFLAGS   ?= -O2
CPPFLAGS ?=
LDFLAGS  ?=
LDLIBS   ?=

GC_CFLAGS = -std=c99 -W -Wall

GC_VERSION := $(shell git describe --tags --always --dirty 2>/dev/null | sed 's/^v//')
ifeq ($(strip $(GC_VERSION)),)
GC_VERSION := 0.0.0-dev
endif
GC_VERDEF = -DGC_VERSION='"$(GC_VERSION)"'

# web.c is generated from ../web by embed.sh whenever a file there changes.
WEB       := $(wildcard ../web/*)
SOURCES.c := $(filter-out web.c,$(wildcard *.c)) web.c
OBJS       = $(SOURCES.c:.c=.o)

debug: CFLAGS = -g -O0

.SUFFIXES:
.SUFFIXES: .o .c
%.o: %.c
	$(CC) $(GC_CFLAGS) $(GC_VERDEF) $(CPPFLAGS) $(CFLAGS) -MMD -c $< -o $@

.PHONY: all debug ut ut-asan clean

all debug: $(BIN)

web.c: $(WEB) embed.sh
	sh embed.sh ../web > web.c

$(BIN): $(OBJS)
	$(CC) $(GC_CFLAGS) $(CFLAGS) $(LDFLAGS) -o $(BIN) $(OBJS) $(LDLIBS)

$(TESTBIN): $(SOURCES.c)
	$(CC) $(GC_CFLAGS) -g -DUNIT_TEST $(CPPFLAGS) $(SOURCES.c) -o $(TESTBIN) $(LDLIBS)

ut: $(TESTBIN)
	./$(TESTBIN)

ut-asan: $(SOURCES.c)
	$(CC) $(GC_CFLAGS) -g -DUNIT_TEST -fsanitize=address,undefined -fno-omit-frame-pointer \
	  $(CPPFLAGS) $(SOURCES.c) -o $(TESTBIN)_asan $(LDLIBS)
	./$(TESTBIN)_asan

clean:
	-rm -f $(BIN) $(TESTBIN) $(TESTBIN)_asan $(OBJS) $(OBJS:.o=.d) web.c

-include $(OBJS:.o=.d)
