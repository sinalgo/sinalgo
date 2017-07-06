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
package sinalgo.gui.helper;


import java.awt.Color;
import java.awt.Graphics;
import java.io.PrintStream;

import sinalgo.configuration.Configuration;
import sinalgo.gui.transformation.PositionTransformation;

/**
 * A Class with a static method to draw an Arrow. Depending on the value of Configuration.drawArrows
 * the arrows are drawn with arrow heads or without.
 */
public class Arrow {	
	
	private static int unzoomedArrowLength = Configuration.arrowLength;
	private static int unzoomedArrowWidth = Configuration.arrowWidth;
	
	/**
	 * Method to be used to draw an arrow from (x1,y1) to (x2,y2) with the arrow head at (x2,y2). The
	 * head of the arrow is only drawn, if the boolean flag 'Configuration.drawArrows' is set to true.
	 * 
	 * @param x1 The x-value of the start node.
	 * @param y1 The y-value of the start node.
	 * @param x2 The x-node of the end node.
	 * @param y2 The y-node of the end node.
	 * @param g The Graphics object to paint the arrows into.
	 * @param pt The transformation instance the actual image is transformed with.
	 * @param col The color to draw the Arrow with.
	 */
	public static void drawArrow(int x1, int y1, int x2, int y2, Graphics g, PositionTransformation pt, Color col){
		Color tmpCol = g.getColor();
		g.setColor(col);
		
		if(Configuration.drawArrows){
			drawArrowHead(x1, y1, x2, y2, g, pt, col);
		}
		g.drawLine(x1, y1, x2, y2);

		g.setColor(tmpCol);
	}
	
	/**
	 * Method to be used to only draw an arrow Head for an arrow from (x1,y1) to (x2,y2) with the arrow head at (x2,y2). The
	 * head of the arrow is only drawn, if the boolean flag 'Configuration.drawArrows' is set to true.
	 * 
	 * @param x1 The x-value of the start node.
	 * @param y1 The y-value of the start node.
	 * @param x2 The x-node of the end node.
	 * @param y2 The y-node of the end node.
	 * @param g The Graphics object to paint the arrows into.
	 * @param pt The transformation instance the actual image is transformed with.
	 * @param col The color to draw the Arrow Head with.
	 */
	public static void drawArrowHead(int x1, int y1, int x2, int y2, Graphics g, PositionTransformation pt, Color col){
		if(x1 == x2 && y1 == y2){
			return; // cannot draw an arrow from a point to the same point
		}
		if(Configuration.drawArrows){
			Color tmpCol = g.getColor();
			g.setColor(col);
			
			// the size of the arrow
			double arrowLength = unzoomedArrowLength * pt.getZoomFactor();
			double arrowWidth = unzoomedArrowWidth * pt.getZoomFactor();
			double lineLength = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));

			// shorten the arrow if the two nodes are very close
			if(2 * arrowLength >= lineLength) {
				arrowLength = lineLength / 3;
			}
			
			double factor = 1/lineLength;
			
			// unit vector in opposite direction of arrow
			double ux = (x1 - x2) * factor;
			double uy = (y1 - y2) * factor;
			
			// intersection point of line and arrow
			double ix = x2 + arrowLength * ux;
			double iy = y2 + arrowLength * uy;
			
			// one end-point of the triangle is (x2,y2), the second end-point (ex1, ey1) and the third (ex2, ey2)
			arrowX[0] = x2;
			arrowY[0] = y2;
			arrowX[1] = (int)(ix + arrowWidth * uy);
			arrowY[1] = (int)(iy - arrowWidth * ux);
			arrowX[2] = (int)(ix - arrowWidth * uy);
			arrowY[2] = (int)(iy + arrowWidth * ux);

			g.fillPolygon(arrowX, arrowY, 3);
			
			g.setColor(tmpCol);
		}
	}

	private static int arrowX[] = new int[3];
	private static int arrowY[] = new int[3];
	
	
	
	/**
	 * This method writes an arrow (Edge) as metapost into the given printstream. It decides itself
	 * whether to draw the arrow as an arrow or as a simple line by checking the drawArrows-method
	 * of the Configuration.
	 * 
	 * @param sX The x-value of the start node.
	 * @param sY The y-value of the start node.
	 * @param eX The x-node of the end node.
	 * @param eY The y-node of the end node.
	 * @param ps The Printstream to write the metapost output to.
	 * @param c The color of the Arrow do draw.
	 */
	public static void drawArrowToMetaPost(double sX, double sY, double eX, double eY, PrintStream ps, Color c){
		if(Configuration.drawArrows){
			ps.print("drawarrow (" + sX + "," + sY + ")--("+eX+ "," + eY+ ") " +
					"withpen pencircle scaled 0.1 withcolor ("+c.getRed()+", "+c.getGreen()+", "+c.getBlue()+");\n");
		}
		else{
			ps.print("draw (" + sX + "," + sY + ")--("+eX+ "," + eY+ ") " +
					"withpen pencircle scaled 0.1 withcolor ("+c.getRed()+", "+c.getGreen()+", "+c.getBlue()+");\n");
			
		}
	}

	/**
	 * This method writes an arrow-head as metapost into the given printstream. If the drawArrows flag
	 * is set true, the head is written to the stream, otherwise it does nothing.
	 * 
	 * @param sX The x-value of the start node.
	 * @param sY The y-value of the start node.
	 * @param eX The x-node of the end node.
	 * @param eY The y-node of the end node.
	 * @param ps The Printstream to write the metapost output to.
	 * @param c The color of the Arrow-head do draw.
	 */
	public static void drawArrowHeadToMetaPost(double sX, double sY, double eX, double eY, PrintStream ps, Color c){
		if(Configuration.drawArrows){
			ps.print("filldraw arrowhead (" + sX + "," + sY + ")--("+eX+ "," + eY+ ") " +
					"withpen pencircle scaled 0.1 withcolor ("+c.getRed()+", "+c.getGreen()+", "+c.getBlue()+");\n");
		}		
	}
}
