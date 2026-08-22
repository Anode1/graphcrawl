/**
 * @(#)ShowURLThread.java
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

import java.applet.Applet;
//import netscape.javascript.JSObject;    //was used for JS communication

/**
 * Thread showing url in frame which name has been passed into constructor
 *
 * @author Vasili Gavrilov
 * @version 1.0
 * @since   JDK1.0
 */
public class ShowURLThread implements Runnable{

    /**
     * Debug mode flag
     */
    protected boolean debug;

    /**
     * URL of a document to show in the browser's frame
     */
    protected String url;

    /**
     * Name of the frame where to show a document
     */
    protected String framename;

    /**
     * Ref to the applet
     */
    protected Applet applet;

    /**
     * Worker thread
     */
    protected Thread thread;

   /**
    * Priority of the worker thread
    */
    protected int priority=5;

    /**
     * Default constructor
     */
    public ShowURLThread(String url, String framename, Applet applet) {

      this.url=url;
      this.framename=framename;
      this.applet=applet;
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
    * Body of this thread
    */
    public void run(){

      try{
        if(url==null){    //just for sure
          System.err.println("Trying to connect with null URL");
          return;
        }

        url=new String(url.trim());
        //System.out.println("codebase="+applet.getCodeBase());

        if(url.equals(""))return;

        if(!url.startsWith("http:"))url=new String(applet.getCodeBase()+url);

        if(debug)System.out.println("ShowUrlThread::Connecting: "+url);

        applet.showStatus((url==null?"null":url) + " connecting...");

        java.net.URL _url=new java.net.URL(url);
           // String parms[] = {url};
           //     JSObject window = JSObject.getWindow(this);
           //     if(window != null)
           //         window.call("showPage", parms);
        applet.getAppletContext().showDocument(_url, framename);
      }
      catch(java.net.MalformedURLException me){
        System.err.println("Failed to open:"+url+":"+me);
      }
      finally{
        applet.showStatus("");
      }
    }

}