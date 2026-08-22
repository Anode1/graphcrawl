/**
 * @(#)GraphView.java
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
import NavigatorApplet;

/**
 * Main panel for representing a graph. Currently has both model and view
 *
 * @author Vasili Gavrilov
 * @version 1.0
 * @since   JDK1.0
 */
public class GraphView extends Panel implements ArriveEventListener{

    /**
     * Ref to the parent applet
     */
    protected NavigatorApplet applet;

    /**
     * Visited nodes list
     */
    public Vector visitedNodes=new Vector();

    /**
     * visible nodes
     */
    protected OrderedHashtable nodes=new OrderedHashtable(100);

    /**
     * thread repainting this component
     */
    protected Motor motor;

    /**
     * Ref to the current central node
     */
    protected NodeView centralNode;

    /**
     * Long Label Node View (current mouse cursor in front of it)
     */
    protected NodeView longLabeledNodeView;

    /**
     * Ref to the Graph Model
     */
    protected GraphModel graphModel;

    /**
     * current graph view depth (model can have more levels)
     */
    protected int depth=2;//default

    /**
     * Length of regular children edges
     */
    public final int CHILD_EDGE_LENGTH=120;

    /**
     * Not used for now
     */
    public final int ELLIPSOIDALITY=2;

    /**
     * Length of regular parents edges
     */
    public final int PARENT_EDGE_LENGTH=120;

    /**
     * Sector where neighbours (children and parents have been placed initially)
     */
    public final int SECTOR_FOR_NEIGHBOURS=120;

    /**
     * Flag indicating the state where applet is idle
     */
    public final static int IDLE = 1;

    /**
     * Flag indicating the state where some node has been dragged
     */
    public final static int DRAG = 2;

    /**
     * Flag indicating the state where some node has been advancing to the center
     */
    public final static int ADVANCING = 3;

    /**
     * Reference to the math library
     */
    private AngleMath math=AngleMath.getInstance();

    /**
     * State of this component
     */
    public int state=IDLE;

    /**
     * Offscreen image been painted to offScreenGraphics
     */
    protected Image offScreenImage;

    /**
     * Offscreen graphics context
     */
    protected Graphics offScreenGraphics;

    /**
     * Currently dragged node
     */
    protected NodeView draggedNode;

    /**
     * Previous single click time - needed for double-click determining
     */
    protected long prevClick = System.currentTimeMillis();

    /**
     * Number of milliseconds between clicks in double-click
     */
    protected final long deltaBetweenClicks = 300L;

    /**
     * Waiting server animation
     */
    protected Animation animation;

    /**
     * Default constructor
     */
    public GraphView(NavigatorApplet applet, GraphModel graphModel){

        this.applet=applet;
        this.graphModel=graphModel;

        motor=new Motor(this);

        //initialize and load images for waiting server animation:
        animation=new Animation(this, applet);
        animation.loadImages();
    }

    /**
     * Called only once on init stage
     */
    public void setInitNode(Node node){

          centralNode = createNode(node);
          centralNode.setCenter(getCenterX(), getCenterY());
          this.addNode(new String(node.getID()), centralNode);
          centralNode.setHighlighted(true);
          setState(IDLE);
          applet.showUrl(node);
    }

    /**
     * Make a Node be central node
     */
    public void selectNode(Node node){

        motor.resumeThread();

        NodeView nodeView=this.getNode(node.id);

        if(nodeView==null){ //ABSENT in the view?

          this.removeAll();    //remove previous nodes - we do not need them anymore
          centralNode = createNode(node);
          centralNode.setCenter(getCenterX(), getCenterY());
          this.addNode(new String(node.getID()), centralNode);
          centralNode.setHighlighted(true);
          setState(IDLE);
          applet.resetGraphModel(node.id, depth);
          //applet.showUrl(node);
        }
        else{ //THERE IS IN THE VIEW:
           if(nodeView.isTerminal()){ //for Terminals - only show URL
             deselectAll();
             nodeView.setHighlighted(true);
             //applet.showUrl(node);       
           }
           else{    //Not Terminal:
             moveToCenter(nodeView);
           }
        }
    }


    /**
     * Moves already existing in the view node to the center making it
     * the central node
     */
    public void moveToCenter(NodeView nodeView){

        this.centralNode=nodeView;

        removeAllButCentral();

        this.setState(ADVANCING);
        nodeView.addArriveEventListener(this);
        Point v=nodeView.getVelocity(bounds().width>>1, bounds().height>>1, 30);
        this.setV(v);
        applet.resetGraphModel(nodeView.getNodeModel().getID(), depth);
    }

    public NodeView createNode(Node nodeModel){

        NodeView nodeView=new NodeView(this, nodeModel);
        /*
        try{
            int idInt=Integer.parseInt(nodeModel.id);
            //System.out.println("OVALS_MIN_ID="+applet.OVALS_MIN_ID+" OVALS_MAX_ID="+applet.OVALS_MAX_ID);
            if(idInt >= Categories.OVALS_MIN_ID && idInt <= Categories.OVALS_MAX_ID){
                //System.out.println(id);
                nodeView.setType(Sprite.OVAL);
            }
            //else nodeView.setType(Sprite.RECTANGLE);
        }
        catch(NumberFormatException nfe){
            System.out.println("A node has not-integer id so it's impossible to determine ovals range");
        }
        */
        return nodeView;
    }

    /**
     * Expands the node
     */
    public void expandNode(NodeView nodeView){

        if(nodeView==null){
          System.out.println("Trying to expand null!");
          return;
        }

        this.removeAllButCentral();

        nodeView.expanded=false;
        nodeView.fromAngle=0;
        expand(nodeView, 1, depth);//recursively build the graph level by level
        addVisitedNode(nodeView.nodeModel);
        centralNode.visited=true;

        deselectAll();//hack to walkaround deselection bug
        nodeView.setHighlighted(true);

        motor.suspendThread();
    }

    /**
     * Expands the node
     */
    public void expandCentral(){

        expandNode(centralNode);
    }

    /**
     * double-click on a node view handler
     */
    private void doubleClickOnNode(Event e, NodeView nodeView){

        Node nodeModel=nodeView.getNodeModel();

        deselectAll();
        nodeView.setHighlighted(true);

        if(!nodeView.isTerminal())selectNode(nodeModel);
        else motor.suspendThread();

        if((e.modifiers & 0x1) == 0){ //mask for Shift
            applet.showUrl(nodeModel); //show URL
        }
    }

    /**
     * Returns current depth of the graph
     */
    public int getDepth(){

      return depth;
    }

    /**
     * Sets the depth of the graph view.
     */
    public void setDepth(int depth){

       this.depth=depth;
       expandNode(centralNode);
       repaint();
    }

    /**
     * Removes all nodes except the central one from this graph view
     */
    public void removeAllButCentral(){
    
        removeAll();
        addNode(new String(centralNode.nodeModel.id),centralNode);
    }

    /**
     * Returns current central node of this graph view
     */
    public NodeView getCentralNode(){

        return centralNode;
    }

    /**
     * Gets the current state
     */
    public int getState(){

        return this.state;
    }

    /**
     * Sets current state
     */
    public void setState(int state){

        this.state=state;
    }

    /**
     * Deselects all node views
     */
    public void deselectAll(){

        for (Enumeration enum = nodes.elements() ; enum.hasMoreElements() ;) {
            NodeView anode=(NodeView)enum.nextElement();
            anode.setHighlighted(false);
        }
    }

    /**
     * Translates all node views
     */
    public void translate(int x, int y){

        for (Enumeration enum = nodes.elements() ; enum.hasMoreElements() ;) {
            NodeView node=(NodeView)enum.nextElement();
            node.translate(x,y);
        }
    }

    /**
     * Returns the first node view containing specified point
     */
    public NodeView getContains(int x, int y){

     //   NodeView lastNode=null;
        for (Enumeration enum = nodes.elements() ; enum.hasMoreElements() ;) {
            NodeView node=(NodeView)enum.nextElement();
            if(node.inside(x,y)){
                return node;    //
      //          lastNode=node;      //

            }
        }
        return null;                 //
       // return lastNode;                         //
    }

    /**
     * Returns the first node view containing specified point
     */
    public NodeView getContains(Point p){

        return this.getContains(p.x, p.y);
    }

    /**
     * Gets a node by the key
     */
    public NodeView getNode(String key){

        return (NodeView)nodes.get(key);
    }

    /**
     * Adds a node assotiated with thw key
     */
    public NodeView addNode(String key, NodeView node){

        return (NodeView)nodes.put(new String(key), node);
    }

    /**
     * Removes a node assotiated with the key
     */
    public NodeView removeNode(String key){

        return (NodeView)nodes.remove(key);
    }

    /**
     * Returns true if passed node has been visited in this session already
     */
    public boolean isVisited(NodeView node){

        return visitedNodes.contains(node.nodeModel.id);
    }

    /**
     * Adds a node to visited nodes
     */
    public void addVisitedNode(Node node){

        if(!visitedNodes.contains(node.id)){
            visitedNodes.addElement(new String(node.id));
        }
    }

    /**
     * Removes all nodes from this graph view
     */
    public void removeAll(){

        nodes=new OrderedHashtable(100);
    }

    /**
     * Sets equal velocity for all nodes in this view.
     * Not used in this implementation.
     */
    public void setV(int vx, int vy){

        for (Enumeration enum = nodes.elements() ; enum.hasMoreElements() ;) {
            NodeView node=(NodeView)enum.nextElement();
            node.setV(vx, vy);
        }
    }

    /**
     * Sets equal velocity for all nodes in this view.
     */
    public void setV(Point p){

        for (Enumeration enum = nodes.elements() ; enum.hasMoreElements() ;) {
            NodeView node=(NodeView)enum.nextElement();
            node.setV(p);
        }
    }

    /**
     * Creates the graph view from the graph model recursively begining from
     * the node passed
     */
    private void expand(NodeView nodeView, int level, int depth){

        if(nodeView.expanded)return; //already expanded
        nodeView.expanded=true;

        Node nodeModel=nodeView.getNodeModel();
        
        Vector childrenIds=(Vector)nodeModel.getChildrenIds().clone(); // from the model
        Vector parentsIds=(Vector)nodeModel.getParentsIds().clone();   // from the model

        int numOfChildren=childrenIds.size();
        int numOfParents=parentsIds.size();

        if(numOfChildren>0)nodeView.setTerminal(false);
        else{ //numChildren==0
           nodeView.setTerminal(true);
           if(numOfParents==0)return; //single node
        }

        if(level==depth){ //boundary level
            return;
        }

        int angleBetween=0;
        if(level==1){ //2PI
            angleBetween=(int)(360/(numOfChildren+numOfParents));
        }
        else{
            angleBetween=(int)(SECTOR_FOR_NEIGHBOURS/(numOfChildren+numOfParents));   //only sector
        }
        //System.out.println(nodeView.getNodeModel().getID()+":"+angleBetween);
//parents:
        int n=0;
        for (; n< numOfParents; n++){

            String aParentId=(String)parentsIds.elementAt(n);
            NodeView aParent=this.getNode(aParentId);

            if(aParent==null){
              aParent=createNeighbour(nodeView, aParentId, n, angleBetween, PARENT_EDGE_LENGTH);
              this.addNode(aParentId, aParent);
            }
            //aParent.expanded=false;
        }

//children:
        for (int j=0; j< numOfChildren; j++,n++){

            String aChildId=(String)childrenIds.elementAt(j);
            NodeView aChild=this.getNode(aChildId);

            if(aChild==null){
              aChild=createNeighbour(nodeView, aChildId, n, angleBetween, CHILD_EDGE_LENGTH);
              this.addNode(aChildId, aChild);
            }
            //aChild.expanded=false;
        }

//System.out.println(nodeView.getNodeModel().getLabel()+":"+nodeView.fromAngle);

        for (int i=0; i<numOfParents; i++){

            String aParentId=(String)parentsIds.elementAt(i);
            NodeView aParent=this.getNode(aParentId);
            if(aParent!=null){
              expand(aParent, level+1, depth);   //recursive call for parents
            }
        }

        for (int i=0; i<numOfChildren; i++){

            String aChildId=(String)childrenIds.elementAt(i);
            NodeView aChild=this.getNode(aChildId);
            if(aChild!=null){
              expand(aChild, level+1, depth);   //recursive call for children
            }
            //aChild.expanded=false;
        }


    }//expand

    /**
     * Creates a neighbour for a node and places it according to the passed parameters
     */
    private NodeView createNeighbour(NodeView parentNodeView, String aChildId, int i, int angleBetween, int edgeLength){

          Node aNodeFromModel=applet.getGraphModel().getNode(aChildId);
          if(aNodeFromModel==null){    //create dummy node 
               aNodeFromModel=new Node(new String(aChildId),"label id="+aChildId,"",null,new Vector(1),new Vector(1));
          }

          int angle=angleBetween*i+parentNodeView.fromAngle;
          double dx=math.sin(angle);
          int centerX=parentNodeView.getMiddleX()+/*(int)((nodeView.width/2)*dx)*/+(int)(edgeLength*dx);
          int centerY=parentNodeView.getMiddleY()-(int)(edgeLength*math.cos(angle));

          NodeView aChild=createNode(aNodeFromModel);

          aChild.setCenter(centerX, centerY);
          if(isVisited(aChild)){
               aChild.visited=true;
          }
          //calculate angle start for children:

          aChild.fromAngle=angleBetween*i + parentNodeView.fromAngle-(SECTOR_FOR_NEIGHBOURS>>1);

          addNode(new String(aChildId), aChild);

          return aChild;
    }
    
    /**
     * Returns the left bound for nodes
     */
    public int getGraphLeftBound(){

       return 1;
    }

    /**
     * Returns the right bound for nodes
     */
    public int getGraphRightBound(){

       return size().width-2;
    }

    /**
     * Returns the top bound for nodes
     */
    public int getGraphTopBound(){

       return 1;
    }

    /**
     * Returns the bottom bound for nodes
     */
    public int getGraphBottomBound(){

       return size().height-2;
    }

    /**
     * Gets the X-axis center of this graph panel
     */
    public int getCenterX(){

       return (int)(size().width>>1);
    }

    /**
     * Gets the Y-axis center of this graph panel
     */
    public int getCenterY(){

       return (int)(size().height>>1);
    }

    /**
     * Gets the central point of this graph panel
     */
    public Point getCenter(){

       return new Point((int)(size().width>>1),(int)(size().height>>1));
    }

    /**
     * Updates position of the dragged node view
     */
    protected void updatePositionDuringDrag(int x, int y, boolean forAll){

       //System.out.println(x+ " "+y);
       int leftBound   = getGraphLeftBound();
       int rightBound  = getGraphRightBound() - draggedNode.width;
       int topBound    = getGraphTopBound();
       int bottomBound = getGraphBottomBound() - draggedNode.height;

       if(forAll){
          return;
       }

       //for dragged node only:
       draggedNode.x += x - draggedNode.oldx;
       draggedNode.y += y - draggedNode.oldy;
       draggedNode.oldx = x;
       draggedNode.oldy = y;

       if(draggedNode.x < leftBound){
          draggedNode.x = leftBound;
          draggedNode.oldx=leftBound;
       }
       else if(draggedNode.x > rightBound){
          draggedNode.x = rightBound;
          draggedNode.oldx=rightBound;
       }

       if(draggedNode.y < topBound){
          draggedNode.y = topBound;
          draggedNode.oldy = topBound;
       }
       else if(draggedNode.y > bottomBound){
          draggedNode.y = bottomBound;
          draggedNode.oldy = bottomBound;
       }

    }

    /**
     * Updates all nodes and calls paint()
     */
    public void update(Graphics g){

       for (Enumeration enum = nodes.elements() ; enum.hasMoreElements() ;) {
          NodeView node=(NodeView)enum.nextElement();
          node.update();
       }

       paint(g);
    }

    /**
     * Paints this component
     */
    public void paint(Graphics g){

       int w=bounds().width;
       int h=bounds().height;

       if(offScreenImage == null || offScreenImage.getWidth(null) != w || offScreenImage.getHeight(null) != h){
          offScreenImage = createImage(w, h);
          offScreenGraphics = offScreenImage.getGraphics();
       }

       offScreenGraphics.setColor(Color.white);
       offScreenGraphics.fillRect(0, 0, w, h);
       //offScreenGraphics.setColor(defaultColor);
       offScreenGraphics.setColor(Color.black);

       if(applet.isWaitingForServer()){
          animation.paint(offScreenGraphics,w,h);
       }

       paintEdges(offScreenGraphics);
       paintNodes(offScreenGraphics);

       g.drawImage(offScreenImage, 0, 0, null);
    }

    /**
     * Paints all edges assotiated with a node (both children and parents) including dummy edges
     */
    private void paintEdgesForNode(NodeView nodeView, Graphics g){

      Node nodeModel=nodeView.nodeModel;
      int numberOfHidden=0;

      // edges to parents:

      Vector parentIds=(Vector)nodeModel.parents.clone();
      for (Enumeration e = parentIds.elements() ; e.hasMoreElements() ;) {
         String aParentId=(String)e.nextElement();
         NodeView parent=(NodeView)this.getNode(aParentId);
         if(parent==null)numberOfHidden++;
         else{
            //if(parent.visited)g.setColor(Color.blue);
            Edge edgeToParent=new Edge(nodeView,parent);
            edgeToParent.paint(g);
            //if(parent.visited)g.setColor(Color.black);
         }
      }

      //edges to children:
      Vector childrenIds=(Vector)nodeModel.children.clone();
      for (Enumeration e = childrenIds.elements() ; e.hasMoreElements() ;) {
         String aChildId=(String)e.nextElement();
         NodeView child=(NodeView)this.getNode(aChildId);
         if(child==null)numberOfHidden++;
         else{
            if(child.visited)g.setColor(Color.blue);
            g.drawLine(nodeView.getMiddleX(), nodeView.getMiddleY() , child.getMiddleX(), child.getMiddleY());
            if(child.visited)g.setColor(Color.black);
         }
      }

      if(numberOfHidden==0){
        return;
      }

      //dummy edges for hidden children
      int edgeLength=35;

      int angleRange=90;
      int xDistance=nodeView.getMiddleX()-this.getCenterX();
      int yDistance=nodeView.getMiddleY()-this.getCenterY();

      if(xDistance==0){
        xDistance=1;
      }
      if(yDistance==0){
        yDistance=1;
      }

      int thisNodeAngle=0;
      if(!nodeView.equals(centralNode)){
        //thisNodeAngle=(int)math.rad2Deg(Math.atan2(yDistance,xDistance))+90-(angleRange>>1);
        thisNodeAngle=(int)math.atan2(yDistance,xDistance)+90-(angleRange>>1);
      }
      else{
        angleRange=360;
      }
      int fromAngle=thisNodeAngle;//-(angleRange>>1);
      int angleBetween=(int)(angleRange/numberOfHidden);

      for (int i=0; i< numberOfHidden; i++){

        int angle=angleBetween*i+fromAngle;
        double dx=math.sin(angle);
        int nonExistingX=nodeView.getMiddleX()+(int)((nodeView.width>>1)*dx)+(int)(edgeLength*dx);
        int nonExistingY=nodeView.getMiddleY()-(int)(edgeLength*math.cos(angle));

        g.drawLine(nodeView.getMiddleX(), nodeView.getMiddleY() , nonExistingX, nonExistingY);
        g.fillOval(nonExistingX-2, nonExistingY-2, 4, 4);  //bulbs at the ends
      }
    }

    /**
     * Paints all edges
     */
    public void paintEdges(Graphics g){

        //not passedEdges:
       for (Enumeration e = nodes.elements() ; e.hasMoreElements() ;) {
          this.paintEdgesForNode((NodeView)e.nextElement(), g);
       }
    }

    /**
     * Paints all nodes
     */
    public void paintNodes(Graphics g){

       for (Enumeration e = nodes.elements() ; e.hasMoreElements() ;) {
           NodeView node=(NodeView)e.nextElement();
           node.paint(g);
       }
    }

    /**
     * Standard mouse event handler
     */
    public boolean mouseDown(Event e, int x, int y){

        motor.resumeThread();

        NodeView nodeView = getContains(x, y);
        if(nodeView != null){

            //remove long label:
            if(longLabeledNodeView!=null){
              longLabeledNodeView.setLongLabel(false);
              longLabeledNodeView=null;
            }

            long click = System.currentTimeMillis();
            if(click - prevClick < deltaBetweenClicks){
              doubleClickOnNode(e, nodeView);
            }
            else{
              prevClick = click;
              nodeView.oldx = x;
              nodeView.oldy = y;
              draggedNode = nodeView;
              setState(DRAG);
            }
        }
        return true;
    }

    /**
     * Standard mouse event handler
     */
    public boolean mouseDrag(Event e, int x, int y){

        if(getState() == DRAG && draggedNode != null){

            boolean forAll=false;
            if((e.modifiers & 0x1) != 0){ //mask for Shift
              forAll=true;
            }
            updatePositionDuringDrag(x, y, forAll);
            draggedNode.resetWrappedLabel();
        }
        return true;
    }

    /**
     * Standard mouse event handler
     */
    public boolean mouseMove(Event e, int x, int y){

       //long click = System.currentTimeMillis();
       //if(click - prevMove > timeNotToPaint){

       if(getState() == DRAG){
          return true;
       }

       NodeView nodeView = getContains(x, y);
       if(nodeView != null){
          if(nodeView==longLabeledNodeView){ //we move mouse inside one NodeView
                //skip
          }
          else{ //another nodeView or null
                if(longLabeledNodeView!=null){ //if there is old
                    longLabeledNodeView.setLongLabel(false);
                }
                longLabeledNodeView=nodeView;
                longLabeledNodeView.setLongLabel(true);
                repaint();
          }
          //System.out.println("longLabel for node:"+nodeView.labelView);
          //nodeView.setLongLabel(true);
       }
       else{ //on null NodeView i.e. exited
          if(longLabeledNodeView!=null){
              longLabeledNodeView.setLongLabel(false);
              longLabeledNodeView=null;
              repaint();
          }
       }

   //    prevMove = click;


       return true;
    }

    /**
     * Standard mouse event handler
     */
    public boolean mouseUp(Event e, int x, int y){

        if(getState() == DRAG){
            draggedNode = null;
            state = IDLE;
            motor.suspendThread();
        }

        return true;
    }

    /**
     * Callback on node arriving.
     * @see ArriveEvent
     * @see ArriveEventListener
     */
    public void onArrive(ArriveEvent e){

        NodeView node = (NodeView)e.getSource();  //who has been arrived?

        if(node != null){
            this.setV(0,0);
            node.removeArriveEventListener(this);
            node.setStopBounds(null);
            node.resetWrappedLabel();
        }
        setState(IDLE);
    }

    /**
     * Returns this panel repainting thread
     */
    public Motor getMotor(){
    
        return motor;
    }


}
