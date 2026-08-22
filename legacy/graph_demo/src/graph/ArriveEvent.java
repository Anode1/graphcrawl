/**
 * @(#)ArriveEvent.java
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
 * Event fired by Node when it comes to desination and notifies by this event
 * all registered listeners
 *
 * @author Vasili Gavrilov
 * @version 1.0
 * @see     ArriveEventListener
 * @since   JDK1.0
 */
public class ArriveEvent extends java.util.EventObject  {

    /**
     * The constructor
     */
    public ArriveEvent(NodeView node){
    
       super(node) ;
    }

}