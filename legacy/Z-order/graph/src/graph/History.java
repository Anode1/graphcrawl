/**
 * @(#)History.java
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

import java.util.*;

/**
 * Nodes history
 *
 * @author Vasili Gavrilov
 * @version 1.0
 * @since   JDK1.0
 */
public class History extends java.util.Hashtable{

    /**
     * Default constructor
     */
    public History(){

       super();
    }

    /**
     * See corresponding superclass's constructor
     */
    public History(int capacity){

       super(capacity);
    }

    /**
     * See corresponding superclass's constructor
     */

    public History(int capacity, float factor){

       super(capacity, factor);
    }

    /**
     * Returns a node by the key
     */
    public Node getNode(String key){

      return (Node)get(key);
    }

    /**
     * Adds a node assotiating a key with it
     */
    public void addNode(String key, Node node){

      put(new String(key),node);
    }

    /**
     * Removes the node specified by the key
     */
    public Node removeNode(String key){

      return (Node)remove(key);
    }

/**
 * For debugging purposes only
 */
    public String print(){
    
        String result="\r\n";
        for (Enumeration enum = keys() ; enum.hasMoreElements() ;) {
            String aKey=(String)enum.nextElement();
            Object aValue=get(aKey);

            if(aValue instanceof Node){
               Node aNode=(Node)aValue;
               result+=aValue.toString();
            }
            else{
              System.out.println("Cache::print: not LightNode found in cache!");
            }
            result+="\r\n";
        }
        return result;
    }

}