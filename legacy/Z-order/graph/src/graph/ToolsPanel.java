/**
 * @(#)ToolsPanel.java
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

import java.awt.Checkbox;
import java.awt.Panel;
import java.awt.Choice;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Event;
import java.awt.Color;
import java.awt.Insets;
import java.io.PrintStream;
import java.util.Enumeration;

import NavigatorApplet;

/**
 * Main ToolBar appearing at the bottom of the applet
 *
 * @author Vasili Gavrilov
 * @version 1.0
 * @since   JDK1.0
 */
public class ToolsPanel extends Panel{

    protected Choice depth_choice, history_choice;

  //  protected Checkbox rollover_checkbox;

    /**
     * Ref to the applet
     */
    protected NavigatorApplet applet;

    /**
     * The nodes history
     */
    protected History history;

    /**
     * Default constructor
     */
    public ToolsPanel(NavigatorApplet applet){

        history = new History();
        this.applet = applet;

        setBackground(Color.lightGray);
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

//depth label:
        Label depth_label = new Label("Tree Depth");
        c.gridx = 0;
        c.gridy = 0;
        c.fill = 0;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 0);
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(depth_label, c);
        add(depth_label);

//depth_choice:
        depth_choice = new Choice();
        depth_choice.addItem("2");
        depth_choice.addItem("3");
        c.gridx = 1;
        c.gridy = 0;
        c.fill = 0;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 0, 5, 5);
        c.anchor =  GridBagConstraints.WEST;
        gridbag.setConstraints(depth_choice, c);
        add(depth_choice);

//history_label:
        Label history_label = new Label("History");
        c.gridx = 2;
        c.gridy = 0;
        c.fill = 0;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 0);
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(history_label, c);
        add(history_label);

//history_choice:
        history_choice = new Choice();
        history_choice.resize(100, 20);
        c.gridx = 3;
        c.gridy = 0;
        c.fill = 2;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 0, 5, 5);
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(history_choice, c);
        add(history_choice);

//rollover_checkbox:
/*
        rollover_checkbox = new Checkbox("Rollover");
        c.gridx = 4;
        c.gridy = 0;
        c.fill = 0;
        c.weightx = 0.0D;
        c.weighty = 0.0D;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(rollover_checkbox, c);
        rollover_checkbox.setState(true);
        add(rollover_checkbox);
*/
        setVisible(true);
    }

    /**
     * Adds a node to the history
     */
    public void addHistoryNode(Node node){
    
        if(history.getNode(node.getID()) != null){ //already in history
            return;
        }
        else{
            history_choice.addItem(new String(node.getLabel()));
            history.addNode(new String(node.getID()), node);
            return;
        }
    }

    /**
     * Actions handler
     */
    public boolean action(Event e, Object arg){

        if(e.target == depth_choice){
            int depth = 2;
            String item = ((Choice)e.target).getSelectedItem();
            try{
                depth = Integer.parseInt(item);
            }
            catch(NumberFormatException nfe){
                System.out.println(nfe);
                boolean flag = true;
                return flag;
            }
            applet.getGraphView().setDepth(depth);
        }
        /*
        else if(e.target == rollover_checkbox){
          //System.out.println(e.);
        }
        */
        else if(e.target == history_choice){
            String itemInChoice = ((Choice)e.target).getSelectedItem().trim();
            Enumeration keys = history.keys();
            do{
                if(!keys.hasMoreElements())
                    break;
                String key = (String)keys.nextElement();
                Node node = (Node)history.get(key);
                if(itemInChoice.equals(node.getLabel().trim())){

                  //  if(applet.getGraphView().getState() != 0){
                   applet.getGraphView().selectNode(node);
                   applet.showUrl(node); 
                        //System.out.println(node.id);
                   // }
                }
            }
            while(true);
        }
        return true;
    }


}
