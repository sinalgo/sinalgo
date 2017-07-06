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

import javax.swing.JMenuItem;

import sinalgo.gui.GUI;
import sinalgo.gui.dialogs.EdgeInfoDialog;
import sinalgo.nodes.edges.Edge;
import sinalgo.runtime.Runtime;

/**
 * The PopupMenu which pops up upon a right-click on an Edge
 */

@SuppressWarnings("serial")
public class EdgePopupMenu extends AbstractPopupMenu implements ActionListener {

	private Edge edge = null;
	private JMenuItem info = new JMenuItem("Info");
	private JMenuItem delete = new JMenuItem("Delete");
	
	/**
	 * The constructor for the EdgePopupMenu class.
	 * 
	 * @param p The parent GUI used to trigger the zooming.
	 */
	public EdgePopupMenu(GUI p){
		parent = p;
		info.addActionListener(this);
		delete.addActionListener(this);
	}
	
	/**
	 * The method to generate an EdgeInformationDialog for the specified Edge.
	 * 
	 * @param e The edge the information is about.
	 */
	public void compose(Edge e){
		edge = e;
		
		this.removeAll();
		
		
		this.add(info);
		this.add(delete);
		this.addSeparator();
		
		this.add(zoomIn);
		this.add(zoomOut);
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals(info.getActionCommand())){
			new EdgeInfoDialog(parent, edge);
		}
		else if(e.getActionCommand().equals(delete.getActionCommand())){
			Runtime.removeEdge(edge);
			parent.redrawGUINow();
		}
	}
}
