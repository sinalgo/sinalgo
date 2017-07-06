package projects.sample5.nodes.messages;

import projects.sample5.nodes.timers.RetryPayloadMessageTimer;
import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Message;

/**
 * A message used to transport data from a sender to a receiver
 */
public class PayloadMsg extends Message {
	public Node destination; // node who should receive this msg
	public Node sender; // node who sent this msg
	public int sequenceNumber; // a number to identify this msg, set by the sender 
	public RetryPayloadMessageTimer ackTimer; // The timer set on the sender that will fire if there is no ACK returning from the destination
	public boolean requireACK = false; // Indicates whether this msg needs to be ACKed  
	
	/**
	 * Constructor
	 * @param destination The node to send this msg to
	 * @param sender The sender who sends this msg
	 */
	public PayloadMsg(Node destination, Node sender) {
		this.destination = destination;
		this.sender = sender;
	}
	
	@Override
	public Message clone() {
		return this; // requires the read-only policy
	}

}
