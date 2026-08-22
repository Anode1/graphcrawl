/**
 * @(#)Edge.java
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for NON-COMMERCIAL purposes
 * and without fee is hereby granted provided that this
 * copyright notice and author's name appears in all copies.
 * THE AUTHOR MAKE NO REPRESENTATIONS OR
 * WARRANTIES ABOUT THE SUITABILITY OF THE SOFTWARE, EITHER
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. THE AUTHOR
 * SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED
 * BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 */

package graph;

import java.awt.*;

/**
 * Class representing an edge of a graph. Currently used for parents edges painting only 
 *
 * @author Vasili Gavrilov
 * @version 1.0
 * @since   JDK1.0
 */
public class Edge{

  /**
   * Parent node
   */
  protected NodeView fromNode;

  /**
   * Child node
   */
  protected NodeView toNode;

  /**
   * Reference to the math library
   */
  protected static AngleMath math=AngleMath.getInstance();

  /**
   * Default constructor
   */
  public Edge(NodeView fromNode, NodeView toNode){

    this.fromNode=fromNode;
    this.toNode=toNode;
  }

  /**
   * Returns parent node
   */
  public NodeView getFromNode(){

    return fromNode;
  }

  /**
   * Returns child node
   */
  public NodeView getToNode(){
  
    return toNode;
  }

  /**
   * Overwrites Object's method. Edges considered to be equal if they have both
   * the same parent as well as child. So, parent/child pair uniqually define the edge
   */
  public boolean equals(Edge another){

    if(another==null)return false;
    return (another.getFromNode().equals(this.fromNode) && another.getToNode().equals(this.toNode));
  }

  /**
   * Paints this egde
   */
  public void paint(Graphics g){

    g.drawLine(fromNode.getMiddleX(), fromNode.getMiddleY(), toNode.getMiddleX(), toNode.getMiddleY());

    Color defaultColor=g.getColor();
    g.setColor(Color.gray);
    drawArrow(g, fromNode.getMiddleX(), fromNode.getMiddleY(), toNode.getMiddleX(), toNode.getMiddleY());
    g.setColor(defaultColor);
  }

  /**
   * Draws the arrow in the middle of the edge
   */
  protected void drawArrow(Graphics g, int x0, int y0, int x, int y){

    int arrowAngle=30;
    int arrowLength=20;

    int middlePointX=(x0+x)/2;
    int middlePointY=(y0+y)/2;

    int thisEdgeAngle=(int)math.atan2(y-y0,x-x0)+90;

    int leftSegmentAngle=thisEdgeAngle-arrowAngle/2;
    int firstArrowTailX=middlePointX+(int)(arrowLength*math.sin(leftSegmentAngle));
    int firstArrowTailY=middlePointY-(int)(arrowLength*math.cos(leftSegmentAngle));

    int rightSegmentAngle=thisEdgeAngle+arrowAngle/2;
    int secondArrowTailX=middlePointX+(int)(arrowLength*math.sin(rightSegmentAngle));
    int secondArrowTailY=middlePointY-(int)(arrowLength*math.cos(rightSegmentAngle));

    int[] polygonXArray={middlePointX,firstArrowTailX,secondArrowTailX};
    int[] polygonYArray={middlePointY,firstArrowTailY,secondArrowTailY};

    g.fillPolygon(polygonXArray,polygonYArray,3);

  }


}
