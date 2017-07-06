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
package sinalgo.gui.controlPanel;


import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JPanel;
import javax.swing.event.MouseInputListener;

import sinalgo.gui.GUI;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.gui.transformation.Transformation3D;
import sinalgo.runtime.Global;

/**
 * A panel that displays the part of the graph shown on the screen. This
 * panel helps the user to display which part of the graph is shwon on the screen
 * and also to quickly move the view.
 */
@SuppressWarnings("serial")
public class ZoomPanel extends JPanel implements MouseInputListener, MouseWheelListener {
	GUI gui;
	PositionTransformation pt;
	
	
	/**
	 * Default constructor
	 * @param aGui The GUI
	 * @param aPT The position transformator
	 */
	ZoomPanel(GUI aGui, PositionTransformation aPT) {
		this.gui = aGui;
		this.pt = aPT;
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		this.addMouseWheelListener(this);
	}
	
	// size of the border (in percent) around the zoomPanel graphics
	private double borderFactor = 0.15;
	
	/**
	 * Determines the preferred height of this panel, given its width
	 * @param width The total width of the panel
	 * @return the preferred height of this panel, given its width
	 */
	public int getPreferredHeight(int width) {
		if(pt instanceof Transformation3D) {
			return width;
		} else {
			return (int) ((1 - 2* borderFactor) * width);
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.Component#paint(java.awt.Graphics)
	 */
	public void paint(Graphics g) {
		synchronized(pt){
			// allow some border around
			int border = (int) (getWidth() * borderFactor);
			if(pt instanceof Transformation3D) {
				// but not in 3D
				border = 0;
			}
	
			int dim = Math.min(getWidth() - 2*border, getHeight());
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
			pt.drawZoomPanel(g, dim, border, 0, getWidth(), getHeight());
		}
	}

	public void mouseClicked(MouseEvent e) {
		if(Global.isRunning) { return; } // block mouse input while simulating
		// on double click, zoom to fit or default view
		if(e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 2) {
			// fit, but do not rotate
			pt.zoomToFit(gui.getGraphPanel().getWidth(),
			               gui.getGraphPanel().getHeight());
			gui.setZoomFactor(pt.getZoomFactor());
		} else if(e.getButton() == MouseEvent.BUTTON3 && e.getClickCount() >= 2) {
			// rotate to default view
			pt.defaultView(gui.getGraphPanel().getWidth(),
				             gui.getGraphPanel().getHeight());
			gui.setZoomFactor(pt.getZoomFactor());
		}
	}

	Point shiftStartPoint = null;
	Point rotateStartPoint = null;
	
	public void mousePressed(MouseEvent e) {
		if(Global.isRunning) { return; } // block mouse input while simulating
		if(e.getButton() == MouseEvent.BUTTON1) { // translate the view
			shiftStartPoint = e.getPoint();
			setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
		} else if(e.getButton() == MouseEvent.BUTTON3) {
			// rotate if 3D
			if(pt instanceof Transformation3D) {
				rotateStartPoint = e.getPoint();
			}
		}
	}

	public void mouseReleased(MouseEvent e) {
		if(Global.isRunning) { return; } // block mouse input while simulating
		if(shiftStartPoint != null || rotateStartPoint != null){
			gui.redrawGUI();
		}
		shiftStartPoint = null;
		rotateStartPoint = null;
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	public void mouseEntered(MouseEvent e) {
		if(Global.isRunning) { return; } // block mouse input while simulating
	}

	public void mouseExited(MouseEvent e) {
		if(Global.isRunning) { return; } // block mouse input while simulating
	}

	public void mouseDragged(MouseEvent e) {
		if(Global.isRunning) { return; } // block mouse input while simulating
		if(shiftStartPoint != null) {
			// shift the view
			pt.moveView((int) ((shiftStartPoint.x - e.getX()) / pt.getZoomPanelZoomFactor() * pt.getZoomFactor()), 
			            (int) ((shiftStartPoint.y - e.getY()) / pt.getZoomPanelZoomFactor() * pt.getZoomFactor()));
			shiftStartPoint = e.getPoint();
			gui.redrawControl();
		} else if(rotateStartPoint != null) {
			if(pt instanceof Transformation3D) {
				Transformation3D t3d = (Transformation3D) pt;
				t3d.rotate(e.getX() - rotateStartPoint.x, 
				           e.getY() - rotateStartPoint.y, 
				           !e.isControlDown(), true); // read keyboard - ctrl allows to freely rotate
				rotateStartPoint = e.getPoint();
				gui.redrawControl();
			}
		}
	}

	public void mouseMoved(MouseEvent e) {
		if(Global.isRunning) { return; } // block mouse input while simulating
	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		if(Global.isRunning) { return; } // block mouse input while simulating
		int clicks = e.getWheelRotation();
		if(clicks < 0) {
			gui.zoom(1.08); // zoom In  
		}	else {
			gui.zoom(1.0 / 1.08); // zoom out
		}
	}
}
