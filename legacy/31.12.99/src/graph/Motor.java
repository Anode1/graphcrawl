/**
 * @(#)Motor.java
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

import java.awt.Component;

/**
 * Thread refreshing some component
 */
public class Motor implements Runnable{

 /**
  * working thread
  */
 private volatile Thread thread;

 /**
  * Component to be repainted
  */
 protected Component component;

 /**
  * period between repaintings
  */
 protected long refreshPeriod=100;

 /**
  * Priority of the refresher thread
  */
 protected int priority=4;

 /**
  * Flag indicating that this thread has been suspended temporarily.
  */
 private boolean isSuspended;

 /**
  * debug mode
  */
 protected boolean debug=true;

 /**
  * Default constructor
  */
 public Motor(Component component) {

   this.component=component;
 }

  /**
   * Constructor
   */
 public Motor(Component component, long refreshPeriod) {

   this.component=component;
   this.refreshPeriod=refreshPeriod;
 }

 /**
  * Starts repainter thread.
  */
 public void start(){

	 if (thread == null){
			thread = new Thread(this);
			thread.setPriority(priority);
			thread.start();
	 }
 }

 /**
  * Stops repainter thread.
  */
 public void stop(){

	  thread = null;
 }

 /**
  * Safely resumes this thread
  */
/*
 public synchronized void _resumeThread(){

    if(isSuspended){
      isSuspended=!isSuspended;
      notify();
    }
 }
*/

 /**
  * Safely resumes this thread
  */
 public void resumeThread(){

    if(isSuspended){
      isSuspended=!isSuspended;
    }
 }

 public void suspendThread(){

    if(!isSuspended){
      isSuspended=!isSuspended;
      counter=0;
    }
 }

 protected int counter;

 /**
  * Body of the repainting thread
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
        /*
        if(isSuspended){

            if(counter>=3){     //repaint 3 times after suspention
              try{

                synchronized(this){
                 // while(isSuspended){
                        wait();
                 // }
                    System.out.println("resumed");
                }

              }
              catch (InterruptedException e) { }
            }
            else counter++;
         }
         */

         t += refreshPeriod;

         if(isSuspended){
            counter++;
            if(counter<3)component.repaint();    //paint 3 times after suspention
         }
         else{
            component.repaint(); //will repaint immediately if the thread has been released
         }

     }//while

 }//run

 /**
  * Finalizer. Stops repainter thread.
  */
  public void finalize() {

	   this.stop();
  }

}