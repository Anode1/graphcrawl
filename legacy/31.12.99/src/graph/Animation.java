/**
 * @(#)Animation.java
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
import java.applet.Applet;

/**
 * Default animation has been painted into the applet. If after images are loaded
 * setPanel() has neen called then it's painted to this panel
 *
 * @author Vasili Gavrilov
 * @version 1.0
 * @since   JDK1.0
 */
public class Animation{

  protected int counter;
  protected int triples;
  protected int periodLength = 4;
  protected int currentFrame;
  protected Image frames[];

  /**
   * panel where to show animation
   */
  protected Panel panel;

  /**
   * the applet MediaTracker and Parameters taken from
   */
  protected Applet applet;

  /**
   * horizontal shift
   */
  protected int fromX = 30;

  /**
   * vertical shift
   */
  protected int fromY = 30;

  /**
   * Default constructor
   */
  public Animation(Panel panel, Applet applet){

    this.panel=panel;
    this.applet=applet;
  }

  /**
   * Changes the panel where to display animation
   */
  public void setPanel(Panel panel){
    this.panel=panel;
  }

  /**
   * Loads images
   */
  public void loadImages(){

        MediaTracker tracker = new MediaTracker(applet);
        int number = 0;

       //redefine horizontal shift if there is corresponding parameter:
        if(applet.getParameter("iconFromX") != null){
            try{
                fromX=Integer.parseInt(applet.getParameter("iconFromX"));
            }
            catch(NumberFormatException e){
                System.out.println("parameter iconFromX is incorrect");
            }
        }

        //redefine vertical shift if there is corresponding parameter:
        if(applet.getParameter("iconFromY") != null){
            try{
                fromY=Integer.parseInt(applet.getParameter("iconFromY"));
            }
            catch(NumberFormatException e){
                System.out.println("parameter iconFromX is incorrect");
            }
        }

        String numberString = applet.getParameter("number_of_frames_in_anim");
        if(numberString == null){
            System.out.println("No animation files specified in html parameters");
            return;
        }

        try{
            number = Integer.parseInt(numberString);
        }
        catch(NumberFormatException nfe){
            System.out.println("number_of_frames_in_anim parameter in html is not a number!");
            return;
        }

        frames = new Image[number];
        for(int i = 0; i < number; i++){
            String filename = applet.getParameter("file_" + (i + 1));
            if(filename != null){
                frames[i] = applet.getImage(applet.getDocumentBase(), filename);
                tracker.addImage(frames[i], i);
            }
        }

        try{
            tracker.waitForAll();
        }
        catch(InterruptedException e){
            System.out.println("Loading is interupted!");
        }
    }

    public void paint(Graphics g, int w, int h){

        counter++;
        if(counter % 12 == 0){
            triples++;
            currentFrame++;
        }
        int numberOfPeriods = (w/2 - fromX) / periodLength; //dynamically, according to applet.width
        int currentPeriod=0;
        for(int i = 0; i < numberOfPeriods; i++){
            currentPeriod = triples % 3;
            int beg = fromX + (i + currentPeriod) * periodLength;
            if(i % 3 == 0)
                g.drawLine(beg, fromY, beg + periodLength, fromY);
        }

        if(frames != null && frames[currentFrame % frames.length] != null){
            g.drawImage(frames[currentFrame % frames.length], fromX, fromY-32, panel);
        }
        g.drawString("Connecting to the server...", w/2 + 10, fromY);
    }

}