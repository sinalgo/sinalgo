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
package sinalgo.nodes;



import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Polygon;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

import sinalgo.configuration.Configuration;
import sinalgo.configuration.CorruptConfigurationEntryException;
import sinalgo.configuration.WrongConfigurationException;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.io.eps.EPSOutputPrintStream;
import sinalgo.models.ConnectivityModel;
import sinalgo.models.InterferenceModel;
import sinalgo.models.MobilityModel;
import sinalgo.models.Model;
import sinalgo.models.ReliabilityModel;
import sinalgo.nodes.edges.Edge;
import sinalgo.nodes.messages.Inbox;
import sinalgo.nodes.messages.Message;
import sinalgo.nodes.messages.NackBox;
import sinalgo.nodes.messages.Packet;
import sinalgo.nodes.messages.PacketCollection;
import sinalgo.nodes.messages.Packet.PacketType;
import sinalgo.nodes.timers.Timer;
import sinalgo.runtime.GUIRuntime;
import sinalgo.runtime.Global;
import sinalgo.runtime.Main;
import sinalgo.runtime.NotInGUIModeException;
import sinalgo.runtime.Runtime;
import sinalgo.runtime.events.PacketEvent;
import sinalgo.runtime.nodeCollection.NodeCollectionInfoInterface;
import sinalgo.tools.logging.LogL;
import sinalgo.tools.storage.DoublyLinkedListEntry;
import sinalgo.tools.storage.ReusableListIterator;
import sinalgo.tools.storage.SortableVector;


/**
 * The base class for all node implementations. 
 */
public abstract class Node implements DoublyLinkedListEntry{ 
	
	/**
	 * This annotation is used to mark methods that should be accessible
	 * through a popup menu that pops up when the user right-clicks on this node.
	 * <p>
	 * Note: As there is no user-interaction pre-implemented, the method to which
	 * this annotation is attached must be parameter-less.  
	 */
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface NodePopupMethod {
		/** The text shown in the popup menu */
		String menuText();
	}
	
	/**
	 * Every time the user right-clicks on a node to obtain the popup-menu
	 * for the node, the menu is assembled and includes methods annotated
	 * with the {@link NodePopupMethod} annotation. Before including such a
	 * method in the list, this method is called, to allow the project
	 * to decide at runtime whether the method should be included or not, and,
	 * if necessary, change the default menu text.  
	 * @param m The method of this node, annotated with {@link NodePopupMethod}
	 * @param defaultText The default menu text specified in the annotation.
	 * @return The text to be displayed for the given method in the popup menu, 
	 * <code>null</code> if the method should not be included in the popup menu.
	 */
	public String includeMethodInPopupMenu(Method m, String defaultText) {
		return defaultText; // The default implementation uses the default text.
		// Do NOT modify this code. To obtain a different behavior, overwrite
		// this method in your Node implementation. 
	}
	
	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------
	//The following methods need to be implemented by the subclass.
	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------
	
	/**
	 * This method is invoked after all the Messages are received. Overwrite it to specify what to do 
	 * with incoming messages.
	 * @param inbox a instance of a iterator-like class Inbox. It is used to traverse the incoming
	 * packets and to get information about them. 
	 * @see Node#step() for the order of calling the methods.
	 */
	public abstract void handleMessages(Inbox inbox);
	
	/**
	 * Handle all messages sent by this node that were scheduled to arrive
	 * in the previous round, but were dropped.
	 * Overwrite this method in your subclass if you wisth to perform
	 * an action upon dropped messages.
	 * <p>  
	 * The framework calls (and stores the dropped messages) only if 
	 * <code>generateNAckMessages</code> is enabled in the project configuration 
	 * file.
	 * @param nackBox The NackBox, an iterator-like object that contains the set of dropped messages.
	 */
	public void handleNAckMessages(NackBox nackBox) {
		// no code here! The user may overwrite this method in the subclass
	}

	/**
	 * This method is invoked at the beginning of each step. 
	 * Add actions to this method that this node should perform in every step.
	 * @see Node#step() for the calling sequence of the node methods.
	 */
	public abstract void preStep();
	
	
	/**
	 * This method is called exactly once upon creation of this node
	 * and allows the subclasses to perform some node-specific initialization.
	 * <p>
	 * When a set of nodes is generated, this method may be called before all nodes
	 * are added to the framework. Therefore, this method should not depend on other
	 * nodes of the framework.
	 */
	public abstract void init();
	
	
	/**
	 * At the beginning of each round, the framework moves all nodes according to their mobility model.
	 * Then, it iterates over all nodes to update the connections, according to the nodes connectivity model.
	 * <p>
	 * This method is called in the step of this node if the set of outgoing connections had changed in 
	 * this round. I.e. a new edge was added or an edge was removed.
	 * <p>
	 * As a result, this method is called nearly always in the very first round, when the network graph
	 * is determined for the first time.   
	 */
	public abstract void neighborhoodChange();
	
	
	/**
	 * The node calls this method at the end of its step. 
	 */
	public abstract void postStep(); 

	/**
	 * Returns a string describing this node.
	 * <p>
	 * The toString() method is used in several places of the GUI to describe this node:<ul>
	 * <li>The tool-tip text that shows when the mouse hovers over a node</li>
	 * <li>The node information dialog also contains this string</li>
	 * </li>
	 * The GUI does not wrap the text you return. Therefore, you may format the string
	 * manually by adding line-break characters '\n' as needed.  
	 * @return A String representing this node
	 */
	public String toString() {
		return "Node(ID="+this.ID+")"; 
	}
	
	/**
	 * This method checks if the configuration meets the specification of the node. This 
	 * function is called exactly once just after the initialisazion of a node but before 
	 * the first usage.
	 * @throws WrongConfigurationException if the requirements are not met. 
	 */
	public abstract void checkRequirements() throws WrongConfigurationException;
	
	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------
	// End of methods that need to be implemented by the subclass
	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------
	
	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------
	// Diverse node settings / members
	// => may be used by subclasses
	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------
	
	/**
	 * Returns the collection of timers currently active at this node.
	 * This collection only holds the timers in synchronous simulation mode.
	 * In asynchronous simulation mode, the timers are stored as events in
	 * the global event queue. 
	 * @return The collection of timers currently active at this node.
	 */
	public TimerCollection getTimers() {
		return timers;
	}
	
	/**
	 * The inbox of this node.
	 * <p>
	 * In every round, the method performs its step() method. At the beginning of the step method, it 
	 * fills this inbox with the messages that arrive in this round. At the end of the round, it 
	 * empties this inbox.
	 * <p>
	 * This inbox instance lets you iterate (several times) over all messages received in this round
	 * and retrive information about each message. 
	 */
	protected Inbox inbox = null;
	
	/**
	 * The nackBox that contains the packets that did not 
	 * arrive in the previous round (in synchronous mode).
	 * <p>
	 * If a message sent by this node is dropped, this node is informed
	 * the round after the message should have arrived. In asynchrone 
	 * mode, this node is informed at the time the message would arrive 
	 * at its destination.
	 * <p>
	 * This feature needs to be enabled in the project configuration:
	 * set <code>generateNAckMessages</code> to true. If a sender node
	 * needs not to be informed about dropped messages, you should turn off
	 * this feature to save computing power. 
	 */
	protected NackBox nackBox = null;  

	/**
	 * The ID of the node. The system requires two nodes not to have the same ID. This is done by the 
	 * NodeFactory which creates the nodes and automatically assigns them an ID.
	 * @see Node#createNodeByClassname(String)
	 */
	public int ID;

	/**
	 * The collection of all outgoing Links.
	 */
	public Connections outgoingConnections = new NodeOutgoingConnectionsList(true);

	/**
	 * Adds a (unidirectional) connection from this node to another node.
	 * <p>
	 * Unless the default edge type is bidirectional, this only adds a 
	 * unidirectional edge from this node to the target node. If the default
	 * edge type is bidirectional, the edge itself will ensure that the
	 * connection is established in both directions.
	 * <p>
	 * In synchrounous mode, the added edge will only persist if the connectivity 
	 * model handles the edge accordingly. 
	 * @param n The node to which a connection should be established.
	 */
	public void addConnectionTo(Node n) {
		outgoingConnections.add(this, n, false);
	}

	/**
	 * Adds a bidirectional connection from this node to another node.
	 * This method adds an edge from this node to the target node
	 * and vice versa. If the default edge type is bidirectional, 
	 * this method does no harm, but a call to {@link #addConnectionTo(Node)} 
	 * would be equivalent.
	 * <p>
	 * In synchrounous mode, the added edge will only persist if the connectivity 
	 * model handles the edge accordingly. 
 	 * @param n The node to which a connection should be established.
	 */
	public void addBidirectionalConnectionTo(Node n) {
		outgoingConnections.add(this, n, false);
		n.outgoingConnections.add(n, this, false); // BUG FIX 8 April 2008
	}

	
	/**
	 * Sets the position of this node.
	 * @param p The new position.
	 */
	public final void setPosition(Position p){
		setPosition(p.xCoord, p.yCoord, p.zCoord);
	}
	
	/**
	 * Sets the position of this node.
	 * @param x The new x-coordinate of this node
	 * @param y The new y-coordinate of this node
	 * @param z The new z-coordinate of this node
	 */
	public final void setPosition(double x, double y, double z) {
		position.xCoord = x;
		position.yCoord = y;
		position.zCoord = z;
		cropPos(position);
		Runtime.nodes.updateNodeCollection(this); // note that this method tests whether the node is already added to the node collection
		nodePositionUpdated();
	}
	
	/**
	 * This method is called by the framework whenever
	 * this node is assigned a new position. Overwrite
	 * this method in the subclass to immediately react 
	 * to position changes.  
	 */
	protected void nodePositionUpdated() {
	}
	
	/**
	 * Returns the position of the node.
	 * @return The position of the node.
	 */
	public final Position getPosition(){ 
		return position; 
	}

	/**
	 * @return true if the two objects are nodes and their ID is equal.
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		if(o instanceof Node) {
			Node n = (Node) o;
			return n.ID == this.ID;
		}
		return false;
	}
	
	/**
	 * Comparison of this node to another node
	 * @param n The other node to test for equality
	 * @return True if the other node has the same ID as this node.
	 */
	public boolean equals(Node n) {
		if(n == null) {
			return false;
		}
		return n.ID == this.ID;
	}
	
	
	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------
	// Send / broadcast messages
	// => may be used by subclasses
	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------
	
	/**
	 * This method sets the intensity of radio module for this node. The intensity lies between 0.0 
	 * and 1.0. If i is a negative number it is set to 0.0 and if it is greater than 1.0 it is set
	 * to 1.0. 
	 * <br>
	 * Note, that 1.0 is the default value for the intensity. If you want to send a packet
	 * with a different intensity, change the default intensity of the sender, or call the send
	 * method with the intensity as additional parameter.
	 *
	 * @param i The intensity with which the radio module will send further packets.
	 * @see Node#send(Message, Node, double) 
	 */
	public final void setRadioIntensity(double i){ 
		if(i < 0){
			i = 0;
		}	else {
			if(i > 1.0){
				i = 1.0;
			}
		}
		intensity = i;
	}
	
	/**
	 * This method returns the intensity of the radio module of this node.
	 *
	 * @return The intensity of the radio module. 
	 */
	public final double getRadioIntensity(){
		return intensity;
	}

	/**
	 * This method sends a Message to a specified target with the given intensity. 
	 * @param m The Message to send.
	 * @param target The ID of the target node.
	 * @param intensity The intensity to send the message with.
	 * @throws NoConnectionException Thrown when there exists no connection to the specified target. 
	 */
	public final void send(Message m, Node target, double intensity) {
		Edge connection = null;
		//check, if a connection to the destination node exists
		edgeIteratorInstance.reset();
		while( edgeIteratorInstance.hasNext()){
			Edge edge = edgeIteratorInstance.next();
			if(edge.endNode.equals(target)){
				connection = edge;
				break;
			}
		}
		// If there is no edge, the message is marked to not arrive immediately in the sendMessage() method
		Packet sentP = sendMessage(m, connection, this, target, intensity);
		if(Configuration.interference){ //only add the message in the packetsInTheAirBuffer, if interference is turned on
			Runtime.packetsInTheAir.add(sentP);
		}	
	}
	
	/**
	 * This method sends a Message to a specified target node. Note that this message is more efficient concerning 
	 * the memory usage than the sendPacket message, but note also that with this method the user sends a message and receives a packet 
	 * (as there are always packets received). If that disturbes you use the sendPacket method instead.
	 *
	 * @param m The Message to send.
	 * @param target The target node.
	 * @throws NoConnectionException Thrown when there exists no connection to the specified target. 
	 */
	public final void send(Message m, Node target) {
		send(m, target, this.intensity);
	}
	
	/**
	 * Directly sends a message to another node of the framework. 
	 * <ul>
	 * <li>This send message does not require a link between the sender and the receiver.</li> 
	 * <li>Does not create interference, and cannot be dropped due to interference.</li>
	 * <li>Is not dropped by the reliability model</li>
	 * <li>However, the delivery time depends on the messageTransmissionModel.</li>
	 * </ul>
	 * This send method may be interesting to implement P2P situations. 
	 * @param msg The message to send
	 * @param target The destination node of the message
	 */
	public final void sendDirect(Message msg, Node target) {
		Message clonedMsg = msg.clone(); // send a copy of the message
		if(clonedMsg == null) {
			Main.fatalError("The clone() method of '" + msg.getClass().getName() + "' returns null \n" + "instead of a copy of the message.");
		}
		Packet packet = Packet.fabricatePacket(clonedMsg);
		double transmissionTime = Global.messageTransmissionModel.timeToReach(this, target, msg);
		
		// fill in the data of the header
		packet.arrivingTime = Global.currentTime + transmissionTime;
		packet.sendingTime = Global.currentTime;
		packet.origin = this;
		packet.destination = target;
		packet.edge = null;
		packet.intensity = intensity;
		packet.positiveDelivery = true; // no disturbtion
		packet.type = PacketType.UNICAST;

		Global.numberOfMessagesInThisRound++; // statistics
		
		if(Global.isAsynchronousMode) {
			// add a packet event to the event list
			Runtime.eventQueue.insert(PacketEvent.getNewPacketEvent(packet, Global.currentTime + transmissionTime));			
		} else { // Synchronous
			//check whether the simulation is currently running or not.
			if(!Global.isRunning){
				//The simulation is not running and the send is called. The node is not allowed to 
				//send messages outside of their simulation cycle due to synchronisazion issues. Instead
				//of calling the send-method directly please use a timer.
				Main.fatalError("The node "+this.ID+" tried to send a message outside of its simulation " +
				                "cycle. Due to synchroniazion issues, this is not allowed.\n" +
				                "This problem probably came up due to a call from a nodes popup method.\n" +
				                "Do not directly call the send-method but start a timer\n" + "so that the node sends during its simulation cycle.");
				//this will never happen because the fatal error will kill the application.
			}
			// place the packet in the destination's receive buffer
			target.packetBuffer.addPacket(packet); // place the packet in the targets receive buffer
		}			
		//There is no interference created by this message - never add it to the list of 'packetsInTheAir'
	}
	
	/**
	 * This method broadcasts a Message. Note that this message is more efficient concerning the memory usage than the sendPacket message, but note
	 * also that with this method the user sends a message and receives a packet (as there are always packets received). If that disturbes you use 
	 * the broadcastPacket method instead.
	 * 
	 * @param m The message to be sent to all the neighbors.
	 */	
	public final void broadcast(Message m){
		broadcastMessage(m, this.intensity);
	}
	
	/**
	 * This method broadcasts a Message with a given intensity. Note that this message is more efficient concerning the 
	 * memory usage than the sendPacket message, but note also that with this method the user sends a message and receives 
	 * a packet (as there are always packets received). If that disturbes you use the broadcastPacket method instead.
	 * 
	 * @param m The message to be sent to all the neighbors.
	 * @param intensity The intensity to send the messages with.
	 */	
	public final void broadcast(Message m, double intensity){
		broadcastMessage(m, intensity);
	}
	
	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------
	// The models used by this node, setters and getters
	// => may be used by subclasses
	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------

	/**
	 * The connectivity model of this node.
	 */
	protected ConnectivityModel connectivityModel;

	/**
	 * The reliability model of this node. 
	 */
	protected ReliabilityModel reliabilityModel;

	/**
	 * The interference model of this node. 
	 */
	protected InterferenceModel interferenceModel;
	
	/**
	 * The mobility model of this node. 
	 */
	protected MobilityModel mobilityModel;
	
	/**
	 * Sets the ConnectivityModel for this node.
	 * @param cM The new ConnectivityModel.
	 */
	public final void setConnectivityModel(ConnectivityModel cM){ connectivityModel = cM; }

	/**
	 * Returns the ConnectivityModel currently used by this node.
	 * @return The ConnectivityModel currently used by this node.
	 */
	public final ConnectivityModel getConnectivityModel(){ return connectivityModel; }
	
	/**
	 * Sets the ReliabilityModel for this node.
	 * @param rM The new ReliabilityModel.
	 */
	public final void setReliabilityModel(ReliabilityModel rM){ reliabilityModel = rM; }

	/**
	 * Returns the ReliabilityModel currently used by this node.
	 * @return The ReliabilityModel currently used by this node.
	 */
	public final ReliabilityModel getReliabilityModel(){ return reliabilityModel; }
	
	/**
	 * Sets the InterferenceModel for this node.
	 * @param iM The new InterferenceModel.
	 */
	public final void setInterferenceModel(InterferenceModel iM){ interferenceModel = iM; }

	/**
	 * This method returns the InterferenceModel currently used by this node.
	 * @return The InterferenceModel currently used by this node.
	 */
	public final InterferenceModel getInterferenceModel(){ return interferenceModel; }

	/**
	 * Sets the MobilityModel for this node.
	 * @param mM The new MobilityModel.
	 */
	public final void setMobilityModel(MobilityModel mM){ mobilityModel = mM; }

	/**
	 * Returns the MobilityModel currently used by this node.
	 * @return The MobilityModel currently used by this node.
	 */
	public final MobilityModel getMobilityModel(){ return mobilityModel; }


	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------
	// Drawing methods of this node (how it is drawn on the GUI and to EPS
	// => may be used by subclasses
	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------
	
	/**
	 * Enables / disables highlighting of this node.
	 * A highlighted node appears with a special color on the screen. The exact 
	 * behavior of highlighting is implemented in the draw method of this node. 
	 * @param highlighted Indicates whether to highlight this node or not.
	 */
	public final void highlight(boolean highlighted){
		try {
			GUIRuntime rt = Main.getGuiRuntime();
			rt.getGUI().getGraphPanel().setNodeHighlighted(this, highlighted);
		} catch (NotInGUIModeException e) {
			// the simulation is not in gui mode and thus the node is not set highlighted
			//do nothing
		}
	}

	/**
	 * Set the default color member 'nodeColor' of this node. This
	 * node uses this color by defaut if the method 'getColor()'
	 * is not overwritten in your node subclass. Overwrite
	 * getColor() in your subclass to implement more advanced
	 * coloring schemes.    
	 *
	 * @param c The new color.
	 */
	public void setColor(Color c){ 
		nodeColor = c; 
	}

	/**
	 * Determines the color in which this node is painted.
	 * <p>
	 * Either use setColor() to change the color of this node, 
	 * or overwrite this method in your node subclass to 
	 * further customize the color of this node.
	 * <p>
	 * Note that the framework always uses this method to 
	 * determine the color of this node. 
	 * @return The color of this node.
	 */
	public Color getColor() {
		return nodeColor; 
	}
	
	/**
	 * Contains the diameter (in pixels) of the node. Note that this
	 * varialbe stores the actual number of pixels and is not scaled due to the 
	 * zooming factor. This member should not be assigned a value. To change the size
	 * of a node, change the 'defaultDrwaingSizeInPixels' member.    
	 */
	protected int drawingSizeInPixels;
	
	/**
	 * The default size of this node (in pixels) when the zoom factor equals 1.
	 */
	protected int defaultDrawingSizeInPixels;
	
	/**
	 * Polygon instance used to draw a node as a route.
	 * This instance is used by all nodes - it's static to save memory
	 */
	protected static Polygon routePolygon = null; 
	
	/**
	 * Sets the size at which this node is drawn. The size is specified in pixels
	 * and denotes the size of this node when the zoom factor equals one. When
	 * the GUI shows a zoomed view of the network graph, the nodes are scaled accordingly.
	 * <p>
	 * To obtain the actual size (in pixels) at which this node was drawn during the 
	 * last draw method, use the member 'drawingSizeInPixels'.
	 * @param size The size in pixels of this node, when the zoom factor equals 1. 
	 */
	public void setDefaultDrawingSizeInPixels(int size) {
		defaultDrawingSizeInPixels = size;
	}
	
	/**
	 * This method draws this node to the specified Graphics. Each node is responsible 
	 * itself for its appearence in the gui.
	 * <br>
	 * You may overwrite this method in your subclass to implement a different behavior. 
	 * @param g The Graphics to draw the node to.
	 * @param pt The transformation object used to display the graph
	 * @param highlight If true, this node should be highlighted to be distinguished from ohters. This
	 * flag is used to distinguish a selected node in the GUI. 
	 */
	public void draw(Graphics g, PositionTransformation pt, boolean highlight){
		Color backupColor = g.getColor();
		drawingSizeInPixels = (int) (defaultDrawingSizeInPixels* pt.getZoomFactor()); // half the side-length in pixels of the square
		pt.translateToGUIPosition(position);
		int x = pt.guiX - (drawingSizeInPixels >> 1);
		int y = pt.guiY - (drawingSizeInPixels >> 1);
		Color color = getColor();
		if(highlight) {
			// a highlighted node is surrounded by a red square
			g.setColor(color == Color.RED ? Color.BLACK : Color.RED);
			g.fillRect(x-2, y-2, drawingSizeInPixels+4, drawingSizeInPixels+4);
		}
		g.setColor(color);
		g.fillRect(x, y, drawingSizeInPixels, drawingSizeInPixels);
		g.setColor(backupColor);
	}

	/**
	 * Same as Node.draw(Graphics g, PositionTransformation pt, boolean highlight), but
	 * draws this node as a disk instead of a square. 
	 * @param g The graphics object
	 * @param pt The position transformation object
	 * @param highlight Whether to highlight the node
	 * @param sizeInPixels The size (in pixels) of the drawing diameter
	 */
	protected void drawAsDisk(Graphics g, PositionTransformation pt, boolean highlight, int sizeInPixels){
		Color backupColor = g.getColor();
		drawingSizeInPixels = sizeInPixels;
		pt.translateToGUIPosition(position);
		int x = pt.guiX - (drawingSizeInPixels >> 1);
		int y = pt.guiY - (drawingSizeInPixels >> 1);
		Color color = getColor();
		if(highlight) {
			// a highlighted node is surrounded by a red square
			g.setColor(color == Color.RED ? Color.BLACK : Color.RED);
			g.fillOval(x-2, y-2, drawingSizeInPixels+4, drawingSizeInPixels+4);
		}
		g.setColor(color);
		g.fillOval(x, y, drawingSizeInPixels, drawingSizeInPixels);
		g.setColor(backupColor);
	}

	/**
	 * Draws a disk for a node and writes some text into the disk. The size 
	 * of the disk is such that all of the text just fits into the disk.
	 * @param g The graphics object
	 * @param pt The position transformation object
	 * @param highlight Whether to highlight the node
	 * @param text The text to include in the node
	 * @param fontSize The size of the font
	 * @param textColor The color of the text
	 */
	protected void drawNodeAsDiskWithText(Graphics g, PositionTransformation pt, boolean highlight, String text, int fontSize, Color textColor){
		// Set the font 
		Font font = new Font(null, 0, (int) (fontSize * pt.getZoomFactor())); 
		g.setFont(font);
		
		// Determine the height and width of the text to be written
		FontMetrics fm = g.getFontMetrics(font); 
		int h = (int) Math.ceil(fm.getHeight());
		int w = (int) Math.ceil(fm.stringWidth(text));
		
		// reset the cover-area of this node s.t. mouse events are recognized correctly 
		this.drawingSizeInPixels = Math.max(h,w);
		pt.translateToGUIPosition(getPosition());
		
		// Draw the node
		Color c = g.getColor();
		g.setColor(this.getColor());
		int d = Math.max(h,w); // draw a square with equal edge length that surrounds the text
		if(highlight) {		// Add a red ring if highlighted 
			g.setColor(Color.RED);
			g.fillOval(pt.guiX - d/2-2, pt.guiY - d/2-2, d+4, d+4); // print a circle for the node
		}
		g.fillOval(pt.guiX - d/2, pt.guiY - d/2, d, d); // print a circle for the node
		
		g.setColor(textColor); // color of the font
		g.drawString(text, pt.guiX - w/2, pt.guiY + h/2 - 2); // print the text onto the circle
		g.setColor(c); // restore color
	}

	
	/**
	 * Draws a square for a node and writes some text into the square. The size 
	 * of the square is such that all of the text just fits into the square.
	 * @param g The graphics object
	 * @param pt The position transformation object
	 * @param highlight Whether to highlight the node
	 * @param text The text to include in the node
	 * @param fontSize The size of the font
	 * @param textColor The color of the text
	 */
	protected void drawNodeAsSquareWithText(Graphics g, PositionTransformation pt, boolean highlight, String text, int fontSize, Color textColor){
		// Set the font 
		Font font = new Font(null, 0, (int) (fontSize * pt.getZoomFactor())); 
		g.setFont(font);
		
		// Determine the height and width of the text to be written
		FontMetrics fm = g.getFontMetrics(font); 
		int h = (int) Math.ceil(fm.getHeight());
		int w = (int) Math.ceil(fm.stringWidth(text));
		
		// reset the cover-area of this node s.t. mouse events are recognized correctly 
		this.drawingSizeInPixels = Math.max(h,w);
		pt.translateToGUIPosition(getPosition());
		
		// Draw the node
		Color c = g.getColor();
		g.setColor(this.getColor());
		int d = Math.max(h,w); // draw a square with equal edge length that surrounds the text
		if(highlight) {		// Add a red ring if highlighted 
			g.setColor(Color.RED);
			g.fillRect(pt.guiX - d/2-2, pt.guiY - d/2-2, d+4, d+4); // print a circle for the node
		}
		g.fillRect(pt.guiX - d/2, pt.guiY - d/2, d, d); // print a circle for the node
		
		g.setColor(textColor); // color of the font
		g.drawString(text, pt.guiX - w/2, pt.guiY + h/2 - 2); // print the text onto the circle
		g.setColor(c); // restore color
	}
	
	/**
	 * Draws this node as a route
	 * @param g The graphics object
	 * @param pt The position transformation object
	 * @param highlight Whether to highlight the node
	 * @param sizeInPixels The diameter of the route in pixels
	 */
	public void drawAsRoute(Graphics g, PositionTransformation pt, boolean highlight, int sizeInPixels) {
		if(routePolygon == null) {
			 routePolygon = new Polygon();
		}

		Color backupColor = g.getColor();
		drawingSizeInPixels = sizeInPixels;
		sizeInPixels >>= 1; // div by 2
		pt.translateToGUIPosition(getPosition());
		int x = pt.guiX;
		int y = pt.guiY;
		Color color = getColor();
		if(highlight) {
			// a highlighted node is surrounded by a red square
			g.setColor(color == Color.RED ? Color.BLACK : Color.RED);
			routePolygon.reset();
			routePolygon.addPoint(x, y + sizeInPixels + 2);
			routePolygon.addPoint(x - sizeInPixels - 2, y);
			routePolygon.addPoint(x, y - sizeInPixels - 2);
			routePolygon.addPoint(x + sizeInPixels + 2, y);
			g.fillPolygon(routePolygon);
		}
		g.setColor(color);
		routePolygon.reset();
		routePolygon.addPoint(x, y+sizeInPixels);
		routePolygon.addPoint(x-sizeInPixels, y);
		routePolygon.addPoint(x, y-sizeInPixels);
		routePolygon.addPoint(x+sizeInPixels, y);
		g.fillPolygon(routePolygon);

		g.setColor(backupColor);
	}
	
	
	/**
	 * Draw this node in PS.
	 * @param pw The PS stream to write the commands for this line to
	 * @param pt Transformation object to obtain GUI coordinates of the endpoints of this edge.
	 */
	public void drawToPostScript(EPSOutputPrintStream pw, PositionTransformation pt) {
		drawToPostscriptAsSquare(pw, pt, drawingSizeInPixels, getColor());
	}

	/**
	 * Draw this node in PS as a square.
	 * @param pw The PS stream to write the commands for this line to
	 * @param pt Transformation object to obtain GUI coordinates of the endpoints of this edge.
	 * @param size The side-length of the square, e.g. drawingSizeInPixels
	 * @param c The color to draw this square with
	 */
	protected void drawToPostscriptAsSquare(EPSOutputPrintStream pw, PositionTransformation pt, double size, Color c) {
		pt.translateToGUIPosition(getPosition());
		pw.setColor(c.getRed(), c.getGreen(), c.getBlue());
		pw.drawFilledRectangle(pt.guiXDouble - (size/2.0), pt.guiYDouble  - (size/2.0), size, size);
	}
	
	/**
	 * Draw this node in PS as a disk.
	 * @param pw The PS stream to write the commands for this line to
	 * @param pt Transformation object to obtain GUI coordinates of the endpoints of this edge.
	 * @param radius The radius of the disk, e.g. use drawingSizeInPixels/2
	 * @param c The color to draw this disk with 			
	 */
	protected void drawToPostScriptAsDisk(EPSOutputPrintStream pw, PositionTransformation pt, double radius, Color c) {
		pt.translateToGUIPosition(getPosition());
		pw.setColor(c.getRed(), c.getGreen(), c.getBlue());
		pw.drawFilledCircle(pt.guiXDouble, pt.guiYDouble, radius);
	}

	/**
	 * Draws this node as a route with the given size
	 * @param pw The PS stream to write the commands for this line to
	 * @param pt Transformation object to obtain GUI coordinates of the endpoints of this edge.
	 * @param size The diameter of this route
	 * @param c The color to use 
	 */
	protected void drawToPostscriptAsRoute(EPSOutputPrintStream pw, PositionTransformation pt, double size, Color c) {
		pt.translateToGUIPosition(getPosition());
		pw.setColor(c.getRed(), c.getGreen(), c.getBlue());
		double d = size / 2;
		pw.drawFilledPolygon(pt.guiXDouble, pt.guiYDouble + d,
		                     pt.guiXDouble - d, pt.guiYDouble, 
		                     pt.guiXDouble, pt.guiYDouble - d,
		                     pt.guiXDouble + d, pt.guiYDouble);
	}
	
	

	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------
	// STEP
	// => For your information. This method should only be called by the framework!
	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------
	
	/**
	 * <b>This method is framework internal and should not be used by the project developer.</b>
	 * This method is called in each round on each node (At least in the synchronous simulation mode) 
	 * by the system. It specifies the order in which the behavior methods are called. Study this
	 * method carefully to understand the simulation.
	 * @throws WrongConfigurationException 
	 */
	public final void step() throws WrongConfigurationException{
		
		//update the message buffer
		packetBuffer.updateMessageBuffer();
		
		preStep();
		
		// check, if some connections have changed in the last step
		if(neighborhoodChanged) {
			neighborhoodChange(); 
		}
		
		timersToHandle.clear();
		// Fire all timers which are going off in this round
		if(timers.size() > 0){
			Iterator<Timer> it = timers.iterator();
			while(it.hasNext()) {
				Timer timer = it.next();
				if(timer.getFireTime() <= Global.currentTime){
					it.remove();
					// we may not call fire() while iterating over the list of timers of this node,
					// as the timer could reschedule itself and require to be added again to the
					// timers list of this node. Therefore, store all timers that fire in a separate
					// list and call them afterwards. 
					timersToHandle.add(timer); 
				}
			}

			// sort timers by their exact time when they expired 
			timersToHandle.sort();
			for(Timer t : timersToHandle) {
				t.fire();
			}
		}

		// Handle dropped messages (messages that were sent by this node, but that do not arrive.
		if(Configuration.generateNAckMessages) {
			PacketCollection pc = Global.isEvenRound ? nAckBufferEvenRound : nAckBufferOddRound;
			if(nackBox == null) {
				nackBox = new NackBox(pc);
			} else {
				nackBox.resetForList(pc);
			}
			handleNAckMessages(nackBox);
		}
		
		//call the 'handleMessages' ALWAYS, and pass the appropriate Inbox. This Inbox
		//can also be a an Iterator over an empty list.
		inbox = packetBuffer.getInbox();
		handleMessages(inbox);
		
		// a custom method that may do something at the end of the step
		postStep();
		
		//all the packets in the inbox and nackBox are not used anymore and can be freed.
		inbox.freePackets();
		if(Configuration.generateNAckMessages) {
			nackBox.freePackets(); // this resets the nAckBuffer
		}
	}

	
	
	
	
	
	
	
	
	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------
	// Framework specific methods and member variables
	// => You should not need to modify/overwrite/call/use any of these members or methods
	//-----------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------
	
	// A boolean indicating whether the neighborhood of this node has changed in this round.
	private boolean neighborhoodChanged = false;
	
	// !!! NOTE: this is a static vector used by all nodes!
	//it gets cleared by every node at the begining of the step-method and thus can be reused by all the nodes.
	private static SortableVector<Timer> timersToHandle = new SortableVector<Timer>();
	
	/**
	 * The list of active timers.
	 */
	private TimerCollection timers = new TimerCollection();

	/**
	 * The current sending intensity of this node. The value
	 * lies in the range [0, 1].  
	 */
	private double intensity = 1.0; 

	/**
	 * A counter to assign each node a unique ID, at the time when it is generated.
	 */
	private static int idCounter = 0; 
	
	/**
	 * <b>This member is framework internal and should not be used by the project developer.</b> 
	 * <p>
	 * Resets the framework s.t. the next generated node receives ID 0, such as 
	 * if the framework would have been restarted. 
	 * <p>
	 * After calling this method and generating new nodes, nodes
	 * generated prior to calling this method should not be used
	 * anymore. 
	 */
	public static void resetIDCounter() {
		idCounter = 0;
	}
	
	/**
	 * <b>This member is framework internal and should not be used by the project developer.</b> 
	 * The node collection may store some implementation dependent information
	 * in this member as to speed up its operation.
	 * If the node collection utilizes this field, it initializes it
	 * when the node is added to the node collection.  
	 */
	public NodeCollectionInfoInterface nodeCollectionInfo = null;
	
	/**
	 * <b>This member is framework internal and should not be used by the project developer.</b><br>
	 * Indicates whether this node has been added to the node colleciton.
	 * 
	 * This flag is set to true when this node is added to the node collection
	 * and set to false when removed from the node collection.   
	 */
	public boolean holdInNodeCollection = false;

	/**
	 * A node-internal iterator over all outgoing edges of this node.
	 */
	private ReusableListIterator<Edge> edgeIteratorInstance = outgoingConnections.iterator();
	
	/**
	 * The buffer, where all arriving messages are stored.
	 */
	private PacketBuffer packetBuffer = new InboxPacketBuffer(true);
	
	/**
	 * Buffer that holds all packets that were sent by this round, and should
	 * have arrived in the previous round, but were dropped.
	 * This list contains the messages that should be reported in the following 
	 * round, whose round-number is even.
	 */
	private PacketCollection nAckBufferEvenRound = new PacketCollection();
	
	/**
	 * Buffer that holds all packets that were sent by this round, and should
	 * have arrived in the previous round, but were dropped.
	 * This list contains the messages that should be reported in the following 
	 * round, whose round-number is odd.
	 */
	private PacketCollection nAckBufferOddRound = new PacketCollection();

	// the color of the node, used in the default getColor() implementation 
	protected Color nodeColor = new Color(0, 0, 0); 

	//the position of the node
	private Position position = new Position(0,0,0);

	/**
	 * Default constructor to construct a node. Initializes the ID of this node. 
	 */
	protected Node() {
		try {
			defaultDrawingSizeInPixels = Configuration.getIntegerParameter("Node/defaultSize");
		} catch (CorruptConfigurationEntryException e) {
			Main.fatalError(e.getMessage());
		}
		// assign the next free ID
		this.ID = ++ idCounter; 
	}
	
	/**
	 * <b>This member is framework internal and should not be used by the project developer.</b> 
	 * Calls the connectivity model of this node to validate and update
	 * all outgoing connections of this node.
	 */
	public final void updateConnections() {
		if(connectivityModel.updateConnections(this)){
			neighborhoodChanged = true;
		} else {
			neighborhoodChanged = false;
		}
	}
	
	/**
	 * <b>This method is framework internal and should not be used by the project developer.</b><br>
	 * @return The list of packets that are being sent to this node.
	 */
	public PacketBuffer getInboxPacketBuffer() {
		return packetBuffer;
	}
	
	/**
	 * <b>This method is framework internal and should not be used by the project developer.</b><br>
	 * @param p Adds packet p to the list of messages that were sent by this node, but did not arrive. 
	 */
	public void addNackPacket(Packet p) {
		if(p.type != PacketType.UNICAST) {
			return; // only nacknowledge unicast messages
		}
		if(Global.isEvenRound) { // add to the buffer of the next round
			nAckBufferOddRound.add(p); 
		} else {
			nAckBufferEvenRound.add(p);
		}
	}
	
	/**
	 * Crops the postion to the the size of the field. This guarantees that the
	 * node does not leave the field.
	 *
	 * @param p The position to crop.
	 */
	private final void cropPos(Position p){
		if(p.xCoord < 0){ 
			p.xCoord = 0; 
		}	else if(p.xCoord >= Configuration.dimX) { 
			p.xCoord = Configuration.dimX - Position.epsilonPosition; 
		} 
		if(p.yCoord < 0) {
			p.yCoord = 0; 
		} else if(p.yCoord >= Configuration.dimY) {
			p.yCoord = Configuration.dimY - Position.epsilonPosition; 
		}
		if(p.zCoord < 0) {
			p.zCoord = 0; 
		} else if(p.zCoord >= Configuration.dimZ) {
			p.zCoord = Configuration.dimZ - Position.epsilonPosition; 
		}
	}
	
	/**
	 * Tests whether this node covers covers a certain position on the screen.
	 * The provided point (x,y) indicates a point on the screen, and first needs
	 * to be transformed to the logical coordinates used by the simulation.  
	 * 
	 * <br> This method is used by the GUI to determine whether the mouse points 
	 * onto this node.
	 *
	 * @param x The x coordinate of the position on the screen
	 * @param y The y coordinate of the position on the screen
	 * @param pt The current transformation.
	 * @return True if this node covers the given position, otherwise false. 
	 */
	public final boolean isInside(int x, int y, PositionTransformation pt){
		pt.translateToGUIPosition(position);
		int delta = (int) (0.5 * drawingSizeInPixels); // half the side-length in pixels of the square
		return Math.abs(x - pt.guiX) <= delta && Math.abs(y - pt.guiY) <= delta;
	}
	
	/**
	 * <b>This member is framework internal and should not be used by the project developer.</b><br>
	 * Finishes the initialization of this node after it has been created. This method
	 * <ul>
	 *   <li>Sets default models for all models that have not been set so far to the appropriate default model. 
	 *       The default models are specified in <code>configuration.java</code> and can be overwritten with the
	 *       XML configuration file.</li>
	 *   <li>If addToRuntime is true, calls the <code>checkRequirement()</code> method of the node-implementatoin.</li>
	 *   <li>If addToRuntime is true, calls the <code>init()</code> method of the node-implementation.</li>
	 *   <li>If addToRuntime is true, adds this node to the runtime environment.</li>
	 * </ul>
	 * @param addToRuntime If true, final addition steps to initialize a node and add it to the framework are performed. 
	 * If set to false, only the default models are added. 
	 * @throws WrongConfigurationException if the node is not properly configurated. 
	 * This mostly happens when node-implementation expects a certain kind of model, 
	 * but that the required model was not set. 
	 */
	public final void finishInitializationWithDefaultModels(boolean addToRuntime) throws WrongConfigurationException {
		// set default (empty) models if they're not yet set
		try {
			if(connectivityModel == null) {
				setConnectivityModel(Model.getConnectivityModelInstance(Configuration.DefaultConnectivityModel));
			}
			if(interferenceModel == null) {
				setInterferenceModel(Model.getInterferenceModelInstance(Configuration.DefaultInterferenceModel));
			}
			if(mobilityModel == null) {
				setMobilityModel(Model.getMobilityModelInstance(Configuration.DefaultMobilityModel));
			}
			if(reliabilityModel == null) {
				setReliabilityModel(Model.getReliabilityModelInstance(Configuration.DefaultReliabilityModel));
			}
			if(addToRuntime) {
				init();
				checkRequirements();
				Runtime.addNode(this);
			}
		}	catch(NullPointerException nPE){
			Global.log.logln(LogL.ERROR_DETAIL, "There was an Exception during the generation of a node" + nPE.getMessage());
			throw nPE;
		}	catch(WrongConfigurationException wCE){
			Global.log.logln(LogL.ERROR_DETAIL, "There was an Exception during the generation of a node: "+wCE.getMessage());
			throw wCE;
		}	
	}
	
	/**
	 * This method broadcasts a Message with a given intensity. Note that this message is more efficient concerning the 
	 * memory usage than the sendPacket message, but note also that with this method the user sends a message and receives 
	 * a packet (as there are always packets received). If that disturbes you use the broadcastPacket method instead. 
	 * 
	 * @param m The message to be sent to all the neighbors.
	 * @param intensity The intensity to send the messages with.
	 */
	private void broadcastMessage(Message m, double intensity){
		//check whether the simulation is currently running or not.
		if(!Global.isRunning && !Global.isAsynchronousMode){
			//The simulation is not running and the broadcast is called. The node is not allowed to 
			//send messages outside of their simulation cycle due to synchronisazion issues. Instead
			//of calling the broadcast method directly please use a timer.
			Main.fatalError("The node "+this.ID+" tried to broadcast a message outside of its simulation " +
					"cycle. Due to synchroniazion issues, this is not allowed.\n" +
					"This problem probably came up due to a call from a nodes popup method.\n" +
					"Do not directly call the broadcast-method but start a timer so that the node sends during its simulation cycle.");
			return;
		}
		// only add the message in the packetsInTheAirBuffer, if interference is turned on
		if(Configuration.interference){
			Packet longestPacket = null; // find the packet that takes longest until delivery
			
			// send the Message to all your neighbors
			edgeIteratorInstance.reset();
			while( edgeIteratorInstance.hasNext()){
				Edge e = edgeIteratorInstance.next();
				Packet sentP = sendMessage(m, e, e.startNode, e.endNode, intensity);
				sentP.type = PacketType.MULTICAST;
				Runtime.packetsInTheAir.addPassivePacket(sentP);
				if(longestPacket == null || longestPacket.arrivingTime < sentP.arrivingTime){ // NOTE that the second statement is not esecuted if the first one is true
					longestPacket = sentP;
				}
			}
			if(longestPacket != null){
				Runtime.packetsInTheAir.upgradeToActivePacket(longestPacket);
			} else { // there was no neighbor
				// For the interference, we need to send a packet anyways. Send it to this
				// node itself. 
				Packet sentP = sendMessage(m, null, this, this, intensity);
				sentP.type = PacketType.MULTICAST;
				sentP.denyDelivery(); // ensure that the packet never arrives at this node
				Runtime.packetsInTheAir.add(sentP);
			} 
		}	else { // no interference
			edgeIteratorInstance.reset();
			while( edgeIteratorInstance.hasNext()){
				Edge e = edgeIteratorInstance.next();
				Packet sentP = sendMessage(m, e, e.startNode, e.endNode, intensity);
				sentP.type = PacketType.DUMMY;
			}
		}
	}
	
	/**
	 * Starts the send-process to deliver a message to a target node.
	 * 
	 * This method sends a clone of the message, such that the sender does not need to 
	 * worry what the receiving node does with the message. This copy is obtained using the
	 * <code>clone</code> method of the message, which the user must implement. 
	 * @param msg The message to be sent.  
	 * @param edge The edge over which the message is sent, may be null, if there is no edge,
	 * in which case the packet is dropped immediately
	 * @param sender The sender node who sends the message
	 * @param target The destination node who should receive the message
	 * @param intensity The radio-intensity of the sender node
	 * @return The packet that has been transmitted.
	 */
	private Packet sendMessage(Message msg, Edge edge, Node sender, Node target, double intensity) {
		if(Global.isAsynchronousMode){
			return asynchronousSending(msg, edge, sender, target, intensity);
		}
		else{
			return synchronousSending(msg, edge, sender, target, intensity);
		}
	}
	
	/**
	 * Sends a message in the asynchronous simulation mode
	 * @param msg The message to be sent
	 * @param edge The edge over which the message is sent, may be null, if there is no edge,
	 * in which case the packet is dropped immediately
	 * @param sender The sender node who sends the message
	 * @param target The destination node who should receive the message
	 * @param intensity The intensity at which the message is sent
	 * @return The packet encapsulating the message
	 */
	private Packet asynchronousSending(Message msg, Edge edge, Node sender, Node target, double intensity){
				
		Message clonedMsg = msg.clone(); // send a copy of the message
		if(clonedMsg == null) {
			Main.fatalError("The clone() method of '" + msg.getClass().getName() + "' returns null \n" + "instead of a copy of the message.");
		}
		Packet packet = Packet.fabricatePacket(clonedMsg);
		double transmissionTime = Global.messageTransmissionModel.timeToReach(sender, target, msg);
		
		// fill in the data of the header
		packet.arrivingTime = Global.currentTime + transmissionTime;
		packet.sendingTime = Global.currentTime;
		packet.origin = sender;
		packet.destination = target;
		packet.edge = edge;
		packet.intensity = intensity;
		packet.type = PacketType.UNICAST;
//		 this property must be checked when the entire packet was assembled
		if(edge != null) {
			packet.positiveDelivery = reliabilityModel.reachesDestination(packet);
			edge.addMessageForThisEdge(packet.message);
		} else {
			packet.positiveDelivery = false; // when there is no edge, the packet is immediately dropped
		}
		
		Global.numberOfMessagesOverAll++; // statistics (don't increment the counter that counts the number of sent messages per round. This counter has no meaning in the async mode.)
		
		Runtime.eventQueue.insert(PacketEvent.getNewPacketEvent(packet, Global.currentTime + transmissionTime));
		
		return packet;
	}
	
	/**
	 * Sends a message in the synchronous simulation mode
	 * @param msg The message to be sent
	 * @param edge The edge over which the message is sent, may be null, if there is no edge,
	 * in which case the packet is dropped immediately
	 * @param sender The sender node who sends the message
	 * @param target The destination node who should receive the message
	 * @param intensity The intensity at which the message is sent
	 * @return The packet encapsulating the message
	 */
	private Packet synchronousSending(Message msg, Edge edge, Node sender, Node target, double intensity){
		//check whether the simulation is currently running or not.
		if(!Global.isRunning){
			//The simulation is not running and the send is called. The node is not allowed to 
			//send messages outside of their simulation cycle due to synchronisazion issues. Instead
			//of calling the send-method directly please use a timer.
			Main.fatalError("The node "+this.ID+" tried to send a message outside of its simulation " +
					"cycle. Due to synchroniazion issues, this is not allowed.\n" +
					"This problem probably came up due to a call from a nodes popup method.\n" +
					"Do not directly call the send-method but start a timer so that the node sends during its simulation cycle.");
			//this will never happen because the fatal error will kill the application.
			return null;
		}
		else{
			Message clonedMsg = msg.clone(); // send a copy of the message
			if(clonedMsg == null) {
				Main.fatalError("The clone() method of '" + msg.getClass().getName() + "' returns null \n" + "instead of a copy of the message.");
			}
			Packet packet = Packet.fabricatePacket(clonedMsg);
			double transmissionTime = Global.messageTransmissionModel.timeToReach(sender, target, msg);
			
			// fill in the data of the header
			packet.arrivingTime = Global.currentTime + transmissionTime;
			packet.sendingTime = Global.currentTime;
			packet.origin = sender;
			packet.destination = target;
			packet.edge = edge;
			packet.intensity = intensity;
			packet.type = PacketType.UNICAST;
	//		 this property must be checked when the entire packet was assembled
			if(edge != null) {
				packet.positiveDelivery = reliabilityModel.reachesDestination(packet);
				edge.addMessageForThisEdge(packet.message);
			} else {
				packet.positiveDelivery = false; // when there is no edge, the packet is immediately dropped
			}
			
			target.packetBuffer.addPacket(packet); // place the packet in the targets receive buffer
			
			Global.numberOfMessagesInThisRound++; // statistics (At the end of the round, this member is added to Global.numberOfMessagesOverAll.)
			
			return packet;
		}
	}

	/**
	 * <b>This member is framework internal and should not be used by the project developer.</b> 
	 * Generates a node given its class name.
	 * <p>
	 * This method only creates a node object, but does not set any of its member variables and also 
	 * does not add it to the runtime system.
	 * <p>
	 * The classPath must be either absolute (of the form jnr.projects.xx.nodes.nodeImplementations.YY) 
	 * or relative (of the form YY). 
	 * If the classPath is relative, it may be prefixed with the project-name and a colon: (PP:YY, where 
	 * PP stands for the project name). The project-prefix is necessary to denote that the nodes 
	 * implementation can be found in this project.
	 *
	 * @param classPath The classPath of the node implementation to use. 
	 * @return A new node implementation of the class corresponding to the classname.
	 * @throws WrongConfigurationException If the node cannot be created.
	 */
	public final static Node createNodeByClassname(String classPath) throws WrongConfigurationException {
		Class<?> nodeClass;
		Node node = null;
		
		if(!classPath.contains(".")) { // is a relative path
			if(classPath.contains(":")){ // project implementation
				String[] splitter = classPath.split(":");
				classPath = Configuration.userProjectsPath + "." + splitter[0] + ".nodes.nodeImplementations." + splitter[1];
			} else {
				classPath = Configuration.defaultProjectPath + ".nodes.nodeImplementations." + classPath;
			}
		}
		
		try{
			nodeClass = Class.forName(classPath);
			Constructor<?> constructor = nodeClass.getConstructor(); 
			// NOTE: we could also call newInstance() on the class-object. But this would not encapsulate 
			// exceptions that may be thrown in the constructor.
			node = (Node) constructor.newInstance();
		}
		catch(ClassNotFoundException e) {
			throw new WrongConfigurationException(e, "Class not found. Please write a class called: " + classPath + ".java and compile it."); 
		} catch(NoClassDefFoundError e) {
			// happens when the user uses wrong upper and lower case for the type
			throw new WrongConfigurationException(e, "Class not found ("+ classPath + ".java). Please ensure correct upper and lower case."); 
		}	catch(ClassCastException e) { 
			throw new WrongConfigurationException(e, classPath + ".java does not extend class Node.java.");
		}	catch(InstantiationException e){
			throw new WrongConfigurationException(e, "Could not instanciate a node object of type " + classPath);
		}	catch(IllegalAccessException e){
			throw new WrongConfigurationException(e, "Failed while creating a node of type " + classPath);			
		} catch (IllegalArgumentException e) {
			throw new WrongConfigurationException(e, "Could not instanciate a node object of type " + classPath);
		} catch (InvocationTargetException e) {
			throw new WrongConfigurationException(e, "Could not instanciate a node object of type " + classPath);
		} catch (SecurityException e) {
			throw new WrongConfigurationException(e, "Could not instanciate a node object of type " + classPath);
		} catch (NoSuchMethodException e) {
			throw new WrongConfigurationException(e, "Could not instanciate a node object of type " + classPath);
		}
		return node;
	}

  // the DLLE entry for the DoublyLinkedList
	private DLLFingerList dllFingerList = new DLLFingerList();

	/** 
	 * <b>This member is framework internal and should not be used by the project developer.</b> 
	 * @see sinalgo.tools.storage.DoublyLinkedListEntry#getDoublyLinkedListFinger()
	 */
	public DLLFingerList getDoublyLinkedListFinger() {
		 // <b>This method is framework internal and should not be used by the project developer.</b> 
		return dllFingerList;
	}
}
