# The port: NavigatorApplet (Java 1.0, 1999) to the browser canvas

Source of the port: `legacy/Z-order/graph/src`, the last snapshot
(2000-01-03). `web/graphcrawl.js` keeps one section per class, the class
names, and the integer arithmetic, so the picture and the timing come out
the same.

## Class map

| Java | JavaScript | notes |
| --- | --- | --- |
| `NavigatorApplet` | `NavigatorApplet` | `<param>` values come from `window.GRAPHCRAWL_PARAMS`; `getParameter`, `showStatus`, `getImage` kept |
| `graph.GraphView` (Panel) | `GraphView` | the canvas; `size()`/`bounds()` are the CSS box |
| `graph.NodeView` | `NodeView` | |
| `graph.Sprite` (Rectangle) | `Sprite` | `inside`, `update` with edge reflection |
| `graph.WrappedLabel` | `WrappedLabel` | the word-wrap loop verbatim, including the last word overflowing |
| `graph.Edge` | `Edge` | arrow at the middle of a parent edge |
| `graph.Motor` (Thread) | `Motor` | `setInterval(100)`; suspended, two more repaints then none |
| `graph.Animation` | `Animation` | dashed line, period 4, frame change every 12 paints |
| `graph.Categories` | `Categories` | `category_icon_N`, `ovals_min_id`, `ovals_max_id` |
| `graph.GrayFilter` | `GrayFilter` | the same per-pixel integer transform, once per icon |
| `graph.AngleMath` | `AngleMath` | tables replaced by the functions; integer degrees and the atan2 halving loop kept |
| `graph.Node`, `GraphModel`, `History` | same | `Map` instead of `Hashtable`; ids stay strings |
| `graph.Parser` | `Parser` | the `\;` skip and `decode` verbatim |
| `graph.InfoRequestThread`, `HttpRequest`, `CGIParameters` | `InfoRequest` | `fetch` of `cgi?keywordid=&depth=` (`wid`, `ThemeID` sent when set) |
| `graph.ShowURLThread` | `NavigatorApplet.showUrl` | the `content` frame is an `<iframe name="content">` |
| `graph.OrderedHashtable` | `OrderedHashtable` | `Map` in insertion order; `putTop` re-inserts |
| `graph.ToolsPanel` | `ToolsPanel` | two `<select>`s: Tree Depth (2, 3) and History |
| `graph.ArriveEvent`, `ArriveEventListener` | `ArriveEvent`, `onArrive` | |
| `graph.Config`, `ToolTpisManagerThread`, `Utils` | dropped | unused in 1999 too |

## Behaviour kept

| | |
| --- | --- |
| placement | neighbours of the central node on a full circle, deeper ones on a 120 degree sector facing away; edges 120 px; angles integer degrees |
| double-click | two mouse downs within 300 ms; shift held keeps the content frame |
| crawl | the view empties to the clicked node, which glides at 30 px per 100 ms tick to the centre; arrival is leaving the rectangle spanned by start and centre (1 px margin), so it overshoots by up to a step; the request runs meanwhile and the expansion waits for arrival |
| terminal nodes | no children: green, double-click only shows the URL |
| colours | central yellow, others cyan, terminal green, border gray, visited label and child edge blue |
| hidden neighbours | 35 px stubs with 4 px bulbs, on a 90 degree fan away from the centre, 360 for the central node |
| parent edges | the gray arrowhead at the midpoint |
| hover | the full label in a white box with a black border, word-wrapped; an icon is drawn through `GrayFilter` meanwhile |
| drag | bounded by the panel; shift-drag does nothing (`forAll` returned early in 1999 too) |
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
- `first_node` may come from `#id` in the URL or from `/api/info`.
- Threads are `fetch`, `setTimeout` and `setInterval`; a superseded request is
  aborted and its result dropped, as `Thread.stop()` did.
