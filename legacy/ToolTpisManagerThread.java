/**
 * @(#)ToolTpisManagerThread.java
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

/**
 * Thread showing Tool Tips (in our case - triggering labels for Node Views).
 * Was planned to be used but currently @see Timer does the same job
 *
 * @author Vasili Gavrilov
 * @version 1.0
 * @since   JDK1.0
 */
public class ToolTpisManagerThread implements Runnable{

    /**
     * Debug mode flag
     */
    protected boolean debug;

    /**
     * Worker thread
     */
    protected Thread thread;

    /**
     * Priority of the worker thread
     */
    protected int priority=3;

    /**
     * period between repaintings
     */
    protected long refreshPeriod=1000;

    /**
     * Flag indicating that this thread has been suspended temporarily.
     */
    private boolean isSuspended;

    /**
     * Default constructor
     */
    public ToolTpisManagerThread(){
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

	    thread = null;
    }

    /**
     * Body of this thread
     */
    public void run(){

      long t = System.currentTimeMillis();
      Thread thisThread=Thread.currentThread();
      t += refreshPeriod; //for the first time

	    while (thread==thisThread) {

        try{
            thread.sleep(Math.max(0, t-System.currentTimeMillis()));
        }
        catch (InterruptedException e) { }


         t += refreshPeriod;

         if(!isSuspended){

            System.out.println("done");
            //do the job
         }

      }//while

    }

    /**
     * Finalizer. Stops repainter thread.
     */
    public void finalize() {

	     this.stop();
    }

}