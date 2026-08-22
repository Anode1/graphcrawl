/**
 * @(#)NavigatorApplet.java
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

//package graph;

import java.applet.Applet;
import java.awt.*;
import java.util.*;

import graph.*;

/**
 * Main applet class.
 *
 * @author Vasili Gavrilov
 * @version 1.0
 * @since   JDK1.0
 */
public class NavigatorApplet extends Applet{

    /**
     * debug flag. true if the applet has been loaded from parameters only
     */
    public boolean fromAppletParameters;       //debug mode - if the data is in index.html

    /**
     * Graph model instance
     */
    protected GraphModel graphModel;  //Has been reloaded each HTTP request to the server

    /**
     * Graph view (GUI component: graph panel)
     */
    protected GraphView graphView;         //graph panel

    /**
     * waiting server flag. True if the server was requested but result still has not been received
     */
    protected boolean waitingForServer;    //flag indicating that the applet still waiting server's response

    /**
     * Flag used if the server been queried for a tree only once: on init
     */
    protected boolean staticApplet;        //if cache has been loaded only once

    /**
     * debug mode - prints additional information about request to console
     */
    protected boolean debug;

    /**
     * Panel at the bottom with tools (history, depth)
     */
    protected ToolsPanel toolsPanel;          //panel at the bottom

    /**
     * Requestor thread
     */
    private volatile InfoRequestThread communicationThread;

    /**
     * Thread showing pages into browser's frame
     */
    private volatile ShowURLThread showURLThread;

    /**
     * Thread showing Tool Tips (in our case - long labels for nodes)
     */
    //private volatile ToolTpisManagerThread toolTpisManagerThread;

    /**
     * Internal flag used in this class for init state indication
     */
    private boolean initialized;

    /**
     * Default constructor
     */
    public NavigatorApplet(){
    }

    /**
     * Standard applet init() method. Mainly reads all parameters, creates GUI
     * and at the end makes request to the server for initial tree using
     * parent id taken from parameters
     */
    public void init(){

      try{
        setLayout(new BorderLayout());

        graphModel=new GraphModel(1);

        graphView=new GraphView(this, graphModel);
        add("Center", graphView);

        toolsPanel = new ToolsPanel(this);
        add("South", toolsPanel);

        //toolTpisManagerThread =new ToolTpisManagerThread();

    //parameters:
        if(getParameter("line_0") != null){    //there are parameters
            fromAppletParameters = true;
        }

        Categories categories=new Categories(this);

        String staticParameter = getParameter("static_version");
        if(staticParameter != null && (staticParameter.equals("true") || staticParameter.equals("yes"))){
            staticApplet = true;
        }

        String keywordid = getParameter("first_node");
        if(keywordid == null){
            System.out.println("first_node is null");
        }
        resetGraphModel(keywordid, graphView.getDepth());
      }
      catch(Exception e){
        e.printStackTrace();
      }
    }

    /**
     * applet's start method. Also starts graphView
     */
    public void start(){

        graphView.getMotor().start(); //start "live" graph panel
        //toolTpisManagerThread.start();
    }

    /**
     * applet's stop method. Also stops graphView
     */
    public void stop(){

        graphView.getMotor().stop();  //stop "live" graph panel if applet stopped
        //toolTpisManagerThread.stop();
    }

    /**
     * Sets graph model for this applet
     */
    public void setGraphModel(GraphModel graphModel){

        this.graphModel=graphModel;
    }

    /**
     * Refills the cache by new information taken from the server using InfoRequestThread
     */
    public void resetGraphModel(String keywordid, int depth){

        if(staticApplet && graphModel!=null)return; //not the first time if staticApplet

        setWaitingForServer(true);

        if(communicationThread!=null)communicationThread.stop();
        communicationThread=new InfoRequestThread(this, keywordid, depth, fromAppletParameters);
        communicationThread.setDebug(debug);
        communicationThread.start();
    }

    /**
     * Callback function called by Communicator Thread when data has been arrived from the server
     */
    public void dataReady(String firstNodeId){

      try{
        setWaitingForServer(false);

        if(debug)System.out.println(graphModel.toString());

        if(!initialized){
            Node firstNode=graphModel.getNode(firstNodeId);
            if(firstNode==null){
              System.err.println("The first node with id="+firstNodeId+" is absent in data!");
              return;
            }
            graphView.setInitNode(firstNode);
            initialized=true;
            //System.out.println("Initialized");
        }

        while(graphView.getState() == GraphView.ADVANCING){
            try{
                Thread.sleep(100L);
            }
            catch(InterruptedException interruptedexception) { }
        }

        graphView.expandCentral();
      }
      catch(Exception e){
        e.printStackTrace();
      }
    }

    /**
     * Returns true if a request to the server was made but a result still not received
     */
    public boolean isWaitingForServer(){

        return waitingForServer;
    }

    /**
     * Sets waiting server status flag.
     */
    public void setWaitingForServer(boolean waitingForServer){

        this.waitingForServer=waitingForServer;

        if(waitingForServer){
          showStatus("Getting information from the server");
        }
        else{
          showStatus("");
        }
    }

    /**
     * Returns the graph view assotiated with this Navigator applet
     */
    public GraphView getGraphView(){

        return graphView;
    }

    /**
     * Returns the graph model
     */
    public GraphModel getGraphModel(){

        return graphModel;
    }

    /**
     * Shows URL in the content frame (separate thread does it) and puts the node
     * into the History
     */
    public void showUrl(Node node){

        toolsPanel.addHistoryNode(node);

        if(showURLThread!=null)showURLThread.stop(); //kill safely previous thread

        if(node.getUrl()==null){
          System.err.println("Trying to connect with null URL");
          return;
        }
        //if(debug)
        //System.out.println("Trying to connect:"+node.getUrl());

        showURLThread=new ShowURLThread(node.getUrl(),"content",this);
        showURLThread.start();
    }

    /**
     * Kills communicator thread if alive
     */
    public void finalize() {

     //gracefully stop threads:
        if(communicationThread!=null){
          communicationThread.stop();
          communicationThread=null;
        }
        if(showURLThread!=null){
          showURLThread.stop();
          showURLThread=null;
        }
        /*
        if(toolTpisManagerThread!=null){
          toolTpisManagerThread.stop();
          toolTpisManagerThread=null;
        }
        */
        if(graphView!=null && graphView.getMotor()!=null){
          graphView.getMotor().stop();
        }
    }

}
