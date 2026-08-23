/* graphcrawl.js -- the 1999 NavigatorApplet (Java 1.0, AWT) on the browser
 * canvas. One section per original class, same names, same integer
 * arithmetic where Java had ints, so positions, angles and timings come out
 * the same: 100 ms motor, 30 px per tick toward the centre, 120 px edges,
 * 120 degree sectors, 300 ms double click. doc/PORT.md maps the classes and
 * lists the deviations.
 *
 * Copyright (C) 1999, 2026 Vasili Gavrilov. GNU GPL v2 or later.
 */
'use strict';

/* java.awt.Color names the applet used */
const Color = {
  white: '#ffffff', black: '#000000', cyan: '#00ffff', green: '#00ff00',
  yellow: '#ffff00', gray: '#808080', blue: '#0000ff', lightGray: '#c0c0c0'
};

/* The look. "1999" is the applet's: Dialog 12, square cyan/green/yellow
 * boxes with a gray border, black edges. "modern" keeps the same meanings
 * in a current palette: rounded cards, system type, slate edges. Geometry
 * is the same in both; only paint differs. */
const Themes = {
  '1999': {
    font: '12px sans-serif', rowHeight: 15, descent: 3, pad: 2, radius: 0, shadow: null,
    background: Color.white, text: Color.black, visitedText: Color.blue,
    central: { fill: Color.yellow, border: Color.gray },
    more:    { fill: Color.cyan,   border: Color.gray },    /* has neighbours off screen */
    done:    { fill: Color.green,  border: Color.gray },    /* every neighbour on screen */
    edge: Color.black, edgeVisited: Color.blue, arrow: Color.gray, arrowLength: 20,
    labelFill: Color.white, labelBorder: Color.black
  },
  modern: {
    font: '13px ui-sans-serif, system-ui, -apple-system, "Segoe UI", Roboto, sans-serif',
    rowHeight: 18, descent: 4, pad: 6, radius: 6, shadow: 'rgba(0, 0, 0, 0.15)',
    background: '#fafafa', text: '#374151', visitedText: '#1d4ed8',
    central: { fill: '#fef3c7', border: '#f59e0b' },
    more:    { fill: '#dbeafe', border: '#3b82f6' },
    done:    { fill: '#dcfce7', border: '#22c55e' },
    edge: '#9ca3af', edgeVisited: '#60a5fa', arrow: '#9ca3af', arrowLength: 14,
    labelFill: '#ffffff', labelBorder: '#d1d5db'
  }
};
let Theme = Themes.modern;

/* java.awt.Rectangle: what Sprite and the stop bounds use of it */
class Rect {
  constructor(x, y, w, h) { this.x = x; this.y = y; this.width = w; this.height = h; }
  contains(px, py) {
    return px >= this.x && py >= this.y && px < this.x + this.width && py < this.y + this.height;
  }
  add(px, py) {                                    /* Rectangle.add(Point) */
    const x1 = Math.min(this.x, px), y1 = Math.min(this.y, py);
    const x2 = Math.max(this.x + this.width, px), y2 = Math.max(this.y + this.height, py);
    this.x = x1; this.y = y1; this.width = x2 - x1; this.height = y2 - y1;
  }
}

/* java.awt.Graphics over a 2d context: the calls the applet made, with AWT's
 * pixel conventions (a drawRect outlines w+1 x h+1 pixels; a line is one
 * pixel wide on pixel centres). Font metrics mimic Dialog 12 plain. */
class Graphics {
  constructor(ctx, scale) {
    this.ctx = ctx;
    ctx.font = Theme.font;
    ctx.textBaseline = 'alphabetic';
    ctx.lineWidth = 1 / (scale || 1);
    this.fm = { height: Theme.rowHeight, descent: Theme.descent, stringWidth: (s) => Math.ceil(ctx.measureText(s).width) };
    this.setColor(Theme.text);
  }
  /* a rounded box: the modern card; radius 0 is the 1999 rectangle */
  roundRect(x, y, w, h, r, fill, stroke) {
    const c = this.ctx;
    if (r <= 0) {
      if (fill) c.fillRect(x, y, w, h);
      if (stroke) c.strokeRect(x + 0.5, y + 0.5, w, h);
      return;
    }
    c.beginPath();
    c.moveTo(x + r, y);
    c.arcTo(x + w, y, x + w, y + h, r);
    c.arcTo(x + w, y + h, x, y + h, r);
    c.arcTo(x, y + h, x, y, r);
    c.arcTo(x, y, x + w, y, r);
    c.closePath();
    if (fill) {
      if (Theme.shadow) { c.shadowColor = Theme.shadow; c.shadowBlur = 3; c.shadowOffsetY = 1; }
      c.fill();
      c.shadowColor = 'transparent'; c.shadowBlur = 0; c.shadowOffsetY = 0;
    }
    if (stroke) c.stroke();
  }
  getFontMetrics() { return this.fm; }
  setColor(c) { this.color = c; this.ctx.fillStyle = c; this.ctx.strokeStyle = c; }
  getColor() { return this.color; }
  fillRect(x, y, w, h) { this.ctx.fillRect(x, y, w, h); }
  drawRect(x, y, w, h) { this.ctx.strokeRect(x + 0.5, y + 0.5, w, h); }
  fillOval(x, y, w, h) {
    const c = this.ctx;
    c.beginPath(); c.ellipse(x + w / 2, y + h / 2, w / 2, h / 2, 0, 0, 2 * Math.PI); c.fill();
  }
  drawOval(x, y, w, h) {
    const c = this.ctx;
    c.beginPath(); c.ellipse(x + 0.5 + w / 2, y + 0.5 + h / 2, w / 2, h / 2, 0, 0, 2 * Math.PI); c.stroke();
  }
  drawLine(x0, y0, x1, y1) {
    const c = this.ctx;
    c.beginPath(); c.moveTo(x0 + 0.5, y0 + 0.5); c.lineTo(x1 + 0.5, y1 + 0.5); c.stroke();
  }
  drawString(s, x, y) { this.ctx.fillText(s, x, y); }
  drawImage(img, x, y) { if (img) this.ctx.drawImage(img, x, y); }
  fillPolygon(xs, ys, n) {
    const c = this.ctx;
    c.beginPath(); c.moveTo(xs[0], ys[0]);
    for (let i = 1; i < n; i++) c.lineTo(xs[i], ys[i]);
    c.closePath(); c.fill();
  }
}

/* ---- graph/AngleMath.java: degree tables at 1 degree resolution. The
 * tables are replaced by the functions they cached; the argument handling
 * (integer degrees, the atan2 halving loop) is kept, since it shapes the
 * picture. ---- */
const AngleMath = {
  DEG_TO_RAD: (2.0 * Math.PI) / 360.0,
  RAD_TO_DEG: 360.0 / (2.0 * Math.PI),
  TABLE_SIZE: 360,
  cos(degree) {
    if (degree >= 360) degree = degree % 360;
    else if (degree < 0) degree = (-degree) % 360;
    return Math.cos(this.DEG_TO_RAD * degree);
  },
  sin(degree) {
    if (degree >= 360) degree = degree % 360;
    else if (degree < 0) {
      degree = (-degree) % 360;
      return -Math.sin(this.DEG_TO_RAD * degree);
    }
    return Math.sin(this.DEG_TO_RAD * degree);
  },
  /* atan_table[v1x+180][v1y+180] = atan2(v1x, v1y) in degrees 0..360, after
   * halving both arguments into the table's range (integer division). */
  atan2(v1x, v1y) {
    while (Math.abs(v1x) >= this.TABLE_SIZE / 2 || Math.abs(v1y) >= this.TABLE_SIZE / 2) {
      v1x = Math.trunc(v1x / 2);
      v1y = Math.trunc(v1y / 2);
    }
    let t = Math.atan2(v1x, v1y) * this.RAD_TO_DEG;
    if (t < 0.0) t += 360.0;
    return t;
  }
};

/* ---- graph/GrayFilter.java: the per-pixel RGB transform, applied once per
 * icon to an offscreen canvas (FilteredImageSource) ---- */
const GrayFilter = {
  filterRGB(rgb) {
    const r = Math.floor(((rgb & 0xff0000) + 0x018000) / 3) & 0xff0000;
    const g = Math.floor(((rgb & 0x00ff00) + 0x018000) / 3) & 0x00ff00;
    const b = Math.floor(((rgb & 0x0000ff) + 0x000180) / 3) & 0x0000ff;
    return r | g | b;
  },
  apply(img) {
    const c = document.createElement('canvas');
    c.width = img.width; c.height = img.height;
    const x = c.getContext('2d');
    x.drawImage(img, 0, 0);
    const d = x.getImageData(0, 0, c.width, c.height), p = d.data;
    for (let i = 0; i < p.length; i += 4) {
      const o = this.filterRGB((p[i] << 16) | (p[i + 1] << 8) | p[i + 2]);
      p[i] = (o >> 16) & 0xff; p[i + 1] = (o >> 8) & 0xff; p[i + 2] = o & 0xff;
    }
    x.putImageData(d, 0, 0);
    return c;
  }
};

/* ---- graph/ArriveEvent.java ---- */
class ArriveEvent {
  constructor(node) { this.source = node; }
  getSource() { return this.source; }
}

/* ---- graph/Sprite.java: a rectangle with velocity, shape, image, stop
 * bounds and listeners ---- */
class Sprite {
  constructor(panel) {
    this.panel = panel;
    this.x = 0; this.y = 0;
    this.width = 5; this.height = 5;                /* for debugging purposes */
    this.v = { x: 0, y: 0 };
    this.stopBounds = null;
    this.listeners = [];
    this.draggable = true;
    this.type = Sprite.RECTANGLE;
    this.image = null; this.grayedImage = null;
    this.imageWidth = 0; this.imageHeight = 0;
    this.color = Theme.more.fill;
    this.borderColor = Theme.more.border;
    this.oldx = 0; this.oldy = 0;
  }
  setType(t) { this.type = t; }
  getType() { return this.type; }
  setColor(c) { this.color = c; }
  getColor() { return this.color; }
  setV(vx, vy) {
    if (typeof vx === 'object') { this.v.x = vx.x; this.v.y = vx.y; }
    else { this.v.x = vx; this.v.y = vy; }
  }
  getMiddleX() { return this.x + (this.width >> 1); }
  getMiddleY() { return this.y + (this.height >> 1); }
  inside(X, Y) { return X >= this.x && Y >= this.y && X < this.x + this.width && Y < this.y + this.height; }
  getCenter() { return { x: this.getMiddleX(), y: this.getMiddleY() }; }
  setCenter(cx, cy) { this.x = cx - (this.width >> 1); this.y = cy - (this.height >> 1); }
  setDraggable(b) { this.draggable = b; }
  isDraggable() { return this.draggable; }
  setImage(image) {
    this.image = image;
    this.grayedImage = GrayFilter.apply(image);
    this.imageWidth = image.width; this.imageHeight = image.height;
    this.width = this.imageWidth; this.height = this.imageHeight;
  }
  /* walk around the situation when the rectangle has width or height 0 */
  setStopBounds(r) {
    this.stopBounds = r;
    if (r != null) { r.x -= 1; r.y -= 1; r.width += 2; r.height += 2; }
  }
  getStopBounds() { return this.stopBounds; }
  paintShaded(g) { this.paintSprite(g, true); }
  paint(g) { this.paintSprite(g, false); }
  paintSprite(g, shaded) {
    const ind = Sprite.indent;
    if (this.image == null) {
      const defaultColor = g.getColor();
      g.setColor(this.color);
      if (this.type === Sprite.RECTANGLE) {
        g.roundRect(this.x - ind, this.y, this.width + ind + ind, this.height, Theme.radius, true, false);
        g.setColor(this.borderColor);
        g.roundRect(this.x - ind, this.y, this.width + ind + ind, this.height, Theme.radius, false, true);
      } else {
        g.fillOval(this.x - 1 - ind, this.y, this.width + ind + ind + 2, this.height);
        g.setColor(this.borderColor);
        g.drawOval(this.x - 1 - ind, this.y, this.width + ind + ind + 2, this.height);
      }
      g.setColor(defaultColor);
    } else {
      const prevColor = g.getColor();
      g.setColor(Color.white);
      g.fillRect(this.x, this.y, this.imageWidth, this.imageHeight);   /* white background for icon */
      g.setColor(prevColor);
      g.drawImage(shaded ? this.grayedImage : this.image, this.x, this.y);
    }
  }
  /* move by the velocity, reflecting off the panel's edges */
  update() {
    const ind = Sprite.indent, size = this.panel.size();
    let temp = ind;
    this.x += this.v.x;
    if (this.x < temp) { this.x = temp; this.v.x = -this.v.x; }
    temp = size.width - this.width - ind - ind + 1;
    if (this.x > temp) { this.x = temp; this.v.x = -this.v.x; }
    temp = ind;
    this.y += this.v.y;
    if (this.y < temp) { this.y = temp; this.v.y = -this.v.y; }
    temp = size.height - this.height - ind - ind + 1;
    if (this.y > temp) { this.y = temp; this.v.y = -this.v.y; }
  }
}
Sprite.OVAL = 0;
Sprite.RECTANGLE = 1;
Sprite.indent = 2;

/* ---- graph/WrappedLabel.java: the long label shown under the cursor ---- */
class WrappedLabel {
  constructor(panel, string) {
    this.panel = panel; this.string = string;
    this.x0 = 0; this.y0 = 0;
    this.width = 5; this.height = WrappedLabel.row_height;
    this.fm = null; this.shortLabelWidth = 0; this.descent = 0;
    this.dynamic = false;
    this.strings = null; this.x = null; this.y = null;
    this.bgColor = Color.white;
  }
  getWidth() { return this.width; }
  getHeight() { return this.height; }
  setBGColor(c) { this.bgColor = c; }
  isInitialized() { return this.fm != null; }
  init(fm, row_height, descent, shortLabelWidth) {
    this.fm = fm;
    WrappedLabel.row_height = row_height;
    this.descent = descent;
    this.shortLabelWidth = shortLabelWidth;
    const longLabelWidth = fm.stringWidth(this.string);
    if (longLabelWidth > shortLabelWidth) {
      this.dynamic = true;
    } else {
      this.dynamic = false;
      this.height = row_height;
      this.width = longLabelWidth;
    }
  }
  /* divide the string into lines and place them; called when the node moves */
  reset(x0, y0) {
    const indent = WrappedLabel.indent, row_height = WrappedLabel.row_height;
    this.x0 = x0 >= indent ? x0 : indent;
    this.y0 = y0;
    const rightBorder = this.panel.size().width;
    if (!this.dynamic) {
      if (this.x0 > rightBorder - this.width - indent - indent + 1)
        this.x0 = rightBorder - this.width - indent - indent + 1;
      return;
    }
    const words = this.string.split(/\s+/).filter((w) => w.length > 0);
    const numWords = words.length;
    /* Never below 0: the loop below only advances while a line still fits, so
     * a negative width would spin forever. 1999 could not produce one, but the
     * world is now the canvas divided by the zoom (doc/PORT.md, `fit`), and
     * zooming in on a narrow panel makes it narrower than a short label. */
    const maxWidth = Math.max(0, rightBorder - this.shortLabelWidth - indent - indent);
    const strings = [];
    let curLine = '';
    for (let i = 0; i < numWords;) {
      while (this.fm.stringWidth(curLine) <= maxWidth) {
        curLine += words[i] + ' ';
        i++;
        if (i === numWords) break;
      }
      strings.push(curLine);
      curLine = '';
    }
    this.strings = strings;
    this.x = new Array(strings.length);
    this.y = new Array(strings.length);
    this.width = 0; this.height = 0;
    for (let i = 0; i < strings.length; i++) {
      const w = this.fm.stringWidth(strings[i]);
      if (w > this.width) this.width = w;
      this.height += row_height;
      this.x[i] = this.x0;
      this.y[i] = y0 + i * row_height + row_height - this.descent;
    }
    if (this.x0 > rightBorder - this.width - indent - indent + 1) {
      this.x0 = rightBorder - this.width - indent - indent + 1;
      for (let i = 0; i < strings.length; i++) this.x[i] = this.x0;
    }
  }
  paint(g) {
    const indent = WrappedLabel.indent, row_height = WrappedLabel.row_height;
    const prevColor = g.getColor();
    g.setColor(this.bgColor != null ? this.bgColor : Theme.labelFill);
    g.roundRect(this.x0 - indent, this.y0, this.width + indent + indent, this.height, Theme.radius, true, false);
    g.setColor(Theme.labelBorder);
    g.roundRect(this.x0 - indent, this.y0, this.width + indent + indent, this.height, Theme.radius, false, true);
    g.setColor(prevColor);
    if (this.dynamic) {
      for (let i = 0; i < this.strings.length; i++) g.drawString(this.strings[i], this.x[i], this.y[i]);
    } else {
      g.drawString(this.string, this.x0, this.y0 + row_height - this.descent);
    }
  }
}
WrappedLabel.row_height = 5;
WrappedLabel.indent = 2;

/* ---- graph/NodeView.java: the visible node ---- */
class NodeView extends Sprite {
  constructor(panel, nodeModel) {
    super(panel);
    this.nodeModel = nodeModel;
    this.fromAngle = 0;
    this.toAngle = 360;
    this.wrappedLabel = null;
    this.showLongLabel = false;
    this.expanded = false;
    this.visited = false;
    this.notHighligted = Theme.done;
    this.needReset = true;
    this.shortLabelWidth = -1;
    this.descent = -1;
    this.fm = null;
    this.highlighted = false;
    this.terminal = true;
    this.setNodeModel(nodeModel);
  }
  /* the label, the icon or the shape from the model; a fresh model from a
   * later response replaces a stub made from an id alone */
  setNodeModel(nodeModel) {
    this.nodeModel = nodeModel;
    const label = nodeModel.label;
    this.shortLabel = label.length > NodeView.labelViewLength
      ? label.substring(0, NodeView.labelViewLength - 4) + '...' : label;
    this.fm = null;
    this.wrappedLabel = null;
    const categories = Categories.getInstance();
    const iconImage = categories.getImageForCategory(nodeModel.category);
    if (iconImage == null) {
      this.image = null; this.grayedImage = null;
      this.type = categories.isInOvalRange(nodeModel.id) ? Sprite.OVAL : Sprite.RECTANGLE;
    } else {
      this.setImage(iconImage);
    }
  }
  setLongLabel(b) { this.showLongLabel = b; }
  setHighlighted(h) {
    this.highlighted = h;
    const c = h ? Theme.central : this.notHighligted;
    this.color = c.fill;
    this.borderColor = c.border;
  }
  isHighlighted() { return this.highlighted; }
  isTerminal() { return this.terminal; }
  /* "terminal" in 1999 was "no children"; here it is "nothing more to show" */
  setTerminal(t) {
    this.terminal = t;
    this.notHighligted = t ? Theme.done : Theme.more;
    if (!this.highlighted) { this.color = this.notHighligted.fill; this.borderColor = this.notHighligted.border; }
  }
  resetWrappedLabel() { this.needReset = true; }
  addArriveEventListener(l) { this.listeners.push(l); }
  removeArriveEventListener(l) {
    const i = this.listeners.indexOf(l);
    if (i >= 0) this.listeners.splice(i, 1);
  }
  notifyArriveEventListeners(e) { for (const l of this.listeners.slice()) l.onArrive(e); }
  equals(nodeView) { return nodeView != null && this.nodeModel.equals(nodeView.getNodeModel()); }
  getNodeModel() { return this.nodeModel; }
  /* the velocity vector toward a point, and the stop bounds on the way */
  getVelocity(newPointX, newPointY, velocity) {
    const oldPointX = this.getMiddleX(), oldPointY = this.getMiddleY();
    const angleInRad = Math.atan2(newPointY - oldPointY, newPointX - oldPointX);
    const scaledX = Math.trunc(velocity * Math.cos(angleInRad));
    const scaledY = Math.trunc(velocity * Math.sin(angleInRad));
    const newBounds = new Rect(oldPointX, oldPointY, 0, 0);
    newBounds.add(newPointX, newPointY);
    this.setStopBounds(newBounds);
    return { x: scaledX, y: scaledY };
  }
  /* dimensions from the font, once the first paint has a Graphics */
  init(g) {
    this.fm = g.getFontMetrics();
    this.descent = this.fm.descent;
    NodeView.row_height = this.fm.height;
    this.shortLabelWidth = this.fm.stringWidth(this.shortLabel);
    const oldMiddleX = this.getMiddleX(), oldMiddleY = this.getMiddleY();
    if (this.image == null) {
      this.width = this.shortLabelWidth;
      this.height = NodeView.row_height;
      this.x = oldMiddleX - (this.width >> 1);
      this.y = oldMiddleY - (this.height >> 1);
    } else {
      this.x = oldMiddleX - (this.imageWidth >> 1);
      this.y = oldMiddleY - (this.imageHeight >> 1);
    }
  }
  paint(g) {
    const defaultColor = g.getColor();
    g.setColor(this.visited ? Theme.visitedText : Theme.text);
    if (this.fm == null) this.init(g);
    const row_height = NodeView.row_height;
    let vertShift = 0;
    if (this.image != null) vertShift = this.imageHeight + NodeView.gapBetweenLabelAndImage;
    const horTextPos = this.getMiddleX() - (this.shortLabelWidth >> 1);
    if (this.showLongLabel) {
      if (this.wrappedLabel == null) {
        this.wrappedLabel = new WrappedLabel(this.panel, this.nodeModel.label);
        this.wrappedLabel.setBGColor(Color.white);
        this.wrappedLabel.init(this.fm, row_height, this.descent, this.shortLabelWidth);
      }
      if (this.needReset) {
        this.wrappedLabel.reset(horTextPos, this.y + vertShift);
        this.needReset = false;
      }
      if (this.image != null) super.paintShaded(g);
      this.wrappedLabel.paint(g);
    } else {
      super.paint(g);
      if (this.image != null) {
        const prevColor = g.getColor();
        g.setColor(Theme.background);
        g.fillRect(horTextPos, this.y + vertShift, this.shortLabelWidth, row_height);
        g.setColor(prevColor);
      }
      g.drawString(this.shortLabel, horTextPos, this.y + vertShift + row_height - this.descent);
    }
    g.setColor(defaultColor);
  }
  update() {
    super.update();
    if (this.stopBounds != null) {
      if (!this.stopBounds.contains(this.x + (this.width >> 1), this.y + (this.height >> 1)))
        this.notifyArriveEventListeners(new ArriveEvent(this));
    }
  }
}
NodeView.labelViewLength = 15;
NodeView.gapBetweenLabelAndImage = 2;
NodeView.row_height = -1;

/* ---- graph/Edge.java: a parent edge, with the arrow at its middle ---- */
class Edge {
  constructor(fromNode, toNode) { this.fromNode = fromNode; this.toNode = toNode; }
  getFromNode() { return this.fromNode; }
  getToNode() { return this.toNode; }
  equals(another) {
    return another != null && another.getFromNode().equals(this.fromNode) && another.getToNode().equals(this.toNode);
  }
  paint(g) {
    g.drawLine(this.fromNode.getMiddleX(), this.fromNode.getMiddleY(), this.toNode.getMiddleX(), this.toNode.getMiddleY());
    const defaultColor = g.getColor();
    g.setColor(Theme.arrow);
    this.drawArrow(g, this.fromNode.getMiddleX(), this.fromNode.getMiddleY(), this.toNode.getMiddleX(), this.toNode.getMiddleY());
    g.setColor(defaultColor);
  }
  drawArrow(g, x0, y0, x, y) {
    const math = AngleMath;
    const arrowAngle = 30, arrowLength = Theme.arrowLength;
    const middlePointX = (x0 + x) >> 1, middlePointY = (y0 + y) >> 1;
    const thisEdgeAngle = Math.trunc(math.atan2(y - y0, x - x0)) + 90;
    const leftSegmentAngle = thisEdgeAngle - (arrowAngle >> 1);
    const firstArrowTailX = middlePointX + Math.trunc(arrowLength * math.sin(leftSegmentAngle));
    const firstArrowTailY = middlePointY - Math.trunc(arrowLength * math.cos(leftSegmentAngle));
    const rightSegmentAngle = thisEdgeAngle + (arrowAngle >> 1);
    const secondArrowTailX = middlePointX + Math.trunc(arrowLength * math.sin(rightSegmentAngle));
    const secondArrowTailY = middlePointY - Math.trunc(arrowLength * math.cos(rightSegmentAngle));
    g.fillPolygon([middlePointX, firstArrowTailX, secondArrowTailX],
                  [middlePointY, firstArrowTailY, secondArrowTailY], 3);
  }
}
Edge.color = Color.gray;     /* the 1999 value; Theme.arrow is what paints */

/* ---- graph/Motor.java: the repaint thread, 100 ms; suspended, it repaints
 * twice more and stops ---- */
class Motor {
  constructor(component, refreshPeriod) {
    this.component = component;
    this.refreshPeriod = refreshPeriod || 100;
    this.timer = null;
    this.isSuspended = false;
    this.counter = 0;
  }
  start() { if (this.timer == null) this.timer = setInterval(() => this.run(), this.refreshPeriod); }
  stop() { if (this.timer != null) { clearInterval(this.timer); this.timer = null; } }
  resumeThread() { if (this.isSuspended) this.isSuspended = false; }
  suspendThread() { if (!this.isSuspended) { this.isSuspended = true; this.counter = 0; } }
  run() {
    if (this.isSuspended) {
      this.counter++;
      if (this.counter < 3) this.component.repaint();
    } else {
      this.component.repaint();
    }
  }
}

/* ---- graph/Animation.java: "Connecting to the server..." ---- */
class Animation {
  constructor(panel, applet) {
    this.panel = panel; this.applet = applet;
    this.counter = 0; this.triples = 0; this.periodLength = 4; this.currentFrame = 0;
    this.frames = null;
    this.fromX = 30; this.fromY = 30;
  }
  setPanel(panel) { this.panel = panel; }
  async loadImages() {
    const applet = this.applet;
    const fx = applet.getParameter('iconFromX'), fy = applet.getParameter('iconFromY');
    if (fx != null && !isNaN(parseInt(fx, 10))) this.fromX = parseInt(fx, 10);
    if (fy != null && !isNaN(parseInt(fy, 10))) this.fromY = parseInt(fy, 10);
    const numberString = applet.getParameter('number_of_frames_in_anim');
    if (numberString == null) return;
    const number = parseInt(numberString, 10);
    if (isNaN(number)) return;
    this.frames = new Array(number).fill(null);
    const loads = [];
    for (let i = 0; i < number; i++) {
      const filename = applet.getParameter('file_' + (i + 1));
      if (filename != null) loads.push(applet.getImage(filename).then((img) => { this.frames[i] = img; }));
    }
    await Promise.all(loads);
  }
  paint(g, w, h) {
    this.counter++;
    if (this.counter % 12 === 0) { this.triples++; this.currentFrame++; }
    const numberOfPeriods = Math.trunc(((w >> 1) - this.fromX) / this.periodLength);
    for (let i = 0; i < numberOfPeriods; i++) {
      const currentPeriod = this.triples % 3;
      const beg = this.fromX + (i + currentPeriod) * this.periodLength;
      if (i % 3 === 0) g.drawLine(beg, this.fromY, beg + this.periodLength, this.fromY);
    }
    if (this.frames != null && this.frames.length > 0 && this.frames[this.currentFrame % this.frames.length] != null)
      g.drawImage(this.frames[this.currentFrame % this.frames.length], this.fromX, this.fromY - 32);
    g.drawString('Connecting to the server...', (w >> 1) + 10, this.fromY);
  }
}

/* ---- graph/Categories.java: icons per category, and the id range drawn
 * as ovals ---- */
class Categories {
  constructor(applet) {
    this.applet = applet;
    this.catImages = [];
    Categories.instance = this;
    const mn = applet.getParameter('ovals_min_id'), mx = applet.getParameter('ovals_max_id');
    if (mn != null && !isNaN(parseInt(mn, 10))) Categories.OVALS_MIN_ID = parseInt(mn, 10);
    if (mx != null && !isNaN(parseInt(mx, 10))) Categories.OVALS_MAX_ID = parseInt(mx, 10);
  }
  static getInstance() { return Categories.instance; }
  async loadImages() {
    const loads = [];
    this.catImages = new Array(255).fill(null);
    for (let i = 0; i < 255; i++) {
      const filename = this.applet.getParameter('category_icon_' + (i + 1));
      if (filename != null) loads.push(this.applet.getImage(filename).then((img) => { this.catImages[i] = img; }));
    }
    await Promise.all(loads);
  }
  getImageForCategory(categoryString) {
    if (!/^-?\d+$/.test(categoryString || '')) return null;
    const categoryNum = parseInt(categoryString, 10);
    if (this.catImages == null || this.catImages.length < categoryNum) return null;
    if (categoryNum <= 0) return null;
    return this.catImages[categoryNum - 1] || null;
  }
  isInOvalRange(stringId) {
    if (!/^-?\d+$/.test(stringId || '')) return false;
    const id = parseInt(stringId, 10);
    return Categories.OVALS_MIN_ID < id && Categories.OVALS_MAX_ID > id;
  }
}
Categories.OVALS_MIN_ID = -1000;
Categories.OVALS_MAX_ID = 0;
Categories.instance = null;

/* ---- graph/Node.java: the light node of the cache ---- */
class Node {
  constructor(id, label, category, url, children, parents) {
    this.id = id;
    this.label = label == null ? '' : label;
    this.category = category;
    this.url = url;
    this.children = children;
    this.parents = parents;
  }
  getID() { return this.id; }
  getLabel() { return this.label; }
  getCategory() { return this.category; }
  getUrl() { return this.url; }
  getChildrenIds() { return this.children; }
  getNumChildren() { return this.children.length; }
  getParentsIds() { return this.parents; }
  getNumParents() { return this.parents.length; }
  equals(node) { return node != null && this.id === node.id; }
  toString() {
    return this.id + ';' + this.label + ';' + this.category + ';' + this.url + ';' +
      this.children.join(',') + ';' + this.parents.join(',');
  }
}

/* ---- graph/GraphModel.java: the cache, replaced on every request ---- */
class GraphModel {
  constructor() { this.map = new Map(); }
  getNode(key) { return this.map.has(key) ? this.map.get(key) : null; }
  addNode(key, node) { this.map.set(key, node); }
  removeNode(key) { const n = this.getNode(key); this.map.delete(key); return n; }
  toString() {
    let result = '\r\n';
    for (const n of this.map.values()) result += n.toString() + '\r\n';
    return result;
  }
}

/* ---- graph/History.java ---- */
class History {
  constructor() { this.map = new Map(); }
  getNode(key) { return this.map.has(key) ? this.map.get(key) : null; }
  addNode(key, node) { this.map.set(key, node); }
  keys() { return this.map.keys(); }
  get(key) { return this.map.get(key); }
}

/* ---- graph/Parser.java: one line -> one Node, as in 1999 ---- */
class Parser {
  constructor() { this.graph = new GraphModel(); this.currentLine = 0; }
  parseLine(line) {
    try {
      this.currentLine++;
      if (line.charAt(0) === '#') return;
      const children = [], parents = [];
      let i = line.indexOf(';');
      if (i < 0) throw new Error('no id separator');
      const id = line.substring(0, i);
      const stringWithoutId = line.substring(i + 1);
      i = stringWithoutId.indexOf(';');
      let needToRemove = false;
      while (i > 0 && stringWithoutId.charAt(i - 1) === '\\') {      /* skip "\;" */
        i = stringWithoutId.indexOf(';', i + 1);
        needToRemove = true;
      }
      if (i < 0) throw new Error('no label separator');
      let label = stringWithoutId.substring(0, i);
      if (needToRemove) label = Parser.decode(label);
      const stringWithoutLabel = stringWithoutId.substring(i + 1);
      i = stringWithoutLabel.indexOf(';');
      if (i < 0) throw new Error('no category separator');
      const category = stringWithoutLabel.substring(0, i);
      const stringWithoutCategory = stringWithoutLabel.substring(i + 1);
      i = stringWithoutCategory.indexOf(';');
      if (i < 0) throw new Error('no url separator');
      const value = stringWithoutCategory.substring(0, i);
      const stringWithoutValue = stringWithoutCategory.substring(i + 1);
      /* the rest may be: ";" "5,8;" "34;" ";4,6" ";3" "12,5;53,76" "1;77" */
      const indexOfseparator = stringWithoutValue.indexOf(';');
      if (indexOfseparator > 0) {
        const childrenString = stringWithoutValue.substring(0, indexOfseparator);
        for (const t of childrenString.split(',')) {
          const aChild = t.trim();
          if (aChild.length > 0) children.push(aChild);
        }
      }
      const parentsString = stringWithoutValue.substring(indexOfseparator + 1).trim();
      if (parentsString.length > 0) {
        for (const t of parentsString.split(',')) {
          const aParent = t.trim();
          if (aParent.length > 0) parents.push(aParent);
        }
      }
      this.graph.addNode(id, new Node(id, label, category, value, children, parents));
    } catch (e) {
      console.log('Parser::error in line ' + this.currentLine + ':' + e + ' the line:' + line);
    }
  }
  getGraphModel() { return this.graph; }
  /* removes '\' characters (in labels) */
  static decode(s) { return s.split('\\').join(''); }
}

/* ---- graph/InfoRequestThread.java: fetch the tree, parse it, hand the
 * model to the applet and call dataReady. Stopped requests are ignored. ---- */
class InfoRequest {
  constructor(applet, parentID, depth, limit, fromAppletParameters) {
    this.applet = applet; this.parentID = parentID; this.depth = depth; this.limit = limit;
    this.fromAppletParameters = fromAppletParameters;
    this.debug = false;
    this.alive = false;
    this.abort = null;
    this.error = null;
  }
  setDebug(d) { this.debug = d; }
  start() {
    if (this.alive) return;
    this.alive = true;
    if (this.fromAppletParameters) {
      /* the 1999 debug mode simulated the network with a one second sleep */
      const delay = parseInt(this.applet.getParameter('delay_ms') || '1000', 10);
      setTimeout(() => {
        if (!this.alive) return;
        this.getFromAppletParameters();
        this.applet.dataReady(this.parentID);
      }, delay);
    } else {
      /* delay_ms also holds a server's answer back, to watch the 1999 animation */
      const delay = parseInt(this.applet.getParameter('delay_ms') || '0', 10);
      this.getFromCGI().then(() => {
        if (!this.alive) return;
        /* nothing arrived: the model is still the previous one, so expanding
         * it again would redraw the old view and say nothing */
        if (this.error != null) { this.applet.requestFailed(this.error); return; }
        if (delay > 0) setTimeout(() => { if (this.alive) this.applet.dataReady(this.parentID); }, delay);
        else this.applet.dataReady(this.parentID);
      });
    }
  }
  stop() {
    this.alive = false;
    if (this.abort) this.abort.abort();
  }
  async getFromCGI() {
    const cgi = this.applet.getParameter('cgi');
    if (cgi == null) { console.log('There is no cgi parameter specified'); return; }
    let url = cgi + (cgi.indexOf('?') < 0 ? '?' : '&') + 'keywordid=' + encodeURIComponent(this.parentID);
    const wid = this.applet.getParameter('wid'), themeId = this.applet.getParameter('themeId');
    if (wid != null) url += '&wid=' + encodeURIComponent(wid);
    if (themeId != null) url += '&ThemeID=' + encodeURIComponent(themeId);
    if (this.depth > 0) url += '&depth=' + this.depth;
    if (this.limit > 0) url += '&limit=' + this.limit;
    if (this.debug) console.log('GET:' + url);
    this.abort = new AbortController();
    try {
      const r = await fetch(url, { cache: 'no-store', signal: this.abort.signal });
      const text = await r.text();
      const parser = new Parser();
      let cut = 0;
      for (const raw of text.split('\n')) {
        const line = raw.replace(/\r$/, '');
        if (line.length === 0) continue;
        const m = /^#cut (\d+)/.exec(line);
        if (m) { cut = parseInt(m[1], 10); continue; }
        parser.parseLine(line);
      }
      if (this.alive) { this.applet.setGraphModel(parser.getGraphModel()); this.applet.lastCut = cut; }
    } catch (e) {
      if (this.alive) {
        this.error = e;
        console.log('InfoRequestThread::getFromCGI:Error during http request:' + e);
      }
    }
  }
  getFromAppletParameters() {
    const parser = new Parser();
    for (let i = 0; this.applet.getParameter('line_' + i) != null; i++)
      parser.parseLine(this.applet.getParameter('line_' + i));
    this.applet.setGraphModel(parser.getGraphModel());
  }
}

/* ---- graph/OrderedHashtable.java: the nodes of the view in their Z order.
 * The last key is painted last, on top, and wins a hit test. ---- */
class OrderedHashtable {
  constructor() { this.map = new Map(); }
  get(key) { return this.map.has(key) ? this.map.get(key) : null; }
  put(key, value) {
    const prev = this.get(key);
    this.map.delete(key);          /* a re-put moves the key to the top */
    this.map.set(key, value);
    return prev;
  }
  remove(key) { const prev = this.get(key); this.map.delete(key); return prev; }
  keys() { return this.map.keys(); }
  elements() { return Array.from(this.map.values()); }
  putTop(key) {
    if (!this.map.has(key)) return;
    const v = this.map.get(key);
    this.map.delete(key);
    this.map.set(key, v);
  }
}

/* ---- graph/GraphView.java: the panel. Model walking, placement, painting
 * and the mouse, as in 1999. ---- */
class GraphView {
  constructor(applet, graphModel, canvas) {
    this.applet = applet;
    this.graphModel = graphModel;
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d');
    this.visitedNodes = [];
    this.nodes = new OrderedHashtable();
    this.motor = new Motor(this);
    this.centralNode = null;
    this.longLabeledNodeView = null;
    /* the node budget of one view, and the optional hop cap (0 = none) */
    this.limit = parseInt(applet.getParameter('limit') || '100', 10);
    this.depth = parseInt(applet.getParameter('depth') || '0', 10);
    this.CHILD_EDGE_LENGTH = 120;
    this.ELLIPSOIDALITY = 2;
    this.PARENT_EDGE_LENGTH = 120;
    this.SECTOR_FOR_NEIGHBOURS = 120;
    this.math = AngleMath;
    this.state = GraphView.IDLE;
    this.draggedNode = null;
    this.prevClick = Date.now();
    this.deltaBetweenClicks = 300;
    this.animation = new Animation(this, applet);
    this.cssWidth = 1; this.cssHeight = 1;
    /* the world is the canvas divided by the zoom: nodes keep 1999 geometry
     * and the view shrinks to hold a deep neighbourhood (doc/PORT.md) */
    this.scale = 1;
    this.fitOnExpand = applet.getParameter('fit') !== 'false';
    this.fanoutMax = parseInt(applet.getParameter('fanout_max') || '0', 10);   /* 0: the budget alone bounds it */
    this.stubsMax = parseInt(applet.getParameter('stubs_max') || '24', 10);
    this.terminalCrawl = applet.getParameter('terminal_crawl') !== 'false';
    /* pixels of arc per neighbour: the circle grows with the count; 0 keeps
     * the fixed 120 px of 1999 */
    this.spread = parseInt(applet.getParameter('spread') || '40', 10);
    this.panStart = null;
    this.expandStatus = '';
    this.fit();
    this.bindMouse();
    if (typeof ResizeObserver !== 'undefined') new ResizeObserver(() => { this.fit(); this.repaint(); }).observe(canvas);
    else window.addEventListener('resize', () => { this.fit(); this.repaint(); });
  }
  /* the canvas backing store follows its CSS box and the device pixel ratio */
  fit() {
    const r = this.canvas.getBoundingClientRect();
    const dpr = window.devicePixelRatio || 1;
    this.cssWidth = Math.max(1, Math.round(r.width));
    this.cssHeight = Math.max(1, Math.round(r.height));
    const w = Math.round(this.cssWidth * dpr), h = Math.round(this.cssHeight * dpr);
    if (this.canvas.width !== w || this.canvas.height !== h) { this.canvas.width = w; this.canvas.height = h; }
    this.dpr = dpr;
  }
  size() { return { width: Math.trunc(this.cssWidth / this.scale), height: Math.trunc(this.cssHeight / this.scale) }; }
  bounds() { return this.size(); }
  /* zoom about the centre: the world box changes, the nodes follow its centre */
  setZoom(s) {
    s = Math.max(0.05, Math.min(4, s));
    const oc = this.getCenter();
    this.scale = s;
    const nc = this.getCenter();
    this.translate(nc.x - oc.x, nc.y - oc.y);
    for (const n of this.nodes.elements()) n.resetWrappedLabel();
    this.repaint();
  }
  /* the box around every node, with room for stubs and labels */
  shownBox() {
    let x1 = Infinity, y1 = Infinity, x2 = -Infinity, y2 = -Infinity;
    for (const n of this.nodes.elements()) {
      x1 = Math.min(x1, n.x - 50); x2 = Math.max(x2, n.x + n.width + 50);
      y1 = Math.min(y1, n.y - 45); y2 = Math.max(y2, n.y + n.height + 45);
    }
    return { x1: x1, y1: y1, x2: x2, y2: y2 };
  }
  /* zoom out until the shown box fits the canvas, never past 1, then centre
   * the box rather than the central node: an expansion to one side would
   * otherwise leave the other side empty */
  fitView() {
    if (this.nodes.elements().length === 0) return;
    let b = this.shownBox();
    const s = Math.min(1, this.cssWidth / (b.x2 - b.x1), this.cssHeight / (b.y2 - b.y1));
    if (Math.abs(s - this.scale) > 0.01) this.setZoom(s);
    b = this.shownBox();
    const cc = this.getCenter();
    this.translate(Math.trunc(cc.x - (b.x1 + b.x2) / 2), Math.trunc(cc.y - (b.y1 + b.y2) / 2));
  }
  /* what the expansion left off screen, for the status line */
  countHidden() {
    let hidden = 0;
    for (const n of this.nodes.elements()) {
      for (const id of n.nodeModel.children) if (this.getNode(id) == null) hidden++;
      for (const id of n.nodeModel.parents) if (this.getNode(id) == null) hidden++;
    }
    return hidden;
  }
  setInitNode(node) {
    this.centralNode = this.createNode(node);
    this.centralNode.setCenter(this.getCenterX(), this.getCenterY());
    this.addNode(node.getID(), this.centralNode);
    this.centralNode.setHighlighted(true);
    this.setState(GraphView.IDLE);
    this.applet.showUrl(node);
  }
  /* make a node the central node */
  selectNode(node) {
    this.motor.resumeThread();
    const nodeView = this.getNode(node.id);
    if (nodeView == null) {                     /* absent in the view */
      this.removeAll();
      this.centralNode = this.createNode(node);
      this.centralNode.setCenter(this.getCenterX(), this.getCenterY());
      this.addNode(node.getID(), this.centralNode);
      this.centralNode.setHighlighted(true);
      this.setState(GraphView.IDLE);
      this.applet.resetGraphModel(node.id, this.depth, this.limit);
    } else if (nodeView.isTerminal() && !this.terminalCrawl) {   /* 1999: terminals only show their URL */
      this.deselectAll();
      nodeView.setHighlighted(true);
    } else {
      this.moveToCenter(nodeView);
    }
  }
  moveToCenter(nodeView) {
    this.centralNode = nodeView;
    this.removeAllButCentral();
    this.setState(GraphView.ADVANCING);
    nodeView.addArriveEventListener(this);
    const v = nodeView.getVelocity(this.bounds().width >> 1, this.bounds().height >> 1, 30);
    this.setV(v);
    this.applet.resetGraphModel(nodeView.getNodeModel().getID(), this.depth, this.limit);
  }
  createNode(nodeModel) { return new NodeView(this, nodeModel); }
  expandNode(nodeView) {
    if (nodeView == null) { console.log('Trying to expand null!'); return; }
    this.removeAllButCentral();
    nodeView.expanded = false;
    nodeView.fromAngle = 0;
    this.expand(nodeView);                      /* breadth first from the model */
    this.addVisitedNode(nodeView.nodeModel);
    this.centralNode.visited = true;
    this.deselectAll();                         /* hack to walk around a deselection bug */
    nodeView.setHighlighted(true);
    this.motor.suspendThread();
    this.settle();
  }
  /* after any change of the set: colours for the ends, sizes, fit, status */
  settle() {
    const hidden = this.markEnds();
    if (this.centralNode != null) this.centralNode.setHighlighted(true);
    this.paint();                               /* sizes every new node from its label, moves none */
    if (this.fitOnExpand) this.fitView();
    this.repaint();
    const shown = this.nodes.elements().length;
    const more = this.applet.lastCut || this.cutByLimit;
    this.expandStatus = shown + ' node' + (shown === 1 ? '' : 's') + ' shown' +
      (hidden ? ', ' + hidden + ' neighbour' + (hidden === 1 ? '' : 's') + ' beyond' : '') +
      (more ? ' (the budget of ' + this.limit + '; blue nodes hold more)' : hidden ? '' : ': the whole reachable graph');
    this.applet.showStatus(this.expandStatus);
  }
  expandCentral() { this.expandNode(this.centralNode); }
  doubleClickOnNode(mod, nodeView) {
    const nodeModel = nodeView.getNodeModel();
    if (mod && (mod.alt || mod.ctrl)) {         /* open it where it stands */
      this.expandInPlace(nodeView);
      return;
    }
    this.deselectAll();
    nodeView.setHighlighted(true);
    if (!nodeView.isTerminal() || this.terminalCrawl) this.selectNode(nodeModel);
    else this.motor.suspendThread();
    if (!(mod && mod.shift)) this.applet.showUrl(nodeModel);
  }
  /* fetch a node's own neighbourhood and add it to the view around the node,
   * the centre unchanged: the picture grows outward by one click's cost */
  expandInPlace(nodeView) {
    const id = nodeView.nodeModel.id;
    this.motor.resumeThread();
    this.applet.fetchModel(id, 2, this.limit).then((model) => {
      if (model == null) { this.motor.suspendThread(); return; }   /* fetchModel said why */
      const mine = this.applet.getGraphModel();
      for (const n of model.map.values()) if (mine.getNode(n.id) == null) mine.addNode(n.id, n);
      const fresh = mine.getNode(id);
      if (fresh != null && fresh !== nodeView.nodeModel) nodeView.setNodeModel(fresh);
      if (nodeView.level == null) nodeView.level = 2;
      this.placeAround(nodeView, nodeView.level, Infinity);
      this.addVisitedNode(nodeView.nodeModel);
      nodeView.visited = true;
      this.motor.suspendThread();
      this.settle();
    });
  }
  getDepth() { return this.depth; }
  getLimit() { return this.limit; }
  /* a new budget or hop cap: ask the server again for the central node */
  setDepth(depth) { this.depth = depth; this.refetch(); }
  setLimit(limit) { this.limit = limit; this.refetch(); }
  refetch() {
    if (this.centralNode == null) return;
    this.removeAllButCentral();
    this.applet.resetGraphModel(this.centralNode.nodeModel.id, this.depth, this.limit);
  }
  /* green when every neighbour is on screen, blue when crawling here shows
   * more: the unexpanded ends of the walk. Returns the total left off screen,
   * which is the count the status line wants, so settle walks the lists once */
  markEnds() {
    let total = 0;
    for (const n of this.nodes.elements()) {
      let hidden = 0;
      for (const id of n.nodeModel.children) if (this.getNode(id) == null) hidden++;
      for (const id of n.nodeModel.parents) if (this.getNode(id) == null) hidden++;
      n.setTerminal(hidden === 0);
      total += hidden;
    }
    return total;
  }
  removeAllButCentral() {
    this.removeAll();
    this.addNode(this.centralNode.nodeModel.id, this.centralNode);
  }
  getCentralNode() { return this.centralNode; }
  getState() { return this.state; }
  setState(s) { this.state = s; }
  deselectAll() { for (const n of this.nodes.elements()) n.setHighlighted(false); }
  translate(x, y) { for (const n of this.nodes.elements()) { n.x += x; n.y += y; } }
  /* the topmost node view containing the point */
  getContains(x, y) {
    let lastNode = null;
    for (const node of this.nodes.elements()) if (node.inside(x, y)) lastNode = node;
    return lastNode;
  }
  getNode(key) { return this.nodes.get(key); }
  addNode(key, node) { return this.nodes.put(key, node); }
  removeNode(key) { return this.nodes.remove(key); }
  isVisited(node) { return this.visitedNodes.indexOf(node.nodeModel.id) >= 0; }
  addVisitedNode(node) { if (this.visitedNodes.indexOf(node.id) < 0) this.visitedNodes.push(node.id); }
  removeAll() { this.nodes = new OrderedHashtable(); }
  setV(vx, vy) { for (const n of this.nodes.elements()) n.setV(vx, vy); }
  /* build the view from the model breadth first, beginning at the node
   * passed: each node places the neighbours the model holds, parents first
   * as in 1999, on its circle or sector, until the budget is spent or the
   * hop cap reached. A neighbour the model lacks is a hidden stub. */
  expand(root) {
    const queue = [root];
    root.level = 1;
    root.expanded = true;
    let placed = 1;
    this.cutByLimit = false;
    while (queue.length > 0) {
      const nodeView = queue.shift();
      if (this.depth > 0 && nodeView.level >= this.depth) continue;
      const made = this.placeAround(nodeView, nodeView.level, this.limit - placed);
      placed += made.length;
      for (const v of made) queue.push(v);
      /* the budget is spent: placeAround would return nothing from here on */
      if (placed >= this.limit) { this.cutByLimit = true; break; }
    }
    /* it only cut the view if something was actually left off screen; one walk
     * of the lists, not one per node still queued */
    if (this.cutByLimit && this.countHidden() === 0) this.cutByLimit = false;
  }
  /* place the neighbours of NODEVIEW that the model holds and the view does
   * not, at most BUDGET of them, with the 1999 angles; returns them */
  placeAround(nodeView, level, budget) {
    const model = this.applet.getGraphModel(), nodeModel = nodeView.getNodeModel();
    const toPlace = [], made = [];
    for (const id of nodeModel.getParentsIds())
      if (this.getNode(id) == null && model.getNode(id) != null) toPlace.push({ id: id, parent: true });
    for (const id of nodeModel.getChildrenIds())
      if (this.getNode(id) == null && model.getNode(id) != null) toPlace.push({ id: id, parent: false });
    if (toPlace.length === 0 || budget <= 0) return made;
    /* fanout_max, when set, caps one node's ring; fewer the deeper the level */
    const most = this.fanoutMax > 0
      ? (level === 1 ? this.fanoutMax : Math.max(6, Math.trunc(this.fanoutMax / (level * 2)))) : toPlace.length;
    const count = Math.min(toPlace.length, most, budget);
    const sector = level === 1 ? 360 : this.SECTOR_FOR_NEIGHBOURS;
    const angleBetween = Math.trunc(sector / count);
    /* the radius that gives each neighbour `spread` pixels of arc */
    let arc = Math.trunc(count * this.spread * 360 / (sector * 2 * Math.PI));
    if (level > 1) arc = Math.min(arc, 3 * this.CHILD_EDGE_LENGTH);    /* a sector, not a ring */
    for (let i = 0; i < count; i++) {
      const t = toPlace[i];
      if (this.getNode(t.id) != null) continue;
      const length = Math.max(t.parent ? this.PARENT_EDGE_LENGTH : this.CHILD_EDGE_LENGTH, arc);
      const view = this.createNeighbour(nodeView, t.id, i, angleBetween, length);
      view.level = level + 1;
      view.expanded = true;
      made.push(view);
    }
    return made;
  }
  /* a neighbour placed on the sector around its parent node view */
  createNeighbour(parentNodeView, aChildId, i, angleBetween, edgeLength) {
    const math = this.math;
    let aNodeFromModel = this.applet.getGraphModel().getNode(aChildId);
    if (aNodeFromModel == null)                 /* a dummy node */
      aNodeFromModel = new Node(aChildId, 'label id=' + aChildId, '', null, [], []);
    const angle = angleBetween * i + parentNodeView.fromAngle;
    const dx = math.sin(angle);
    const centerX = parentNodeView.getMiddleX() + Math.trunc(edgeLength * dx);
    const centerY = parentNodeView.getMiddleY() - Math.trunc(edgeLength * math.cos(angle));
    const aChild = this.createNode(aNodeFromModel);
    aChild.setCenter(centerX, centerY);
    if (this.isVisited(aChild)) aChild.visited = true;
    aChild.fromAngle = angleBetween * i + parentNodeView.fromAngle - (this.SECTOR_FOR_NEIGHBOURS >> 1);
    this.addNode(aChildId, aChild);
    return aChild;
  }
  getGraphLeftBound() { return 1; }
  getGraphRightBound() { return this.size().width - 2; }
  getGraphTopBound() { return 1; }
  getGraphBottomBound() { return this.size().height - 2; }
  getCenterX() { return this.size().width >> 1; }
  getCenterY() { return this.size().height >> 1; }
  getCenter() { return { x: this.getCenterX(), y: this.getCenterY() }; }
  updatePositionDuringDrag(x, y, forAll) {
    const d = this.draggedNode;
    const leftBound = this.getGraphLeftBound(), rightBound = this.getGraphRightBound() - d.width;
    const topBound = this.getGraphTopBound(), bottomBound = this.getGraphBottomBound() - d.height;
    if (forAll) return;
    d.x += x - d.oldx;
    d.y += y - d.oldy;
    d.oldx = x; d.oldy = y;
    if (d.x < leftBound) { d.x = leftBound; d.oldx = leftBound; }
    else if (d.x > rightBound) { d.x = rightBound; d.oldx = rightBound; }
    if (d.y < topBound) { d.y = topBound; d.oldy = topBound; }
    else if (d.y > bottomBound) { d.y = bottomBound; d.oldy = bottomBound; }
  }
  /* Component.repaint(): AWT ran update(g) on its own thread, so the motor's
   * tick and a mouse move each move the nodes one step and paint. Synchronous
   * on purpose: requestAnimationFrame stops in a hidden tab and in a headless
   * capture, and the glide with it. */
  repaint() { this.update(); }
  /* update(Graphics): move every node, then paint */
  update() {
    for (const node of this.nodes.elements()) node.update();
    this.paint();
  }
  paint() {
    const ctx = this.ctx, w = this.size().width, h = this.size().height;
    ctx.setTransform(this.dpr * this.scale, 0, 0, this.dpr * this.scale, 0, 0);
    const g = new Graphics(ctx, this.scale);
    g.setColor(Theme.background);
    g.fillRect(0, 0, w, h);
    g.setColor(Theme.edge);
    if (this.applet.isWaitingForServer()) this.animation.paint(g, w, h);
    this.paintEdges(g);
    this.paintNodes(g);
  }
  /* edges of a node to its parents and children in the view, and dummy edges
   * with bulbs for those not in it */
  paintEdgesForNode(nodeView, g) {
    const math = this.math, nodeModel = nodeView.nodeModel;
    let numberOfHidden = 0;
    for (const aParentId of nodeModel.parents.slice()) {
      const parent = this.getNode(aParentId);
      if (parent == null) numberOfHidden++;
      else new Edge(nodeView, parent).paint(g);
    }
    for (const aChildId of nodeModel.children.slice()) {
      const child = this.getNode(aChildId);
      if (child == null) numberOfHidden++;
      else {
        if (child.visited) g.setColor(Theme.edgeVisited);
        g.drawLine(nodeView.getMiddleX(), nodeView.getMiddleY(), child.getMiddleX(), child.getMiddleY());
        if (child.visited) g.setColor(Theme.edge);
      }
    }
    if (numberOfHidden === 0) return;
    const edgeLength = 35;
    let angleRange = 90;
    let xDistance = nodeView.getMiddleX() - this.getCenterX();
    let yDistance = nodeView.getMiddleY() - this.getCenterY();
    if (xDistance === 0) xDistance = 1;
    if (yDistance === 0) yDistance = 1;
    let thisNodeAngle = 0;
    if (!nodeView.equals(this.centralNode)) thisNodeAngle = Math.trunc(math.atan2(yDistance, xDistance)) + 90 - (angleRange >> 1);
    else angleRange = 360;
    const fromAngle = thisNodeAngle;
    const stubs = Math.min(numberOfHidden, this.stubsMax);     /* the rest is a count */
    const angleBetween = Math.trunc(angleRange / stubs);
    let lastX = 0, lastY = 0;
    for (let i = 0; i < stubs; i++) {
      const angle = angleBetween * i + fromAngle;
      const dx = math.sin(angle);
      const nonExistingX = nodeView.getMiddleX() + Math.trunc((nodeView.width >> 1) * dx) + Math.trunc(edgeLength * dx);
      const nonExistingY = nodeView.getMiddleY() - Math.trunc(edgeLength * math.cos(angle));
      g.drawLine(nodeView.getMiddleX(), nodeView.getMiddleY(), nonExistingX, nonExistingY);
      g.fillOval(nonExistingX - 2, nonExistingY - 2, 4, 4);      /* bulbs at the ends */
      lastX = nonExistingX; lastY = nonExistingY;
    }
    if (numberOfHidden > stubs) g.drawString('+' + (numberOfHidden - stubs), lastX + 4, lastY + 4);
  }
  paintEdges(g) { for (const n of this.nodes.elements()) this.paintEdgesForNode(n, g); }
  paintNodes(g) { for (const n of this.nodes.elements()) n.paint(g); }
  /* the mouse: Event.mouseDown / mouseDrag / mouseMove / mouseUp of 1999 */
  bindMouse() {
    const c = this.canvas;
    const at = (e) => {
      const r = c.getBoundingClientRect();
      return { x: Math.trunc((e.clientX - r.left) / this.scale), y: Math.trunc((e.clientY - r.top) / this.scale) };
    };
    c.addEventListener('wheel', (e) => {
      e.preventDefault();
      this.setZoom(this.scale * (e.deltaY < 0 ? 1.15 : 1 / 1.15));
    }, { passive: false });
    c.addEventListener('pointerdown', (e) => {
      if (e.button !== 0) return;
      e.preventDefault();
      c.setPointerCapture(e.pointerId);
      const p = at(e);
      this.mouseDown({ shift: e.shiftKey, alt: e.altKey, ctrl: e.ctrlKey || e.metaKey }, p.x, p.y);
    });
    c.addEventListener('pointermove', (e) => {
      const p = at(e);
      if (this.getState() === GraphView.PAN && (e.buttons & 1)) {
        this.translate(p.x - this.panStart.x, p.y - this.panStart.y);
        this.panStart = p;
        this.repaint();
      } else if (this.getState() === GraphView.DRAG && (e.buttons & 1)) this.mouseDrag(e.shiftKey, p.x, p.y);
      else this.mouseMove(e.shiftKey, p.x, p.y);
    });
    c.addEventListener('pointerup', (e) => { const p = at(e); this.mouseUp(e.shiftKey, p.x, p.y); });
    c.addEventListener('pointercancel', (e) => { const p = at(e); this.mouseUp(e.shiftKey, p.x, p.y); });
  }
  mouseDown(mod, x, y) {
    if (typeof mod !== 'object' || mod == null) mod = { shift: !!mod };
    this.motor.resumeThread();
    const nodeView = this.getContains(x, y);
    if (nodeView != null) {
      this.nodes.putTop(nodeView.nodeModel.id);
      if (this.longLabeledNodeView != null) {
        this.longLabeledNodeView.setLongLabel(false);
        this.longLabeledNodeView = null;
      }
      const click = Date.now();
      if (click - this.prevClick < this.deltaBetweenClicks) {
        this.doubleClickOnNode(mod, nodeView);
      } else {
        this.prevClick = click;
        nodeView.oldx = x; nodeView.oldy = y;
        this.draggedNode = nodeView;
        this.setState(GraphView.DRAG);
      }
    } else if (this.getState() === GraphView.IDLE) {   /* empty space: pan the view */
      this.panStart = { x: x, y: y };
      this.setState(GraphView.PAN);
    }
    return true;
  }
  mouseDrag(shift, x, y) {
    if (this.getState() === GraphView.DRAG && this.draggedNode != null) {
      this.nodes.putTop(this.draggedNode.nodeModel.id);
      this.updatePositionDuringDrag(x, y, shift);
      this.draggedNode.resetWrappedLabel();
    }
    return true;
  }
  mouseMove(shift, x, y) {
    if (this.getState() === GraphView.DRAG) return true;
    const nodeView = this.getContains(x, y);
    if (nodeView != null) {
      this.nodes.putTop(nodeView.nodeModel.id);
      if (nodeView !== this.longLabeledNodeView) {
        if (this.longLabeledNodeView != null) this.longLabeledNodeView.setLongLabel(false);
        this.longLabeledNodeView = nodeView;
        this.longLabeledNodeView.setLongLabel(true);
        const m = nodeView.nodeModel;
        this.applet.showStatus(m.id + ': ' + m.label + ' (' + m.children.length + ' children, ' + m.parents.length + ' parents)');
        this.repaint();
      }
    } else if (this.longLabeledNodeView != null) {
      this.longLabeledNodeView.setLongLabel(false);
      this.longLabeledNodeView = null;
      this.applet.showStatus(this.expandStatus);
      this.repaint();
    }
    return true;
  }
  mouseUp(shift, x, y) {
    if (this.getState() === GraphView.PAN) {
      this.panStart = null;
      this.state = GraphView.IDLE;
      return true;
    }
    if (this.getState() === GraphView.DRAG) {
      this.draggedNode = null;
      this.state = GraphView.IDLE;
      this.motor.suspendThread();
    }
    return true;
  }
  /* ArriveEventListener: the gliding node has reached the centre */
  onArrive(e) {
    const node = e.getSource();
    if (node != null) {
      this.setV(0, 0);
      node.removeArriveEventListener(this);
      node.setStopBounds(null);
      node.resetWrappedLabel();
    }
    this.setState(GraphView.IDLE);
  }
  getMotor() { return this.motor; }
}
GraphView.IDLE = 1;
GraphView.DRAG = 2;
GraphView.ADVANCING = 3;
GraphView.PAN = 4;

/* ---- graph/ToolsPanel.java: Tree Depth and History choices ---- */
class ToolsPanel {
  constructor(applet, depthChoice, historyChoice, root) {
    this.applet = applet;
    this.history = new History();
    this.depth_choice = depthChoice;
    this.history_choice = historyChoice;
    const depthMax = parseInt(applet.getParameter('depth_max') || '8', 10);
    depthChoice.textContent = '';
    for (let d = 0; d <= depthMax; d++) {
      const o = document.createElement('option');
      o.value = String(d);
      o.textContent = d === 0 ? 'any' : String(d);
      if (d === applet.getGraphView().getDepth()) o.selected = true;
      depthChoice.appendChild(o);
    }
    depthChoice.addEventListener('change', () => {
      const depth = parseInt(depthChoice.value, 10);
      if (!isNaN(depth)) applet.getGraphView().setDepth(depth);
    });
    const limitChoice = root.querySelector('#limit');
    if (limitChoice) {
      const steps = [10, 25, 50, 100, 200, 500, 1000, 2000, 5000];
      const cur = applet.getGraphView().getLimit();
      if (steps.indexOf(cur) < 0) { steps.push(cur); steps.sort((a, b) => a - b); }
      limitChoice.textContent = '';
      for (const n of steps) {
        const o = document.createElement('option');
        o.textContent = String(n);
        if (n === cur) o.selected = true;
        limitChoice.appendChild(o);
      }
      limitChoice.addEventListener('change', () => {
        const n = parseInt(limitChoice.value, 10);
        if (!isNaN(n)) applet.getGraphView().setLimit(n);
      });
    }
    const goto = root.querySelector('#goto'), find = root.querySelector('#find');
    const back = root.querySelector('#back'), results = root.querySelector('#results');
    this.results = results;
    if (goto) goto.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && goto.value.trim() !== '') applet.goTo(goto.value.trim());
    });
    if (find) find.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') applet.find(find.value.trim());
    });
    if (back) back.addEventListener('click', () => history.back());
    historyChoice.addEventListener('change', () => {
      const itemInChoice = historyChoice.value.trim();
      for (const key of this.history.keys()) {
        const node = this.history.get(key);
        if (itemInChoice === node.getLabel().trim()) {
          applet.getGraphView().selectNode(node);
          applet.showUrl(node);
        }
      }
    });
  }
  /* the label search's answers: one link each, to crawl to */
  showResults(nodes) {
    if (!this.results) return;
    this.results.textContent = '';
    if (nodes.length === 0) {
      const li = document.createElement('li');
      li.textContent = 'nothing found';
      this.results.appendChild(li);
    }
    for (const n of nodes) {
      const li = document.createElement('li');
      const a = document.createElement('a');
      a.href = '#' + encodeURIComponent(n.id);
      a.textContent = n.label === '' ? n.id : n.label + ' (' + n.id + ')';
      li.appendChild(a);
      this.results.appendChild(li);
    }
    this.results.hidden = false;
  }
  addHistoryNode(node) {
    if (this.history.getNode(node.getID()) != null) return;
    const o = document.createElement('option');
    o.textContent = node.getLabel();
    this.history_choice.appendChild(o);
    this.history_choice.value = node.getLabel();
    this.history.addNode(node.getID(), node);
  }
}

/* ---- NavigatorApplet.java ---- */
class NavigatorApplet {
  /* params: the <param> values of 1999, as an object; root: the element
   * holding #graph, #depth, #history, #status and the "content" frame */
  constructor(params, root) {
    this.params = params;
    this.root = root;
    this.fromAppletParameters = false;
    this.graphModel = null;
    this.graphView = null;
    this.waitingForServer = false;
    this.staticApplet = false;
    this.debug = params.debug === 'true';
    this.toolsPanel = null;
    this.communicationThread = null;
    this.initialized = false;
    this.images = new Map();
    this.lastCut = 0;
  }
  getParameter(name) {
    const v = this.params[name];
    return v == null ? null : String(v);
  }
  /* Applet.getImage + MediaTracker: resolved when loaded; null on failure */
  getImage(name) {
    if (this.images.has(name)) return this.images.get(name);
    const p = new Promise((resolve) => {
      const img = new Image();
      img.onload = () => resolve(img);
      img.onerror = () => { console.log('cannot load ' + name); resolve(null); };
      img.src = name;
    });
    this.images.set(name, p);
    return p;
  }
  showStatus(s) {
    const el = this.root.querySelector('#status');
    if (el) el.textContent = s;
  }
  async init() {
    this.graphModel = new GraphModel();
    this.graphView = new GraphView(this, this.graphModel, this.root.querySelector('#graph'));
    this.toolsPanel = new ToolsPanel(this, this.root.querySelector('#depth'), this.root.querySelector('#history'), this.root);
    /* back and forward walk the crawl: the hash is the node */
    window.addEventListener('popstate', () => {
      const id = location.hash.length > 1 ? decodeURIComponent(location.hash.slice(1)) : null;
      const c = this.graphView.getCentralNode();
      if (id != null && (c == null || c.nodeModel.id !== id)) this.goTo(id);
    });
    if (this.getParameter('line_0') != null) this.fromAppletParameters = true;
    const categories = new Categories(this);
    await categories.loadImages();
    await this.graphView.animation.loadImages();
    const s = this.getParameter('static_version');
    if (s != null && (s === 'true' || s === 'yes')) this.staticApplet = true;
    const keywordid = this.getParameter('first_node');
    if (keywordid == null) console.log('first_node is null');
    this.resetGraphModel(keywordid, this.graphView.getDepth(), this.graphView.getLimit());
  }
  start() { this.graphView.getMotor().start(); }
  stop() { this.graphView.getMotor().stop(); }
  setGraphModel(m) { this.graphModel = m; }
  /* refill the cache from the server (or the parameters) */
  resetGraphModel(keywordid, depth, limit) {
    if (this.staticApplet && this.graphModel != null && this.initialized) return;
    if (keywordid != null && location.hash.slice(1) !== encodeURIComponent(keywordid))
      history.pushState(null, '', '#' + encodeURIComponent(keywordid));
    this.setWaitingForServer(true);
    if (this.communicationThread != null) this.communicationThread.stop();
    this.communicationThread = new InfoRequest(this, keywordid, depth, limit || 0, this.fromAppletParameters);
    this.communicationThread.setDebug(this.debug);
    this.communicationThread.start();
  }
  /* callback from the request when the data has arrived */
  dataReady(firstNodeId) {
    this.setWaitingForServer(false);
    if (this.debug) console.log(this.graphModel.toString());
    if (!this.initialized) {
      const firstNode = this.graphModel.getNode(firstNodeId);
      if (firstNode == null) {
        console.log('The first node with id=' + firstNodeId + ' is absent in data!');
        this.showStatus('The first node with id=' + firstNodeId + ' is absent in data');
        return;
      }
      this.graphView.setInitNode(firstNode);
      this.initialized = true;
    } else {
      /* the centre may be a stub made from an id: take the node the server sent */
      const c = this.graphView.getCentralNode();
      const fresh = this.graphModel.getNode(c.nodeModel.id);
      if (fresh != null) { if (fresh !== c.nodeModel) c.setNodeModel(fresh); }
      else this.showStatus('no node ' + c.nodeModel.id + ' in the graph');
    }
    const wait = () => {
      if (this.graphView.getState() === GraphView.ADVANCING) setTimeout(wait, 100);
      else this.graphView.expandCentral();
    };
    wait();
  }
  /* the answer never came: keep the view that is up and say so, rather than
   * re-expanding the model of the previous node as though nothing happened */
  requestFailed(e) {
    this.setWaitingForServer(false);
    this.showStatus('the server did not answer: ' + e);
  }
  isWaitingForServer() { return this.waitingForServer; }
  setWaitingForServer(w) {
    this.waitingForServer = w;
    this.showStatus(w ? 'Getting information from the server' : '');
  }
  getGraphView() { return this.graphView; }
  getGraphModel() { return this.graphModel; }
  /* a neighbourhood on its own, for expanding in place: the model of the
   * server's answer (or of the line_N parameters), the view untouched */
  async fetchModel(id, depth, limit) {
    const parser = new Parser();
    if (this.fromAppletParameters) {
      for (let i = 0; this.getParameter('line_' + i) != null; i++) parser.parseLine(this.getParameter('line_' + i));
      return parser.getGraphModel();
    }
    const cgi = this.getParameter('cgi');
    let url = cgi + (cgi.indexOf('?') < 0 ? '?' : '&') + 'keywordid=' + encodeURIComponent(id);
    if (depth > 0) url += '&depth=' + depth;
    if (limit > 0) url += '&limit=' + limit;
    this.showStatus('Getting information from the server');
    try {
      const r = await fetch(url, { cache: 'no-store' });
      for (const line of (await r.text()).split('\n')) if (line.length > 0) parser.parseLine(line.replace(/\r$/, ''));
    } catch (e) { this.showStatus('the server did not answer: ' + e); return null; }
    return parser.getGraphModel();
  }
  /* crawl to an id typed, searched or taken from the URL */
  goTo(id) {
    if (!this.initialized) { this.params.first_node = id; return; }
    const node = this.graphModel.getNode(id) || new Node(id, 'id ' + id, '', '', [], []);
    this.graphView.selectNode(node);
  }
  /* label search: /api/find on a server, the line_N parameters without one */
  async find(text) {
    if (text === '') { if (this.toolsPanel.results) this.toolsPanel.results.hidden = true; return; }
    const parser = new Parser();
    if (this.fromAppletParameters) {
      for (let i = 0; this.getParameter('line_' + i) != null; i++) parser.parseLine(this.getParameter('line_' + i));
    } else {
      try {
        const r = await fetch('/api/find?q=' + encodeURIComponent(text), { cache: 'no-store' });
        for (const line of (await r.text()).split('\n')) if (line.length > 0) parser.parseLine(line.replace(/\r$/, ''));
      } catch (e) { this.showStatus('find failed: ' + e); return; }
    }
    const lower = text.toLowerCase(), found = [];
    for (const n of parser.getGraphModel().map.values())
      if (n.label.toLowerCase().indexOf(lower) >= 0) found.push(n);
    this.toolsPanel.showResults(found);
  }
  /* The url field resolved, when it is a document the frame may open. A graph
   * file is third-party data (an edge list, a crawl, apt's output), and the
   * frame shares this page's origin, so a "javascript:" or "data:" url would
   * run as the page itself. http and https only; null refuses. */
  static safeUrl(url) {
    let u;
    try { u = new URL(url, location.href); } catch (e) { return null; }
    return u.protocol === 'http:' || u.protocol === 'https:' ? u.href : null;
  }
  /* show the node's URL in the "content" frame and put it into the history */
  showUrl(node) {
    this.toolsPanel.addHistoryNode(node);
    const frame = this.root.querySelector('iframe[name=content]');
    if (frame == null) return;
    let url = node.getUrl();
    if (url == null) { console.log('Trying to connect with null URL'); return; }
    url = url.trim();
    const safe = url === '' ? '' : NavigatorApplet.safeUrl(url);
    if (safe === null) this.showStatus('node ' + node.getID() + ': refused a url that is not http or https');
    if (safe === '' || safe === null) {
      /* no document, or none this frame may open: show the record itself
       * (a deviation, doc/PORT.md) */
      frame.removeAttribute('src');
      frame.srcdoc = '<pre style="font:13px monospace;white-space:pre-wrap">' +
        NavigatorApplet.escape(node.toString().split(';').join(';\n')) + '</pre>';
      return;
    }
    this.showStatus(safe + ' connecting...');
    frame.removeAttribute('srcdoc');
    frame.src = safe;
    this.showStatus('');
  }
  static escape(s) {
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  }
}

/* the page: parameters from GRAPHCRAWL_PARAMS, the first node from #id in
 * the URL or from the server's /api/info */
window.addEventListener('DOMContentLoaded', async () => {
  const params = window.GRAPHCRAWL_PARAMS || {};
  if (location.hash.length > 1) params.first_node = decodeURIComponent(location.hash.slice(1));
  const q = new URLSearchParams(location.search);      /* ?depth=4&limit=50&style=1999 override the page */
  for (const k of ['depth', 'limit', 'style']) if (q.get(k)) params[k] = q.get(k);
  Theme = Themes[params.style] || Themes.modern;
  Sprite.indent = Theme.pad;
  WrappedLabel.indent = Theme.pad;
  document.documentElement.dataset.style = Themes[params.style] ? params.style : 'modern';
  if (params.cgi != null && params.line_0 == null) {
    try {
      const t = await (await fetch('/api/info', { cache: 'no-store' })).text();
      const m = /^first=(.*)$/m.exec(t), f = /^file=(.*)$/m.exec(t);
      if (m && params.first_node == null) params.first_node = m[1].trim();
      if (f) document.title = 'graphcrawl: ' + f[1].trim().split('/').pop();
    } catch (e) { console.log('no /api/info: ' + e); }
  }
  if (typeof window.GRAPHCRAWL_BEFORE === 'function') await window.GRAPHCRAWL_BEFORE(params);   /* a page's own setup */
  const applet = new NavigatorApplet(params, document);
  window.graphcrawl = applet;
  await applet.init();
  applet.start();
});
