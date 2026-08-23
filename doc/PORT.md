# The port: NavigatorApplet (Java 1.0, 1999) to the browser canvas

Source of the port: `legacy/Z-order/graph/src`, the last snapshot
(2000-01-03). `web/graphcrawl.js` keeps one section per class, the class
names, and the integer arithmetic, so the picture and the timing come out
the same. Everything added since is a page parameter with the 1999
behaviour as one of its values (the table at the end).

## Class map

| Java | JavaScript | notes |
| --- | --- | --- |
| `NavigatorApplet` | `NavigatorApplet` | `<param>` values come from `window.GRAPHCRAWL_PARAMS`; `getParameter`, `showStatus`, `getImage` kept |
| `graph.GraphView` (Panel) | `GraphView` | the canvas; `size()`/`bounds()` are the CSS box divided by the zoom |
| `graph.NodeView` | `NodeView` | `setNodeModel` lets a stub made from an id take the node the server sends |
| `graph.Sprite` (Rectangle) | `Sprite` | `inside`, `update` with edge reflection |
| `graph.WrappedLabel` | `WrappedLabel` | the word-wrap loop verbatim, including the last word overflowing |
| `graph.Edge` | `Edge` | arrow at the middle of a parent edge |
| `graph.Motor` (Thread) | `Motor` | `setInterval(100)`; suspended, two more repaints then none |
| `graph.Animation` | `Animation` | dashed line, period 4, frame change every 12 paints |
| `graph.Categories` | `Categories` | `category_icon_N`, `ovals_min_id`, `ovals_max_id` |
| `graph.GrayFilter` | `GrayFilter` | the same per-pixel integer transform, once per icon |
| `graph.AngleMath` | `AngleMath` | tables replaced by the functions; integer degrees and the atan2 halving loop kept |
| `graph.Node`, `GraphModel`, `History` | same | `Map` instead of `Hashtable`; ids stay strings |
| `graph.Parser` | `Parser` | the `\;` skip and `decode` verbatim; `#` lines skipped |
| `graph.InfoRequestThread`, `HttpRequest`, `CGIParameters` | `InfoRequest` | `fetch` of `cgi?keywordid=&depth=` (`wid`, `ThemeID` sent when set); a `#cut N` trailer is reported |
| `graph.ShowURLThread` | `NavigatorApplet.showUrl` | the `content` frame is an `<iframe name="content">` |
| `graph.OrderedHashtable` | `OrderedHashtable` | `Map` in insertion order; `putTop` re-inserts |
| `graph.ToolsPanel` | `ToolsPanel` | Tree Depth, History, and the added Back, Go to id, Find |
| `graph.ArriveEvent`, `ArriveEventListener` | `ArriveEvent`, `onArrive` | |
| `graph.Config`, `ToolTpisManagerThread`, `Utils` | dropped | unused in 1999 too |

## Behaviour kept

| | |
| --- | --- |
| placement | neighbours of the central node on a full circle, deeper ones on a 120 degree sector facing away; angles integer degrees; edges 120 px (or longer, see `spread`) |
| double-click | two mouse downs within 300 ms; shift held keeps the content frame |
| crawl | the view empties to the clicked node, which glides at 30 px per 100 ms tick to the centre; arrival is leaving the rectangle spanned by start and centre (1 px margin), so it overshoots by up to a step; the request runs meanwhile and the expansion waits for arrival |
| colours | central yellow, others cyan, terminal (no children) green, border gray, visited label and child edge blue |
| hidden neighbours | 35 px stubs with 4 px bulbs, on a 90 degree fan away from the centre, 360 for the central node |
| parent edges | the gray arrowhead at the midpoint |
| hover | the full label in a white box with a black border, word-wrapped; an icon is drawn through `GrayFilter` meanwhile |
| drag | a node, bounded by the panel; shift-drag does nothing (`forAll` returned early in 1999 too) |
| z-order | the node under the mouse moves to the top |
| depth change | re-expands the central node from the model in hand |
| history | every node shown in the content frame; choosing one re-centres it |
| waiting | "Connecting to the server..." with the two server gifs, while `waitingForServer` |
| static mode | `line_N` parameters instead of a server, with the 1999 one second simulated delay (`delay_ms`) |

## Deviations

- `repaint()` is synchronous: the motor tick calls `update()` then `paint()`
  in place of AWT's queued repaint. Queueing it on `requestAnimationFrame`
  was tried and froze the glide in a hidden tab and in a headless capture.
  A mouse move therefore advances a gliding node by one step, as it did in
  AWT when `update(g)` ran.
- `OrderedHashtable.put` of an existing key moves it to the top. In 1999 the
  key was appended a second time and painted twice; the visible order is the
  same.
- A node without a URL shows its record in the content frame. In 1999 the
  frame stayed as it was.
- Font metrics are fixed at height 15, descent 3 (Dialog 12 on Windows);
  widths come from `measureText`.
- No offscreen image: the canvas is double-buffered by the browser.
- Threads are `fetch`, `setTimeout` and `setInterval`; a superseded request is
  aborted and its result dropped, as `Thread.stop()` did.
- The crawl is in the URL: `#ID` is pushed on every re-centring, so the
  browser's Back and Forward walk it, and a link to a node works.

## Added, each a parameter

| parameter | default | 1999 value | what |
| --- | --- | --- | --- |
| `depth` | 2 | 2 | the starting depth; `?depth=N` in the URL overrides |
| `depth_max` | 6 | 3 | the last entry of the Tree Depth choice (the server stops at 8) |
| `fit` | true | false | after an expansion, zoom out until every node is on the canvas; the wheel zooms, dragging the background pans; nodes keep their 1999 coordinates, the world box is the canvas divided by the zoom |
| `spread` | 40 | 0 | pixels of arc per neighbour: the circle's radius grows past 120 px so labels do not pile up on a hub |
| `fanout_max` | 60 | unlimited | neighbours of one node given a view; the rest are hidden stubs with a `+N` |
| `stubs_max` | 24 | unlimited | stubs drawn per node; the rest is the `+N` |
| `terminal_crawl` | true | false | double-click crawls into a node without children too; in 1999 a terminal only showed its URL |
| Go to id, Find, Back | | absent | the id box re-centres; Find asks `/api/find` (or searches the `line_N` parameters) and lists links; Back is the browser's |
| status line | | "Getting information from the server" only | nodes shown and neighbours hidden after an expansion; the node under the mouse with its counts; the walk's cut |

The expansion sizes every node with one paint before the fit, because the
1999 `update()` clamps nodes into the panel and would pull a spilled
neighbourhood to the borders before the view had a chance to zoom out.
