/**
 * @(#)Node.java
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
import java.util.Enumeration;

/**
 * Light node class used in cache
 *
 * @author Vasili Gavrilov
 * @version 1.0
 * @since   JDK1.0
 */
public class Node{
    
    public String id;
    public String label;
    public String url;
    public String category;
    public Vector children;  //id strings for children
    public Vector parents;   //id strings for parents

    /**
     * Default constructor
     */
    public Node(String id, String label, String category, String url, Vector children, Vector parents){

        this.id       = id;
        this.label    = label;
        if(label==null)this.label="";  //extra protection
        this.category = category;
        this.url      = url;
        this.children = children;
        this.parents  = parents;
    }

    /**
     * Gets id of this node
     */
    public String getID(){

       return id;
    }

    /**
     * Gets label of this node
     */
    public String getLabel(){

       return label;
    }

    /**
     * Gets category of this node
     */
    public String getCategory(){

       return category;
    }

    /**
     * Gets URL of this node
     */
    public String getUrl(){

       return url;
    }

    /**
     * Gets all children ids
     */
    public Vector getChildrenIds(){

       return children;
    }

    /**
     * Gets the number of children for this node
     */
    public int getNumChildren(){

       return children.size();
    }

    /**
     * Gets all parents ids
     */
    public Vector getParentsIds(){

       return parents;
    }

    /**
     * Gets the number of parents for this node
     */
    public int getNumParents(){

       return parents.size();
    }

    public boolean equals(Node node){
    
        if(node==null)return false;
        return id.equals(node.id);
    }

/**
 * For debugging purposes
 */
    public String toString(){

        String result=id+";"+label+";"+category+";"+url+";";

        for (Enumeration enum = children.elements() ; enum.hasMoreElements() ;) {
            result+=(String)enum.nextElement();
            if(enum.hasMoreElements())result+=",";
        }
        result+=";";

        for (Enumeration enum = parents.elements() ; enum.hasMoreElements() ;) {
            result+=(String)enum.nextElement();
            if(enum.hasMoreElements())result+=",";
        }
        
        return result;
    }

}