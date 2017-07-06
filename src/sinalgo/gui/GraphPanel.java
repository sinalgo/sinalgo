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
package sinalgo.gui;


import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolTip;
import javax.swing.event.MouseInputListener;

import sinalgo.configuration.Configuration;
import sinalgo.configuration.WrongConfigurationException;
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
import sinalgo.runtime.Runtime;
import sinalgo.tools.Tuple;
import sinalgo.tools.logging.LogL;
import sinalgo.tools.logging.Logging;


/**
 * A panel where the Graph is painted into.
 */
@SuppressWarnings("serial")
public class GraphPanel extends JPanel {
	private Image offscreen = null;
	// needs to be set to true whenever offscreen has been assigned a new object
	private boolean newOffscreen = true;
	
	private boolean forcedDraw = false;
	
	private NodePopupMenu nodePopupMenu = null;
	private EdgePopupMenu edgePopupMenu = null;
	private SpacePopupMenu spacePopupMenu = null;
	private GUI parent;
	
	private Node nodeToDrag;
	private Position nodeToDragInitialPosition = new Position(); // initial position of the node that is being dragged, only set if nodeToDrag is set
	private Node nodeToAddEdge;
	private Node targetNodeToAddEdge;
	private Point shiftStartPoint;
	private Point rotateStartPoint;
	private Vector<Node> nodesToHighlight = new Vector<Node>(10);
	private Vector<Node> nodesToDrawCoordCube = new Vector<Node>(10);
	private Node toolTipDrawCoordCube = null;
	private Node nodeToDragDrawCoordCube = null;
	private int minMouseMovementUntilNodeMovement = 10; // threshold (in mouse-pixels) for 3D before moving a node
	
	//this scales the underlying image
	private int imageSizeX = 0; 
	private int imageSizeY = 0; 
	
	private Point currentCursorPosition = new Point(0,0); // the position of the cursor
	private Rectangle zoomRect = null;
	private int zoomRectMinSize = 5;
	
	MyMouseListener ml;
	
	private Logging log = Logging.getLogger(); // system wide logger
	
	// The first time we draw the graph, apply the default view
	private boolean defaultViewOnNextDraw = true;

	/**
	 * A boolean indicating whether the graph was already painted once or not.
	 */
	public static boolean firstTimePainted = false;
	
	private PositionTransformation pt;
	private int myLastPtVersionNumber = -1;
	
	// Support to let the user select a node
	private int cancelAreaWidth, cancelAreaHeight, cancelAreaOffsetX; // dimension of the cancel area printed directly onto the graphics
	private Node userSelectsNodeCurrentFocus = null; // the node over which the mouse currently hovers, if the user is to select a node
	// Set to true while the user is asked to select a node
	private boolean userSelectsNodeMode = false;
	private Stack<Tuple<NodeSelectionHandler, String>> userSelectsNodeHandler = new Stack<Tuple<NodeSelectionHandler, String>>(); // stack of handlers, pick topmost one
	
	/**
	 * Constructor for the GraphPanel class.
	 * @param p The parent Frame (GUI) where the Graph Panel is added.
	 */
	public GraphPanel(GUI p){
		parent = p;
		pt = parent.getTransformator();
		
		ml = new MyMouseListener();
		this.addMouseListener(ml);
		this.addMouseMotionListener(ml);
		this.addMouseWheelListener(ml);
		this.addKeyListener(new MyKeyListener()); 
		this.setFocusable(true);
		
		nodePopupMenu = new NodePopupMenu(parent);
		this.add(nodePopupMenu);
		
		edgePopupMenu = new EdgePopupMenu(parent);
		this.add(edgePopupMenu);
		
		spacePopupMenu = new SpacePopupMenu(parent);
		this.add(spacePopupMenu);
		
		imageSizeX = getWidth();
		imageSizeY = getHeight();

		// update the transformation object 
		pt.setWidth(imageSizeX);
		pt.setHeight(imageSizeY);
		
		this.addComponentListener(new MyComponentListener());
	}

	/**
	 * Called when the user removes all nodes
	 */
	public void allNodesAreRemoved() {
		nodesToDrawCoordCube.clear();
		nodesToHighlight.clear();
	}
	
	/**
	 * Triggers the default view the next time this graph panel is redrawn.  
	 */
	public void requestDefaultViewOnNextDraw() {
		defaultViewOnNextDraw = true;
	}
	
	/**
	 * Requires that the graph panel is fully redrawn 
	 * during the next call to paint().
	 */
	public void requireFullDrawOnNextPaint() {
		log.logln(LogL.GUI_SEQ, "GraphPanel.requireFullDrawOnNextPaint()s");
		forcedDraw = true;
	}
	
	/**
	 * This method zooms its ZoomableGraphics so that field fits best in the scrollable pane
	 * and redraws the graph.
	 */
	public void defaultView() {
		pt.defaultView(imageSizeX, imageSizeY);
		parent.setZoomFactor(pt.getZoomFactor()); // initiates redrawing the graph
	}
	
	/**
	 * @see GraphPanel#defaultView() 
	 */
	private void defaultViewWithoutRedraw() {
		pt.defaultView(imageSizeX, imageSizeY);
		parent.setZoomFactorNoRepaint(pt.getZoomFactor());
	}
	

	/**
	 * Creates a new _offscreen image object according 
	 * to the current dimensions of this panel. 
	 */
	private void getNewOffscreen() {
		log.logln(LogL.GUI_SEQ, "GraphPanel.getNewOffscreen: Allocating a new offscreen image.");
		imageSizeX = getWidth();
		imageSizeY = getHeight();
		offscreen = null;
		if(imageSizeX > 0 && imageSizeY > 0) {
			// update the transformation object 
			pt.setWidth(imageSizeX);
			pt.setHeight(imageSizeY);
			offscreen = createImage(imageSizeX, imageSizeY);
			newOffscreen = true;
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.Component#paint(java.awt.Graphics)
	 */
	public void paint(Graphics g) {
		if(Global.isRunning) {
			// if possible, draw the previous image, but without updating it!
			if(offscreen != null){
				g.drawImage(offscreen, 0, 0, this);
				//drawOnTop(this.getGraphics());
				log.logln(LogL.GUI_SEQ, "GraphPanel.paint(): Simulation is running -> draw offscreen.");
			}
			return;
		}

		if(imageSizeX != this.getWidth() || imageSizeY != this.getHeight()) {
			log.logln(LogL.GUI_SEQ, "GraphPanel.paint(): We missed a resize event.");
			getNewOffscreen();
		}
		GraphPanel.firstTimePainted = true;
		if(offscreen == null){
			getNewOffscreen();
		}
		if(offscreen != null) {
			// we may not need to redraw the graph, but can reuse the old offscreen image
			if(myLastPtVersionNumber != pt.getVersionNumber() || newOffscreen || forcedDraw) {
				log.logln(LogL.GUI_SEQ, "GraphPanel.paint(): drawing graph to offscreen" + (myLastPtVersionNumber != pt.getNumberOfDimensions() ? " ptVersionNumber changed" : " new Offscreen"));
				draw(offscreen.getGraphics());
				myLastPtVersionNumber = pt.getVersionNumber();
				forcedDraw = false;
				newOffscreen = false;
			} else {
				log.logln(LogL.GUI_SEQ, "GraphPanel.paint(): no changes -> draw old offscreen");
			}
			g.drawImage(offscreen, 0, 0, this);
			drawOnTop(g);
		} else {
			// the offscreen object is not available - draw on the provided graphics directly (note: this will draw the background white)
			draw(g);
			drawOnTop(g);
		}
	}
	
	/**
	 * Immediately repaints the graph. 
	 * You should call GUI#redrawGraph() to redraw the 
	 * graph, this method is used internaly.   
	 */
	public void paintNow() {
		log.log(LogL.GUI_SEQ, "GraphPanel.paintNow()");
		if(offscreen != null) {
			draw(offscreen.getGraphics());
			myLastPtVersionNumber = pt.getVersionNumber();
			newOffscreen = false;
			this.getGraphics().drawImage(offscreen, 0, 0, this);
			drawOnTop(this.getGraphics());
		} else {
			this.repaint(); // defer paint to default call
		}
	}
	
	/**
	 * Draws the graph to a given graphics object.
	 * @param g The graphics to paint to
	 */
	private void draw(Graphics g) {
		synchronized(pt){
			log.logln(LogL.GUI_SEQ, "GraphPanel.draw(): draw imgSize=(" + imageSizeX + "," + imageSizeY + ")");
			if(defaultViewOnNextDraw) {
				defaultViewWithoutRedraw();
				defaultViewOnNextDraw = false;
			}
			
			g.clearRect(0, 0, imageSizeX, imageSizeY);
			pt.drawBackground(g);
			
			if(Configuration.useMap){
				Runtime.map.paintMap(g, pt);
			}
	
			g.setColor(Color.BLACK);
			
			// Draw the graph
			try{
				// First draw all edges, only then the nodes
				Enumeration<Node> nodeEnumer;
				if(Configuration.drawEdges) {
					nodeEnumer = Runtime.nodes.getSortedNodeEnumeration(true);
					while(nodeEnumer.hasMoreElements()){
						Node node = nodeEnumer.nextElement();
						// first draw all outgoing edges of this node
						Iterator<Edge> edgeIter = node.outgoingConnections.iterator();
						while(edgeIter.hasNext()){
							Edge e = edgeIter.next();
							e.draw(g, pt);
						}
					}
				}
				// Draw the nodes in a separate loop
				if(Configuration.drawNodes) {
					// Draw the nodes in a separate loop
					nodeEnumer = Runtime.nodes.getSortedNodeEnumeration(true);
					while(nodeEnumer.hasMoreElements()){
						Node node = nodeEnumer.nextElement();
						node.draw(g, pt, false);
					}
				}
			} 
			catch(ConcurrentModificationException eME){
				//catch the concurrent modification exception from the iterators of the drawing. Do not do anything when it is thrown.
				//We don't care, when this exception is thrown because this only happens when someone is zooming in the middle of a 
				//round. It only happens if an edge is added or removed from a node during redrawing. Catching this exception and not
				//doing anything results in a wrong picture during zooming (when there was a concurrentModificationException) and we
				//decided that it is not worth the slowdown of the simulation just to always get a correct picture.
			}
			
			if(Configuration.showMessageAnimations) {
				Animations.drawEnvelopes(g, pt);
			}
			
			// perform the custom drawing. Note that the custom paint is only called when also the
			// entire graph was painted. This ensures that there should be no conflict due to any
			// concurrent data accesses
			Global.customGlobal.customPaint(g, pt);
		}
	}
	
	/**
	 * Draws all the additional stuff over the given graphics. Additional stuff means the temporary stuff from the 
	 * mouse input like the red edge during adding an edge or the red square that is drawn to zoom to a special position
	 * or a node to highlight.
	 *  
	 * @param g The graphics object to draw the additional stuff onto.
	 */
	public void drawOnTop(Graphics g) {
		// draw the line to add a new edge
		if(nodeToAddEdge != null) {
			pt.translateToGUIPosition(nodeToAddEdge.getPosition());
			if(pt.guiX != currentCursorPosition.x || pt.guiY != currentCursorPosition.y) {
				Arrow.drawArrow(pt.guiX, pt.guiY, currentCursorPosition.x, currentCursorPosition.y, 
				                g, pt, Color.RED);
			}
		}

		// draw the rectangle for zooming
		if(zoomRect != null){
			if((Math.abs(zoomRect.height) > zoomRectMinSize) && (Math.abs(zoomRect.width) > zoomRectMinSize)) {
				Color temp = g.getColor();
				g.setColor(Color.RED);
				int topx = zoomRect.x;
				int topy = zoomRect.y;
				if(zoomRect.width < 0) {
					topx += zoomRect.width;
				}
				if(zoomRect.height < 0) {
					topy += zoomRect.height;
				}
				g.drawRect(topx, topy, Math.abs(zoomRect.width), Math.abs(zoomRect.height));
				// VERY strange: If we also draw some non-vertical/non-horizontal lines when drawing 
				// the rectangle (which consists of only vertical and horizontal lines), the
				// drawing method becomes SOMEHOW MUCH faster. 
				// (It seems not to matter whether we draw the lines before or after, drawing the 
				// line outside the clipping area seems not to help.)
				// OK - seems not to be a problem on all PCs... 
				g.drawLine(zoomRect.x, zoomRect.y, zoomRect.x + 1, zoomRect.y +1);
				g.setColor(temp);
			}
		}
		
		// Draw the highlighted node
		for(Node highLighted : nodesToHighlight) {
			highLighted.draw(g, pt, true);
		}
		if(toolTipDrawCoordCube != null) {
			this.drawNodeCubeCoords(g, toolTipDrawCoordCube);
		}
		if(nodeToDragDrawCoordCube != null) {
			this.drawNodeCubeCoords(g, nodeToDragDrawCoordCube);
		}
		for(Node cubeNode : nodesToDrawCoordCube) {
			this.drawNodeCubeCoords(g, cubeNode);
		}
		if(nodeToAddEdge != null) {
			nodeToAddEdge.draw(g, pt, true);
		}
		if(targetNodeToAddEdge != null) {
			targetNodeToAddEdge.draw(g, pt, true);
		}
		if(nodeToDrag != null) {
			nodeToDrag.draw(g, pt, true);
		}
		
		if(userSelectsNodeMode) {
			if(userSelectsNodeCurrentFocus != null) {
				userSelectsNodeCurrentFocus.draw(g, pt, true);
			}
			if(!userSelectsNodeHandler.isEmpty()) {
				Tuple<NodeSelectionHandler, String> h = userSelectsNodeHandler.peek();
				String text = h.second;
				String textCancel = "Cancel";
				Font font = new Font(null, 0, 12); 
				g.setFont(font);
				int len1 = (int) g.getFontMetrics().getStringBounds(text, g).getWidth();
				int len2 = (int) g.getFontMetrics().getStringBounds(textCancel, g).getWidth();
				int height = g.getFontMetrics().getHeight();
				g.setColor(Color.LIGHT_GRAY);
				// draw the boxes for the text and the button
				g.fill3DRect(0, 0, len1 + len2 + 38, height + 10, true);
				g.fill3DRect(len1 + 15, 3, len2 + 20, height+5, true);
				// Write the text
				g.setColor(Color.RED);
				g.drawString(text, 10, height+2);
				g.setColor(Color.BLACK);
				g.drawString(textCancel, len1 + 25, height+2);
				// set the 
				cancelAreaWidth = len2 + 25;
				cancelAreaHeight = height + 9;
				cancelAreaOffsetX = len1 + 10;
			}
		}
	}
	
	private void drawNodeCubeCoords(Graphics g, Node n) {
		Position p = n.getPosition();
		this.drawCubeCoordLine(g, p.xCoord, p.yCoord, p.zCoord, 0, p.yCoord, p.zCoord);
		this.drawCubeCoordLine(g, p.xCoord, p.yCoord, p.zCoord, p.xCoord, 0, p.zCoord);
		this.drawCubeCoordLine(g, p.xCoord, p.yCoord, p.zCoord, p.xCoord, p.yCoord, 0);
		this.drawCubeCoordLine(g, p.xCoord, 0, 0, p.xCoord, p.yCoord, 0);
		this.drawCubeCoordLine(g, 0, p.yCoord, 0, p.xCoord, p.yCoord, 0);
		this.drawCubeCoordLine(g, p.xCoord, 0, p.zCoord, 0, 0, p.zCoord);
		this.drawCubeCoordLine(g, p.xCoord, 0, p.zCoord, p.xCoord, 0, 0);
		this.drawCubeCoordLine(g, 0, p.yCoord, p.zCoord, 0, 0, p.zCoord);
		this.drawCubeCoordLine(g, 0, p.yCoord, p.zCoord, 0, p.yCoord, 0);
	}
	
	/**
	 * Forces the Graph panel that it draws itself in the next paint call. Note that this is not
	 * the same as RepaintNow as this only forces the next paint call to recalculate the background
	 * image and does not paint it. RepaintNow really paints it now. Note that this method is 
	 * very eccifient and useful if you know that the graphpanel is redrawn anyway after that call.
	 * Like when an popup method is closed.
	 */
	public void forceDrawInNextPaint(){
		forcedDraw = true;
	}
	
	/**
	 * This method overrides the createToolTip method of the JComponent. It returns a new
	 * instance of MyToolTip to display informations about the place where the cursor is
	 * stoped for a second.
	 * 
	 * @see javax.swing.JComponent#createToolTip()
	 */
	public JToolTip createToolTip(){
		return new MultiLineToolTip();
	}
	
	/**
	 * This method returns the String to display in the ToolTip for this Component.
	 * It checks, if there is a Node or an Edge under the current cursor position and
	 * returns an info string about it.
	 * 
	 * @return The String to display in the ToolTip for this Component.
	 * @see javax.swing.JComponent#getToolTipText(java.awt.event.MouseEvent)
	 */
	public String getToolTipText(MouseEvent event){
		
		//block the appearence of the ToolTip when the simulation is runing. This prevents the
		//simulation from getting inconsistencies from multiple threads drawing into the same
		//buffer
		if(Global.isRunning){
			return null;
		}
		
		Edge edgeUnderPos = null;
		
		Enumeration<Node> nodeEnumer = Runtime.nodes.getSortedNodeEnumeration(false);
		while(nodeEnumer.hasMoreElements()){
			Node node = nodeEnumer.nextElement();
			if(node.isInside(event.getX(), event.getY(), pt)){
				if(Configuration.dimensions == 3) {
					toolTipDrawCoordCube = node;
					repaint();
				}
				return "Node "+node.ID+":\n"+node.toString();
			}
			// give precendence to the nodes - only if there is no node at the cursor, select the edge
			if(edgeUnderPos == null) {
				edgeUnderPos = getFirstEdgeAtPosition(event.getX(), event.getY(), node);
			}
		}
		if(edgeUnderPos != null){
			return "Edge from "+edgeUnderPos.startNode.ID+" to "+edgeUnderPos.endNode.ID+":\n"+edgeUnderPos.toString();
		}
		return null;
	}
	
	/**
	 * Asks the user to select a node from the GUI and blocks until this is done.
	 * 
	 * If several calls interleave, the calls are served LIFO.
	 * 
	 * This method may only be called in GUI mode!
	 * @param handler The handler to invoke when a node is selected
	 * @param text Text to display to the user
	 */
	public void getNodeSelectedByUser(NodeSelectionHandler handler, String text) {
		if(!Global.isGuiMode) {
			Main.fatalError("Invalid call to 'GUI.getNodeSelectedByUser()'. This method is not supported in batch mode.");
		}
		userSelectsNodeHandler.push(new Tuple<NodeSelectionHandler, String>(handler, text));
		userSelectsNodeMode = true;
		setDefaultCursor();
		repaint(); // async call that does not repaint the network graph, but only the stuff on top of the graph
	}
	
	/**
	 * Set the default mouse cursor, depending on the current state of the GUI.
	 */
	private void setDefaultCursor() {
		if(userSelectsNodeMode) {
			parent.getComponent(0).setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); 
		} else {
			parent.getComponent(0).setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}
	

	/**
	 * Add a node for which the coordinate cube should be drawn. 
	 * A node may be added several times!
	 * @param n The node to add
	 */
	public void setNodeToDrawCoordinateCube(Node n) {
		nodesToDrawCoordCube.add(n);
	}
	
	/**
	 * Tests whether for a given node, the coordinate cube is drawn 
	 * @param n The node to test
	 * @return True if the coordinate cube is drawn for the given node
	 */
	public boolean containsNodeToDrawCoordinateCube(Node n) {
		return nodesToDrawCoordCube.contains(n);
	}
	
	/**
	 * Removes a node from the list of the nodes to draw the coordinate cube. 
	 * @param n The node to remove.
	 */
	public void removeNodeToDrawCoordinateCube(Node n) {
		nodesToDrawCoordCube.remove(n);
	}
	
	
	/**
	 * Sets the given node on highlighted if the highlighted-flag is set true. If the flag is false
	 * it checks, whethter the given node is highlighted and dis-highlights it.
	 * 
	 * @param n The node to highlight.
	 * @param highlighted Indicates whether to highlight the node or to dis-highlight it.
	 */
	public void setNodeHighlighted(Node n, boolean highlighted){
		if(highlighted){
			nodesToHighlight.add(n);
		}
		else if(nodesToHighlight.contains(n)){
			//if this is highlighted node, dis-highlight it.
			nodesToHighlight.remove(n);
		}
	}

	/**
	 * Loops over all nodes and returns the first one
	 * that covers the given position on the GUI. If no
	 * node covers this postion, this method returns null.
	 * @param x the x offset of the position to test
	 * @param y the y offset of the position to test
	 * @return An arbitrary node that covers the position (x,y) on the gui, null if no node covers this position.
	 */
	public Node getFirstNodeAtPosition(int x, int y) {
		Enumeration<Node> nodeEnumer = Runtime.nodes.getSortedNodeEnumeration(false);
		while(nodeEnumer.hasMoreElements()){
			Node node = nodeEnumer.nextElement();
			if(node.isInside(x, y, pt)) {
				return node;
			}
		}
		return null;
	}
	
	/**
	 * Loops over all edges of a node and returns the first
	 * one that touches a given position (x,y). If none of the edges
	 * touches (x,y), this method returns null.
	 * <p>
	 * If the found edge is bidirectional, the method returns the edge
	 * to whose end-point the mouse-click was closer.
	 * @param x the x offset of the position to test.
	 * @param y the y offset of the position to test.
	 * @param n The node for which the edges should be checked
	 * @return An arbitrary edge incident to node n that touches position (x,y) when drawn on the gui, null if no edge satisfies this criteria.
	 */
	public Edge getFirstEdgeAtPosition(int x, int y, Node n) {
		for(Edge e : n.outgoingConnections) {
			if(e.isInside(x, y, pt)) {
				Edge opposEdge = e.getOppositeEdge();
				if(opposEdge != null){
					//find out which one we want to delete.
					pt.translateToGUIPosition(e.endNode.getPosition());
					Position oneEnd = new Position(pt.guiXDouble, pt.guiYDouble, 0.0);
					pt.translateToGUIPosition(opposEdge.endNode.getPosition());
					Position otherEnd = new Position(pt.guiXDouble, pt.guiYDouble, 0.0);
					Position eventPos = new Position(x, y, 0.0);
					
					if(eventPos.distanceTo(oneEnd) > eventPos.distanceTo(otherEnd)){
						return opposEdge;
					}	else {
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
	 * @param g
	 * @param fromX
	 * @param fromY
	 * @param fromZ
	 * @param toX
	 * @param toY
	 * @param toZ
	 */
	public void drawCubeCoordLine(Graphics g, double fromX, double fromY, double fromZ, double toX, double toY, double toZ) {
		pt.translateToGUIPosition(fromX, fromY, fromZ);
		int guiX = pt.guiX;
		int guiY = pt.guiY;
		pt.translateToGUIPosition(toX, toY, toZ);
		g.setColor(Color.LIGHT_GRAY);
		g.drawLine(guiX, guiY, pt.guiX, pt.guiY);
	}
	
	
	/**
	 * Draws a dotted line on the graphics
	 * @param g The graphics to paint on
	 * @param fromX 
	 * @param fromY
	 * @param toX
	 * @param toY
	 */
	public static void drawDottedLine(Graphics g, int fromX, int fromY, int toX, int toY) {
		int dx = toX - fromX;
		int dy = toY - fromY;
		if(dx == 0 && dy == 0) {
			return;
		}
		boolean swapped = false;
		if(Math.abs(dx) < Math.abs(dy)) {
			int temp = fromX; fromX = fromY; fromY = temp;
			temp = toX; toX = toY; toY = temp;
			temp = dy; dy = dx; dx = temp;
			swapped = true;
		}
		if(dx < 0) { // swap 'from' and 'to' 
			int temp = fromX; fromX = toX; toX = temp;
			temp = fromY; fromY = toY; toY = temp;
			dx = -dx; dy = -dy;
		}
		double delta = ((double) dy) / dx;
		boolean paint = true;
		for(int i=0; i<= dx; i++) {
			int y = fromY + (int) (i * delta);
			if(paint) {
				if(swapped) {
					g.fillRect(y, i+fromX, 1, 1); // only a single dot
				} else {
					g.fillRect(i+fromX, y, 1, 1); // only a single dot
				}
			}
			paint = !paint;
		}
	}
	
	/**
	 * Draws a bold line between two points. This method produces
	 * an approximation by drawing several lines. It is possible, that
	 * the line is not tightly filled. 
	 * @param g The graphics to paint the line on
	 * @param fromX 
	 * @param fromY
	 * @param toX
	 * @param toY
	 * @param strokeWidth The width (in pixels) to draw the line
	 */
	public static void drawBoldLine(Graphics g, int fromX, int fromY, int toX, int toY, int strokeWidth) {
		for(int i=1; i<strokeWidth; i++) {
			g.drawLine(fromX+i, fromY, toX+i, toY);
			g.drawLine(fromX-i, fromY, toX-i, toY);
			g.drawLine(fromX, fromY+i, toX, toY+i);
			g.drawLine(fromX, fromY-i, toX, toY-i);
		}
	}
	
	class MyMouseListener implements MouseInputListener, MouseWheelListener{
		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
		 */
		public void mouseClicked(MouseEvent event){
			//block this during a runing simulation.
			if(Global.isRunning){
				return;
			}
			
			Global.log.logln(LogL.GUI_DETAIL, "Mouse Clicked");
				
			if(userSelectsNodeMode && event.getClickCount() == 1 && event.getButton() == MouseEvent.BUTTON1) {
				if(event.getX() >= cancelAreaOffsetX && 
						event.getX() <= cancelAreaOffsetX + cancelAreaWidth  &&
						event.getY() <= cancelAreaHeight) {
					if(!userSelectsNodeHandler.isEmpty()) {
						Tuple<NodeSelectionHandler,String> h = userSelectsNodeHandler.pop();
						userSelectsNodeMode = !userSelectsNodeHandler.isEmpty();
						repaint(); // async call that does not repaint the network graph, but only the stuff on top of the graph
						h.first.handleNodeSelectedEvent(null); // abort
					} else {
						userSelectsNodeMode = false;
						repaint(); // async call that does not repaint the network graph, but only the stuff on top of the graph
					}
					setDefaultCursor();
				}
			}
			
			
			
			if((event.getClickCount() == 2)&&(event.getButton() == MouseEvent.BUTTON1)){
				// Left mouse has been clicked - create a default node at this position
				if(pt.supportReverseTranslation()) {
					pt.translateToLogicPosition(event.getX(), event.getY());
					try {
						parent.addSingleDefaultNode(new Position(pt.logicX, pt.logicY, pt.logicZ)); 
						parent.redrawGUI();
					} catch (WrongConfigurationException e1) {
						Main.minorError(e1);
					}
					repaint();
				} else {
					// Cannot create a new node clicki-bunti if the gui coord cannot be translated to logic coordinates.
				}
			}	else if(event.getButton() == MouseEvent.BUTTON3){
				// Right mouse button has been clicked - show menu 
				Node clickedNode = null;
				Edge clickedEdge = null;
				//go throught all the nodes and their edges to find out, if one is under the cursor
				Enumeration<Node> nodeEnumer = Runtime.nodes.getSortedNodeEnumeration(false);
				while(nodeEnumer.hasMoreElements()){
					Node node = nodeEnumer.nextElement();
					if(node.isInside(event.getX(), event.getY(), pt)){
						//rightClick on a Node
						clickedNode = node;
						break; // take the first node that matches
					}
					if(clickedEdge == null) {
						clickedEdge = getFirstEdgeAtPosition(event.getX(), event.getY(), node);
					}
				}
				if(clickedNode != null){
					Global.log.logln(LogL.GUI_DETAIL,"User clicked on node "+clickedNode.ID);
					nodePopupMenu.compose(clickedNode);
					nodePopupMenu.show(event.getComponent(), event.getX(), event.getY());
				}
				else if(clickedEdge != null){
					Global.log.logln(LogL.GUI_DETAIL,"right click on a edge");
					edgePopupMenu.compose(clickedEdge);
					edgePopupMenu.show(event.getComponent(), event.getX(), event.getY());
				}
				else{
					Global.log.logln(LogL.GUI_DETAIL,"User clicked in the free space");
					spacePopupMenu.compose(event.getPoint());
					spacePopupMenu.show(event.getComponent(), event.getX(), event.getY());
				}
			} else if(event.getButton() == MouseEvent.BUTTON1 && userSelectsNodeMode){
				Node selected = getFirstNodeAtPosition(event.getX(), event.getY()); 
				if(selected != null) {
					if(!userSelectsNodeHandler.isEmpty()) {
						Tuple<NodeSelectionHandler,String> h = userSelectsNodeHandler.pop();
						userSelectsNodeMode = !userSelectsNodeHandler.isEmpty();
						setDefaultCursor();
						repaint(); // async call that does not repaint the network graph, but only the stuff on top of the graph
						h.first.handleNodeSelectedEvent(selected);
					} else {
						userSelectsNodeMode = false;
					}
				}
			}
			
			Global.log.logln(LogL.GUI_ULTRA_DETAIL, "Mouse Clicked finished");
		}
		
		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
		 */
		public void mousePressed(MouseEvent e){
			// block any mouse events while a simulation round is being performed
			if(Global.isRunning){
				return;
			}
			Global.log.logln(LogL.GUI_DETAIL, "Mouse Pressed");
			
			if(e.getButton() == MouseEvent.BUTTON3){
				// The right mouse button is pressed : move a node
				if(nodeToDrag == null){
					Node node = getFirstNodeAtPosition(e.getX(), e.getY());
					if(null != node) {
						nodeToDragInitialPosition.assign(node.getPosition());
						GraphPanel.this.requestFocusInWindow(); // request focus s.t. key events are obtained (escape)
						if(pt.supportReverseTranslation()) { // only start dragging if it's supported
							nodeToDrag = node;
							parent.getComponent(0).setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
						} else {
							nodeToDrag = node;
							nodeToDragDrawCoordCube = node;
							minMouseMovementUntilNodeMovement = 10;
							repaint(); // did not change the graph!
						}
					} else {
						// rotate if 3D
						if(pt instanceof Transformation3D) {
							rotateStartPoint = e.getPoint();
						}
					}
				} 
			}	
			else if(e.getButton() == MouseEvent.BUTTON1) { // the left-button
				if(e.isControlDown()) {
					// left button + control = zoom into a region 
					zoomRect = new Rectangle(e.getX(), e.getY(), 0, 0);	
				} else {
				
					// The left mouse button is pressed - connect two nodes if the mouse
					// event started over a node
					if(nodeToAddEdge == null){
						Node node = getFirstNodeAtPosition(e.getX(), e.getY());
						if(null != node) {
							nodeToAddEdge = node;		
							GraphPanel.this.requestFocusInWindow(); // request focus to obtain key events (escape)
						} else {
							// scroll the pane
							shiftStartPoint = e.getPoint();
							parent.getComponent(0).setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
						}
					}
				}
			}
			Global.log.logln(LogL.GUI_ULTRA_DETAIL, "Mouse Pressed finished");
		}
		
		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
		 */
		public void mouseReleased(MouseEvent e){
			
			//block this during a runing simulation.
			if(Global.isRunning){
				return;
			}
			
			Global.log.logln(LogL.GUI_DETAIL, "Mouse Released");
			
			shiftStartPoint = null;
			rotateStartPoint = null;
			
			if(e.getButton() == MouseEvent.BUTTON1) { // the left button
				if(nodeToAddEdge != null){
					Node targetNode = getFirstNodeAtPosition(e.getX(), e.getY()); 
					//check if there is a targetNode otherwise the endpoint isn't a node (do nothing then)
					if(targetNode != null){
						//check if the target node is different to the startnode (do not add an edge from a node to itself
						if(targetNode.ID != nodeToAddEdge.ID){
							try{
								//the user added a edge from nodeToAddEdge to targetNode
								nodeToAddEdge.outgoingConnections.add(nodeToAddEdge, targetNode, false);
							}
							catch(WrongConfigurationException wCE){
								JOptionPane.showMessageDialog(parent, wCE.getMessage(), "Configuration Error", JOptionPane.ERROR_MESSAGE);
							}
						}
					}
					targetNodeToAddEdge = null;
					nodeToAddEdge = null;
					if(targetNode != null) {
						parent.redrawGUI(); // we added an edge, need to repaint the graph
					} else {
						repaint();
					}
				} else {
					// Translate the graph
					setDefaultCursor();
				}
				// Handle the button-release for the case that we were zooming into a rectangle
				if(zoomRect != null){
					if((Math.abs(zoomRect.height) > zoomRectMinSize) && (Math.abs(zoomRect.width) > zoomRectMinSize)) {
						if(zoomRect.width < 0) {
							zoomRect.x += zoomRect.width;
							zoomRect.width = -zoomRect.width;
						}
						if(zoomRect.height < 0) {
							zoomRect.y += zoomRect.height;
							zoomRect.height = -zoomRect.height;
						}
						pt.zoomToRect(zoomRect);
						parent.setZoomFactor(pt.getZoomFactor());
					}
					zoomRect = null;
					parent.redrawGUI();
				}
			}
			else if (e.getButton() == MouseEvent.BUTTON3) { // the right button
				nodeToDragDrawCoordCube = null;
				setDefaultCursor();
				nodeToDrag = null;
				parent.redrawGUI();
			}
			Global.log.logln(LogL.GUI_ULTRA_DETAIL, "Mouse Released finished");
		}
		
		/**
		 * Translates the view such that a dragged object may be placed 
		 * outside the current visible area. 
		 * @param p The current position of the cursor
		 */
		private void moveViewOnMousesDrag(Point p) {
			int dx = 0, dy = 0;
			int move = 10;
			int border = 10;
			boolean requireMove = false;
			if(p.x < border) {
				dx = move;
				requireMove = true;
			}
			if(p.x > imageSizeX - border) {
				dx = -move;
				requireMove = true;
			}
			if(p.y < border) {
				dy = 10;
				requireMove = true;
			}
			if(p.y > imageSizeY - border) {
				dy = -move;
				requireMove = true;
			}
			if(requireMove) {
				pt.moveView(dx, dy);
			}
		}
		
		/* (non-Javadoc)
		 * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
		 */
		public void mouseDragged(MouseEvent e){
			//block this during a runing simulation.
			if(Global.isRunning){
				return;
			}

			currentCursorPosition.setLocation(e.getX(), e.getY());
			if(pt.supportReverseTranslation()) {
				pt.translateToLogicPosition(e.getX(), e.getY());
				if((pt.logicX < Configuration.dimX)&&
						(pt.logicX > 0)&&
						(pt.logicY < Configuration.dimY)&&
						(pt.logicY > 0)){
					parent.setMousePosition(pt.getLogicPositionString());
				}
			} 

			Global.log.logln(LogL.GUI_DETAIL, "Mouse Dragged");
			if(nodeToDrag != null){
				if(pt.supportReverseTranslation()) {
					// cannot support node movement by the mouse if the gui coordinate cannot be translated to the logic counterpart
					pt.translateToLogicPosition(e.getX(), e.getY());
					nodeToDrag.setPosition(pt.logicX, pt.logicY, pt.logicZ);
					moveViewOnMousesDrag(e.getPoint());
					parent.redrawGUI(); // we need to repaint the graph panel
				} else {
					// 3D: move along the axis to which the angle of the mouse-motion is smallest
					pt.translateToGUIPosition(nodeToDrag.getPosition());
					
					// mouse diff vector
					int mouseDx = e.getX() - pt.guiX;
					int mouseDy = e.getY() - pt.guiY;
					double mouseLength = Math.sqrt(mouseDx * mouseDx + mouseDy * mouseDy);
					if(mouseLength <= minMouseMovementUntilNodeMovement) {
						return;
					}
					minMouseMovementUntilNodeMovement = 1; // after starting to move, we use a better resolution
					
					pt.translateToGUIPosition(0, 0, 0);
					double originX = pt.guiXDouble, originY = pt.guiYDouble;
					
					// mouse-movement in direction of x-axis
					pt.translateToGUIPosition(1, 0, 0);
					double cX = pt.guiXDouble - originX;
					double cY = pt.guiYDouble - originY; 
					double xLength = Math.sqrt(cX * cX + cY * cY);
					double aX = (mouseDx * cX + mouseDy * cY) / (mouseLength * xLength); // cos(angle) of mouse-movement with x-axis

					// mouse-movement in direction of y-axis
					pt.translateToGUIPosition(0, 1, 0);
					cX = pt.guiXDouble - originX;
					cY = pt.guiYDouble - originY; 
					double yLength = Math.sqrt(cX * cX + cY * cY);
					double aY = (mouseDx * cX + mouseDy * cY) / (mouseLength * yLength); // cos(angle) of mouse-movement with y-axis

					// mouse-movement in direction of z-axis					
					pt.translateToGUIPosition(0, 0, 1);
					cX = pt.guiXDouble - originX;
					cY = pt.guiYDouble - originY; 
					double zLength = Math.sqrt(cX * cX + cY * cY);
					double aZ = (mouseDx * cX + mouseDy * cY) / (mouseLength * zLength); // cos(angle) of mouse-movement with z-axis
					
					// Don't move along an axis if the axis is nearly perpendicular to the screen
					if(xLength * 15 < yLength && xLength * 15 < zLength) {
						aX = 0;
					}
					if(yLength * 15 < xLength && yLength * 15 < zLength) {
						aY = 0;
					}
					if(zLength * 15 < xLength && zLength * 15 < yLength) {
						aZ = 0;
					}

					Position p = nodeToDrag.getPosition();
					if(Math.abs(aX) > Math.abs(aY) && Math.abs(aX) > Math.abs(aZ)) {
						nodeToDrag.setPosition(p.xCoord + Math.signum(aX) * mouseLength / xLength, p.yCoord, p.zCoord);
					} else if(Math.abs(aY) > Math.abs(aZ)) {
						nodeToDrag.setPosition(p.xCoord, p.yCoord + Math.signum(aY) * mouseLength / yLength, p.zCoord);
					} else {
						nodeToDrag.setPosition(p.xCoord, p.yCoord, p.zCoord + Math.signum(aZ) * mouseLength / zLength);
					}
					moveViewOnMousesDrag(e.getPoint());
					parent.redrawGUI(); // we need to repaint the graph panel
				}
			}	else if(nodeToAddEdge != null){
				moveViewOnMousesDrag(e.getPoint());
				targetNodeToAddEdge = getFirstNodeAtPosition(e.getX(), e.getY());
				// the drawing of the line is done while redrawing
				repaint();
			} else if(zoomRect != null){
				zoomRect.width = e.getX() - zoomRect.x;
				zoomRect.height = e.getY() - zoomRect.y;
				// currently, it's only allowed to select the region from top left to bottom right :-(
//				if(zoomRect.width < 0){ zoomRect.width = 0; }
//				if(zoomRect.height < 0){ zoomRect.height = 0; }
				repaint();
			} else if(shiftStartPoint != null) {
				pt.moveView(e.getX() - shiftStartPoint.x, e.getY() - shiftStartPoint.y);
				shiftStartPoint = e.getPoint();
				parent.redrawGUI(); // we need to redraw the graph - the view has changed
			} else if(rotateStartPoint != null) {
				if(pt instanceof Transformation3D) {
					Transformation3D t3d = (Transformation3D) pt;
					t3d.rotate(e.getX() - rotateStartPoint.x, 
					           e.getY() - rotateStartPoint.y, 
					           !e.isControlDown(), false); // read keyboard - ctrl allows to freely rotate
					rotateStartPoint = e.getPoint();
					parent.redrawGUI(); // need to redraw the graph - the view has changed
				}
			}
			Global.log.logln(LogL.GUI_ULTRA_DETAIL, "Mouse Dragged finished");
		}

		/* (non-Javadoc)
		 * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
		 */
		public void mouseMoved(MouseEvent e) {
			currentCursorPosition.setLocation(e.getX(), e.getY());
			if(pt.supportReverseTranslation()) {
				pt.translateToLogicPosition(e.getX(), e.getY());
				if((pt.logicX < Configuration.dimX)&&
						(pt.logicX > 0)&&
						(pt.logicY < Configuration.dimY)&&
						(pt.logicY > 0)){
					parent.setMousePosition(pt.getLogicPositionString());
				}
			} 
			
			if(userSelectsNodeMode) {
				userSelectsNodeCurrentFocus = getFirstNodeAtPosition(e.getX(), e.getY());
				repaint(); // async call that does not repaint the network graph, but only the stuff on top of the graph
			} else {
				userSelectsNodeCurrentFocus = null;
			}
			if(toolTipDrawCoordCube != null) {
				toolTipDrawCoordCube = null;
				repaint();
			}
		}
		
		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
		 */
		public void mouseEntered(MouseEvent e){}
		/* (non-Javadoc)
		 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
		 */
		public void mouseExited(MouseEvent e){
		}

		/* (non-Javadoc)
		 * @see java.awt.event.MouseWheelListener#mouseWheelMoved(java.awt.event.MouseWheelEvent)
		 */
		public void mouseWheelMoved(MouseWheelEvent e) {
			//block zooming while a simulation is running
			if(Global.isRunning){
				return;
			}
			int clicks = e.getWheelRotation();
			if(clicks < 0) {
				parent.zoom(Configuration.wheelZoomStep); // zoom In  
			}	else {
				parent.zoom(1.0 / Configuration.wheelZoomStep); // zoom out
			}
		}
	} // END OF CLASS MyMouseListener

	class MyKeyListener implements KeyListener {
		public void keyPressed(KeyEvent e) {
			// react to pressing escape
			if(e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
				if(nodeToDrag != null) { // stop dragging a node, and undo
					nodeToDrag.getPosition().assign(nodeToDragInitialPosition);
					nodeToDragDrawCoordCube = null;
					nodeToDrag = null;
					parent.redrawGUI(); // node position has changed, full repaint 
				}
				if(nodeToAddEdge != null) {
					nodeToAddEdge = null;
					targetNodeToAddEdge = null;
					repaint(); // no need to redraw whole gui, just repaint layer
				}
				if(zoomRect != null) {
					zoomRect = null;
					repaint();
				}
			}
		}
		public void keyReleased(KeyEvent e) {
		}
		public void keyTyped(KeyEvent e) {
		}
	}
	
	/**
	 *  Compontent Listener implementation
	 */
	class MyComponentListener implements ComponentListener {
		public void componentResized(ComponentEvent e) {
			getNewOffscreen();
			// don't force a redraw, s.t. if the resize happens during a simulation, the
			// graph is not repainted. (but this may leave an empty graph if a simulation is running)
			// We could disallow to resize the window during simulation, but then, the window flickers
			parent.redrawGUI(); 
		}

		public void componentMoved(ComponentEvent e) {
		}

		public void componentShown(ComponentEvent e) {
		}

		public void componentHidden(ComponentEvent e) {
		}
	} // END OF CLASS MyComponentListener
}