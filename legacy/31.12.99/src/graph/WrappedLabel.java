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

    /**
     * Actual width of label
     */
    public int width=5;

    /**
     * Maximal width
     */
    public static final int maxWidth=50;

    /**
     * Label height
     */
    public static int row_height=5;

    /**
     * Font height
     */
    public int height=row_height;

    /**
     * Label font descent
     */
    public int descent;

    /**
     * Indents
     */
    public static final int indent=0;

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
     * Default constructor
     */
    public WrappedLabel(Panel panel, String string){

        this.panel=panel;
        this.string=string;
    }

    /**
     * Sets the label string
     */
    public void setString(String string){

        this.string=string;
    }

    /**
     * Gets the label string
     */
    public String getString(){

        return string;
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

    public void resetSize(Graphics g){

        FontMetrics fm=g.getFontMetrics();
        descent=fm.getDescent();
        row_height=fm.getHeight();
        height=row_height;
        width=fm.stringWidth(string);

        if(width<maxWidth){
           strings=new String[1];
           x=new int[1];
           y=new int[1];
           x[0]=-width/2;
           y[0]=row_height-descent;
           strings[0]=string;
        }
        else{
           StringTokenizer st=new StringTokenizer(string);
           int N=st.countTokens();
           strings=new String[N];
           x=new int[N];
           y=new int[N];
           width=0;
           height=0;
           for(int i=0; st.hasMoreTokens(); i++){
              strings[i]=st.nextToken();
              int w=fm.stringWidth(strings[i]);
              if(w>width)width=w;  //take maximal string
              height+=row_height;
              x[i]=-w/2;
              y[i]=i*row_height+row_height-descent;
           }
         }
    }

    /**
     * Standard paint routine
     */
    public void paint(Graphics g, int midX, int y0){

        for(int i=0; i<strings.length; i++){
           g.drawString(strings[i], midX+x[i], y0+y[i]);
        }

    }


    public void paintAsAnnotation(Graphics g, int midX, int y0){

        Color prevColor=g.getColor();
        g.setColor(Color.white);
        g.fillRect(midX-width/2-indent, y0+1, width+indent+indent, height);
        g.setColor(prevColor);
        paint(g, midX, y0);

    }//paintNode


}