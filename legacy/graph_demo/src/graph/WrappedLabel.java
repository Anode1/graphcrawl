/**
 * @(#)WrappedLabel.java
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

import java.util.Vector;
import java.util.StringTokenizer;
import java.awt.Panel;
import java.awt.FontMetrics;
import java.awt.Color;
import java.awt.Graphics;

/**
 * Wrapped label (as in Windows)
 *
 * @author Vasili Gavrilov
 * @version 1.0
 * @since   JDK1.0
 */
public class WrappedLabel{

    /**
     * where to paint this label
     */
    protected Panel panel;

    protected int x0;

    protected int y0;

    /**
     * Actual width of label
     */
    public int width=5;

    /**
     * Label height
     */
    public static int row_height=5;

    /**
     * Ref to actual FontMetrics
     */
    protected FontMetrics fm;

    /**
     * Font height
     */
    public int height=row_height;

    /**
     * Ref to short label width defined in superclass
     */
    protected int shortLabelWidth;

    /**
     * Label font descent
     */
    public int descent;

    /**
     * True if we have to recalculate width each time (if actual width of the
     * label is longer than maximal)
     */
    protected boolean dynamic;

    /**
     * Indents
     */
    public static final int indent=2;

    /**
     * The whole string
     */
    protected String string;

    /**
     * Array of strings
     */
    protected String[] strings;

    /**
     * x coordinates for strings
     */
    protected int[] x;

    /**
     * y coordinates for strings
     */
    protected int[] y;

    /**
     * Background color. If null than transparent.
     */
    protected static Color bgColor=Color.white;

    /**
     * Default constructor
     */
    public WrappedLabel(Panel panel, String string){

        this.panel=panel;
        this.string=string;
    }

    /**
     * Returns width
     */
    public int getWidth(){

        return width;
    }

    /**
     * Returns height
     */
    public int getHeight(){

        return height;
    }

    /**
     * Returns descent
     */
    public int getDescent(){

        return descent;
    }

    /**
     * get background Color
     */
    public Color getBGColor(){

        return bgColor;
    }

    /**
     * Sets background Color
     */
    public void setBGColor(Color bgColor){

        this.bgColor=bgColor;
    }

    /**
     * Returns true if this wrapped label has been initialized already
     */
    public boolean isInitialized(){

        return fm!=null;
    }

    /**
     * Resets the size of the label according to the current Graphics context
     * properties (being called from paint routine) 
     */
    public void init(FontMetrics fm, int row_height, int descent, int shortLabelWidth){

        this.fm=fm;
        this.row_height=row_height;
        this.descent=descent;
        this.shortLabelWidth=shortLabelWidth;
        int longLabelWidth=fm.stringWidth(string);

        if(longLabelWidth>shortLabelWidth){   //the same as shortLabel (static width)
           dynamic=true;
        }
        else{                  //dynamic width
           dynamic=false;
           height=row_height;
           width=longLabelWidth;
        }
    }

    /**
     * Main string dividing routine. Determined coordinates of substrings.
     * Called by NodeView when it has been moved (dragged to a new place or
     * after creating)
     */
    public void reset(int x0, int y0){

           //System.out.println("reset");

           if(x0>=indent) this.x0=x0;
           else this.x0=indent;

           this.y0=y0;

           if(!dynamic){
              //correct x if needed
              int rightBorder=panel.size().width;
              if(this.x0 > rightBorder-width-indent-indent+1){
                 this.x0 = rightBorder-width-indent-indent+1;
              }
              return;
           }

           StringTokenizer st=new StringTokenizer(string);
           int numWords=st.countTokens();
           String[] words=null;
           words=new String[numWords];
           
           //populate array of words:
           for(int i=0; i<numWords; i++){
              words[i]=st.nextToken();
           }

           int rightBorder=panel.size().width;
           int maxWidth=rightBorder-shortLabelWidth-indent-indent;

           //determine number of lines:
           int numberLines=0;
           String curLine="";
           for(int i=0; i<numWords;){
              numberLines++;
              while(fm.stringWidth(curLine)<=maxWidth){
                curLine+=words[i]+" ";
                i++;
                if(i==numWords)break;
              }
              curLine=new String("");
           }

           x=new int[numberLines];
           y=new int[numberLines];
           strings=new String[numberLines];

           //populate array:
           int curlineNum=0;
           curLine=new String("");
           for(int i=0; i<numWords;){
              while(fm.stringWidth(curLine)<=maxWidth){
                curLine+=words[i]+" ";
                i++;
                if(i==numWords)break;
              }
              strings[curlineNum]=curLine;
              curlineNum++;
              curLine=new String("");
           }

           width=0;
           height=0;

           //determine max width and height:
           for(int i=0; i<numberLines; i++){
              int w=fm.stringWidth(strings[i]);
              if(w>width)width=w;  //take maximal string
              height+=row_height;
              x[i]=this.x0;
              y[i]=y0+i*row_height+row_height-descent;
           }


           //correct x if needed
           if(this.x0 > rightBorder-width-indent-indent+1){
              this.x0 = rightBorder-width-indent-indent+1;
              for(int i=0; i<numberLines; i++){
                 x[i]=this.x0;
              }
           }

    }

    /**
     * Standard paint routine
     */
    public void paint(Graphics g){

        Color prevColor=g.getColor();
        if(bgColor!=null){
           g.setColor(bgColor);
        }
        g.fillRect(x0-indent, y0, width+indent+indent, height);
        g.setColor(Color.black);
        g.drawRect(x0-indent, y0, width+indent+indent, height);

        g.setColor(prevColor);

        if(dynamic){
          for(int i=0; i<strings.length; i++){
            g.drawString(strings[i], x[i], y[i]);
          }
        }
        else{   //static
            g.drawString(string, x0, y0+row_height-descent);
        }


    }



}
