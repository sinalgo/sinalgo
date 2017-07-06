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


import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

import sinalgo.gui.GUI;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.nodes.Position;

/**
 * The class for the popupmenus displayed on the graph panel when the user presses the right mouse button
 * over a place, where there is no node and no edge.
 */
@SuppressWarnings("serial")
public class SpacePopupMenu extends AbstractPopupMenu implements ActionListener{
	
	private Point pos = null;
	
	private JMenuItem add = new JMenuItem("Add Node");
	
	/**
	 * The constructor for the SpacePopupMenu class.
	 *
	 * @param p The Frame to add the AddNodeDialog to if the user clicked AddNode.
	 */
	public SpacePopupMenu(GUI p){
		parent = p;
		add.addActionListener(this);
	}
	
	/**
	 * This method composes a Popupmenu for the given position.
	 *
	 * @param p The position the user clicked to.
	 */
	public void compose(Point p){
		pos = p;
		
		this.removeAll();
		
		if(parent.getTransformator().supportReverseTranslation()){
			this.add(add);
			this.addSeparator();
		}

		this.add(zoomIn);
		this.add(zoomOut);
	}
	
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals(add.getActionCommand())){
			PositionTransformation pt = parent.getTransformator(); 
			if(pt.supportReverseTranslation()) {
				pt.translateToLogicPosition(pos.x, pos.y);
				parent.addSingleNode(new Position(pt.logicX, pt.logicY, pt.logicZ));
			}
		}
	}
}