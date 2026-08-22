/**
 * @(#)CGIParameters.java
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
import java.net.*;

/**
 * General CGI parameters class. 
 * Supports arrays (nultiple values for one key).
 *
 * @author Vasili Gavrilov
 * @version 1.0
 * @since   JDK1.0
 */
public class CGIParameters extends Hashtable{

        /**
         * Default constructor
         */
        public CGIParameters(){
           super();
        }

        /**
         * See corresponding superclass constructor
         */
        public CGIParameters(int capacity){
          super(capacity);
        }

        /**
         * See corresponding superclass constructor
         */
        public CGIParameters(int capacity, float factor){

          super(capacity, factor);
        }

        /**
         * Adds one parameter value
         */
        public void addParam(String key, String value){

            if(key==null){
              System.err.println("Parameters::addParam:You are trying to set null key for a parameter!");
              return;
            }
            if(value==null){
              System.err.println("Parameters::addParam:You are trying to send null value for key:"+key);
              return;
            }
            Object existing=this.get(key);
            if(existing==null){//didn't exist before
                put(new String(key), new String(value));
            }
            else if(existing instanceof Vector){
               Vector existingVector=(Vector)existing;
               existingVector.addElement(new String(value));
            }
            else if(existing instanceof String){ //change to Vector
               Vector newVector=new Vector(3);
               newVector.addElement((String)existing);
               newVector.addElement(new String(value));
               put(new String(key), newVector); //overwrite String
            }
        }

        /**
         * Adds all parameters from another CGIParameters class to this class
         */
        public void addParams(CGIParameters anotherParms){

          for (Enumeration enum = anotherParms.keys(); enum.hasMoreElements() ;) {
              String aKey=(String)enum.nextElement();
              Object aValue=anotherParms.get(aKey);
              addParam(aKey,aValue);
          }
        }

        /**
         * Convenience method when we do not know in advance what type will bve passed
         */
        public void addParam(String key, Object o){

            if(o==null)return;
            if(o instanceof String)addParam(key,(String)o);
            else if(o instanceof Vector)addParam(key,(Vector)o);
            else System.err.println("Parameters::addParam: trying to add unsupported type of param: only String and Vector are allowed");
        }

        /**
         * Adds string array parameter
         */
        public void addParam(String key, Vector vector){

            if(key==null){
              System.err.println("Parameters::addParam:You are trying to set null key for a vector parameter!");
              return;
            }
            if(vector==null){
              System.err.println("Parameters::addParam:You are trying to send null value for key:"+key);
              return;
            }

            Object existing=get(key);
            if(existing==null){//didn't exist before
                put(new String(key), vector);
            }
            else if(existing instanceof Vector){
               Vector v=(Vector)existing;
               for (Enumeration enum = vector.elements() ; enum.hasMoreElements() ;) {
                 String elem=(String)enum.nextElement();
                 if(elem!=null)v.addElement(new String(elem));
               }
            }
            else if(existing instanceof String){ //change to Vector
               String ex=(String)existing;
               vector.addElement(new String(ex));
               put(new String(key), vector); //overwrite String
            }

        }

        /**
         * Removes parameter
         */
        public Object removeParam(String key){

            return remove(key);
        }

        /**
         * Returns all parameters in the URLEncoded form String
         */
        public String getEncoded(){

          StringBuffer buf = new StringBuffer();
          for (Enumeration enum = this.keys(); enum.hasMoreElements() ;) {
              String aKey=(String)enum.nextElement();
              Object aValue=this.get(aKey);

              if(aValue instanceof String){ //change to Vector
                buf.append(URLEncoder.encode(aKey) + "=" + URLEncoder.encode((String)aValue));
              }
              else if(aValue instanceof Vector){
                Vector v=(Vector)aValue;
                for (Enumeration vals = v.elements() ; vals.hasMoreElements() ;) {
                  String elem=(String)vals.nextElement();
                  if(elem!=null){
                    buf.append(URLEncoder.encode(aKey) + "=" + URLEncoder.encode(elem));
                  }
                  if(vals.hasMoreElements())buf.append("&");
                }
              }
              if(enum.hasMoreElements())buf.append("&");
          }
          return buf.toString();
        }

/**
 * For debugging purposes only. Otherwise use getEncoded()
 */
        public String toString(){

          String result="";
          for (Enumeration enum = this.keys(); enum.hasMoreElements() ;) {
              String aKey=(String)enum.nextElement();
              Object aValue=get(aKey);
              result+=aKey.toString();
              if(aValue==null){
                result+="=null;";
              }
              else if(aValue instanceof String){
                result+="="+aValue.toString()+";";
              }
              else if(aValue instanceof Vector){
                result+="="+vector2String((Vector)aValue)+";";
              }
          }
          return result;
        }

/**
 * Used by toString()
 */
        private String vector2String(Vector v){
        
          String result="";
          if(v==null || v.size()==0)return "[]";
          result+="[";
          for (Enumeration enum = v.elements(); enum.hasMoreElements() ;) {
              result += (String)enum.nextElement();
              if(enum.hasMoreElements())result += ";";
          }
          result+="]";
          return result;
        }


}