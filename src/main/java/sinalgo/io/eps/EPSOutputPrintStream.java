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
package sinalgo.io.eps;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import sinalgo.exception.SinalgoFatalException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;

/**
 * Implements a state machine to print eps commands to a file. It helps the user
 * to output a graph to an eps. Implements some basic macros, lets the user add
 * custom macros, handles the eps header and trailer and offers an interface to
 * easilly print lines, circles and arrows.
 * <p>
 * Note that this Stream is a state machine. So all the setings (like the width
 * of the lines) are used for all successive lines until you reset it.
 * <p>
 * Note that the eps coordinate system is not the same as the one used in the
 * framework. The (0/0) point in the framework is (according to the drawing
 * routines of swing and awt) in the left upper corner whereas in eps it is in
 * the left lower corner. This makes it necessary to mirror the y coordinates
 * according to the bounding box. When using the drawing methods provided by
 * this class the mirroring is done automatically. When you are writing your own
 * direct output to the file use the mirror-method to mirror the y coordinates
 * correctly.
 */
@Getter(AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public class EPSOutputPrintStream extends PrintStream {

    private int boundingBoxX;
    private int boundingBoxY;
    private int boundingBoxWidth;
    private int boundingBoxHeight;
    private HashMap<String, String> macros = new HashMap<>();
    private double colorR;
    private double colorG;
    private double colorB;
    private double lineWidth = 1.0;

    /**
     * The length of the arrows for all the successive arrows until the next
     * call of setArrowLength.
     *
     * @param arrowLength The length of the Arrows.
     */
    @Setter
    private double arrowLength = 10;

    /**
     * The width of the arrows for all the successive arrows until the next
     * call of setArrowWidth.
     *
     * @param width The width of the Arrows.
     */
    @Setter
    private double arrowWidth = 2;

    private int fontSize = 12;
    private String font = "Courier";

    /**
     * Creates an EPSOutputStream for a given File. Prints the eps-commands to the
     * given file.
     *
     * @param outputFile The file to write the eps-commands to.
     * @throws FileNotFoundException Thrown if the PrintStream could not be opened for the given file.
     */
    public EPSOutputPrintStream(File outputFile) throws FileNotFoundException {
        super(outputFile);
    }

    // settings with output to the file

    /**
     * Sets the with of the line to a given value. Note that this method sets the
     * general width of all lines printed until the next call of setLineWidth. If
     * the line width is the same as before the call, no output is written to the
     * file.
     *
     * @param width The width of the all successive lines drawn until the next call of
     *              setLineWidth.
     */
    public void setLineWidth(double width) {
        if (this.getLineWidth() != width) {
            // only print it if the width changed.
            this.lineWidth = width;
            this.println("0 " + this.getLineWidth() + " dtransform truncate idtransform setlinewidth pop");
        }
    }

    /**
     * Sets the color of the state machine to the given one. All the successive
     * drawing actions until the next call of the setColor method are drawn in the
     * given color. The default color of the eps drawing calls is black.
     *
     * @param r The red component of the color to set.
     * @param g The green component of the color to set.
     * @param b The blue component of the color to set.
     */
    public void setColor(int r, int g, int b) {
        double dR = this.mapIntColToDoubleCol(r), dG = this.mapIntColToDoubleCol(g), dB = this.mapIntColToDoubleCol(b);
        if ((this.getColorR() != dR) || (this.getColorG() != dG) || (this.getColorB() != dB)) {
            // only print the statement, if the color changed
            this.setColorR(dR);
            this.setColorG(dG);
            this.setColorB(dB);
            this.println(this.getColorR() + " " + this.getColorG() + " " + this.getColorB() + " setrgbcolor");
        }
    }

    private double mapIntColToDoubleCol(int initial) {
        return initial / 255.0;
    }

    // settings without output to the file.

    /**
     * Sets the bounding box for the eps. Note that in order really set the bounding
     * box of the resulting eps you have to call this method before the writeHeader
     * function.
     *
     * @param x      The x coordinate of the bounding box startpoint.
     * @param y      The y coordinate of the bounding box startpoint.
     * @param width  The width of the bounding box.
     * @param height The height of the bounding box.
     */
    public void setBoundingBox(int x, int y, int width, int height) {
        this.setBoundingBoxX(x);
        this.setBoundingBoxY(y);
        this.setBoundingBoxWidth(width);
        this.setBoundingBoxHeight(height);
    }

    // helpers

    /**
     * Draws a line from the given startpoint to the given endpoint using the macro
     * 'line' which is a default macro that is available in the file if you called
     * writeMacros before. Note that it appears in the color you set last with the
     * setColor method.
     *
     * @param startX The x coordinate of the startpoint.
     * @param startY The y coordinate of the startpoint.
     * @param endX   The x coordinate of the endPoint.
     * @param endY   The y coordinate of the endPoint.
     */
    public void drawLine(double startX, double startY, double endX, double endY) {
        this.println(startX + " " + this.mirrorCoords(startY) + " " + endX + " " + this.mirrorCoords(endY) + " line");
    }

    /**
     * Draws a filled circle with the given center and the given radius. Note that
     * it appears in the color you set last with the setColor method.
     *
     * @param centerX The x coordinate of the center.
     * @param centerY The y coordinate of the center.
     * @param radius  The radius of the circle.
     */
    public void drawFilledCircle(double centerX, double centerY, double radius) {
        this.println(centerX + " " + this.mirrorCoords(centerY) + " " + radius + " filledCircle");
    }

    /**
     * Draws a filled rectangle with the given startpoint and the given whidth and
     * height. Note that it appears in the color you set last with the setColor
     * method.
     *
     * @param x      The x coordinate of the startpoint of the rectangle.
     * @param y      The y coordinate of the startpoint of the rectangle.
     * @param width  The width of the rectangle.
     * @param height The height of the rectangle.
     */
    public void drawFilledRectangle(double x, double y, double width, double height) {
        this.println(x + " " + this.mirrorCoords(y) + " " + (x + width) + " " + this.mirrorCoords(y) + " " + (x + width) + " "
                + this.mirrorCoords(y + height) + " " + x + " " + this.mirrorCoords(y + height) + " filled4Polygon");
    }

    /**
     * Draws a polygon with specified vertices to the eps-file. Two values from the
     * positions array are taken as coordinates for the polygon. Note that this
     * implies that the positions array has to have an even number of elements in it
     * as the coordinates are 2-valued. Generates an fatal error if the array hasn't
     * got any elements or if there is an uneven number of them.
     *
     * @param positions The array that specifies the coordinates for the polygon.
     */
    public void drawFilledPolygon(double... positions) {
        if (positions.length == 0 || positions.length % 2 != 0) {
            throw new SinalgoFatalException("You are trying to draw a polygon which hasn't got an even number of parameters.\n");
        }
        for (int i = 0; i < positions.length; i++) {
            double paramx = positions[i];
            double paramy = positions[++i];// increment
            this.print(paramx + " " + this.mirrorCoords(paramy) + " ");
        }
        this.print("newpath moveto ");
        for (int i = 1; i < positions.length / 2; i++) {
            this.print("lineto ");
        }
        this.print("closepath fill stroke\n");
    }

    /**
     * Draws the arrow head for an arrow pointing from the given startpoint to the
     * given endpoint.
     *
     * @param x1 The x coordinate of the startpoint.
     * @param y1 The y coordinate of the startpoint.
     * @param x2 The x coordinate of the endpoint.
     * @param y2 The y coordinate of the endpoint.
     */
    public void drawArrowHead(double x1, double y1, double x2, double y2) {
        this.drawArrow(x1, y1, x2, y2, false);
    }

    /**
     * Draws an arrow from the startpoint to the endpoint.
     *
     * @param x1 The x coordinate of the startpoint.
     * @param y1 The y coordinate of the startpoint.
     * @param x2 The x coordinate of the endpoint.
     * @param y2 The y coordinate of the endpoint.
     */
    public void drawArrow(double x1, double y1, double x2, double y2) {
        this.drawArrow(x1, y1, x2, y2, true);
    }

    /**
     * Draws an arrow from (x1, y1) to (x2, y2) with the current settings for the
     * arrow (arrowLength, arrowWidth). The flag drawLine is used to enable/disable
     * the line between the two endpoints. If set to false, only the arrow-head is
     * drawn.
     *
     * @param x1       The initial x coordinate
     * @param y1       The initial y coordinate
     * @param x2       The final x coordinate
     * @param y2       The final y coordinate
     * @param drawLine If true, draws a line. Else, draws only the arrow-head
     */
    private void drawArrow(double x1, double y1, double x2, double y2, boolean drawLine) {
        // only dras the arrow head when the source coords and the destination coords
        // are different
        // this can happen in 2D (when two nodes are on the same place) and in 3D (when
        // the first
        // node is behind the second one.)
        if (x1 != x2 || y1 != y2) {
            double lineLength = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
            double factor = 1.0 / lineLength;
            double aLen = this.getArrowLength();
            // shorten the arrow if the two nodes are very close
            if (2 * this.getArrowLength() >= lineLength) {
                aLen = lineLength / 3;
            }

            // unit vector in opposite direction of arrow
            double ux = (x1 - x2) * factor;
            double uy = (y1 - y2) * factor;

            // intersection point of line and arrow
            double ix = x2 + aLen * ux;
            double iy = y2 + aLen * uy;

            if (drawLine) {
                this.println(x1 + " " + this.mirrorCoords(y1) + " " + ix + " " + this.mirrorCoords(iy) + " line");
            }

            // one end-point of the triangle is (x2,y2), the second end-point (ex1, ey1) and
            // the third (ex2, ey2)
            double ex1 = ix + this.getArrowWidth() * uy;
            double ey1 = iy - this.getArrowWidth() * ux;
            double ex2 = ix - this.getArrowWidth() * uy;
            double ey2 = iy + this.getArrowWidth() * ux;

            this.println(x2 + " " + this.mirrorCoords(y2) + " " + ex1 + " " + this.mirrorCoords(ey1) + " " + ex2 + " "
                    + this.mirrorCoords(ey2) + " filledArrowHead");
        }
    }

    /**
     * Prints a given string at a given position with the fontSize and the font set
     * previously.
     *
     * @param text The text to print.
     * @param posX The x component of the position.
     * @param posY The y component of the position.
     */
    public void drawText(String text, double posX, double posY) {
        this.println("newpath " + posX + " " + this.mirrorCoords(posY) + " moveto (" + text + ") show");
    }

    /**
     * Sets the font size.
     *
     * @param size The size of the font to print successive texts with.
     */
    public void setFontSize(int size) {
        if (size != this.getFontSize()) {
            this.println(size + " scalefont setfont");
            this.fontSize = size;
        }
    }

    /**
     * Sets the font. For example setFont("Courier");
     *
     * @param font The font to print successive texts with.
     */
    public void setFont(String font) {
        if (!font.equals(this.getFont())) {
            this.println("/" + font + " findfont\n");
            this.font = font;
        }
    }

    /**
     * Adds a macro to the EPS file. The macro is only printed into the EPS file if
     * it is defined for the first time. Consecutive calls to add a macro with the
     * same call and command are ignored. Assigning a different command to a
     * previously assigned macro-name is possible.
     * <p>
     * Through this adding policy, the user may call the addMacro method arbitrarily
     * often, but the macro is only printed to the EPS file if really needed.
     *
     * @param name    The name of the macro to be used.
     * @param command The commands of the macro.
     */
    public void addMacro(String name, String command) {
        if (!this.getMacros().containsKey(name)) { // we have a new macro
            this.println("/" + name + " {" + command + "} def");
            this.getMacros().put(name, command);
        } else {
            if (!this.getMacros().get(name).equals(command)) {
                // print it as it is different to the one specified before with the same name
                this.println("/" + name + " {" + command + "} def");
                // replace the one in the map by this one.
                this.getMacros().put(name, command);
            }
        }
    }

    /**
     * Mirrors the coordinates from the framework coordinates (with the origin in
     * the top left corner) to the eps coordinates (with the origin in the bottom
     * left corner) only call this method on the y coordinate of your coordinate as
     * the x coordinate stays the same. Note that you have to set the bounding box
     * prior to using this method. The methods defined in this class all mirror the
     * coordinates themself, so i.e. the drawLine method can be called with the
     * original framework coordinates.
     *
     * @param original The original y component of the framework coordinate.
     * @return The mirrored y component of the eps coordinate.
     */
    public double mirrorCoords(double original) {
        if (this.getBoundingBoxHeight() == 0) {
            throw new SinalgoFatalException(
                    "The height of the bounding box is 0 and thus the coordinates can not be mirrored correctly. Please "
                            + "set the bounding box of the graph prior to drawing items.");
        }
        return this.getBoundingBoxHeight() - original;
    }

    /**
     * Writes the eps header to the file. Please set the bounding Box before calling
     * this method.
     */
    public void writeHeader() {
        this.println("%!PS-Adobe-3.0 EPSF-3.0");
        this.println("%%BoundingBox: " + this.getBoundingBoxX() + " " + this.getBoundingBoxY() + " " + this.getBoundingBoxWidth() + " "
                + this.getBoundingBoxHeight());
        this.println("%%Creator: Sinalgo");
        this.println("%%Pages: 1");
        this.println("%%EndComments");
        this.println("%%Page: 1 1");
        this.println();
        this.println("/" + this.getFont() + " findfont");
        this.println(this.getFontSize() + " scalefont setfont");
        this.println();
    }

    /**
     * Writes the end of the page to the eps file. Call this method to finish the
     * file.
     */
    public void writeEOF() {
        this.println();
        this.println("showpage");
        this.println("%%EOF");
    }
}
