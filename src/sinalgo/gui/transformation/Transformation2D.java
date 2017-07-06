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
import java.awt.Graphics;
import java.awt.Rectangle;

import sinalgo.configuration.Configuration;
import sinalgo.io.eps.EPSOutputPrintStream;
import sinalgo.nodes.Position;

/**
 * Transforms a logic coordinate used by the simulation to a GUI coordinate. 
 * This transformation instance is to be used in 2D situations, when the nodes
 * only carry 2D position information.
 */
public class Transformation2D extends PositionTransformation {
	// The offset
	int dx, dy;
	
	/**
	 * Default constructor 
	 */
	public Transformation2D() {
		dx = dy = 0;
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.gui.transformation.PositionTransformation#getNumberOfDimensions()
	 */
	public int getNumberOfDimensions() {
		return 2;
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.gui.transformation.PositionTransformation#zoomToFit(int, int)
	 */
	protected void _zoomToFit(int width, int height) {
		int border = 1; // one pixel s.t. border is drawn in GUI
		double newZoom = Math.min((double)(width - border) / Configuration.dimX , 
		                          (double)(height - border) / Configuration.dimY );
		setZoomFactor(newZoom);
		dx = Math.max(0, (int) ((width - border - Configuration.dimX * newZoom) / 2));
		dy = Math.max(0, (int) ((height - border - Configuration.dimY * newZoom) / 2));
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.gui.transformation.PositionTransformation#defaultView(int, int)
	 */
	protected void _defaultView(int width, int height) {
		zoomToFit(width, height);// the same in 2D
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.gui.transformation.PositionTransformation#setZoomFactor(double)
	 */
	protected void _setZoomFactor(double newFactor) {
		// ensure that the center of the visible area remains at the same point  
		determineCenter();
		translateToLogicPosition(centerX, centerY);
		double cx = logicX, cy = logicY, cz = logicZ;
		this.zoomFactor = newFactor; // we need to set the new factor already now
		translateToGUIPosition(cx, cy, cz);
		moveView(-guiX + centerX, -guiY + centerY);
	}
	
	int centerX, centerY;
	
	/**
	 * Determines the center of the visible square and stores it in
	 * centerX, centerY; 
	 */
	private void determineCenter() {
		translateToGUIPosition(0, 0, 0);
		int minX = Math.max(guiX, 0), minY = Math.max(guiY, 0);
		translateToGUIPosition(Configuration.dimX, Configuration.dimY, Configuration.dimZ);  
		int maxX = Math.min(guiX, width), maxY = Math.min(guiY, height);
		centerX = (minX + maxX) / 2;
		centerY = (minY + maxY) / 2;
	}
	
	@Override
	public void translateToGUIPosition(double x, double y, double z) {
		// we already have a planar field - only need to scale according to the zoom factor
		this.guiXDouble = dx + x * zoomFactor;
		this.guiYDouble = dy + y * zoomFactor;
		this.guiX = (int) guiXDouble;
		this.guiY = (int) guiYDouble;
	}

	@Override
	public void translateToGUIPosition(Position pos) {
		translateToGUIPosition(pos.xCoord, pos.yCoord, pos.zCoord);
	}

	@Override
	public boolean supportReverseTranslation() {
		return true;
	}
	
	@Override
	public void translateToLogicPosition(int x, int y) {
		logicX = (x - dx) / zoomFactor;
		logicY = (y - dy) / zoomFactor;
		logicZ = 0;
	}

	@Override
	protected void _moveView(int x, int y) {
		dx += x;
		dy += y;
	}

	@Override
	public void drawBackground(Graphics g) {
		translateToGUIPosition(Configuration.dimX, Configuration.dimY, Configuration.dimZ);
		g.setColor(Color.WHITE);
		g.fillRect(dx, dy, guiX - dx, guiY - dy);
		g.setColor(Color.BLACK);
		g.drawLine(dx, dy, guiX, dy);
		g.drawLine(dx, dy, dx, guiY);
		g.drawLine(guiX, dy, guiX, guiY);
		g.drawLine(dx, guiY, guiX, guiY);
		
		// TODO: draw rulers if specified in the config
	}

	@Override
	public void drawBackgroundToPostScript(EPSOutputPrintStream ps) {
		//translateToGUIPosition(Configuration.dimX, Configuration.dimY, Configuration.dimZ);
		translateToGUIPosition(0,0,0);
		double x0 = guiXDouble, y0 = guiYDouble;
		translateToGUIPosition(Configuration.dimX, Configuration.dimY, 0);
		// draw the bounding box (black)
		ps.setColor(0,0,0);
		ps.drawLine(x0, y0, this.guiXDouble, y0);
		ps.drawLine(x0, y0, x0, this.guiYDouble);
		ps.drawLine(this.guiXDouble, this.guiYDouble, x0, this.guiYDouble);
		ps.drawLine(this.guiXDouble, this.guiYDouble, this.guiXDouble, y0);
	}
		
	double zoomPanelRatio = 1;
	
	@Override
	public void drawZoomPanel(Graphics g, int side,
	                          int offsetX, int offsetY, 
	                          int bgwidth, int bgheight) {
		double ratio = Math.min((double)(side) / Configuration.dimX, (double)(side) / Configuration.dimY);
		int offx = (int) (ratio * (Configuration.dimY - Configuration.dimX)/2); 
		int offy = (int) (ratio * (Configuration.dimX - Configuration.dimY)/2);
		if(offx < 0) {
			offx = 0;
		}
		if(offy < 0) {
			offy = 0;
		}
		offx += offsetX;
		offy += offsetY;
		
		g.setColor(new Color(0.8f, 0.8f, 0.8f));
		g.fillRect(offx, offy, (int)(Configuration.dimX * ratio), (int)(Configuration.dimY * ratio));
		g.setColor(Color.BLACK);
		g.drawRect(offx, offy, -1 + (int)(Configuration.dimX * ratio), -1 + (int)(Configuration.dimY * ratio));
		
		translateToGUIPosition(0, 0, 0);
		int leftX = guiX;
		int leftY = guiY;
		translateToGUIPosition(Configuration.dimX, Configuration.dimY, Configuration.dimZ);
		int rightX = guiX;
		int rightY = guiY;
		
		int ax = (int) (ratio * Configuration.dimX * (-leftX) / (rightX - leftX));
		int ay = (int) (ratio * Configuration.dimY * (-leftY) / (rightY - leftY));
		
		int bx = (int) (ratio * Configuration.dimX * (width - leftX) / (rightX - leftX));
		int by = (int) (ratio * Configuration.dimY * (height -leftY) / (rightY - leftY));

		ax = Math.max(0, ax);
		ay = Math.max(0, ay);
		bx = Math.min((int) (ratio * Configuration.dimX-1), bx);
		by = Math.min((int) (ratio * Configuration.dimY-1), by);

		g.setColor(Color.WHITE);
		g.fillRect(offx + ax, offy + ay, bx - ax, by - ay);
		g.setColor(Color.RED);
		g.drawRect(offx + ax, offy + ay, bx - ax, by - ay);
		
		g.setColor(Color.BLACK);
		g.drawRect(offx, offy, -1 + (int)(Configuration.dimX * ratio), -1 + (int)(Configuration.dimY * ratio));

		//shadeRect(g, offx + ax, offy + ay, bx - ax, by - ay);
		//shadeAllButRect(g, offx + ax, offy + ay, bx - ax, by - ay, bgwidth, bgheight);
		
		zoomPanelRatio = ratio;
	}
	
//	private void shadeRect(Graphics g, int x, int y, int width, int height) {
//		int rate = 3;
//		int xbool = 0;
//		for(int i=x; i< x+width; i++) {
//			int ybool = xbool;
//			for(int j=y; j<y+height; j++) {
//				if(ybool % rate == 0) {
//					g.drawLine(i, j, i, j);
//				}
//				ybool++;
//			}
//			xbool++;
//		}
//	}
//	
//	private void shadeAllButRect(Graphics g, int x, int y, int width, int height, int bgwidth, int bgheight) {
//		int rate = 3;
//		int xbool = 0;
//		for(int i=0; i<bgwidth; i++) {
//			int ybool = xbool;
//			for(int j=0; j<bgheight; j++) {
//				if(i < x || i > x + width || j < y || j > y + height) {
//					if(ybool % rate == 0) {
//						g.drawLine(i, j, i, j);
//					}
//				}
//				ybool++;
//			}
//			xbool++;
//		}
//	}

	@Override
	public double getZoomPanelZoomFactor() {
		return zoomPanelRatio;
	}

	@Override
	public String getLogicPositionString() {
		return "(" + (int) logicX + ", " + (int) logicY + ")";
	}

	@Override
	public String getGUIPositionString() {
		return "(" + guiX + ", " + guiY + ")";
	}

	@Override
	protected void _zoomToRect(Rectangle rect) {
		translateToLogicPosition(rect.x, rect.y);
		double lx = logicX, ly = logicY, lz = logicZ;
		double newZoomFactor = zoomFactor * Math.min((double)(width) / rect.width, (double)(height) / rect.height);
		// and set the new zoom factor
		_setZoomFactor(newZoomFactor);
		translateToGUIPosition(lx, ly, lz);
		moveView(-guiX, -guiY);
	}
}
