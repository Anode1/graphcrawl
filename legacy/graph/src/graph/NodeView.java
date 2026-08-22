/**
 * @(#)NodeView.java
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

import java.util.*;
import java.awt.*;
import java.applet.Applet;

/**
 * Class representing a node view (visible representation for a NodeModel)
 *
 * @author Vasili Gavrilov
 * @version 1.0
 * @since   JDK1.0
 */
public class NodeView extends Sprite{

    /**
     * Ref to math tables instance
     */
    protected static AngleMath math=AngleMath.getInstance();

    /**
     * Node model assotiated with this node view
     */
    public Node nodeModel;

    /**
     * Angle low boundary for this node placement
     */
    public int fromAngle=0;

    /**
     * Angle high boundary for this node placement
     */
    public int toAngle=360;//default

    /**
     * Label view
     */
    public WrappedLabel wrappedLabel;

    /**
     * shortened label (labelViewLength defines max width of it)
     */
    protected String shortLabel;

    /**
     * Maximal length of label when it has not been cut
     */
    protected static int labelViewLength=15;

    /**
     * True if long version of label has been shown
     */
    protected boolean showLongLabel;

    /**
     * Internal flag used not to expand this node several times
     */
    public boolean expanded;

    /**
     * Flag indicating that this node has been visited
     */
    public boolean visited;

    /**
     * Default color for usual nodes shapes when it is not an image
     */
    public static final Color DEFAULT_COLOR     = Color.cyan;

    /**
     * Default color for terminal nodes shapes when it is not an image
     */
    public static final Color TERMINAL_COLOR    = Color.green;

    /**
     * Default color for highlighted nodes shapes when it is not an image
     */
    public static final Color HIGHLIGHTED_COLOR = Color.yellow;

    /**
     * Default color for terminal nodes shapes when it is not an image
     */
    public Color notHighligtedColor = TERMINAL_COLOR;

    /**
     * Default font color for visited nodes
     */
    public static final Color VISITED_FONT_COLOR = Color.blue;

    /**
     * Default nodes labels font color
     */
    public static final Color DEFAULT_FONT_COLOR = Color.black;

    /**
     * Gap between image and label text
     */
    protected static final int gapBetweenLabelAndImage=2;

    /**
     * string height
     */
    public static int row_height=-1;

    /**
     * Flag that the wrapped label should be reset
     */
    protected boolean resetWrappedLabel=true;

    /**
     * The width of shortened label
     */
    public int shortLabelWidth=-1;

    /**
     * Label font descent
     */
    protected int descent=-1;

    protected FontMetrics fm;

    /**
     * Aux flag indicating that this component has been painted the first time
     */
    protected boolean firstTime=true;

    /**
     * True if this node has been highlighted
     */
    protected boolean highlighted;

    /**
     * True if this node has been highlighted
     */
    protected boolean terminal=true;

    /**
     * Default constructor
     */
    public NodeView(Panel panel, Node nodeModel){

        super(panel);
        this.nodeModel=nodeModel;

        //set cut label:
        String label=nodeModel.label;

        if(label.length() > this.labelViewLength){
           this.shortLabel=label.substring(0,labelViewLength-4)+"...";
        }
        else{
           this.shortLabel=label;
        }

        Categories categories=Categories.getInstance();

        Image iconImage=categories.getImageForCategory(nodeModel.category);

        if(iconImage==null){
            if(categories.isInOvalRange(nodeModel.id)){  //OVAL TYPE
               this.type=this.OVAL;
            }
        }
        else{   //image
            setImage(iconImage);
        }

    }

    /**
     * Sets long label
     */
    public void setLongLabel(boolean showLongLabel){

      this.showLongLabel=showLongLabel;
    }

    /**
     * Highlights this node
     */
    public void setHighlighted(boolean highlighted){

        this.highlighted=highlighted;
        if(highlighted){
           color=HIGHLIGHTED_COLOR;
        }
        else{
           color=notHighligtedColor;
        }
    }

    /**
     * Returns true if this node has been highlighted
     */
    public boolean isHighlighted(){

        return highlighted;
    }

    /**
     * Changes the default node color
     */
    public void setUsualColor(Color newColor){

        notHighligtedColor=newColor;
    }

    /**
     * Gets the default node color
     */
    public Color getUsualColor(){

        return notHighligtedColor;
    }

    /**
     * Overwrites superclass method
     */
    public boolean inside(int X, int Y){

        return super.inside(X, Y);
    }

    /**
     * Returns true if this node is a terminal node
     */
    public boolean isTerminal(){

        return terminal;
    }

    /**
     * Makes this node terminal
     */
    public void setTerminal(boolean terminal){

        this.terminal=terminal;
        if(terminal){
           notHighligtedColor=TERMINAL_COLOR;
        }
        else{
           notHighligtedColor=DEFAULT_COLOR;
        }
    }

    /**
     * Forces to reset wrapper label
     */
    public void resetWrappedLabel(){

        this.resetWrappedLabel=true;
    }

    /**
     * Adds arrive listener
     */
    public synchronized void addArriveEventListener(ArriveEventListener arriveListener){

        listeners.addElement(arriveListener) ;
    }

    /**
     * Removes arrive listener
     */
    public synchronized void removeArriveEventListener(ArriveEventListener arriveListener){

        listeners.removeElement(arriveListener) ;
    }

    /*
     * Notifies all arrive listeners (callback used in the case when
     * some information passed in ArriveEvent to listeners
     */
    public void notifyArriveEventListeners(ArriveEvent e){

        Vector l=null;
        synchronized(this){
            l = (Vector)listeners.clone();
        }
        for (int i=0 ; i < l.size() ; i++) { // deliver the event
            ((ArriveEventListener)l.elementAt(i)).onArrive(e);
        }
    }

    /**
     * Overwrites default object's method: nodes are the same if their
     * node models are the same
     */
    public boolean equals(NodeView nodeView){

        if(nodeView==null)return false;

        return this.nodeModel.equals(nodeView.getNodeModel());
    }

    /**
     * Returns the node model associated with this view
     */
    public Node getNodeModel(){

        return nodeModel;
    }

    /**
     * Previously named as move method. Calculates velocity vector needed to arrive
     * to the point and sets stop boundary for this node
     */
    public Point getVelocity(int newPointX, int newPointY, int velocity){

        int oldPointX=getMiddleX();
        int oldPointY=getMiddleY();

        //System.out.println(oldPointX+"/"+oldPointY+" "+newPointX+"/"+newPointY);

        // no need to use cached tables here, because move is invoked only once:
        double angleInRad=Math.atan2(newPointY-oldPointY, newPointX-oldPointX);

        int scaledX=(int)(velocity*Math.cos(angleInRad));
        int scaledY=(int)(velocity*Math.sin(angleInRad));

        Rectangle newBounds=new Rectangle(new Point(oldPointX,oldPointY));
        newBounds.add(new Point(newPointX,newPointY));
        setStopBounds(newBounds);
        //setV(scaledX,scaledY);
        return new Point(scaledX,scaledY);
    }

    /**
     * Initializes this NodeView: sets dimentions and font properties
     */
    public void init(Graphics g){

        fm=g.getFontMetrics();
        descent=fm.getDescent();
        row_height=fm.getHeight();
        shortLabelWidth=fm.stringWidth(shortLabel);

        //redefine w and h on base of the string:
        int oldMiddleX=getMiddleX();
        int oldMiddleY=getMiddleY();

        //define this node dimentions:
        if(image==null){
            width=shortLabelWidth;
            height=row_height;
            x=oldMiddleX-(width>>1);
            y=oldMiddleY-(height>>1);
        }
        else{
            x=oldMiddleX-(imageWidth>>1);
            y=oldMiddleY-(imageHeight>>1);
        }

    }

    /**
     * Standard paint routine
     */
    public void paint(Graphics g){

//redefine color:
        Color defaultColor=g.getColor();
        if(visited){
            g.setColor(VISITED_FONT_COLOR);
        }
        else{
            g.setColor(DEFAULT_FONT_COLOR);
        }

        //redefine x and y according to the real label String width/height:
        if(fm==null){
            init(g);
        }

        int vertShift=0;
        if(image!=null){
           vertShift=imageHeight+gapBetweenLabelAndImage;
        }
        int horTextPos=getMiddleX()-(shortLabelWidth>>1);

        if(showLongLabel){

           if(wrappedLabel==null){
              //System.out.println("Created");
              wrappedLabel=new WrappedLabel(panel, nodeModel.label);
              wrappedLabel.setBGColor(Color.white);
              wrappedLabel.init(fm, row_height, descent, shortLabelWidth);   //reset size according to actual FontMetrics
           }
           if(resetWrappedLabel){
              wrappedLabel.reset(horTextPos, y+vertShift);   //reset size according to actual FontMetrics
              resetWrappedLabel=false;
           }

           if(image!=null) super.paintShaded(g);       //paint sprite (picture) for long label
           wrappedLabel.paint(g);
         
        }
        else{
           super.paint(g);       //paint sprite (picture)

           if(image!=null){
             //background for text:
             Color prevColor=g.getColor();
             g.setColor(Color.white);
    	       g.fillRect(horTextPos, y+vertShift, shortLabelWidth, row_height); //white background for icon
             g.setColor(prevColor);
           }
           g.drawString(shortLabel, horTextPos, y+vertShift+row_height-descent);
        }


//return old color back:
        g.setColor(defaultColor);

    }//paintNode

    /**
     * Updates this node view position
     */
    public void update(){

        super.update();
        if(stopBounds!=null){
            if(!stopBounds.contains(x+(width>>1),y+(height>>1))){
                notifyArriveEventListeners(new ArriveEvent(this));
                //stopBounds=null;
            }
        }
    }

}
