/**
 * @(#)Categories.java
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
 * Class having knowlege about categories.
 *
 *
 * @author Vasili Gavrilov
 * @version 1.0
 * @since   JDK1.0
 */
public class Categories {

  /**
   * the applet MediaTracker and Parameters taken from
   */
  protected Applet applet;

  /**
   * min bound for ovals
   */
  public static int OVALS_MIN_ID = -1000;

  /**
   * max bound for ovals
   */
  public static int OVALS_MAX_ID = 0;

  /**
   * Images array for categories
   */
  protected Image[] catImages;

  /**
   * Minimum id values for categories
   */
  protected int[] catMins;

  /**
   * Maximum id values for categories
   */
  protected int[] catMaxs;

  /**
   * Ref to the single instance of this singleton
   */
  private static Categories instance;

  /**
   * Constructor
   */
  public Categories(Applet applet){

      this.applet=applet;
      instance=this;
      instance.loadImages();

      if(applet.getParameter("ovals_min_id") != null){
            try{
                OVALS_MIN_ID = Integer.parseInt(applet.getParameter("ovals_min_id"));
            }
            catch(NumberFormatException e){
                System.out.println("parameter ovals_min_id is incorrect");
            }
      }

      if(applet.getParameter("ovals_max_id") != null){
            try{
                OVALS_MAX_ID = Integer.parseInt(applet.getParameter("ovals_max_id"));
            }
            catch(NumberFormatException e){
                System.out.println("parameter ovals_max_id is incorrect");
            }
      }
  }

  /**
   * Returns the instance of this singleton
   */
  public static Categories getInstance(){

    return instance;
  }

  /**
   * Loads all images for all categories
   */
  public void loadImages(){
  
     try{
        MediaTracker tracker = new MediaTracker(applet);
        int number = 0;

        catImages = new Image[255];
        for(int i = 0; i < 255; i++){
            String filename = applet.getParameter("category_icon_" + (i + 1));
            if(filename != null){
                catImages[i] = applet.getImage(applet.getDocumentBase(), filename);
                tracker.addImage(catImages[i], i);
            }
        }

        tracker.waitForAll();
     }
     catch(InterruptedException ie){
         System.err.println("Loading is interupted!");
     }
     catch(Exception e){
         e.printStackTrace();
     }
  }

  public Image getImageForCategory(String categoryString){

     //System.out.println(categoryString);
     int categoryNum=0;
     try{
        categoryNum=Integer.parseInt(categoryString);
     }
     catch(NumberFormatException nfe){    //not integer
        return null;
     }
     if(catImages==null || catImages.length < categoryNum)return null;

     if(categoryNum==0)return null;
     return catImages[categoryNum-1];
  }

/*
  public Image getImageForID(String stringId){
     int id=0;
     try{
        id=Integer.parseInt(stringId);
     }
     catch(NumberFormatException nfe){    //not integer
        return null;
     }

     if(id>=-600 && id<-501){
        return getImageForCategory(1);
     }
     else if(id>=-500 && id<=-401){
        return getImageForCategory(2);
     }
     else if(id>=-400 && id<=-301){
        return getImageForCategory(3);
     }
     else if(id>=-300 && id<=-201){
        return getImageForCategory(4);
     }
     else if(id>=-200 && id<=-101){
        return getImageForCategory(5);
     }
     else if(id>=-100 && id<=-1){
        return getImageForCategory(6);
     }
     return null;
  }
*/

  public boolean isInOvalRange(String stringId){
  
     int id=0;
     try{
        id=Integer.parseInt(stringId);
     }
     catch(NumberFormatException nfe){    //not integer
        return false;
     }
     
     return (OVALS_MIN_ID<id && OVALS_MAX_ID>id);
  }


} 