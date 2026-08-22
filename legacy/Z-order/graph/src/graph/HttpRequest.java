/**
 * @(#)HttpRequest.java
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

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * URL url = new URL(getCodeBase(), "/servlet/ServletName");
 * HttpRequest msg = new HttpRequest(url);
 * InputStream in = msg.makeGetRequest(cgiParameters);
 *
 * @author Vasili Gavrilov
 * @version 1.0
 * @since   JDK1.0
 */
public class HttpRequest {

  /**
   * CGI or servlet to ask for information about nodes (nodes tree)
   */
  protected URL cgi;

  /**
   * If true prints additional debugging information
   */
  protected boolean debug;

  /**
   * The constructor
   */
  public HttpRequest(URL cgi){

    this.cgi = cgi;
  }

  /**
   * Sets debug mode
   */
  public void setDebug(boolean debug){

    this.debug=debug;
  }

  /**
   * Makes GET request to servlet or CGI with CGIParameters
   *
   * @return an InputStream to read the response
   * @exception IOException if an I/O error occurs
   */
  public InputStream makeGetRequest(CGIParameters cgiparams) throws IOException {

    String argString = null;

    if(cgiparams != null && cgiparams.size()>0) {
      argString = "?" + cgiparams.getEncoded();
    }
    else argString="";

    URL url = new URL(cgi.toExternalForm() + argString);
    if(debug)System.out.println("GET:"+cgi.toExternalForm() + argString);

    // Turn off caching
    URLConnection con = url.openConnection();
    con.setUseCaches(false);

    return con.getInputStream();
  }

  /**
   * Performs a POST request to servlet or CGI, posting CGIParameters.
   *
   * @return an InputStream to read the response
   * @exception IOException if an I/O error occurs
   */
  public InputStream makePostRequest(CGIParameters cgiparams) throws IOException {

    String argString = null;
    if(cgiparams != null && cgiparams.size()>0) {
      argString = cgiparams.getEncoded();  // notice no "?"
    }
    else argString="";

    URLConnection con = cgi.openConnection();
    if(debug)System.out.println("POST:"+cgi+" with parameters:"+cgiparams.toString());

    con.setDoInput(true);
    con.setDoOutput(true);
    con.setUseCaches(false);

    // Work around a Netscape bug
    con.setRequestProperty("Content-Type",
                           "application/x-www-form-urlencoded");

    DataOutputStream out = new DataOutputStream(con.getOutputStream());
    out.writeBytes(argString);
    out.flush();
    out.close();

    return con.getInputStream();
  }

  /**
   * Performs a POST request to the cgi, uploading a serialized object.
   * <p>
   * The cgi can receive the object in its <tt>doPost()</tt> method
   * like this:
   * <pre>
   *     ObjectInputStream objin =
   *       new ObjectInputStream(req.getInputStream());
   *     Object obj = objin.readObject();
   * </pre>
   * The type of the uploaded object can be retrieved as the subtype of the
   * content type (<tt>java-internal/<i>classname</i></tt>).
   *
   * @param obj the serializable object to upload
   * @return an InputStream to read the response
   * @exception IOException if an I/O error occurs
   */
  public InputStream makePostRequest(Serializable obj) throws IOException {

    URLConnection con = cgi.openConnection();
    if(debug)System.out.println("POST:"+cgi);

    // Prepare for both input and output
    con.setDoInput(true);
    con.setDoOutput(true);

    // Turn off caching
    con.setUseCaches(false);

    // Set the content type to be java-internal/classname
    con.setRequestProperty("Content-Type",
                           "java-internal/" + obj.getClass().getName());

    // Write the serialized object as post data
    ObjectOutputStream out = new ObjectOutputStream(con.getOutputStream());
    out.writeObject(obj);
    out.flush();
    out.close();

    return con.getInputStream();
  }


}
