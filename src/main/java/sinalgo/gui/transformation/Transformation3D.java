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

import sinalgo.configuration.Configuration;
import sinalgo.exception.SinalgoFatalException;
import sinalgo.gui.GraphPanel;
import sinalgo.io.eps.EPSOutputPrintStream;
import sinalgo.nodes.Position;

import java.awt.*;

/**
 * Transforms a logic coordinate used by the simulation to a GUI coordinate.
 * This transformation instance is to be used in 3D situations, when the nodes
 * carry 3D position information.
 */
public class Transformation3D extends PositionTransformation {

    // Transformation matrixes
    private double[][] tm = new double[4][4]; // the 4x4 transformation matrix
    private double[][] rotm = new double[4][4]; // the 4x4 transformation matrix
    private double[][] tempm = new double[4][4]; // the 4x4 transformation matrix
    private double tmAngleX, tmAngleY, tmAngleZ; // the rotation angles of tm around the x, y, and z axis

    // Two placeholders used to draw polygons
    private int[] polyLineX = new int[5];
    private int[] polyLineY = new int[5];

    // The boundaries of the gui coordinates when the graph is drawn with the
    // current transformation matrix.
    private int minX, maxX, minY, maxY;

    // The result of multiplying a vector with tm using multiply(double, double,
    // double) is stored in these members.
    private double resultX, resultY, resultZ;

    private double maxDim = Math.max(Math.max(Configuration.getDimX(), Configuration.getDimY()), Configuration.getDimZ());

    private double[][] zpm = new double[4][4]; // matrix for drawing the zoom panel
    private double zoomPanelZoom = 1; // zoom of the zoom panel

    // The boundary points of the simulation area
    private Position pos000 = new Position(0, 0, 0);
    private Position posx00 = new Position(Configuration.getDimX(), 0, 0);
    private Position pos0y0 = new Position(0, Configuration.getDimY(), 0);
    private Position pos00z = new Position(0, 0, Configuration.getDimZ());
    private Position posxy0 = new Position(Configuration.getDimX(), Configuration.getDimY(), 0);
    private Position pos0yz = new Position(0, Configuration.getDimY(), Configuration.getDimZ());
    private Position posx0z = new Position(Configuration.getDimX(), 0, Configuration.getDimZ());
    private Position posxyz = new Position(Configuration.getDimX(), Configuration.getDimY(), Configuration.getDimZ());

    private Position[] posList = new Position[8]; // list of all 8 above positions

    // The color for the 3 faces we can see through
    private Color cubeSeeThroughColor = new Color(0.96f, 0.96f, 0.96f);
    // Color for faces we look at from the back
    private Color cubeFaceColor = Color.WHITE;
    private Color cubeFaceBackColor = new Color(240, 249, 254);

    @Override
    public int getNumberOfDimensions() {
        return 3;
    }

    @Override
    protected void onChangeMoveView(int x, int y) {
        this.translate(x, y, 0, this.tm);
    }

    /**
     * Rotates the view according to a mouse gesture, along the vector (x,y). The
     * rotation may preserve the direction of the z-axis, or move freely.
     *
     * @param x             Number of pixels mouse moved in x-direction
     * @param y             Number of pixels mouse moved in y-direction
     * @param preserveZAxis true, if the z-axis should preserve its orientation
     * @param isZoomPanel   True, if the rotation origins from the zoomPanel
     */
    public void rotate(int x, int y, boolean preserveZAxis, boolean isZoomPanel) {
        // rotate around the center of the cube
        this.multiply(Configuration.getDimX() / 2, Configuration.getDimY() / 2, Configuration.getDimZ() / 2, this.tm);
        double offsetX = this.resultX, offsetY = this.resultY, offsetZ = this.resultZ;
        this.translate(-offsetX, -offsetY, -offsetZ, this.tm);

        double factor = isZoomPanel ? 0.01 : 1.5f / (this.maxDim * this.getZoomFactor()); // rotate slower with high zoom;

        if (!preserveZAxis) {
            this.rotateY(x * factor, this.tm);
            this.rotateX(-y * factor, this.tm);
        } else {
            double tmp = this.tmAngleX;
            this.rotateX(-tmp, this.tm); // do not change the direction of the z-axis
            this.rotateZ(-x * factor, this.tm);
            this.rotateX(tmp, this.tm);
            this.rotateX(-y * factor, this.tm);
        }
        this.translate(offsetX, offsetY, offsetZ, this.tm);
        this.bumpVersionNumber();
    }

    /**
     * Initializes two vectors polyLineX and polyLineY to draw a polygon between 4
     * positions.
     *
     * @param p1 The first positions
     * @param p2 The second positions
     * @param p3 The third positions
     * @param p4 The fourth positions
     */
    private void initPolyLine(Position p1, Position p2, Position p3, Position p4, double[][] matrix,
                              boolean usePerspective) {
        this.translateToGUIPosition(p1, matrix, usePerspective);
        this.polyLineX[0] = this.polyLineX[4] = this.getGuiX();
        this.polyLineY[0] = this.polyLineY[4] = this.getGuiY();
        this.translateToGUIPosition(p2, matrix, usePerspective);
        this.polyLineX[1] = this.getGuiX();
        this.polyLineY[1] = this.getGuiY();
        this.translateToGUIPosition(p3, matrix, usePerspective);
        this.polyLineX[2] = this.getGuiX();
        this.polyLineY[2] = this.getGuiY();
        this.translateToGUIPosition(p4, matrix, usePerspective);
        this.polyLineX[3] = this.getGuiX();
        this.polyLineY[3] = this.getGuiY();
    }

    /**
     * Draws a dotted line from p1 to p2.
     *
     * @param p1 The initial position
     * @param p2 The final position
     */
    private void drawDottedLine(Graphics g, Position p1, Position p2, double[][] matrix, boolean usePerspective) {
        this.translateToGUIPosition(p1, matrix, usePerspective);
        int fromX = this.getGuiX(), fromY = this.getGuiY();
        this.translateToGUIPosition(p2, matrix, usePerspective);
        int toX = this.getGuiX(), toY = this.getGuiY();
        GraphPanel.drawDottedLine(g, fromX, fromY, toX, toY);
    }

    /**
     * Draws a dotted line from p1 to p2.
     *
     * @param p1 The initial position
     * @param p2 The final position
     */
    private void drawLine(Graphics g, Position p1, Position p2, double[][] matrix, boolean usePerspective) {
        this.translateToGUIPosition(p1, matrix, usePerspective);
        int fromX = this.getGuiX(), fromY = this.getGuiY();
        this.translateToGUIPosition(p2, matrix, usePerspective);
        int toX = this.getGuiX(), toY = this.getGuiY();
        g.drawLine(fromX, fromY, toX, toY);
    }

    @Override
    public void drawBackground(Graphics g) {
        // TODO: draw rulers if specified in the config
        this.drawCube(g, this.cubeFaceColor, this.cubeFaceBackColor, this.cubeSeeThroughColor, Color.DARK_GRAY, this.tm,
                Configuration.isUsePerspectiveView());
        this.drawCubeAxeArrows(g, this.tm, Configuration.isUsePerspectiveView());
    }

    /**
     * Draw the cube of the simulation area
     *
     * @param g The graphics to draw on
     */
    private void drawCube(Graphics g, Color fgColor, Color bgColor, Color seeThroughColor, Color lineColor,
                          double[][] matrix, boolean usePerspective) {
        // draw the closing sides of the box in light gray
        this.determineVisibility(matrix, usePerspective);
        this.drawCubeBackground(g, fgColor, bgColor, seeThroughColor, matrix, usePerspective);
        this.drawCubeWireFrame(g, lineColor, matrix, usePerspective, null);
    }

    /**
     * Draws the faces of the cube
     * <p>
     * A call to determineVisibility() is required prior to calling this method.
     *
     * @param g              The graphics to draw on
     * @param fgColor        The foreground color
     * @param bgColor        The background color
     * @param matrix         The transformation matrix to use
     * @param usePerspective Whether to use the user's perspective or not
     */
    private void drawCubeBackground(Graphics g, Color fgColor, Color bgColor, Color seeThroughColor, double[][] matrix,
                                    boolean usePerspective) {
        g.setColor(seeThroughColor);
        this.initPolyLine(this.pos00z, this.posx0z, this.posxyz, this.pos0yz, matrix, usePerspective);
        g.fillPolygon(this.polyLineX, this.polyLineY, 5);

        this.initPolyLine(this.pos0y0, this.posxy0, this.posxyz, this.pos0yz, matrix, usePerspective);
        g.fillPolygon(this.polyLineX, this.polyLineY, 5);

        this.initPolyLine(this.posx00, this.posxy0, this.posxyz, this.posx0z, matrix, usePerspective);
        g.fillPolygon(this.polyLineX, this.polyLineY, 5);

        Color colXY, colYZ, colXZ;
        colXY = this.faceVisibilityXY ? fgColor : bgColor;
        colXZ = this.faceVisibilityXZ ? fgColor : bgColor;
        colYZ = this.faceVisibilityYZ ? fgColor : bgColor;

        // determine which of the 3 main faces is in front (the one with the largest
        // resultZ -> draw it last
        this.translateToGUIPosition(this.posxy0, matrix, usePerspective);
        double raiseXY = this.resultZ;
        this.translateToGUIPosition(this.pos0yz, matrix, usePerspective);
        double raiseYZ = this.resultZ;
        this.translateToGUIPosition(this.posx0z, matrix, usePerspective);
        double raiseXZ = this.resultZ;

        // determine sequence to draw the faces s.t. they overdraw themselves correctly
        if (raiseXY < raiseYZ) {
            if (raiseXY < raiseXZ) { // xy first
                this.drawFaceXY(g, colXY, matrix, usePerspective);
                if (raiseXZ < raiseYZ) { // xz yz
                    this.drawFaceXZ(g, colXZ, matrix, usePerspective);
                    this.drawFaceYZ(g, colYZ, matrix, usePerspective);
                } else { // yz xz
                    this.drawFaceYZ(g, colYZ, matrix, usePerspective);
                    this.drawFaceXZ(g, colXZ, matrix, usePerspective);
                }
            } else { // XZ XY YZ
                this.drawFaceXZ(g, colXZ, matrix, usePerspective);
                this.drawFaceXY(g, colXY, matrix, usePerspective);
                this.drawFaceYZ(g, colYZ, matrix, usePerspective);
            }
        } else { // yz < xy
            if (raiseYZ < raiseXZ) { // yz first
                this.drawFaceYZ(g, colYZ, matrix, usePerspective);
                if (raiseXY < raiseXZ) { // xy xz
                    this.drawFaceXY(g, colXY, matrix, usePerspective);
                    this.drawFaceXZ(g, colXZ, matrix, usePerspective);
                } else { // xz xy
                    this.drawFaceXZ(g, colXZ, matrix, usePerspective);
                    this.drawFaceXY(g, colXY, matrix, usePerspective);
                }
            } else { // xz < yz => xz yz xy
                this.drawFaceXZ(g, colXZ, matrix, usePerspective);
                this.drawFaceYZ(g, colYZ, matrix, usePerspective);
                this.drawFaceXY(g, colXY, matrix, usePerspective);
            }
        }
    }

    private void determineVisibility(double[][] matrix, boolean usePerspective) {
        // vectors along the axis
        this.translateToGUIPosition(0, 0, 0, matrix, usePerspective);
        double origX = this.resultX;
        double origY = this.resultY;
        this.translateToGUIPosition(this.posx00, matrix, usePerspective);
        double xX = this.resultX - origX;
        double xY = this.resultY - origY;
        this.translateToGUIPosition(this.pos0y0, matrix, usePerspective);
        double yX = this.resultX - origX;
        double yY = this.resultY - origY;
        this.translateToGUIPosition(this.pos00z, matrix, usePerspective);
        double zX = this.resultX - origX;
        double zY = this.resultY - origY;

        // cross product, test whether resulting vector points into screen or out of
        // screen)
        this.faceVisibilityXZ = xY * zX - xX * zY < 0;
        this.faceVisibilityXY = xX * yY - xY * yX < 0;
        this.faceVisibilityYZ = yX * zY - yY * zX < 0;

        // and the outer faces
        // vectors along the axis
        this.translateToGUIPosition(this.posxyz, matrix, usePerspective);
        origX = this.resultX;
        origY = this.resultY;
        this.translateToGUIPosition(this.pos0yz, matrix, usePerspective);
        xX = this.resultX - origX;
        xY = this.resultY - origY;
        this.translateToGUIPosition(this.posx0z, matrix, usePerspective);
        yX = this.resultX - origX;
        yY = this.resultY - origY;
        this.translateToGUIPosition(this.posxy0, matrix, usePerspective);
        zX = this.resultX - origX;
        zY = this.resultY - origY;

        // cross product, test whether resulting vector points into screen or out of
        // screen)
        this.faceVisibilityXZ2 = xY * zX - xX * zY > 0;
        this.faceVisibilityXY2 = xX * yY - xY * yX > 0;
        this.faceVisibilityYZ2 = yX * zY - yY * zX > 0;
    }

    private boolean faceVisibilityXZ;
    private boolean faceVisibilityXY;
    private boolean faceVisibilityYZ;
    private boolean faceVisibilityXZ2;
    private boolean faceVisibilityXY2;
    private boolean faceVisibilityYZ2;

    /**
     * Draws the face XY with the given color
     *
     * @param g              The graphics object to draw to
     * @param col            The color to draw the face in
     * @param matrix         The transformation matrix to use
     * @param usePerspective Whether to use perspective or not
     */
    private void drawFaceXY(Graphics g, Color col, double[][] matrix, boolean usePerspective) {
        g.setColor(col);
        this.initPolyLine(this.pos000, this.posx00, this.posxy0, this.pos0y0, matrix, usePerspective); // XY
        g.fillPolygon(this.polyLineX, this.polyLineY, 5);
    }

    /**
     * Draws the face YZ with the given color
     *
     * @param g              The graphics object to draw to
     * @param col            The color to draw the face in
     * @param matrix         The transformation matrix to use
     * @param usePerspective Whether to use perspective or not
     */
    private void drawFaceYZ(Graphics g, Color col, double[][] matrix, boolean usePerspective) {
        g.setColor(col);
        this.initPolyLine(this.pos000, this.pos0y0, this.pos0yz, this.pos00z, matrix, usePerspective); // YZ
        g.fillPolygon(this.polyLineX, this.polyLineY, 5);
    }

    /**
     * Draws the face XZ with the given color
     *
     * @param g              The graphics object to draw to
     * @param col            The color to draw the face in
     * @param matrix         The transformation matrix to use
     * @param usePerspective Whether to use perspective or not
     */
    private void drawFaceXZ(Graphics g, Color col, double[][] matrix, boolean usePerspective) {
        g.setColor(col);
        this.initPolyLine(this.pos000, this.posx00, this.posx0z, this.pos00z, matrix, usePerspective); // XZ
        g.fillPolygon(this.polyLineX, this.polyLineY, 5);
    }

    /**
     * Draws a line, either to the screen or to postscript
     *
     * @param g              The graphics to draw on
     * @param from           The initial position
     * @param to             The final position
     * @param matrix         The transformation matrix to use
     * @param usePerspective Whether to use perspective or not
     * @param pw             The printstream
     */
    private void helperDrawLine(Graphics g, Position from, Position to, double[][] matrix, boolean usePerspective,
                                EPSOutputPrintStream pw) {
        if (g != null) {
            this.drawLine(g, from, to, matrix, usePerspective);
        } else {
            this.drawLineToPostScript(pw, from, to);
        }
    }

    /**
     * Draws a dotted line, either to the screen or to postscript
     *
     * @param g              The graphics to draw on
     * @param from           The initial position
     * @param to             The final position
     * @param matrix         The transformation matrix to use
     * @param usePerspective Whether to use perspective or not
     * @param pw             The printstream
     */
    private void helperDrawDottedLine(Graphics g, Position from, Position to, double[][] matrix, boolean usePerspective,
                                      EPSOutputPrintStream pw) {
        if (g != null) {
            this.drawDottedLine(g, from, to, matrix, usePerspective);
        } else {
            this.drawDottedLineToPostScript(pw, from, to);
        }
    }

    /**
     * Draws the wireframe of the cube, dotting the hidden lines
     *
     * @param g              The graphics to draw on
     * @param lineColor      The color of the line
     * @param matrix         The transformation matrix to use
     * @param usePerspective Whether to use perspective or not
     * @param pw             The printstream
     */
    private void drawCubeWireFrame(Graphics g, Color lineColor, double[][] matrix, boolean usePerspective,
                                   EPSOutputPrintStream pw) {
        // Draw the wire frame
        if (g != null) {
            g.setColor(lineColor);
        }

        // 000 -> x00
        if (this.faceVisibilityXY && this.faceVisibilityXZ) {
            this.helperDrawDottedLine(g, this.pos000, this.posx00, matrix, usePerspective, pw);
        } else {
            this.helperDrawLine(g, this.pos000, this.posx00, matrix, usePerspective, pw);
        }

        // 000 -> 00z
        if (this.faceVisibilityXZ && this.faceVisibilityYZ) {
            this.helperDrawDottedLine(g, this.pos000, this.pos00z, matrix, usePerspective, pw);
        } else {
            this.helperDrawLine(g, this.pos000, this.pos00z, matrix, usePerspective, pw);
        }

        // 000 -> 0y0
        if (this.faceVisibilityYZ && this.faceVisibilityXY) {
            this.helperDrawDottedLine(g, this.pos000, this.pos0y0, matrix, usePerspective, pw);
        } else {
            this.helperDrawLine(g, this.pos000, this.pos0y0, matrix, usePerspective, pw);
        }

        // xz0 -> 00z
        if (this.faceVisibilityXZ && this.faceVisibilityXY2) {
            this.helperDrawDottedLine(g, this.posx0z, this.pos00z, matrix, usePerspective, pw);
        } else {
            this.helperDrawLine(g, this.posx0z, this.pos00z, matrix, usePerspective, pw);
        }

        // xz0 -> xyz
        if (this.faceVisibilityYZ2 && this.faceVisibilityXY2) {
            this.helperDrawDottedLine(g, this.posx0z, this.posxyz, matrix, usePerspective, pw);
        } else {
            this.helperDrawLine(g, this.posx0z, this.posxyz, matrix, usePerspective, pw);
        }

        // xz0 -> x00
        if (this.faceVisibilityXZ && this.faceVisibilityYZ2) {
            this.helperDrawDottedLine(g, this.posx0z, this.posx00, matrix, usePerspective, pw);
        } else {
            this.helperDrawLine(g, this.posx0z, this.posx00, matrix, usePerspective, pw);
        }

        // 0yz -> 00z
        if (this.faceVisibilityYZ && this.faceVisibilityXY2) {
            this.helperDrawDottedLine(g, this.pos0yz, this.pos00z, matrix, usePerspective, pw);
        } else {
            this.helperDrawLine(g, this.pos0yz, this.pos00z, matrix, usePerspective, pw);
        }

        // 0yz -> xyz
        if (this.faceVisibilityXZ2 && this.faceVisibilityXY2) {
            this.helperDrawDottedLine(g, this.pos0yz, this.posxyz, matrix, usePerspective, pw);
        } else {
            this.helperDrawLine(g, this.pos0yz, this.posxyz, matrix, usePerspective, pw);
        }

        // 0yz -> 0y0
        if (this.faceVisibilityYZ && this.faceVisibilityXZ2) {
            this.helperDrawDottedLine(g, this.pos0yz, this.pos0y0, matrix, usePerspective, pw);
        } else {
            this.helperDrawLine(g, this.pos0yz, this.pos0y0, matrix, usePerspective, pw);
        }

        // xy0 -> xyz
        if (this.faceVisibilityXZ2 && this.faceVisibilityYZ2) {
            this.helperDrawDottedLine(g, this.posxy0, this.posxyz, matrix, usePerspective, pw);
        } else {
            this.helperDrawLine(g, this.posxy0, this.posxyz, matrix, usePerspective, pw);
        }

        // xy0 -> x00
        if (this.faceVisibilityXY && this.faceVisibilityYZ2) {
            this.helperDrawDottedLine(g, this.posxy0, this.posx00, matrix, usePerspective, pw);
        } else {
            this.helperDrawLine(g, this.posxy0, this.posx00, matrix, usePerspective, pw);
        }

        // xy0 -> 0y0
        if (this.faceVisibilityXY && this.faceVisibilityXZ2) {
            this.helperDrawDottedLine(g, this.posxy0, this.pos0y0, matrix, usePerspective, pw);
        } else {
            this.helperDrawLine(g, this.posxy0, this.pos0y0, matrix, usePerspective, pw);
        }
    }

    /**
     * Draw the arrow and names of the axes
     *
     * @param g The graphics to draw on
     */
    private void drawCubeAxeArrows(Graphics g, double[][] matrix, boolean usePerspective) {
        this.translateToGUIPosition(this.pos000, matrix, usePerspective);
        int originX = this.getGuiX();
        int originY = this.getGuiY();
        g.setFont(new Font("", Font.PLAIN, 10));
        this.translateToGUIPosition(Configuration.getDimX(), 0, 0, matrix, usePerspective);
        this.drawAxeName(g, "x", originX, originY, this.getGuiX(), this.getGuiY());
        this.translateToGUIPosition(0, Configuration.getDimY(), 0, matrix, usePerspective);
        this.drawAxeName(g, "y", originX, originY, this.getGuiX(), this.getGuiY());
        this.translateToGUIPosition(0, 0, Configuration.getDimZ(), matrix, usePerspective);
        this.drawAxeName(g, "z", originX, originY, this.getGuiX(), this.getGuiY());
    }

    /**
     * Draws an arrow and an axe labeling for a given axis
     *
     * @param g       The graphics
     * @param name    name of the axe
     * @param originX X-coord of origin
     * @param originY Y-coord of origin
     * @param toX     X-coord of end-point of axis
     * @param toY     Y-coord of end-point of axis
     */
    private void drawAxeName(Graphics g, String name, int originX, int originY, int toX, int toY) {
        double factor = 1 / Math.max(0.00000001,
                Math.sqrt((toX - originX) * (toX - originX) + (toY - originY) * (toY - originY)));
        // unit vector in direction of axis
        double ux = (toX - originX) * factor;
        double uy = (toY - originY) * factor;
        // draw a line to connect the arrow with the axis
        g.drawLine(toX, toY, (int) (toX + 10 * ux), (int) (toY + 10 * uy));
        this.drawArrow(toX, toY, toX + (int) (14 * ux), toY + (int) (14 * uy), g);
        g.drawString(name, toX + (int) (18 * ux), toY + (int) (18 * uy));
    }

    /**
     * Draws an arrow in the direction (x1, y1) to (x2, y2) [given in gui
     * coordinates]
     *
     * @param x1 The initial x coordinate
     * @param y1 The initial y coordinate
     * @param x2 The final x coordinate
     * @param y2 The final y coordinate
     * @param g  The graphics to draw on
     */
    private void drawArrow(int x1, int y1, int x2, int y2, Graphics g) {
        int length = 10;
        int width = 2;

        double factor = 1 / (Math.max(0.00000001, Math.sqrt(Math.pow((x1 - x2), 2) + (Math.pow((y1 - y2), 2)))));

        double[] c = new double[2];
        c[0] = (x1 - x2) * factor;
        c[1] = (y1 - y2) * factor;

        double[] ae = new double[2];

        ae[0] = x2 + length * c[0];
        ae[1] = y2 + length * c[1];

        double[] d = new double[2];
        d[0] = c[1];
        d[1] = -c[0];

        int arrowX[] = new int[4];
        int arrowY[] = new int[4];

        arrowX[0] = arrowX[3] = Math.round(x2);
        arrowX[1] = (int) Math.round((ae[0] + width * d[0]));
        arrowX[2] = (int) Math.round((ae[0] - width * d[0]));

        arrowY[0] = arrowY[3] = Math.round(y2);
        arrowY[1] = (int) Math.round((ae[1] + width * d[1]));
        arrowY[2] = (int) Math.round((ae[1] - width * d[1]));

        g.drawPolyline(arrowX, arrowY, 4);
        g.fillPolygon(arrowX, arrowY, 4);
    }

    @Override
    public void drawBackgroundToPostScript(EPSOutputPrintStream pw) {
        pw.println("%the background");

        if (!Configuration.isEpsDrawBackgroundWhite()) {
            pw.setColor(250, 250, 250);
        } else {
            pw.setColor(255, 255, 255);
        }

        this.drawPolygonToPostScript(pw, this.pos00z, this.posx0z, this.posxyz, this.pos0yz);
        this.drawPolygonToPostScript(pw, this.pos0y0, this.posxy0, this.posxyz, this.pos0yz);
        this.drawPolygonToPostScript(pw, this.posx00, this.posxy0, this.posxyz, this.posx0z);

        if (!Configuration.isEpsDrawBackgroundWhite()) {
            pw.setColor(240, 240, 240);
        }

        this.drawPolygonToPostScript(pw, this.pos000, this.posx00, this.posxy0, this.pos0y0);
        this.drawPolygonToPostScript(pw, this.pos000, this.posx00, this.posx0z, this.pos00z);
        this.drawPolygonToPostScript(pw, this.pos000, this.pos0y0, this.pos0yz, this.pos00z);

        pw.setColor(0, 0, 0);

        this.determineVisibility(this.tm, Configuration.isUsePerspectiveView());
        this.drawCubeWireFrame(null, null, null, false, pw);

        // // .........
        // drawLineToPostScript(pw, pos000, posx00);
        // drawLineToPostScript(pw, pos000, pos0y0);
        // drawLineToPostScript(pw, pos000, pos00z);
        //
        // drawLineToPostScript(pw, posx0z, pos00z);
        // drawLineToPostScript(pw, posx0z, posx00);
        //
        // drawLineToPostScript(pw, pos0yz, pos00z);
        // drawLineToPostScript(pw, pos0yz, pos0y0);
        //
        // drawLineToPostScript(pw, posxy0, posx00);
        // drawLineToPostScript(pw, posxy0, pos0y0);
        //
        // pw.println("%the dotted lines for the background.");
        // pw.println("[2 2] 0 setdash");
        // drawLineToPostScript(pw, posx0z, posxyz);
        // drawLineToPostScript(pw, pos0yz, posxyz);
        // drawLineToPostScript(pw, posxy0, posxyz);
        // pw.println("[] 0 setdash\n");

        this.translateToGUIPosition(this.pos000);
        double originX = this.getGuiXDouble();
        double originY = this.getGuiYDouble();
        this.translateToGUIPosition(Configuration.getDimX(), 0, 0);
        this.drawAxesToPostScript(pw, "x", originX, originY, this.getGuiXDouble(), this.getGuiYDouble());
        this.translateToGUIPosition(0, Configuration.getDimY(), 0);
        this.drawAxesToPostScript(pw, "y", originX, originY, this.getGuiXDouble(), this.getGuiYDouble());
        this.translateToGUIPosition(0, 0, Configuration.getDimZ());
        this.drawAxesToPostScript(pw, "z", originX, originY, this.getGuiXDouble(), this.getGuiYDouble());
    }

    private void drawAxesToPostScript(EPSOutputPrintStream pw, String name, double originX, double originY, double toX,
                                      double toY) {
        double factor = 1 / Math.max(0.00000001,
                Math.sqrt((toX - originX) * (toX - originX) + (toY - originY) * (toY - originY)));
        // unit vector in direction of axis
        double ux = (toX - originX) * factor;
        double uy = (toY - originY) * factor;
        // draw a line to connect the arrow with the axis
        // pw.drawLine(toX, toY, (toX + 10*ux), toY + 10*uy);
        pw.drawArrow(toX, toY, (toX + 14 * ux), toY + 14 * uy);
        pw.setFontSize(12);
        pw.drawText(name, toX + 18 * ux, toY + 18 * uy);
    }

    private void drawPolygonToPostScript(EPSOutputPrintStream pw, Position p1, Position p2, Position p3, Position p4) {
        this.translateToGUIPosition(p1);
        double p1X = this.getGuiXDouble(), p1Y = this.getGuiYDouble();
        this.translateToGUIPosition(p2);
        double p2X = this.getGuiXDouble(), p2Y = this.getGuiYDouble();
        this.translateToGUIPosition(p3);
        double p3X = this.getGuiXDouble(), p3Y = this.getGuiYDouble();
        this.translateToGUIPosition(p4);
        double p4X = this.getGuiXDouble(), p4Y = this.getGuiYDouble();
        pw.drawFilledPolygon(p1X, p1Y, p2X, p2Y, p3X, p3Y, p4X, p4Y);
    }

    /**
     * Draw a line between two points
     *
     * @param pw   The stream to write the Postscript commands to
     * @param from Start point of line
     * @param to   End point of line
     */
    private void drawLineToPostScript(EPSOutputPrintStream pw, Position from, Position to) {
        this.translateToGUIPosition(from);
        double fromX = this.getGuiXDouble();
        double fromY = this.getGuiYDouble();
        this.translateToGUIPosition(to);
        pw.drawLine(fromX, fromY, this.getGuiXDouble(), this.getGuiYDouble());
    }

    /**
     * Draw a dotted line between two points
     *
     * @param pw   The stream to write the Postscript commands to
     * @param from Start point of line
     * @param to   End point of line
     */
    private void drawDottedLineToPostScript(EPSOutputPrintStream pw, Position from, Position to) {
        pw.println("[2 2] 0 setdash\n");
        this.translateToGUIPosition(from);
        double fromX = this.getGuiXDouble();
        double fromY = this.getGuiYDouble();
        this.translateToGUIPosition(to);
        pw.drawLine(fromX, fromY, this.getGuiXDouble(), this.getGuiYDouble());
        pw.println("[] 0 setdash\n");
    }

    @Override
    protected void onChangeZoomFactor(double zoomfactor) {
        this.scaleInTheMiddleOfScreen(zoomfactor / this.getZoomFactor(), this.tm);
    }

    /**
     * Scales the transformation matrix s.t. the current center of the visible
     * screen remains in the center. This method does not set the zoom factor to
     * this transformation object.
     *
     * @param deltaZoom The factor of the scaling
     * @param matrix    The transformation matrix
     */
    private void scaleInTheMiddleOfScreen(double deltaZoom, double[][] matrix) {
        this.scale(deltaZoom, matrix);
        // move the window back s.t. the center of the screen before zooming lies again
        // in the center of the screen
        this.translate(this.getWidth() / 2 * (1 - deltaZoom), this.getHeight() / 2 * (1 - deltaZoom), 0, matrix);
    }

    /**
     * Scales the transformation matrix, but does not set the zoom factor to this
     * transformation object.
     *
     * @param deltaZoom The factor of the scaling
     * @param matrix    The transformation matrix
     */
    private void scaleInTheMiddleOfCube(double deltaZoom, double[][] matrix) {
        this.multiply(Configuration.getDimX() / 2, Configuration.getDimY() / 2, Configuration.getDimZ() / 2, matrix);
        double offsetX = this.resultX;
        double offsetY = this.resultY;
        double offsetZ = this.resultZ;
        this.translate(-offsetX, -offsetY, -offsetZ, matrix);
        this.scale(deltaZoom, matrix);
        this.translate(offsetX, offsetY, offsetZ, matrix);
    }

    @Override
    protected void onChangeZoomToFit(int width, int height) {
        this.setZoomFactor(this.getZoomFactor() * this.zoomToFit(width, height, this.tm, Configuration.isUsePerspectiveView()));
    }

    @Override
    protected void onChangeDefaultView(int width, int height) {
        this.rotateToDefault(this.tm);
        this.onChangeZoomToFit(width, height);
    }

    /**
     * Rotates the cube such that the z-axis becomes a single point, and sets the
     * zoomfactor such that the image nicely fits on the screen.
     *
     * @param width  The width of the window
     * @param height The height of the window
     */
    public void defaultViewXY(int width, int height) {
        this.reset(this.tm);
        this.onChangeZoomToFit(width, height);
        this.bumpVersionNumber();
    }

    /**
     * Rotates the cube such that the y-axis becomes a single point, and sets the
     * zoomfactor such that the image nicely fits on the screen.
     *
     * @param width  The width of the window
     * @param height The height of the window
     */
    public void defaultViewXZ(int width, int height) {
        this.reset(this.tm);
        this.rotateX(Math.PI / 2, this.tm);
        this.onChangeZoomToFit(width, height);
        this.bumpVersionNumber();
    }

    /**
     * Rotates the cube such that the x-axis becomes a single point, and sets the
     * zoomfactor such that the image nicely fits on the screen.
     *
     * @param width  The width of the window
     * @param height The height of the window
     */
    public void defaultViewYZ(int width, int height) {
        this.reset(this.tm);
        this.rotateZ(Math.PI / 2, this.tm);
        this.rotateX(Math.PI / 2, this.tm);
        this.onChangeZoomToFit(width, height);
        this.bumpVersionNumber();
    }

    /**
     * Rotates the transformation matrix to the default perspective
     *
     * @param matrix The transformation matrix
     */
    private void rotateToDefault(double[][] matrix) {
        // set the rotation matrix such that it looks nice.
        this.reset(matrix);
        // rotate around the center of the cube
        this.multiply(Configuration.getDimX() / 2, Configuration.getDimY() / 2, Configuration.getDimZ() / 2, this.tm);
        double offsetX = this.resultX, offsetY = this.resultY, offsetZ = this.resultZ;
        this.translate(-offsetX, -offsetY, -offsetZ, matrix);

        this.rotateZ(Math.PI / 2, matrix);
        this.rotateX(Math.PI / 2, matrix);
        this.rotateY(-Math.PI / 6, matrix);
        this.rotateX(-Math.PI / 8, matrix);

        this.translate(offsetX, offsetY, offsetZ, matrix);
    }

    /**
     * Translates, and scales the matrix such that it nicely fits into a given
     * window. Does not update the scaling factor!
     *
     * @param width  The width of the graphics object
     * @param height The height of the graphics object
     * @param matrix The transformation matrix
     * @return The factor by which the zoom factor was multiplied
     */
    private double zoomToFit(int width, int height, double[][] matrix, boolean usePerspective) {
        int axesOffset = 30; // border to allow for names of axes
        this.determineBoundingBox(matrix, usePerspective);
        double currentWidth = this.maxX - this.minX; // add some space for the axe naming
        double currentHeight = this.maxY - this.minY;
        double delta = Math.min((width - 2 * axesOffset) / currentWidth, (height - 2 * axesOffset) / currentHeight);
        this.scaleInTheMiddleOfCube(delta, matrix);
        this.determineBoundingBox(matrix, usePerspective);
        this.translate(-this.minX + (width - (this.maxX - this.minX)) / 2, -this.minY + (height - (this.maxY - this.minY)) / 2, 0, matrix);
        return delta;
    }

    /**
     * Scales the transformation matrix by a factor.
     *
     * @param factor The scaling factor
     * @param matrix The transaformation matrix
     */
    private void scale(double factor, double[][] matrix) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {
                matrix[i][j] *= factor;
            }
        }
    }

    private void rotateX(double angle, double[][] matrix) {
        if (matrix == this.tm) {
            this.tmAngleX += angle;
        }
        // top row
        this.rotm[0][0] = 1;
        this.rotm[0][1] = 0;
        this.rotm[0][2] = 0;
        this.rotm[0][3] = 0;

        this.rotm[1][0] = 0;
        this.rotm[1][1] = Math.cos(angle);
        this.rotm[1][2] = -Math.sin(angle);
        this.rotm[1][3] = 0;

        this.rotm[2][0] = 0;
        this.rotm[2][1] = Math.sin(angle);
        this.rotm[2][2] = Math.cos(angle);
        this.rotm[2][3] = 0;

        this.rotm[3][0] = 0;
        this.rotm[3][1] = 0;
        this.rotm[3][2] = 0;
        this.rotm[3][3] = 1;
        this.multiply(matrix);
    }

    private void rotateY(double angle, double[][] matrix) {
        if (matrix == this.tm) {
            this.tmAngleY += angle;
        }
        // top row
        this.rotm[0][0] = Math.cos(angle);
        this.rotm[0][1] = 0;
        this.rotm[0][2] = Math.sin(angle);
        this.rotm[0][3] = 0;

        this.rotm[1][0] = 0;
        this.rotm[1][1] = 1;
        this.rotm[1][2] = 0;
        this.rotm[1][3] = 0;

        this.rotm[2][0] = -Math.sin(angle);
        this.rotm[2][1] = 0;
        this.rotm[2][2] = Math.cos(angle);
        this.rotm[2][3] = 0;

        this.rotm[3][0] = 0;
        this.rotm[3][1] = 0;
        this.rotm[3][2] = 0;
        this.rotm[3][3] = 1;
        this.multiply(matrix);
    }

    private void rotateZ(double angle, double[][] matrix) {
        if (matrix == this.tm) {
            this.tmAngleZ += angle;
        }
        // top row
        this.rotm[0][0] = Math.cos(angle);
        this.rotm[0][1] = -Math.sin(angle);
        this.rotm[0][2] = 0;
        this.rotm[0][3] = 0;

        this.rotm[1][0] = Math.sin(angle);
        this.rotm[1][1] = Math.cos(angle);
        this.rotm[1][2] = 0;
        this.rotm[1][3] = 0;

        this.rotm[2][0] = 0;
        this.rotm[2][1] = 0;
        this.rotm[2][2] = 1;
        this.rotm[2][3] = 0;

        this.rotm[3][0] = 0;
        this.rotm[3][1] = 0;
        this.rotm[3][2] = 0;
        this.rotm[3][3] = 1;
        this.multiply(matrix);
    }

    /**
     * Translates the image by the current offset
     *
     * @param x      The offset's x component
     * @param y      The offset's y component
     * @param z      The offset's z component
     * @param matrix The transformation matrix
     */
    private void translate(double x, double y, double z, double[][] matrix) {
        matrix[0][3] += x;
        matrix[1][3] += y;
        matrix[2][3] += z;
    }

    /**
     * Multiplies tm = rotm * tm. Temporarily stores the result in tempm.
     */
    private void multiply(double[][] matrix) {
        // multiplies rotm * tm, and stores the result in tm
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                double sum = 0;
                for (int k = 0; k < 4; k++) {
                    sum += this.rotm[i][k] * matrix[k][j];
                }
                this.tempm[i][j] = sum;
            }
        }
        for (int i = 0; i < 4; i++) {
            System.arraycopy(this.tempm[i], 0, matrix[i], 0, 4);
        }
    }

    /**
     * Determines the 2D bounding box of the cube. It stores the extermal
     * coordinates of the cube in the member variables minX, maxX, minY, maxY.
     */
    private void determineBoundingBox(double[][] matrix, boolean usePerspective) {
        this.translateToGUIPosition(this.posList[0], matrix, usePerspective);
        this.minX = this.maxX = this.getGuiX();
        this.minY = this.maxY = this.getGuiY();
        for (int i = 1; i < 8; i++) {
            this.translateToGUIPosition(this.posList[i], matrix, usePerspective);
            if (this.getGuiX() < this.minX) {
                this.minX = this.getGuiX();
            }
            if (this.getGuiX() > this.maxX) {
                this.maxX = this.getGuiX();
            }
            if (this.getGuiY() < this.minY) {
                this.minY = this.getGuiY();
            }
            if (this.getGuiY() > this.maxY) {
                this.maxY = this.getGuiY();
            }
        }
    }

    /**
     * Default constructor
     */
    public Transformation3D() {
        this.posList[0] = this.pos000;
        this.posList[1] = this.posx00;
        this.posList[2] = this.pos0y0;
        this.posList[3] = this.pos00z;
        this.posList[4] = this.posxy0;
        this.posList[5] = this.posx0z;
        this.posList[6] = this.pos0yz;
        this.posList[7] = this.posxyz;

        this.reset(this.tm);
    }

    /**
     * Resets the transformation matrix
     */
    private void reset(double[][] matrix) {
        if (matrix == this.tm) {
            this.tmAngleX = this.tmAngleY = this.tmAngleZ = 0;
            this.setZoomFactor(1);
        }
        // initialize the default transformation matrix (no rotation at all)
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                matrix[i][j] = i == j ? 1 : 0;
            }
        }
    }

    /**
     * Multiply tm with a given vector. The result is stored in the member variables
     * resultX, resultY, and resultZ.
     *
     * @param x the x-coordinate of the vector to multiply with tm.
     * @param y the y-coordinate of the vector to multiply with tm.
     * @param z the z-coordinate of the vector to multiply with tm.
     */
    private void multiply(double x, double y, double z, double[][] matrix) {
        // Note that we negate the logic y value to mirror the cube, such as to obtain
        // the usual x y z coordinate system orientation
        this.resultX = x * matrix[0][0] - y * matrix[0][1] + z * matrix[0][2] + matrix[0][3];
        this.resultY = x * matrix[1][0] - y * matrix[1][1] + z * matrix[1][2] + matrix[1][3];
        this.resultZ = x * matrix[2][0] - y * matrix[2][1] + z * matrix[2][2] + matrix[2][3];
    }

    @Override
    public void translateToGUIPosition(double x, double y, double z) {
        this.translateToGUIPosition(x, y, z, this.tm, Configuration.isUsePerspectiveView());
    }

    /**
     * Same as translateToGUIPosition, but utilizes the specified transformation
     * matrix
     *
     * @param x      The offset's x component
     * @param y      The offset's y component
     * @param z      The offset's z component
     * @param matrix The matrix which defines the transformation
     */
    private void translateToGUIPosition(double x, double y, double z, double[][] matrix, boolean usePerspective) {
        this.multiply(x, y, z, matrix);
        // we project onto the X/Y field
        // possibly add some perspective
        if (usePerspective) {
            double perspectiveZ = Configuration.getPerspectiveViewDistance() * this.maxDim * this.getZoomFactor();
            this.resultX = this.getWidth() / 2 + (this.getWidth() / 2 - this.resultX) * perspectiveZ / (this.resultZ - perspectiveZ);
            this.resultY = this.getHeight() / 2 + (this.getHeight() / 2 - this.resultY) * perspectiveZ / (this.resultZ - perspectiveZ);
        }

        this.setGuiXDouble(this.resultX);
        this.setGuiYDouble(this.resultY);
        this.setGuiX((int) this.resultX);
        this.setGuiY((int) this.resultY);
    }

    /**
     * @param pos    The logic position to translate to a GUI position
     * @param matrix The rotation matrix to use
     */
    private void translateToGUIPosition(Position pos, double[][] matrix, boolean usePerspective) {
        this.translateToGUIPosition(pos.getXCoord(), pos.getYCoord(), pos.getZCoord(), matrix, usePerspective);
    }

    /**
     * The GUI coordinates are obtained by applying this transformation object
     * (rotation, translation and scaling) to a position and then projecting it on
     * the X/Y plane by setting the z-coordinate to zero. The members guiX and guiY
     * correspond to the x- and y-coordinate of the translated position. This method
     * returns the corresponding z-coordinate of a translated position.
     *
     * @param pos The logic position to translate
     * @return The z-coordinate of
     */
    public double translateToGUIPositionAndGetZOffset(Position pos) {
        this.translateToGUIPosition(pos.getXCoord(), pos.getYCoord(), pos.getZCoord(), this.tm, Configuration.isUsePerspectiveView());
        return this.resultZ;
    }

    @Override
    public void translateToGUIPosition(Position pos) {
        this.translateToGUIPosition(pos.getXCoord(), pos.getYCoord(), pos.getZCoord(), this.tm, Configuration.isUsePerspectiveView());
    }

    @Override
    public boolean supportReverseTranslation() {
        return false;
    }

    @Override
    public void translateToLogicPosition(int x, int y) {
        throw new SinalgoFatalException("Trying to translate a GUI coordinate to a 3D coordinate, even though this "
                + "is not supported in 3D!");
    }

    @Override
    public void drawZoomPanel(Graphics g, int side, int offsetX, int offsetY, int bgwidth, int bgheight) {
        // copy the current transformation matrix
        for (int i = 0; i < 4; i++) {
            System.arraycopy(this.tm[i], 0, this.zpm[i], 0, 4);
        }

        this.zoomPanelZoom = this.getZoomFactor() * this.zoomToFit(side, side, this.zpm, false); // note: we copied tm to zpm
        // draw the background first in gray, then at the end, paint the visible part in
        // white
        this.determineVisibility(this.zpm, false);
        Color faceColor = new Color(0.8f, 0.8f, 0.8f);
        this.drawCubeBackground(g, faceColor, faceColor, new Color(0.85f, 0.85f, 0.85f), this.zpm, false);

        this.determineBoundingBox(this.zpm, false);
        offsetX += this.minX;
        offsetY += this.minY;
        int boundingBoxWidth = this.maxX - this.minX;
        int boundingBoxHeight = this.maxY - this.minY;

        this.determineBoundingBox(this.tm, false);

        double dimX = (this.maxX - this.minX) / this.getZoomFactor();
        double dimY = (this.maxY - this.minY) / this.getZoomFactor();
        this.translateToGUIPosition(0, 0, 0, this.zpm, false);

        int ax = (int) (this.zoomPanelZoom * dimX * (-this.minX) / (this.maxX - this.minX));
        int ay = (int) (this.zoomPanelZoom * dimY * (-this.minY) / (this.maxY - this.minY));
        int bx = (int) (this.zoomPanelZoom * dimX * (this.getWidth() - this.minX) / (this.maxX - this.minX));
        int by = (int) (this.zoomPanelZoom * dimY * (this.getHeight() - this.minY) / (this.maxY - this.minY));
        ax = Math.max(0, ax);
        ay = Math.max(0, ay);
        bx = Math.min(boundingBoxWidth, bx);
        by = Math.min(boundingBoxHeight, by);

        Shape oldClip = g.getClip();

        // draw a red line around the visible part
        g.setClip(offsetX + ax - 1, offsetY + ay - 1, bx - ax + 2, by - ay + 2);
        this.drawCubeBackground(g, Color.RED, Color.RED, Color.RED, this.zpm, false);
        // ...but not on the wireframe
        this.drawCubeWireFrame(g, Color.BLACK, this.zpm, false, null);
        g.setClip(offsetX + ax, offsetY + ay, bx - ax, by - ay);
        // and draw in white the visible area
        this.drawCubeBackground(g, this.cubeFaceColor, this.cubeFaceBackColor, this.cubeSeeThroughColor, this.zpm, false);
        g.setClip(oldClip);
        // put on top the wire frame and the axes
        this.drawCubeWireFrame(g, Color.BLACK, this.zpm, false, null);
        this.drawCubeAxeArrows(g, this.zpm, false);
    }

    @Override
    public double getZoomPanelZoomFactor() {
        return this.zoomPanelZoom;
    }

    @Override
    public String getLogicPositionString() {
        return "(" + this.getLogicX() + ", " + this.getLogicY() + ", " + this.getLogicZ() + ")";
    }

    @Override
    public String getGUIPositionString() {
        return "(" + this.getGuiX() + ", " + this.getGuiY() + ")";
    }

    @Override
    protected void onChangeZoomToRect(Rectangle rect) {
        double delta = Math.min((double) (this.getWidth()) / rect.width, (double) (this.getHeight()) / rect.height);
        this.onChangeMoveView(-rect.x, -rect.y);
        this.scale(delta, this.tm);
        this.setZoomFactor(this.getZoomFactor() * delta);
    }
}
