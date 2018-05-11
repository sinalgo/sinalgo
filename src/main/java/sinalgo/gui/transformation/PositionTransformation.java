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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import sinalgo.configuration.Configuration;
import sinalgo.exception.SinalgoFatalException;
import sinalgo.gui.GraphPanel;
import sinalgo.io.eps.EPSOutputPrintStream;
import sinalgo.nodes.Position;

import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Instances of this abstract class implement how logic coordinates used in the
 * simulation are converted to 2D coordinates to be displayed on the screen.
 * <p>
 * This process includes the zooming support view a subsection of the entire
 * graph.
 */
public abstract class PositionTransformation {

    /**
     * The version number of a position transformation object is incremented every
     * time the transformation changes.
     * <p>
     * Use this number to detect whether the transformation object has changed
     * between two points A and B of execution. To do so, store the version number
     * at point A and then compare it to the new version number at point B in your
     * program. The transformation has changed iff the values has changed.
     * <p>
     * Note that this version number may overflow.
     *
     * @return The version number of this transformation object.
     */
    // every time the transformation changes, this number is incremented by 1
    // I.e. in each public method call that changes the transformation, increment
    // this member.
    @Getter
    private long versionNumber;

    /**
     * The current zoom factor used to draw the elements of the field
     * (nodes, edges).
     *
     * @return The current zoom factor.
     */
    @Getter
    @Setter(AccessLevel.PROTECTED)
    private double zoomFactor = 1;

    /**
     * Width (in pixel) of the panel on which the graph is painted
     *
     * @param width The new width.
     * @return The current width.
     */
    @Getter
    @Setter
    private int width;

    /**
     * Height(in pixel) of the panel on which the graph is painted
     *
     * @param height The new height.
     * @return The current height.
     */
    @Getter
    @Setter
    private int height;

    /**
     * Sets the zoom factor for drawing the GUI. This value indicates the factor at
     * which all GUI elements of the field (nodes, edges) are scaled.
     *
     * @param zoomFactor The factor all GUI elements of the field are scaled with.
     */
    public final void changeZoomFactor(double zoomFactor) {
        // Note that the subclasses may depend on the calling sequence!
        // While onChangeZoomFactor() is being called, the old value of zoomFactor
        // is still set.
        this.onChangeZoomFactor(zoomFactor);
        this.setZoomFactor(zoomFactor);
        this.bumpVersionNumber();
    }

    /**
     * Bumps the version number by one.
     */
    protected void bumpVersionNumber() {
        this.versionNumber++;
    }

    /**
     * @see PositionTransformation#changeZoomFactor(double)
     */
    protected abstract void onChangeZoomFactor(double zoomfactor);

    /**
     * Zoomes such that the indicated rectangle (given in GUI coordinates) becomes
     * visible on the screen. Note that the new zoom factor will be only set in the
     * transformation matrix, and needs to be manually set in the framework after
     * calling this method.
     *
     * @param rect The rectangle describing the new view.
     */
    public final void zoomToRect(Rectangle rect) {
        this.onChangeZoomToRect(rect);
    }

    /**
     * @param rect The rectangle describing the new view.
     * @see PositionTransformation#zoomToRect(Rectangle)
     */
    protected abstract void onChangeZoomToRect(Rectangle rect);

    /**
     * Adapts the transformation such that the graph nicely fits into the window,
     * whose dimensions are given.
     * <p>
     * Note that this method only changes the transforamtion, but does not adapt the
     * zoom factor elsewhere. This needs to be done manually after calling this
     * method.
     *
     * @param width  The width of the window
     * @param height The height of the window
     */
    public final void zoomToFit(int width, int height) {
        this.onChangeZoomToFit(width, height);
        this.bumpVersionNumber();
    }

    /**
     * @see PositionTransformation#zoomToFit(int, int)
     */
    protected abstract void onChangeZoomToFit(int width, int height);

    /**
     * Adapts the transformation to a default view.
     * <p>
     * Note that this method only changes the transforamtion, but does not adapt the
     * zoom factor elsewhere. This needs to be done manually after calling this
     * method.
     *
     * @param width  The width of the window
     * @param height The height of the window
     */
    public final void defaultView(int width, int height) {
        this.onChangeDefaultView(width, height);
        this.bumpVersionNumber();
    }

    /**
     * @see PositionTransformation#defaultView(int, int)
     */
    protected abstract void onChangeDefaultView(int width, int height);

    /**
     * Translates the view
     *
     * @param x Number of pixels to move in x-direction
     * @param y Number of pixels to move in y-direction
     */
    public final void moveView(int x, int y) {
        this.onChangeMoveView(x, y);
        this.bumpVersionNumber();
    }

    /**
     * @see PositionTransformation#moveView(int, int)
     */
    protected abstract void onChangeMoveView(int x, int y);

    /**
     * Draw the background for the graph. I.e. the white background and the rulers,
     * if necessary.
     * <p>
     * The implementation of this method may not change the transformation matrix!
     *
     * @param g The graph to paint to
     */
    public abstract void drawBackground(Graphics g);

    /**
     * Draw the background for the graph in PS.
     *
     * @param ps The stream where to write the PS commands.
     * @see PositionTransformation#drawBackground(Graphics)
     */
    public abstract void drawBackgroundToPostScript(EPSOutputPrintStream ps);

    /**
     * The x-offset of the GUI coordinate determined in the last call to
     * translateToGUIPosition
     */
    @Getter
    @Setter
    private int guiX;

    /**
     * The x-offset of the GUI coordinate determined in the last call to
     * translateToGUIPosition
     */
    @Getter
    @Setter
    private int guiY;

    /**
     * The exact x-offset of the GUI coordinate determined in the last call to
     * translateToGUIPosition
     */
    @Getter
    @Setter
    private double guiXDouble;

    /**
     * The exact y-offset of the GUI coordinate determined in the last call to
     * translateToGUIPosition
     */
    @Getter
    @Setter
    private double guiYDouble;

    /**
     * The x-offset of the GUI coordinate determined in the last call to
     * translateToLogicPosition.
     */
    @Getter
    @Setter
    private double logicX;

    /**
     * The y-offset of the GUI coordinate determined in the last call to
     * translateToLogicPosition.
     */
    @Getter
    @Setter
    private double logicY;

    /**
     * The z-offset of the GUI coordinate determined in the last call to
     * translateToLogicPosition.
     */
    @Getter
    @Setter
    private double logicZ;

    /**
     * Translates a position from the simulation intern view (logic view) to GUI
     * coordinates (GUI view).
     * <p>
     * The 2D coordinate is not returned directly, but must be retrived through the
     * two member variables guiX and guiY, which are set by this method. This
     * approach avoids allocation of a 2D point object to be returned.
     *
     * @param x X-coordinate of the logic position to translate
     * @param y Y-coordinate of the logic position to translate
     * @param z Z-coordinate of the logic position to translate
     */
    public abstract void translateToGUIPosition(double x, double y, double z);

    /**
     * Translates a position from the simulation intern view (logic view) to GUI
     * coordinates (GUI view).
     * <p>
     * The 2D coordinate is not returned directly, but must be retrived through the
     * two member variables guiX and guiY, which are set by this method. This
     * approach avoids allocation of a 2D point object to be returned.
     *
     * @param pos The logic position to translate.
     */
    public abstract void translateToGUIPosition(Position pos);

    /**
     * @return The dimensionality of this transformation object. E.g. 2 for 2D and 3
     * for 3D
     */
    public abstract int getNumberOfDimensions();

    /**
     * Indicates whether this implementation supports reverse translation, from GUI
     * coordinates to logic coordinates.
     *
     * @return True if this transformation implementation can convert from GUI
     * coordinates to logic coordinates, otherwise false.
     */
    public abstract boolean supportReverseTranslation();

    /**
     * Transforms a GUI coordinate to a logic coordinate, used by the simulation. If
     * this method is called, but the transformation implementation does not support
     * reverse translation, this method terminates the execution with a fatal error.
     *
     * @param x The x-axis coordinate to translate to.
     * @param y The y-axis coordinate to translate to.
     */
    public abstract void translateToLogicPosition(int x, int y);

    /**
     * Draws zoom panel in the control panel. This image should show the entire
     * simulation area fitted into the square of the given side length and indicate
     * with a red rectangle the area that is currently visible on the screen.
     * <p>
     * The implementation of this method may not change the transformation matrix!
     *
     * @param g          The graphics object to draw to
     * @param sideLength the side-length of the square into which the method should draw
     * @param offsetX    X-coordinate of point on the graphics which is the top-left corner
     *                   of the square to draw into
     * @param offsetY    Y-coordinate of point on the graphics which is the top-left corner
     *                   of the square to draw into
     * @param bgwidth    The width of the entire graphics area
     * @param bgheight   The height of the entire graphics area
     */
    public abstract void drawZoomPanel(Graphics g, int sideLength, int offsetX, int offsetY, int bgwidth, int bgheight);

    /**
     * Determines the zoom factor used to draw the zoomPanel.
     *
     * @return The zoom factor used by the zoomPanel.
     */
    public abstract double getZoomPanelZoomFactor();

    /**
     * @return A string representing the position determined in the last call to
     * translateToLogicPosition
     */
    public abstract String getLogicPositionString();

    /**
     * @return A string representing the position determined in the last call to
     * translateToGUIPosition
     */
    public abstract String getGUIPositionString();

    /**
     * Draws a line between two points
     *
     * @param g    The graphics object to draw to
     * @param from Start node of the line
     * @param to   End node of the line
     */
    public void drawLine(Graphics g, Position from, Position to) {
        this.translateToGUIPosition(from);
        int fromX = this.getGuiX();
        int fromY = this.getGuiY();
        this.translateToGUIPosition(to);
        g.drawLine(fromX, fromY, this.getGuiX(), this.getGuiY());
    }

    /**
     * Draws a bold line between two points
     *
     * @param g           The graphics object to draw to
     * @param from        Start node of the line
     * @param to          End node of the line
     * @param strokeWidth The width of the line to draw
     */
    public void drawBoldLine(Graphics g, Position from, Position to, int strokeWidth) {
        this.translateToGUIPosition(from);
        int fromX = this.getGuiX();
        int fromY = this.getGuiY();
        this.translateToGUIPosition(to);
        GraphPanel.drawBoldLine(g, fromX, fromY, this.getGuiX(), this.getGuiY(), strokeWidth);
    }

    /**
     * Draws a dotted line between two points
     *
     * @param g    The graphics object to draw to
     * @param from Start node of the line
     * @param to   End node of the line
     */
    public void drawDottedLine(Graphics g, Position from, Position to) {
        this.translateToGUIPosition(from);
        int fromX = this.getGuiX();
        int fromY = this.getGuiY();
        this.translateToGUIPosition(to);
        GraphPanel.drawDottedLine(g, fromX, fromY, this.getGuiX(), this.getGuiY());
    }

    /**
     * Draws a circle.
     *
     * @param g      The graphics object to draw to
     * @param center The center around which the circle should be drawn
     * @param radius The radius of the circle
     */
    public void drawCircle(Graphics g, Position center, double radius) {
        this.translateToGUIPosition(center);
        int r = (int) (this.getZoomFactor() * radius);
        g.drawOval(this.getGuiX() - r, this.getGuiY() - r, 2 * r, 2 * r);
    }

    /**
     * Creates a new instance of the field transformation specified in the
     * configuration file.
     *
     * @return The new field transformation object.
     */
    public static PositionTransformation loadFieldTransformator() {
        PositionTransformation result;
        String name;
        if (Configuration.getDimensions() == 2) {
            name = Configuration.getGuiPositionTransformation2D();
        } else if (Configuration.getDimensions() == 3) {
            name = Configuration.getGuiPositionTransformation3D();
        } else {
            throw new SinalgoFatalException(
                    "The 'dimensions' field in the configuration file is invalid. Valid values are either '2' for 2D or '3' for 3D. (Cannot create corresponding position transformation instance.)");
        }
        try {
            Class<?> c = Thread.currentThread().getContextClassLoader().loadClass(name);
            Constructor<?> cons = c.getConstructor();
            result = (PositionTransformation) cons.newInstance();
        } catch (ClassNotFoundException e) {
            throw new SinalgoFatalException("Cannot find the class " + name
                    + " which contains the implementation for the field transformation. Please check the guiPositionTransformation field in the config file.");
        } catch (SecurityException e) {
            throw new SinalgoFatalException("Cannot generate the field transformation object due to a security exception:\n\n"
                    + e.getMessage());
        } catch (NoSuchMethodException | IllegalArgumentException e) {
            throw new SinalgoFatalException("The field transformation " + name + " must provide a constructor taking no arguments.\n\n"
                    + e.getMessage());
        } catch (InstantiationException e) {
            throw new SinalgoFatalException(
                    "Classes usable as field transformators must be instantiable classes, i.e. no interfaces and not abstract.\n\n"
                            + e.getMessage());
        } catch (IllegalAccessException e) {
            throw new SinalgoFatalException(
                    "Cannot generate the field transformator object due to illegal access:\n\n" + e.getMessage());
        } catch (InvocationTargetException e) {
            throw new SinalgoFatalException("Exception while instanciating " + name + ":\n\n" + e.getCause().getMessage());
        }
        return result;
    }
}
