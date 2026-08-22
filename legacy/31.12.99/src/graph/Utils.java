/**
 * @(#)Utils.java
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
import java.awt.event.*;
import java.util.*;
import java.io.*;

/**
 * Utility class - simly set of useful funcions used by different
 * classes
 *
 * @author Vasili Gavrilov
 * @version 1.0
 * @since   JDK1.0
 */
public class Utils{


/**
 * Puts component me being centralized relative to the parent
 */
public void setCentalizedLocationRelativeMe(Component parent, Component me){

    if(parent==null)return;

     Point parentLoc=parent.getLocation();
     int x=0,y=0; //my future coordinates

     int parentX=parentLoc.x;
     int parentY=parentLoc.y;
     int parentW=parent.getSize().width;
     int parentH=parent.getSize().height;

     int w=me.getSize().width;
     int h=me.getSize().height;

     if(w<parentW)x=parentX+(parentW-w)/2;
     else x=parentX-(w-parentW)/2;

     if(h<parentH)y=parentY+(parentH-h)/2;
     else y=parentY-(h-parentH)/2;

     me.setLocation(x,y);
}

/**
 * Gets the point where to put the component "me" been centralized relative to the parent
 */
public static Point getCentalizedLocationRelativeParent(Component parent, Component me){

     if(parent==null)return null;

     int parentW=parent.getSize().width;
     int parentH=parent.getSize().height;

     int w=me.getSize().width;
     int h=me.getSize().height;

     int x=0,y=0;

     if(w<parentW)x=(parentW-w)/2;
     else x=0;

     if(h<parentH)y=(parentH-h)/2;
     else y=0;

     return new Point(x,y);
}

/**
 * Returns the main parent frame for the component 
 */
public Frame getParentFrame(Component c){

   while(c.getParent()!=null){
       c=c.getParent();
   }
   return (Frame)c;
}


/**
 * Returns the main parent frame for the component
 */
public String printParents(Component c){

   String string="";

   while(c.getParent()!=null){
       string=string+c.getName()+"/";
       c=c.getParent();
   }
   string=string+c.getName();
   return string;
}



}//Utils