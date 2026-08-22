# graphcrawl -- top level. The engine is in c/; util/ holds the generator.
#
#   make            build c/graphcrawl, copied up as ./graphcrawl
#   make ut         engine tests + CLI/HTTP tests (tests/run.sh)
#   make example    example/big.txt, 100k nodes, checked
#   make clean

.PHONY: all ut codeut cliut clean example

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
	./util/mkgraph -n 100000 > example/big.txt
	./graphcrawl --check example/big.txt

clean:
	$(MAKE) -C c clean
	$(MAKE) -C util clean
	-rm -f graphcrawl
