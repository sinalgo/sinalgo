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
package sinalgo.nodes.edges;



import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Line2D;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import sinalgo.configuration.Configuration;
import sinalgo.configuration.CorruptConfigurationEntryException;
import sinalgo.gui.helper.Arrow;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.io.eps.EPSOutputPrintStream;
import sinalgo.nodes.Node;
import sinalgo.nodes.Position;
import sinalgo.nodes.messages.Message;
import sinalgo.runtime.Global;
import sinalgo.runtime.Main;
import sinalgo.runtime.Runtime;
import sinalgo.tools.logging.Logging;
import sinalgo.tools.storage.DoublyLinkedListEntry;



/**
 * The default impelementation of a network edge, which connects a node to another node.
 * <p> 
 * All edges of this framework are unidirectional. The bidirectional edges just ensure
 * that there is an edge object in both directions.  
 */
public class Edge implements DoublyLinkedListEntry{
	
	/**
	 * @return The unique ID of this edge.
	 */
	public long getID() {
		return ID;
	}
	
	/**
	 * The start node of the edge. Edges in this simulation are directed.
	 */
	public Node startNode;

	/**
	 * The end node of the edge. Edges in this simulation are directed.
	 */
	public Node endNode;
	
	/**
	 * Initializes the edge. This method can be overridden by subclasses to perform
	 * some edge specific initialization, that is called whenever a new edge is needed.
	 * <p>
	 * The start- and end-nodes, the ID as well as the oppositeEdge fields are already 
	 * set when this method is called.
	 */
	public void initializeEdge() {
	}
	
	/**
	 * This is the oposite method to the initializeEdge method. It is called whenever an Edge is removed 
	 * from the graph. When impelementing new Edges this method can be overridden to clean up when the 
	 * edge is removed from the graph. The BidirectionalEdge for example could remove its related edge from 
	 * the system.
	 * <p>
	 * This method is called after this edge has been removed from the list of outgoing connections
	 * of its start node.
	 * <p>
	 * This method must NOT free() the edge.
	 */
	public void cleanUp(){
	}
	
	/**
	 * @return The number of messages that are currently being sent over this edge.
	 */
	public int getNumberOfMessagesOnThisEdge() {
		return numberOfMessagesOnThisEdge;
	}
	
	/**
	 * @return The edge that connects the two end nodes of this edge in the other 
	 * direction. Null if there is no such edge.
	 */
	public Edge getOppositeEdge() {
		return oppositeEdge;
	}
	
	/**
	 * Test equality of this edge with another edge. They are considered equal
	 * if they connect the same pair of nodes in the same direciton.
	 *
	 * @param e The edge to compare to
	 * @return True if the edges are equal, otherwise false. 
	 */
	public boolean equals(Edge e){
		return ((this.startNode.ID == e.startNode.ID)&&(this.endNode.ID == e.endNode.ID));
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		// the default implementation returns the name of this edge-class
		return "Type: " + Global.toShortName(this.getClass().getName());
	}
	

	
	
	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------
	// Drawing this edge
	// => Overwrite these methods to change the appearance of edges of this class.
	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------
	
	public static Color defaultEdgeColorPassive = Color.BLACK;
	public static Color defaultEdgeColorActive = Color.RED;
	{
		try {
			String s = Configuration.getStringParameter("Edge/PassiveColor");
			defaultEdgeColorPassive = Color.decode(s);
		} catch(CorruptConfigurationEntryException e) { // there is no config entry -> take default
		} catch(NumberFormatException e) {
			Main.fatalError("Invalid color specification for the configuration entry Edge/PassiveColor. Expected a hexadecimal number of the form 0xrrggbb");
		}
		try {
			String s = Configuration.getStringParameter("Edge/ActiveColor");
			defaultEdgeColorActive = Color.decode(s);
		} catch(CorruptConfigurationEntryException e) { // there is no config entry -> take default
		} catch(NumberFormatException e) {
			Main.fatalError("Invalid color specification for the configuration entry Edge/ActiveColor. Expected a hexadecimal number of the form 0xrrggbb");
		}
	}
	
	
	/**
	 * The default color of the edge, to be used when no message is sent over this edge.
	 */
	public Color defaultColor = defaultEdgeColorPassive;
		
	/**
	 * The color of this edge to be used when at least one message is sent over this edge.
	 */
	private Color sendingColor = defaultEdgeColorActive;
	
	/**
	 * Returns the edge color to be used to draw this edge. 
	 * 
	 * This default implementation returns the sendingColor if at least one message 
	 * is sent over this edge, otherwise the defaultColor.
	 * @return The edge color to be used.
	 */
	public Color getColor() {
		if(this.numberOfMessagesOnThisEdge > 0){
			return sendingColor;
		} else {
			return defaultColor;
		}
	}
	
	/**
	 * The default implementation to draw this edge on the GUI.
	 * You may overwrite this method to obtain a different look. 
	 * @param g The graphics object to draw the edge to. 
	 * @param pt The current transformation object.
	 */
	public void draw(Graphics g, PositionTransformation pt) {
		Position p1 = startNode.getPosition();
		pt.translateToGUIPosition(p1);
		int fromX = pt.guiX, fromY = pt.guiY; // temporarily store
		Position p2 = endNode.getPosition();
		pt.translateToGUIPosition(p2);
		
		if((this.numberOfMessagesOnThisEdge == 0)&&
				(this.oppositeEdge != null)&&
				(this.oppositeEdge.numberOfMessagesOnThisEdge > 0)){
			// only draws the arrowHead (if drawArrows is true)
			Arrow.drawArrowHead(fromX, fromY, pt.guiX, pt.guiY, g, pt, getColor());
		} else {
			Arrow.drawArrow(fromX, fromY, pt.guiX, pt.guiY, g, pt, getColor());
		}
	}
	
	/**
	 * Draw this edge in MetaPost.
	 * @param pw The PostScript stream to write the commands for this edge to
	 * @param pt Transformation object to obtain GUI coordinates of the endpoints of this edge.
	 */
	public void drawToPostScript(EPSOutputPrintStream pw, PositionTransformation pt) {
		pt.translateToGUIPosition(startNode.getPosition());
		double eSX = pt.guiXDouble;
		double eSY = pt.guiYDouble;
		pt.translateToGUIPosition(endNode.getPosition());
		Color c = getColor();
		pw.setColor(c.getRed(), c.getGreen(), c.getBlue());
		pw.setLineWidth(0.5);
		
		if(Configuration.drawArrows){
			pw.drawArrow(eSX, eSY, pt.guiXDouble, pt.guiYDouble);
		}
		else{
			pw.drawLine(eSX, eSY, pt.guiXDouble, pt.guiYDouble);
		}
	}
	

	
	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------
	// Framework specific methods and member variables
	// => You should not need to modify/overwrite/call/use any of these members or methods
	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------

	private long ID = 0; // The (unique) id of this edge. 

	/**
	 * A reference to the edge connecting the two end-nodes of this edge in the opposite direction.
	 * This member may be null if the opposite edge does not exist.
	 * <p>
	 * This opposite edge is mainly used for drawing the edges properly, s.t. an inactive edge does
	 * not overpaint an active edge. 
	 */
	public Edge oppositeEdge = null;
	
	/**
	 * <b>This member is framework internal and should not be used by the project developer.</b> 
	 * The number of messages that are currently sent on this edge. If its value is 0, there are no 
	 * messages sent on this edge and it is drawn with the default color. Otherwise the edge is 
	 * drawn with the sending color. Note that this number has to be set upon resceiving a mesage.
	 */
	public int numberOfMessagesOnThisEdge = 0; 
	
	/**
	 * Called by the framework whenever a message is sent over this edge. 
	 * This edge may react to the message by drawing itself differently.
	 * <p>
	 * In the default implementation, the edge just increments a counter to keep
	 * track of the number of messages sent over this edge at any time. 
	 * @param msg The message that is being sent over this edge.
	 */
	public void addMessageForThisEdge(Message msg) {
		numberOfMessagesOnThisEdge++;
	}
	
	/**
	 * Called by the framework whenever a message is not anymore being 
	 * sent over this edge.
	 * <p> 
	 * In the default implementation, the edge just decrements a counter to keep
	 * track of the number of messages sent over this edge at any time. 
	 * @param msg The message that was being sent over this message. 
	 */
	public void removeMessageForThisEdge(Message msg) {
		numberOfMessagesOnThisEdge--;
	}

	/**
	 * <b>This member is framework internal and should not be used by the project developer.</b> 
	 * A boolean used to indicate, wether the edge has been validated in this round.
	 * so after having validated all the edges all dead links remain false. 
	 */
	public boolean valid = false;
	
	/**
	 * <b>This member is framework internal and should not be used by the project developer.</b> 
	 * Determines and sets the edge that connects the two end nodes of this 
	 * edge in the other direction.
	 * <p>
	 * If there is no such edge, the field otherEdge is set to null.  
	 */
	protected final void findOppositeEdge() {
		Iterator<Edge> edgeIter = endNode.outgoingConnections.iterator();
		while(edgeIter.hasNext()){
			Edge e = edgeIter.next();
			if((e.startNode.ID == endNode.ID)&&(e.endNode.ID == startNode.ID)){
				this.oppositeEdge = e;
				e.oppositeEdge = this;
				return;
			}
		}
		this.oppositeEdge = null; // no other edge found 
	}

	/**
	 * <b>This member is framework internal and should not be used by the project developer.</b> 
	 * Tests whether this edge covers covers a certain position on the screen.
	 * The provided point (x,y) indicates a point on the screen, and first needs
	 * to be transformed to the logical coordinates used by the simulation.  
	 * 
	 * <br> This method is used by the GUI to determine whether the mouse points 
	 * onto this edge.
	 *
	 * @param xCoord The x coordinate of the position on the screen
	 * @param yCoord The y coordinate of the position on the screen
	 * @param pt The transformation object used to translate between logic and gui coordinates.
	 * @return True if this edge covers the given position, otherwise false. 
	 */
	public boolean isInside(int xCoord, int yCoord, PositionTransformation pt){
		Position p1 = startNode.getPosition();
		pt.translateToGUIPosition(p1);
		int fromX = pt.guiX, fromY = pt.guiY; // temporarily store
		Position p2 = endNode.getPosition();
		pt.translateToGUIPosition(p2);
		double dist = Line2D.ptSegDist(fromX, fromY, pt.guiX, pt.guiY, xCoord, yCoord);
		return dist < 3;
	}
	
	/**
	 * This is a stack containig all the unused packet instances. To reduce the garbage collection time,
	 * used Packets are not destroyed but are added to a Packet pool. When a new instance is requested, 
	 * the system only creates a new instance, when the stack is empty.
	 */
	private static EdgePool freeEdges = new EdgePool();
	
	/**
	 * The ID that is given to the next edge that is returned by the fabricateEdge-method. Is increased 
	 * so that no two edges have the same id.
	 */
	private static long nextId = 1;

	private static Constructor<?> constructor = null;
	private static String nameOfSearchedEdge = "";
	
	public static int numEdgesOnTheFly = 0;

	/**
	 * <b>This member is framework internal and should not be used by the project developer.</b> 
	 * This method creates a Edge of the type specified in the XML configuraiton file in the framework field 
	 * <code>edgeType</code>. In normal use there is only one type of 
	 * edges in the whole simulation. 
	 * <br>
	 * Alternatively, you can also use the concrete constructor of a Edge class, but if
	 * you want to use the default class and do not want to care about the class and the reflection use
	 * this method.
	 *
	 * @param from The startNode for the edge.
	 * @param to The endNode for the edge.
	 * @return An Edge form the given startNode to the given endNode.
	 */
	public final static Edge fabricateEdge(Node from, Node to){
		Edge edge = freeEdges.get();
		// TODO: this is expensive!
		if(edge != null) { // we can recycle an edge
			if(edge.startNode != null || edge.endNode != null) { // sanity check
				Main.fatalError(Logging.getCodePosition() + " Edge factory failed! About to return an edge that was already returned. (Probably, free() was called > 1 on this edge.)");
			}
		} else try { // need to construct a new edge
			if(Configuration.hasEdgeTypeChanged() || constructor == null) { // 
				constructor = null;
				nameOfSearchedEdge = Configuration.getEdgeType();
				Class<?> edgeClass = Class.forName(nameOfSearchedEdge);
				Constructor<?>[] list = edgeClass.getDeclaredConstructors();
				// Test all constructors of the edge-class whether it has 
				// a corresponding constructor.
				for(Constructor<?> c : list) {
					Class<?>[] paramClasses = c.getParameterTypes();
					if(paramClasses.length != 0) {
						continue; // need a constructor that takes 2 params
					}	else {
						constructor = c;
						break;
					}
				}
				if(constructor == null) {
					throw new NoSuchMethodException("Did not find a valid constructor for the "+nameOfSearchedEdge+" class.");
				}
				Configuration.setEdgeTypeChanged(false);
			}
			edge = (Edge) constructor.newInstance();
		}	catch(ClassNotFoundException cNFE){
			Main.fatalError("The implementation of the edge '" + nameOfSearchedEdge + "' could not be found.\n" +
			                "Change the Type in the XML-File or implement it." + "");
		} catch (IllegalArgumentException e) {
			Main.fatalError("Exception caught while creating edge '" + nameOfSearchedEdge + "'.\n" + e);
		} catch (InstantiationException e) {
			Main.fatalError("Exception caught while creating edge '" + nameOfSearchedEdge + "'.\n" + e);
		} catch (IllegalAccessException e) {
			Main.fatalError("Exception caught while creating edge '" + nameOfSearchedEdge + "'.\n" + e);
		} catch (InvocationTargetException e) {
			Main.fatalError("Exception caught while creating edge '" + nameOfSearchedEdge + "'.\n" + e.getCause());
		} catch (SecurityException e) {
			Main.fatalError("Exception caught while creating edge '" + nameOfSearchedEdge + "'.\n" + e);
		} catch (NoSuchMethodException e) {
			Main.fatalError("Cannot instanciate an edge of type '" + nameOfSearchedEdge + 
			                "' for two nodes of type \n(" + from.getClass().getName() + ", " + 
			                to.getClass().getName() + ").\n" +
			                "To select a different edge type, change the config.xml file\n" +
			                "or use the settings dialog in the GUI."
			);
		}

		// initialize the edge
		edge.startNode = from;
		edge.endNode = to;
		edge.oppositeEdge = null;

		edge.sendingColor = defaultEdgeColorActive;
		edge.defaultColor = defaultEdgeColorPassive;
		edge.valid = false;
		
		edge.numberOfMessagesOnThisEdge = 0;
		edge.ID = getNextFreeID();
		
		edge.findOppositeEdge(); // if there is an edge in the opposite direction, set the oppositeEdge field
		edge.initializeEdge(); // Finally, call a custom initialization method
		numEdgesOnTheFly ++;
		return edge;
	}
		
	/**
	 * <b>This member is framework internal and should not be used by the project developer.</b> 
	 * Method called to remove this edge from the graph and drop messages sent over this edge.
	 * This method does not remove the edge from the outgoingConnections list of the host-node, 
	 * and it does NOT free the edge. But it calls the cleanUp() method. 
	 */
	public final void removeEdgeFromGraph(){
		if(Configuration.asynchronousMode) {
			// This is quite slow as it iterates over all pending events. However,
			// synchronous simulations are not mobile, therefore this method is not called often.
			Runtime.eventQueue.invalidatePacketEventsForThisEdge(this);
		} else {
			this.endNode.getInboxPacketBuffer().invalidatePacketsSentOverThisEdge(this);
		}
		this.cleanUp();
	}

	/**
	 * <b>This member is framework internal and should not be used by the project developer.</b> 
	 * Returns this edge to the edge fractory for recycling. Call 
	 * this method after you have removed this edge from the list of
	 * outgoing edges of a node and have no further need for this object.
	 * <p>
	 * After calling this method, there should be no references left pointing 
	 * to this edge.  
	 * <p>
	 * This method removes the link from the (potential) edge that connects the
	 * same nodes in the other direction. All other linkage added by user implementations
	 * must be removed by the user by overriding this method accordingly.  
	 */
	public final void free() {
		// reset the linkage between this edge and the edge in the other direction, if it exists 
		if(oppositeEdge != null) {
			if(oppositeEdge.oppositeEdge == this) {
				oppositeEdge.oppositeEdge = null;
			}
			oppositeEdge = null;
		}
		this.startNode = null;
		this.endNode = null;
		this.defaultColor = null;
		this.sendingColor = null;
		this.oppositeEdge = null;
		numEdgesOnTheFly --;
		freeEdges.add(this);
	}
	
	// the DLLE entry for the DoublyLinkedList
	private DLLFingerList dllFingerList = new DLLFingerList();

	/**
	 * <b>This member is framework internal and should not be used by the project developer.</b>  
	 * @see sinalgo.tools.storage.DoublyLinkedListEntry#getDoublyLinkedListFinger()
	 */
	public DLLFingerList getDoublyLinkedListFinger() {
		return dllFingerList;
	}
	
	/**
	 * <b>This member is framework internal and should not be used by the project developer.</b> 
	 * @return The next ID to be used for an edge. 
	 */
	private static long getNextFreeID(){
		if(nextId == 0){
			Main.fatalError("The Edge ID counter overflowed.");
		}
		return Edge.nextId++;//implicit post-increment
	}
}
