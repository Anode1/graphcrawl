/**
 * @(#)Sprite.java
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
import java.util.Vector;
import java.applet.*;

/*
 * Universal Sprite object
 *
 * @author Vasili Gavrilov
 * @version 1.0
 * @since   JDK1.0
 */
public class Sprite extends Rectangle{

  /**
   * Velocity of this sprite
   */
  public Point v=new Point(0,0);

  /**
   * where to paint this sprite
   */
  protected Panel panel;

  /**
   * Used when the sprite moves to a boundary and stops on it.
   * This is the boundary defined as a Rectangle.
   */
  protected Rectangle stopBounds;

  /**
   * listeners of this sprite (used when arriving to the center)
   */
  protected Vector listeners = new Vector(1);

  /**
   * True if this sprite is draggable by mouse
   */
  protected boolean draggable=true;

  /**
   * constant used for oval sprites shapes
   */
  public final static int OVAL=0;

  /**
   * constant used for rectangular sprites shapes
   */
  public final static int RECTANGLE=1;

  /**
   * Type of this sprite. Currently supported shapes are: OVAL, RECTANGLE, PICTURE.
   */
  public int type=RECTANGLE;

  /**
   * Images for this sprite. Not used in current implementation
   */
  public Image[] images;

  /**
   * Image for this sprite
   */
  public Image image;

  /**
   * Image height
   */
  public int imageHeight=0;

  /**
   * Image width
   */
  public int imageWidth=0;

  /**
   * Indents
   */
  public static final int indent=2;

  /**
   * default background color if rectangle or oval (not PICTURE type)
   */
  public Color color=Color.cyan;

  /**
   * default border color (if not PICTURE type)
   */
  protected Color borderColor=Color.gray;

  /**
   * Aux coordinates used during drugging
   */
  public int oldx, oldy;

  /**
   * Default constructor
   */
  public Sprite(Panel panel){

    super();
    this.panel=panel;
    width=5;      //for debugging purposes
    height=5;    //for debugging purposes
  }

  /**
   * Sets type of this sprite
   */
  public void setType(int type){

    this.type=type;
  }

  /**
   * Gets the type of this sprite
   */
  public int getType(){

    return type;
  }

  /**
   * Sets default color for this sprite (if type is not PICTURE)
   */
  public void setColor(Color c){

    color = c;
  }

  /**
   * Gets default color
   */
  public Color getColor(){

    return color;
  }  

  /**
   * Sets a velocity of movement for this sprite
   */
  public void setV(int vx, int vy){

    v.x=vx;
    v.y=vy;
  }

  /**
   * Sets a velocity of movement for this sprite
   */
  public void setV(Point p){

    v.x=p.x;
    v.y=p.y;
  }

  /**
   * Sets x
   */
  public void setX(int x){

    this.x=x;
  }

  /**
   * Sets y
   */
  public void setY(int y){

    this.y=y;
  }

  /**
   * Returns the x coordinate of the center of this node view
   */
  public int getMiddleX(){

    return (int)(x+width/2);
  }

  /**
   * Returns y coordinate of the center of this node view
   */
  public int getMiddleY(){

    return (int)(y+height/2);
  }

  /**
   * Overwrites superclass method
   */
  public boolean inside(int X, int Y){

    return super.inside(X, Y);
  }

  /**
   * Returns the center of this node view
   */
  public Point getCenter(){

    return new Point((int)(x+width/2),(int)(y+height/2));
  }

  /**
   * Sets a new center for this node view
   */
  public void setCenter(int centX, int centY){

    this.x=centX-width/2;
    this.y=centY-height/2;
  }

  /**
   * Makes this sprite draggable or not
   */
  public void setDraggable(boolean b) {

    draggable = b;
  }

  /**
   * Returns true if this sprite is draggable
   */
  public boolean isDraggable() {

    return draggable;
  }

  /**
   * Sets image for this Sprite
   */
   public void setImage(Image image){

     this.image=image;
     
     if(image!=null){
        imageWidth=image.getWidth(null);
        imageHeight=image.getHeight(null);
        width=imageWidth;
        height=imageHeight;
     }
   }

  /**
   * Sets a bound where this sprite will stop if it moves
   */
  public void setStopBounds(Rectangle stopBounds){

    this.stopBounds=stopBounds;
    if(stopBounds!=null){   //walk around the situation when rectangle has width or height=0
        stopBounds.x-=1;
        stopBounds.y-=1;
        stopBounds.width+=2;
        stopBounds.height+=2;
    }
  }

  /**
   * Gets a bound where this sprite will stop if it moves
   */
  public Rectangle getStopBounds(){

    return stopBounds;
  }

  /**
   * Standard painting routine
   */
  protected void paint(Graphics g){

    if(image==null){

        Color defaultColor=g.getColor();
        g.setColor(color);               //color for rectangle

        if(type==RECTANGLE){
    	       g.fillRect(x-indent,y,width+indent+indent,height);
	           g.setColor(borderColor);
             g.drawRect(x-indent,y,width+indent+indent,height);
        }
        else{ //OVAL
    	      g.fillOval(x-1-indent,y,width+indent+indent+2,height);
	          g.setColor(borderColor);
            g.drawOval(x-1-indent,y,width+indent+indent+2,height);
        }

        g.setColor(defaultColor);
    }
    else{ //image not null
        //background:
        Color prevColor=g.getColor();
        g.setColor(Color.white);

        int imx=this.getMiddleX()-5;
        int imy=y+3;

    	  g.fillRect(imx,imy,imageWidth,imageHeight); //white background for icon
 	      //g.fillRect(x-2,y+15,width+4,height); //white background for text
        g.setColor(prevColor);
        g.drawImage(image,imx,imy,panel);
    }

  }

  /**
   * Standard update routine. Updates sprite's position if it moves
   * (has velocity the magnitude which is not 0)
   */
  public void update(){

  //  if(v.x!=0){
      int temp=indent;
      x+=v.x;
      if(x<temp){
        x=temp;
        v.x=-v.x;   //reflection from the boundary
      }
      temp=panel.size().width-width-indent-indent+1;
      if(x>temp){
        x=temp;
        v.x=-v.x;
      }
  //  }
  //  if(v.y!=0){
      temp=indent;
      y+=v.y;
      if(y<temp){
        y=temp;
        v.y=-v.y;
      }
      temp=panel.size().height-height-indent-indent+1;
      if(y>temp){
        y=temp;
        v.y=-v.y;
      }

  //  }
  }//update

}