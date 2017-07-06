package projects.sample5.nodes.messages;

import projects.sample5.nodes.timers.RetryFloodingTimer;
import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Message;

/**
 * The message used to determine a route between two nodes using
 * incremental flooding.
 * This message requires the read-only policy. 
 */
public class FloodFindMsg extends Message {

	/**
	 * True if this is a find-message, false if it is the answer-message 
	 * that returns from the destination when the flooding was successful.
	 */
	public boolean isFindMessage = true; 

	/**
	 * The TTL for this message when it's being sent as a find-msg
	 */
	public int ttl;
	
	/**
	 * The node that wishes to determine the route to the desinationNode;
	 */
	public Node sender;
	
	
	/**
	 * Number of hops to sender 
	 */
	public int hopsToSender;
	
	/**
	 * Sequence ID of this message 
	 */
	public int sequenceID; 
	
	public RetryFloodingTimer retryTimer = null;
	
	/**
	 * The node to which a route should be established. 
	 */
	public Node destination;
	
	/**
	 * Default constructor. 
	 */
	public FloodFindMsg(int seqID, Node sender, Node dest) {
		ttl = 4; // initial TTL
		isFindMessage = true;
		hopsToSender = 0;
		sequenceID = seqID;
		this.sender = sender;
		destination = dest;
	}
	
	@Override
	public Message clone() {
		// This message requires a read-only policy
		return this;
	}
	
	
	/**
	 * @return A real clone of this message, i.e. a new message object
	 */
	public FloodFindMsg getRealClone() {
		FloodFindMsg msg = new FloodFindMsg(this.sequenceID, this.sender, this.destination);
		msg.ttl = this.ttl;
		msg.isFindMessage = this.isFindMessage;
		msg.hopsToSender = this.hopsToSender;
		msg.retryTimer = this.retryTimer;
		return msg;
	}

}
