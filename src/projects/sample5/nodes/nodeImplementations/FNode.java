package projects.sample5.nodes.nodeImplementations;

import java.awt.Color;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;

import projects.defaultProject.models.messageTransmissionModels.ConstantTime;
import projects.sample5.nodes.messages.AckPayload;
import projects.sample5.nodes.messages.FloodFindMsg;
import projects.sample5.nodes.messages.PayloadMsg;
import projects.sample5.nodes.timers.PayloadMessageTimer;
import projects.sample5.nodes.timers.RetryFloodingTimer;
import projects.sample5.nodes.timers.RetryPayloadMessageTimer;
import sinalgo.configuration.WrongConfigurationException;
import sinalgo.gui.helper.NodeSelectionHandler;
import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Inbox;
import sinalgo.nodes.messages.Message;
import sinalgo.nodes.messages.NackBox;
import sinalgo.tools.Tools;
import sinalgo.tools.logging.Logging;

/**
 * A node that implements a flooding strategy to determine paths to other nodes.
 */
public class FNode extends Node {


	/**
	 * A routing table entry
	 */
	public class RoutingEntry {
		public int sequenceNumber; // sequence number used when this entry was created
		public int numHops; // number of hops to reach destination
		public Node nextHop; // next hop to take  

		public RoutingEntry(int seqNumber, int hops, Node hop) {
			this.sequenceNumber = seqNumber;
			this.numHops = hops;
			this.nextHop = hop;
		}
	}
	
	// counter, incremented and added for each msg sent (not forwarded) by this node
	public int seqID = 0; // an ID used to distinguish successive msg
	
	// The routing table of this node, maps destination node to a routing entry
	Hashtable<Node, RoutingEntry> routingTable = new Hashtable<Node, RoutingEntry>();
	
	// messages that could not be sent so far, because no route is known
	Vector<PayloadMsg> messagesOnHold = new Vector<PayloadMsg>();
	
	/**
	 * Method to clear this node's routing table 
	 */
	public void clearRoutingTable() {
		routingTable.clear();
	}
	
	@Override
	public void checkRequirements() throws WrongConfigurationException {
		// The message delivery time must be constant, this allows the project
		// to easily predict the waiting times
		if(!(Tools.getMessageTransmissionModel() instanceof ConstantTime)) {
			Tools.fatalError("This project requires that messages are sent with the ConstantTime MessageTransmissionModel.");
		}
	}

	@Override
	public void handleMessages(Inbox inbox) {
		while(inbox.hasNext()) {
			Message msg = inbox.next();
			
			// ---------------------------------------------------------------
			if(msg instanceof FloodFindMsg) { // This node received a flooding message. 
				// ---------------------------------------------------------------
				FloodFindMsg m = (FloodFindMsg) msg;
				if(m.isFindMessage) { 
					// forward the message, it's a find-message that has to be 
					// forwarded if the TTL allows. At the same time, update this node's routing
					// table s.t. it knows how to route to the sender of the flooding-msg.
					boolean forward = true;
					if(m.sender.equals(this)) { // the message bounced back - discard the msg
						forward = false; 
					} else { // update routing table to the sender of this node
						RoutingEntry re = routingTable.get(m.sender);
						if(re == null) { // add a new routing entry 
							routingTable.put(m.sender, new RoutingEntry(m.sequenceID, m.hopsToSender, inbox.getSender()));
							useNewRoutingInfo(m.destination, inbox.getSender());
						} else if(re.sequenceNumber < m.sequenceID) { // update the existing entry 
							re.numHops = m.hopsToSender;
							re.sequenceNumber = m.sequenceID;
							re.nextHop = inbox.getSender();
						} else {
							forward = false; // we've already seen this message once - don't forward it a 2nd time
						}
					}
					if(m.destination.equals(this)) { // the lookup has succeeded, this is the node that was searched
						this.setColor(Color.BLUE);
						FloodFindMsg copy = m.getRealClone();
						copy.hopsToSender = 1; // now, this field contains the hops to the destination
						copy.isFindMessage = false;
						copy.sequenceID = ++this.seqID;
						this.send(copy, inbox.getSender()); // send back the echo message
						forward = false;
					}

					if(forward && m.ttl > 1) { // forward the flooding request
						FloodFindMsg copy = m.getRealClone();
						copy.ttl--;
						copy.hopsToSender++;
						this.broadcast(copy);
					}
				} else { // return the message back to the sender
					// update the routing table
					boolean forward = true;
					this.setColor(Color.GREEN);
					RoutingEntry re = routingTable.get(m.destination);
					if(re == null) { // add a new routing entry 
						routingTable.put(m.destination, new RoutingEntry(m.sequenceID, m.hopsToSender, inbox.getSender()));
						useNewRoutingInfo(m.destination, inbox.getSender());
					} else if(re.sequenceNumber < m.sequenceID) { // update the existing entry 
						re.numHops = m.hopsToSender;
						re.sequenceNumber = m.sequenceID;
						re.nextHop = inbox.getSender();
					} else {
						forward = false; 
					}
					if(m.sender.equals(this)) {
						// this node sent the request - remove timers
						m.retryTimer.deactivate();
					} else if(forward) {
						re = routingTable.get(m.sender);
						if(re != null) {
							m.hopsToSender++; // we can modify the message, its a unicast
							send(m, re.nextHop);
						}
					}
				}
			} 
			// ---------------------------------------------------------------
			if(msg instanceof PayloadMsg) {
				PayloadMsg m = (PayloadMsg) msg;
				if(m.destination.equals(this)) { // the message was for this node
					if(msg instanceof AckPayload) { // it is an ACK message
						m.ackTimer.deactivate();
						this.setColor(Color.ORANGE);
					} else { // it is a Payload Msg
						// handle the payload
						this.setColor(Color.YELLOW);
						// send back an ACK
						AckPayload ack = new AckPayload(m.sender, this);
						ack.sequenceNumber = m.sequenceNumber;
						ack.ackTimer = m.ackTimer;
						sendPayloadMessage(ack);
					}
				} else { // the message was not for this node -> forward
					sendPayloadMessage(m);
				}
			} 
		}
	}

	/* (non-Javadoc)
	 * @see sinalgo.nodes.Node#handleNAckMessages(sinalgo.nodes.messages.NackBox)
	 */
	public void handleNAckMessages(NackBox nackBox) {
		Logging log = Logging.getLogger();
		while(nackBox.hasNext()) {
			nackBox.next();
			log.logln("Node " + this.ID + " could not send a message to " + nackBox.getReceiver().ID);
		}
	}
	
	@NodePopupMethod(menuText = "Send Message To...")
	public void sendMessageTo() {
		Tools.getNodeSelectedByUser(new NodeSelectionHandler() {
			public void handleNodeSelectedEvent(Node n) {
				if(n == null) {
					return; // aborted
				}
				PayloadMsg msg = new PayloadMsg(n, FNode.this);
				msg.requireACK = true;
				msg.sequenceNumber = ++FNode.this.seqID;
				PayloadMessageTimer t = new PayloadMessageTimer(msg);
				t.startRelative(1, FNode.this);
			}
		}, "Select a node to send a message to...");
	}

	/**
	 * Tries to send a message if there is a routing entry. 
	 * If there is no routing entry, a search is started, and the
	 * message is put in a buffer of messages on hold.
	 * @param msg
	 * @param to
	 */
	public void sendPayloadMessage(PayloadMsg msg) {
		RoutingEntry re = routingTable.get(msg.destination);
		if(re != null) {
			if(msg.sender.equals(this) && msg.requireACK) { // this node wants to have the message sent - it waits for an ack
				RetryPayloadMessageTimer rpmt = new RetryPayloadMessageTimer(msg);
				rpmt.startRelative(re.numHops * 3, this); // We wait a bit longer than necessary
				if(msg.ackTimer != null){
					msg.ackTimer.deactivate();
				}
				msg.ackTimer = rpmt;
			}
			send(msg, re.nextHop);
			return ;
		} else {
			lookForNode(msg.destination, 4);
			messagesOnHold.add(msg);
		}
	}
	
	/**
	 * Starts a search for a given node with the given TTL
	 * @param destination
	 * @param ttl
	 */
	public void lookForNode(Node destination, int ttl) {
		if(ttl > 10000000) { // this limits to graphs of diameter 10^7 ....
			return; // we've already searched too far - there is probably no connection! 
		}

		FloodFindMsg m = new FloodFindMsg(++this.seqID, this, destination);
		m.ttl = ttl;
		RetryFloodingTimer rft = new RetryFloodingTimer(destination, m.ttl);
		// The TTL must depend on the message transmission time. We assume here a constant msg. transm. time of 1 unit.
		rft.startRelative(m.ttl * 2 + 1, this); 
		m.retryTimer = rft;
		this.broadcast(m);
	}
	
	private void useNewRoutingInfo(Node destination, Node nextHop) {
		Iterator<PayloadMsg> it = messagesOnHold.iterator();
		while(it.hasNext()) {
			PayloadMsg m = it.next();
			if(m.destination.equals(destination)) {
				this.sendPayloadMessage(m);
				it.remove();
			}
		}
	}
	
	@Override
	public void init() {
	}

	@Override
	public void neighborhoodChange() {
		// we could remove routing-table entries that use this neighbor
	}

	@Override
	public void preStep() {
	}

	@Override
	public void postStep() {
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.nodes.Node#toString()
	 */
	public String toString() {
		// show the routing table entries
		String r = "";
		for(Entry<Node, RoutingEntry> e : routingTable.entrySet()) {
			r += e.getKey().ID + " => " + e.getValue().nextHop.ID + " (" + e.getValue().numHops+ ")"+ "\n";
		}
		return "\n" + r;
	}
}
