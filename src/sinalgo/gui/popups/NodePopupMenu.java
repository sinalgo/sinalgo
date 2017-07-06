/*
 Copyright (c) 2007, Distributed Computing Group (DCG)
                    ETH Zurich
                    Switzerland
                    dcg.ethz.ch

 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 - Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the
   distribution.

 - Neither the name 'Sinalgo' nor the names of its contributors may be
   used to endorse or promote products derived from this software
   without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package sinalgo.gui.popups;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import javax.swing.JMenuItem;

import sinalgo.configuration.Configuration;
import sinalgo.gui.GUI;
import sinalgo.gui.dialogs.NodeInfoDialog;
import sinalgo.nodes.Node;
import sinalgo.runtime.Main;
import sinalgo.runtime.Runtime;


/**
 * The class for the popupmenus displayed on the graph panel when the user presses the right mouse button
 * over a node.
 */
@SuppressWarnings("serial")
public class NodePopupMenu extends AbstractPopupMenu implements ActionListener{

	private HashMap<String, Method> methodsAndDescriptions = new HashMap<String, Method>();
	
	private Node node = null;
	private JMenuItem info = new JMenuItem("Info");
	private JMenuItem delete = new JMenuItem("Delete Node");
	private JMenuItem showCoordinateCube = new JMenuItem("Show coordinate cube");
	private JMenuItem hideCoordinateCube = new JMenuItem("Hide coordinate cube");
	
	/**
	 * The constructor for the NodePopupMenu class.
	 *
	 * @param p The parent gui, where the popupMenu appears in.
	 */
	public NodePopupMenu(GUI p){
		parent = p;
		info.addActionListener(this);
		delete.addActionListener(this);
		showCoordinateCube.addActionListener(this);
		hideCoordinateCube.addActionListener(this);
	}
	
	/**
	 * This method composes the popupmenu for the given node.
	 *
	 * @param n The node the popupmenu is about.
	 */
	public void compose(Node n){
		
		node = n;
		
		methodsAndDescriptions.clear();
		this.removeAll();
		
		this.add(info);

		if(Configuration.dimensions == 3) {
			if(parent.getGraphPanel().containsNodeToDrawCoordinateCube(n)) {
				this.add(hideCoordinateCube);
			} else {
				this.add(showCoordinateCube);
			}
		}
		
		this.add(delete);
		
		this.addSeparator();
		
		JMenuItem dummy = new JMenuItem("No Methods specified");
		dummy.setEnabled(false);
		
		boolean customMethods = false;
		
		Method[] methods = node.getClass().getMethods();
		for(int i = 0; i < methods.length; i++){
			Method method = methods[i];
			Node.NodePopupMethod info = method.getAnnotation(Node.NodePopupMethod.class);
			if(info != null){
				String text = n.includeMethodInPopupMenu(method, info.menuText());
				if(text == null) { // the user dismissed this menu item
					continue;
				}
				JMenuItem item = new JMenuItem(text);
				item.addActionListener(this); // BUGFIX for 0.75.0 -> 0.75.1 : this line was missing
				this.add(item);
				customMethods = true;
				methodsAndDescriptions.put(text, method); // BUGFIX: 1st parameter was info.menuText() 
			}
		}
		
		if(!customMethods){
			this.add(dummy);
		}
		
		this.addSeparator();
		
		this.add(zoomIn);
		this.add(zoomOut);
	}
	
	public void actionPerformed(ActionEvent event) {
		if(event.getActionCommand().equals(info.getActionCommand())){
			new NodeInfoDialog(parent, node); 
		}	else if(event.getActionCommand().equals(delete.getActionCommand())){
			Runtime.removeNode(node);
			parent.redrawGUI();
		} else if(event.getActionCommand().equals(showCoordinateCube.getActionCommand())) {
			parent.getGraphPanel().setNodeToDrawCoordinateCube(node);
			parent.repaint(); // need not repaint the graph, only the toppings
		}  else if(event.getActionCommand().equals(hideCoordinateCube.getActionCommand())) {
			parent.getGraphPanel().removeNodeToDrawCoordinateCube(node);
			parent.repaint(); // need not repaint the graph, only the toppings
		}	else {
			// try to execute a custom-command
			Method clickedMethod = methodsAndDescriptions.get(event.getActionCommand());
			if(clickedMethod == null) {
				Main.fatalError("Cannot find method associated with menu item " + event.getActionCommand());
				return;
			}
			try{
				synchronized(parent.getTransformator()){
					//synchronize it on the transformator to grant not to be concurrent with
					//any drawing or modifying action
					clickedMethod.invoke(node, new Object[0]);
				}
			}	catch(InvocationTargetException e) {
				String text = "";
				if(null != e.getCause()) {
					text = "\n\n" + e.getCause() + "\n\n" + e.getCause().getMessage();
				}
				Main.minorError("The method '" + clickedMethod.getName() +
				                "' has thrown an exception and did not terminate properly:\n" + e + text);
			} catch (IllegalArgumentException e) {
				Main.minorError("The method '" + clickedMethod.getName() + 
				                "' cannot be invoked without parameters:\n" + e + "\n\n" + e.getMessage());
			} catch (IllegalAccessException e) {
				Main.minorError("The method '" + clickedMethod.getName() + 
				                "' cannot be accessed:\n" + e + "\n\n" + e.getMessage());
			}
			
			parent.redrawGUI();
		}
	}
}
