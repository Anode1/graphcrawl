/**
 * @(#)OrderedHashtable.java
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
import java.util.Vector;

/**
 * Hashtable having array of keys enumerating them in different orders
 *
 * @author Vasili Gavrilov
 * @version 1.0
 * @since   JDK1.0
 */
public class OrderedHashtable extends Hashtable{

    /**
     * Array of keys
     */
    protected Vector view;

    /**
     * Default constructor
     */
    public OrderedHashtable(){

       super();
       view=new Vector();
    }

    /**
     * Constructor. @see Hashtable
     */
    public OrderedHashtable(int capacity){

       super(capacity);
       view=new Vector(capacity);
    }

    /**
     * Constructor. @see Hashtable
     */
    public OrderedHashtable(int capacity, float factor){

       super(capacity, factor);
       view=new Vector(capacity);
    }

    /**
     * Overwrites default method
     */
    public synchronized Object get(String key){

       Object result=super.get(key);
       return result;
    }

    /**
     * Overwrites default method
     */
    public synchronized Object put(String key, Object value){

      view.addElement(new String(key));
      return super.put(key, value);
    }

    /**
     * Overwrites default method
     */
    public synchronized Object remove(Object key){

      if(!view.removeElement(key)){
         System.err.println("Ordered hashtable::remove:was not removed");
      }
      return super.remove(key);
    }

    /**
     * Overwrites default method
     */
    public synchronized java.util.Enumeration keys(){

	    return view.elements();
    }

    /**
     * Removes a key from the middle of the view and puts it to the top (as the last element)
     */
    public synchronized void putTop(String key){

      for (int i=0; i<view.size(); i++){
          String o=(String)view.elementAt(i);
          if(key.equals(o)){
             view.removeElementAt(i);
             view.addElement(key);
             return;
          }
      }
    }

}