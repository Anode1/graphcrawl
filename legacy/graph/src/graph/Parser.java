/**
 * @(#)Parser.java
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
import java.util.NoSuchElementException;

/**
 * Lines parser. Used in Communicator
 *
 * @author Vasili Gavrilov
 * @version 1.0
 * @since   JDK1.0
 */
public class Parser {

 /**
  * Graph model this parser constructs from the data
  */
 protected GraphModel graph;

 /**
  * currently parsed line
  */
 protected int currentLine=0;

 /**
  * Default constructor
  */
 public Parser(){

    graph=new GraphModel(100);
 }

/**
 * Parses one line and adds newly constructed LightNode into nodesCache
 */
 public void parseLine(String line){

      try{
          currentLine++;

          Vector children=new Vector();
          Vector parents=new Vector(1);

          int i=line.indexOf(';');
          String id=line.substring(0,i);
          String stringWithoutId=new String(line.substring(i+1));

          //skip "\;":

          i=stringWithoutId.indexOf(';');
          boolean needToRemove=false;
          while(i>0 && (stringWithoutId.charAt(i-1)=='\\')){
             i=stringWithoutId.indexOf(';',i+1);
             needToRemove=true;
          }

          String label=stringWithoutId.substring(0,i);
          if(needToRemove){
            label=decode(label);
          }

          String stringWithoutLabel=new String(stringWithoutId.substring(i+1));

          i=stringWithoutLabel.indexOf(';');
          String category=stringWithoutLabel.substring(0,i);
          String stringWithoutCategory=new String(stringWithoutLabel.substring(i+1));

          i=stringWithoutCategory.indexOf(';');
          String value=stringWithoutCategory.substring(0,i);
          String stringWithoutValue=new String(stringWithoutCategory.substring(i+1));

          //System.out.println("<"+id+"/"+label+"/"+value+"/");

          //System.out.println("stringWithoutValue:"+stringWithoutValue);
          //rest may be:
          //;      - no children,no parents
          //5,8;
          //34;    - no parents
          //;4,6
          //;3     - no children
          //12,5;53,76
          //1;77

          StringTokenizer st1=null;
          int indexOfseparator=stringWithoutValue.indexOf(';');
          if(indexOfseparator>0){
            try{
              String childrenString=stringWithoutValue.substring(0, indexOfseparator);
              if(childrenString.length()>0){
                st1=new StringTokenizer(childrenString+",", ",");
                while(st1.hasMoreTokens()){
                  String aChild=st1.nextToken().trim();
                  if(aChild!=null && aChild.length()>0){
                       //System.out.print(aNeighbour+" ");
                       children.addElement(aChild);
                  }
                }
              }
            }catch(NoSuchElementException nse){}
          }
                //System.out.print("parents for id="+id+":");
          String parentsString=stringWithoutValue.substring(indexOfseparator+1).trim();
          try{
              if(parentsString.length()>0){
                   st1=new StringTokenizer(parentsString+",", ",");
                   while(st1.hasMoreTokens()){
                       String aParent=st1.nextToken().trim();
                       if(aParent.length()>0){
                          //System.out.print(aNeighbour+" ");
                           parents.addElement(aParent);
                       }
                    }
               }
          }catch(NoSuchElementException nse){
          }

          //System.out.println("");
          Node currentNode=new Node(id,label,category,value,children,parents);
	        graph.addNode(new String(id),currentNode);
	        //if(parentID.equals(id))applet.prevParentNode=currentNode;
	    }
      catch(Exception e){//not valid line
          System.out.println("Parser::error in line "+currentLine+":"+e+" the line:"+line);
      }
 }

 /**
  * Returns newly constructed graph model
  */
 public GraphModel getGraphModel(){

    return graph;
 }

 /**
  * Removes '\' characters (in labels)
  */
 public static String decode(String in){

    StringBuffer sb = new StringBuffer();
    int next = 0;
    int prev = 0;

    while((next = in.indexOf("\\", next)) >= 0){
          sb.append(in.substring(prev, next) /*+ "x"*/);
          next++;
          prev = next;
    }
    sb.append(in.substring(prev));

    return sb.toString();
 }

}