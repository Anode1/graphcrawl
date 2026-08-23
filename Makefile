# graphcrawl -- top level. The engine is in c/; util/ holds the generator.
#
#   make            build c/graphcrawl, copied up as ./graphcrawl
#   make ut         engine tests + CLI/HTTP tests (tests/run.sh)
#   make example    example/big.txt, 100k nodes, checked
#   make clean

.PHONY: all ut codeut cliut clean example film

all:
	$(MAKE) -C c all
	$(MAKE) -C util all
	@cp -f c/graphcrawl graphcrawl && echo "built ./graphcrawl"

codeut:
	$(MAKE) -C c ut

cliut: all
	@sh tests/run.sh "$(CURDIR)/c/graphcrawl"

ut: codeut cliut

example: all
	./util/mkgraph -n 100000 -H 500 -l 50 > example/big.txt
	./graphcrawl --check example/big.txt
	apt-cache dumpavail | python3 util/aptgraph.py > example/packages.txt
	./graphcrawl --reverse example/packages.txt | sort -t';' -k1,1n -k2,2n -u > example/packages.txt.parents
	./graphcrawl --check example/packages.txt

# The README's moving figure (screenshots/crawl.gif): tests/film.sh over the
# package graph. Needs Chrome and ImageMagick; the output is a committed fixture.
film: all
	@test -s example/packages.txt || $(MAKE) example
	@sh tests/film.sh example/packages.txt screenshots/crawl.gif

clean:
	$(MAKE) -C c clean
	$(MAKE) -C util clean
	-rm -f graphcrawl
