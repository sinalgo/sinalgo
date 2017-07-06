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
package projects.sample2.nodes.nodeImplementations;


import java.awt.Color;
import java.awt.Graphics;
import java.lang.reflect.Method;
import java.util.TreeSet;

import sinalgo.configuration.WrongConfigurationException;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.io.eps.EPSOutputPrintStream;
import sinalgo.nodes.Node;
import sinalgo.nodes.edges.Edge;
import sinalgo.nodes.messages.Inbox;
import sinalgo.runtime.Runtime;
import sinalgo.tools.Tools;


/**
 * The class to simulate the sample2-project.
 */
public class S2Node extends Node implements Comparable<S2Node> {

	private static int maxNeighbors = 0; // global field containing the max number of neighbors any node ever had
	
	private boolean isMaxNode = false; // flag set to true when this node has most neighbors
	private boolean drawAsNeighbor = false; // flag set by a neighbor to color specially
	
	// The set of nodes this node has already seen
	private TreeSet<S2Node> neighbors = new TreeSet<S2Node>();
	
	/**
	 * Reset the list of neighbors of this node.
	 */
	public void reset() {
		neighbors.clear();
	}
	
	@Override
	public void checkRequirements() throws WrongConfigurationException {
	}

	@Override
	public void handleMessages(Inbox inbox) {
	}

	@Override
	public void init() {
	}

	@Override
	public void neighborhoodChange() {
		for(Edge e : this.outgoingConnections){
			neighbors.add((S2Node) e.endNode); // only adds really new neighbors
		}
	}

	@Override
	public void preStep() {
		// color this node specially when it has most neighbors
		if(this.neighbors.size() >= S2Node.maxNeighbors) {
			S2Node.maxNeighbors = this.neighbors.size();
			this.isMaxNode = true;
		} else {
			this.isMaxNode = false;
		}
	}

	@Override
	public void postStep() {
	}
	
	private static boolean isColored = false;
	
	/**
	 * Colors all the nodes that this node has seen once.
	 */
	@NodePopupMethod(menuText="Color Neighbors")
	public void ColorNeighbors(){
		for(S2Node n : neighbors) {
			n.drawAsNeighbor = true;
		}
		isColored = true;
		// redraw the GUI to show the neighborhood immediately
		if(Tools.isSimulationInGuiMode()) {
			Tools.repaintGUI();
		}
	}
	
	/**
	 * Resets the color of all previously colored nodes.
	 */
	@NodePopupMethod(menuText="Undo Coloring")
	public void UndoColoring() { // NOTE: Do not change method name!
		// undo the coloring for all nodes
		for(Node n : Runtime.nodes){
			((S2Node) n).drawAsNeighbor = false;
		}
		isColored = false;
		// redraw the GUI to show the neighborhood immediately
		if(Tools.isSimulationInGuiMode()) {
			Tools.repaintGUI();
		}
	}

	/* (non-Javadoc)
	 * @see sinalgo.nodes.Node#includeMethodInPopupMenu(java.lang.reflect.Method, java.lang.String)
	 */
	public String includeMethodInPopupMenu(Method m, String defaultText) {
		if(!isColored && m.getName().equals("UndoColoring")) {
			return null; // there's nothing to be undone
		}
		return defaultText;
	}
	

	@Override
	public String toString() {
		return "This node has seen "+neighbors.size()+" neighbors during its life.";
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.nodes.Node#draw(java.awt.Graphics, sinalgo.gui.transformation.PositionTransformation, boolean)
	 */
	public void draw(Graphics g, PositionTransformation pt, boolean highlight) {
		// Set the color of this node depending on its state
		if(isMaxNode) {
			this.setColor(Color.RED);
		} else if(drawAsNeighbor) {
			this.setColor(Color.BLUE);
		} else {
			this.setColor(Color.BLACK);
		}
		double fraction = Math.max(0.1, ((double) neighbors.size()) / Tools.getNodeList().size());
		this.drawingSizeInPixels = (int) (fraction * pt.getZoomFactor() * this.defaultDrawingSizeInPixels);
		drawAsDisk(g, pt, highlight, this.drawingSizeInPixels);
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.nodes.Node#drawToPostScript(sinalgo.io.eps.EPSOutputPrintStream, sinalgo.gui.transformation.PositionTransformation)
	 */
	public void drawToPostScript(EPSOutputPrintStream pw, PositionTransformation pt) {
		// the size and color should still be set from the GUI draw method
		drawToPostScriptAsDisk(pw, pt, drawingSizeInPixels/2, getColor());
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(S2Node tmp) {
		if(this.ID < tmp.ID) {
			return -1;
		} else {
			if(this.ID == tmp.ID) {
				return 0;
			} else {
				return 1;
			}
		}
	}
	
}
