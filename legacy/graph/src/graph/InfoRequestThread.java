/**
 * @(#)InfoRequestThread.java
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
import java.io.*;
import java.util.*;
import java.net.*;
import java.applet.Applet;
import NavigatorApplet;

/**
 * Thread making communication with the server, getting result tree, parsing it
 * and putting nodes into hashtable directly to the applet
 *
 * @author Vasili Gavrilov
 * @version 1.0
 * @since   JDK1.0
 */
public class InfoRequestThread implements Runnable{

 /**
  * Ref to the main applet. Mainly for extracting parameters from the applet.
  */
 protected NavigatorApplet applet;

 /**
  * Parent ID (the root of the requested nodes tree)
  */
 protected String parentID;

 /**
  * flag indicating degug mode
  */
 protected boolean debug;

 /**
  * flag indicating where to take nodes tree: from applet parameters or make
  * Http request to the server
  */
 protected boolean fromAppletParameters;

 /**
  * Worker thread
  */
 protected Thread thread;

/**
 * Priority of the worker thread
 */
 protected int priority=5;

 /**
  * Preferred depth of the response tree
  */
 protected int depth;

 /**
  * Default constructor
  */
 public InfoRequestThread(NavigatorApplet applet, String parentID, int depth, boolean fromAppletParameters){

        this.applet=applet;
        this.parentID=parentID;
        this.depth=depth;
        this.fromAppletParameters=fromAppletParameters;
 }

 /**
  * Starts worker thread.
  */
  public void start(){

    if (thread == null){
			  thread = new Thread(this);
			  thread.setPriority(priority);
			  thread.start();
	  }
  }

 /**
  * Stops worker thread.
  */
  public void stop(){

    if(thread!=null)thread.stop();
    thread = null;
  }

 /**
  * The body of this thread
  */
 public void run(){

    if(fromAppletParameters){

        getFromAppletParameters();
        //uncomment the next line if we want to simulate network delay during debugging:
        try{Thread.sleep(1000);}catch(Exception e){};
    }
    else{
        getFromCGI();
    }

    applet.dataReady(parentID); //callback

 }//run

 /**
  * Aux function used internally when server used for requesting
  */
 protected void getFromCGI(){

    String line="";
    //if(applet.prevParentNode!=null)applet.cache.put(new String(applet.prevParentNode.id), applet.prevParentNode);

    DataInputStream is=null;
    try{

       String cgi = applet.getParameter("cgi");
       if(cgi == null){
           System.out.println("There is no cgi parameter specified");
           return;
       }

       URL url=new URL(applet.getCodeBase(),cgi);
       HttpRequest httpRequest=new HttpRequest(url);

       CGIParameters params=new CGIParameters();

     //parentID
       params.addParam("keywordid", parentID);

     //wid
       String wid=applet.getParameter("wid");
       if(wid!=null) params.addParam("wid",wid);

     //themeId
       String themeId=applet.getParameter("themeId");
       if(themeId!=null) params.addParam("ThemeID",themeId);

     //depth
       try{
          params.addParam("depth",Integer.toString(depth));
       }
       catch(NumberFormatException nfe){
          System.err.println("InfoRequestThread::getFromCGI:invalid depth");
       }

       httpRequest.setDebug(debug);

       is=new DataInputStream(httpRequest.makeGetRequest(params));

       Parser parser=new Parser();
       //System.out.println("Received:");
       while((line=is.readLine())!=null){
           parser.parseLine(line);
       }
       applet.setGraphModel(parser.getGraphModel());

    }
    catch(Exception e){
        System.out.println("InfoRequestThread::getFromCGI:Error during http request:");
        e.printStackTrace();
    }
    finally{
        if(is!=null)try{is.close();}catch(IOException e){};

    }
 }

 /**
  * Internally used method when nodes tree has been taken from applet parameters
  */
 protected void getFromAppletParameters(){

      int i=0;
      Parser parser=new Parser();
	    while(applet.getParameter("line_"+i)!=null){
	        String line=applet.getParameter("line_"+i);
          parser.parseLine(line);
	        i++;
	    }
      applet.setGraphModel(parser.getGraphModel());
 }

 /**
  * Sets debugging mode
  */
 public void setDebug(boolean debug){
 
    this.debug=debug;
 }


}