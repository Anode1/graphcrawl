/**
 * @(#)GraphModel.java
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

import java.util.Hashtable;
import java.util.Enumeration;

/**
 * Nodes cache. Has been renewed each request to the server.
 *
 * @author Vasili Gavrilov
 * @version 1.0
 * @since   JDK1.0
 */
public class GraphModel extends Hashtable{

    /**
     * Default constructor
     */
    public GraphModel(){

       super();
    }

    /**
     * See corresponding superclass's constructor
     */
    public GraphModel(int capacity){

       super(capacity);
    }

    /**
     * See corresponding superclass's constructor
     */
    public GraphModel(int capacity, float factor){

       super(capacity, factor);
    }

    /**
     * Returns the node corresponding to some key
     */
    public Node getNode(String key){

       return (Node)get(key);
    }

    /**
     * Adds new node assocoated with key
     */
    public void addNode(String key, Node node){

       put(new String(key),node);
    }

    /**
     * Removes the node corresponding to some key
     */
    public Node removeNode(String key){
    
       return (Node)remove(key);
    }

/**
 * For debugging purposes. Not very efficient: supposed to be used only for contents checking
 */
    public String toString(){

        String result="\r\n";
        for (Enumeration enum = keys() ; enum.hasMoreElements() ;) {
            String aKey=(String)enum.nextElement();
            Object aValue=get(aKey);

            if(aValue instanceof Node){
               Node aNode=(Node)aValue;
               result+=aValue.toString();
            }
            else{
              System.out.println("Cache::print: no LightNode found in cache!");
            }
            result+="\r\n";
        }
        return result;
    }

}