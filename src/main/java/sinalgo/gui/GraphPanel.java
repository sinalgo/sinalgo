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
package sinalgo.gui;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import sinalgo.configuration.Configuration;
import sinalgo.exception.SinalgoFatalException;
import sinalgo.exception.WrongConfigurationException;
import sinalgo.gui.helper.Animations;
import sinalgo.gui.helper.Arrow;
import sinalgo.gui.helper.NodeSelectionHandler;
import sinalgo.gui.multiLineTooltip.MultiLineToolTip;
import sinalgo.gui.popups.EdgePopupMenu;
import sinalgo.gui.popups.NodePopupMenu;
import sinalgo.gui.popups.SpacePopupMenu;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.gui.transformation.Transformation3D;
import sinalgo.nodes.Node;
import sinalgo.nodes.Position;
import sinalgo.nodes.edges.Edge;
import sinalgo.runtime.Global;
import sinalgo.runtime.Main;
import sinalgo.runtime.SinalgoRuntime;
import sinalgo.tools.Tuple;
import sinalgo.tools.logging.LogL;
import sinalgo.tools.logging.Logging;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.Stack;
import java.util.Vector;

/**
 * A panel where the Graph is painted into.
 */
@Getter(AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
public class GraphPanel extends JPanel {

    private static final long serialVersionUID = -7446360484673626267L;

    private Image offscreen;
    // needs to be set to true whenever offscreen has been assigned a new object

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    private boolean newOffscreen = true;

    private boolean forcedDraw;

    private NodePopupMenu nodePopupMenu;
    private EdgePopupMenu edgePopupMenu;
    private SpacePopupMenu spacePopupMenu;

    private final GUI parentGUI;

    private Node nodeToDrag;
    private Position nodeToDragInitialPosition = new Position(); // initial position of the node that is being dragged,
    // only set if nodeToDrag is set
    private Node nodeToAddEdge;
    private Node targetNodeToAddEdge;
    private Point shiftStartPoint;
    private Point rotateStartPoint;
    private Vector<Node> nodesToHighlight = new Vector<>(10);
    private Vector<Node> nodesToDrawCoordCube = new Vector<>(10);
    private Node toolTipDrawCoordCube;
    private Node nodeToDragDrawCoordCube;
    private int minMouseMovementUntilNodeMovement = 10; // threshold (in mouse-pixels) for 3D before moving a node

    // this scales the underlying image
    private int imageSizeX;
    private int imageSizeY;

    private Point currentCursorPosition = new Point(0, 0); // the position of the cursor
    private Rectangle zoomRect;
    private int zoomRectMinSize = 5;

    private Logging log = Logging.getLogger(); // system wide logger

    // The first time we draw the graph, apply the default view
    private boolean defaultViewOnNextDraw = true;

    /**
     * A boolean indicating whether the graph was already painted once or not.
     */
    @Getter
    @Setter
    private static boolean firstTimePainted;

    private final PositionTransformation pt;
    private long myLastPtVersionNumber = -1;

    // Support to let the user select a node
    private int cancelAreaWidth;
    private int cancelAreaHeight;
    private int cancelAreaOffsetX; // dimension of the cancel area printed directly
    // onto the graphics
    private Node userSelectsNodeCurrentFocus; // the node over which the mouse currently hovers, if the user is
    // to select a node
    // Set to true while the user is asked to select a node
    private boolean userSelectsNodeMode;
    private Stack<Tuple<NodeSelectionHandler, String>> userSelectsNodeHandler = new Stack<>(); // stack
    // of
    // handlers,
    // pick
    // topmost
    // one

    /**
     * Constructor for the GraphPanel class.
     *
     * @param p The parentGUI Frame (GUI) where the Graph Panel is added.
     */
    public GraphPanel(GUI p) {
        this.parentGUI = p;
        this.pt = this.parentGUI.getTransformator();

        MyMouseListener ml = new MyMouseListener();
        this.addMouseListener(ml);
        this.addMouseMotionListener(ml);
        this.addMouseWheelListener(ml);
        this.addKeyListener(new MyKeyListener());
        this.setFocusable(true);

        this.nodePopupMenu = new NodePopupMenu(this.parentGUI);
        this.add(this.nodePopupMenu);

        this.edgePopupMenu = new EdgePopupMenu(this.parentGUI);
        this.add(this.edgePopupMenu);

        this.spacePopupMenu = new SpacePopupMenu(this.parentGUI);
        this.add(this.spacePopupMenu);

        this.imageSizeX = this.getWidth();
        this.imageSizeY = this.getHeight();

        // update the transformation object
        this.pt.setWidth(this.imageSizeX);
        this.pt.setHeight(this.imageSizeY);

        this.addComponentListener(new MyComponentListener());
    }

    /**
     * Called when the user removes all nodes
     */
    public void allNodesAreRemoved() {
        this.nodesToDrawCoordCube.clear();
        this.nodesToHighlight.clear();
    }

    /**
     * Triggers the default view the next time this graph panel is redrawn.
     */
    public void requestDefaultViewOnNextDraw() {
        this.defaultViewOnNextDraw = true;
    }

    /**
     * Requires that the graph panel is fully redrawn during the next call to
     * paint().
     */
    public void requireFullDrawOnNextPaint() {
        this.log.logln(LogL.GUI_SEQ, "GraphPanel.requireFullDrawOnNextPaint()s");
        this.setForcedDraw(true);
    }

    /**
     * This method zooms its ZoomableGraphics so that field fits best in the
     * scrollable pane and redraws the graph.
     */
    public void defaultView() {
        this.pt.defaultView(this.imageSizeX, this.imageSizeY);
        this.parentGUI.setZoomFactor(this.pt.getZoomFactor()); // initiates redrawing the graph
    }

    /**
     * @see GraphPanel#defaultView()
     */
    private void defaultViewWithoutRedraw() {
        this.pt.defaultView(this.imageSizeX, this.imageSizeY);
        this.parentGUI.setZoomFactorNoRepaint(this.pt.getZoomFactor());
    }

    /**
     * Creates a new _offscreen image object according to the current dimensions of
     * this panel.
     */
    private void createNewOffscreen() {
        this.getLog().logln(LogL.GUI_SEQ, "GraphPanel.createNewOffscreen: Allocating a new offscreen image.");
        this.setImageSizeX(this.getWidth());
        this.setImageSizeY(this.getHeight());
        this.setOffscreen(null);
        if (this.getImageSizeX() > 0 && this.getImageSizeY() > 0) {
            // update the transformation object
            this.getPt().setWidth(this.getImageSizeX());
            this.getPt().setHeight(this.getImageSizeY());
            this.setOffscreen(this.createImage(this.getImageSizeX(), this.getImageSizeY()));
            this.setNewOffscreen(true);
        }
    }

    @Override
    public void paint(Graphics g) {
        if (Global.isRunning()) {
            // if possible, draw the previous image, but without updating it!
            if (this.getOffscreen() != null) {
                g.drawImage(this.getOffscreen(), 0, 0, this);
                // drawOnTop(this.getGraphics());
                this.getLog().logln(LogL.GUI_SEQ, "GraphPanel.paint(): Simulation is running -> draw offscreen.");
            }
            return;
        }

        if (this.getImageSizeX() != this.getWidth() || this.getImageSizeY() != this.getHeight()) {
            this.getLog().logln(LogL.GUI_SEQ, "GraphPanel.paint(): We missed a resize event.");
            this.createNewOffscreen();
        }
        GraphPanel.setFirstTimePainted(true);
        if (this.getOffscreen() == null) {
            this.createNewOffscreen();
        }
        if (this.getOffscreen() != null) {
            // we may not need to redraw the graph, but can reuse the old offscreen image
            if (this.getMyLastPtVersionNumber() != this.getPt().getVersionNumber()
                    || this.isNewOffscreen() || this.isForcedDraw()) {
                this.getLog().logln(LogL.GUI_SEQ,
                        "GraphPanel.paint(): drawing graph to offscreen"
                                + (this.getMyLastPtVersionNumber() != this.getPt().getNumberOfDimensions() ?
                                " ptVersionNumber changed" : " new Offscreen"));
                this.draw(this.getOffscreen().getGraphics());
                this.setMyLastPtVersionNumber(this.getPt().getVersionNumber());
                this.setForcedDraw(false);
                this.setNewOffscreen(false);
            } else {
                this.log.logln(LogL.GUI_SEQ, "GraphPanel.paint(): no changes -> draw old offscreen");
            }
            g.drawImage(this.getOffscreen(), 0, 0, this);
            this.drawOnTop(g);
        } else {
            // the offscreen object is not available - draw on the provided graphics
            // directly (note: this will draw the background white)
            this.draw(g);
            this.drawOnTop(g);
        }
    }

    /**
     * Immediately repaints the graph. You should call GUI#redrawGraph() to redraw
     * the graph, this method is used internaly.
     */
    public void paintNow() {
        this.getLog().log(LogL.GUI_SEQ, "GraphPanel.paintNow()");
        if (this.getOffscreen() != null) {
            this.draw(this.getOffscreen().getGraphics());
            this.setMyLastPtVersionNumber(this.getPt().getVersionNumber());
            this.setNewOffscreen(false);
            this.getGraphics().drawImage(this.getOffscreen(), 0, 0, this);
            this.drawOnTop(this.getGraphics());
        } else {
            this.repaint(); // defer paint to default call
        }
    }

    /**
     * Draws the graph to a given graphics object.
     *
     * @param g The graphics to paint to
     */
    private void draw(Graphics g) {
        synchronized (this.getPt()) {
            this.getLog().logln(LogL.GUI_SEQ, "GraphPanel.draw(): draw imgSize=(" + this.getImageSizeX() + "," + this.getImageSizeY() + ")");
            if (this.isDefaultViewOnNextDraw()) {
                this.defaultViewWithoutRedraw();
                this.setDefaultViewOnNextDraw(false);
            }

            g.clearRect(0, 0, this.getImageSizeX(), this.getImageSizeY());
            this.pt.drawBackground(g);

            if (Configuration.isUseMap()) {
                SinalgoRuntime.getMap().paintMap(g, this.getPt());
            }

            g.setColor(Color.BLACK);

            // Draw the graph
            try {
                // First draw all edges, only then the nodes
                Enumeration<Node> nodeEnumer;
                if (Configuration.isDrawEdges()) {
                    nodeEnumer = SinalgoRuntime.getNodes().getSortedNodeEnumeration(true);
                    while (nodeEnumer.hasMoreElements()) {
                        Node node = nodeEnumer.nextElement();
                        // first draw all outgoing edges of this node
                        for (Edge e : node.getOutgoingConnections()) {
                            e.draw(g, this.getPt());
                        }
                    }
                }
                // Draw the nodes in a separate loop
                if (Configuration.isDrawNodes()) {
                    // Draw the nodes in a separate loop
                    nodeEnumer = SinalgoRuntime.getNodes().getSortedNodeEnumeration(true);
                    while (nodeEnumer.hasMoreElements()) {
                        Node node = nodeEnumer.nextElement();
                        node.draw(g, this.getPt(), false);
                    }
                }
            } catch (ConcurrentModificationException eME) {
                // catch the concurrent modification exception from the iterators of the
                // drawing. Do not do anything when it is thrown.
                // We don't care, when this exception is thrown because this only happens when
                // someone is zooming in the middle of a
                // round. It only happens if an edge is added or removed from a node during
                // redrawing. Catching this exception and not
                // doing anything results in a wrong picture during zooming (when there was a
                // concurrentModificationException) and we
                // decided that it is not worth the slowdown of the simulation just to always
                // get a correct picture.
            }

            if (Configuration.isShowMessageAnimations()) {
                Animations.drawEnvelopes(g, this.getPt());
            }

            // perform the custom drawing. Note that the custom paint is only called when
            // also the
            // entire graph was painted. This ensures that there should be no conflict due
            // to any
            // concurrent data accesses
            Global.getCustomGlobal().customPaint(g, this.getPt());
        }
    }

    /**
     * Draws all the additional stuff over the given graphics. Additional stuff
     * means the temporary stuff from the mouse input like the red edge during
     * adding an edge or the red square that is drawn to zoom to a special position
     * or a node to highlight.
     *
     * @param g The graphics object to draw the additional stuff onto.
     */
    public void drawOnTop(Graphics g) {
        // draw the line to add a new edge
        if (this.getNodeToAddEdge() != null) {
            this.getPt().translateToGUIPosition(this.getNodeToAddEdge().getPosition());
            if (this.getPt().getGuiX() != this.getCurrentCursorPosition().x || this.getPt().getGuiY() != this.getCurrentCursorPosition().y) {
                Arrow.drawArrow(this.getPt().getGuiX(), this.getPt().getGuiY(),
                        this.getCurrentCursorPosition().x, this.getCurrentCursorPosition().y, g, this.getPt(), Color.RED);
            }
        }

        // draw the rectangle for zooming
        if (this.zoomRect != null) {
            if ((Math.abs(this.getZoomRect().height) > this.getZoomRectMinSize()) && (Math.abs(this.getZoomRect().width) > this.getZoomRectMinSize())) {
                Color temp = g.getColor();
                g.setColor(Color.RED);
                int topx = this.getZoomRect().x;
                int topy = this.getZoomRect().y;
                if (this.getZoomRect().width < 0) {
                    topx += this.getZoomRect().width;
                }
                if (this.getZoomRect().height < 0) {
                    topy += this.getZoomRect().height;
                }
                g.drawRect(topx, topy, Math.abs(this.getZoomRect().width), Math.abs(this.getZoomRect().height));
                // VERY strange: If we also draw some non-vertical/non-horizontal lines when
                // drawing
                // the rectangle (which consists of only vertical and horizontal lines), the
                // drawing method becomes SOMEHOW MUCH faster.
                // (It seems not to matter whether we draw the lines before or after, drawing
                // the
                // line outside the clipping area seems not to help.)
                // OK - seems not to be a problem on all PCs...
                g.drawLine(this.getZoomRect().x, this.getZoomRect().y, this.getZoomRect().x + 1, this.getZoomRect().y + 1);
                g.setColor(temp);
            }
        }

        // Draw the highlighted node
        for (Node highLighted : this.getNodesToHighlight()) {
            highLighted.draw(g, this.getPt(), true);
        }
        if (this.getToolTipDrawCoordCube() != null) {
            this.drawNodeCubeCoords(g, this.getToolTipDrawCoordCube());
        }
        if (this.getNodeToDragDrawCoordCube() != null) {
            this.drawNodeCubeCoords(g, this.getNodeToDragDrawCoordCube());
        }
        for (Node cubeNode : this.getNodesToDrawCoordCube()) {
            this.drawNodeCubeCoords(g, cubeNode);
        }
        if (this.getNodeToAddEdge() != null) {
            this.getNodeToAddEdge().draw(g, this.getPt(), true);
        }
        if (this.getTargetNodeToAddEdge() != null) {
            this.getTargetNodeToAddEdge().draw(g, this.getPt(), true);
        }
        if (this.getNodeToDrag() != null) {
            this.getNodeToDrag().draw(g, this.getPt(), true);
        }

        if (this.isUserSelectsNodeMode()) {
            if (this.getUserSelectsNodeCurrentFocus() != null) {
                this.getUserSelectsNodeCurrentFocus().draw(g, this.getPt(), true);
            }
            if (!this.getUserSelectsNodeHandler().isEmpty()) {
                Tuple<NodeSelectionHandler, String> h = this.getUserSelectsNodeHandler().peek();
                String text = h.getSecond();
                String textCancel = "Cancel";
                Font font = new Font(null, Font.PLAIN, 12);
                g.setFont(font);
                int len1 = (int) g.getFontMetrics().getStringBounds(text, g).getWidth();
                int len2 = (int) g.getFontMetrics().getStringBounds(textCancel, g).getWidth();
                int height = g.getFontMetrics().getHeight();
                g.setColor(Color.LIGHT_GRAY);
                // draw the boxes for the text and the button
                g.fill3DRect(0, 0, len1 + len2 + 38, height + 10, true);
                g.fill3DRect(len1 + 15, 3, len2 + 20, height + 5, true);
                // Write the text
                g.setColor(Color.RED);
                g.drawString(text, 10, height + 2);
                g.setColor(Color.BLACK);
                g.drawString(textCancel, len1 + 25, height + 2);
                // set the
                this.setCancelAreaWidth(len2 + 25);
                this.setCancelAreaHeight(height + 9);
                this.setCancelAreaOffsetX(len1 + 10);
            }
        }
    }

    private void drawNodeCubeCoords(Graphics g, Node n) {
        Position p = n.getPosition();
        this.drawCubeCoordLine(g, p.getXCoord(), p.getYCoord(), p.getZCoord(), 0, p.getYCoord(), p.getZCoord());
        this.drawCubeCoordLine(g, p.getXCoord(), p.getYCoord(), p.getZCoord(), p.getXCoord(), 0, p.getZCoord());
        this.drawCubeCoordLine(g, p.getXCoord(), p.getYCoord(), p.getZCoord(), p.getXCoord(), p.getYCoord(), 0);
        this.drawCubeCoordLine(g, p.getXCoord(), 0, 0, p.getXCoord(), p.getYCoord(), 0);
        this.drawCubeCoordLine(g, 0, p.getYCoord(), 0, p.getXCoord(), p.getYCoord(), 0);
        this.drawCubeCoordLine(g, p.getXCoord(), 0, p.getZCoord(), 0, 0, p.getZCoord());
        this.drawCubeCoordLine(g, p.getXCoord(), 0, p.getZCoord(), p.getXCoord(), 0, 0);
        this.drawCubeCoordLine(g, 0, p.getYCoord(), p.getZCoord(), 0, 0, p.getZCoord());
        this.drawCubeCoordLine(g, 0, p.getYCoord(), p.getZCoord(), 0, p.getYCoord(), 0);
    }

    /**
     * Forces the Graph panel that it draws itself in the next paint call. Note that
     * this is not the same as RepaintNow as this only forces the next paint call to
     * recalculate the background image and does not paint it. RepaintNow really
     * paints it now. Note that this method is very eccifient and useful if you know
     * that the graphpanel is redrawn anyway after that call. Like when an popup
     * method is closed.
     */
    public void forceDrawInNextPaint() {
        this.setForcedDraw(true);
    }

    /**
     * This method overrides the createToolTip method of the JComponent. It returns
     * a new instance of MyToolTip to display informations about the place where the
     * cursor is stoped for a second.
     *
     * @see javax.swing.JComponent#createToolTip()
     */
    @Override
    public JToolTip createToolTip() {
        return new MultiLineToolTip();
    }

    /**
     * This method returns the String to display in the ToolTip for this Component.
     * It checks, if there is a Node or an Edge under the current cursor position
     * and returns an info string about it.
     *
     * @return The String to display in the ToolTip for this Component.
     * @see javax.swing.JComponent#getToolTipText(java.awt.event.MouseEvent)
     */
    @Override
    public String getToolTipText(MouseEvent event) {

        // block the appearence of the ToolTip when the simulation is runing. This
        // prevents the
        // simulation from getting inconsistencies from multiple threads drawing into
        // the same
        // buffer
        if (Global.isRunning()) {
            return null;
        }

        Edge edgeUnderPos = null;

        Enumeration<Node> nodeEnumer = SinalgoRuntime.getNodes().getSortedNodeEnumeration(false);
        while (nodeEnumer.hasMoreElements()) {
            Node node = nodeEnumer.nextElement();
            if (node.isInside(event.getX(), event.getY(), this.getPt())) {
                if (Configuration.getDimensions() == 3) {
                    this.setToolTipDrawCoordCube(node);
                    this.repaint();
                }
                return "Node " + node.getID() + ":\n" + node.toString();
            }
            // give precendence to the nodes - only if there is no node at the cursor,
            // select the edge
            if (edgeUnderPos == null) {
                edgeUnderPos = this.getFirstEdgeAtPosition(event.getX(), event.getY(), node);
            }
        }
        if (edgeUnderPos != null) {
            return "Edge from " + edgeUnderPos.getStartNode().getID()
                    + " to " + edgeUnderPos.getEndNode().getID() + ":\n"
                    + edgeUnderPos.toString();
        }
        return null;
    }

    /**
     * Asks the user to select a node from the GUI and blocks until this is done.
     * <p>
     * If several calls interleave, the calls are served LIFO.
     * <p>
     * This method may only be called in GUI mode!
     *
     * @param handler The handler to invoke when a node is selected
     * @param text    Text to display to the user
     */
    public void getNodeSelectedByUser(NodeSelectionHandler handler, String text) {
        if (!Global.isGuiMode()) {
            throw new SinalgoFatalException(
                    "Invalid call to 'GUI.getNodeSelectedByUser()'. This method is not supported in batch mode.");
        }
        this.getUserSelectsNodeHandler().push(new Tuple<>(handler, text));
        this.setUserSelectsNodeMode(true);
        this.setDefaultCursor();
        this.repaint(); // async call that does not repaint the network graph, but only the stuff on top
        // of the graph
    }

    /**
     * Set the default mouse cursor, depending on the current state of the GUI.
     */
    private void setDefaultCursor() {
        if (this.isUserSelectsNodeMode()) {
            this.getParentGUI().getComponent(0).setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            this.getParentGUI().getComponent(0).setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Add a node for which the coordinate cube should be drawn. A node may be added
     * several times!
     *
     * @param n The node to add
     */
    public void setNodeToDrawCoordinateCube(Node n) {
        this.getNodesToDrawCoordCube().add(n);
    }

    /**
     * Tests whether for a given node, the coordinate cube is drawn
     *
     * @param n The node to test
     * @return True if the coordinate cube is drawn for the given node
     */
    public boolean containsNodeToDrawCoordinateCube(Node n) {
        return this.getNodesToDrawCoordCube().contains(n);
    }

    /**
     * Removes a node from the list of the nodes to draw the coordinate cube.
     *
     * @param n The node to remove.
     */
    public void removeNodeToDrawCoordinateCube(Node n) {
        this.getNodesToDrawCoordCube().remove(n);
    }

    /**
     * Sets the given node on highlighted if the highlighted-flag is set true. If
     * the flag is false it checks, whethter the given node is highlighted and
     * dis-highlights it.
     *
     * @param n           The node to highlight.
     * @param highlighted Indicates whether to highlight the node or to dis-highlight it.
     */
    public void setNodeHighlighted(Node n, boolean highlighted) {
        if (highlighted) {
            this.getNodesToHighlight().add(n);
        } else // if this is highlighted node, dis-highlight it.
            this.getNodesToHighlight().remove(n);
    }

    /**
     * Loops over all nodes and returns the first one that covers the given position
     * on the GUI. If no node covers this postion, this method returns null.
     *
     * @param x the x offset of the position to test
     * @param y the y offset of the position to test
     * @return An arbitrary node that covers the position (x,y) on the gui, null if
     * no node covers this position.
     */
    public Node getFirstNodeAtPosition(int x, int y) {
        Enumeration<Node> nodeEnumer = SinalgoRuntime.getNodes().getSortedNodeEnumeration(false);
        while (nodeEnumer.hasMoreElements()) {
            Node node = nodeEnumer.nextElement();
            if (node.isInside(x, y, this.getPt())) {
                return node;
            }
        }
        return null;
    }

    /**
     * Loops over all edges of a node and returns the first one that touches a given
     * position (x,y). If none of the edges touches (x,y), this method returns null.
     * <p>
     * If the found edge is bidirectional, the method returns the edge to whose
     * end-point the mouse-click was closer.
     *
     * @param x the x offset of the position to test.
     * @param y the y offset of the position to test.
     * @param n The node for which the edges should be checked
     * @return An arbitrary edge incident to node n that touches position (x,y) when
     * drawn on the gui, null if no edge satisfies this criteria.
     */
    public Edge getFirstEdgeAtPosition(int x, int y, Node n) {
        for (Edge e : n.getOutgoingConnections()) {
            if (e.isInside(x, y, this.getPt())) {
                Edge opposEdge = e.getOppositeEdge();
                if (opposEdge != null) {
                    // find out which one we want to delete.
                    this.pt.translateToGUIPosition(e.getEndNode().getPosition());
                    Position oneEnd = new Position(this.getPt().getGuiXDouble(), this.getPt().getGuiYDouble(), 0.0);
                    this.pt.translateToGUIPosition(opposEdge.getEndNode().getPosition());
                    Position otherEnd = new Position(this.getPt().getGuiXDouble(), this.getPt().getGuiYDouble(), 0.0);
                    Position eventPos = new Position(x, y, 0.0);

                    if (eventPos.distanceTo(oneEnd) > eventPos.distanceTo(otherEnd)) {
                        return opposEdge;
                    } else {
                        return e;
                    }
                } else { // there is no opposite edge, return e
                    return e;
                }
            }
        }
        return null;
    }

    /**
     * Draws a dotted line given the logic coordinates
     *
     * @param g     The graphics to paint on
     * @param fromX The initial x coordinate
     * @param fromY The initial y coordinate
     * @param fromZ The initial z coordinate
     * @param toX   The final x coordinate
     * @param toY   The final y coordinate
     * @param toZ   The final z coordinate
     */
    public void drawCubeCoordLine(Graphics g, double fromX, double fromY, double fromZ, double toX, double toY,
                                  double toZ) {
        this.getPt().translateToGUIPosition(fromX, fromY, fromZ);
        int guiX = this.getPt().getGuiX();
        int guiY = this.getPt().getGuiY();
        this.getPt().translateToGUIPosition(toX, toY, toZ);
        g.setColor(Color.LIGHT_GRAY);
        g.drawLine(guiX, guiY, this.getPt().getGuiX(), this.getPt().getGuiY());
    }

    /**
     * Draws a dotted line on the graphics
     *
     * @param g     The graphics to paint on
     * @param fromX The initial x coordinate
     * @param fromY The initial y coordinate
     * @param toX   The final x coordinate
     * @param toY   The final y coordinate
     */
    public static void drawDottedLine(Graphics g, int fromX, int fromY, int toX, int toY) {
        int dx = toX - fromX;
        int dy = toY - fromY;
        if (dx == 0 && dy == 0) {
            return;
        }
        boolean swapped = false;
        if (Math.abs(dx) < Math.abs(dy)) {
            int temp = fromX;
            fromX = fromY;
            fromY = temp;
            temp = toX;
            toX = toY;
            toY = temp;
            temp = dy;
            dy = dx;
            dx = temp;
            swapped = true;
        }
        if (dx < 0) { // swap 'from' and 'to'
            int temp = fromX;
            fromX = toX;
            toX = temp;
            temp = fromY;
            fromY = toY;
            toY = temp;
            dx = -dx;
            dy = -dy;
        }
        double delta = ((double) dy) / dx;
        boolean paint = true;
        for (int i = 0; i <= dx; i++) {
            int y = fromY + (int) (i * delta);
            if (paint) {
                if (swapped) {
                    g.fillRect(y, i + fromX, 1, 1); // only a single dot
                } else {
                    g.fillRect(i + fromX, y, 1, 1); // only a single dot
                }
            }
            paint = !paint;
        }
    }

    /**
     * Draws a bold line between two points. This method produces an approximation
     * by drawing several lines. It is possible, that the line is not tightly
     * filled.
     *
     * @param g           The graphics to paint the line on
     * @param fromX       The initial x coordinate
     * @param fromY       The initial y coordinate
     * @param toX         The final x coordinate
     * @param toY         The final y coordinate
     * @param strokeWidth The width (in pixels) to draw the line
     */
    public static void drawBoldLine(Graphics g, int fromX, int fromY, int toX, int toY, int strokeWidth) {
        for (int i = 1; i < strokeWidth; i++) {
            g.drawLine(fromX + i, fromY, toX + i, toY);
            g.drawLine(fromX - i, fromY, toX - i, toY);
            g.drawLine(fromX, fromY + i, toX, toY + i);
            g.drawLine(fromX, fromY - i, toX, toY - i);
        }
    }

    class MyMouseListener implements MouseInputListener, MouseWheelListener {

        @Override
        public void mouseClicked(MouseEvent event) {
            // block this during a runing simulation.
            if (Global.isRunning()) {
                return;
            }

            Global.getLog().logln(LogL.GUI_DETAIL, "Mouse Clicked");

            if (GraphPanel.this.isUserSelectsNodeMode() && event.getClickCount() == 1
                    && event.getButton() == MouseEvent.BUTTON1) {
                if (event.getX() >= GraphPanel.this.getCancelAreaOffsetX()
                        && event.getX() <= GraphPanel.this.getCancelAreaOffsetX() + GraphPanel.this.getCancelAreaWidth()
                        && event.getY() <= GraphPanel.this.getCancelAreaHeight()) {
                    if (!GraphPanel.this.getUserSelectsNodeHandler().isEmpty()) {
                        Tuple<NodeSelectionHandler, String> h = GraphPanel.this.getUserSelectsNodeHandler().pop();
                        GraphPanel.this.setUserSelectsNodeMode(!GraphPanel.this.getUserSelectsNodeHandler().isEmpty());
                        GraphPanel.this.repaint(); // async call that does not repaint the network graph, but only the stuff on top
                        // of the graph
                        h.getFirst().handleNodeSelectedEvent(null); // abort
                    } else {
                        GraphPanel.this.setUserSelectsNodeMode(false);
                        GraphPanel.this.repaint(); // async call that does not repaint the network graph, but only the stuff on top
                        // of the graph
                    }
                    GraphPanel.this.setDefaultCursor();
                }
            }

            if ((event.getClickCount() == 2) && (event.getButton() == MouseEvent.BUTTON1)) {
                // Left mouse has been clicked - create a default node at this position
                // else cannot create a new node clicki-bunti if the gui coord cannot be
                // translated to logic coordinates.
                if (GraphPanel.this.getPt().supportReverseTranslation()) {
                    GraphPanel.this.getPt().translateToLogicPosition(event.getX(), event.getY());
                    try {
                        GraphPanel.this.getParentGUI().addSingleDefaultNode(new Position(GraphPanel.this.getPt().getLogicX(), GraphPanel.this.pt.getLogicY(), GraphPanel.this.pt.getLogicZ()));
                        GraphPanel.this.getParentGUI().redrawGUI();
                    } catch (WrongConfigurationException e1) {
                        Main.minorError(e1);
                    }
                    GraphPanel.this.repaint();
                }
            } else if (event.getButton() == MouseEvent.BUTTON3) {
                // Right mouse button has been clicked - show menu
                Node clickedNode = null;
                Edge clickedEdge = null;
                // go throught all the nodes and their edges to find out, if one is under the
                // cursor
                Enumeration<Node> nodeEnumer = SinalgoRuntime.getNodes().getSortedNodeEnumeration(false);
                while (nodeEnumer.hasMoreElements()) {
                    Node node = nodeEnumer.nextElement();
                    if (node.isInside(event.getX(), event.getY(), GraphPanel.this.getPt())) {
                        // rightClick on a Node
                        clickedNode = node;
                        break; // take the first node that matches
                    }
                    if (clickedEdge == null) {
                        clickedEdge = GraphPanel.this.getFirstEdgeAtPosition(event.getX(), event.getY(), node);
                    }
                }
                if (clickedNode != null) {
                    Global.getLog().logln(LogL.GUI_DETAIL, "User clicked on node " + clickedNode.getID());
                    GraphPanel.this.getNodePopupMenu().compose(clickedNode);
                    GraphPanel.this.getNodePopupMenu().show(event.getComponent(), event.getX(), event.getY());
                } else if (clickedEdge != null) {
                    Global.getLog().logln(LogL.GUI_DETAIL, "right click on a edge");
                    GraphPanel.this.getEdgePopupMenu().compose(clickedEdge);
                    GraphPanel.this.getEdgePopupMenu().show(event.getComponent(), event.getX(), event.getY());
                } else {
                    Global.getLog().logln(LogL.GUI_DETAIL, "User clicked in the free space");
                    GraphPanel.this.getSpacePopupMenu().compose(event.getPoint());
                    GraphPanel.this.getSpacePopupMenu().show(event.getComponent(), event.getX(), event.getY());
                }
            } else if (event.getButton() == MouseEvent.BUTTON1 && GraphPanel.this.isUserSelectsNodeMode()) {
                Node selected = GraphPanel.this.getFirstNodeAtPosition(event.getX(), event.getY());
                if (selected != null) {
                    if (!GraphPanel.this.getUserSelectsNodeHandler().isEmpty()) {
                        Tuple<NodeSelectionHandler, String> h = GraphPanel.this.getUserSelectsNodeHandler().pop();
                        GraphPanel.this.setUserSelectsNodeMode(!GraphPanel.this.getUserSelectsNodeHandler().isEmpty());
                        GraphPanel.this.setDefaultCursor();
                        GraphPanel.this.repaint(); // async call that does not repaint the network graph, but only the stuff on top
                        // of the graph
                        h.getFirst().handleNodeSelectedEvent(selected);
                    } else {
                        GraphPanel.this.setUserSelectsNodeMode(false);
                    }
                }
            }

            Global.getLog().logln(LogL.GUI_ULTRA_DETAIL, "Mouse Clicked finished");
        }

        @Override
        public void mousePressed(MouseEvent e) {
            // block any mouse events while a simulation round is being performed
            if (Global.isRunning()) {
                return;
            }
            Global.getLog().logln(LogL.GUI_DETAIL, "Mouse Pressed");

            if (e.getButton() == MouseEvent.BUTTON3) {
                // The right mouse button is pressed : move a node
                if (GraphPanel.this.getNodeToDrag() == null) {
                    Node node = GraphPanel.this.getFirstNodeAtPosition(e.getX(), e.getY());
                    if (null != node) {
                        GraphPanel.this.getNodeToDragInitialPosition().assign(node.getPosition());
                        GraphPanel.this.requestFocusInWindow(); // request focus s.t. key events are obtained (escape)
                        if (GraphPanel.this.getPt().supportReverseTranslation()) { // only start dragging if it's supported
                            GraphPanel.this.setNodeToDrag(node);
                            GraphPanel.this.getParentGUI().getComponent(0).setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        } else {
                            GraphPanel.this.setNodeToDrag(node);
                            GraphPanel.this.setNodeToDragDrawCoordCube(node);
                            GraphPanel.this.setMinMouseMovementUntilNodeMovement(10);
                            GraphPanel.this.repaint(); // did not change the graph!
                        }
                    } else {
                        // rotate if 3D
                        if (GraphPanel.this.getPt() instanceof Transformation3D) {
                            GraphPanel.this.setRotateStartPoint(e.getPoint());
                        }
                    }
                }
            } else if (e.getButton() == MouseEvent.BUTTON1) { // the left-button
                if (e.isControlDown()) {
                    // left button + control = zoom into a region
                    GraphPanel.this.setZoomRect(new Rectangle(e.getX(), e.getY(), 0, 0));
                } else {

                    // The left mouse button is pressed - connect two nodes if the mouse
                    // event started over a node
                    if (GraphPanel.this.getNodeToAddEdge() == null) {
                        Node node = GraphPanel.this.getFirstNodeAtPosition(e.getX(), e.getY());
                        if (null != node) {
                            GraphPanel.this.setNodeToAddEdge(node);
                            GraphPanel.this.requestFocusInWindow(); // request focus to obtain key events (escape)
                        } else {
                            // scroll the pane
                            GraphPanel.this.setShiftStartPoint(e.getPoint());
                            GraphPanel.this.getParentGUI().getComponent(0).setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                        }
                    }
                }
            }
            Global.getLog().logln(LogL.GUI_ULTRA_DETAIL, "Mouse Pressed finished");
        }

        @Override
        public void mouseReleased(MouseEvent e) {

            // block this during a runing simulation.
            if (Global.isRunning()) {
                return;
            }

            Global.getLog().logln(LogL.GUI_DETAIL, "Mouse Released");

            GraphPanel.this.setShiftStartPoint(null);
            GraphPanel.this.setRotateStartPoint(null);

            if (e.getButton() == MouseEvent.BUTTON1) { // the left button
                if (GraphPanel.this.getNodeToAddEdge() != null) {
                    Node targetNode = GraphPanel.this.getFirstNodeAtPosition(e.getX(), e.getY());
                    // check if there is a targetNode otherwise the endpoint isn't a node (do
                    // nothing then)
                    if (targetNode != null) {
                        // check if the target node is different to the startnode (do not add an edge
                        // from a node to itself
                        if (targetNode.getID() != GraphPanel.this.getNodeToAddEdge().getID()) {
                            try {
                                // the user added a edge from nodeToAddEdge to targetNode
                                GraphPanel.this.getNodeToAddEdge().getOutgoingConnections()
                                        .add(GraphPanel.this.getNodeToAddEdge(), targetNode, false);
                            } catch (WrongConfigurationException wCE) {
                                JOptionPane.showMessageDialog(GraphPanel.this.getParentGUI(), wCE.getMessage(),
                                        "Configuration Error", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                    GraphPanel.this.setTargetNodeToAddEdge(null);
                    GraphPanel.this.setNodeToAddEdge(null);
                    if (targetNode != null) {
                        GraphPanel.this.getParentGUI().redrawGUI(); // we added an edge, need to repaint the graph
                    } else {
                        GraphPanel.this.repaint();
                    }
                } else {
                    // Translate the graph
                    GraphPanel.this.setDefaultCursor();
                }
                // Handle the button-release for the case that we were zooming into a rectangle
                if (GraphPanel.this.getZoomRect() != null) {
                    if ((Math.abs(GraphPanel.this.getZoomRect().height) > GraphPanel.this.getZoomRectMinSize())
                            && (Math.abs(GraphPanel.this.getZoomRect().width) > GraphPanel.this.getZoomRectMinSize())) {
                        if (GraphPanel.this.getZoomRect().width < 0) {
                            GraphPanel.this.getZoomRect().x += GraphPanel.this.getZoomRect().width;
                            GraphPanel.this.getZoomRect().width = -GraphPanel.this.getZoomRect().width;
                        }
                        if (GraphPanel.this.getZoomRect().height < 0) {
                            GraphPanel.this.getZoomRect().y += GraphPanel.this.getZoomRect().height;
                            GraphPanel.this.getZoomRect().height = -GraphPanel.this.getZoomRect().height;
                        }
                        GraphPanel.this.getPt().zoomToRect(GraphPanel.this.getZoomRect());
                        GraphPanel.this.getParentGUI().setZoomFactor(GraphPanel.this.getPt().getZoomFactor());
                    }
                    GraphPanel.this.setZoomRect(null);
                    GraphPanel.this.getParentGUI().redrawGUI();
                }
            } else if (e.getButton() == MouseEvent.BUTTON3) { // the right button
                GraphPanel.this.setNodeToDragDrawCoordCube(null);
                GraphPanel.this.setDefaultCursor();
                GraphPanel.this.setNodeToDrag(null);
                GraphPanel.this.getParentGUI().redrawGUI();
            }
            Global.getLog().logln(LogL.GUI_ULTRA_DETAIL, "Mouse Released finished");
        }

        /**
         * Translates the view such that a dragged object may be placed outside the
         * current visible area.
         *
         * @param p The current position of the cursor
         */
        private void moveViewOnMousesDrag(Point p) {
            int dx = 0, dy = 0;
            int move = 10;
            int border = 10;
            boolean requireMove = false;
            if (p.x < border) {
                dx = move;
                requireMove = true;
            }
            if (p.x > GraphPanel.this.getImageSizeX() - border) {
                dx = -move;
                requireMove = true;
            }
            if (p.y < border) {
                dy = 10;
                requireMove = true;
            }
            if (p.y > GraphPanel.this.getImageSizeY() - border) {
                dy = -move;
                requireMove = true;
            }
            if (requireMove) {
                GraphPanel.this.getPt().moveView(dx, dy);
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            // block this during a runing simulation.
            if (Global.isRunning()) {
                return;
            }

            GraphPanel.this.getCurrentCursorPosition().setLocation(e.getX(), e.getY());
            if (GraphPanel.this.getPt().supportReverseTranslation()) {
                GraphPanel.this.getPt().translateToLogicPosition(e.getX(), e.getY());
                if ((GraphPanel.this.getPt().getLogicX() < Configuration.getDimX())
                        && (GraphPanel.this.getPt().getLogicX() > 0)
                        && (GraphPanel.this.getPt().getLogicY() < Configuration.getDimY())
                        && (GraphPanel.this.getPt().getLogicY() > 0)) {
                    GraphPanel.this.getParentGUI().setMousePosition(GraphPanel.this.getPt().getLogicPositionString());
                }
            }

            Global.getLog().logln(LogL.GUI_DETAIL, "Mouse Dragged");
            if (GraphPanel.this.getNodeToDrag() != null) {
                if (GraphPanel.this.getPt().supportReverseTranslation()) {
                    // cannot support node movement by the mouse if the gui coordinate cannot be
                    // translated to the logic counterpart
                    GraphPanel.this.getPt().translateToLogicPosition(e.getX(), e.getY());
                    GraphPanel.this.getNodeToDrag()
                            .setPosition(GraphPanel.this.getPt().getLogicX(), GraphPanel.this.getPt().getLogicY(), GraphPanel.this.getPt().getLogicZ());
                    this.moveViewOnMousesDrag(e.getPoint());
                    GraphPanel.this.getParentGUI().redrawGUI(); // we need to repaint the graph panel
                } else {
                    // 3D: move along the axis to which the angle of the mouse-motion is smallest
                    GraphPanel.this.getPt().translateToGUIPosition(GraphPanel.this.getNodeToDrag().getPosition());

                    // mouse diff vector
                    int mouseDx = e.getX() - GraphPanel.this.getPt().getGuiX();
                    int mouseDy = e.getY() - GraphPanel.this.getPt().getGuiY();
                    double mouseLength = Math.sqrt(mouseDx * mouseDx + mouseDy * mouseDy);
                    if (mouseLength <= GraphPanel.this.getMinMouseMovementUntilNodeMovement()) {
                        return;
                    }
                    GraphPanel.this.setMinMouseMovementUntilNodeMovement(1); // after starting to move, we use a better resolution

                    GraphPanel.this.getPt().translateToGUIPosition(0, 0, 0);
                    double originX = GraphPanel.this.getPt().getGuiXDouble(), originY = GraphPanel.this.getPt().getGuiYDouble();

                    // mouse-movement in direction of x-axis
                    GraphPanel.this.pt.translateToGUIPosition(1, 0, 0);
                    double cX = GraphPanel.this.getPt().getGuiXDouble() - originX;
                    double cY = GraphPanel.this.getPt().getGuiYDouble() - originY;
                    double xLength = Math.sqrt(cX * cX + cY * cY);
                    double aX = (mouseDx * cX + mouseDy * cY) / (mouseLength * xLength); // cos(angle) of mouse-movement
                    // with x-axis

                    // mouse-movement in direction of y-axis
                    GraphPanel.this.getPt().translateToGUIPosition(0, 1, 0);
                    cX = GraphPanel.this.getPt().getGuiXDouble() - originX;
                    cY = GraphPanel.this.getPt().getGuiYDouble() - originY;
                    double yLength = Math.sqrt(cX * cX + cY * cY);
                    double aY = (mouseDx * cX + mouseDy * cY) / (mouseLength * yLength); // cos(angle) of mouse-movement
                    // with y-axis

                    // mouse-movement in direction of z-axis
                    GraphPanel.this.getPt().translateToGUIPosition(0, 0, 1);
                    cX = GraphPanel.this.getPt().getGuiXDouble() - originX;
                    cY = GraphPanel.this.getPt().getGuiYDouble() - originY;
                    double zLength = Math.sqrt(cX * cX + cY * cY);
                    double aZ = (mouseDx * cX + mouseDy * cY) / (mouseLength * zLength); // cos(angle) of mouse-movement
                    // with z-axis

                    // Don't move along an axis if the axis is nearly perpendicular to the screen
                    if (xLength * 15 < yLength && xLength * 15 < zLength) {
                        aX = 0;
                    }
                    if (yLength * 15 < xLength && yLength * 15 < zLength) {
                        aY = 0;
                    }
                    if (zLength * 15 < xLength && zLength * 15 < yLength) {
                        aZ = 0;
                    }

                    Position p = GraphPanel.this.getNodeToDrag().getPosition();
                    if (Math.abs(aX) > Math.abs(aY) && Math.abs(aX) > Math.abs(aZ)) {
                        GraphPanel.this.getNodeToDrag().setPosition(p.getXCoord() + Math.signum(aX) * mouseLength / xLength, p.getYCoord(), p.getZCoord());
                    } else if (Math.abs(aY) > Math.abs(aZ)) {
                        GraphPanel.this.getNodeToDrag().setPosition(p.getXCoord(), p.getYCoord() + Math.signum(aY) * mouseLength / yLength, p.getZCoord());
                    } else {
                        GraphPanel.this.getNodeToDrag().setPosition(p.getXCoord(), p.getYCoord(), p.getZCoord() + Math.signum(aZ) * mouseLength / zLength);
                    }
                    this.moveViewOnMousesDrag(e.getPoint());
                    GraphPanel.this.getParentGUI().redrawGUI(); // we need to repaint the graph panel
                }
            } else if (GraphPanel.this.getNodeToAddEdge() != null) {
                this.moveViewOnMousesDrag(e.getPoint());
                GraphPanel.this.setTargetNodeToAddEdge(GraphPanel.this.getFirstNodeAtPosition(e.getX(), e.getY()));
                // the drawing of the line is done while redrawing
                GraphPanel.this.repaint();
            } else if (GraphPanel.this.getZoomRect() != null) {
                GraphPanel.this.getZoomRect().width = e.getX() - GraphPanel.this.getZoomRect().x;
                GraphPanel.this.getZoomRect().height = e.getY() - GraphPanel.this.getZoomRect().y;
                // currently, it's only allowed to select the region from top left to bottom
                // right :-(
                // if(zoomRect.width < 0){ zoomRect.width = 0; }
                // if(zoomRect.height < 0){ zoomRect.height = 0; }
                GraphPanel.this.repaint();
            } else if (GraphPanel.this.getShiftStartPoint() != null) {
                GraphPanel.this.getPt().moveView(e.getX() - GraphPanel.this.getShiftStartPoint().x,
                        e.getY() - GraphPanel.this.getShiftStartPoint().y);
                GraphPanel.this.setShiftStartPoint(e.getPoint());
                GraphPanel.this.getParentGUI().redrawGUI(); // we need to redraw the graph - the view has changed
            } else if (GraphPanel.this.getRotateStartPoint() != null) {
                if (GraphPanel.this.getPt() instanceof Transformation3D) {
                    Transformation3D t3d = (Transformation3D) GraphPanel.this.getPt();
                    t3d.rotate(e.getX() - GraphPanel.this.getRotateStartPoint().x,
                            e.getY() - GraphPanel.this.getRotateStartPoint().y, !e.isControlDown(), false); // read
                    // keyboard - ctrl allows to freely rotate
                    GraphPanel.this.setRotateStartPoint(e.getPoint());
                    GraphPanel.this.getParentGUI().redrawGUI(); // need to redraw the graph - the view has changed
                }
            }
            Global.getLog().logln(LogL.GUI_ULTRA_DETAIL, "Mouse Dragged finished");
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            GraphPanel.this.getCurrentCursorPosition().setLocation(e.getX(), e.getY());
            if (GraphPanel.this.getPt().supportReverseTranslation()) {
                GraphPanel.this.getPt().translateToLogicPosition(e.getX(), e.getY());
                if ((GraphPanel.this.getPt().getLogicX() < Configuration.getDimX())
                        && (GraphPanel.this.getPt().getLogicX() > 0)
                        && (GraphPanel.this.getPt().getLogicY() < Configuration.getDimY())
                        && (GraphPanel.this.getPt().getLogicY() > 0)) {
                    GraphPanel.this.getParentGUI().setMousePosition(GraphPanel.this.getPt().getLogicPositionString());
                }
            }

            if (GraphPanel.this.isUserSelectsNodeMode()) {
                GraphPanel.this.setUserSelectsNodeCurrentFocus(GraphPanel.this.getFirstNodeAtPosition(e.getX(), e.getY()));
                GraphPanel.this.repaint(); // async call that does not repaint the network graph, but only the stuff on top
                // of the graph
            } else {
                GraphPanel.this.setUserSelectsNodeCurrentFocus(null);
            }
            if (GraphPanel.this.getToolTipDrawCoordCube() != null) {
                GraphPanel.this.setToolTipDrawCoordCube(null);
                GraphPanel.this.repaint();
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            // block zooming while a simulation is running
            if (Global.isRunning()) {
                return;
            }
            int clicks = e.getWheelRotation();
            if (clicks < 0) {
                GraphPanel.this.getParentGUI().zoom(Configuration.getWheelZoomStep()); // zoom In
            } else {
                GraphPanel.this.getParentGUI().zoom(1.0 / Configuration.getWheelZoomStep()); // zoom out
            }
        }
    } // END OF CLASS MyMouseListener

    class MyKeyListener implements KeyListener {

        @Override
        public void keyPressed(KeyEvent e) {
            // react to pressing escape
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                if (GraphPanel.this.getNodeToDrag() != null) { // stop dragging a node, and undo
                    GraphPanel.this.getNodeToDrag().getPosition().assign(GraphPanel.this.getNodeToDragInitialPosition());
                    GraphPanel.this.setNodeToDragDrawCoordCube(null);
                    GraphPanel.this.setNodeToDrag(null);
                    GraphPanel.this.getParentGUI().redrawGUI(); // node position has changed, full repaint
                }
                if (GraphPanel.this.getNodeToAddEdge() != null) {
                    GraphPanel.this.setNodeToAddEdge(null);
                    GraphPanel.this.setTargetNodeToAddEdge(null);
                    GraphPanel.this.repaint(); // no need to redraw whole gui, just repaint layer
                }
                if (GraphPanel.this.getZoomRect() != null) {
                    GraphPanel.this.setZoomRect(null);
                    GraphPanel.this.repaint();
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }
    }

    /**
     * Compontent Listener implementation
     */
    class MyComponentListener implements ComponentListener {

        @Override
        public void componentResized(ComponentEvent e) {
            GraphPanel.this.createNewOffscreen();
            // don't force a redraw, s.t. if the resize happens during a simulation, the
            // graph is not repainted. (but this may leave an empty graph if a simulation is
            // running)
            // We could disallow to resize the window during simulation, but then, the
            // window flickers
            GraphPanel.this.getParentGUI().redrawGUI();
        }

        @Override
        public void componentMoved(ComponentEvent e) {
        }

        @Override
        public void componentShown(ComponentEvent e) {
        }

        @Override
        public void componentHidden(ComponentEvent e) {
        }
    } // END OF CLASS MyComponentListener
}
