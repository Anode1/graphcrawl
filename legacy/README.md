# The 1999 applet

Five snapshots of the JBuilder 3 project, Java 1.0 AWT, by Vasili Gavrilov.
Each holds `src/NavigatorApplet.java` and `src/graph/*.java`, the compiled
`classes/`, the icons and the test pages. `WS_FTP.LOG` files record uploads
to `www.snap.ca/.../thinkwire/nonlinear/applet/vasili/` from 1999-11-25.

| directory | newest source | differs from `graph/` by |
| --- | --- | --- |
| `31.12.99` | 1999-12-27 | older: no `Config`, no `GrayFilter`, earlier `Sprite`, `WrappedLabel`, `ToolsPanel` |
| `2.01.99` | 2000-01-02 | `depth=2` default (`graph/` has 10) |
| `graph` | 2000-01-02 | the base |
| `graph_demo` | 2000-01-02 | `depth=2`; `classes/alesh.html`, the family demo |
| `Z-order/graph` | 2000-01-03 | the last: `OrderedHashtable` keeps a key order, `putTop` raises the node under the mouse, `getContains` returns the topmost |

`New jZip archive file.zip` is `graph_demo` again. `ToolTpisManagerThread.java`
was never wired in. `1.jpg`, `2.jpg`, `3.jpg` are screenshots of the applet
(a file-system graph; the family); `graph.jpg` is a hand sketch of a
genealogy as a DAG over time.

`Shimon_Even_Advanced_Graph_Algorithms/` holds photographs of lecture notes
from Professor Even's course, kept for the record.

The port is `web/graphcrawl.js`; `doc/PORT.md` maps the classes.
