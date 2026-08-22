/**
 * @(#)GrayFilter.java
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

/**
 * Grey filter
 *
 * <p>
 * General usage:
 * <blockquote><pre>
 *    ImageFilter filter=new GrayFilter();
 *    ImageProducer producer=new FilteredImageSource(oldImage.getSource(), filter);
 *    Image newImage=this.createImage(producer);
 * </pre></blockquote>
 * <p>
 *
 * @see RGBImageFilter
 * @author Vasili Gavrilov
 * @version 1.0
 * @since   JDK1.0
 */
public class GrayFilter extends java.awt.image.RGBImageFilter{

    /**
     * Constructor
     */
    public GrayFilter(){

      canFilterIndexColorModel=true;
    }

    /**
     * @see RGBImageFilter
     */
    public int filterRGB(int x,int y, int rgb){
    
        int a=rgb & 0xff000000;
        int r=(((rgb & 0xff0000)+0x018000)/3) & 0xff0000;
        int g=(((rgb & 0x00ff00)+0x018000)/3) & 0x00ff00;
        int b=(((rgb & 0x0000ff)+0x000180)/3) & 0x0000ff;
        return a|r|g|b;
    }
}


