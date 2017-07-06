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
package projects.defaultProject.nodes.edges;

import java.awt.Graphics;

import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.io.eps.EPSOutputPrintStream;
import sinalgo.nodes.edges.Edge;


/**
 * An edge that carries a boolean flag that can be used freely
 * by the project programmer. 
 */
public class BooleanEdge extends Edge {

	/**
	 * The flag of this edge, per default set to true.  
	 */
	public boolean flag = true;
	
	/**
	 * Set this memeber to true to draw only the boolean edges whose flag is 
	 * set to true. 
	 */
	public static boolean onlyUseFlagedEdges = false;
	
	/**
	 * @return Whether this edge is drawn on the GUI or to PostScript.
	 */
	public boolean isDrawn() {
		return !onlyUseFlagedEdges || flag;
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.nodes.edges.Edge#draw(java.awt.Graphics, sinalgo.gui.transformation.PositionTransformation)
	 */
	public void draw(Graphics g, PositionTransformation pt) {
		if(isDrawn()) {
			super.draw(g, pt);
		}
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.nodes.edges.Edge#drawToPostScript(sinalgo.io.eps.EPSOutputPrintStream, sinalgo.gui.transformation.PositionTransformation)
	 */
	public void drawToPostScript(EPSOutputPrintStream pw, PositionTransformation pt) {
		if(isDrawn()) {
			super.drawToPostScript(pw, pt);
		}
	}
}

