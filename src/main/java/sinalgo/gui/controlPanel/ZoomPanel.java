/*
BSD 3-Clause License

Copyright (c) 2007-2013, Distributed Computing Group (DCG)
                         ETH Zurich
                         Switzerland
                         dcg.ethz.ch
              2017-2018, André Brait

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

* Neither the name of the copyright holder nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package sinalgo.gui.controlPanel;

import sinalgo.gui.GUI;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.gui.transformation.Transformation3D;
import sinalgo.runtime.Global;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * A panel that displays the part of the graph shown on the screen. This panel
 * helps the user to display which part of the graph is shwon on the screen and
 * also to quickly move the view.
 */

public class ZoomPanel extends JPanel implements MouseInputListener, MouseWheelListener {

    private static final long serialVersionUID = -8525553690793845242L;

    private final GUI gui;
    private final PositionTransformation pt;

    /**
     * Default constructor
     *
     * @param aGui The GUI
     * @param aPT  The position transformator
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
     *
     * @param width The total width of the panel
     * @return the preferred height of this panel, given its width
     */
    int getPreferredHeight(int width) {
        if (this.pt instanceof Transformation3D) {
            return width;
        } else {
            return (int) ((1 - 2 * this.borderFactor) * width);
        }
    }

    @Override
    public void paint(Graphics g) {
        synchronized (this.pt) {
            // allow some border around
            int border = (int) (this.getWidth() * this.borderFactor);
            if (this.pt instanceof Transformation3D) {
                // but not in 3D
                border = 0;
            }

            int dim = Math.min(this.getWidth() - 2 * border, this.getHeight());
            g.setColor(this.getBackground());
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
            this.pt.drawZoomPanel(g, dim, border, 0, this.getWidth(), this.getHeight());
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (Global.isRunning) {
            return;
        } // block mouse input while simulating
        // on double click, zoom to fit or default view
        if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 2) {
            // fit, but do not rotate
            this.pt.zoomToFit(this.gui.getGraphPanel().getWidth(), this.gui.getGraphPanel().getHeight());
            this.gui.setZoomFactor(this.pt.getZoomFactor());
        } else if (e.getButton() == MouseEvent.BUTTON3 && e.getClickCount() >= 2) {
            // rotate to default view
            this.pt.defaultView(this.gui.getGraphPanel().getWidth(), this.gui.getGraphPanel().getHeight());
            this.gui.setZoomFactor(this.pt.getZoomFactor());
        }
    }

    private Point shiftStartPoint = null;
    private Point rotateStartPoint = null;

    @Override
    public void mousePressed(MouseEvent e) {
        if (Global.isRunning) {
            return;
        } // block mouse input while simulating
        if (e.getButton() == MouseEvent.BUTTON1) { // translate the view
            this.shiftStartPoint = e.getPoint();
            this.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            // rotate if 3D
            if (this.pt instanceof Transformation3D) {
                this.rotateStartPoint = e.getPoint();
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (Global.isRunning) {
            return;
        } // block mouse input while simulating
        if (this.shiftStartPoint != null || this.rotateStartPoint != null) {
            this.gui.redrawGUI();
        }
        this.shiftStartPoint = null;
        this.rotateStartPoint = null;
        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        if (Global.isRunning) {
            return;
        } // block mouse input while simulating
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (Global.isRunning) {
            return;
        } // block mouse input while simulating
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (Global.isRunning) {
            return;
        } // block mouse input while simulating
        if (this.shiftStartPoint != null) {
            // shift the view
            this.pt.moveView((int) ((this.shiftStartPoint.x - e.getX()) / this.pt.getZoomPanelZoomFactor() * this.pt.getZoomFactor()),
                    (int) ((this.shiftStartPoint.y - e.getY()) / this.pt.getZoomPanelZoomFactor() * this.pt.getZoomFactor()));
            this.shiftStartPoint = e.getPoint();
            this.gui.redrawControl();
        } else if (this.rotateStartPoint != null) {
            if (this.pt instanceof Transformation3D) {
                Transformation3D t3d = (Transformation3D) this.pt;
                t3d.rotate(e.getX() - this.rotateStartPoint.x, e.getY() - this.rotateStartPoint.y, !e.isControlDown(), true); // read
                // keyboard
                // -
                // ctrl
                // allows
                // to
                // freely
                // rotate
                this.rotateStartPoint = e.getPoint();
                this.gui.redrawControl();
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (Global.isRunning) {
            return;
        } // block mouse input while simulating
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (Global.isRunning) {
            return;
        } // block mouse input while simulating
        int clicks = e.getWheelRotation();
        if (clicks < 0) {
            this.gui.zoom(1.08); // zoom In
        } else {
            this.gui.zoom(1.0 / 1.08); // zoom out
        }
    }
}
