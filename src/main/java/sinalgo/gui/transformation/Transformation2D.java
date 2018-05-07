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
package sinalgo.gui.transformation;

import lombok.NoArgsConstructor;
import sinalgo.configuration.Configuration;
import sinalgo.io.eps.EPSOutputPrintStream;
import sinalgo.nodes.Position;

import java.awt.*;

/**
 * Transforms a logic coordinate used by the simulation to a GUI coordinate.
 * This transformation instance is to be used in 2D situations, when the nodes
 * only carry 2D position information.
 */
@NoArgsConstructor
public class Transformation2D extends PositionTransformation {

    // The offset
    private int dx, dy;

    @Override
    public int getNumberOfDimensions() {
        return 2;
    }

    @Override
    protected void onChangeZoomToFit(int width, int height) {
        int border = 1; // one pixel s.t. border is drawn in GUI
        double newZoom = Math.min((double) (width - border) / Configuration.getDimX(),
                (double) (height - border) / Configuration.getDimY());
        this.changeZoomFactor(newZoom);
        this.dx = Math.max(0, (int) ((width - border - Configuration.getDimX() * newZoom) / 2));
        this.dy = Math.max(0, (int) ((height - border - Configuration.getDimY() * newZoom) / 2));
    }

    @Override
    protected void onChangeDefaultView(int width, int height) {
        this.zoomToFit(width, height);// the same in 2D
    }

    @Override
    protected void onChangeZoomFactor(double zoomfactor) {
        // ensure that the center of the visible area remains at the same point
        this.determineCenter();
        this.translateToLogicPosition(this.centerX, this.centerY);
        double cx = this.getLogicX(), cy = this.getLogicY(), cz = this.getLogicZ();
        this.setZoomFactor(zoomfactor); // we need to set the new factor already now
        this.translateToGUIPosition(cx, cy, cz);
        this.moveView(-this.getGuiX() + this.centerX, -this.getGuiY() + this.centerY);
    }

    private int centerX, centerY;

    /**
     * Determines the center of the visible square and stores it in centerX,
     * centerY;
     */
    private void determineCenter() {
        this.translateToGUIPosition(0, 0, 0);
        int minX = Math.max(this.getGuiX(), 0), minY = Math.max(this.getGuiY(), 0);
        this.translateToGUIPosition(Configuration.getDimX(), Configuration.getDimY(), Configuration.getDimZ());
        int maxX = Math.min(this.getGuiX(), this.getWidth()), maxY = Math.min(this.getGuiY(), this.getHeight());
        this.centerX = (minX + maxX) / 2;
        this.centerY = (minY + maxY) / 2;
    }

    @Override
    public void translateToGUIPosition(double x, double y, double z) {
        // we already have a planar field - only need to scale according to the zoom
        // factor
        this.setGuiXDouble(this.dx + x * this.getZoomFactor());
        this.setGuiYDouble(this.dy + y * this.getZoomFactor());
        this.setGuiX((int) this.getGuiXDouble());
        this.setGuiY((int) this.getGuiYDouble());
    }

    @Override
    public void translateToGUIPosition(Position pos) {
        this.translateToGUIPosition(pos.getXCoord(), pos.getYCoord(), pos.getZCoord());
    }

    @Override
    public boolean supportReverseTranslation() {
        return true;
    }

    @Override
    public void translateToLogicPosition(int x, int y) {
        this.setLogicX((x - this.dx) / this.getZoomFactor());
        this.setLogicY((y - this.dy) / this.getZoomFactor());
        this.setLogicZ(0);
    }

    @Override
    protected void onChangeMoveView(int x, int y) {
        this.dx += x;
        this.dy += y;
    }

    @Override
    public void drawBackground(Graphics g) {
        this.translateToGUIPosition(Configuration.getDimX(), Configuration.getDimY(), Configuration.getDimZ());
        g.setColor(Color.WHITE);
        g.fillRect(this.dx, this.dy, this.getGuiX() - this.dx, this.getGuiY() - this.dy);
        g.setColor(Color.BLACK);
        g.drawLine(this.dx, this.dy, this.getGuiX(), this.dy);
        g.drawLine(this.dx, this.dy, this.dx, this.getGuiY());
        g.drawLine(this.getGuiX(), this.dy, this.getGuiX(), this.getGuiY());
        g.drawLine(this.dx, this.getGuiY(), this.getGuiX(), this.getGuiY());

        // TODO: draw rulers if specified in the config
    }

    @Override
    public void drawBackgroundToPostScript(EPSOutputPrintStream ps) {
        // translateToGUIPosition(Configuration.dimX, Configuration.dimY,
        // Configuration.dimZ);
        this.translateToGUIPosition(0, 0, 0);
        double x0 = this.getGuiXDouble(), y0 = this.getGuiYDouble();
        this.translateToGUIPosition(Configuration.getDimX(), Configuration.getDimY(), 0);
        // draw the bounding box (black)
        ps.setColor(0, 0, 0);
        ps.drawLine(x0, y0, this.getGuiXDouble(), y0);
        ps.drawLine(x0, y0, x0, this.getGuiYDouble());
        ps.drawLine(this.getGuiXDouble(), this.getGuiYDouble(), x0, this.getGuiYDouble());
        ps.drawLine(this.getGuiXDouble(), this.getGuiYDouble(), this.getGuiXDouble(), y0);
    }

    private double zoomPanelRatio = 1;

    @Override
    public void drawZoomPanel(Graphics g, int side, int offsetX, int offsetY, int bgwidth, int bgheight) {
        double ratio = Math.min((double) (side) / Configuration.getDimX(), (double) (side) / Configuration.getDimY());
        int offx = (int) (ratio * (Configuration.getDimY() - Configuration.getDimX()) / 2);
        int offy = (int) (ratio * (Configuration.getDimX() - Configuration.getDimY()) / 2);
        if (offx < 0) {
            offx = 0;
        }
        if (offy < 0) {
            offy = 0;
        }
        offx += offsetX;
        offy += offsetY;

        g.setColor(new Color(0.8f, 0.8f, 0.8f));
        g.fillRect(offx, offy, (int) (Configuration.getDimX() * ratio), (int) (Configuration.getDimY() * ratio));
        g.setColor(Color.BLACK);
        g.drawRect(offx, offy, -1 + (int) (Configuration.getDimX() * ratio), -1 + (int) (Configuration.getDimY() * ratio));

        this.translateToGUIPosition(0, 0, 0);
        int leftX = this.getGuiX();
        int leftY = this.getGuiY();
        this.translateToGUIPosition(Configuration.getDimX(), Configuration.getDimY(), Configuration.getDimZ());
        int rightX = this.getGuiX();
        int rightY = this.getGuiY();

        int ax = (int) (ratio * Configuration.getDimX() * (-leftX) / (rightX - leftX));
        int ay = (int) (ratio * Configuration.getDimY() * (-leftY) / (rightY - leftY));

        int bx = (int) (ratio * Configuration.getDimX() * (this.getWidth() - leftX) / (rightX - leftX));
        int by = (int) (ratio * Configuration.getDimY() * (this.getHeight() - leftY) / (rightY - leftY));

        ax = Math.max(0, ax);
        ay = Math.max(0, ay);
        bx = Math.min((int) (ratio * Configuration.getDimX() - 1), bx);
        by = Math.min((int) (ratio * Configuration.getDimY() - 1), by);

        g.setColor(Color.WHITE);
        g.fillRect(offx + ax, offy + ay, bx - ax, by - ay);
        g.setColor(Color.RED);
        g.drawRect(offx + ax, offy + ay, bx - ax, by - ay);

        g.setColor(Color.BLACK);
        g.drawRect(offx, offy, -1 + (int) (Configuration.getDimX() * ratio), -1 + (int) (Configuration.getDimY() * ratio));

        // shadeRect(g, offx + ax, offy + ay, bx - ax, by - ay);
        // shadeAllButRect(g, offx + ax, offy + ay, bx - ax, by - ay, bgwidth,
        // bgheight);

        this.zoomPanelRatio = ratio;
    }

    // private void shadeRect(Graphics g, int x, int y, int width, int height) {
    // int rate = 3;
    // int xbool = 0;
    // for(int i=x; i< x+width; i++) {
    // int ybool = xbool;
    // for(int j=y; j<y+height; j++) {
    // if(ybool % rate == 0) {
    // g.drawLine(i, j, i, j);
    // }
    // ybool++;
    // }
    // xbool++;
    // }
    // }
    //
    // private void shadeAllButRect(Graphics g, int x, int y, int width, int height,
    // int bgwidth, int bgheight) {
    // int rate = 3;
    // int xbool = 0;
    // for(int i=0; i<bgwidth; i++) {
    // int ybool = xbool;
    // for(int j=0; j<bgheight; j++) {
    // if(i < x || i > x + width || j < y || j > y + height) {
    // if(ybool % rate == 0) {
    // g.drawLine(i, j, i, j);
    // }
    // }
    // ybool++;
    // }
    // xbool++;
    // }
    // }

    @Override
    public double getZoomPanelZoomFactor() {
        return this.zoomPanelRatio;
    }

    @Override
    public String getLogicPositionString() {
        return "(" + (int) this.getLogicX() + ", " + (int) this.getLogicY() + ")";
    }

    @Override
    public String getGUIPositionString() {
        return "(" + this.getGuiX() + ", " + this.getGuiY() + ")";
    }

    @Override
    protected void onChangeZoomToRect(Rectangle rect) {
        this.translateToLogicPosition(rect.x, rect.y);
        double lx = this.getLogicX(), ly = this.getLogicY(), lz = this.getLogicZ();
        double newZoomFactor = this.getZoomFactor() * Math.min((double) (this.getWidth()) / rect.width, (double) (this.getHeight()) / rect.height);
        // and set the new zoom factor
        this.onChangeZoomFactor(newZoomFactor);
        this.translateToGUIPosition(lx, ly, lz);
        this.moveView(-this.getGuiX(), -this.getGuiY());
    }
}
