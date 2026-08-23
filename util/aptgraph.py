#!/usr/bin/env python3
"""aptgraph.py -- the package dependency graph of this machine's apt, as a
graphcrawl file: one node per available package, ids in name order (so
the file is sorted by construction), children = Depends, url = the
packages.ubuntu.com page. Parents come from `graphcrawl --reverse | sort`.

    apt-cache dumpavail | util/aptgraph.py > example/packages.txt
    graphcrawl --reverse example/packages.txt | sort -t';' -k1,1n -k2,2n -u > example/packages.txt.parents

A real graph with real hubs: libc6 has tens of thousands of dependents.
Memory is the name table (one entry per package); the only part of this
project that holds a whole graph, and it is a build step, not the viewer.
"""
import re
import sys

pkgs = {}          # name -> [deps]
name = None
for line in sys.stdin:
    if line.startswith('Package:'):
        name = line.split(':', 1)[1].strip()
        pkgs.setdefault(name, [])
    elif line.startswith('Depends:') and name:
        deps = []
        for alt in line.split(':', 1)[1].split(','):
            for d in alt.split('|'):
                d = re.sub(r'\(.*?\)', '', d).strip().split(':')[0]
                if d:
                    deps.append(d)
        pkgs[name] = deps

names = sorted(pkgs)
ident = {n: i for i, n in enumerate(names)}
for n in names:
    kids = sorted({ident[d] for d in pkgs[n] if d in ident and d != n})
    sys.stdout.write('%d;%s;;https://packages.ubuntu.com/%s;%s;\n'
                     % (ident[n], n, n, ','.join(str(k) for k in kids)))
