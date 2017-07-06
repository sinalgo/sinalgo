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
package sinalgo.gui.transformation;


import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import sinalgo.configuration.Configuration;
import sinalgo.gui.GraphPanel;
import sinalgo.io.eps.EPSOutputPrintStream;
import sinalgo.nodes.Position;
import sinalgo.runtime.Main;


/**
 * Transforms a logic coordinate used by the simulation to a GUI coordinate. 
 * This transformation instance is to be used in 3D situations, when the nodes
 * carry 3D position information. 
 */
public class Transformation3D extends PositionTransformation {
	// Transformation matrixes
	double [][] tm = new double[4][4]; // the 4x4 transformation matrix
	double[][] rotm = new double[4][4]; // the 4x4 transformation matrix
	double[][] tempm = new double[4][4]; // the 4x4 transformation matrix
	double tmAngleX = 0, tmAngleY = 0, tmAngleZ = 0; // the rotation angles of tm around the x, y, and z axis 

	// Two placeholders used to draw polygons
	private int[] polyLineX = new int[5];
	private int[] polyLineY = new int[5];
	
	// The boundaries of the gui coordinates when the graph is drawn with the current transformation matrix.
	int minX, maxX, minY, maxY;

	// The result of multiplying a vector with tm using multiply(double, double, double) is stored in these members.
	double resultX, resultY, resultZ;
	
	double maxDim = Math.max(Math.max(Configuration.dimX, Configuration.dimY), Configuration.dimZ);

	private double[][] zpm = new double[4][4]; // matrix for drawing the zoom panel
	private double zoomPanelZoom = 1; // zoom of the zoom panel
	
	// The boundary points of the simulation area
	private Position pos000 = new Position(0,0,0);
	private Position posx00 = new Position(Configuration.dimX,0,0);
	private Position pos0y0 = new Position(0,Configuration.dimY,0);
	private Position pos00z = new Position(0,0,Configuration.dimZ);
	private Position posxy0 = new Position(Configuration.dimX,Configuration.dimY,0);
	private Position pos0yz = new Position(0,Configuration.dimY,Configuration.dimZ);
	private Position posx0z = new Position(Configuration.dimX,0,Configuration.dimZ);
	private Position posxyz = new Position(Configuration.dimX,Configuration.dimY,Configuration.dimZ);
	
	private Position[] posList = new Position[8]; // list of all 8 above positions
	
	// The color for the 3 faces we can see through
	private Color cubeSeeThroughColor = new Color(0.96f, 0.96f, 0.96f);
	// Color for faces we look at from the back
	private Color cubeFaceColor = Color.WHITE;
	private Color cubeFaceBackColor = new Color(240, 249, 254);

	/* (non-Javadoc)
	 * @see sinalgo.gui.transformation.PositionTransformation#getNumberOfDimensions()
	 */
	public int getNumberOfDimensions() {
		return 3;
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.gui.transformation.PositionTransformation#moveView(int, int)
	 */
	protected void _moveView(int x, int y) {
		translate(x,y,0, tm);
	}
	
	/**
	 * Rotates the view according to a mouse gesture, along the vector (x,y).
	 * The rotation may preserve the direction of the z-axis, or move freely.
	 * @param x Number of pixels mouse moved in x-direction
	 * @param y Number of pixels mouse moved in y-direction
	 * @param preserveZAxis true, if the z-axis should preserve its orientation
	 * @param isZoomPanel True, if the rotation origins from the zoomPanel 
	 */
	public void rotate(int x, int y, boolean preserveZAxis, boolean isZoomPanel) {
		// rotate around the center of the cube
		multiply(Configuration.dimX / 2, Configuration.dimY / 2, Configuration.dimZ / 2, tm); 
		double offsetX = resultX, offsetY = resultY, offsetZ = resultZ;
		translate(-offsetX, -offsetY, -offsetZ, tm);

		double factor  = isZoomPanel ? 0.01 : 1.5f / (maxDim * zoomFactor); // rotate slower with high zoom;
		
		if(!preserveZAxis) {
			rotateY(x * factor, tm);
			rotateX(-y * factor, tm);
		} else {
			double tmp = tmAngleX;
			rotateX(-tmp, tm); // do not change the direction of the z-axis
			rotateZ(-x * factor, tm);
			rotateX(tmp, tm);
			rotateX(-y * factor, tm);
		}
		translate(offsetX, offsetY, offsetZ, tm);
		versionNumber ++;
	}
	
	/**
	 * Initializes two vectors polyLineX and polyLineY to 
	 * draw a polygon between 4 positions. 
	 * @param p1 The first positions
	 * @param p2 The second positions
	 * @param p3 The third positions
	 * @param p4 The fourth positions
	 */
	private void initPolyLine(Position p1, Position p2, Position p3, Position p4, double[][] matrix, boolean usePerspective) {
		translateToGUIPosition(p1, matrix, usePerspective);
		polyLineX[0] = polyLineX[4] = this.guiX;
		polyLineY[0] = polyLineY[4] = this.guiY;
		translateToGUIPosition(p2, matrix, usePerspective);
		polyLineX[1] = this.guiX;
		polyLineY[1] = this.guiY;
		translateToGUIPosition(p3, matrix, usePerspective);
		polyLineX[2] = this.guiX;
		polyLineY[2] = this.guiY;
		translateToGUIPosition(p4, matrix, usePerspective);
		polyLineX[3] = this.guiX;
		polyLineY[3] = this.guiY;
	}
	
	/**
	 * Draws a dotted line from p1 to p2.
	 * @param p1
	 * @param p2
	 */
	private void drawDottedLine(Graphics g, Position p1, Position p2, double[][] matrix, boolean usePerspective) {
		translateToGUIPosition(p1, matrix, usePerspective);
		int fromX = guiX, fromY = guiY;
		translateToGUIPosition(p2, matrix, usePerspective);
		int toX = guiX, toY = guiY;
		GraphPanel.drawDottedLine(g, fromX, fromY, toX, toY);
	}
	
	/**
	 * Draws a dotted line from p1 to p2.
	 * @param p1
	 * @param p2
	 */
	private void drawLine(Graphics g, Position p1, Position p2, double[][] matrix, boolean usePerspective) {
		translateToGUIPosition(p1, matrix, usePerspective);
		int fromX = guiX, fromY = guiY;
		translateToGUIPosition(p2, matrix, usePerspective);
		int toX = guiX, toY = guiY;
		g.drawLine(fromX, fromY, toX, toY);
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.gui.transformation.PositionTransformation#drawBackground(java.awt.Graphics)
	 */
	public void drawBackground(Graphics g) {
		// TODO: draw rulers if specified in the config
		drawCube(g, cubeFaceColor, cubeFaceBackColor, cubeSeeThroughColor, Color.DARK_GRAY, tm, Configuration.usePerspectiveView);
		drawCubeAxeArrows(g, tm, Configuration.usePerspectiveView);
	}
	
	/**
	 * Draw the cube of the simulation area
	 * @param g
	 */
	private void drawCube(Graphics g, Color fgColor, Color bgColor, Color seeThroughColor, Color lineColor, double[][] matrix, boolean usePerspective) {
		// draw the closing sides of the box in light gray
		determineVisibility(matrix, usePerspective);
		drawCubeBackground(g, fgColor, bgColor, seeThroughColor, matrix, usePerspective);
		drawCubeWireFrame(g, lineColor, matrix, usePerspective, null);
	}
	
	/**
	 * Draws the faces of the cube
	 * <p>
	 * A call to determineVisibility() is required prior to calling this method.
	 * @param g
	 * @param fgColor
	 * @param bgColor
	 * @param matrix
	 * @param usePerspective
	 */
	private void drawCubeBackground(Graphics g, Color fgColor, Color bgColor, Color seeThroughColor, double[][] matrix, boolean usePerspective) {
		g.setColor(seeThroughColor);
		initPolyLine(pos00z, posx0z, posxyz, pos0yz, matrix, usePerspective);
		g.fillPolygon(polyLineX, polyLineY, 5);
		
		initPolyLine(pos0y0, posxy0, posxyz, pos0yz, matrix, usePerspective);
		g.fillPolygon(polyLineX, polyLineY, 5);

		initPolyLine(posx00, posxy0, posxyz, posx0z, matrix, usePerspective);
		g.fillPolygon(polyLineX, polyLineY, 5);


		Color colXY, colYZ, colXZ;
		colXY = faceVisibilityXY ? fgColor : bgColor;
		colXZ = faceVisibilityXZ ? fgColor : bgColor;
		colYZ = faceVisibilityYZ ? fgColor : bgColor;

		// determine which of the 3 main faces is in front (the one with the largest resultZ -> draw it last
		translateToGUIPosition(posxy0, matrix, usePerspective);
		double raiseXY = resultZ;
		translateToGUIPosition(pos0yz, matrix, usePerspective);
		double raiseYZ = resultZ;
		translateToGUIPosition(posx0z, matrix, usePerspective);
		double raiseXZ = resultZ;

		// determine sequence to draw the faces s.t. they overdraw themselves correctly
		if(raiseXY < raiseYZ) {
			if(raiseXY < raiseXZ) { // xy first
				drawFaceXY(g, colXY, matrix, usePerspective);
				if(raiseXZ < raiseYZ) { // xz yz
					drawFaceXZ(g, colXZ, matrix, usePerspective);
					drawFaceYZ(g, colYZ, matrix, usePerspective);
				} else { // yz xz
					drawFaceYZ(g, colYZ, matrix, usePerspective);
					drawFaceXZ(g, colXZ, matrix, usePerspective);
				}
			} else { // XZ XY YZ
				drawFaceXZ(g, colXZ, matrix, usePerspective);
				drawFaceXY(g, colXY, matrix, usePerspective);
				drawFaceYZ(g, colYZ, matrix, usePerspective);
			}
		} else { // yz < xy
			if(raiseYZ < raiseXZ) { // yz first
				drawFaceYZ(g, colYZ, matrix, usePerspective);
				if(raiseXY < raiseXZ) { // xy xz
					drawFaceXY(g, colXY, matrix, usePerspective);
					drawFaceXZ(g, colXZ, matrix, usePerspective);
				} else { // xz xy
					drawFaceXZ(g, colXZ, matrix, usePerspective);
					drawFaceXY(g, colXY, matrix, usePerspective);
				}
			} else { // xz < yz => xz yz xy 
				drawFaceXZ(g, colXZ, matrix, usePerspective);
				drawFaceYZ(g, colYZ, matrix, usePerspective);
				drawFaceXY(g, colXY, matrix, usePerspective);
			}
		}
	}
	
	private void determineVisibility(double[][] matrix, boolean usePerspective) {
		// vectors along the axis
		translateToGUIPosition(0, 0, 0, matrix, usePerspective);
		double origX = resultX;
		double origY = resultY;
		translateToGUIPosition(posx00, matrix, usePerspective);
		double xX = resultX - origX;
		double xY = resultY - origY;
		translateToGUIPosition(pos0y0, matrix, usePerspective);
		double yX = resultX - origX;
		double yY = resultY - origY;
		translateToGUIPosition(pos00z, matrix, usePerspective);
		double zX = resultX - origX;
		double zY = resultY - origY;
		
		// cross product, test whether resulting vector points into screen or out of screen)
		faceVisibilityXZ = xY * zX - xX * zY < 0;
		faceVisibilityXY = xX * yY - xY * yX < 0;
		faceVisibilityYZ = yX * zY - yY * zX < 0;
		
		// and the outer faces
		// vectors along the axis
		translateToGUIPosition(posxyz, matrix, usePerspective);
		origX = resultX;
		origY = resultY;
		translateToGUIPosition(pos0yz, matrix, usePerspective);
		xX = resultX - origX;
		xY = resultY - origY;
		translateToGUIPosition(posx0z, matrix, usePerspective);
		yX = resultX - origX;
		yY = resultY - origY;
		translateToGUIPosition(posxy0, matrix, usePerspective);
		zX = resultX - origX;
		zY = resultY - origY;
		
		// cross product, test whether resulting vector points into screen or out of screen)
		faceVisibilityXZ2 = xY * zX - xX * zY > 0;
		faceVisibilityXY2 = xX * yY - xY * yX > 0;
		faceVisibilityYZ2 = yX * zY - yY * zX > 0;
	}
	
	boolean faceVisibilityXZ;
	boolean faceVisibilityXY;
	boolean faceVisibilityYZ;
	boolean faceVisibilityXZ2;
	boolean faceVisibilityXY2;
	boolean faceVisibilityYZ2;
	
	/**
	 * Draws the face XY with the given color
	 * @param g The graphics object to draw to
	 * @param col The color to draw the face in
	 * @param matrix The transformation matrix to use
	 * @param usePerspective Whether to use perspective or not
	 */
	private void drawFaceXY(Graphics g, Color col, double[][] matrix, boolean usePerspective) {
		g.setColor(col);
		initPolyLine(pos000, posx00, posxy0, pos0y0, matrix, usePerspective); // XY
		g.fillPolygon(polyLineX, polyLineY, 5);
	}

	/**
	 * Draws the face YZ with the given color
	 * @param g The graphics object to draw to
	 * @param col The color to draw the face in
	 * @param matrix The transformation matrix to use
	 * @param usePerspective Whether to use perspective or not
	 */
	private void drawFaceYZ(Graphics g, Color col, double[][] matrix, boolean usePerspective) {
		g.setColor(col);
		initPolyLine(pos000, pos0y0, pos0yz, pos00z, matrix, usePerspective); // YZ
		g.fillPolygon(polyLineX, polyLineY, 5);
	}

	/**
	 * Draws the face XZ with the given color
	 * @param g The graphics object to draw to
	 * @param col The color to draw the face in
	 * @param matrix The transformation matrix to use
	 * @param usePerspective Whether to use perspective or not
	 */
	private void drawFaceXZ(Graphics g, Color col, double[][] matrix, boolean usePerspective) {
		g.setColor(col);
		initPolyLine(pos000, posx00, posx0z, pos00z, matrix, usePerspective); // XZ
		g.fillPolygon(polyLineX, polyLineY, 5);
	}

	/**
	 * Draws a line, either to the screen or to postscript
	 * @param g
	 * @param from
	 * @param to
	 * @param matrix
	 * @param usePerspective
	 * @param pw
	 */
	private void helperDrawLine(Graphics g, Position from, Position to, double[][] matrix, boolean usePerspective, EPSOutputPrintStream pw) {
		if(g != null) {
			drawLine(g, from, to, matrix, usePerspective);
		} else {
			drawLineToPostScript(pw, from, to);
		}
	}

	/**
	 * Draws a dotted line, either to the screen or to postscript
	 * @param g
	 * @param from
	 * @param to
	 * @param matrix
	 * @param usePerspective
	 * @param pw
	 */
	private void helperDrawDottedLine(Graphics g, Position from, Position to, double[][] matrix, boolean usePerspective, EPSOutputPrintStream pw) {
		if(g != null) {
			drawDottedLine(g, from, to, matrix, usePerspective);
		} else {
			drawDottedLineToPostScript(pw, from, to);
		}
	}

	/**
	 * Draws the wireframe of the cube, dotting the hidden lines
	 * @param g
	 * @param lineColor
	 * @param matrix
	 * @param usePerspective
	 * @param pw
	 */
	private void drawCubeWireFrame(Graphics g, Color lineColor, double[][] matrix, boolean usePerspective, EPSOutputPrintStream pw) {
		// Draw the wire frame
		if(g != null) {
			g.setColor(lineColor);
		}
		
		// 000 -> x00
		if(faceVisibilityXY && faceVisibilityXZ) {
			helperDrawDottedLine(g, pos000, posx00, matrix, usePerspective, pw);
		} else {
			helperDrawLine(g, pos000, posx00, matrix, usePerspective, pw);
		}

		// 000 -> 00z
		if(faceVisibilityXZ && faceVisibilityYZ) {
			helperDrawDottedLine(g, pos000, pos00z, matrix, usePerspective, pw);
		} else {
			helperDrawLine(g, pos000, pos00z, matrix, usePerspective, pw);
		}

		// 000 -> 0y0
		if(faceVisibilityYZ && faceVisibilityXY) {
			helperDrawDottedLine(g, pos000, pos0y0, matrix, usePerspective, pw);
		} else {
			helperDrawLine(g, pos000, pos0y0, matrix, usePerspective, pw);
		}
		
		// xz0 -> 00z
		if(faceVisibilityXZ && faceVisibilityXY2) {
			helperDrawDottedLine(g, posx0z, pos00z, matrix, usePerspective, pw);
		} else {
			helperDrawLine(g, posx0z, pos00z, matrix, usePerspective, pw);
		}

		// xz0 -> xyz
		if(faceVisibilityYZ2 && faceVisibilityXY2) {
			helperDrawDottedLine(g, posx0z, posxyz, matrix, usePerspective, pw);
		} else {
			helperDrawLine(g, posx0z, posxyz, matrix, usePerspective, pw);
		}

		// xz0 -> x00
		if(faceVisibilityXZ && faceVisibilityYZ2) {
			helperDrawDottedLine(g, posx0z, posx00, matrix, usePerspective, pw);
		} else {
			helperDrawLine(g, posx0z, posx00, matrix, usePerspective, pw);
		}

		// 0yz -> 00z
		if(faceVisibilityYZ && faceVisibilityXY2) {
			helperDrawDottedLine(g, pos0yz, pos00z, matrix, usePerspective, pw);
		} else {
			helperDrawLine(g, pos0yz, pos00z, matrix, usePerspective, pw);
		}

		// 0yz -> xyz
		if(faceVisibilityXZ2 && faceVisibilityXY2) {
			helperDrawDottedLine(g, pos0yz, posxyz, matrix, usePerspective, pw);
		} else {
			helperDrawLine(g, pos0yz, posxyz, matrix, usePerspective, pw);
		}

		// 0yz -> 0y0
		if(faceVisibilityYZ && faceVisibilityXZ2) {
			helperDrawDottedLine(g, pos0yz, pos0y0, matrix, usePerspective, pw);
		} else {
			helperDrawLine(g, pos0yz, pos0y0, matrix, usePerspective, pw);
		}

		// xy0 -> xyz
		if(faceVisibilityXZ2 && faceVisibilityYZ2) {
			helperDrawDottedLine(g, posxy0, posxyz, matrix, usePerspective, pw);
		} else {
			helperDrawLine(g, posxy0, posxyz, matrix, usePerspective, pw);
		}

		// xy0 -> x00
		if(faceVisibilityXY && faceVisibilityYZ2) {
			helperDrawDottedLine(g, posxy0, posx00, matrix, usePerspective, pw);
		} else {
			helperDrawLine(g, posxy0, posx00, matrix, usePerspective, pw);
		}
		
		// xy0 -> 0y0
		if(faceVisibilityXY && faceVisibilityXZ2) {
			helperDrawDottedLine(g, posxy0, pos0y0, matrix, usePerspective, pw);
		} else {
			helperDrawLine(g, posxy0, pos0y0, matrix, usePerspective, pw);
		}
	}
	
	/**
	 * Draw the arrow and names of the axes
	 * @param g
	 */
	private void drawCubeAxeArrows(Graphics g, double[][] matrix, boolean usePerspective) {
		translateToGUIPosition(pos000, matrix, usePerspective); 
		int originX = guiX;
		int originY = guiY;
		g.setFont(new Font("", 0, 10));
		translateToGUIPosition(Configuration.dimX, 0, 0, matrix, usePerspective);
		drawAxeName(g, "x", originX, originY, guiX, guiY);
		translateToGUIPosition(0, Configuration.dimY, 0, matrix, usePerspective);
		drawAxeName(g, "y", originX, originY, guiX, guiY);
		translateToGUIPosition(0, 0, Configuration.dimZ, matrix, usePerspective);
		drawAxeName(g, "z", originX, originY, guiX, guiY);
	}
	
	/**
	 * Draws an arrow and an axe labeling for a given axis
	 * @param g The graphics
	 * @param name name of the axe
	 * @param originX X-coord of origin
	 * @param originY Y-coord of origin
	 * @param toX X-coord of end-point of axis
	 * @param toY Y-coord of end-point of axis
	 */
	private void drawAxeName(Graphics g, String name, int originX, int originY, int toX, int toY) {
		double factor = 1 / Math.max(0.00000001, Math.sqrt((toX - originX)*(toX -originX) + (toY-originY)*(toY-originY)));
    // unit vector in direction of axis
		double ux = (toX - originX) * factor; 
		double uy = (toY - originY) * factor;
		// draw a line to connect the arrow with the axis
		g.drawLine(toX, toY, (int) (toX + 10 * ux), (int)(toY + 10*uy));
		drawArrow(toX, toY, toX + (int)(14*ux), toY + (int) (14*uy), g);
		g.drawString(name, toX + (int)(18*ux), toY + (int)(18*uy));
	}

	/**
	 * Draws an arrow in the direction (x1, y1) to (x2, y2) [given in gui coordinates]
	 * @param g
	 */
	private void drawArrow(int x1, int y1, int x2, int y2, Graphics g) {
		int length = 10;
		int width = 2;
			
		double factor = 1/(Math.max(0.00000001, Math.sqrt(Math.pow((x1 - x2), 2)+(Math.pow((y1 - y2), 2)))));
		
		double[] c = new double[2];
		c[0] = (x1 - x2) * factor;
		c[1] = (y1 - y2) * factor;
		
		double[] ae = new double[2];
		
		ae[0] = x2 + length*c[0];
		ae[1] = y2 + length*c[1];
		
		double[] d = new double[2];
		d[0] = c[1];
		d[1] = -c[0];
		
		int arrowX[] = new int[4];
		int arrowY[] = new int[4];
		
		arrowX[0] = arrowX[3] = Math.round(x2);
		arrowX[1] = (int)Math.round((ae[0]+ width*d[0]));
		arrowX[2] = (int)Math.round((ae[0]- width*d[0]));
		
		arrowY[0] = arrowY[3] = Math.round(y2);
		arrowY[1] = (int)Math.round((ae[1]+ width*d[1]));
		arrowY[2] = (int)Math.round((ae[1]- width*d[1]));
		
		g.drawPolyline(arrowX, arrowY, 4);
		g.fillPolygon(arrowX, arrowY, 4);
	}
	
	@Override
	public void drawBackgroundToPostScript(EPSOutputPrintStream pw) {
		pw.println("%the background");
		
		if(!Configuration.epsDrawBackgroundWhite){ pw.setColor(250, 250, 250); }
		else{ pw.setColor(255, 255, 255); }
		
		drawPolygonToPostScript(pw, pos00z, posx0z, posxyz, pos0yz);
		drawPolygonToPostScript(pw, pos0y0, posxy0, posxyz, pos0yz);
		drawPolygonToPostScript(pw, posx00, posxy0, posxyz, posx0z);

		if(!Configuration.epsDrawBackgroundWhite){ pw.setColor(240, 240, 240); }
		
		drawPolygonToPostScript(pw, pos000, posx00, posxy0, pos0y0);
		drawPolygonToPostScript(pw, pos000, posx00, posx0z, pos00z);
		drawPolygonToPostScript(pw, pos000, pos0y0, pos0yz, pos00z);

		pw.setColor(0, 0, 0);

		determineVisibility(tm, Configuration.usePerspectiveView);
		this.drawCubeWireFrame(null, null, null, false, pw);
		
//		// .........
//		drawLineToPostScript(pw, pos000, posx00);
//		drawLineToPostScript(pw, pos000, pos0y0);
//		drawLineToPostScript(pw, pos000, pos00z);
//		
//		drawLineToPostScript(pw, posx0z, pos00z);
//		drawLineToPostScript(pw, posx0z, posx00);
//
//		drawLineToPostScript(pw, pos0yz, pos00z);
//		drawLineToPostScript(pw, pos0yz, pos0y0);
//		
//		drawLineToPostScript(pw, posxy0, posx00);
//		drawLineToPostScript(pw, posxy0, pos0y0);
//		
//		pw.println("%the dotted lines for the background.");
//		pw.println("[2 2] 0 setdash");
//		drawLineToPostScript(pw, posx0z, posxyz);
//		drawLineToPostScript(pw, pos0yz, posxyz);
//		drawLineToPostScript(pw, posxy0, posxyz);
//		pw.println("[] 0 setdash\n");
		
		translateToGUIPosition(pos000); 
		double originX = guiXDouble;
		double originY = guiYDouble;
		translateToGUIPosition(Configuration.dimX, 0, 0);
		drawAxesToPostScript(pw, "x", originX, originY, guiXDouble, guiYDouble);
		translateToGUIPosition(0, Configuration.dimY, 0);
		drawAxesToPostScript(pw, "y", originX, originY, guiXDouble, guiYDouble);
		translateToGUIPosition(0, 0, Configuration.dimZ);
		drawAxesToPostScript(pw, "z", originX, originY, guiXDouble, guiYDouble);
	}
	
	private void drawAxesToPostScript(EPSOutputPrintStream pw, String name, double originX, double originY, double toX, double toY) {
		double factor = 1 / Math.max(0.00000001, Math.sqrt((toX - originX)*(toX -originX) + (toY-originY)*(toY-originY)));
		// unit vector in direction of axis
		double ux = (toX - originX) * factor; 
		double uy = (toY - originY) * factor;
		// draw a line to connect the arrow with the axis
		//pw.drawLine(toX, toY, (toX + 10*ux), toY + 10*uy);
		pw.drawArrow(toX, toY, (toX + 14*ux), toY + 14*uy);
		pw.setFontSize(12);
		pw.drawText(name, toX + 18*ux, toY + 18*uy);
	}
	
	private void drawPolygonToPostScript(EPSOutputPrintStream pw, Position p1, Position p2, Position p3, Position p4){
		translateToGUIPosition(p1);
		double p1X = guiXDouble, p1Y = guiYDouble;
		translateToGUIPosition(p2);
		double p2X = guiXDouble, p2Y = guiYDouble;
		translateToGUIPosition(p3);
		double p3X = guiXDouble, p3Y = guiYDouble;
		translateToGUIPosition(p4);
		double p4X = guiXDouble, p4Y = guiYDouble;
		pw.drawFilledPolygon(p1X, p1Y, p2X, p2Y, p3X, p3Y, p4X, p4Y);
	}

	/**
	 * Draw a line between two points
	 * @param pw The stream to write the Postscript commands to
	 * @param from Start point of line
	 * @param to End point of line
	 */
	private void drawLineToPostScript(EPSOutputPrintStream pw, Position from, Position to) {
		translateToGUIPosition(from);
		double fromX = guiXDouble;
		double fromY = guiYDouble;
		translateToGUIPosition(to);
		pw.drawLine(fromX, fromY, guiXDouble, guiYDouble);
	}
	
	/**
	 * Draw a dotted line between two points
	 * @param pw The stream to write the Postscript commands to
	 * @param from Start point of line
	 * @param to End point of line
	 */
	private void drawDottedLineToPostScript(EPSOutputPrintStream pw, Position from, Position to) {
		pw.println("[2 2] 0 setdash\n");
		translateToGUIPosition(from);
		double fromX = guiXDouble;
		double fromY = guiYDouble;
		translateToGUIPosition(to);
		pw.drawLine(fromX, fromY, guiXDouble, guiYDouble);
		pw.println("[] 0 setdash\n");
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.gui.transformation.PositionTransformation#setZoomFactor(double)
	 */
	protected void _setZoomFactor(double newFactor) {
		scaleInTheMiddleOfScreen(newFactor / zoomFactor, tm);
	}
	
	/**
	 * Scales the transformation matrix s.t. the current center of the visible screen remains in the center.
	 * This method does not set the zoom factor to this transformation object.
	 * @param deltaZoom The factor of the scaling
	 * @param matrix
	 */
	private void scaleInTheMiddleOfScreen(double deltaZoom, double[][] matrix) {
		scale(deltaZoom, matrix);
		// move the window back s.t. the center of the screen before zooming lies again in the center of the screen
		translate(width / 2 *(1- deltaZoom), height / 2 *(1- deltaZoom), 0, matrix);
	}
	
	/**
	 * Scales the transformation matrix, but does not set the zoom factor to this 
	 * transformation object.
	 * @param deltaZoom The factor of the scaling
	 * @param matrix
	 */
	private void scaleInTheMiddleOfCube(double deltaZoom, double[][] matrix) {
		multiply(Configuration.dimX / 2, Configuration.dimY / 2, Configuration.dimZ / 2, matrix); 
		double offsetX = resultX;	
		double offsetY = resultY;	
		double offsetZ = resultZ;	
		translate(-offsetX, -offsetY, -offsetZ, matrix);
		scale(deltaZoom, matrix);
		translate(offsetX, offsetY, offsetZ, matrix);
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.gui.transformation.PositionTransformation#zoomToFit(int, int)
	 */
	protected void _zoomToFit(int width, int height) {
		zoomFactor *= zoomToFit(width, height, tm, Configuration.usePerspectiveView);
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.gui.transformation.PositionTransformation#defaultView(int, int)
	 */
	protected void _defaultView(int width, int height) {
		rotateToDefault(tm);
		_zoomToFit(width, height);
	}
	
	/**
	 * Rotates the cube such that the z-axis becomes a single point, 
	 * and sets the zoomfactor such that the image nicely fits on 
	 * the screen.
	 * @param width The width of the window
	 * @param height The height of the window
	 */
	public void defaultViewXY(int width, int height) {
		reset(tm);
		_zoomToFit(width, height);
		versionNumber++;
	}

	/**
	 * Rotates the cube such that the y-axis becomes a single point, 
	 * and sets the zoomfactor such that the image nicely fits on 
	 * the screen.
	 * @param width The width of the window
	 * @param height The height of the window
	 */
	public void defaultViewXZ(int width, int height) {
		reset(tm);
		this.rotateX(Math.PI / 2, tm);
		_zoomToFit(width, height);
		versionNumber++;
	}
	
	/**
	 * Rotates the cube such that the x-axis becomes a single point, 
	 * and sets the zoomfactor such that the image nicely fits on 
	 * the screen.
	 * @param width The width of the window
	 * @param height The height of the window
	 */
	public void defaultViewYZ(int width, int height) {
		reset(tm);
		this.rotateZ(Math.PI / 2, tm);
		this.rotateX(Math.PI / 2, tm);
		_zoomToFit(width, height);
		versionNumber++;
	}

	/**
	 * Rotates the transformation matrix to the default perspective
	 * @param matrix The transformation matrix
	 */
	private void rotateToDefault(double[][] matrix) {
		// set the rotation matrix such that it looks nice. 
		reset(matrix);
		// rotate around the center of the cube
		multiply(Configuration.dimX / 2, Configuration.dimY / 2, Configuration.dimZ / 2, tm); 
		double offsetX = resultX, offsetY = resultY, offsetZ = resultZ;
		translate(-offsetX, -offsetY, -offsetZ, matrix);

		rotateZ(Math.PI / 2, matrix);
		rotateX(Math.PI / 2, matrix);
		rotateY(-Math.PI / 6, matrix);
		rotateX(-Math.PI / 8, matrix);
		
		translate(offsetX, offsetY, offsetZ, matrix);
	}
	
	/**
	 * Translates, and scales the matrix such that it nicely fits into
	 * a given window. Does not update the scaling factor!
	 * @param width The width of the graphics object 
	 * @param height The height of the graphics object 
	 * @param matrix The transformation matrix
	 * @return The factor by which the zoom factor was multiplied
	 */
	private double zoomToFit(int width, int height, double[][] matrix, boolean usePerspective) {
		int axesOffset = 30; // border to allow for names of axes
		determineBoundingBox(matrix, usePerspective);
		double currentWidth = maxX - minX ; // add some space for the axe naming
		double currentHeight = maxY - minY;
		double delta = Math.min((width - 2*axesOffset) / currentWidth, (height - 2*axesOffset) / currentHeight);
		scaleInTheMiddleOfCube(delta, matrix); 
		determineBoundingBox(matrix, usePerspective);
		translate(-minX + (width - (maxX - minX))/ 2, -minY + (height - (maxY - minY))/ 2, 0, matrix);
		return delta;
	}
	
	/**
	 * Scales the transformation matrix by a factor.
	 * @param factor
	 * @param matrix
	 */
	private void scale(double factor, double[][] matrix) {
		for(int i=0; i<3; i++) {
			for(int j=0; j<4; j++) {
				matrix[i][j] *= factor;
			}
		}
	}

	private void rotateX(double angle, double[][] matrix) {
		if(matrix == tm) {
			tmAngleX += angle;
		}
		// top row
		rotm[0][0] = 1;
		rotm[0][1] = 0;
		rotm[0][2] = 0;
		rotm[0][3] = 0;

		rotm[1][0] = 0;
		rotm[1][1] = Math.cos(angle);
		rotm[1][2] = -Math.sin(angle);
		rotm[1][3] = 0;
		
		rotm[2][0] = 0;
		rotm[2][1] = Math.sin(angle);
		rotm[2][2] = Math.cos(angle);
		rotm[2][3] = 0;
		
		rotm[3][0] = 0;
		rotm[3][1] = 0;
		rotm[3][2] = 0;
		rotm[3][3] = 1;
		multiply(matrix);
	}

	private void rotateY(double angle, double[][] matrix) {
		if(matrix == tm) {
			tmAngleY += angle;
		}
		// top row
		rotm[0][0] = Math.cos(angle);
		rotm[0][1] = 0;
		rotm[0][2] = Math.sin(angle);
		rotm[0][3] = 0;

		rotm[1][0] = 0;
		rotm[1][1] = 1;
		rotm[1][2] = 0;
		rotm[1][3] = 0;
		
		rotm[2][0] = -Math.sin(angle);
		rotm[2][1] = 0;
		rotm[2][2] = Math.cos(angle);
		rotm[2][3] = 0;
		
		rotm[3][0] = 0;
		rotm[3][1] = 0;
		rotm[3][2] = 0;
		rotm[3][3] = 1;
		multiply(matrix);
	}

	private void rotateZ(double angle, double[][] matrix) {
		if(matrix == tm) {
			tmAngleZ += angle;
		}
		// top row
		rotm[0][0] = Math.cos(angle);
		rotm[0][1] = -Math.sin(angle);
		rotm[0][2] = 0;
		rotm[0][3] = 0;

		rotm[1][0] = Math.sin(angle);
		rotm[1][1] = Math.cos(angle);
		rotm[1][2] = 0;
		rotm[1][3] = 0;
		
		rotm[2][0] = 0;
		rotm[2][1] = 0;
		rotm[2][2] = 1;
		rotm[2][3] = 0;
		
		rotm[3][0] = 0;
		rotm[3][1] = 0;
		rotm[3][2] = 0;
		rotm[3][3] = 1;
		multiply(matrix);
	}
	
	/**
	 * Translates the image by the current offset
	 * @param x
	 * @param y
	 * @param z
	 */
	private void translate(double x, double y, double z, double[][] matrix) {
		matrix[0][3] += x;
		matrix[1][3] += y;
		matrix[2][3] += z;
	}

	/**
	 * Multiplies tm = rotm * tm. 
	 * Temporarily stores the result in tempm. 
	 */
	private void multiply(double[][] matrix) {
		// multiplies rotm * tm, and stores the result in tm
		for(int i=0; i<4; i++) {
			for(int j=0; j<4; j++) {
				double sum = 0;
				for(int k=0; k<4; k++) {
					sum += rotm[i][k] * matrix[k][j];
				}
				tempm[i][j] = sum;
			}
		}
		for(int i=0; i<4; i++) {
			for(int j=0; j<4; j++) {
				matrix[i][j] = tempm[i][j];
			}
		}
	}

	/**
	 * Determines the 2D bounding box of the cube. It stores the
	 * extermal coordinates of the cube in the member variables
	 * minX, maxX, minY, maxY.  
	 */
	private void determineBoundingBox(double[][] matrix, boolean usePerspective) {
		translateToGUIPosition(posList[0], matrix, usePerspective);
		minX = maxX = guiX;
		minY = maxY = guiY;
		for(int i=1; i<8; i++) {
			translateToGUIPosition(posList[i], matrix, usePerspective);
			if(guiX < minX) {
				minX = guiX;
			}
			if(guiX > maxX) {
				maxX = guiX;
			}
			if(guiY < minY) {
				minY = guiY;
			}
			if(guiY > maxY) {
				maxY = guiY;
			}
		}
	}
	
	
	/**
	 * Default constructor 
	 */
	public Transformation3D() {
		posList[0] = pos000;
		posList[1] = posx00;
		posList[2] = pos0y0;
		posList[3] = pos00z;
		posList[4] = posxy0;
		posList[5] = posx0z;
		posList[6] = pos0yz;
		posList[7] = posxyz;
		
		reset(tm);
	}
	
	/**
	 * Resets the transformation matrix 
	 */
	private void reset(double[][] matrix) {
		if(matrix == tm) {
			tmAngleX = tmAngleY = tmAngleZ = 0;
			zoomFactor = 1;
		}
		// initialize the default transformation matrix (no rotation at all)
		for(int i=0; i<4; i++) {
			for(int j=0; j<4; j++) {
				matrix[i][j] = i == j ? 1 : 0;
			}
		}
	}
	
	/**
	 * Multiply tm with a given vector. The result is stored
	 * in the member variables resultX, resultY, and resultZ.
	 * @param x the x-coordinate of the vector to multiply with tm.  
	 * @param y the y-coordinate of the vector to multiply with tm.
	 * @param z the z-coordinate of the vector to multiply with tm.
	 */
	private void multiply(double x, double y, double z, double[][] matrix) {
		// Note that we negate the logic y value to mirror the cube, such as to obtain 
		// the usual x y z coordinate system orientation
		resultX =  x * matrix[0][0] - y * matrix[0][1] + z * matrix[0][2] + matrix[0][3];
		resultY =  x * matrix[1][0] - y * matrix[1][1] + z * matrix[1][2] + matrix[1][3];
		resultZ =  x * matrix[2][0] - y * matrix[2][1] + z * matrix[2][2] + matrix[2][3];
	}
	
	@Override
	public void translateToGUIPosition(double x, double y, double z) {
		translateToGUIPosition(x, y, z, tm, Configuration.usePerspectiveView);
	}
	
	/**
	 * Same as translateToGUIPosition, but utilizes the specified transformation matrix
	 * @param x
	 * @param y
	 * @param z
	 * @param matrix The matrix which defines the transformation
	 */
	private void translateToGUIPosition(double x, double y, double z, double[][] matrix, boolean usePerspective) {
		multiply(x,y,z, matrix);
		// we project onto the X/Y field
		// possibly add some perspective
		if(usePerspective) {
			double perspectiveZ = Configuration.perspectiveViewDistance * maxDim * zoomFactor;
			resultX = width/2 + (width/2 - resultX) * perspectiveZ / (resultZ - perspectiveZ);
			resultY = height/2 + (height/2 - resultY) * perspectiveZ / (resultZ - perspectiveZ);
		}
		
		this.guiXDouble = resultX;
		this.guiYDouble = resultY;
		this.guiX = (int) resultX;
		this.guiY = (int) resultY;
	}
	
	/**
	 * @param pos The logic position to translate to a GUI position
	 * @param matrix The rotation matrix to use
	 */
	private void translateToGUIPosition(Position pos, double[][] matrix, boolean usePerspective) {
		translateToGUIPosition(pos.xCoord, pos.yCoord, pos.zCoord, matrix, usePerspective);
	}

	/**
	 * The GUI coordinates are obtained by applying this 
	 * transformation object (rotation, translation and scaling) to
	 * a position and then projecting it on the X/Y plane by setting 
	 * the z-coordinate to zero. The members guiX and guiY correspond to 
	 * the x- and y-coordinate of the translated position.
	 * This method returns the corresponding z-coordinate of a translated position.    
	 * @param pos The logic position to translate
	 * @return The z-coordinate of 
	 */
	public double translateToGUIPositionAndGetZOffset(Position pos) {
		translateToGUIPosition(pos.xCoord, pos.yCoord, pos.zCoord, tm, Configuration.usePerspectiveView);
		return resultZ;
	}
	
	@Override
	public void translateToGUIPosition(Position pos) {
		translateToGUIPosition(pos.xCoord, pos.yCoord, pos.zCoord, tm, Configuration.usePerspectiveView);
	}

	@Override
	public boolean supportReverseTranslation() {
		return false;
	}

	@Override
	public void translateToLogicPosition(int x, int y) {
		Main.fatalError("Trying to translate a GUI coordinate to a 3D coordinate, even though this " +
				"is not supported in 3D!");
	}

	@Override
	public void drawZoomPanel(Graphics g, int side,
	                          int offsetX, int offsetY, 
	                          int bgwidth, int bgheight) {
		// copy the current transformation matrix 
		for(int i=0; i<4; i++) {
			for(int j=0; j<4; j++) {
				zpm[i][j] = tm[i][j];
			}
		}

		zoomPanelZoom = zoomFactor * zoomToFit(side, side, zpm, false); // note: we copied tm to zpm
    // draw the background first in gray, then at the end, paint the visible part in white
		determineVisibility(zpm, false);
		Color faceColor = new Color(0.8f, 0.8f, 0.8f);
		drawCubeBackground(g, faceColor, faceColor, new Color(0.85f, 0.85f, 0.85f), zpm, false);

		determineBoundingBox(zpm, false);
		offsetX += minX;
		offsetY += minY;
		int boundingBoxWidth = maxX - minX;
		int boundingBoxHeight = maxY - minY;
		
		determineBoundingBox(tm, false);
		
		double dimX = (maxX - minX) / zoomFactor;
		double dimY = (maxY - minY) / zoomFactor;
		translateToGUIPosition(0, 0, 0, zpm, false);
		
		int ax = (int) (zoomPanelZoom * dimX * (-minX) / (maxX - minX));
		int ay = (int) (zoomPanelZoom * dimY * (-minY) / (maxY - minY));
		int bx = (int) (zoomPanelZoom * dimX * (width - minX) / (maxX - minX));
		int by = (int) (zoomPanelZoom * dimY * (height -minY) / (maxY - minY));
		ax = Math.max(0, ax);
		ay = Math.max(0, ay);
		bx = Math.min(boundingBoxWidth, bx);
		by = Math.min(boundingBoxHeight, by);
		
		Shape oldClip = g.getClip();

		// draw a red line around the visible part
		g.setClip(offsetX + ax -1, offsetY + ay -1, bx - ax + 2, by - ay + 2);
		drawCubeBackground(g, Color.RED, Color.RED, Color.RED, zpm, false);
		// ...but not on the wireframe
		drawCubeWireFrame(g, Color.BLACK, zpm, false, null);
		g.setClip(offsetX + ax, offsetY + ay, bx - ax, by - ay);
		// and draw in white the visible area
		drawCubeBackground(g, cubeFaceColor, cubeFaceBackColor, cubeSeeThroughColor, zpm, false);
		g.setClip(oldClip);
		// put on top the wire frame and the axes
		drawCubeWireFrame(g, Color.BLACK, zpm, false, null);
		drawCubeAxeArrows(g, zpm, false);
	}

	@Override
	public double getZoomPanelZoomFactor() {
		return zoomPanelZoom;
	}

	@Override
	public String getLogicPositionString() {
		return "(" + logicX + ", " + logicY + ", " + logicZ + ")"; 
	}

	@Override
	public String getGUIPositionString() {
		return "(" + guiX + ", " + guiY + ")";
	}

	@Override
	protected void _zoomToRect(Rectangle rect) {
		double delta = Math.min((double)(width) / rect.width, (double)(height) / rect.height);
		_moveView(-rect.x, -rect.y);
		scale(delta, tm);
		this.zoomFactor *= delta;
	}
}
